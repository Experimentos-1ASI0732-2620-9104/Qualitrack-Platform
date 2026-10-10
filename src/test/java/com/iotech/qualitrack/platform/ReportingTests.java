package com.iotech.qualitrack.platform;

import com.iotech.qualitrack.platform.batch.domain.model.aggregates.Batch;
import com.iotech.qualitrack.platform.batch.domain.model.valueobjects.BatchStatus;
import com.iotech.qualitrack.platform.batch.domain.repositories.BatchRepository;
import com.iotech.qualitrack.platform.ca.domain.repositories.DeviationAlertRepository;
import com.iotech.qualitrack.platform.equipment.domain.model.entities.BpmParameterConfig;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.CriticalVariable;
import com.iotech.qualitrack.platform.equipment.domain.repositories.*;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaOperationalDataService;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;
import com.iotech.qualitrack.platform.ra.infrastructure.documents.ReportDocumentWriterImpl;
import com.iotech.qualitrack.platform.equipment.domain.model.aggregates.Equipment;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentStatus;
import com.iotech.qualitrack.platform.equipment.domain.model.valueobjects.EquipmentType;
import com.iotech.qualitrack.platform.laboratory.domain.model.aggregates.Laboratory;
import com.iotech.qualitrack.platform.laboratory.domain.model.valueobjects.LaboratoryName;
import com.iotech.qualitrack.platform.laboratory.domain.repositories.LaboratoryRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReportingTests {
    private final EquipmentRepository equipment = mock(EquipmentRepository.class);
    private final BatchRepository batches = mock(BatchRepository.class);
    private final DeviationAlertRepository alerts = mock(DeviationAlertRepository.class);
    private final BpmParameterConfigRepository configs = mock(BpmParameterConfigRepository.class);
    private final LaboratoryRepository laboratories = mock(LaboratoryRepository.class);
    private final RaOperationalDataService data = new RaOperationalDataService(equipment, batches, alerts, configs,
            mock(com.iotech.qualitrack.platform.batch.domain.repositories.RawMaterialUsageRepository.class),
            laboratories,
            mock(MaintenanceRepository.class));

    @Test void emptyRepositoryProducesNoInventedMetricOrHealthScore() {
        var dashboard = data.dashboard(47L);
        assertThat(dashboard.getMetrics()).isEmpty();
        assertThat(dashboard.getOverallHealthScore()).isNull();
        assertThat(dashboard.getLaboratoryId()).isEqualTo(47L);
    }

    @Test void countsUseOnlyTheRequestedLaboratoryRecords() {
        var released = mock(Batch.class);
        when(released.getStatus()).thenReturn(BatchStatus.RELEASED);
        var pending = mock(Batch.class);
        when(pending.getStatus()).thenReturn(BatchStatus.PENDING);
        when(batches.findAllByLabId(47L)).thenReturn(List.of(released, pending));
        var dashboard = data.dashboard(47L);
        assertThat(dashboard.getMetrics()).extracting(metric -> metric.getValue()).containsExactly(2.0, 1.0);
        assertThat(dashboard.getMetrics()).allMatch(metric -> metric.getTargetValue() == null);
        verify(batches).findAllByLabId(47L);
        verify(alerts, never()).findAll();
    }

    @Test void equipmentReportUsesActualConfiguredLimits() {
        var device = device();
        when(configs.findAllByEquipmentId(23L)).thenReturn(List.of(
                new BpmParameterConfig(9L, 23L, new CriticalVariable("Temperature"), 18.0, 24.0, "C")));
        var report = data.equipmentReport(device, "2026-08-01", "2026-08-02", List.of(), "Billy1", Instant.parse("2026-08-02T10:00:00Z"));
        assertThat(report.parameters()).singleElement().satisfies(parameter -> {
            assertThat(parameter.name()).isEqualTo("Temperature");
            assertThat(parameter.minimum()).isEqualTo(18.0);
            assertThat(parameter.maximum()).isEqualTo(24.0);
            assertThat(parameter.unit()).isEqualTo("C");
        });
    }

    @Test void noConfigurationDoesNotCreateFakeLimits() {
        var report = data.equipmentReport(device(), "2026-08-01", "2026-08-02", List.of(), "Billy1", Instant.parse("2026-08-02T10:00:00Z"));
        assertThat(report.parameters()).isEmpty();
        assertThat(report.maintenance()).isEmpty();
        assertThat(report.entries()).isEmpty();
    }

    private Equipment device() {
        var device = mock(Equipment.class);
        when(device.getId()).thenReturn(23L);
        when(device.getName()).thenReturn("Cold chamber");
        when(device.getLabId()).thenReturn(47L);
        when(device.getType()).thenReturn(new EquipmentType("Refrigerator"));
        when(device.getStatus()).thenReturn(EquipmentStatus.OPERATIONAL);
        var laboratory = mock(Laboratory.class);
        when(laboratory.getName()).thenReturn(new LaboratoryName("Multilab"));
        when(laboratories.findById(47L)).thenReturn(Optional.of(laboratory));
        return device;
    }

    @Test void pdfIsReadableMultipageAndDoesNotDropTheLastRow() throws Exception {
        var rows = IntStream.range(0, 120).mapToObj(index -> List.of("Record " + index, "Temperatura y validación")).toList();
        var bytes = new ReportDocumentWriterImpl().write(ReportFormat.PDF, "QualiTrack", rows);
        try (var pdf = Loader.loadPDF(bytes)) {
            assertThat(pdf.getNumberOfPages()).isGreaterThan(1);
            assertThat(new PDFTextStripper().getText(pdf)).contains("Record 119", "validación");
        }
    }

    @Test void csvEscapesQuotesCommasAndSpreadsheetFormulas() {
        var bytes = new ReportDocumentWriterImpl().write(ReportFormat.CSV, "Test",
                List.of(List.of("label", "one,\"two\""), List.of("formula", "=1+2")));
        assertThat(new String(bytes, StandardCharsets.UTF_8)).contains("\"one,\"\"two\"\"\"", "\"'=1+2\"");
    }
}
