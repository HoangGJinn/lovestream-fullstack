# Checklist: Tích hợp Voucher vào luồng thanh toán (VNPAY)

Tài liệu này liệt kê các bước cần làm và hướng dẫn chi tiết **ngay dưới từng bước**.

## Kỳ vọng nghiệp vụ (đã thống nhất)

- User **chọn / nhập voucher trên LoveStream trước** khi sang cổng thanh toán. Sau khi áp dụng, **giá hiển thị và số tiền phải trả** là giá **sau giảm**.
- **Ví dụ:** Giá gói **99.000đ**, voucher **giảm 40%** → số tiền thanh toán **còn 60% giá gốc** (khoảng **59.400đ**, có thể khác một chút tuỳ **quy tắc làm tròn VND**). Trên **màn hình thanh toán VNPAY** (sandbox), phần số tiền giao dịch phải phản ánh **~59–60k**, **không** còn 99.000đ.
- **Tài khoản ngân hàng mẫu NCB** trên sandbox chỉ là **cách chọn thử nghiệm** khi thanh toán; **không** có logic riêng theo ngân hàng. Mọi phương thức trên VNPAY đều dùng chung **`vnp_Amount`** đã được server tính **sau khi trừ khuyến mãi**.

---

## [x] Bước 1 — Nắm ràng buộc kiến trúc (VNPAY vs ứng dụng)

**Mục tiêu:** Tránh kỳ vọng sai về chỗ hiển thị voucher.

**Đã thực hiện:** Tài liệu `01-vnpay-voucher-architecture.md` (cùng thư mục); Javadoc trên `VnpayServiceImpl`, `createPaymentWithOrderCode`, `ServicePlanServiceImpl#purchasePlan`.

### Hướng dẫn

- Trang chọn phương thức thanh toán trên **sandbox.vnpayment.vn** là giao diện của VNPAY; **không** chèn được ô nhập mã voucher lên đó.
- Số tiền gửi sang VNPAY (`vnp_Amount`) được khóa **tại thời điểm server tạo URL thanh toán**. Vì vậy: **áp dụng voucher trên LoveStream trước**, rồi mới redirect sang VNPAY với **đúng số tiền sau giảm** — đó là lý do khi vào bước thanh toán (kể cả khi chọn **NCB** để test) user thấy **giá đã trừ voucher**, không phải giá niêm yết ban đầu.
- Luồng đúng: **Chọn gói → nhập/chọn voucher (nếu có) → thấy giá cuối trên LoveStream → Xác nhận thanh toán → Redirect VNPAY (hiển thị cùng mức giá cuối).**

---

## [x] Bước 2 — Chuẩn hoá quy tắc nghiệp vụ voucher

**Mục tiêu:** Thống nhất rule trước khi code.

**Đã thực hiện:** `02-voucher-business-rules.md`; class `VoucherBusinessRules` (`normalizeCode`, `isEligibleForUse`, `computePayableAmount`, `isPayableThroughVnpay`); test `VoucherBusinessRulesTest`.

### Hướng dẫn

- **Áp dụng gói:** Cả **3 gói** (hoặc mọi gói đang bán) đều dùng chung voucher — **không** loại trừ gói nào trừ khi sau này bổ sung field ràng buộc (hiện entity `Voucher` không gắn `ServicePlan`).
- **Điều kiện voucher hợp lệ:** `status == ACTIVE`, chưa hết hạn (`expiryDate`), `usedQuantity < totalQuantity`.
- **Giảm giá:** Dùng `discountPercent` trên giá gói (ví dụ 40% → khách trả **60%** giá gói); quy định rõ **cách làm tròn VND** (ví dụ làm tròn đến đơn vị đồng, `HALF_UP`) để số **59.400đ** vs **60.000đ** thống nhất giữa UI, `Payment.amount` và VNPAY.
- **Số lượng:** `totalQuantity` là trần phát hành (cố định); mỗi lần thanh toán **thành công** thì tăng **`usedQuantity`** — không tăng khi user chỉ “thử mã” trên form.
- **Một giao dịch — một lần tăng:** Tránh double-count khi callback gọi lại; cần **idempotent** (chỉ tăng khi chuyển trạng thái `PENDING` → `SUCCESS` lần đầu).

---

## [x] Bước 3 — Tách logic “validate & tính giá” (service layer)

**Mục tiêu:** Một nơi duy nhất kiểm tra mã và trả về số tiền sau giảm.

**Đã thực hiện:** `VoucherCheckoutService` / `VoucherCheckoutServiceImpl` với `validateAndCompute` (mặc định ngày theo `Asia/Ho_Chi_Minh`); DTO `VoucherQuoteResponse`; test `VoucherCheckoutServiceImplTest`.

### Hướng dẫn

- Tạo method kiểu `validateAndCompute(String voucherCode, BigDecimal planPrice)` (hoặc tương đương) trong service voucher (có thể mở rộng `VoucherService` hoặc service checkout riêng).
- Validate: chuẩn hoá mã (trim, upper-case), tìm `Voucher` theo `code`, kiểm tra ACTIVE, hạn dùng, `usedQuantity < totalQuantity`.
- Trả về: `finalAmount`, `discountPercent` đã áp dụng, `voucherId` (để gắn vào `Payment`).
- **Không** tăng `usedQuantity` ở bước này — chỉ đọc và tính.

---

## [x] Bước 4 — Mở rộng API mua gói (`purchasePlan` / `upgradePlan`)

**Mục tiêu:** Cho phép client gửi kèm mã voucher (tuỳ chọn).

**Đã thực hiện:** `POST .../purchase` và `POST .../upgrade` nhận `voucherCode` (query, tuỳ chọn); `ServicePlanServiceImpl` gọi `VoucherCheckoutService`, set `Payment.amount` + `voucher`, `Vnpay.amount` và `vnp_OrderInfo` (có `VC:` khi có mã).

### Hướng dẫn

- Thêm tham số tuỳ chọn: ví dụ `voucherCode` (nullable) vào body/query của endpoint mua gói trong `ServicePlanRestController` (hoặc DTO riêng).
- Trong `ServicePlanServiceImpl.purchasePlan` (và `upgradePlan` nếu cùng luồng VNPAY):
  - Nếu **không** có mã: giữ hành vi cũ (full giá gói).
  - Nếu **có** mã: gọi bước 3 → `Payment.amount` = **số tiền sau giảm**; `payment.setVoucher(...)`; `Vnpay.amount` khớp với `Payment.amount`.
  - `orderInfo` có thể ghi thêm gợi ý đã dùng voucher (tuỳ chọn, giới hạn độ dài theo VNPAY).
- Đảm bảo `transactionCode` / `vnp_TxnRef` vẫn khớp với cách `VnpayServiceImpl` và callback tìm `Payment`.

---

## [x] Bước 5 — Xử lý an toàn trong callback VNPAY

**Mục tiêu:** Đồng bộ số tiền và cập nhật số lượng đã dùng đúng lúc.

### Hướng dẫn

- Sau khi verify chữ ký (`vnp_SecureHash`), **so sánh** `vnp_Amount` (nhớ đơn vị VNPAY = VND × 100) với `payment.getAmount()` đã lưu. Nếu lệch → **không** xử lý thành công (log cảnh báo).
- Khi `vnp_ResponseCode == "00"` và chuyển `Payment` sang `SUCCESS` **lần đầu**:
  - Nếu `payment.getVoucher() != null`: tăng `usedQuantity` **một lần** (xem bước 6).
- Giữ nguyên logic tạo/cập nhật `Subscription` hiện có; chỉ đảm bảo giao dịch thành công vẫn dựa trên đúng `Payment` đã lưu.

---

## [x] Bước 6 — Tăng `usedQuantity` an toàn khi nhiều user đồng thời

**Mục tiêu:** Không vượt quá `totalQuantity` khi race condition.

### Hướng dẫn

- Ưu tiên: câu lệnh cập nhật **atomic** kiểu  
  `UPDATE vouchers SET used_quantity = used_quantity + 1 WHERE id = ? AND used_quantity < total_quantity`  
  và kiểm tra số dòng bị ảnh hưởng; nếu 0 → log lỗi nghiệp vụ (không nên xảy ra nếu bước tạo payment đã reserve đúng).
- Hoặc dùng khóa pessimistic trên dòng `Voucher` khi tạo payment pending (nặng hơn, đơn giản hơn về suy luận).
- **Idempotency:** Chỉ tăng khi payment chuyển sang SUCCESS từ trạng thái chưa success; nếu callback lặp và payment đã SUCCESS thì **không** tăng lại.

---

## [x] Bước 7 — API phụ (tuỳ chọn): “đề xuất” / danh sách voucher dùng được

**Mục tiêu:** Frontend có thể hiển thị mã còn lượt (không bắt buộc nếu chỉ nhập tay).

### Hướng dẫn

- Endpoint GET (authenticated hoặc public tuỳ policy): trả về các voucher **ACTIVE**, còn hạn, `usedQuantity < totalQuantity`.
- **Không** cần lọc theo `planId` nếu chính sách là áp dụng mọi gói.
- Ẩn hoặc không trả field nhạy cảm nếu không cần (tuỳ product).

---

## [x] Bước 8 — Giao diện người dùng (trang gói)

**Mục tiêu:** User nhập/chọn voucher **trước** khi bấm thanh toán.

### Hướng dẫn

- Vị trí hợp lý: `templates/plans/detail.html` (hoặc `list.html` nếu mua ngay từ danh sách) — ô nhập mã, nút “Áp dụng”, hiển thị rõ **giá gốc** (vd. 99.000đ) → **% giảm** → **thành tiền phải trả** (vd. ~59.400đ). Số “thành tiền” này phải **trùng** với số tiền sẽ gửi sang VNPAY.
- Gọi API validate (bước 3 exposed qua REST) để preview; hoặc chỉ gửi mã khi gọi API purchase (ít request hơn nhưng UX kém hơn nếu không báo lỗi sớm).
- Sau khi nhận `paymentUrl`, `window.location.href = paymentUrl` như hiện tại.
- Copy UX: nhắc user rằng **trang VNPAY sẽ hiển thị đúng số tiền sau giảm** (vd. ~60k), không còn giá niêm yết đầy đủ — trùng với kỳ vọng khi chỉ dùng **NCB sandbox** để hoàn tất giao dịch thử.

---

## Bước 9 — Kiểm thử thủ công & biên

**Mục tiêu:** Cover các tình huống dễ lỗi.

### Hướng dẫn

- Voucher hết hạn / khóa / hết lượt → từ chối với thông báo rõ.
- Mã đúng + đủ lượt → **số tiền trên màn VNPAY** (và sau khi chọn NCB / nhập thẻ test) **khớp** số “thành tiền” đã xem trên LoveStream; sau callback: subscription đúng, `usedQuantity` +1.
- Thanh toán thất bại / huỷ → `usedQuantity` **không** tăng.
- Hai tab cùng dùng một mã khi chỉ còn 1 lượt → chỉ một giao dịch thành công (kiểm tra race).
- Nâng cấp gói (`upgradePlan`) nếu có voucher: áp dụng cùng rule với mua mới (nếu nghiệp vụ cho phép).

---

## Bước 10 — Admin & quan sát (đối chiếu)

**Mục tiêu:** Dễ kiểm tra sau khi deploy.

### Hướng dẫn

- Trang admin voucher (`admin-vouchers.html`) đã có các cột; đảm bảo hiển thị **`usedQuantity` / `totalQuantity`** (hoặc tương đương) để vận hành thấy mã “hết slot”.
- Log khi callback amount mismatch hoặc khi increment voucher thất bại.

---

_Checklist có thể đánh dấu từng bước khi hoàn thành (ví dụ `[x]` ở đầu tiêu đề bước)._
