package com.iotech.qualitrack.platform.laboratory.application.acl;

import com.iotech.qualitrack.platform.laboratory.application.queryservices.EnvironmentQueryService;
import com.iotech.qualitrack.platform.laboratory.application.queryservices.LaboratoryQueryService;
import com.iotech.qualitrack.platform.laboratory.application.queryservices.StaffQueryService;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetEnvironmentByIdQuery;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetEnvironmentsByLaboratoryIdQuery;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetLaboratoryByIdQuery;
import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.StaffMember;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetStaffMemberByIdQuery;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetStaffMemberByUserIdQuery;
import com.iotech.qualitrack.platform.laboratory.interfaces.acl.LaboratoryContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application-layer implementation of the Laboratory ACL facade.
 */
@Service
public class LaboratoryContextFacadeImpl implements LaboratoryContextFacade {

    private final LaboratoryQueryService laboratoryQueryService;
    private final EnvironmentQueryService environmentQueryService;
    private final StaffQueryService staffQueryService;

    public LaboratoryContextFacadeImpl(LaboratoryQueryService laboratoryQueryService,
                                       EnvironmentQueryService environmentQueryService,
                                       StaffQueryService staffQueryService) {
        this.laboratoryQueryService = laboratoryQueryService;
        this.environmentQueryService = environmentQueryService;
        this.staffQueryService = staffQueryService;
    }

    @Override
    public boolean existsLaboratoryById(Long labId) {
        var query = new GetLaboratoryByIdQuery(labId);

        var result = laboratoryQueryService.handle(query);

        return result.isPresent();
    }

    @Override
    public boolean existsEnvironment(Long laboratoryId, Long environmentId) {
        if (laboratoryId == null || laboratoryId <= 0 || environmentId == null || environmentId <= 0) return false;
        return environmentQueryService.handle(new GetEnvironmentByIdQuery(laboratoryId, environmentId)).isPresent();
    }

    @Override
    public Optional<EnvironmentReference> findEnvironment(Long laboratoryId, Long environmentId) {
        if (laboratoryId == null || laboratoryId <= 0 || environmentId == null || environmentId <= 0) return Optional.empty();
        return environmentQueryService.handle(new GetEnvironmentByIdQuery(laboratoryId, environmentId))
                .map(environment -> new EnvironmentReference(environment.getId(), environment.getLaboratoryId(),
                        environment.getCode(), environment.getName(),
                        environment.getUsage() == null ? null : environment.getUsage().name()));
    }

    @Override
    public List<EnvironmentReference> findEnvironments(Long laboratoryId) {
        if (laboratoryId == null || laboratoryId <= 0) return List.of();
        return environmentQueryService.handle(new GetEnvironmentsByLaboratoryIdQuery(laboratoryId)).stream()
                .map(environment -> new EnvironmentReference(environment.getId(), environment.getLaboratoryId(),
                        environment.getCode(), environment.getName(),
                        environment.getUsage() == null ? null : environment.getUsage().name()))
                .toList();
    }

    @Override
    public Optional<StaffReference> findStaffMember(Long laboratoryId, Long staffId) {
        if (laboratoryId == null || laboratoryId <= 0 || staffId == null || staffId <= 0) return Optional.empty();
        return staffQueryService.handle(new GetStaffMemberByIdQuery(laboratoryId, staffId)).map(LaboratoryContextFacadeImpl::toReference);
    }

    @Override
    public Optional<StaffReference> findStaffMemberByAccount(Long userId) {
        if (userId == null || userId <= 0) return Optional.empty();
        return staffQueryService.handle(new GetStaffMemberByUserIdQuery(userId)).map(LaboratoryContextFacadeImpl::toReference);
    }

    private static StaffReference toReference(StaffMember member) {
        return new StaffReference(member.getId(), member.getLaboratoryId(), member.getFullName(), member.getRole(),
                member.isActive(), member.getUserId(), member.getAccessRole() == null ? null : member.getAccessRole().name());
    }
}
