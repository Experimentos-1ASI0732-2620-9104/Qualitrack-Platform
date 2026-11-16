package com.iotech.qualitrack.platform.tracking.application.queryservices;

import com.iotech.qualitrack.platform.tracking.domain.model.aggregates.EnvironmentalProfile;
import com.iotech.qualitrack.platform.tracking.domain.model.entities.ActuationEvent;
import com.iotech.qualitrack.platform.tracking.domain.model.entities.Measurement;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetActuationEventsQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetContainerMonitorProfileQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetDeviceConnectionQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetDeviceProfileQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetEnvironmentProfileQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.queries.GetMeasurementsQuery;
import com.iotech.qualitrack.platform.tracking.domain.model.valueobjects.DeviceConnection;

import java.util.List;
import java.util.Optional;

/**
 * Queries of Tracking &amp; Telemetry. An empty result means the environment, device or profile is not available;
 * an empty list inside a present result means there are no records in the period.
 */
public interface TrackingQueryService {

    /**
     * @return the profile of the environment; empty when it has not been configured or belongs to another laboratory
     */
    Optional<EnvironmentalProfile> handle(GetEnvironmentProfileQuery query);

    /**
     * @return the profile of the container monitor; empty when the device is not a container monitor of the
     * environment or has no profile yet
     */
    Optional<EnvironmentalProfile> handle(GetContainerMonitorProfileQuery query);

    /**
     * @return the profile the device applies (the environment's for the environmental device, its own for a
     * container monitor); empty when the device is not in the environment or there is no profile yet
     */
    Optional<EnvironmentalProfile> handle(GetDeviceProfileQuery query);

    /**
     * @return the readings of the period, oldest first (possibly none); empty when the device is not available
     */
    Optional<List<Measurement>> handle(GetMeasurementsQuery query);

    /**
     * @return the actions of the period, oldest first (possibly none); empty when the device is not a container
     * monitor of the environment
     */
    Optional<List<ActuationEvent>> handle(GetActuationEventsQuery query);

    /**
     * @return the connection state, derived from the latest reading or action received; empty when the device is
     * not an IoT device of the environment
     */
    Optional<DeviceConnection> handle(GetDeviceConnectionQuery query);
}
