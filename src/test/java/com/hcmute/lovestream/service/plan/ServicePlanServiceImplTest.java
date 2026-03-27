package com.hcmute.lovestream.service.plan;

import com.hcmute.lovestream.entity.ServicePlan;
import com.hcmute.lovestream.entity.Subscription;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.SubscriptionStatus;
import com.hcmute.lovestream.repository.PaymentRepository;
import com.hcmute.lovestream.repository.ServicePlanRepository;
import com.hcmute.lovestream.repository.SubscriptionRepository;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.service.vnpay.VnpayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicePlanServiceImplTest {

    @Mock
    private ServicePlanRepository servicePlanRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VnpayService vnpayService;

    @InjectMocks
    private ServicePlanServiceImpl servicePlanService;

    @Test
    void getMaxAllowedVideoHeight_shouldMapHdPlanTo720p() {
        User user = User.builder().id("u1").email("hd@demo.local").build();
        ServicePlan plan = ServicePlan.builder().id("p1").name("HD").resolution("HD").build();
        Subscription subscription = Subscription.builder().id("s1").user(user).plan(plan).status(SubscriptionStatus.ACTIVE).build();

        when(userRepository.findByEmail("hd@demo.local")).thenReturn(Optional.of(user));
        when(subscriptionRepository.findTopByUserAndStatusOrderByEndDateDesc(user, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));

        assertEquals(720, servicePlanService.getMaxAllowedVideoHeight("hd@demo.local"));
        assertEquals("HD (720p)", servicePlanService.getCurrentPlanQualityLabel("hd@demo.local"));
    }

    @Test
    void getMaxAllowedVideoHeight_shouldMapFullHdPlanTo1080p() {
        User user = User.builder().id("u2").email("fhd@demo.local").build();
        ServicePlan plan = ServicePlan.builder().id("p2").name("Full HD").resolution("Full HD").build();
        Subscription subscription = Subscription.builder().id("s2").user(user).plan(plan).status(SubscriptionStatus.ACTIVE).build();

        when(userRepository.findByEmail("fhd@demo.local")).thenReturn(Optional.of(user));
        when(subscriptionRepository.findTopByUserAndStatusOrderByEndDateDesc(user, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));

        assertEquals(1080, servicePlanService.getMaxAllowedVideoHeight("fhd@demo.local"));
        assertEquals("Full HD (1080p)", servicePlanService.getCurrentPlanQualityLabel("fhd@demo.local"));
    }

    @Test
    void getMaxAllowedVideoHeight_shouldFallbackToDefaultWhenNoActiveSubscription() {
        when(userRepository.findByEmail("nosub@demo.local")).thenReturn(Optional.empty());

        assertEquals(480, servicePlanService.getMaxAllowedVideoHeight("nosub@demo.local"));
        assertEquals("SD (480p)", servicePlanService.getCurrentPlanQualityLabel("nosub@demo.local"));
        assertEquals(480, servicePlanService.getMaxAllowedVideoHeight(null));
    }
}

