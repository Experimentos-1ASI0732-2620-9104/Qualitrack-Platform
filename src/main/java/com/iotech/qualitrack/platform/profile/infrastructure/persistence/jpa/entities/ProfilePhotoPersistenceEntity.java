package com.iotech.qualitrack.platform.profile.infrastructure.persistence.jpa.entities;

import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PhotoImage;
import com.iotech.qualitrack.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Image of a profile photo, kept apart from the profile so that reading a profile does not load it.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "profile_photos", uniqueConstraints = @UniqueConstraint(name = "uk_profile_photos_profile", columnNames = "profile_id"))
public class ProfilePhotoPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    // MEDIUMBLOB in MySQL: the size limit of PhotoImage is far below its 16 MB.
    @Lob
    @Column(name = "content", nullable = false, length = PhotoImage.MAX_SIZE_BYTES)
    private byte[] content;
}
