package com.iotech.qualitrack.platform.subscription.domain.model;

import com.iotech.qualitrack.platform.subscription.domain.model.aggregates.Subscription;
import com.iotech.qualitrack.platform.subscription.domain.model.commands.ActivateSubscriptionCommand;
import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.*;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import static org.assertj.core.api.Assertions.*;

class SubscriptionAccessTests {
    private Subscription subscription(String end) {
        return Subscription.activate(new ActivateSubscriptionCommand(27L, null, PlanCode.BASIC,
                BillingCycle.MONTHLY, "cus_fixture", "sub_fixture", "cs_fixture",
                OffsetDateTime.now().minusDays(1).toString(), end));
    }
    @Test void activeFuturePeriodAllowsAccessBeforeLaboratoryCreation() {
        var subscription = subscription(OffsetDateTime.now().plusDays(1).toString());
        assertThat(subscription.grantsAccess()).isTrue();
        assertThat(subscription.getLaboratoryId()).isNull();
    }
    @Test void expiredOrUnknownPeriodsNeverGrantAccess() {
        assertThat(subscription(OffsetDateTime.now().minusDays(1).toString()).grantsAccess()).isFalse();
        assertThat(subscription(null).grantsAccess()).isFalse();
        assertThat(subscription("invalid").grantsAccess()).isFalse();
    }
    @Test void cancelledOrUnpaidSubscriptionDoesNotGrantAccess() {
        var subscription = subscription(OffsetDateTime.now().plusDays(1).toString());
        subscription.synchronizeStatus(SubscriptionStatus.PENDING_PAYMENT);
        assertThat(subscription.grantsAccess()).isFalse();
        subscription.synchronizeStatus(SubscriptionStatus.ACTIVE);
        assertThat(subscription.grantsAccess()).isTrue();
        subscription.synchronizeStatus(SubscriptionStatus.CANCELLED);
        assertThat(subscription.grantsAccess()).isFalse();
    }
    @Test void cancellingTheRenewalKeepsAccessUntilThePeriodEnds() {
        var subscription = subscription(OffsetDateTime.now().plusDays(10).toString());
        subscription.cancelRenewal(27L, OffsetDateTime.now().toString());
        assertThat(subscription.isCancelAtPeriodEnd()).isTrue();
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.grantsAccess()).isTrue();
        assertThatThrownBy(() -> subscription.cancelRenewal(27L, OffsetDateTime.now().toString()))
                .isInstanceOf(IllegalStateException.class);
        subscription.synchronizeStatus(SubscriptionStatus.CANCELLED);
        assertThat(subscription.grantsAccess()).isFalse();
        assertThatThrownBy(() -> subscription.cancelRenewal(27L, OffsetDateTime.now().toString()))
                .isInstanceOf(IllegalStateException.class);
    }
    @Test void aRenewalResumedAtTheProviderClearsTheCancellation() {
        var subscription = subscription(OffsetDateTime.now().plusDays(10).toString());
        subscription.cancelRenewal(27L, "2026-10-04T10:00:00Z");
        subscription.synchronizeRenewal(true, "2026-10-04T11:00:00Z");
        assertThat(subscription.getCancelledAt()).isEqualTo("2026-10-04T10:00:00Z");
        subscription.synchronizeRenewal(false, "2026-10-04T12:00:00Z");
        assertThat(subscription.isCancelAtPeriodEnd()).isFalse();
        assertThat(subscription.getCancelledAt()).isNull();
        assertThat(subscription.getCancelledBy()).isNull();
        subscription.synchronizeRenewal(true, "2026-10-04T13:00:00Z");
        assertThat(subscription.isCancelAtPeriodEnd()).isTrue();
        assertThat(subscription.getCancelledAt()).isEqualTo("2026-10-04T13:00:00Z");
    }
    @Test void laboratoryAssociationCannotBeReassigned() {
        var subscription = subscription(OffsetDateTime.now().plusDays(1).toString());
        subscription.assignLaboratory(47L);
        assertThatThrownBy(() -> subscription.assignLaboratory(48L)).isInstanceOf(IllegalStateException.class);
    }
}
