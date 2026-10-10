package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.PharmaceuticalProduct;
import com.iotech.qualitrack.platform.batch.domain.repositories.ProductRepository;
import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.assemblers.ProductPersistenceAssembler;
import com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.repositories.ProductPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter of the product repository.
 */
@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductPersistenceRepository repository;

    public ProductRepositoryImpl(ProductPersistenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PharmaceuticalProduct> findById(Long id) {
        return repository.findById(id).map(ProductPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<PharmaceuticalProduct> findAllByLaboratoryIdAndEnvironmentId(Long laboratoryId, Long environmentId) {
        return repository.findAllByLaboratoryIdAndEnvironmentIdOrderByNameAsc(laboratoryId, environmentId).stream()
                .map(ProductPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<PharmaceuticalProduct> findByNameAndLaboratoryId(String name, Long laboratoryId) {
        return repository.findByNameAndLaboratoryId(name, laboratoryId).map(ProductPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public boolean existsByLaboratoryIdAndCode(Long laboratoryId, String code) {
        return repository.existsByLaboratoryIdAndCode(laboratoryId, code);
    }

    @Override
    public PharmaceuticalProduct save(PharmaceuticalProduct product) {
        var saved = repository.save(ProductPersistenceAssembler.toPersistenceFromDomain(product));
        return ProductPersistenceAssembler.toDomainFromPersistence(saved);
    }
}
