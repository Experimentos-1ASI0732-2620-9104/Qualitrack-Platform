package com.iotech.qualitrack.platform.profile.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "profiles", uniqueConstraints = @UniqueConstraint(name = "uk_profiles_user", columnNames = "user_id"))
public class ProfilePersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "full_name", length = 120)
    private String fullName;

    @Column(name = "dni", length = 8)
    private String dni;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "location", length = 120)
    private String location;

    @Column(name = "photo_content_type", length = 20)
    private String photoContentType;

    @Column(name = "photo_size_bytes")
    private Integer photoSizeBytes;

    @Column(name = "photo_updated_at")
    private Instant photoUpdatedAt;
}
