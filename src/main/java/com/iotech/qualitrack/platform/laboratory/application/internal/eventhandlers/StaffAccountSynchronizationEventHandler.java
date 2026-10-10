package com.iotech.qualitrack.platform.laboratory.application.internal.eventhandlers;

import com.iotech.qualitrack.platform.iam.interfaces.events.UserAccountUpdatedIntegrationEvent;
import com.iotech.qualitrack.platform.laboratory.domain.repositories.StaffRepository;
import com.iotech.qualitrack.platform.profile.interfaces.events.ProfileUpdatedIntegrationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Keeps the staff list in line with what each staff member changes in the account and the profile: the e-mail of the
 * account and the full name of the profile.
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

    @EventListener(ProfileUpdatedIntegrationEvent.class)
    public void on(ProfileUpdatedIntegrationEvent event) {
        staffRepository.findByUserId(event.userId())
                .filter(member -> member.rename(event.fullName()))
                .ifPresent(staffRepository::save);
    }
}
