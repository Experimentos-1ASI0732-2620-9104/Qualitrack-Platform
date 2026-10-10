package com.iotech.qualitrack.platform.subscription.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.subscription.domain.exceptions.StripeCheckoutSessionCreationException;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.CreateCheckoutSessionCommand;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * ACL service used by the Subscription bounded context to interact with Stripe.
 *
 * <p>This service isolates Stripe SDK details from the subscription domain and
 * application services.</p>
 */
@Service
public class ExternalStripeService {

    private final String stripeSecretKey;
    private final String frontendUrl;

    public ExternalStripeService(@Value("${stripe.secret-key}") String stripeSecretKey,
                                 @Value("${application.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.stripeSecretKey = stripeSecretKey;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Creates a real Stripe Checkout Session for a subscription plan.
     *
     * @param command checkout command containing user, laboratory, plan and redirect data
     * @param stripePriceId Stripe Price ID configured for the selected plan
     * @return checkout session identifier and hosted checkout URL
     */
    public StripeCheckoutSessionResult createCheckoutSession(
            CreateCheckoutSessionCommand command,
            String stripePriceId
    ) {
        validateReturnUrl(command.successUrl(), "/subscriptions/success");
        validateReturnUrl(command.cancelUrl(), "/subscriptions/cancel");
        if (stripeSecretKey.isBlank()) throw new IllegalStateException("Stripe checkout is not configured");
        try {
            var metadata = new java.util.HashMap<String, String>();
            metadata.put("userId", command.userId().toString());
            metadata.put("planCode", command.planCode().name());
            metadata.put("billingCycle", command.billingCycle().name());
            if (command.laboratoryId() != null) metadata.put("laboratoryId", command.laboratoryId().toString());
            var params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setSuccessUrl(appendCheckoutSessionPlaceholder(command.successUrl()))
                    .setCancelUrl(command.cancelUrl())
                    .setClientReferenceId(String.valueOf(command.userId()))
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(stripePriceId)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .putAllMetadata(metadata)
                    .setSubscriptionData(SessionCreateParams.SubscriptionData.builder().putAllMetadata(metadata).build())
                    .build();

            var session = Session.create(params, requestOptions());

            return new StripeCheckoutSessionResult(session.getId(), session.getUrl());

        } catch (StripeException e) {
            throw new StripeCheckoutSessionCreationException(e.getMessage());
        }
    }

    /**
     * Retrieves Stripe subscription details needed to activate a local subscription.
     *
     * @param stripeSubscriptionId Stripe subscription identifier
     * @return Stripe subscription data used by the subscription BC
     */
    public StripeSubscriptionDetails retrieveSubscriptionDetails(String stripeSubscriptionId) {
        try {
            var subscription = Subscription.retrieve(stripeSubscriptionId, requestOptions());

            var periodStart = getCurrentPeriodStart(subscription);
            var periodEnd = getCurrentPeriodEnd(subscription);

            return new StripeSubscriptionDetails(
                    subscription.getCustomer(),
                    subscription.getId(),
                    toIsoDateTime(periodStart),
                    toIsoDateTime(periodEnd),
                    subscription.getStatus(),
                    Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd())
            );

        } catch (StripeException e) {
            throw new IllegalStateException("Failed to retrieve Stripe subscription: " + e.getMessage(), e);
        }
    }

    /**
     * Cancels the renewal of a Stripe subscription: Stripe ends it when the current period ends (cancel_at_period_end).
     *
     * @param stripeSubscriptionId Stripe subscription identifier
     */
    public void cancelRenewal(String stripeSubscriptionId) {
        try {
            var subscription = Subscription.retrieve(stripeSubscriptionId, requestOptions());
            subscription.update(SubscriptionUpdateParams.builder().setCancelAtPeriodEnd(true).build(), requestOptions());
        } catch (StripeException e) {
            throw new IllegalStateException("Failed to cancel the renewal of the Stripe subscription: " + e.getMessage(), e);
        }
    }

    private RequestOptions requestOptions() {
        return RequestOptions.builder()
                .setApiKey(stripeSecretKey)
                .build();
    }

    private void validateReturnUrl(String value, String path) {
        var expected = java.net.URI.create(frontendUrl);
        var actual = java.net.URI.create(value);
        if (!java.util.Objects.equals(expected.getScheme(), actual.getScheme())
                || !java.util.Objects.equals(expected.getAuthority(), actual.getAuthority())
                || !path.equals(actual.getPath()) || actual.getUserInfo() != null
                || actual.getFragment() != null) {
            throw new IllegalArgumentException("Checkout return URL must belong to the configured frontend");
        }
    }

    private String appendCheckoutSessionPlaceholder(String successUrl) {
        if (successUrl.contains("{CHECKOUT_SESSION_ID}")) {
            return successUrl;
        }

        var separator = successUrl.contains("?") ? "&" : "?";
        return successUrl + separator + "session_id={CHECKOUT_SESSION_ID}";
    }

    private Long getCurrentPeriodStart(Subscription subscription) {
        if (subscription.getItems() == null
                || subscription.getItems().getData() == null
                || subscription.getItems().getData().isEmpty()) {
            return null;
        }

        return subscription.getItems().getData().getFirst().getCurrentPeriodStart();
    }

    private Long getCurrentPeriodEnd(Subscription subscription) {
        if (subscription.getItems() == null
                || subscription.getItems().getData() == null
                || subscription.getItems().getData().isEmpty()) {
            return null;
        }

        return subscription.getItems().getData().getFirst().getCurrentPeriodEnd();
    }

    private String toIsoDateTime(Long epochSeconds) {
        if (epochSeconds == null) return null;

        return Instant.ofEpochSecond(epochSeconds)
                .atOffset(ZoneOffset.UTC)
                .toString();
    }
}
