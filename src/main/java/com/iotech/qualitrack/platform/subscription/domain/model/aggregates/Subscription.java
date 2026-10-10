package com.iotech.qualitrack.platform.subscription.domain.model.aggregates;

import com.iotech.qualitrack.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.ActivateSubscriptionCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.SubscriptionStatus;
import lombok.Getter;

import java.util.Objects;

/**
 * Subscription aggregate root.
 *
 * <p>Represents the active commercial relationship between a laboratory and
 * QualiTrack, backed by Stripe as the payment provider.</p>
 */
@Getter
public class Subscription extends AbstractDomainAggregateRoot<Subscription> {

    private Long id;
    private Long userId;
    private Long laboratoryId;
    private PlanCode planCode;
    private BillingCycle billingCycle;
    private SubscriptionStatus status;
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private String stripeCheckoutSessionId;
    private String currentPeriodStart;
    private String currentPeriodEnd;
    /** When the renewal was cancelled; the subscription keeps its access until {@link #currentPeriodEnd}. */
    private String cancelledAt;
    private Long cancelledBy;
    /** True when the subscription ends with the current period instead of renewing (US23). */
    private boolean cancelAtPeriodEnd;

    /**
     * Default constructor.
     */
    public Subscription() {
        // Required for reconstruction
    }

    /**
     * Reconstructs a subscription aggregate.
     */
    public Subscription(
            Long id,
            Long userId,
            Long laboratoryId,
            PlanCode planCode,
            BillingCycle billingCycle,
            SubscriptionStatus status,
            String stripeCustomerId,
            String stripeSubscriptionId,
            String stripeCheckoutSessionId,
            String currentPeriodStart,
            String currentPeriodEnd,
            String cancelledAt,
            Long cancelledBy,
            boolean cancelAtPeriodEnd
    ) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "User id is required");
        if (laboratoryId != null && laboratoryId <= 0) {
            throw new IllegalArgumentException("Laboratory id must be positive");
        }
        this.laboratoryId = laboratoryId;
        this.planCode = Objects.requireNonNull(planCode, "Plan code is required");
        this.billingCycle = Objects.requireNonNull(billingCycle, "Billing cycle is required");
        this.status = Objects.requireNonNull(status, "Subscription status is required");
        this.stripeCustomerId = stripeCustomerId;
        this.stripeSubscriptionId = stripeSubscriptionId;
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.cancelledAt = cancelledAt;
        this.cancelledBy = cancelledBy;
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
    }

    /**
     * Creates an activated subscription from a Stripe activation command.
     *
     * @param command The activation command.
     * @return A new active subscription.
     */
    public static Subscription activate(ActivateSubscriptionCommand command) {
        return new Subscription(
                null,
                command.userId(),
                command.laboratoryId(),
                command.planCode(),
                command.billingCycle(),
                SubscriptionStatus.ACTIVE,
                command.stripeCustomerId(),
                command.stripeSubscriptionId(),
                command.stripeCheckoutSessionId(),
                command.currentPeriodStart(),
                command.currentPeriodEnd(),
                null,
                null,
                false
        );
    }

    /**
     * Cancels the renewal: the subscription stays active, and keeps its access, until the end of the paid period, when
     * the payment provider ends it (US23).
     *
     * @param cancelledBy The user requesting the cancellation.
     * @param cancelledAt The cancellation timestamp.
     * @throws IllegalStateException when the subscription is not active or its renewal is already cancelled
     */
    public void cancelRenewal(Long cancelledBy, String cancelledAt) {
        if (!SubscriptionStatus.ACTIVE.equals(this.status)) {
            throw new IllegalStateException("Only the renewal of an active subscription can be cancelled");
        }
        if (cancelAtPeriodEnd) {
            throw new IllegalStateException("The renewal of the subscription is already cancelled");
        }
        if (cancelledBy == null || cancelledBy <= 0) {
            throw new IllegalArgumentException("Cancelled by must be a positive user id");
        }
        if (cancelledAt == null || cancelledAt.isBlank()) {
            throw new IllegalArgumentException("Cancelled at is required");
        }

        this.cancelAtPeriodEnd = true;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = cancelledAt;
    }

    /**
     * Indicates whether the subscription is active.
     *
     * @return true if active
     */
    public boolean isActive() {
        return SubscriptionStatus.ACTIVE.equals(status);
    }

    public boolean grantsAccess() {
        if (!isActive() || currentPeriodEnd == null) return false;
        try {
            return java.time.OffsetDateTime.parse(currentPeriodEnd).isAfter(java.time.OffsetDateTime.now());
        } catch (java.time.format.DateTimeParseException exception) {
            return false;
        }
    }

    public void assignLaboratory(Long laboratoryId) {
        if (laboratoryId == null || laboratoryId <= 0) {
            throw new IllegalArgumentException("A positive laboratory identifier is required");
        }
        if (this.laboratoryId != null && !this.laboratoryId.equals(laboratoryId)) {
            throw new IllegalStateException("Subscription already belongs to another laboratory");
        }
        this.laboratoryId = laboratoryId;
    }

    public void updatePeriod(String start, String end) {
        this.currentPeriodStart = start;
        this.currentPeriodEnd = end;
    }

    public void synchronizeStatus(SubscriptionStatus status) {
        this.status = Objects.requireNonNull(status);
    }

    /**
     * Applies the renewal decision recorded by the payment provider: a renewal resumed there clears the cancellation, and
     * a cancellation made there (without a user of the platform) is kept.
     *
     * @param cancelAtPeriodEnd whether the provider ends the subscription with the current period
     * @param at moment of the synchronization, used when the cancellation was made at the provider
     */
    public void synchronizeRenewal(boolean cancelAtPeriodEnd, String at) {
        if (!cancelAtPeriodEnd && status == SubscriptionStatus.ACTIVE) {
            this.cancelAtPeriodEnd = false;
            this.cancelledAt = null;
            this.cancelledBy = null;
        } else if (cancelAtPeriodEnd && !this.cancelAtPeriodEnd) {
            this.cancelAtPeriodEnd = true;
            if (this.cancelledAt == null) this.cancelledAt = at;
        }
    }
}
