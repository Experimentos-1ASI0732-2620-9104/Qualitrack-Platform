package com.iotech.qualitrack.platform.laboratory.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.iam.interfaces.events.UserAccountUpdatedIntegrationEvent;
import com.iotech.qualitrack.platform.laboratory.domain.repositories.StaffRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Keeps the e-mail of the staff list in line with the one each staff member changes in the account. Profile renames
 * the staff member through the Laboratory facade when its full name changes.
 */
@Service
public class StaffAccountSynchronizationEventHandler {

    private final StaffRepository staffRepository;

    public StaffAccountSynchronizationEventHandler(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @EventListener(UserAccountUpdatedIntegrationEvent.class)
    public void on(UserAccountUpdatedIntegrationEvent event) {
        staffRepository.findByUserId(event.userId())
                .filter(member -> member.changeEmail(event.email()))
                .ifPresent(staffRepository::save);
    }

}
