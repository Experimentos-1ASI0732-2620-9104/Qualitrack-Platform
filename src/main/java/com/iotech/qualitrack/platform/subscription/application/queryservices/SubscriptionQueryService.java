package com.iotech.qualitrack.platform.subscription.application.queryservices;

import com.iotech.qualitrack.platform.subscription.domain.model.aggregates.Subscription;
import com.iotech.qualitrack.platform.subscription.domain.model.entities.SubscriptionPayment;
import com.iotech.qualitrack.platform.subscription.domain.model.entities.SubscriptionPlan;
import com.iotech.qualitrack.platform.subscription.domain.model.queries.GetActiveSubscriptionByLaboratoryIdQuery;
import com.iotech.qualitrack.platform.subscription.domain.model.queries.GetActiveSubscriptionByUserIdQuery;
import com.iotech.qualitrack.platform.subscription.domain.model.queries.GetBillingSummaryByLaboratoryIdQuery;
import com.iotech.qualitrack.platform.subscription.domain.model.queries.GetPaymentsBySubscriptionIdQuery;
import com.iotech.qualitrack.platform.subscription.domain.model.queries.GetSubscriptionByIdQuery;
import com.iotech.qualitrack.platform.subscription.domain.model.queries.GetSubscriptionByStripeCheckoutSessionIdQuery;
import com.iotech.qualitrack.platform.subscription.domain.model.queries.GetSubscriptionPlansQuery;

import java.util.List;
import java.util.Optional;

/**
 * Application query service contract for subscription read operations.
 */
public interface SubscriptionQueryService {

    List<SubscriptionPlan> handle(GetSubscriptionPlansQuery query);

    Optional<Subscription> handle(GetActiveSubscriptionByLaboratoryIdQuery query);

    Optional<Subscription> handle(GetActiveSubscriptionByUserIdQuery query);

    List<Subscription> handle(GetBillingSummaryByLaboratoryIdQuery query);

    List<SubscriptionPayment> handle(GetPaymentsBySubscriptionIdQuery query);

    Optional<Subscription> handle(GetSubscriptionByStripeCheckoutSessionIdQuery query);

    Optional<Subscription> handle(GetSubscriptionByIdQuery query);
}