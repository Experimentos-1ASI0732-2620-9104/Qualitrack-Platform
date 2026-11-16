package com.iotech.qualitrack.platform.subscription.domain.model.entities;

import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.Money;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PaymentProvider;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PaymentStatus;
import lombok.Getter;

import java.util.Objects;

/**
 * Domain entity representing a subscription payment transaction.
 */
@Getter
public class SubscriptionPayment {

    private Long id;
    private Long subscriptionId;
    private PaymentProvider provider;
    private String providerPaymentId;
    private String stripeCheckoutSessionId;
    private Money amount;
    private PaymentStatus status;
    private String paidAt;

    /**
     * Default constructor.
     */
    public SubscriptionPayment() {
        // Required for reconstruction
    }

    /**
     * Reconstructs a subscription payment.
     */
    public SubscriptionPayment(
            Long id,
            Long subscriptionId,
            PaymentProvider provider,
            String providerPaymentId,
            String stripeCheckoutSessionId,
            Money amount,
            PaymentStatus status,
            String paidAt
    ) {
        this.id = id;
        this.subscriptionId = Objects.requireNonNull(subscriptionId, "Subscription id is required");
        this.provider = Objects.requireNonNull(provider, "Payment provider is required");
        this.providerPaymentId = providerPaymentId;
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
        this.amount = Objects.requireNonNull(amount, "Amount is required");
        this.status = Objects.requireNonNull(status, "Payment status is required");
        this.paidAt = paidAt;
    }

    /**
     * Creates a new Stripe payment record.
     */
    public static SubscriptionPayment stripePayment(
            Long subscriptionId,
            String providerPaymentId,
            String stripeCheckoutSessionId,
            Money amount,
            PaymentStatus status,
            String paidAt
    ) {
        return new SubscriptionPayment(
                null,
                subscriptionId,
                PaymentProvider.STRIPE,
                providerPaymentId,
                stripeCheckoutSessionId,
                amount,
                status,
                paidAt
        );
    }

    /**
     * Indicates whether this payment is completed.
     *
     * @return true if the payment is paid
     */
    public boolean isPaid() {
        return PaymentStatus.PAID.equals(status);
    }
}