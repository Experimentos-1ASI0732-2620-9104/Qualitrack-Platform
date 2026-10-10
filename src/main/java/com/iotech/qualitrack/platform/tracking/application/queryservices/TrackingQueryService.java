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
 * Queries of Tracking &amp; Telemetry. An empty result means the environment, device or profile is not available.
 */
public interface TrackingQueryService {

    Optional<EnvironmentalProfile> handle(GetEnvironmentProfileQuery query);

    Optional<EnvironmentalProfile> handle(GetContainerMonitorProfileQuery query);

    Optional<EnvironmentalProfile> handle(GetDeviceProfileQuery query);

    Optional<List<Measurement>> handle(GetMeasurementsQuery query);

    Optional<List<ActuationEvent>> handle(GetActuationEventsQuery query);

    Optional<DeviceConnection> handle(GetDeviceConnectionQuery query);
}
