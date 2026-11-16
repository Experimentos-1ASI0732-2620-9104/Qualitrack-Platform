package com.iotech.qualitrack.platform.subscription.application;

import com.iotech.qualitrack.platform.subscription.application.internal.commandservices.SubscriptionCommandServiceImpl;
import com.iotech.qualitrack.platform.subscription.application.internal.outboundservices.acl.ExternalStripeService;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.RecordStripePaymentCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.entities.SubscriptionPayment;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PaymentStatus;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionPaymentRepository;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionPlanRepository;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Recording of the first payment of a subscription, made through a checkout, and of the payments of its renewals. */
class SubscriptionPaymentRecordingTests {
    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final SubscriptionPaymentRepository payments = mock(SubscriptionPaymentRepository.class);
    private final SubscriptionCommandServiceImpl service = new SubscriptionCommandServiceImpl(
            mock(SubscriptionPlanRepository.class), subscriptions, payments, mock(ExternalStripeService.class),
            mock(ApplicationEventPublisher.class));

    @Test void aPaymentNeedsACheckoutSessionOrAProviderPayment() {
        assertThatThrownBy(() -> renewal(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("checkout session id or provider payment id");
        assertThat(renewal("in_renewal").stripeCheckoutSessionId()).isNull();
    }

    @Test void aRenewalIsRecordedNextToTheOtherPaymentsWithoutCheckoutSession() {
        when(subscriptions.existsById(73L)).thenReturn(true);
        when(payments.findByProviderPaymentId("in_renewal")).thenReturn(Optional.empty());
        when(payments.save(any())).thenAnswer(invocation -> {
            SubscriptionPayment payment = invocation.getArgument(0);
            return new SubscriptionPayment(82L, payment.getSubscriptionId(), payment.getProvider(),
                    payment.getProviderPaymentId(), payment.getStripeCheckoutSessionId(), payment.getAmount(),
                    payment.getStatus(), payment.getPaidAt());
        });

        var result = service.handle(renewal("in_renewal"));

        assertThat(result.isSuccess()).isTrue();
        verify(payments, never()).findByStripeCheckoutSessionId(any());
        verify(payments).save(argThat(payment -> "in_renewal".equals(payment.getProviderPaymentId())
                && payment.getStripeCheckoutSessionId() == null));
    }

    @Test void theSameInvoiceIsNotRecordedTwice() {
        when(subscriptions.existsById(73L)).thenReturn(true);
        when(payments.findByProviderPaymentId("in_renewal")).thenReturn(Optional.of(mock(SubscriptionPayment.class)));

        assertThat(service.handle(renewal("in_renewal")).isSuccess()).isFalse();
        verify(payments, never()).save(any());
    }

    private static RecordStripePaymentCommand renewal(String invoiceId) {
        return new RecordStripePaymentCommand(73L, invoiceId, null, new BigDecimal("199.00"), "USD",
                PaymentStatus.PAID, "2026-11-04T23:51:30Z");
    }
}
