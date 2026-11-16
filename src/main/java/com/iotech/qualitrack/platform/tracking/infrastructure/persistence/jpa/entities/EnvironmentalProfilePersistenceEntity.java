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
 *
 * <p>The unique {@code environment_id} and {@code device_id} keep a single profile per environment and per container
 * monitor. The thresholds and rules are loaded eagerly because the profile is always used whole.</p>
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

    /**
     * Configured environment; set only for the ENVIRONMENT scope.
     */
    @Column(unique = true)
    private Long environmentId;

    /**
     * Configured container monitor; set only for the CONTAINER_MONITOR scope.
     */
    @Column(unique = true)
    private Long deviceId;

    /**
     * Configuration version of the domain; it is not a JPA optimistic lock.
     */
    @Column(nullable = false)
    private Long version;

    /**
     * User that made the last change of the configuration.
     */
    private Long configuredBy;

    /**
     * Moment of the last change of the configuration.
     */
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
