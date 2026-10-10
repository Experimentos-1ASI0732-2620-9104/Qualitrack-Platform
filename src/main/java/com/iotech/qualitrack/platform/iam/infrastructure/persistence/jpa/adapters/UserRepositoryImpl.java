package com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.iam.domain.model.aggregates.User;
import com.iotech.qualitrack.platform.iam.domain.repositories.UserRepository;
import com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.assemblers.UserPersistenceAssembler;
import com.iotech.qualitrack.platform.iam.infrastructure.persistence.jpa.repositories.UserPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation for IAM user repository port.
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserPersistenceRepository userPersistenceRepository;

    public UserRepositoryImpl(UserPersistenceRepository userPersistenceRepository) {
        this.userPersistenceRepository = userPersistenceRepository;
    }

    @Override
    public Optional<User> findById(Long id) {
        return userPersistenceRepository.findById(id)
                .map(UserPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userPersistenceRepository.findByUsername(username)
                .map(UserPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<User> findByIdForUpdate(Long id) {
        return userPersistenceRepository.findByIdForUpdate(id)
                .map(UserPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<User> findAll() {
        return userPersistenceRepository.findAll().stream()
                .map(UserPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<User> findByLaboratoryId(Long laboratoryId) {
        return userPersistenceRepository.findByLaboratoryId(laboratoryId).stream()
                .map(UserPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public User save(User user) {
        var entity = UserPersistenceAssembler.toPersistenceFromDomain(user);
        var savedEntity = userPersistenceRepository.save(entity);

        return UserPersistenceAssembler.toDomainFromPersistence(savedEntity);
    }

    @Override
    public boolean existsById(Long id) {
        return userPersistenceRepository.existsById(id);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userPersistenceRepository.existsByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userPersistenceRepository.findByEmail(email).map(UserPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userPersistenceRepository.existsByEmail(email);
    }
}
