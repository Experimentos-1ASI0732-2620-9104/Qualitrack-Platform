package com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.adapters;

import com.iotech.qualitrack.platform.ca.domain.model.entities.NotificationPreference;
import com.iotech.qualitrack.platform.ca.domain.repositories.NotificationPreferenceRepository;
import com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.assemblers.NotificationPreferencePersistenceAssembler;
import com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.repositories.NotificationPreferencePersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementation for the {@link NotificationPreferenceRepository} port.
 *
 * <p>Translates domain-level requests for notification preferences into
 * Spring Data JPA database operations.</p>
 */
@Repository
public class NotificationPreferenceRepositoryImpl implements NotificationPreferenceRepository {

    private final NotificationPreferencePersistenceRepository repository;

    public NotificationPreferenceRepositoryImpl(NotificationPreferencePersistenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<NotificationPreference> findById(Long id) {
        return repository.findById(id)
                .map(NotificationPreferencePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<NotificationPreference> findByUserId(Long userId) {
        return repository.findByUserId(userId)
                .map(NotificationPreferencePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<NotificationPreference> findByUserIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) return List.of();
        return repository.findByUserIdIn(userIds).stream()
                .map(NotificationPreferencePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<NotificationPreference> findAll() {
        return repository.findAll().stream()
                .map(NotificationPreferencePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public NotificationPreference save(NotificationPreference notificationPreference) {
        var entityToSave = NotificationPreferencePersistenceAssembler.toPersistenceFromDomain(notificationPreference);

        var saved = repository.save(entityToSave);

        return NotificationPreferencePersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return repository.existsByUserId(userId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}