package com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.entities;

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
 * Row of {@code environmental_profile_thresholds}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ThresholdEmbeddable {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MonitoredMetric metric;

    private Double normalMin;

    private Double normalMax;

    private Double criticalMin;

    private Double criticalMax;
}
