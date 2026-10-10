package com.iotech.qualitrack.platform.ca.application.internal.commandservices;

import com.iotech.qualitrack.platform.ca.application.commandservices.NotificationCommandService;
import com.iotech.qualitrack.platform.ca.application.internal.outboundservices.acl.CaExternalEquipmentService;
import com.iotech.qualitrack.platform.ca.application.internal.outboundservices.acl.CaExternalIamService;
import com.iotech.qualitrack.platform.ca.application.internal.outboundservices.acl.CaExternalLaboratoryService;
import com.iotech.qualitrack.platform.ca.application.internal.outboundservices.notifications.AlertEmailNotifier;
import com.iotech.qualitrack.platform.ca.domain.model.aggregates.Notification;
import com.iotech.qualitrack.platform.ca.domain.model.commands.MarkAllNotificationsAsReadCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.MarkNotificationAsReadCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.PublishNotificationCommand;
import com.iotech.qualitrack.platform.ca.domain.model.commands.SendAlertEmailNotificationCommand;
import com.iotech.qualitrack.platform.ca.domain.model.entities.ComplianceEvent;
import com.iotech.qualitrack.platform.ca.domain.model.entities.NotificationPreference;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertEmailDelivery;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.ComplianceEventSubject;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.ComplianceEventType;
import com.iotech.qualitrack.platform.ca.domain.repositories.ComplianceEventRepository;
import com.iotech.qualitrack.platform.ca.domain.repositories.DeviationAlertRepository;
import com.iotech.qualitrack.platform.ca.domain.repositories.NotificationPreferenceRepository;
import com.iotech.qualitrack.platform.ca.domain.repositories.NotificationRepository;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Creates the notifications of a laboratory from its preferences and sends the e-mail notices of critical alerts.
 */
@Slf4j
@Service
public class NotificationCommandServiceImpl implements NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final DeviationAlertRepository deviationAlertRepository;
    private final ComplianceEventRepository complianceEventRepository;
    private final CaExternalIamService externalIamService;
    private final CaExternalLaboratoryService externalLaboratoryService;
    private final CaExternalEquipmentService externalEquipmentService;
    private final AlertEmailNotifier alertEmailNotifier;
    private final Clock complianceClock;

    public NotificationCommandServiceImpl(NotificationRepository notificationRepository,
                                          NotificationPreferenceRepository preferenceRepository,
                                          DeviationAlertRepository deviationAlertRepository,
                                          ComplianceEventRepository complianceEventRepository,
                                          CaExternalIamService externalIamService,
                                          CaExternalLaboratoryService externalLaboratoryService,
                                          CaExternalEquipmentService externalEquipmentService,
                                          AlertEmailNotifier alertEmailNotifier,
                                          Clock complianceClock) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.deviationAlertRepository = deviationAlertRepository;
        this.complianceEventRepository = complianceEventRepository;
        this.externalIamService = externalIamService;
        this.externalLaboratoryService = externalLaboratoryService;
        this.externalEquipmentService = externalEquipmentService;
        this.alertEmailNotifier = alertEmailNotifier;
        this.complianceClock = complianceClock;
    }

    /**
     * Runs in its own transaction so that a failure to notify never rolls back the change being notified.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Notification> handle(PublishNotificationCommand command) {
        var content = command.content();
        var alertSeverity = content.subject() == ComplianceEventSubject.ALERT ? content.severity() : null;
        var recipients = externalIamService.findRecipients(command.laboratoryId()).stream()
                .filter(recipient -> !recipient.userId().equals(command.actorUserId()))
                .toList();
        var preferences = preferencesOf(recipients.stream().map(CaExternalIamService.Recipient::userId).toList());
        var now = Instant.now(complianceClock);
        var notifications = recipients.stream()
                .filter(recipient -> preferences.get(recipient.userId()).wantsInApp(alertSeverity))
                .map(recipient -> new Notification(recipient.userId(), command.laboratoryId(), content, now))
                .toList();
        return notifications.isEmpty() ? List.of() : notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public Result<Notification, ApplicationError> handle(MarkNotificationAsReadCommand command) {
        var found = notificationRepository.findById(command.notificationId())
                .filter(notification -> notification.belongsTo(command.userId()));
        if (found.isEmpty()) return Result.failure(ApplicationError.notFound("Notification", command.notificationId()));
        var notification = found.get();
        if (notification.markAsRead(Instant.now(complianceClock))) notificationRepository.save(notification);
        return Result.success(notification);
    }

    @Override
    @Transactional
    public int handle(MarkAllNotificationsAsReadCommand command) {
        return notificationRepository.markAllAsRead(command.userId(), Instant.now(complianceClock));
    }

    @Override
    public Result<AlertEmailDelivery, ApplicationError> handle(SendAlertEmailNotificationCommand command) {
        var found = deviationAlertRepository.findById(command.alertId());
        if (found.isEmpty()) return Result.failure(ApplicationError.notFound("DeviationAlert", command.alertId()));
        var alert = found.get();
        if (!alert.isCritical() || !alert.isOpen()) {
            return Result.failure(ApplicationError.conflict("DeviationAlert",
                    "Only open critical alerts are notified by e-mail"));
        }
        var recipients = externalIamService.findRecipients(alert.getLaboratoryId()).stream()
                .filter(recipient -> recipient.email() != null && !recipient.userId().equals(command.requestedBy()))
                .toList();
        var preferences = preferencesOf(recipients.stream().map(CaExternalIamService.Recipient::userId).toList());
        var addressees = recipients.stream()
                .filter(recipient -> preferences.get(recipient.userId()).wantsEmail(AlertSeverity.CRITICAL))
                .toList();
        var environmentName = externalLaboratoryService.environmentName(alert.getLaboratoryId(), alert.getEnvironmentId());
        var deviceName = externalEquipmentService.findSource(alert.getLaboratoryId(), alert.getEnvironmentId(), alert.getEquipmentId())
                .map(CaExternalEquipmentService.AlertSource::name).orElse(null);
        var delivered = (int) addressees.stream()
                .filter(recipient -> send(new AlertEmailNotifier.AlertNotice(recipient.email(), alert.getId(),
                        environmentName, deviceName, alert.getParameterName(), alert.getRecordedValue(),
                        alert.getThresholdValue(), alert.getUnit(), alert.getTimestamp())))
                .count();
        var sentAt = Instant.now(complianceClock).toString();
        var delivery = new AlertEmailDelivery(alert.getId(), addressees.size(), delivered, sentAt);
        complianceEventRepository.save(new ComplianceEvent(alert.getId(), ComplianceEventType.DEVIATION_ALERT_EMAIL_SENT,
                "E-mail notice of the critical alert accepted for %d of %d recipients.".formatted(delivered, addressees.size()),
                sentAt, command.requestedBy()));
        if (delivery.failed()) {
            return Result.failure(ApplicationError.externalServiceFailure("e-mail",
                    "The e-mail provider did not accept the notices; check that e-mail is configured"));
        }
        return Result.success(delivery);
    }

    private boolean send(AlertEmailNotifier.AlertNotice notice) {
        try {
            return alertEmailNotifier.send(notice);
        } catch (RuntimeException exception) {
            log.warn("The e-mail notice of alert {} could not be sent: {}", notice.alertId(), exception.getMessage());
            return false;
        }
    }

    /**
     * Preferences of each user; the defaults for the users who never saved them.
     */
    private Map<Long, NotificationPreference> preferencesOf(List<Long> userIds) {
        var stored = preferenceRepository.findByUserIds(userIds).stream()
                .collect(Collectors.toMap(NotificationPreference::getUserId, Function.identity(), (first, second) -> first));
        return userIds.stream().distinct().collect(Collectors.toMap(Function.identity(),
                userId -> Objects.requireNonNullElseGet(stored.get(userId), () -> new NotificationPreference(userId))));
    }
}
