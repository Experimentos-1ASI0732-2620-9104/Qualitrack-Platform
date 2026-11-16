package com.iotech.qualitrack.platform.subscription.domain.model;

import com.iotech.qualitrack.platform.subscription.domain.model.entities.SubscriptionPlan;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.Money;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.SubscriptionPlanCatalog;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionPlanTests {
    private SubscriptionPlan plan(Integer equipmentLimit) {
        return new SubscriptionPlan(1L, PlanCode.ENTERPRISE, "Enterprise", "Test fixture",
                new Money(new BigDecimal("599.00"), "USD"), BillingCycle.MONTHLY,
                "price_fixture", 10, equipmentLimit, true);
    }

    @Test void preservesFiniteEquipmentAllowances() {
        assertThat(plan(5).getMaxEquipment()).isEqualTo(5);
    }

    @Test void representsUnlimitedEquipmentWithoutAnInventedNumericLimit() {
        assertThat(plan(null).getMaxEquipment()).isNull();
        assertThat(plan(null).isSelectable()).isTrue();
    }

    @Test void rejectsNegativeEquipmentLimits() {
        assertThatThrownBy(() -> plan(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    private SubscriptionPlan offer(PlanCode code, BillingCycle cycle, String stripePriceId) {
        return SubscriptionPlan.offer(code, "Plan", "Test fixture", new Money(new BigDecimal("199.00"), "USD"),
                cycle, stripePriceId, 10, 5);
    }

    @Test void offersANewActivePlanWithoutABlankStripePrice() {
        var offered = offer(PlanCode.BASIC, BillingCycle.MONTHLY, "  ");
        assertThat(offered.getId()).isNull();
        assertThat(offered.isSelectable()).isTrue();
        assertThat(offered.getStripePriceId()).isNull();
        assertThat(offer(PlanCode.BASIC, BillingCycle.MONTHLY, " price_basic ").getStripePriceId()).isEqualTo("price_basic");
    }

    @Test void catalogOffersEachPlanAndStripePriceOnce() {
        var catalog = new SubscriptionPlanCatalog(List.of(
                offer(PlanCode.BASIC, BillingCycle.MONTHLY, "price_basic_monthly"),
                offer(PlanCode.BASIC, BillingCycle.YEARLY, null),
                offer(PlanCode.ENTERPRISE, BillingCycle.YEARLY, null)));
        assertThat(catalog.plans()).hasSize(3);
        assertThatThrownBy(() -> new SubscriptionPlanCatalog(List.of(
                offer(PlanCode.BASIC, BillingCycle.MONTHLY, "price_a"),
                offer(PlanCode.BASIC, BillingCycle.MONTHLY, "price_b")))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SubscriptionPlanCatalog(List.of(
                offer(PlanCode.BASIC, BillingCycle.MONTHLY, "price_a"),
                offer(PlanCode.ENTERPRISE, BillingCycle.MONTHLY, "price_a")))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SubscriptionPlanCatalog(List.of(plan(5)))).isInstanceOf(IllegalArgumentException.class);
    }
}
