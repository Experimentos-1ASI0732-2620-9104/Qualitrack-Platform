package com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.tracking.domain.model.aggregates.EnvironmentalProfile;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationRule;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalThreshold;
import com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.entities.ActuationRuleEmbeddable;
import com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.entities.EnvironmentalProfilePersistenceEntity;
import com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.entities.ThresholdEmbeddable;

import java.util.ArrayList;

/**
 * Static assembler between environmental profile domain and persistence representations.
 */
public final class EnvironmentalProfilePersistenceAssembler {

    private EnvironmentalProfilePersistenceAssembler() {
    }

    public static EnvironmentalProfile toDomainFromPersistence(EnvironmentalProfilePersistenceEntity entity) {
        if (entity == null) return null;
        var thresholds = entity.getThresholds().stream()
                .map(row -> new EnvironmentalThreshold(row.getMetric(), row.getNormalMin(), row.getNormalMax(),
                        row.getCriticalMin(), row.getCriticalMax()))
                .toList();
        var rules = entity.getActuationRules().stream()
                .map(row -> new ActuationRule(row.getMetric(), row.getState(), row.getAction()))
                .toList();
        return new EnvironmentalProfile(entity.getId(), entity.getLaboratoryId(), entity.getScope(),
                entity.getEnvironmentId(), entity.getDeviceId(), entity.getVersion(), thresholds, rules,
                entity.getConfiguredBy(), entity.getConfiguredAt());
    }

    public static EnvironmentalProfilePersistenceEntity toPersistenceFromDomain(EnvironmentalProfile profile,
                                                                                 EnvironmentalProfilePersistenceEntity entity) {
        if (profile == null) return null;
        var target = entity == null ? new EnvironmentalProfilePersistenceEntity() : entity;
        if (profile.getId() != null) target.setId(profile.getId());
        target.setLaboratoryId(profile.getLaboratoryId());
        target.setScope(profile.getScope());
        target.setEnvironmentId(profile.getEnvironmentId());
        target.setDeviceId(profile.getDeviceId());
        target.setVersion(profile.getVersion());
        target.setConfiguredBy(profile.getUpdatedBy());
        target.setConfiguredAt(profile.getUpdatedAt());
        var thresholds = new ArrayList<ThresholdEmbeddable>();
        profile.getThresholds().forEach(threshold -> thresholds.add(new ThresholdEmbeddable(threshold.metric(),
                threshold.normalMin(), threshold.normalMax(), threshold.criticalMin(), threshold.criticalMax())));
        target.getThresholds().clear();
        target.getThresholds().addAll(thresholds);
        var rules = new ArrayList<ActuationRuleEmbeddable>();
        profile.getActuationRules().forEach(rule ->
                rules.add(new ActuationRuleEmbeddable(rule.metric(), rule.state(), rule.action())));
        target.getActuationRules().clear();
        target.getActuationRules().addAll(rules);
        return target;
    }
}
