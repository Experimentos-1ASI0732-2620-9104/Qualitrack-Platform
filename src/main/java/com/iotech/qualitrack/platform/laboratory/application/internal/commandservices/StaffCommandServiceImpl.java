package com.iotech.qualitrack.platform.laboratory.application.internal.commandservices;

import com.iotech.qualitrack.platform.laboratory.application.commandservices.StaffCommandService;
import com.iotech.qualitrack.platform.laboratory.application.internal.outboundservices.acl.ExternalIamService;
import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.StaffMember;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.DeactivateStaffCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.RegisterStaffCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.events.StaffDeactivatedEvent;
import com.iotech.qualitrack.platform.laboratory.domain.model.events.StaffRegisteredEvent;
import com.iotech.qualitrack.platform.laboratory.domain.repositories.LaboratoryRepository;
import com.iotech.qualitrack.platform.laboratory.domain.repositories.StaffRepository;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service implementation that executes staff commands.
 *
 * <p>Registers staff members together with the account they use to sign in, and deactivates them together
 * with that account, enforcing laboratory existence and e-mail uniqueness.</p>
 */
@Service
public class StaffCommandServiceImpl implements StaffCommandService {

    private final StaffRepository staffRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final ExternalIamService externalIamService;
    private final ApplicationEventPublisher eventPublisher;

    public StaffCommandServiceImpl(StaffRepository staffRepository,
                                   LaboratoryRepository laboratoryRepository,
                                   ExternalIamService externalIamService,
                                   ApplicationEventPublisher eventPublisher) {
        this.staffRepository = staffRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.externalIamService = externalIamService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Result<RegisteredStaff, ApplicationError> handle(RegisterStaffCommand command) {
        if (!laboratoryRepository.existsById(command.laboratoryId())) {
            return Result.failure(ApplicationError.notFound("Laboratory", command.laboratoryId()));
        }
        if (staffRepository.existsByEmail(command.email()) || externalIamService.existsAccount(command.email())) {
            return Result.failure(ApplicationError.conflict("StaffMember",
                    "The e-mail '%s' is already registered".formatted(command.email())));
        }
        var staffMember = staffRepository.save(new StaffMember(command));
        // The account is created last: the credentials are sent only when everything else was accepted.
        var account = externalIamService.createAccount(command.laboratoryId(), command.email(), command.fullName(),
                command.accessRole());
        staffMember.linkAccount(account.userId());
        var savedStaff = staffRepository.save(staffMember);
        eventPublisher.publishEvent(new StaffRegisteredEvent(
                savedStaff.getId(),
                savedStaff.getLaboratoryId(),
                savedStaff.getFullName(),
                savedStaff.getRole(),
                savedStaff.getEmail()
        ));
        return Result.success(new RegisteredStaff(savedStaff, account));
    }

    @Override
    @Transactional
    public Result<StaffMember, ApplicationError> handle(DeactivateStaffCommand command) {
        var found = staffRepository.findById(command.staffMemberId()).filter(staff -> staff.belongsTo(command.laboratoryId()));
        if (found.isEmpty()) {
            return Result.failure(ApplicationError.notFound("StaffMember", command.staffMemberId()));
        }
        var staff = found.get();
        if (!staff.isActive()) {
            return Result.failure(ApplicationError.conflict("StaffMember", "Staff member is already inactive"));
        }
        staff.deactivate();
        var savedStaff = staffRepository.save(staff);
        externalIamService.disableAccount(savedStaff.getUserId());
        eventPublisher.publishEvent(StaffDeactivatedEvent.from(savedStaff));
        return Result.success(savedStaff);
    }
}
