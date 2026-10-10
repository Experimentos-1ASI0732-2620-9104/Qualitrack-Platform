package com.iotech.qualitrack.platform;

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
        subscription.cancelRenewal(27L, OffsetDateTime.now().toString());
        assertThat(subscription.grantsAccess()).isTrue();
        subscription.synchronizeStatus(SubscriptionStatus.CANCELLED);
        assertThat(subscription.grantsAccess()).isFalse();
    }
    @Test void laboratoryAssociationCannotBeReassigned() {
        var subscription = subscription(OffsetDateTime.now().plusDays(1).toString());
        subscription.assignLaboratory(47L);
        assertThatThrownBy(() -> subscription.assignLaboratory(48L)).isInstanceOf(IllegalStateException.class);
    }
}
