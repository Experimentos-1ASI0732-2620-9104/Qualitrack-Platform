package com.iotech.qualitrack.platform;

import com.iotech.qualitrack.platform.iam.interfaces.acl.IamContextFacade;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.subscription.application.commandservices.SubscriptionCommandService;
import com.iotech.qualitrack.platform.subscription.application.internal.commandservices.StripeWebhookService;
import com.iotech.qualitrack.platform.subscription.application.internal.outboundservices.acl.*;
import com.iotech.qualitrack.platform.subscription.domain.model.aggregates.Subscription;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.*;
import com.iotech.qualitrack.platform.subscription.domain.repositories.*;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class StripeWebhookTests {
    private final SubscriptionCommandService commands = mock(SubscriptionCommandService.class);
    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final SubscriptionPaymentRepository payments = mock(SubscriptionPaymentRepository.class);
    private final ExternalStripeService stripe = mock(ExternalStripeService.class);
    private final IamContextFacade users = mock(IamContextFacade.class);
    private final StripeWebhookService service = new StripeWebhookService(commands, subscriptions, payments, stripe, users);

    @Test void unpaidCheckoutNeverGrantsAccess() {
        var session = session();
        session.setPaymentStatus("unpaid");
        service.fulfill(session);
        verifyNoInteractions(commands, users, stripe);
    }

    @Test void paidCheckoutWaitsForActiveStripeSubscription() {
        when(stripe.retrieveSubscriptionDetails("sub_test")).thenReturn(details("incomplete"));
        service.fulfill(session());
        verifyNoInteractions(commands);
    }

    @Test void activePaidCheckoutPersistsSubscriptionAndPaymentWithoutLaboratory() {
        when(users.lockLaboratoryAssociation(27L)).thenReturn(null);
        when(stripe.retrieveSubscriptionDetails("sub_test")).thenReturn(details("active"));
        when(commands.handle(any(ActivateSubscriptionCommand.class))).thenReturn(Result.success(73L));
        when(commands.handle(any(RecordStripePaymentCommand.class))).thenReturn(Result.success(81L));
        service.fulfill(session());
        verify(commands).handle(argThat((ActivateSubscriptionCommand command) -> command.laboratoryId() == null
                && command.userId().equals(27L) && command.stripeCheckoutSessionId().equals("cs_test")));
        verify(commands).handle(argThat((RecordStripePaymentCommand command) -> command.subscriptionId().equals(73L)
                && command.amount().compareTo(new java.math.BigDecimal("12.00")) == 0));
    }

    @Test void repeatedWebhookDoesNotReactivateOrDuplicatePayments() {
        when(subscriptions.findByStripeCheckoutSessionId("cs_test")).thenReturn(Optional.of(mock(Subscription.class)));
        when(payments.findByStripeCheckoutSessionId("cs_test")).thenReturn(Optional.of(
                mock(com.iotech.qualitrack.platform.subscription.domain.model.entities.SubscriptionPayment.class)));
        service.fulfill(session());
        verifyNoInteractions(commands, stripe);
    }

    @Test void forgedLaboratoryMetadataIsRejected() {
        when(users.lockLaboratoryAssociation(27L)).thenReturn(42L);
        var session = session();
        session.setMetadata(Map.of("userId", "27", "laboratoryId", "999"));
        assertThatThrownBy(() -> service.fulfill(session)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(commands, stripe);
    }

    @Test void returnUrlsCannotRedirectToOtherOrigins() {
        var gateway = new ExternalStripeService("", "http://localhost:4200");
        var command = new CreateCheckoutSessionCommand(27L, null,
                com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode.BASIC,
                com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle.MONTHLY,
                "https://example.org/subscriptions/success", "http://localhost:4200/subscriptions/cancel");
        assertThatThrownBy(() -> gateway.createCheckoutSession(command, "price_test"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("configured frontend");
    }

    private StripeSubscriptionDetails details(String status) {
        return new StripeSubscriptionDetails("cus_test", "sub_test", OffsetDateTime.now().toString(),
                OffsetDateTime.now().plusMonths(1).toString(), status, false);
    }

    private Session session() {
        var session = new Session();
        session.setId("cs_test");
        session.setMode("subscription");
        session.setStatus("complete");
        session.setPaymentStatus("paid");
        session.setSubscription("sub_test");
        session.setAmountTotal(1200L);
        session.setCurrency("usd");
        session.setMetadata(Map.of("userId", "27", "planCode", "BASIC", "billingCycle", "MONTHLY"));
        return session;
    }
}
