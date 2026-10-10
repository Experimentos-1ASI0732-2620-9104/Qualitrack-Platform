package com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.iam.domain.model.valueobjects.UserStatus;
import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * JPA persistence entity for IAM users.
 */
@Getter
@Setter
@Entity
@Table(
        name = "iam_users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"username"}),
                @UniqueConstraint(columnNames = {"email"})
        }
)
public class UserPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(nullable = false, length = 80)
    private String username;

    @Column(length = 120)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "laboratory_id")
    private Long laboratoryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserStatus status;

    @Column(name = "password_change_required", nullable = false)
    private boolean passwordChangeRequired;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "iam_user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RolePersistenceEntity> roles = new HashSet<>();
}