package com.iotech.qualitrack.platform.ra.application;

import com.iotech.qualitrack.platform.batch.interfaces.acl.BatchContextFacade;
import com.iotech.qualitrack.platform.ca.interfaces.acl.ComplianceContextFacade;
import com.iotech.qualitrack.platform.equipment.interfaces.acl.EquipmentContextFacade;
import com.iotech.qualitrack.platform.laboratory.interfaces.acl.LaboratoryContextFacade;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalBatchService;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalComplianceService;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalEquipmentService;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaExternalLaboratoryService;
import com.iotech.qualitrack.platform.ra.application.internal.outboundservices.acl.RaOperationalDataService;
import com.iotech.qualitrack.platform.ra.domain.model.entities.KpiMetric;
import com.iotech.qualitrack.platform.ra.domain.model.entities.DeviationTrend;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.EnvironmentalReading;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.MeasurementSummary;
import com.iotech.qualitrack.platform.ra.domain.model.valueobjects.ReportFormat;
import com.iotech.qualitrack.platform.ra.infrastructure.documents.ReportDocumentWriterImpl;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReportingTests {
    private final BatchContextFacade batches = mock(BatchContextFacade.class);
    private final EquipmentContextFacade equipment = mock(EquipmentContextFacade.class);
    private final ComplianceContextFacade alerts = mock(ComplianceContextFacade.class);
    private final RaOperationalDataService data = new RaOperationalDataService(new RaExternalBatchService(batches),
            new RaExternalEquipmentService(equipment), new RaExternalComplianceService(alerts),
            new RaExternalLaboratoryService(mock(LaboratoryContextFacade.class)));

    @Test void emptyRepositoryProducesNoInventedMetricOrHealthScore() {
        var dashboard = data.dashboard(47L);
        assertThat(dashboard.getMetrics()).isEmpty();
        assertThat(dashboard.getOverallHealthScore()).isNull();
        assertThat(dashboard.getLaboratoryId()).isEqualTo(47L);
    }

    @Test void countsUseOnlyTheRequestedLaboratoryRecords() {
        when(batches.findBatches(47L)).thenReturn(List.of(batch(1L, "RELEASED"), batch(2L, "PENDING")));
        var dashboard = data.dashboard(47L);
        assertThat(dashboard.getMetrics()).extracting(metric -> metric.getValue()).containsExactly(2.0, 1.0);
        assertThat(dashboard.getMetrics()).allMatch(metric -> metric.getTargetValue() == null);
        verify(batches).findBatches(47L);
        verifyNoInteractions(alerts);
    }

    @Test void unresolvedAlertsAreCountedOnlyForTheEquipmentOfTheLaboratory() {
        when(equipment.findLaboratoryEquipment(47L)).thenReturn(List.of(
                new EquipmentContextFacade.EquipmentReference(5L, 47L, "Refrigerator", "SN-5", "OPERATIONAL"),
                new EquipmentContextFacade.EquipmentReference(6L, 47L, "Incubator", "SN-6", "MAINTENANCE")));
        when(alerts.findDeviceAlerts(List.of(5L, 6L))).thenReturn(List.of(alert(1L, "UNRESOLVED"),
                alert(2L, "ACKNOWLEDGED"), alert(3L, "RESOLVED")));
        var dashboard = data.dashboard(47L);
        assertThat(dashboard.getMetrics()).extracting(KpiMetric::getName)
                .containsExactly("equipment-count", "unresolved-alert-count");
        assertThat(dashboard.getMetrics()).extracting(KpiMetric::getValue).containsExactly(2.0, 2.0);
    }

    private static BatchContextFacade.BatchReference batch(Long id, String status) {
        return new BatchContextFacade.BatchReference(id, 47L, "B-" + id, "Ibuprofen 400 mg", 100.0, "units", status,
                "2026-09-01", null, null);
    }

    private static ComplianceContextFacade.AlertReference alert(Long id, String status) {
        return new ComplianceContextFacade.AlertReference(id, 5L, "ENVIRONMENT", "TEMPERATURE", 31.0, 30.0, "°C",
                Instant.parse("2026-09-01T10:00:00Z"), "CRITICAL", status, 1, null, "2026-09-01T10:00:00Z");
    }

    @Test void timeInRangeWeighsEachEvaluatedReadingUntilTheNextOne() {
        var start = Instant.parse("2026-08-01T10:00:00Z");
        var trend = DeviationTrend.fromReadings(4L, 12L, "TEMPERATURE", "°C", List.of(
                reading("CRITICAL", 33.0, start.plusSeconds(3000)),
                reading("NORMAL", 20.0, start),
                reading("WARNING", 27.0, start.plusSeconds(600)),
                reading(null, 26.0, start.plusSeconds(900)),
                reading("NORMAL", 22.0, start.plusSeconds(1200)),
                reading("NORMAL", null, start.plusSeconds(1300)),
                reading("NORMAL", 21.0, start.plusSeconds(2400))));
        // NORMAL 0-10 min, WARNING 10-20, NORMAL 20-50: 40 of 50 minutes in range.
        assertThat(trend.getTimeInRangePercent()).isEqualTo(80.0);
        assertThat(trend.getDeviationCount()).isEqualTo(2);
        assertThat(trend.getCriticalDeviationCount()).isEqualTo(1);
        assertThat(trend.getEvaluatedReadings()).isEqualTo(5);
        assertThat(trend.getDataPoints()).extracting(point -> point.getRecordedValue()).containsExactly(20.0, 27.0, 26.0, 22.0, 21.0, 33.0);
        assertThat(trend.getTrendDirection().name()).isEqualTo("INCREASING");
    }

    @Test void aPeriodThatStartsDeviatedCountsOnceAndFewReadingsHaveNoTimeInRange() {
        var start = Instant.parse("2026-08-01T10:00:00Z");
        var single = DeviationTrend.fromReadings(4L, 12L, "TEMPERATURE", "°C", List.of(reading("WARNING", 27.0, start)));
        assertThat(single.getTimeInRangePercent()).isNull();
        assertThat(single.getDeviationCount()).isEqualTo(1);
        var empty = DeviationTrend.fromReadings(4L, 12L, "TEMPERATURE", "°C", List.of());
        assertThat(empty.getDataPoints()).isEmpty();
        assertThat(empty.getTimeInRangePercent()).isNull();
        assertThat(empty.getDeviationCount()).isZero();
    }

    @Test void summariesAverageOnlyNumericReadingsOfTheSameDeviceMetricAndUnit() {
        var start = Instant.parse("2026-08-01T10:00:00Z");
        var summaries = MeasurementSummary.summarize(4L, List.of(
                new EnvironmentalReading(12L, "TEMPERATURE", 20.0, "°C", "NORMAL", start),
                new EnvironmentalReading(12L, "TEMPERATURE", 25.0, "°C", "WARNING", start.plusSeconds(60)),
                new EnvironmentalReading(12L, "TEMPERATURE", 26.0, "°C", null, start.plusSeconds(120)),
                new EnvironmentalReading(12L, "RFID_TAG", null, "tag", null, start),
                new EnvironmentalReading(11L, "AIR_QUALITY", 350.0, "ppm", "NORMAL", start)));
        assertThat(summaries).extracting(MeasurementSummary::metric).containsExactly("AIR_QUALITY", "TEMPERATURE");
        var temperature = summaries.get(1);
        assertThat(temperature.readings()).isEqualTo(3);
        assertThat(temperature.average()).isEqualTo(23.67);
        assertThat(temperature.minimum()).isEqualTo(20.0);
        assertThat(temperature.maximum()).isEqualTo(26.0);
        assertThat(temperature.lastMeasuredAt()).isEqualTo(start.plusSeconds(120));
    }

    private static EnvironmentalReading reading(String state, Double value, Instant at) {
        return new EnvironmentalReading(12L, "TEMPERATURE", value, "°C", state, at);
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
