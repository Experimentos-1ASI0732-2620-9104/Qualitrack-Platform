package com.iotech.qualitrack.platform.batch.interfaces.rest.transform;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.PharmaceuticalProduct;
import com.iotech.qualitrack.platform.batch.interfaces.rest.resources.PharmaceuticalProductResource;

/**
 * Maps pharmaceutical products to their REST representation.
 */
public final class ProductResourceFromEntityAssembler {

    private ProductResourceFromEntityAssembler() {
    }

    public static PharmaceuticalProductResource toResourceFromEntity(PharmaceuticalProduct entity) {
        return new PharmaceuticalProductResource(entity.getId(), entity.getLaboratoryId(), entity.getEnvironmentId(),
                entity.getCode(), entity.getName(), entity.getDescription(), entity.getSpecifications(), entity.isActive());
    }
}
