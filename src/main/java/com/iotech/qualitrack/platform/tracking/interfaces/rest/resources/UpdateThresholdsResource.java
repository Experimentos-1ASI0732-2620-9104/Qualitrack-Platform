package com.iotech.qualitrack.platform.tracking.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Full set of thresholds of a profile; it replaces the current one.
 */
@Schema(name = "UpdateThresholdsRequest", description = "Thresholds that replace the current ones; an empty list removes them")
public record UpdateThresholdsResource(@NotNull List<@Valid @NotNull ThresholdResource> thresholds) {
}
