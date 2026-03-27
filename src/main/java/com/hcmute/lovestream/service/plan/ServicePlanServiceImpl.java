package com.hcmute.lovestream.service.plan;

import com.hcmute.lovestream.dto.response.PurchaseResponse;
import com.hcmute.lovestream.dto.response.ServicePlanResponse;
import com.hcmute.lovestream.dto.request.Vnpay;
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
import com.hcmute.lovestream.service.vnpay.VnpayService;
import com.hcmute.lovestream.utils.VnpayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicePlanServiceImpl implements ServicePlanService {

    private static final int DEFAULT_MAX_VIDEO_HEIGHT = 480;
    private static final Pattern HEIGHT_PATTERN = Pattern.compile("(\\d{3,4})");

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
    public List<ServicePlanResponse> getAvailableUpgradePlans(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Subscription activeSubscription = subscriptionRepository
                .findTopByUserAndStatusOrderByEndDateDesc(user, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Bạn chưa có gói dịch vụ đang hoạt động để nâng cấp."));

        return servicePlanRepository.findByIsActiveTrueOrderByPriceAsc()
                .stream()
                .filter(plan -> plan.getPrice().compareTo(activeSubscription.getPlan().getPrice()) > 0)
                .sorted(Comparator.comparing(ServicePlan::getPrice))
                .map(ServicePlanResponse::new)
                .collect(Collectors.toList());
    }

    @Override
    public PurchaseResponse upgradePlan(String userEmail, String targetPlanId, HttpServletRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Subscription activeSubscription = subscriptionRepository
                .findTopByUserAndStatusOrderByEndDateDesc(user, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Bạn chưa có gói dịch vụ đang hoạt động để nâng cấp."));

        ServicePlan currentPlan = activeSubscription.getPlan();
        ServicePlan targetPlan = servicePlanRepository.findByIdAndIsActiveTrue(targetPlanId)
                .orElseThrow(() -> new RuntimeException("Gói dịch vụ không tồn tại hoặc đã bị ẩn"));

        int comparePrice = targetPlan.getPrice().compareTo(currentPlan.getPrice());
        if (comparePrice == 0) {
            throw new RuntimeException("Bạn đang sử dụng gói này.");
        }
        if (comparePrice < 0) {
            throw new RuntimeException("Hiện tại hệ thống chưa hỗ trợ hạ cấp gói dịch vụ trong thời gian còn hiệu lực. Trong trường hợp hạ cấp, chúng tôi sẽ không hoàn lại phần phí chênh lệch. Vui lòng tiếp tục sử dụng gói hiện tại đến khi hết hạn.");
        }

        String orderCode = VnpayUtil.getRandomNumber(10);
        Payment payment = Payment.builder()
                .user(user)
                .servicePlan(targetPlan)
                .amount(targetPlan.getPrice().subtract(currentPlan.getPrice()))
                .paymentGateway(PaymentGateway.VNPAY)
                .status(TransactionStatus.PENDING)
                .transactionCode(orderCode)
                .build();
        paymentRepository.save(payment);

        Vnpay paymentRequest = Vnpay.builder()
                .amount(payment.getAmount().stripTrailingZeros().toPlainString())
                .orderInfo("Nang cap goi: " + currentPlan.getName() + " -> " + targetPlan.getName())
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
                .message("Đang chuyển tới VNPay để thanh toán phần chênh lệch...")
                .planName(targetPlan.getName())
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

    @Override
    @Transactional(readOnly = true)
    public int getMaxAllowedVideoHeight(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return DEFAULT_MAX_VIDEO_HEIGHT;
        }
        return findActiveSubscription(userEmail)
                .map(Subscription::getPlan)
                .map(ServicePlan::getResolution)
                .map(this::resolveHeightFromResolution)
                .orElse(DEFAULT_MAX_VIDEO_HEIGHT);
    }

    @Override
    @Transactional(readOnly = true)
    public String getCurrentPlanQualityLabel(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return "SD (480p)";
        }
        return findActiveSubscription(userEmail)
                .map(Subscription::getPlan)
                .map(plan -> buildQualityLabel(plan.getResolution()))
                .orElse("SD (480p)");
    }

    private Optional<Subscription> findActiveSubscription(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .flatMap(user -> subscriptionRepository.findTopByUserAndStatusOrderByEndDateDesc(user, SubscriptionStatus.ACTIVE));
    }

    private String buildQualityLabel(String resolution) {
        int height = resolveHeightFromResolution(resolution);
        if (height >= 2160) {
            return "4K (2160p)";
        }
        if (height >= 1440) {
            return "2K (1440p)";
        }
        if (height >= 1080) {
            return "Full HD (1080p)";
        }
        if (height >= 720) {
            return "HD (720p)";
        }
        return "SD (480p)";
    }

    private int resolveHeightFromResolution(String resolution) {
        if (resolution == null || resolution.isBlank()) {
            return DEFAULT_MAX_VIDEO_HEIGHT;
        }

        String normalized = resolution.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");

        if (normalized.contains("4k") || normalized.contains("2160")) {
            return 2160;
        }
        if (normalized.contains("2k") || normalized.contains("1440")) {
            return 1440;
        }
        if (normalized.contains("fullhd") || normalized.contains("fhd") || normalized.contains("1080")) {
            return 1080;
        }
        if (normalized.equals("hd") || normalized.contains("720")) {
            return 720;
        }
        if (normalized.equals("sd") || normalized.contains("480")) {
            return 480;
        }

        Matcher matcher = HEIGHT_PATTERN.matcher(normalized);
        if (matcher.find()) {
            int parsedHeight = Integer.parseInt(matcher.group(1));
            if (parsedHeight > 0) {
                return parsedHeight;
            }
        }

        return DEFAULT_MAX_VIDEO_HEIGHT;
    }
}
