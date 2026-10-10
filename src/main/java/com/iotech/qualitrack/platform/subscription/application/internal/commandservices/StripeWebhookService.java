package com.iotech.qualitrack.platform.subscription.application.internal.commandservices;

import com.iotech.qualitrack.platform.iam.interfaces.acl.IamContextFacade;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationException;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.subscription.application.commandservices.SubscriptionCommandService;
import com.iotech.qualitrack.platform.subscription.application.internal.outboundservices.acl.ExternalStripeService;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.ActivateSubscriptionCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.RecordStripePaymentCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PaymentStatus;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.SubscriptionStatus;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionRepository;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionPaymentRepository;
import com.stripe.model.checkout.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Transactional, idempotent fulfillment of signature-verified Stripe events. */
@Service
public class StripeWebhookService {
    private final SubscriptionCommandService commands;
    private final SubscriptionRepository subscriptions;
    private final SubscriptionPaymentRepository payments;
    private final ExternalStripeService stripe;
    private final IamContextFacade users;

    public StripeWebhookService(SubscriptionCommandService commands, SubscriptionRepository subscriptions,
                                SubscriptionPaymentRepository payments, ExternalStripeService stripe,
                                IamContextFacade users) {
        this.commands = commands;
        this.subscriptions = subscriptions;
        this.payments = payments;
        this.stripe = stripe;
        this.users = users;
    }

    @Transactional
    public void fulfill(Session session) {
        if (!"subscription".equals(session.getMode()) || !"complete".equals(session.getStatus())
                || !"paid".equals(session.getPaymentStatus())) return;
        var metadata = session.getMetadata();
        if (metadata == null) throw new IllegalArgumentException("Checkout metadata is missing");
        var userId = Long.valueOf(metadata.get("userId"));
        var laboratoryId = users.lockLaboratoryAssociation(userId);
        var suppliedLab = metadata.get("laboratoryId");
        if (suppliedLab != null && !Long.valueOf(suppliedLab).equals(laboratoryId)) {
            throw new IllegalArgumentException("Checkout laboratory does not match the account");
        }
        var existing = subscriptions.findByStripeCheckoutSessionId(session.getId());
        Long subscriptionId;
        if (existing.isPresent()) {
            subscriptionId = existing.get().getId();
        } else {
            var details = stripe.retrieveSubscriptionDetails(session.getSubscription());
            if (!"active".equals(details.status())) return;
            subscriptionId = requireSuccess(commands.handle(new ActivateSubscriptionCommand(
                    userId, laboratoryId, PlanCode.valueOf(metadata.get("planCode")),
                    BillingCycle.valueOf(metadata.get("billingCycle")), details.stripeCustomerId(),
                    details.stripeSubscriptionId(), session.getId(), details.currentPeriodStart(),
                    details.currentPeriodEnd())));
        }
        if (payments.findByStripeCheckoutSessionId(session.getId()).isEmpty()
                && session.getAmountTotal() != null && session.getCurrency() != null) {
            requireSuccess(commands.handle(new RecordStripePaymentCommand(subscriptionId,
                    session.getPaymentIntent(), session.getId(),
                    BigDecimal.valueOf(session.getAmountTotal()).movePointLeft(2),
                    session.getCurrency().toUpperCase(java.util.Locale.ROOT), PaymentStatus.PAID,
                    OffsetDateTime.now().toString())));
        }
    }

    @Transactional
    public void synchronize(String stripeSubscriptionId) {
        var found = subscriptions.findByStripeSubscriptionId(stripeSubscriptionId);
        if (found.isEmpty()) return;
        users.lockLaboratoryAssociation(found.get().getUserId());
        var subscription = subscriptions.findByStripeSubscriptionId(stripeSubscriptionId).orElseThrow();
        var details = stripe.retrieveSubscriptionDetails(stripeSubscriptionId);
        var active = subscription.getLaboratoryId() == null
                ? subscriptions.findActiveByUserId(subscription.getUserId())
                : subscriptions.findActiveByLaboratoryId(subscription.getLaboratoryId());
        if ("active".equals(details.status()) && active.filter(candidate -> candidate.grantsAccess()
                && !candidate.getId().equals(subscription.getId())).isPresent()) {
            throw new ApplicationException(ApplicationError.conflict("Subscription",
                    "Another active subscription exists; provider reconciliation is required"));
        }
        subscription.updatePeriod(details.currentPeriodStart(), details.currentPeriodEnd());
        subscription.synchronizeStatus(switch (details.status()) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "past_due", "unpaid", "incomplete" -> SubscriptionStatus.PENDING_PAYMENT;
            case "canceled" -> SubscriptionStatus.CANCELLED;
            case "incomplete_expired" -> SubscriptionStatus.EXPIRED;
            default -> SubscriptionStatus.INACTIVE;
        });
        subscription.synchronizeRenewal(details.cancelAtPeriodEnd(), OffsetDateTime.now().toString());
        subscriptions.save(subscription);
    }

    private static Long requireSuccess(Result<Long, ApplicationError> result) {
        return switch (result) {
            case Result.Success<Long, ApplicationError> success -> success.value();
            case Result.Failure<Long, ApplicationError> failure -> throw new ApplicationException(failure.error());
        };
    }
}
