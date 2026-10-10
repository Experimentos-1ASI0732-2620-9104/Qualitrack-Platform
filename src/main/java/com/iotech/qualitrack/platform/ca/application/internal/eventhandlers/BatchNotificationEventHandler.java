package com.iotech.qualitrack.platform.ca.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.batch.interfaces.events.BatchRejectedIntegrationEvent;
import com.iotech.qualitrack.platform.batch.interfaces.events.BatchReleasedIntegrationEvent;
import com.iotech.qualitrack.platform.ca.application.commandservices.NotificationCommandService;
import com.iotech.qualitrack.platform.ca.application.internal.outboundservices.acl.CaExternalProfileService;
import com.iotech.qualitrack.platform.ca.domain.model.commands.PublishNotificationCommand;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.NotificationContent;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Notifies the laboratory of the quality decisions on its batches: released or rejected. A failure to notify never
 * undoes the decision.
 */
@Slf4j
@Service
public class BatchNotificationEventHandler {

    private final NotificationCommandService notificationCommandService;
    private final CaExternalProfileService externalProfileService;

    public BatchNotificationEventHandler(NotificationCommandService notificationCommandService,
                                         CaExternalProfileService externalProfileService) {
        this.notificationCommandService = notificationCommandService;
        this.externalProfileService = externalProfileService;
    }

    @EventListener(BatchReleasedIntegrationEvent.class)
    public void on(BatchReleasedIntegrationEvent event) {
        notify(event.laboratoryId(), event.batchId(), NotificationType.BATCH_RELEASED, event.batchNumber(),
                event.releasedBy(), null);
    }

    @EventListener(BatchRejectedIntegrationEvent.class)
    public void on(BatchRejectedIntegrationEvent event) {
        notify(event.laboratoryId(), event.batchId(), NotificationType.BATCH_REJECTED, event.batchNumber(),
                event.rejectedBy(), event.reason());
    }

    private void notify(Long laboratoryId, Long batchId, NotificationType type, String batchNumber, Long actorUserId,
                        String note) {
        try {
            var content = new NotificationContent(type, null, batchId, null, batchNumber, null, null, null,
                    actorUserId == null ? null : externalProfileService.displayNameOf(actorUserId), note);
            notificationCommandService.handle(new PublishNotificationCommand(laboratoryId, content, actorUserId));
        } catch (RuntimeException exception) {
            log.warn("Notification {} of batch {} not created: {}", type, batchId, exception.getMessage());
        }
    }
}
