package com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.EnvironmentalReading;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportingPeriod;
import com.iotech.qualitrack.platform.tracking.interfaces.acl.TrackingContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Anti-corruption service that reads, through Tracking &amp; Telemetry, the readings and actions of an environment.
 */
@Service
public class RaExternalTrackingService {
    private final TrackingContextFacade trackingContextFacade;

    public RaExternalTrackingService(TrackingContextFacade trackingContextFacade) {
        this.trackingContextFacade = trackingContextFacade;
    }

    /**
     * Readings of the devices of an environment in a period, oldest first.
     */
    public List<EnvironmentalReading> findReadings(Long laboratoryId, Long environmentId, ReportingPeriod period) {
        return trackingContextFacade.findMeasurements(laboratoryId, environmentId, period.from(), period.to()).stream()
                .map(reading -> new EnvironmentalReading(reading.deviceId(), reading.metric(), reading.value(),
                        reading.unit(), reading.state(), reading.measuredAt()))
                .toList();
    }

    /**
     * Actions of the container monitors of an environment in a period, oldest first.
     */
    public List<TrackingContextFacade.ActuationReference> findActuations(Long laboratoryId, Long environmentId,
                                                                        ReportingPeriod period) {
        return trackingContextFacade.findEnvironmentActuations(laboratoryId, environmentId, period.from(), period.to());
    }
}
