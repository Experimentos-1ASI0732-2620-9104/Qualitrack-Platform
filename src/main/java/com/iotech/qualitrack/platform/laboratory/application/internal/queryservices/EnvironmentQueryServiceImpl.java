package com.iotech.qualitrack.platform.laboratory.application.internal.queryservices;

import com.iotech.qualitrack.platform.laboratory.application.queryservices.EnvironmentQueryService;
import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.Environment;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetEnvironmentByIdQuery;
import com.iotech.qualitrack.platform.laboratory.domain.model.queries.GetEnvironmentsByLaboratoryIdQuery;
import com.iotech.qualitrack.platform.laboratory.domain.repositories.EnvironmentRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Application service implementation that resolves environment read queries.
 */
@Service
public class EnvironmentQueryServiceImpl implements EnvironmentQueryService {

    private final EnvironmentRepository environmentRepository;

    public EnvironmentQueryServiceImpl(EnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
    }

    @Override
    public List<Environment> handle(GetEnvironmentsByLaboratoryIdQuery query) {
        return environmentRepository.findAllByLaboratoryId(query.laboratoryId()).stream()
                .sorted(Comparator.comparing(Environment::getCode))
                .toList();
    }

    @Override
    public Optional<Environment> handle(GetEnvironmentByIdQuery query) {
        return environmentRepository.findByIdAndLaboratoryId(query.environmentId(), query.laboratoryId());
    }
}
