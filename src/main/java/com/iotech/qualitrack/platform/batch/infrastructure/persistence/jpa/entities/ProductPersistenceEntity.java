package com.iotech.qualitrack.platform.batch.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA persistence entity of the pharmaceutical product catalog.
 *
 * <p>The product code is unique per laboratory. {@code environment_id} is nullable so products
 * registered before environments existed keep working until they are assigned to one.</p>
 */
@Entity
@Table(name = "pharmaceutical_products", uniqueConstraints = @UniqueConstraint(
        name = "uk_pharmaceutical_products_laboratory_code", columnNames = {"laboratory_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
public class ProductPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "laboratory_id", nullable = false)
    private Long laboratoryId;

    @Column(name = "environment_id")
    private Long environmentId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 1000)
    private String specifications;

    @Column(nullable = false)
    private boolean active = true;
}
