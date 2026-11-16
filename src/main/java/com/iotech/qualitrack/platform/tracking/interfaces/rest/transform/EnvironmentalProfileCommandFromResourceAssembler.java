package com.iotech.qualitrack.platform.tracking.interfaces.rest.transform;

import com.iotech.qualitrack.platform.tracking.domain.model.commands.UpdateActuationRulesCommand;
import com.iotech.qualitrack.platform.tracking.domain.model.commands.UpdateContainerMonitorThresholdsCommand;
import com.iotech.qualitrack.platform.tracking.domain.model.commands.UpdateEnvironmentThresholdsCommand;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationRule;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalThreshold;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.UpdateActuationRulesResource;
import com.iotech.qualitrack.platform.tracking.interfaces.rest.resources.UpdateThresholdsResource;

import java.util.List;

/**
 * Assembler that transforms the profile requests into commands; invalid limits or rules raise
 * IllegalArgumentException (400).
 */
public final class EnvironmentalProfileCommandFromResourceAssembler {

    private EnvironmentalProfileCommandFromResourceAssembler() {
    }

    /**
     * @return the command that replaces the thresholds of the environment
     * @throws IllegalArgumentException when a metric is unknown or the limits are not valid
     */
    public static UpdateEnvironmentThresholdsCommand toEnvironmentCommand(Long laboratoryId, Long environmentId,
                                                                          UpdateThresholdsResource resource) {
        return new UpdateEnvironmentThresholdsCommand(laboratoryId, environmentId, thresholds(resource));
    }

    /**
     * @return the command that replaces the thresholds of the container monitor
     * @throws IllegalArgumentException when a metric is unknown or the limits are not valid
     */
    public static UpdateContainerMonitorThresholdsCommand toContainerMonitorCommand(Long laboratoryId, Long environmentId,
                                                                                    Long deviceId,
                                                                                    UpdateThresholdsResource resource) {
        return new UpdateContainerMonitorThresholdsCommand(laboratoryId, environmentId, deviceId, thresholds(resource));
    }

    /**
     * @return the command that replaces the actuation rules of the container monitor
     * @throws IllegalArgumentException when a metric, state or action is unknown or the rule is not valid
     */
    public static UpdateActuationRulesCommand toActuationRulesCommand(Long laboratoryId, Long environmentId, Long deviceId,
                                                                      UpdateActuationRulesResource resource) {
        var rules = resource.rules().stream()
                .map(rule -> new ActuationRule(TrackingRequestValues.metric(rule.metric()),
                        TrackingRequestValues.state(rule.state()), TrackingRequestValues.action(rule.action())))
                .toList();
        return new UpdateActuationRulesCommand(laboratoryId, environmentId, deviceId, rules);
    }

    /**
     * Converts the requested thresholds; the unit sent by the client is ignored because it is set by the metric.
     */
    private static List<EnvironmentalThreshold> thresholds(UpdateThresholdsResource resource) {
        return resource.thresholds().stream()
                .map(threshold -> new EnvironmentalThreshold(TrackingRequestValues.metric(threshold.metric()),
                        threshold.normalMin(), threshold.normalMax(), threshold.criticalMin(), threshold.criticalMax()))
                .toList();
    }
}
