package com.iotech.qualitrack.platform.subscription.interfaces.rest;

import com.iotech.qualitrack.platform.subscription.application.internal.commandservices.StripeWebhookService;
import com.iotech.qualitrack.platform.subscription.interfaces.rest.resources.StripeWebhookResource;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/stripe/webhooks", produces = "application/json")
public class StripeWebhookController {
    private final String secret;
    private final StripeWebhookService service;

    public StripeWebhookController(@Value("${stripe.webhook-secret}") String secret, StripeWebhookService service) {
        this.secret = secret;
        this.service = service;
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Process Stripe webhook",
            description = "Requires Stripe-Signature over the unmodified body. Only paid checkout sessions "
                    + "with an active Stripe subscription grant access. Failures return 500 for provider retry.")
    public ResponseEntity<StripeWebhookResource> handleStripeWebhook(
            @RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {
        if (secret.isBlank()) return ResponseEntity.status(503).body(new StripeWebhookResource("not-configured"));
        com.stripe.model.Event event;
        try {
            event = Webhook.constructEvent(payload, signature, secret);
        } catch (SignatureVerificationException | RuntimeException exception) {
            return ResponseEntity.badRequest().body(new StripeWebhookResource("invalid-event"));
        }
        try {
            var type = event.getType();
            if ("checkout.session.completed".equals(type) || "checkout.session.async_payment_succeeded".equals(type)) {
                var object = event.getDataObjectDeserializer().deserializeUnsafe();
                if (!(object instanceof Session session)) throw new IllegalArgumentException("Invalid checkout payload");
                service.fulfill(session);
            } else if ("customer.subscription.updated".equals(type) || "customer.subscription.deleted".equals(type)) {
                var object = event.getDataObjectDeserializer().deserializeUnsafe();
                if (object instanceof com.stripe.model.Subscription subscription) service.synchronize(subscription.getId());
            }
            return ResponseEntity.ok(new StripeWebhookResource("processed"));
        } catch (Exception exception) {
            org.slf4j.LoggerFactory.getLogger(StripeWebhookController.class)
                    .error("Stripe event {} could not be fulfilled: {}", event.getId(), exception.getClass().getSimpleName());
            return ResponseEntity.status(500).body(new StripeWebhookResource("processing-failed"));
        }
    }
}
