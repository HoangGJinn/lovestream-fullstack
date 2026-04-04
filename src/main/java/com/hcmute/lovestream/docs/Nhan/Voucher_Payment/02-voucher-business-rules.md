# Bước 2 — Quy tắc nghiệp vụ voucher (chuẩn hoá)

Tài liệu cố định hóa rule dùng chung cho UI, `Payment`, VNPAY và callback. Phần triển khai trong code: `com.hcmute.lovestream.service.voucher.VoucherBusinessRules`.

## 1. Phạm vi gói dịch vụ

- **Mọi gói đang bán** (bao gồm 3 gói hiện có) đều **có thể** áp cùng một voucher, **không** loại trừ theo `ServicePlan`.
- Entity `Voucher` **không** gắn `planId`; sau này nếu cần giới hạn theo gói thì bổ sung schema + rule mới.

## 2. Điều kiện voucher được phép dùng (lúc validate)

Đủ **cả bốn** điều kiện sau (và dữ liệu không null theo entity):

| Điều kiện | Ý nghĩa |
|-----------|---------|
| `status == ACTIVE` | Bị `BLOCKED` thì không dùng được. |
| Ngày sử dụng ≤ `expiryDate` | Hết hạn: ngày hiện tại **sau** `expiryDate` → không dùng được (ngày hết hạn vẫn dùng được **inclusive**). |
| `usedQuantity < totalQuantity` | Hết lượt phát hành → không dùng được. |

## 3. Công thức giảm giá và làm tròn VND

- `discountPercent` là **phần trăm giảm** trên giá gói (0–100).
- **Số tiền khách phải trả** = giá gói × `(100 - discountPercent) / 100`, sau đó **làm tròn đến đơn vị đồng**, **`HALF_UP`** (khớp `RoundingMode.HALF_UP`, scale 0).
- **Ví dụ:** 99.000đ, giảm **40%** → trả **60%** × 99.000 = **59.400đ** (không phải 60.000đ trừ khi làm tròn khác — trong hệ thống này dùng công thức trên).

## 4. Số lượng (`usedQuantity` / `totalQuantity`)

- `totalQuantity`: trần phát hành, **không** tăng theo giao dịch.
- `usedQuantity`: tăng **một đơn vị** khi **một** giao dịch thanh toán **chuyển thành công** (sau callback VNPAY `00`), **không** tăng khi user chỉ nhập mã xem thử trên form.
- **Idempotent:** Nếu callback lặp hoặc giao dịch đã `SUCCESS`, **không** tăng `usedQuantity` thêm lần nữa (chi tiết xử lý ở bước callback / bước 6 checklist).

## 5. Giới hạn kỹ thuật thanh toán

- VNPAY yêu cầu số tiền **> 0**. Nếu công thức cho ra **0đ** (ví dụ giảm 100%), luồng hiện tại **không** tạo được URL thanh toán — cần chính sách riêng (voucher admin không cho `discountPercent == 100`, hoặc luồng kích hoạt không qua VNPAY). Class `VoucherBusinessRules` có hằng và kiểm tra tối thiểu để phát hiện sớm.
