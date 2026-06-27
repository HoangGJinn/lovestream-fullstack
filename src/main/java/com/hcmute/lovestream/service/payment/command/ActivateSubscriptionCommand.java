package com.hcmute.lovestream.service.payment.command;

import com.hcmute.lovestream.entity.Payment;
import com.hcmute.lovestream.entity.Subscription;
import com.hcmute.lovestream.entity.enums.SubscriptionStatus;
import com.hcmute.lovestream.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class ActivateSubscriptionCommand implements CompensatingCommand {

    private final SubscriptionRepository subscriptionRepository;
    private final Payment payment;

    // Lưu lại trạng thái để Undo
    private Subscription previousActiveSubscription;
    private LocalDateTime previousEndDate;
    private Subscription newlyCreatedSubscription;

    @Override
    public void execute() {
        previousActiveSubscription = subscriptionRepository
                .findTopByUserAndStatusOrderByEndDateDesc(payment.getUser(), SubscriptionStatus.ACTIVE)
                .orElse(null);

        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(payment.getServicePlan().getDurationDays());

        if (previousActiveSubscription == null) {
            // Mua mới
            Subscription subscription = Subscription.builder()
                    .user(payment.getUser())
                    .plan(payment.getServicePlan())
                    .startDate(startDate)
                    .endDate(endDate)
                    .status(SubscriptionStatus.ACTIVE)
                    .autoRenew(false)
                    .build();
            newlyCreatedSubscription = subscriptionRepository.save(subscription);
            log.info("[Command] Executed ActivateSubscriptionCommand: Created new subscription");
        } else {
            // Đã có gói
            int comparePlanPrice = payment.getServicePlan().getPrice()
                    .compareTo(previousActiveSubscription.getPlan().getPrice());

            // Nâng cấp: hủy gói cũ, tạo gói mới
            if (comparePlanPrice > 0) {
                previousEndDate = previousActiveSubscription.getEndDate(); // Save old end date to restore later
                
                previousActiveSubscription.setStatus(SubscriptionStatus.CANCELED);
                previousActiveSubscription.setEndDate(startDate);
                subscriptionRepository.save(previousActiveSubscription);

                Subscription upgradedSubscription = Subscription.builder()
                        .user(payment.getUser())
                        .plan(payment.getServicePlan())
                        .startDate(startDate)
                        .endDate(endDate)
                        .status(SubscriptionStatus.ACTIVE)
                        .autoRenew(false)
                        .build();
                newlyCreatedSubscription = subscriptionRepository.save(upgradedSubscription);
                log.info("[Command] Executed ActivateSubscriptionCommand: Upgraded subscription");
            } else {
                // Mua thêm ngày (Extend) - hiện tại code cũ chưa xử lý logic extend, nên coi như skip
                log.info("[Command] Executed ActivateSubscriptionCommand: Skip extend logic");
            }
        }
    }

    @Override
    public void undo() {
        if (newlyCreatedSubscription != null) {
            subscriptionRepository.delete(newlyCreatedSubscription);
            log.info("[Command] Undo ActivateSubscriptionCommand: Deleted newly created subscription");
        }

        if (previousActiveSubscription != null && previousEndDate != null) {
            previousActiveSubscription.setStatus(SubscriptionStatus.ACTIVE);
            previousActiveSubscription.setEndDate(previousEndDate);
            subscriptionRepository.save(previousActiveSubscription);
            log.info("[Command] Undo ActivateSubscriptionCommand: Restored previous active subscription");
        }
    }
}
