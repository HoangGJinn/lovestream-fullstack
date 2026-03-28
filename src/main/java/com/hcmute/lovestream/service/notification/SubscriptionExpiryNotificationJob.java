package com.hcmute.lovestream.service.notification;

import com.hcmute.lovestream.entity.Subscription;
import com.hcmute.lovestream.entity.enums.SubscriptionStatus;
import com.hcmute.lovestream.entity.enums.TypeNotification;
import com.hcmute.lovestream.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryNotificationJob {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<Integer> NOTICE_DAYS = List.of(7, 3, 1);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void sendUpcomingExpiryNotifications() {
        LocalDate today = LocalDate.now(ZONE_ID);

        for (int daysBeforeExpiry : NOTICE_DAYS) {
            LocalDate targetDate = today.plusDays(daysBeforeExpiry);
            LocalDateTime start = targetDate.atStartOfDay();
            LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

            List<Subscription> subscriptions = subscriptionRepository
                    .findByStatusAndEndDateGreaterThanEqualAndEndDateLessThan(
                            SubscriptionStatus.ACTIVE,
                            start,
                            end
                    );

            int sentCount = 0;
            for (Subscription subscription : subscriptions) {
                String dedupeKey = "SUB_EXPIRY:" + subscription.getId() + ":D" + daysBeforeExpiry;
                if (notificationService.createNotification(
                        subscription.getUser(),
                        TypeNotification.BILLING_SUBSCRIPTION,
                        "Goi dang ky sap het han",
                        buildContent(subscription, daysBeforeExpiry),
                        "/account/membership",
                        dedupeKey
                ) != null) {
                    sentCount++;
                }
            }

            if (sentCount > 0) {
                log.info("Created {} subscription expiry notifications for D{}", sentCount, daysBeforeExpiry);
            }
        }
    }

    private String buildContent(Subscription subscription, int daysBeforeExpiry) {
        String planName = subscription.getPlan() != null && subscription.getPlan().getName() != null
                ? subscription.getPlan().getName()
                : "goi hien tai";

        return "Goi " + planName
                + " cua ban se het han sau "
                + daysBeforeExpiry
                + " ngay, vao "
                + subscription.getEndDate().format(DATE_FORMATTER)
                + ". Vui long gia han de tranh gian doan xem phim.";
    }
}
