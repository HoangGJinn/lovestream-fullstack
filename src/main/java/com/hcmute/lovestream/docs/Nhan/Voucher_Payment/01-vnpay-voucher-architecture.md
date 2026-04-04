# Bước 1 — Ràng buộc kiến trúc: VNPAY vs LoveStream (voucher)

Tài liệu cố định hóa các điểm đã ghi nhận trong checklist; mọi thay đổi luồng thanh toán + voucher nên đối chiếu với file này.

## 1. Giao diện VNPAY không tùy biến

- Trang **sandbox.vnpayment.vn** (chọn phương thức, NCB, v.v.) do **VNPAY** cung cấp.
- **Không thể** nhúng ô nhập mã voucher hoặc chỉnh layout trên trang đó.
- Mọi nhập / chọn voucher phải thực hiện **trên ứng dụng LoveStream** trước khi chuyển sang cổng.

## 2. Số tiền khóa tại lúc tạo URL thanh toán

- Tham số **`vnp_Amount`** (và chữ ký đi kèm) được **khóa tại thời điểm** server gọi tạo URL thanh toán (`VnpayServiceImpl#createPaymentWithOrderCode`).
- `paymentRequest.getAmount()` phải là **số tiền cuối cùng** khách phải trả (sau mọi khuyến mãi / voucher), đơn vị VND trong DTO; service sẽ nhân 100 theo yêu cầu VNPAY.
- Sau khi user đã ở trên VNPAY, **không** đổi được số tiền bằng cách thêm voucher trên UI — voucher phải đã được áp **trước** bước này.

## 3. Luồng nghiệp vụ mong muốn

1. User chọn gói trên LoveStream.
2. (Tuỳ chọn) Nhập / chọn voucher → hiển thị **giá cuối** trên LoveStream.
3. Xác nhận thanh toán → server tạo `Payment` với `amount` = giá cuối → tạo URL VNPAY với cùng số tiền.
4. Redirect sang VNPAY → màn cổng hiển thị **đúng mức giá cuối** (ví dụ sandbox NCB vẫn dùng chung `vnp_Amount` đã giảm).

## 4. Phương thức ngân hàng (NCB sandbox)

- Chỉ là **kênh thử nghiệm** khi thanh toán trên sandbox.
- **Không** có nhánh logic riêng theo ngân hàng cho giá hoặc voucher; mọi phương thức chia sẻ cùng **`vnp_Amount`** đã tính ở bước (3).

## 5. Liên kết mã nguồn

- Tạo URL & `vnp_Amount`: `com.hcmute.lovestream.service.vnpay.VnpayServiceImpl#createPaymentWithOrderCode`.
- Luồng mua gói hiện tại: `com.hcmute.lovestream.service.plan.ServicePlanServiceImpl#purchasePlan` (sau này khi có voucher, `Payment.amount` và `Vnpay.amount` phải khớp giá sau giảm).
