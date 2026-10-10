package com.iotech.qualitrack.platform.ca.application.internal.notifications;

import com.iotech.qualitrack.platform.ca.application.commandservices.NotificationCommandService;
import com.iotech.qualitrack.platform.ca.domain.model.commands.SendAlertEmailNotificationCommand;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertEmailDelivery;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends the e-mail notice of a critical alert in the background once the alert is stored, so that the reading that
 * caused it (sent by the Edge) does not wait for the e-mail provider and a reading that is rolled back e-mails nobody.
 */
@Slf4j
@Component
public class CriticalAlertEmailScheduler implements DisposableBean {

    private final NotificationCommandService notificationCommandService;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public CriticalAlertEmailScheduler(NotificationCommandService notificationCommandService) {
        this.notificationCommandService = notificationCommandService;
    }

    /**
     * @param alertId the open critical alert to e-mail
     */
    public void schedule(Long alertId) {
        Runnable task = () -> send(alertId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(task);
                }
            });
        } else {
            executor.execute(task);
        }
    }

    private void send(Long alertId) {
        try {
            switch (notificationCommandService.handle(new SendAlertEmailNotificationCommand(alertId, null))) {
                case Result.Success<AlertEmailDelivery, ApplicationError> success -> log.info(
                        "E-mail notice of critical alert {} accepted for {} of {} recipients.", alertId,
                        success.value().delivered(), success.value().recipients());
                case Result.Failure<AlertEmailDelivery, ApplicationError> failure -> log.warn(
                        "E-mail notice of critical alert {} not sent: {}", alertId, failure.error().details());
            }
        } catch (RuntimeException exception) {
            log.warn("E-mail notice of critical alert {} failed: {}", alertId, exception.getMessage());
        }
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }
}
