package com.iotech.qualitrack.platform.ca.domain.repositories;

import com.iotech.qualitrack.platform.ca.domain.model.aggregates.DeviationAlert;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertSeverity;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.AlertStatus;

import java.util.List;
import java.util.Optional;

/**
 * DeviationAlert repository port.
 *
 * <p>Handles the persistence contract for the DeviationAlert aggregate.</p>
 */
public interface DeviationAlertRepository {

    Optional<DeviationAlert> findById(Long id);

    List<DeviationAlert> findAll();

    List<DeviationAlert> findAllByEquipmentId(Long equipmentId);

    List<DeviationAlert> findAllByBatchId(Long batchId);

    List<DeviationAlert> findAllByStatus(AlertStatus status);

    List<DeviationAlert> findAllBySeverity(AlertSeverity severity);

    /**
     * Alerts of an environment and its monitored containers, newest first.
     */
    List<DeviationAlert> findAllByLaboratoryIdAndEnvironmentId(Long laboratoryId, Long environmentId);

    /**
     * The open alert (unresolved or acknowledged) of the incident of a device and parameter, if any.
     */
    Optional<DeviationAlert> findOpenByEquipmentIdAndParameterName(Long equipmentId, String parameterName);

    DeviationAlert save(DeviationAlert deviationAlert);

    boolean existsById(Long id);

    void deleteById(Long id);
}