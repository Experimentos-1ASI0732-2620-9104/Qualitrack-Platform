package com.iotech.qualitrack.platform.laboratory.domain.model.aggregates;

import com.iotech.qualitrack.platform.laboratory.domain.model.commands.RegisterEnvironmentCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.UpdateEnvironmentCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.EnvironmentUsage;
import com.iotech.qualitrack.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * The Environment Aggregate Root.
 *
 * <p>Represents a physical area of a laboratory or pharmaceutical warehouse, such as a
 * laboratory zone, a production area or a storage room. Environments are the common
 * reference used by other bounded contexts to place raw materials, products, equipment
 * and IoT devices, and to organize the measurements taken in each zone.</p>
 */
@Getter
public class Environment extends AbstractDomainAggregateRoot<Environment> {

    private static final int MAX_CODE_LENGTH = 30;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private Long id;

    /**
     * The laboratory that owns this environment.
     */
    private Long laboratoryId;

    /**
     * Identification of the environment, unique within its laboratory and stored in upper case.
     */
    private String code;

    private String name;

    private String description;

    /**
     * Main use of the environment. It is null until a usage is assigned.
     */
    private EnvironmentUsage usage;

    /**
     * User that assigned the current usage.
     */
    private Long usageAssignedBy;

    /**
     * Moment when the current usage was assigned.
     */
    private Instant usageAssignedAt;

    /**
     * Reconstructs an Environment from persistence data.
     */
    public Environment(Long id, Long laboratoryId, String code, String name, String description,
                       EnvironmentUsage usage, Long usageAssignedBy, Instant usageAssignedAt) {
        this.id = id;
        this.laboratoryId = laboratoryId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.usage = usage;
        this.usageAssignedBy = usageAssignedBy;
        this.usageAssignedAt = usageAssignedAt;
    }

    /**
     * Registers a new environment without an assigned usage.
     *
     * @param command The command containing the registration data.
     */
    public Environment(RegisterEnvironmentCommand command) {
        this.laboratoryId = Objects.requireNonNull(command.laboratoryId(), "Laboratory ID is required");
        this.code = normalizeCode(command.code());
        this.name = normalizeName(command.name());
        this.description = normalizeDescription(command.description());
    }

    /**
     * Updates the identification data of the environment.
     *
     * @param command The command containing the new data.
     */
    public void update(UpdateEnvironmentCommand command) {
        this.code = normalizeCode(command.code());
        this.name = normalizeName(command.name());
        this.description = normalizeDescription(command.description());
    }

    /**
     * Assigns the main use of the environment.
     * <p>Business Rule: the usage must change; assigning the current usage again is rejected.</p>
     *
     * @param newUsage The allowed usage to assign.
     * @param assignedBy The user that performs the assignment.
     * @param assignedAt The moment of the assignment.
     * @throws IllegalStateException if the environment already has the requested usage.
     */
    public void assignUsage(EnvironmentUsage newUsage, Long assignedBy, Instant assignedAt) {
        Objects.requireNonNull(newUsage, "Usage is required");
        Objects.requireNonNull(assignedAt, "Assignment moment is required");
        if (newUsage == this.usage) {
            throw new IllegalStateException("Environment already has usage " + newUsage);
        }
        this.usage = newUsage;
        this.usageAssignedBy = assignedBy;
        this.usageAssignedAt = assignedAt;
    }

    /**
     * Checks whether the environment belongs to the given laboratory.
     *
     * @param laboratoryId The laboratory identifier to compare.
     * @return true when the environment belongs to the laboratory.
     */
    public boolean belongsTo(Long laboratoryId) {
        return this.laboratoryId != null && this.laboratoryId.equals(laboratoryId);
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Environment code is required");
        }
        var normalized = code.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException("Environment code cannot exceed " + MAX_CODE_LENGTH + " characters");
        }
        return normalized;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Environment name is required");
        }
        var normalized = name.trim();
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Environment name cannot exceed " + MAX_NAME_LENGTH + " characters");
        }
        return normalized;
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        var normalized = description.trim();
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Environment description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        return normalized;
    }
}
