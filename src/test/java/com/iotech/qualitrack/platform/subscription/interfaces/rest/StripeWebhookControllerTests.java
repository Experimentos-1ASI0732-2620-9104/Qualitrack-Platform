package com.iotech.qualitrack.platform.subscription.interfaces.rest;

import com.iotech.qualitrack.platform.subscription.application.internal.commandservices.StripeWebhookService;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Events that Stripe signs with the secret of the destination reach the service that handles their type. */
class StripeWebhookControllerTests {
    private static final String SECRET = "whsec_controller_test";

    private final StripeWebhookService service = mock(StripeWebhookService.class);
    private final StripeWebhookController controller = new StripeWebhookController(SECRET, service);

    @Test void paidInvoiceRecordsTheRenewal() throws Exception {
        var payload = """
                {"id":"evt_renewal","object":"event","api_version":"2025-08-27.basil","type":"invoice.paid",
                 "data":{"object":{"id":"in_renewal","object":"invoice","billing_reason":"subscription_cycle",
                 "status":"paid","amount_paid":19900,"currency":"usd",
                 "parent":{"type":"subscription_details","subscription_details":{"subscription":"sub_test"}},
                 "status_transitions":{"paid_at":1793836290}}}}""";

        var response = controller.handleStripeWebhook(payload, signature(payload));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(service).recordRenewalPayment(argThat(invoice -> "in_renewal".equals(invoice.getId())
                && "sub_test".equals(invoice.getParent().getSubscriptionDetails().getSubscription())));
    }

    @Test void eventsWithoutAValidSignatureAreRefused() {
        var payload = "{\"id\":\"evt_renewal\",\"object\":\"event\",\"type\":\"invoice.paid\",\"data\":{\"object\":{}}}";

        var response = controller.handleStripeWebhook(payload, "t=1,v1=invalid");

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        verifyNoInteractions(service);
    }

    private static String signature(String payload) throws Exception {
        var timestamp = Instant.now().getEpochSecond();
        return "t=" + timestamp + ",v1=" + Webhook.Util.computeHmacSha256(SECRET, timestamp + "." + payload);
    }
}
