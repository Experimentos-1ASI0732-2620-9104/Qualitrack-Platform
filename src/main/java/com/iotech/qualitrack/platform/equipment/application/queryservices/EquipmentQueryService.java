package com.iotech.qualitrack.platform.equipment.application.queryservices;

import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetCalibrationAlertsByLabIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByDeviceIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByIdQuery;
import com.iotech.qualitrack.platform.equipment.domain.model.queries.GetEquipmentByLabIdQuery;

import java.util.List;
import java.util.Optional;

/**
 * Application service contract for equipment read queries.
 */
public interface EquipmentQueryService {

    /**
     * Handles retrieval of an equipment of a laboratory by its unique numeric ID.
     *
     * @param query laboratory and equipment ids
     * @return matching equipment, if it belongs to the laboratory
     * @see GetEquipmentByIdQuery
     */
    Optional<Equipment> handle(GetEquipmentByIdQuery query);

    /**
     * Handles retrieval of all equipment associated with a specific laboratory.
     *
     * @param query laboratory-id query
     * @return list of equipment for the given laboratory
     * @see GetEquipmentByLabIdQuery
     */
    List<Equipment> handle(GetEquipmentByLabIdQuery query);

    /**
     * Handles retrieval of an equipment linked to a specific external IoT sensor.
     *
     * @param query device-id query
     * @return matching equipment, if found
     * @see GetEquipmentByDeviceIdQuery
     */
    Optional<Equipment> handle(GetEquipmentByDeviceIdQuery query);

    /**
     * Handles retrieval of equipment that have expired or soon-to-expire calibrations
     * for a specific laboratory.
     *
     * @param query calibration-alerts query
     * @return list of equipment requiring calibration attention
     * @see GetCalibrationAlertsByLabIdQuery
     */
    List<Equipment> handle(GetCalibrationAlertsByLabIdQuery query);
}