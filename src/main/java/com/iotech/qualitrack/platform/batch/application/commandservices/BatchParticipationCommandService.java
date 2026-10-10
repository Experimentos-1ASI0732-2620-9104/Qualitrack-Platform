package com.iotech.qualitrack.platform.batch.application.commandservices;

import com.iotech.qualitrack.platform.batch.domain.model.commands.RegisterEquipmentUsageCommand;
import com.iotech.qualitrack.platform.batch.domain.model.commands.RegisterStaffParticipationCommand;
import com.iotech.qualitrack.platform.batch.domain.model.entities.EquipmentUsage;
import com.iotech.qualitrack.platform.batch.domain.model.entities.StaffParticipation;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;

/**
 * Registers the equipment and staff that take part in the manufacturing of a product batch.
 */
public interface BatchParticipationCommandService {

    /**
     * Associates an equipment of the laboratory with an open batch (US76, TS66).
     *
     * @return the usage; NOT_FOUND when the batch or the equipment does not exist, CONFLICT when the batch is
     * closed, the equipment is not operational or it was already associated with the batch
     */
    Result<EquipmentUsage, ApplicationError> handle(RegisterEquipmentUsageCommand command);

    /**
     * Associates a staff member of the laboratory with an open batch (US77, TS67).
     *
     * @return the participation; NOT_FOUND when the batch or the staff member does not exist, CONFLICT when the
     * batch is closed or the staff member was already associated with the batch
     */
    Result<StaffParticipation, ApplicationError> handle(RegisterStaffParticipationCommand command);
}
