package com.iotech.qualitrack.platform.batch.domain.model.aggregates;

import com.iotech.qualitrack.platform.batch.domain.model.commands.CreateProductCommand;
import com.iotech.qualitrack.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import lombok.Getter;

import java.util.Objects;

/**
 * Pharmaceutical product manufactured in an environment of a laboratory (US71, TS61).
 *
 * <p>The product is the reusable concept; every manufacturing run of it is a {@link Batch}.
 * Products registered before environments existed keep a null environment until the migration
 * script assigns them to one.</p>
 */
@Getter
public class PharmaceuticalProduct extends AbstractDomainAggregateRoot<PharmaceuticalProduct> {
    private Long id;
    private Long laboratoryId;
    private Long environmentId;
    private String code;
    private String name;
    private String description;
    private String specifications;
    private boolean active;

    /**
     * Default constructor required by the persistence assemblers.
     */
    public PharmaceuticalProduct() {
        // Required for reconstruction by assemblers
    }

    /**
     * Reconstructs a product from persistence data.
     */
    public PharmaceuticalProduct(Long id, Long laboratoryId, Long environmentId, String code, String name,
                                 String description, String specifications, boolean active) {
        this.id = id;
        this.laboratoryId = laboratoryId;
        this.environmentId = environmentId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.specifications = specifications;
        this.active = active;
    }

    /**
     * Registers a new active product in the environment given by the command.
     *
     * @param command the validated registration data
     */
    public PharmaceuticalProduct(CreateProductCommand command) {
        Objects.requireNonNull(command, "Create product command is required");
        this.laboratoryId = command.laboratoryId();
        this.environmentId = command.environmentId();
        this.code = command.code();
        this.name = command.name();
        this.description = command.description();
        this.specifications = command.specifications();
        this.active = true;
    }

    /**
     * Tells whether the product is registered in the given environment of the laboratory.
     *
     * @param laboratoryId the laboratory identifier
     * @param environmentId the environment identifier
     * @return true when both identifiers match
     */
    public boolean belongsTo(Long laboratoryId, Long environmentId) {
        return Objects.equals(this.laboratoryId, laboratoryId) && Objects.equals(this.environmentId, environmentId);
    }
}
