package com.iotech.qualitrack.platform.batch.application.internal.commandservices;

import com.iotech.qualitrack.platform.batch.application.commandservices.ProductCommandService;
import com.iotech.qualitrack.platform.batch.application.internal.outboundservices.acl.ExternalLaboratoryService;
import com.iotech.qualitrack.platform.batch.domain.model.aggregates.PharmaceuticalProduct;
import com.iotech.qualitrack.platform.batch.domain.model.commands.CreateProductCommand;
import com.iotech.qualitrack.platform.batch.domain.model.events.ProductCreatedEvent;
import com.iotech.qualitrack.platform.batch.domain.repositories.ProductRepository;
import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.application.result.Result;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers pharmaceutical products in the environments of a laboratory.
 */
@Service
public class ProductCommandServiceImpl implements ProductCommandService {

    private final ProductRepository productRepository;
    private final ExternalLaboratoryService externalLaboratoryService;
    private final ApplicationEventPublisher eventPublisher;

    public ProductCommandServiceImpl(ProductRepository productRepository,
                                     ExternalLaboratoryService externalLaboratoryService,
                                     ApplicationEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.externalLaboratoryService = externalLaboratoryService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Result<PharmaceuticalProduct, ApplicationError> handle(CreateProductCommand command) {
        if (!externalLaboratoryService.existsEnvironment(command.laboratoryId(), command.environmentId())) {
            return Result.failure(ApplicationError.notFound("Environment", command.environmentId()));
        }
        if (productRepository.existsByLaboratoryIdAndCode(command.laboratoryId(), command.code())) {
            return Result.failure(ApplicationError.conflict("PharmaceuticalProduct",
                    "Product with code '%s' already exists in this laboratory".formatted(command.code())));
        }
        if (productRepository.findByNameAndLaboratoryId(command.name(), command.laboratoryId()).isPresent()) {
            return Result.failure(ApplicationError.conflict("PharmaceuticalProduct",
                    "Product with name '%s' already exists in this laboratory".formatted(command.name())));
        }
        var product = productRepository.save(new PharmaceuticalProduct(command));
        eventPublisher.publishEvent(ProductCreatedEvent.from(product));
        return Result.success(product);
    }
}
