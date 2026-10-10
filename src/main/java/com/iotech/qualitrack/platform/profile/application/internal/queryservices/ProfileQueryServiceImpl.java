package com.iotech.qualitrack.platform.profile.application.internal.queryservices;

import com.iotech.qualitrack.platform.profile.application.internal.outboundservices.acl.ExternalIamService;
import com.iotech.qualitrack.platform.profile.application.internal.outboundservices.acl.ExternalLaboratoryService;
import com.iotech.qualitrack.platform.profile.application.internal.outboundservices.storage.ProfilePhotoStorage;
import com.iotech.qualitrack.platform.profile.application.queryservices.ProfileQueryService;
import com.iotech.qualitrack.platform.profile.domain.model.aggregates.Profile;
import com.iotech.qualitrack.platform.profile.domain.model.queries.GetProfileByUserIdQuery;
import com.iotech.qualitrack.platform.profile.domain.model.queries.GetProfilePhotoQuery;
import com.iotech.qualitrack.platform.profile.domain.model.queries.GetStaffProfilePhotoQuery;
import com.iotech.qualitrack.platform.profile.domain.model.queries.GetStaffProfileQuery;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PersonName;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.PhotoImage;
import com.iotech.qualitrack.platform.profile.domain.model.valueobjects.ProfileDetail;
import com.iotech.qualitrack.platform.profile.domain.repositories.ProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Reads profiles with the account they belong to.
 */
@Service
public class ProfileQueryServiceImpl implements ProfileQueryService {

    private final ProfileRepository profileRepository;
    private final ProfilePhotoStorage photoStorage;
    private final ExternalIamService externalIamService;
    private final ExternalLaboratoryService externalLaboratoryService;

    public ProfileQueryServiceImpl(ProfileRepository profileRepository, ProfilePhotoStorage photoStorage,
                                   ExternalIamService externalIamService,
                                   ExternalLaboratoryService externalLaboratoryService) {
        this.profileRepository = profileRepository;
        this.photoStorage = photoStorage;
        this.externalIamService = externalIamService;
        this.externalLaboratoryService = externalLaboratoryService;
    }

    @Override
    public Optional<ProfileDetail> handle(GetProfileByUserIdQuery query) {
        var account = externalIamService.findAccount(query.userId());
        if (account.isEmpty()) return Optional.empty();
        var staffMember = externalLaboratoryService.findStaffMemberByAccount(query.userId());
        var profile = profileRepository.findByUserId(query.userId()).orElseGet(() -> new Profile(query.userId(),
                staffMember.flatMap(member -> PersonName.tryParse(member.fullName())).orElse(null)));
        return Optional.of(new ProfileDetail(profile, account.get().username(), account.get().email(),
                account.get().roles(), staffMember.map(ExternalLaboratoryService.StaffMember::staffId).orElse(null),
                staffMember.map(ExternalLaboratoryService.StaffMember::position).orElse(null)));
    }

    @Override
    public Optional<PhotoImage> handle(GetProfilePhotoQuery query) {
        return profileRepository.findByUserId(query.userId())
                .filter(Profile::hasPhoto)
                .flatMap(profile -> photoStorage.find(profile.getId()));
    }

    @Override
    public Optional<ProfileDetail> handle(GetStaffProfileQuery query) {
        return accountOf(query.laboratoryId(), query.staffId()).flatMap(userId -> handle(new GetProfileByUserIdQuery(userId)));
    }

    @Override
    public Optional<PhotoImage> handle(GetStaffProfilePhotoQuery query) {
        return accountOf(query.laboratoryId(), query.staffId()).flatMap(userId -> handle(new GetProfilePhotoQuery(userId)));
    }

    private Optional<Long> accountOf(Long laboratoryId, Long staffId) {
        return externalLaboratoryService.findStaffMember(laboratoryId, staffId)
                .map(ExternalLaboratoryService.StaffMember::userId);
    }
}
