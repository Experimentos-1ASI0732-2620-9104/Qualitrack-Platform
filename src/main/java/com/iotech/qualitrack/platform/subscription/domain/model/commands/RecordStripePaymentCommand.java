package com.iotech.qualitrack.platform.subscription.domain.model.commands;

import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PaymentStatus;

import java.math.BigDecimal;

/**
 * Command used to record a Stripe payment transaction: the first payment, made through a checkout session, or the
 * payment of a renewal, which Stripe charges through an invoice without a checkout session.
 *
 * @param subscriptionId The internal subscription identifier.
 * @param providerPaymentId Stripe identifier of the payment: the payment intent of a checkout, when Stripe gives one,
 *                          or the invoice of a renewal.
 * @param stripeCheckoutSessionId Stripe checkout session identifier; null for a renewal.
 * @param amount The paid amount.
 * @param currency The ISO currency code.
 * @param status The payment status.
 * @param paidAt The payment timestamp.
 */
public record RecordStripePaymentCommand(
        Long subscriptionId,
        String providerPaymentId,
        String stripeCheckoutSessionId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String paidAt
) {

    public RecordStripePaymentCommand {
        if (subscriptionId == null || subscriptionId <= 0) {
            throw new IllegalArgumentException("Subscription id must be a positive number");
        }
        if (isBlank(stripeCheckoutSessionId) && isBlank(providerPaymentId)) {
            throw new IllegalArgumentException("Stripe checkout session id or provider payment id is required");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("Payment status is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}