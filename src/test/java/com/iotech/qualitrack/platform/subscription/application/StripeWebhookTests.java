package com.iotech.qualitrack.platform.subscription.application;

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
    private final SubscriptionExternalIamService users = mock(SubscriptionExternalIamService.class);
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

    @Test void stripeRenewalDecisionIsSynchronized() {
        var subscription = new Subscription(73L, 27L, null,
                com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode.BASIC,
                com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle.MONTHLY,
                com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.SubscriptionStatus.ACTIVE,
                "cus_test", "sub_test", "cs_test", OffsetDateTime.now().toString(),
                OffsetDateTime.now().plusMonths(1).toString(), null, null, false);
        when(subscriptions.findByStripeSubscriptionId("sub_test")).thenReturn(Optional.of(subscription));
        when(subscriptions.findActiveByUserId(27L)).thenReturn(Optional.of(subscription));
        when(stripe.retrieveSubscriptionDetails("sub_test")).thenReturn(details("active", true));
        service.synchronize("sub_test");
        assertThat(subscription.isCancelAtPeriodEnd()).isTrue();
        assertThat(subscription.grantsAccess()).isTrue();
        when(stripe.retrieveSubscriptionDetails("sub_test")).thenReturn(details("canceled", true));
        service.synchronize("sub_test");
        assertThat(subscription.getStatus())
                .isEqualTo(com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.SubscriptionStatus.CANCELLED);
        assertThat(subscription.grantsAccess()).isFalse();
    }

    @Test void renewalInvoiceRecordsItsPaymentWhenStripeWasPaid() {
        when(subscriptions.findByStripeSubscriptionId("sub_test")).thenReturn(Optional.of(subscription()));
        when(payments.findByProviderPaymentId("in_renewal")).thenReturn(Optional.empty());
        when(commands.handle(any(RecordStripePaymentCommand.class))).thenReturn(Result.success(82L));
        service.recordRenewalPayment(invoice("subscription_cycle"));
        verify(users).lockLaboratoryAssociation(27L);
        verify(commands).handle(argThat((RecordStripePaymentCommand command) -> command.subscriptionId().equals(73L)
                && "in_renewal".equals(command.providerPaymentId()) && command.stripeCheckoutSessionId() == null
                && command.amount().compareTo(new java.math.BigDecimal("199.00")) == 0 && "USD".equals(command.currency())
                && OffsetDateTime.parse(command.paidAt()).toInstant().equals(RENEWAL_PAID_AT)));
    }

    @Test void repeatedRenewalInvoiceIsRecordedOnce() {
        when(subscriptions.findByStripeSubscriptionId("sub_test")).thenReturn(Optional.of(subscription()));
        when(payments.findByProviderPaymentId("in_renewal")).thenReturn(Optional.of(
                mock(com.iotech.qualitrack.platform.subscription.domain.model.entities.SubscriptionPayment.class)));
        service.recordRenewalPayment(invoice("subscription_cycle"));
        verifyNoInteractions(commands);
    }

    @Test void firstInvoiceIsLeftToTheCheckout() {
        service.recordRenewalPayment(invoice("subscription_create"));
        verifyNoInteractions(commands, subscriptions, payments, users);
    }

    @Test void unpaidRenewalInvoiceIsNotRecorded() {
        var invoice = invoice("subscription_cycle");
        invoice.setStatus("open");
        service.recordRenewalPayment(invoice);
        verifyNoInteractions(commands, subscriptions, payments, users);
    }

    @Test void invoiceOfAnUnknownSubscriptionIsIgnored() {
        when(subscriptions.findByStripeSubscriptionId("sub_test")).thenReturn(Optional.empty());
        service.recordRenewalPayment(invoice("subscription_cycle"));
        verifyNoInteractions(commands, payments, users);
    }

    private static final java.time.Instant RENEWAL_PAID_AT = java.time.Instant.parse("2026-11-04T23:51:30Z");

    private Subscription subscription() {
        return new Subscription(73L, 27L, 42L,
                com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode.BASIC,
                com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle.MONTHLY,
                com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.SubscriptionStatus.ACTIVE,
                "cus_test", "sub_test", "cs_test", OffsetDateTime.now().toString(),
                OffsetDateTime.now().plusMonths(1).toString(), null, null, false);
    }

    private com.stripe.model.Invoice invoice(String billingReason) {
        var details = new com.stripe.model.Invoice.Parent.SubscriptionDetails();
        details.setSubscription("sub_test");
        var parent = new com.stripe.model.Invoice.Parent();
        parent.setType("subscription_details");
        parent.setSubscriptionDetails(details);
        var transitions = new com.stripe.model.Invoice.StatusTransitions();
        transitions.setPaidAt(RENEWAL_PAID_AT.getEpochSecond());
        var invoice = new com.stripe.model.Invoice();
        invoice.setId("in_renewal");
        invoice.setBillingReason(billingReason);
        invoice.setStatus("paid");
        invoice.setAmountPaid(19900L);
        invoice.setCurrency("usd");
        invoice.setParent(parent);
        invoice.setStatusTransitions(transitions);
        return invoice;
    }

    private StripeSubscriptionDetails details(String status) {
        return details(status, false);
    }

    private StripeSubscriptionDetails details(String status, boolean cancelAtPeriodEnd) {
        return new StripeSubscriptionDetails("cus_test", "sub_test", OffsetDateTime.now().toString(),
                OffsetDateTime.now().plusMonths(1).toString(), status, cancelAtPeriodEnd);
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
