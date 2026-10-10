package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.PharmaceuticalProduct;
import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.entities.ProductPersistenceEntity;

/**
 * Maps pharmaceutical products between the domain model and JPA.
 */
public final class ProductPersistenceAssembler {

    private ProductPersistenceAssembler() {
    }

    public static PharmaceuticalProduct toDomainFromPersistence(ProductPersistenceEntity entity) {
        if (entity == null) return null;
        return new PharmaceuticalProduct(entity.getId(), entity.getLaboratoryId(), entity.getEnvironmentId(),
                entity.getCode(), entity.getName(), entity.getDescription(), entity.getSpecifications(), entity.isActive());
    }

    public static ProductPersistenceEntity toPersistenceFromDomain(PharmaceuticalProduct product) {
        if (product == null) return null;
        var entity = new ProductPersistenceEntity();
        if (product.getId() != null) entity.setId(product.getId());
        entity.setLaboratoryId(product.getLaboratoryId());
        entity.setEnvironmentId(product.getEnvironmentId());
        entity.setCode(product.getCode());
        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setSpecifications(product.getSpecifications());
        entity.setActive(product.isActive());
        return entity;
    }
}
