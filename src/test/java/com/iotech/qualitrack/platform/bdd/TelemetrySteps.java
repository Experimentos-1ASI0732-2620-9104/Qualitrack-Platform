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
            var measurement = context.send("POST", telemetry("/telemetry-measurements"), context.manager.token(), """
                    {"parameterName":"%s","value":%s,"unit":"%s","timestamp":"%s"}
                    """.formatted(row.get("parameter"), row.get("value"), row.get("unit"), row.get("timestamp")));
            assertThat(measurement.statusCode()).withFailMessage(measurement.body()).isEqualTo(201);
        }
    }

    @Given("the equipment has the following telemetry history:")
    public void theEquipmentHasTheFollowingTelemetryHistory(DataTable table) throws Exception {
        for (var row : table.asMaps()) {
            var point = context.send("POST", telemetry("/telemetry-history"), context.manager.token(), """
                    {"parameterName":"%s","recordedValue":%s,"timestamp":"%s","isAnomaly":%s}
                    """.formatted(row.get("parameter"), row.get("value"), row.get("timestamp"), row.get("anomaly")));
            assertThat(point.statusCode()).withFailMessage(point.body()).isEqualTo(201);
        }
    }

    @When("I consult the latest measurements of the equipment")
    public void iConsultTheLatestMeasurements() throws Exception {
        context.lastResponse = context.send("GET", telemetry("/telemetry-measurements"), context.manager.token(), null);
        assertThat(context.lastResponse.statusCode()).isEqualTo(200);
    }

    @When("I consult the telemetry history from {string} to {string}")
    public void iConsultTheTelemetryHistory(String from, String to) throws Exception {
        context.lastResponse = context.send("GET", telemetry("/telemetry-history?from=" + from + "&to=" + to),
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
            assertThat(measurement.get("parameterName")).isEqualTo(parameter);
            assertThat(((Number) measurement.get("value")).doubleValue()).isEqualTo(value);
            assertThat(measurement.get("unit")).isEqualTo(unit);
        });
    }

    @And("the point recorded at {string} is classified as deviated")
    public void thePointIsClassifiedAsDeviated(String timestamp) {
        assertThat(records()).singleElement().satisfies(point -> {
            assertThat(point.get("timestamp").toString()).startsWith(timestamp.replace("Z", ""));
            assertThat(point.get("isAnomaly")).isEqualTo(true);
        });
    }

    private String telemetry(String path) {
        return "/equipments/" + context.equipmentId + path;
    }

    private List<Map<String, Object>> records() {
        return JsonPath.read(context.lastResponse.body(), "$");
    }
}
