package com.iotech.qualitrack.platform.profile.application.internal.commandservices;

import com.iotech.qualitrack.platform.profile.application.commandservices.ProfileCommandService;
import com.iotech.qualitrack.platform.profile.application.internal.outboundservices.acl.ExternalIamService;
import com.iotech.qualitrack.platform.profile.application.internal.outboundservices.acl.ExternalLaboratoryService;
import com.iotech.qualitrack.platform.profile.application.internal.outboundservices.storage.ProfilePhotoStorage;
import com.iotech.qualitrack.platform.profile.domain.model.aggregates.Profile;
import com.iotech.qualitrack.platform.profile.domain.model.commands.ChangeProfilePhotoCommand;
import com.iotech.qualitrack.platform.profile.domain.model.commands.RemoveProfilePhotoCommand;
import com.iotech.qualitrack.platform.profile.domain.model.commands.UpdateProfileCommand;
import com.iotech.qualitrack.platform.profile.domain.model.events.ProfileUpdatedEvent;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PersonName;
import com.iotech.qualitrack.platform.profile.domain.repositories.ProfileRepository;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Changes profiles, creating the profile of an account the first time it is saved.
 */
@Service
public class ProfileCommandServiceImpl implements ProfileCommandService {

    private final ProfileRepository profileRepository;
    private final ProfilePhotoStorage photoStorage;
    private final ExternalIamService externalIamService;
    private final ExternalLaboratoryService externalLaboratoryService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock profileClock;

    public ProfileCommandServiceImpl(ProfileRepository profileRepository, ProfilePhotoStorage photoStorage,
                                     ExternalIamService externalIamService,
                                     ExternalLaboratoryService externalLaboratoryService,
                                     ApplicationEventPublisher eventPublisher, Clock profileClock) {
        this.profileRepository = profileRepository;
        this.photoStorage = photoStorage;
        this.externalIamService = externalIamService;
        this.externalLaboratoryService = externalLaboratoryService;
        this.eventPublisher = eventPublisher;
        this.profileClock = profileClock;
    }

    @Override
    @Transactional
    public Result<Profile, ApplicationError> handle(UpdateProfileCommand command) {
        var current = profileOf(command.userId());
        if (current.isEmpty()) return Result.failure(ApplicationError.notFound("User", command.userId()));
        var profile = current.get();
        var renamed = profile.update(command.fullName(), command.dni(), command.phoneNumber(), command.location());
        var saved = profileRepository.save(profile);
        if (renamed) eventPublisher.publishEvent(new ProfileUpdatedEvent(saved.getUserId(), saved.getFullNameValue()));
        return Result.success(saved);
    }

    @Override
    @Transactional
    public Result<Profile, ApplicationError> handle(ChangeProfilePhotoCommand command) {
        var current = profileOf(command.userId());
        if (current.isEmpty()) return Result.failure(ApplicationError.notFound("User", command.userId()));
        var profile = current.get();
        profile.changePhoto(command.image(), Instant.now(profileClock));
        var saved = profileRepository.save(profile);
        photoStorage.store(saved.getId(), command.image());
        return Result.success(saved);
    }

    @Override
    @Transactional
    public Result<Profile, ApplicationError> handle(RemoveProfilePhotoCommand command) {
        var current = profileOf(command.userId());
        if (current.isEmpty()) return Result.failure(ApplicationError.notFound("User", command.userId()));
        var profile = current.get();
        if (!profile.removePhoto()) return Result.success(profile);
        var saved = profileRepository.save(profile);
        photoStorage.remove(saved.getId());
        return Result.success(saved);
    }

    /**
     * The stored profile of the account, or a new one that starts with the name the quality manager registered when
     * the account belongs to a staff member. Empty when the account does not exist.
     */
    private Optional<Profile> profileOf(Long userId) {
        var stored = profileRepository.findByUserId(userId);
        if (stored.isPresent()) return stored;
        if (externalIamService.findAccount(userId).isEmpty()) return Optional.empty();
        var registeredName = externalLaboratoryService.findStaffMemberByAccount(userId)
                .flatMap(member -> PersonName.tryParse(member.fullName()))
                .orElse(null);
        return Optional.of(new Profile(userId, registeredName));
    }
}
