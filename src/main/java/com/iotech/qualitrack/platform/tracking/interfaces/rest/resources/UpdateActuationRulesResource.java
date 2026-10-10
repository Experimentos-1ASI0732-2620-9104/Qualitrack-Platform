package com.iotech.qualitrack.platform.tracking.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Full set of actuation rules of a container monitor; it replaces the current one.
 */
@Schema(name = "UpdateActuationRulesRequest", description = "Rules that replace the current ones; an empty list removes them")
public record UpdateActuationRulesResource(@NotNull List<@Valid @NotNull ActuationRuleResource> rules) {
}
