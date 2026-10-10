package com.iotech.qualitrack.platform.subscription.application.internal.commandservices;

import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.subscription.application.commandservices.SubscriptionCommandService;
import com.iotech.qualitrack.platform.subscription.application.internal.outboundservices.acl.ExternalStripeService;
import com.iotech.qualitrack.platform.subscription.domain.model.aggregates.Subscription;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.ActivateSubscriptionCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.CancelSubscriptionCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.CreateCheckoutSessionCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.RecordStripePaymentCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.entities.SubscriptionPayment;
import com.iotech.qualitrack.platform.subscription.domain.model.events.CheckoutSessionCreatedEvent;
import com.iotech.qualitrack.platform.subscription.domain.model.events.SubscriptionActivatedEvent;
import com.iotech.qualitrack.platform.subscription.domain.model.events.SubscriptionCancelledEvent;
import com.iotech.qualitrack.platform.subscription.domain.model.events.SubscriptionPaymentRecordedEvent;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.Money;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionPaymentRepository;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionPlanRepository;
import com.iotech.qualitrack.platform.subscription.domain.repositories.SubscriptionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Application service implementation for subscription commands.
 */
@Service
public class SubscriptionCommandServiceImpl implements SubscriptionCommandService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final ExternalStripeService externalStripeService;
    private final ApplicationEventPublisher eventPublisher;

    public SubscriptionCommandServiceImpl(
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionRepository subscriptionRepository,
            SubscriptionPaymentRepository subscriptionPaymentRepository,
            ExternalStripeService externalStripeService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionPaymentRepository = subscriptionPaymentRepository;
        this.externalStripeService = externalStripeService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<String, ApplicationError> handle(CreateCheckoutSessionCommand command) {
        var existing = command.laboratoryId() == null
                ? subscriptionRepository.findActiveByUserId(command.userId())
                : subscriptionRepository.findActiveByLaboratoryId(command.laboratoryId());
        if (existing.filter(Subscription::grantsAccess).isPresent()) {
            return Result.failure(ApplicationError.conflict("Subscription", "An active subscription already exists"));
        }
        var planResult = subscriptionPlanRepository.findByCodeAndBillingCycle(
                command.planCode(),
                command.billingCycle()
        );

        if (planResult.isEmpty()) {
            return Result.failure(ApplicationError.notFound(
                    "SubscriptionPlan",
                    command.planCode() + "-" + command.billingCycle()
            ));
        }

        var plan = planResult.get();

        if (!plan.isSelectable()) {
            return Result.failure(ApplicationError.conflict(
                    "SubscriptionPlan",
                    "Selected subscription plan is not active"
            ));
        }

        try {
            var checkoutSession = externalStripeService.createCheckoutSession(
                    command,
                    plan.getStripePriceId()
            );

            eventPublisher.publishEvent(new CheckoutSessionCreatedEvent(
                    command.userId(),
                    command.laboratoryId(),
                    command.planCode(),
                    command.billingCycle(),
                    checkoutSession.checkoutSessionId(),
                    checkoutSession.checkoutUrl()
            ));

            return Result.success(checkoutSession.checkoutUrl());

        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("CheckoutSession", e.getMessage()));
        } catch (Exception e) {
            return Result.failure(ApplicationError.unexpected("create-checkout-session", e.getMessage()));
        }
    }

    @Override
    public Result<Long, ApplicationError> handle(ActivateSubscriptionCommand command) {
        try {
            var processed = subscriptionRepository.findByStripeCheckoutSessionId(command.stripeCheckoutSessionId());
            if (processed.isPresent()) return Result.success(processed.get().getId());
            var activeSubscription = command.laboratoryId() == null
                    ? subscriptionRepository.findActiveByUserId(command.userId())
                    : subscriptionRepository.findActiveByLaboratoryId(command.laboratoryId());

            if (activeSubscription.filter(Subscription::grantsAccess).isPresent()) {
                return Result.failure(ApplicationError.conflict("Subscription",
                        "Another active subscription exists; reconcile the duplicate checkout before fulfillment"));
            }
            activeSubscription.ifPresent(subscription -> {
                subscription.synchronizeStatus(com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.SubscriptionStatus.EXPIRED);
                subscriptionRepository.save(subscription);
            });

            var subscription = Subscription.activate(command);
            var savedSubscription = subscriptionRepository.save(subscription);

            eventPublisher.publishEvent(SubscriptionActivatedEvent.from(savedSubscription));

            return Result.success(savedSubscription.getId());

        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("Subscription", e.getMessage()));
        } catch (Exception e) {
            return Result.failure(ApplicationError.unexpected("activate-subscription", e.getMessage()));
        }
    }

    @Override
    public Result<Long, ApplicationError> handle(CancelSubscriptionCommand command) {
        var subscriptionResult = subscriptionRepository.findById(command.subscriptionId());

        if (subscriptionResult.isEmpty()) {
            return Result.failure(ApplicationError.notFound(
                    "Subscription",
                    String.valueOf(command.subscriptionId())
            ));
        }

        try {
            var subscription = subscriptionResult.get();

            if (!subscription.isActive()) {
                return Result.failure(ApplicationError.conflict(
                        "Subscription",
                        "Subscription is not active"
                ));
            }
            if (subscription.isCancelAtPeriodEnd()) {
                return Result.failure(ApplicationError.conflict(
                        "Subscription",
                        "The renewal of the subscription is already cancelled"
                ));
            }

            externalStripeService.cancelRenewal(subscription.getStripeSubscriptionId());

            subscription.cancelRenewal(command.cancelledBy(), OffsetDateTime.now().toString());
            var savedSubscription = subscriptionRepository.save(subscription);

            eventPublisher.publishEvent(SubscriptionCancelledEvent.from(savedSubscription));

            return Result.success(savedSubscription.getId());

        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("Subscription", e.getMessage()));
        } catch (Exception e) {
            return Result.failure(ApplicationError.unexpected("cancel-subscription", e.getMessage()));
        }
    }

    @Override
    public Result<Long, ApplicationError> handle(RecordStripePaymentCommand command) {
        if (!subscriptionRepository.existsById(command.subscriptionId())) {
            return Result.failure(ApplicationError.notFound(
                    "Subscription",
                    String.valueOf(command.subscriptionId())
            ));
        }

        if (command.stripePaymentIntentId() != null
                && subscriptionPaymentRepository.findByProviderPaymentId(command.stripePaymentIntentId()).isPresent()) {
            return Result.failure(ApplicationError.conflict(
                    "SubscriptionPayment",
                    "Payment with provider ID '%s' already exists".formatted(command.stripePaymentIntentId())
            ));
        }

        if (subscriptionPaymentRepository.findByStripeCheckoutSessionId(command.stripeCheckoutSessionId()).isPresent()) {
            return Result.failure(ApplicationError.conflict(
                    "SubscriptionPayment",
                    "Payment with checkout session ID '%s' already exists".formatted(command.stripeCheckoutSessionId())
            ));
        }

        try {
            var payment = SubscriptionPayment.stripePayment(
                    command.subscriptionId(),
                    command.stripePaymentIntentId(),
                    command.stripeCheckoutSessionId(),
                    new Money(command.amount(), command.currency()),
                    command.status(),
                    command.paidAt()
            );

            var savedPayment = subscriptionPaymentRepository.save(payment);

            eventPublisher.publishEvent(SubscriptionPaymentRecordedEvent.from(savedPayment));

            return Result.success(savedPayment.getId());

        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("SubscriptionPayment", e.getMessage()));
        } catch (Exception e) {
            return Result.failure(ApplicationError.unexpected("record-stripe-payment", e.getMessage()));
        }
    }
}
