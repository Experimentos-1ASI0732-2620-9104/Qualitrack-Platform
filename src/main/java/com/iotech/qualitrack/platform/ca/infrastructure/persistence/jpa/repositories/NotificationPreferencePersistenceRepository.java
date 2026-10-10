package com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.entities.NotificationPreferencePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link NotificationPreferencePersistenceEntity}.
 *
 * <p>Handles database operations for the notification_preferences table using
 * Long as the primary key. Provides custom queries for fetching preferences
 * by user ID.</p>
 */
@Repository
public interface NotificationPreferencePersistenceRepository extends JpaRepository<NotificationPreferencePersistenceEntity, Long> {

    /**
     * Finds notification preferences by owner user ID.
     *
     * @param userId The numeric ID of the user.
     */
    Optional<NotificationPreferencePersistenceEntity> findByUserId(Long userId);

    /**
     * Checks if notification preferences already exist for the given user.
     *
     * @param userId The numeric ID of the user.
     */
    boolean existsByUserId(Long userId);

    List<NotificationPreferencePersistenceEntity> findByUserIdIn(Collection<Long> userIds);
}