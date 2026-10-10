package com.iotech.qualitrack.platform.tracking.interfaces.rest.transform;

import com.iotech.qualitrack.platform.tracking.domain.model.aggregates.EnvironmentalProfile;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.ActuationRuleResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.EnvironmentalProfileResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.ThresholdResource;

/**
 * Assembler that transforms environmental profiles into REST resources.
 */
public final class EnvironmentalProfileResourceFromEntityAssembler {

    private EnvironmentalProfileResourceFromEntityAssembler() {
    }

    public static EnvironmentalProfileResource toResourceFromEntity(EnvironmentalProfile profile) {
        var thresholds = profile.getThresholds().stream()
                .map(threshold -> new ThresholdResource(threshold.metric().name(), threshold.unit(), threshold.normalMin(),
                        threshold.normalMax(), threshold.criticalMin(), threshold.criticalMax()))
                .toList();
        var rules = profile.getActuationRules().stream()
                .map(rule -> new ActuationRuleResource(rule.metric().name(), rule.state().name(), rule.action().name()))
                .toList();
        return new EnvironmentalProfileResource(profile.getId(), profile.getScope().name(), profile.getLaboratoryId(),
                profile.getEnvironmentId(), profile.getDeviceId(), profile.getVersion(), thresholds, rules,
                profile.getUpdatedAt() == null ? null : profile.getUpdatedAt().toString(), profile.getUpdatedBy());
    }
}
