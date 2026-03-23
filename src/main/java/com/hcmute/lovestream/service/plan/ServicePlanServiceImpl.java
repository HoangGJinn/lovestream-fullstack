package com.hcmute.lovestream.service.plan;

import com.hcmute.lovestream.dto.response.PurchaseResponse;
import com.hcmute.lovestream.dto.response.ServicePlanResponse;
import com.hcmute.lovestream.entity.Payment;
import com.hcmute.lovestream.entity.ServicePlan;
import com.hcmute.lovestream.entity.Subscription;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.PaymentGateway;
import com.hcmute.lovestream.entity.enums.SubscriptionStatus;
import com.hcmute.lovestream.entity.enums.TransactionStatus;
import com.hcmute.lovestream.repository.PaymentRepository;
import com.hcmute.lovestream.repository.ServicePlanRepository;
import com.hcmute.lovestream.repository.SubscriptionRepository;
import com.hcmute.lovestream.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicePlanServiceImpl implements ServicePlanService {

    private final ServicePlanRepository servicePlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
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
    @Transactional
    public PurchaseResponse purchasePlan(String userEmail, String planId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        ServicePlan plan = servicePlanRepository.findByIdAndIsActiveTrue(planId)
                .orElseThrow(() -> new RuntimeException("Gói dịch vụ không tồn tại hoặc đã bị ẩn"));

        // Tính thời hạn subscription
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate   = startDate.plusDays(plan.getDurationDays());

        // Tạo Payment (giả lập thành công ngay lập tức)
        Payment payment = Payment.builder()
                .user(user)
                .servicePlan(plan)
                .amount(plan.getPrice())
                .paymentGateway(PaymentGateway.VNPAY)
                .status(TransactionStatus.SUCCESS)
                .transactionCode(UUID.randomUUID().toString())
                .build();
        paymentRepository.save(payment);

        // Tạo Subscription
        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .startDate(startDate)
                .endDate(endDate)
                .status(SubscriptionStatus.ACTIVE)
                .autoRenew(false)
                .build();
        subscriptionRepository.save(subscription);

        return PurchaseResponse.builder()
                .message("Đăng ký gói \"" + plan.getName() + "\" thành công!")
                .planName(plan.getName())
                .startDate(startDate)
                .endDate(endDate)
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
