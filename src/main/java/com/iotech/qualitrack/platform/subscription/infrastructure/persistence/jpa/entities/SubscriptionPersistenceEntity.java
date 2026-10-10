package com.iotech.qualitrack.platform.subscription.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

/**
 * JPA entity for subscriptions.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "subscriptions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"stripe_subscription_id"}),
                @UniqueConstraint(columnNames = {"stripe_checkout_session_id"})
        }
)
public class SubscriptionPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "laboratory_id")
    private Long laboratoryId;

    @Column(name = "plan_code", nullable = false, length = 50)
    private PlanCode planCode;

    @Column(name = "billing_cycle", nullable = false, length = 50)
    private BillingCycle billingCycle;

    @Column(name = "status", nullable = false, length = 50)
    private SubscriptionStatus status;

    @Column(name = "stripe_customer_id", length = 150)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 150)
    private String stripeSubscriptionId;

    @Column(name = "stripe_checkout_session_id", length = 150)
    private String stripeCheckoutSessionId;

    @Column(name = "current_period_start", length = 50)
    private String currentPeriodStart;

    @Column(name = "current_period_end", length = 50)
    private String currentPeriodEnd;

    @Column(name = "cancelled_at", length = 50)
    private String cancelledAt;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    // Default so rows inserted without the column (older versions, existing rows) keep renewing.
    @ColumnDefault("false")
    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;
}
