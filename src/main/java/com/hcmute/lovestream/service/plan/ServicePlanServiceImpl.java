package com.hcmute.lovestream.service.plan;

import com.hcmute.lovestream.dto.response.PurchaseResponse;
import com.hcmute.lovestream.dto.response.ServicePlanResponse;
import com.hcmute.lovestream.dto.request.Vnpay;
import com.hcmute.lovestream.entity.Payment;
import com.hcmute.lovestream.entity.ServicePlan;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.PaymentGateway;
import com.hcmute.lovestream.entity.enums.SubscriptionStatus;
import com.hcmute.lovestream.entity.enums.TransactionStatus;
import com.hcmute.lovestream.repository.PaymentRepository;
import com.hcmute.lovestream.repository.ServicePlanRepository;
import com.hcmute.lovestream.repository.SubscriptionRepository;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.service.vnpay.VnpayService;
import com.hcmute.lovestream.utils.VnpayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicePlanServiceImpl implements ServicePlanService {

    private final ServicePlanRepository servicePlanRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final VnpayService vnpayService;

    @Override
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable("activeServicePlans")
    public List<ServicePlanResponse> getAllActivePlans() {
        return servicePlanRepository.findByIsActiveTrueOrderByPriceAsc()
                .stream()
                .map(ServicePlanResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ServicePlanResponse getPlanById(String planId) {
        ServicePlan plan = servicePlanRepository.findByIdAndIsActiveTrue(planId)
                .orElseThrow(() -> new RuntimeException("Gói dịch vụ không tồn tại hoặc đã bị ẩn"));
        return new ServicePlanResponse(plan);
    }

    @Override
    public PurchaseResponse purchasePlan(String userEmail, String planId, HttpServletRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        ServicePlan plan = servicePlanRepository.findByIdAndIsActiveTrue(planId)
                .orElseThrow(() -> new RuntimeException("Gói dịch vụ không tồn tại hoặc đã bị ẩn"));

        // Tạo mã tham chiếu theo format số để gửi vnp_TxnRef
        String orderCode = VnpayUtil.getRandomNumber(10);

        // 1) Tạo Payment ở trạng thái chờ (Subscription sẽ được tạo trong callback VNPay)
        Payment payment = Payment.builder()
                .user(user)
                .servicePlan(plan)
                .amount(plan.getPrice())
                .paymentGateway(PaymentGateway.VNPAY)
                .status(TransactionStatus.PENDING)
                .transactionCode(orderCode)
                .build();
        paymentRepository.save(payment);

        // 2) Tạo paymentUrl và trả về cho frontend redirect sang VNPay
        Vnpay paymentRequest = Vnpay.builder()
                .amount(plan.getPrice().stripTrailingZeros().toPlainString())
                .orderInfo("Thanh toan goi: " + plan.getName())
                .orderId(orderCode)
                .build();

        String paymentUrl;
        try {
            paymentUrl = vnpayService.createPaymentWithOrderCode(paymentRequest, orderCode, request);
        } catch (Exception e) {
                        String detail = (e.getMessage() == null || e.getMessage().isBlank())
                                        ? "Lỗi không xác định"
                                        : e.getMessage();
                        throw new RuntimeException("Không thể tạo link thanh toán VNPay: " + detail, e);
        }

        return PurchaseResponse.builder()
                .message("Đang chuyển tới VNPay để thanh toán...")
                .planName(plan.getName())
                .startDate(null)
                .endDate(null)
                .paymentUrl(paymentUrl)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .map(user -> subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE))
                .orElse(false);
    }
}
