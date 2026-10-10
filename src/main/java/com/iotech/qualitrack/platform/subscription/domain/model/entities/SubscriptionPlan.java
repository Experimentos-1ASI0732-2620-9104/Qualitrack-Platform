package com.iotech.qualitrack.platform.subscription.domain.model.entities;

import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.Money;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode;
import lombok.Getter;

import java.util.Objects;

/**
 * Domain entity representing an available subscription plan.
 */
@Getter
public class SubscriptionPlan {

    private Long id;
    private PlanCode code;
    private String name;
    private String description;
    private Money price;
    private BillingCycle billingCycle;
    private String stripePriceId;
    private Integer maxUsers;
    private Integer maxEquipment;
    private Boolean active;

    /**
     * Default constructor.
     */
    public SubscriptionPlan() {
        // Required for reconstruction
    }

    /**
     * Reconstructs a subscription plan.
     */
    public SubscriptionPlan(
            Long id,
            PlanCode code,
            String name,
            String description,
            Money price,
            BillingCycle billingCycle,
            String stripePriceId,
            Integer maxUsers,
            Integer maxEquipment,
            Boolean active
    ) {
        this.id = id;
        this.code = Objects.requireNonNull(code, "Plan code is required");
        this.name = Objects.requireNonNull(name, "Plan name is required");
        this.description = description;
        this.price = Objects.requireNonNull(price, "Plan price is required");
        this.billingCycle = Objects.requireNonNull(billingCycle, "Billing cycle is required");
        this.stripePriceId = stripePriceId;
        this.maxUsers = Objects.requireNonNull(maxUsers, "Max users is required");
        if (maxEquipment != null && maxEquipment < 0) {
            throw new IllegalArgumentException("Max equipment cannot be negative");
        }
        // Null explicitly represents an unlimited equipment allowance.
        this.maxEquipment = maxEquipment;
        this.active = Objects.requireNonNull(active, "Active flag is required");
    }

    /**
     * Creates a new active plan offered to laboratories.
     *
     * @param stripePriceId the Stripe price charged at checkout; blank when the Stripe account has none yet
     * @param maxEquipment null for an unlimited equipment allowance
     * @return a plan that is not stored yet
     */
    public static SubscriptionPlan offer(
            PlanCode code,
            String name,
            String description,
            Money price,
            BillingCycle billingCycle,
            String stripePriceId,
            Integer maxUsers,
            Integer maxEquipment
    ) {
        var priceId = stripePriceId == null || stripePriceId.isBlank() ? null : stripePriceId.trim();
        return new SubscriptionPlan(null, code, name, description, price, billingCycle, priceId,
                maxUsers, maxEquipment, true);
    }

    /**
     * Indicates whether this plan can be selected by users.
     *
     * @return true if the plan is active
     */
    public boolean isSelectable() {
        return Boolean.TRUE.equals(active);
    }
}
