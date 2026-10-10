package com.iotech.qualitrack.platform.inventory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(name = "ImportRawMaterialRequest",
    description = "Legacy (pre-Inventory) raw material record to import into the environment")
public record ImportRawMaterialResource(@NotNull @Positive Long legacyId) { }
