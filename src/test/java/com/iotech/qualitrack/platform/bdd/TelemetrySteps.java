package com.iotech.qualitrack.platform.bdd;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TelemetrySteps {
    private final ScenarioContext context;

    public TelemetrySteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("the equipment sent the following measurements:")
    public void theEquipmentSentTheFollowingMeasurements(DataTable table) throws Exception {
        for (var row : table.asMaps()) {
            record(row.get("parameter"), row.get("value"), row.get("timestamp"));
        }
    }

    @Given("the equipment has the following telemetry history:")
    public void theEquipmentHasTheFollowingTelemetryHistory(DataTable table) throws Exception {
        var thresholds = context.send("PUT", telemetry("/environmental-profile/thresholds"), context.manager.token(), """
                {"thresholds":[{"metric":"TEMPERATURE","normalMin":2.0,"normalMax":8.0,"criticalMin":0.0,"criticalMax":10.0}]}
                """);
        assertThat(thresholds.statusCode()).withFailMessage(thresholds.body()).isEqualTo(200);
        for (var row : table.asMaps()) {
            record(row.get("parameter"), row.get("value"), row.get("timestamp"));
        }
    }

    @When("I consult the latest measurements of the equipment")
    public void iConsultTheLatestMeasurements() throws Exception {
        context.lastResponse = context.send("GET",
                telemetry("/telemetry-measurements?from=2026-05-12T00:00:00Z&to=2026-05-13T00:00:00Z"),
                context.manager.token(), null);
        assertThat(context.lastResponse.statusCode()).isEqualTo(200);
    }

    @When("I consult the telemetry history from {string} to {string}")
    public void iConsultTheTelemetryHistory(String from, String to) throws Exception {
        context.lastResponse = context.send("GET", telemetry("/telemetry-measurements?from=" + from + "&to=" + to),
                context.manager.token(), null);
        assertThat(context.lastResponse.statusCode()).isEqualTo(200);
    }

    @Then("I see {int} measurements")
    @Then("I see {int} history points")
    public void iSeeRecords(int count) {
        assertThat(records()).hasSize(count);
    }

    @And("I see a {string} measurement of {double} {string}")
    public void iSeeAMeasurement(String parameter, double value, String unit) {
        assertThat(records()).anySatisfy(measurement -> {
            assertThat(measurement.get("metric")).isEqualTo(parameter);
            assertThat(((Number) measurement.get("value")).doubleValue()).isEqualTo(value);
            assertThat(measurement.get("unit")).isEqualTo(unit);
        });
    }

    @And("the point recorded at {string} is classified as deviated")
    public void thePointIsClassifiedAsDeviated(String timestamp) {
        assertThat(records()).singleElement().satisfies(point -> {
            assertThat(point.get("measuredAt").toString()).startsWith(timestamp.replace("Z", ""));
            assertThat(point.get("state")).isIn("WARNING", "CRITICAL");
        });
    }

    private void record(String metric, String value, String measuredAt) throws Exception {
        var measurement = context.send("POST", telemetry("/telemetry-measurements"), context.manager.token(), """
                {"metric":"%s","value":%s,"measuredAt":"%s"}
                """.formatted(metric, value, measuredAt));
        assertThat(measurement.statusCode()).withFailMessage(measurement.body()).isEqualTo(201);
    }

    private String telemetry(String path) {
        return context.environmentPath() + "/container-monitors/" + context.equipmentId + path;
    }

    private List<Map<String, Object>> records() {
        return JsonPath.read(context.lastResponse.body(), "$");
    }
}
