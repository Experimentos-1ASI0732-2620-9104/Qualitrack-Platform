package com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ActuationAction;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.EnvironmentalState;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.MonitoredMetric;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Row of {@code environmental_profile_actuation_rules}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ActuationRuleEmbeddable {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MonitoredMetric metric;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnvironmentalState state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActuationAction action;
}
