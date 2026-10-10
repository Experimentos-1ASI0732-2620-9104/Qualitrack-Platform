package com.iotech.qualitrack.platform;

import com.iotech.qualitrack.platform.ca.application.internal.outboundservices.acl.CaExternalTrackingService;
import com.iotech.qualitrack.platform.ca.application.internal.queryservices.CaQueryServiceImpl;
import com.iotech.qualitrack.platform.ca.domain.model.entities.ComplianceEvent;
import com.iotech.qualitrack.platform.ca.domain.model.queries.GetComplianceEventsByRelatedEntityIdQuery;
import com.iotech.qualitrack.platform.ca.domain.model.valueobjects.*;
import com.iotech.qualitrack.platform.ca.domain.repositories.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class ComplianceScopeTests {
    @Test void matchingNumericIdsDoNotMixEventsFromDifferentResourceTypes() {
        var events = mock(ComplianceEventRepository.class);
        var service = new CaQueryServiceImpl(mock(DeviationAlertRepository.class), events, mock(NotificationPreferenceRepository.class),
                mock(CaExternalTrackingService.class));
        when(events.findAllByRelatedEntityId(42L)).thenReturn(List.of(
                new ComplianceEvent(42L, ComplianceEventType.BATCH_RELEASED, "Batch record", "2026-09-01", 27L),
                new ComplianceEvent(42L, ComplianceEventType.RAW_MATERIAL_LOW_STOCK, "Other tenant material", "2026-09-01", null),
                new ComplianceEvent(42L, ComplianceEventType.NOTIFICATION_PREFERENCE_UPDATED, "Other user", "2026-09-01", 42L)));
        var result = service.handle(new GetComplianceEventsByRelatedEntityIdQuery(42L, ComplianceEventSubject.BATCH));
        assertThat(result).extracting(ComplianceEvent::getDescription).containsExactly("Batch record");
    }
}
