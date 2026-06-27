package com.hcmute.lovestream.service.vnpay;

import jakarta.servlet.http.HttpServletRequest;
import com.hcmute.lovestream.config.payment.VnpayConfig;
import com.hcmute.lovestream.dto.request.Vnpay;
import com.hcmute.lovestream.entity.Payment;
import com.hcmute.lovestream.entity.Subscription;
import com.hcmute.lovestream.entity.enums.SubscriptionStatus;
import com.hcmute.lovestream.entity.enums.TransactionStatus;
import com.hcmute.lovestream.repository.PaymentRepository;
import com.hcmute.lovestream.repository.SubscriptionRepository;
import com.hcmute.lovestream.repository.VoucherRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hcmute.lovestream.util.VnpayUtil;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Tích hợp thanh toán VNPAY.
 * <p>
 * <strong>Voucher / khuyến mãi:</strong> Giao diện trên {@code sandbox.vnpayment.vn} không tùy biến được;
 * mọi áp dụng voucher phải xử lý trên LoveStream <em>trước</em> khi gọi
 * {@link #createPaymentWithOrderCode}. Tham số {@code vnp_Amount} được khóa tại thời điểm tạo URL;
 * {@link Vnpay#getAmount()} phải là số tiền cuối (VND) sau giảm, khớp {@code Payment.amount}.
 * Chi tiết kiến trúc: {@code com/hcmute/lovestream/docs/Nhan/Voucher_Payment/01-vnpay-voucher-architecture.md}.
 */
@Service
@Slf4j
public class VnpayServiceImpl implements VnpayService {

    @Autowired
    private VnpayConfig vnpayConfig;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Override
    public String createPayment(Vnpay paymentRequest, HttpServletRequest request) throws UnsupportedEncodingException {
        // VNPay dùng vnp_TxnRef để định danh giao dịch.
        // Ở project này: vnp_TxnRef = Payment.id
        return createPaymentWithOrderCode(paymentRequest, paymentRequest.getOrderId(), request);
    }

    /**
     * Tạo URL redirect VNPAY. {@code paymentRequest.amount} là số tiền (VND) khách trả; đã phải là giá sau voucher
     * (nếu có) — không thể bổ sung giảm giá trên trang VNPAY.
     */
    @Override
    public String createPaymentWithOrderCode(Vnpay paymentRequest, String orderCode, HttpServletRequest request)
            throws UnsupportedEncodingException {

        // Lấy giá trị trực tiếp trong hàm để đảm bảo Config đã được load
        String vnp_Version = vnpayConfig.getVnp_Version();
        String vnp_Command = vnpayConfig.getVnp_Command();
        String vnp_TmnCode = vnpayConfig.getVnp_TmnCode();
        String vnp_OrderType = vnpayConfig.getOrderType();
        String vnp_ReturnUrl = vnpayConfig.getVnp_ReturnUrl();

        if (isBlank(vnp_Version) || isBlank(vnp_Command) || isBlank(vnp_TmnCode)
                || isBlank(vnp_OrderType) || isBlank(vnp_ReturnUrl) || isBlank(vnpayConfig.getSecretKey())
                || isBlank(vnpayConfig.getVnp_PayUrl())) {
            throw new IllegalStateException("Thiếu cấu hình VNPay trong application.properties");
        }

        if (isBlank(orderCode)) {
            throw new IllegalArgumentException("Thiếu mã tham chiếu thanh toán");
        }

        long amount;
        try {
            BigDecimal amountValue = new BigDecimal(paymentRequest.getAmount());
            if (amountValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
            }

            // VNPay yêu cầu amount ở đơn vị nhỏ nhất (x100)
            amount = amountValue
                    .multiply(BigDecimal.valueOf(100L))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số tiền không hợp lệ");
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Số tiền vượt giới hạn hỗ trợ");
        }

        String vnp_IpAddr = VnpayUtil.getIpAddress(request);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        // vnp_Params.put("vnp_BankCode", "NCB");
        vnp_Params.put("vnp_TxnRef", orderCode); // Gửi orderCode nhưng key vẫn là vnp_TxnRef
        vnp_Params.put("vnp_OrderInfo", paymentRequest.getOrderInfo() != null ? paymentRequest.getOrderInfo()
                : "Thanh toan don hang:" + orderCode);
        vnp_Params.put("vnp_OrderType", vnp_OrderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        TimeZone tz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        Calendar cld = Calendar.getInstance(tz);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(tz);
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (Iterator<String> itr = fieldNames.iterator(); itr.hasNext();) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                // Build hash data
                hashData.append(fieldName).append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String vnp_SecureHash = VnpayUtil.hmacSHA512(vnpayConfig.getSecretKey(), hashData.toString());
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);
        return vnpayConfig.getVnp_PayUrl() + "?" + query;
    }

    @Override
    @Transactional
    public String handlePaymentCallback(HttpServletRequest request) {
        try {
            // Lấy các tham số từ VNPAY
            Map<String, String> vnpParams = new HashMap<>();
            Enumeration<String> params = request.getParameterNames();
            while (params.hasMoreElements()) {
                String paramName = params.nextElement();
                String paramValue = request.getParameter(paramName);
                if (paramValue != null && !paramValue.isEmpty()) {
                    vnpParams.put(paramName, paramValue);
                }
            }

            // Lấy các thông tin quan trọng
            String vnp_ResponseCode = vnpParams.get("vnp_ResponseCode");
            String vnp_TxnRef = vnpParams.get("vnp_TxnRef");
            String vnp_TransactionNo = vnpParams.get("vnp_TransactionNo");
            String vnp_SecureHash = vnpParams.get("vnp_SecureHash");
            String vnp_Amount = vnpParams.get("vnp_Amount");

            // Xóa vnp_SecureHash khỏi params để verify
            vnpParams.remove("vnp_SecureHash");
            vnpParams.remove("vnp_SecureHashType");

            // Verify signature
            if (isBlank(vnp_SecureHash) || !verifySignature(vnpParams, vnp_SecureHash)) {
                log.warn("VNPay callback signature invalid. txnRef={} transactionNo={}", vnp_TxnRef, vnp_TransactionNo);
                return null; // Signature không hợp lệ
            }

            // Tìm Payment theo mã tham chiếu merchant đã gửi đi hoặc mã giao dịch VNPay
            Payment payment = findPaymentForCallback(vnp_TxnRef, vnp_TransactionNo);
            if (payment == null) {
                log.warn("VNPay callback payment not found. txnRef={} transactionNo={}", vnp_TxnRef, vnp_TransactionNo);
                return null;
            }

            // Verify amount
            if (!isBlank(vnp_Amount)) {
                try {
                    long vnpAmountLong = Long.parseLong(vnp_Amount);
                    long expectedAmount = payment.getAmount()
                            .multiply(BigDecimal.valueOf(100L))
                            .setScale(0, RoundingMode.HALF_UP)
                            .longValueExact();
                    if (vnpAmountLong != expectedAmount) {
                        log.warn("VNPay callback amount mismatch. txnRef={} vnp_Amount={} expectedAmount={}", vnp_TxnRef, vnpAmountLong, expectedAmount);
                        return null;
                    }
                } catch (NumberFormatException e) {
                    log.warn("VNPay callback vnp_Amount invalid format. txnRef={}", vnp_TxnRef);
                    return null;
                }
            }

            boolean isSuccess = "00".equals(vnp_ResponseCode);

            if (payment.getStatus() == TransactionStatus.SUCCESS) {
                return payment.getId();
            }

            if (!isSuccess) {
                // Thanh toán thất bại, chỉ cần cập nhật trạng thái
                payment.setStatus(TransactionStatus.FAILED);
                paymentRepository.saveAndFlush(payment);
                return null;
            }

            // ==========================================
            // ÁP DỤNG COMPENSATING COMMAND (SAGA PATTERN)
            // ==========================================
            
            // 1. Khởi tạo danh sách các Command cần chạy
            List<com.hcmute.lovestream.service.payment.command.CompensatingCommand> commands = new ArrayList<>();
            
            commands.add(new com.hcmute.lovestream.service.payment.command.UpdatePaymentStatusCommand(
                    paymentRepository, payment, TransactionStatus.SUCCESS, vnp_TransactionNo));
                    
            if (payment.getVoucher() != null) {
                commands.add(new com.hcmute.lovestream.service.payment.command.UseVoucherCommand(
                        voucherRepository, payment.getVoucher().getId()));
            }
            
            commands.add(new com.hcmute.lovestream.service.payment.command.ActivateSubscriptionCommand(
                    subscriptionRepository, payment));

            // 2. Bộ điều phối (Invoker) chạy các Command
            Stack<com.hcmute.lovestream.service.payment.command.CompensatingCommand> executedCommands = new Stack<>();
            
            try {
                for (com.hcmute.lovestream.service.payment.command.CompensatingCommand command : commands) {
                    command.execute();
                    executedCommands.push(command); // Đẩy vào stack nếu thành công
                }
            } catch (Exception ex) {
                log.error("Lỗi khi thực thi chuỗi giao dịch thanh toán. Đang tiến hành Rollback (Undo)...", ex);
                
                // Chạy lùi (LIFO) để hoàn tác
                while (!executedCommands.isEmpty()) {
                    try {
                        com.hcmute.lovestream.service.payment.command.CompensatingCommand cmd = executedCommands.pop();
                        cmd.undo();
                    } catch (Exception undoEx) {
                        log.error("LỖI NGHIÊM TRỌNG: Quá trình Undo cũng bị lỗi!", undoEx);
                        // Ở hệ thống thực tế cần gửi cảnh báo cho Dev/Admin để can thiệp bằng tay
                    }
                }
                
                return null; // Báo lỗi callback
            }
            // ==========================================

            // Trả về paymentId nếu thành công, ngược lại trả về null.
            return isSuccess ? payment.getId() : null;

        } catch (Exception e) {
            log.error("VNPay callback processing failed", e);
            return null;
        }
    }

    private Payment findPaymentForCallback(String txnRef, String transactionNo) {
        if (!isBlank(txnRef)) {
            Payment paymentByRef = paymentRepository.findByTransactionCode(txnRef)
                    .orElseGet(() -> paymentRepository.findById(txnRef).orElse(null));
            if (paymentByRef != null) {
                return paymentByRef;
            }
        }

        if (!isBlank(transactionNo) && !"0".equals(transactionNo)) {
            return paymentRepository.findByTransactionCode(transactionNo).orElse(null);
        }

        return null;
    }

    private void assignGatewayTransactionCode(Payment payment, String transactionNo) {
        if (isBlank(transactionNo) || "0".equals(transactionNo)) {
            return;
        }

        if (transactionNo.equals(payment.getTransactionCode())) {
            return;
        }

        if (paymentRepository.existsByTransactionCode(transactionNo)) {
            log.warn("Skip duplicate VNPay transactionCode update. paymentId={} transactionNo={}", payment.getId(),
                    transactionNo);
            return;
        }

        payment.setTransactionCode(transactionNo);
    }

    private boolean verifySignature(Map<String, String> vnpParams, String vnp_SecureHash) {
        try {
            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);

            // Build hash data
            StringBuilder hashData = new StringBuilder();
            for (String fieldName : fieldNames) {
                String fieldValue = vnpParams.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append('=')
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    hashData.append('&');
                }
            }
            if (!hashData.isEmpty()) {
                hashData.setLength(hashData.length() - 1);
            }

            // Tính toán hash
            String calculatedHash = VnpayUtil.hmacSHA512(vnpayConfig.getSecretKey(), hashData.toString());

            // So sánh với hash từ VNPAY
            return calculatedHash.equals(vnp_SecureHash);
        } catch (Exception e) {
            log.error("VNPay signature verification failed", e);
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}