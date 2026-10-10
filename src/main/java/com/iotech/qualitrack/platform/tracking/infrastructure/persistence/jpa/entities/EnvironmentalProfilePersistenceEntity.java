package com.iotech.qualitrack.platform.tracking.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.ProfileScope;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Environmental profile stored in {@code environmental_profiles} with its thresholds and actuation rules in child
 * tables.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "environmental_profiles")
public class EnvironmentalProfilePersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(nullable = false)
    private Long laboratoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProfileScope scope;

    @Column(unique = true)
    private Long environmentId;

    @Column(unique = true)
    private Long deviceId;

    @Column(nullable = false)
    private Long version;

    private Long configuredBy;

    private Instant configuredAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "environmental_profile_thresholds", joinColumns = @JoinColumn(name = "profile_id"))
    @OrderColumn(name = "position")
    private List<ThresholdEmbeddable> thresholds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "environmental_profile_actuation_rules", joinColumns = @JoinColumn(name = "profile_id"))
    @OrderColumn(name = "position")
    private List<ActuationRuleEmbeddable> actuationRules = new ArrayList<>();
}
