package com.iotech.qualitrack.platform.laboratory.application.internal.commandservices;

import com.iotech.qualitrack.platform.laboratory.application.commandservices.EnvironmentCommandService;
import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.Environment;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.AssignEnvironmentUsageCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.RegisterEnvironmentCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.commands.UpdateEnvironmentCommand;
import com.iotech.qualitrack.platform.laboratory.domain.model.events.EnvironmentRegisteredEvent;
import com.iotech.qualitrack.platform.laboratory.domain.model.events.EnvironmentUpdatedEvent;
import com.iotech.qualitrack.platform.laboratory.domain.model.events.EnvironmentUsageAssignedEvent;
import com.iotech.qualitrack.platform.laboratory.domain.repositories.EnvironmentRepository;
import com.iotech.qualitrack.platform.laboratory.domain.repositories.LaboratoryRepository;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import com.iotech.qualitrack.platform.shared.application.security.CurrentUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Application service implementation that executes environment commands.
 *
 * <p>Enforces laboratory existence, uniqueness of the environment code inside its
 * laboratory and valid usage transitions, publishing the corresponding domain events
 * after each change is persisted.</p>
 */
@Service
public class EnvironmentCommandServiceImpl implements EnvironmentCommandService {

    private final EnvironmentRepository environmentRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CurrentUser currentUser;

    public EnvironmentCommandServiceImpl(EnvironmentRepository environmentRepository,
                                         LaboratoryRepository laboratoryRepository,
                                         ApplicationEventPublisher eventPublisher,
                                         CurrentUser currentUser) {
        this.environmentRepository = environmentRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.eventPublisher = eventPublisher;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional
    public Result<Environment, ApplicationError> handle(RegisterEnvironmentCommand command) {
        if (!laboratoryRepository.existsById(command.laboratoryId())) {
            return Result.failure(ApplicationError.notFound("Laboratory", command.laboratoryId()));
        }
        try {
            var environment = new Environment(command);
            if (environmentRepository.existsByLaboratoryIdAndCode(environment.getLaboratoryId(), environment.getCode())) {
                return Result.failure(duplicateCode(environment.getCode()));
            }
            var saved = environmentRepository.save(environment);
            eventPublisher.publishEvent(EnvironmentRegisteredEvent.from(saved));
            return Result.success(saved);
        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("Environment", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public Result<Environment, ApplicationError> handle(UpdateEnvironmentCommand command) {
        return environmentRepository.findByIdAndLaboratoryId(command.environmentId(), command.laboratoryId())
                .map(environment -> {
                    try {
                        environment.update(command);
                        if (environmentRepository.existsByLaboratoryIdAndCodeAndIdNot(
                                environment.getLaboratoryId(), environment.getCode(), environment.getId())) {
                            return Result.<Environment, ApplicationError>failure(duplicateCode(environment.getCode()));
                        }
                        var saved = environmentRepository.save(environment);
                        eventPublisher.publishEvent(EnvironmentUpdatedEvent.from(saved));
                        return Result.<Environment, ApplicationError>success(saved);
                    } catch (IllegalArgumentException e) {
                        return Result.<Environment, ApplicationError>failure(
                                ApplicationError.validationError("Environment", e.getMessage()));
                    }
                })
                .orElseGet(() -> Result.failure(ApplicationError.notFound("Environment", command.environmentId())));
    }

    @Override
    @Transactional
    public Result<Environment, ApplicationError> handle(AssignEnvironmentUsageCommand command) {
        return environmentRepository.findByIdAndLaboratoryId(command.environmentId(), command.laboratoryId())
                .map(environment -> {
                    try {
                        environment.assignUsage(command.usage(), currentUser.userId(), Instant.now());
                    } catch (IllegalStateException e) {
                        return Result.<Environment, ApplicationError>failure(
                                ApplicationError.conflict("Environment", e.getMessage()));
                    }
                    var saved = environmentRepository.save(environment);
                    eventPublisher.publishEvent(EnvironmentUsageAssignedEvent.from(saved));
                    return Result.<Environment, ApplicationError>success(saved);
                })
                .orElseGet(() -> Result.failure(ApplicationError.notFound("Environment", command.environmentId())));
    }

    private static ApplicationError duplicateCode(String code) {
        return ApplicationError.conflict("Environment",
                "An environment with code '%s' already exists in this laboratory".formatted(code));
    }
}
