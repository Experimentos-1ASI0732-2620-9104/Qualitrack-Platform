package com.iotech.qualitrack.platform.bdd;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class DeviationAlertSteps {
    private final ScenarioContext context;

    public DeviationAlertSteps(ScenarioContext context) {
        this.context = context;
    }

    @And("the equipment reported a {string} deviation of {string} with value {double} over the threshold {double} {string}")
    public void theEquipmentReportedADeviation(String severity, String parameter, double value, double threshold,
                                               String unit) throws Exception {
        reportDeviation(severity, parameter, value, threshold, unit, java.time.Instant.parse("2026-05-12T11:00:00Z"));
    }

    @And("the equipment reported a {string} deviation of {string} linked to the batch")
    public void theEquipmentReportedADeviationLinkedToTheBatch(String severity, String parameter) throws Exception {
        var usage = context.send("POST", context.environmentPath() + "/usage-assignments", context.manager.token(),
                "{\"usage\":\"PRODUCT_STORAGE\"}");
        assertThat(usage.statusCode()).withFailMessage(usage.body()).isBetween(200, 201);
        var assigned = context.send("PUT", context.environmentPath() + "/products/" + context.productId + "/batches/"
                + context.batchId + "/container-assignment", context.manager.token(),
                "{\"containerMonitorId\":" + context.equipmentId + "}");
        assertThat(assigned.statusCode()).withFailMessage(assigned.body()).isBetween(200, 201);
        reportDeviation(severity, parameter, 72.0, 65.0, "%", java.time.Instant.now());
    }

    @When("I open the alert detail")
    public void iOpenTheAlertDetail() throws Exception {
        context.lastResponse = context.send("GET", "/deviation-alerts/" + context.alertId, context.manager.token(), null);
        assertThat(context.lastResponse.statusCode()).isEqualTo(200);
    }

    @When("a quality manager from another laboratory opens the alert detail")
    public void anotherLaboratoryOpensTheAlertDetail() throws Exception {
        var intruder = context.newManagerWithLaboratory();
        context.lastResponse = context.send("GET", "/deviation-alerts/" + context.alertId, intruder.token(), null);
    }

    @When("I acknowledge the alert")
    public void iAcknowledgeTheAlert() throws Exception {
        context.lastResponse = updateStatus("ACKNOWLEDGED", null);
    }

    @Given("I resolved the alert with the notes {string}")
    public void iResolvedTheAlertWithTheNotes(String notes) throws Exception {
        assertThat(updateStatus("RESOLVED", notes).statusCode()).isIn(200, 201);
    }

    @When("I resolve the alert with the notes {string}")
    public void iResolveTheAlertWithTheNotes(String notes) throws Exception {
        context.lastResponse = updateStatus("RESOLVED", notes);
    }

    @Then("I see the parameter {string} with value {double}, threshold {double} and unit {string}")
    public void iSeeTheDeviationData(String parameter, double value, double threshold, String unit) {
        var body = context.lastResponse.body();
        assertThat(JsonPath.<String>read(body, "$.parameterName")).isEqualTo(parameter);
        assertThat(JsonPath.<Double>read(body, "$.recordedValue")).isEqualTo(value);
        assertThat(JsonPath.<Double>read(body, "$.thresholdValue")).isEqualTo(threshold);
        assertThat(JsonPath.<String>read(body, "$.unit")).isEqualTo(unit);
    }

    @And("the alert severity is {string}")
    public void theAlertSeverityIs(String severity) throws Exception {
        assertThat(JsonPath.<String>read(storedAlert(), "$.severity")).isEqualTo(severity);
    }

    @Then("the alert status is {string}")
    public void theAlertStatusIs(String status) throws Exception {
        assertThat(JsonPath.<String>read(storedAlert(), "$.status")).isEqualTo(status);
    }

    @And("the alert is registered as acknowledged by me")
    public void theAlertIsAcknowledgedByMe() throws Exception {
        assertThat(((Number) JsonPath.read(storedAlert(), "$.acknowledgedBy")).longValue())
                .isEqualTo(context.manager.userId());
    }

    @And("the resolution notes are {string}")
    public void theResolutionNotesAre(String notes) throws Exception {
        assertThat(JsonPath.<String>read(storedAlert(), "$.resolutionNotes")).isEqualTo(notes);
    }

    private void reportDeviation(String severity, String parameter, double value, double threshold, String unit,
                                 java.time.Instant detectedAt) throws Exception {
        var alert = context.send("POST", context.environmentPath() + "/deviation-alerts", context.manager.token(), """
                {"deviceId":%d,"parameterName":"%s","recordedValue":%s,"thresholdValue":%s,"unit":"%s",
                 "detectedAt":"%s","severity":"%s"}
                """.formatted(context.equipmentId, parameter, value, threshold, unit, detectedAt, severity));
        assertThat(alert.statusCode()).withFailMessage(alert.body()).isEqualTo(201);
        context.alertId = ScenarioContext.id(alert);
    }

    private java.net.http.HttpResponse<String> updateStatus(String status, String notes) throws Exception {
        if ("ACKNOWLEDGED".equals(status)) {
            return context.send("POST", "/deviation-alerts/" + context.alertId + "/acknowledgements", context.manager.token(), null);
        }
        var resolutionNotes = notes == null ? "null" : "\"" + notes + "\"";
        return context.send("POST", "/deviation-alerts/" + context.alertId + "/resolutions", context.manager.token(), """
                {"resolutionNotes":%s}
                """.formatted(resolutionNotes));
    }

    private String storedAlert() throws Exception {
        return context.send("GET", "/deviation-alerts/" + context.alertId, context.manager.token(), null).body();
    }
}
