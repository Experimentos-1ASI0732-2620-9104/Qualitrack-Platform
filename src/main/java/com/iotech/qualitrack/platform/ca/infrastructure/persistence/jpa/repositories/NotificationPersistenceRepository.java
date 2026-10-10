package com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.repositories;

import com.iotech.qualitrack.platform.ca.infrastructure.persistence.jpa.entities.NotificationPersistenceEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationPersistenceRepository extends JpaRepository<NotificationPersistenceEntity, Long> {

    List<NotificationPersistenceEntity> findByRecipientUserIdOrderByOccurredAtDescIdDesc(Long recipientUserId, Pageable page);

    List<NotificationPersistenceEntity> findByRecipientUserIdAndReadAtIsNullOrderByOccurredAtDescIdDesc(
            Long recipientUserId, Pageable page);

    long countByRecipientUserIdAndReadAtIsNull(Long recipientUserId);

    @Modifying
    @Query("update NotificationPersistenceEntity n set n.readAt = :readAt "
            + "where n.recipientUserId = :recipientUserId and n.readAt is null")
    int markAllAsRead(@Param("recipientUserId") Long recipientUserId, @Param("readAt") Instant readAt);
}
