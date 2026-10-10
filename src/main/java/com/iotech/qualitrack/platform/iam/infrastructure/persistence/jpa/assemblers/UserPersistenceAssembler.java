package com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.assemblers;

import com.iotech.qualitrack.platform.iam.domain.model.aggregates.User;
import com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.entities.UserPersistenceEntity;

import java.util.stream.Collectors;

/**
 * Assembler that maps IAM user domain aggregates and persistence entities.
 */
public class UserPersistenceAssembler {

    public static User toDomainFromPersistence(UserPersistenceEntity entity) {
        if (entity == null) return null;

        var roles = entity.getRoles().stream()
                .map(RolePersistenceAssembler::toDomainFromPersistence)
                .collect(Collectors.toSet());

        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getPassword(),
                roles,
                entity.getLaboratoryId(),
                entity.getStatus(),
                entity.isPasswordChangeRequired()
        );
    }

    public static UserPersistenceEntity toPersistenceFromDomain(User user) {
        if (user == null) return null;

        var entity = new UserPersistenceEntity();
        entity.setId(user.getId());
        entity.setUsername(user.getUsernameValue());
        entity.setEmail(user.getEmailValue());
        entity.setPassword(user.getPasswordValue());
        entity.setLaboratoryId(user.getLaboratoryId());
        entity.setStatus(user.getStatus());
        entity.setPasswordChangeRequired(user.isPasswordChangeRequired());

        var roles = user.getRoles().stream()
                .map(RolePersistenceAssembler::toPersistenceFromDomain)
                .collect(Collectors.toSet());

        entity.setRoles(roles);

        return entity;
    }
}