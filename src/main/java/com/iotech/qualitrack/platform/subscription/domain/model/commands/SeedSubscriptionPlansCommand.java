package com.iotech.qualitrack.platform.subscription.domain.model.commands;

import com.iotech.qualitrack.platform.subscription.domain.model.valueobjects.SubscriptionPlanCatalog;

import java.util.Objects;

/**
 * Command used to create the plans of the catalog that the database does not have yet.
 *
 * @param catalog the plans QualiTrack offers
 */
public record SeedSubscriptionPlansCommand(SubscriptionPlanCatalog catalog) {

    /**
     * Creates a validated command.
     */
    public SeedSubscriptionPlansCommand {
        Objects.requireNonNull(catalog, "Plan catalog is required");
    }
}
