package com.iotech.qualitrack.platform.batch.application.commandservices;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.PharmaceuticalProduct;
import com.iotech.qualitrack.platform.batch.domain.model.commands.CreateProductCommand;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;

/**
 * Application service for pharmaceutical product registration.
 */
public interface ProductCommandService {
    /**
     * Registers a product in an environment of the laboratory (US71, TS61).
     *
     * @param command the registration data
     * @return the registered product, or NOT_FOUND when the environment is not in the laboratory,
     * CONFLICT when the code or name is already used in the laboratory
     */
    Result<PharmaceuticalProduct, ApplicationError> handle(CreateProductCommand command);
}
