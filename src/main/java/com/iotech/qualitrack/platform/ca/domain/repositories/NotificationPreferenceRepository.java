package com.iotech.qualitrack.platform.ca.domain.repositories;

import com.iotech.qualitrack.platform.ca.domain.model.entities.NotificationPreference;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * NotificationPreference repository port.
 *
 * <p>Handles the persistence contract for user notification preferences.</p>
 */
public interface NotificationPreferenceRepository {

    Optional<NotificationPreference> findById(Long id);

    Optional<NotificationPreference> findByUserId(Long userId);

    /**
     * @return the stored preferences of the users; users who never saved them are left out
     */
    List<NotificationPreference> findByUserIds(Collection<Long> userIds);

    List<NotificationPreference> findAll();

    NotificationPreference save(NotificationPreference notificationPreference);

    boolean existsById(Long id);

    boolean existsByUserId(Long userId);

    void deleteById(Long id);
}