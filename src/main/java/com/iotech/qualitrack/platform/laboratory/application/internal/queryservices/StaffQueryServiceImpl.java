package com.iotech.qualitrack.platform.laboratory.application.internal.queryservices;

import com.iotech.qualitrack.platform.laboratory.application.queryservices.StaffQueryService;
import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.StaffMember;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetStaffByLabIdQuery;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetStaffMemberByIdQuery;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetStaffMemberByUserIdQuery;
import com.iotech.qualitrack.platform.laboratory.domain.repositories.StaffRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Application service implementation that resolves staff member read queries.
 */
@Service
public class StaffQueryServiceImpl implements StaffQueryService {
    private final StaffRepository staffRepository;

    public StaffQueryServiceImpl(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @Override
    public List<StaffMember> handle(GetStaffByLabIdQuery query) {
        return staffRepository.findAllByLaboratoryId(query.laboratoryId());
    }

    @Override
    public Optional<StaffMember> handle(GetStaffMemberByIdQuery query) {
        return staffRepository.findById(query.staffId()).filter(staff -> staff.belongsTo(query.laboratoryId()));
    }

    @Override
    public Optional<StaffMember> handle(GetStaffMemberByUserIdQuery query) {
        return staffRepository.findByUserId(query.userId());
    }
}
