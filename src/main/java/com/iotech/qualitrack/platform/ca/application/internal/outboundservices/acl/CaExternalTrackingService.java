package com.iotech.qualitrack.platform.ca.application.internal.outboundservices.acl;

import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.RelatedActuation;
import com.iotech.qualitrack.platform.tracking.interfaces.acl.TrackingContextFacade;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Anti-corruption service that reads, through Tracking &amp; Telemetry, the actions related to an alert.
 */
@Service
public class CaExternalTrackingService {

    private final TrackingContextFacade trackingContextFacade;

    public CaExternalTrackingService(TrackingContextFacade trackingContextFacade) {
        this.trackingContextFacade = trackingContextFacade;
    }

    /**
     * Actions a device executed for a metric in a period, oldest first.
     */
    public List<RelatedActuation> findActuations(Long laboratoryId, Long deviceId, String metric, Instant from, Instant to) {
        return trackingContextFacade.findActuations(laboratoryId, deviceId, metric, from, to).stream()
                .map(action -> new RelatedActuation(action.id(), action.action(), action.triggerState(), action.result(),
                        action.occurredAt()))
                .toList();
    }
}
