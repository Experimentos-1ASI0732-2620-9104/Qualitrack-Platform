package com.iotech.qualitrack.platform.subscription.infrastructure.configuration;

import com.iotech.qualitrack.platform.subscription.domain.model.entities.SubscriptionPlan;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.BillingCycle;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.Money;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.PlanCode;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.SubscriptionPlanCatalog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class SubscriptionConfiguration {

    /**
     * Plans created at startup when the database does not have them: Standard Lab and Enterprise, monthly and
     * yearly. Their Stripe prices belong to the Stripe account in use, so they come from subscription.plans.stripe-prices.*.
     */
    @Bean
    public SubscriptionPlanCatalog subscriptionPlanCatalog(
            @Value("${subscription.plans.stripe-prices.basic-monthly:}") String basicMonthly,
            @Value("${subscription.plans.stripe-prices.basic-yearly:}") String basicYearly,
            @Value("${subscription.plans.stripe-prices.enterprise-monthly:}") String enterpriseMonthly,
            @Value("${subscription.plans.stripe-prices.enterprise-yearly:}") String enterpriseYearly) {
        return new SubscriptionPlanCatalog(List.of(
                SubscriptionPlan.offer(PlanCode.BASIC, "Standard Lab", "Standard laboratory subscription - monthly",
                        usd("199.00"), BillingCycle.MONTHLY, basicMonthly, 10, 5),
                SubscriptionPlan.offer(PlanCode.BASIC, "Standard Lab", "Standard laboratory subscription - yearly",
                        usd("1990.00"), BillingCycle.YEARLY, basicYearly, 10, 5),
                SubscriptionPlan.offer(PlanCode.ENTERPRISE, "Enterprise", "Enterprise subscription - monthly",
                        usd("599.00"), BillingCycle.MONTHLY, enterpriseMonthly, 10, null),
                SubscriptionPlan.offer(PlanCode.ENTERPRISE, "Enterprise", "Enterprise subscription - yearly",
                        usd("5990.00"), BillingCycle.YEARLY, enterpriseYearly, 10, null)
        ));
    }

    private static Money usd(String amount) {
        return new Money(new BigDecimal(amount), "USD");
    }
}
