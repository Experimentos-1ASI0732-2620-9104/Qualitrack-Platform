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
        reportDeviation(severity, parameter, value, threshold, unit, null);
    }

    @And("the equipment reported a {string} deviation of {string} linked to the batch")
    public void theEquipmentReportedADeviationLinkedToTheBatch(String severity, String parameter) throws Exception {
        reportDeviation(severity, parameter, 72.0, 65.0, "%", context.batchId);
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
        assertThat(updateStatus("RESOLVED", notes).statusCode()).isEqualTo(200);
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
                                 Long batchId) throws Exception {
        var alert = context.send("POST", "/equipments/" + context.equipmentId + "/deviation-alerts", context.manager.token(), """
                {"batchId":%s,"parameterName":"%s","recordedValue":%s,"thresholdValue":%s,"unit":"%s",
                 "timestamp":"2026-05-12T11:00:00Z","severity":"%s"}
                """.formatted(batchId, parameter, value, threshold, unit, severity));
        assertThat(alert.statusCode()).withFailMessage(alert.body()).isEqualTo(201);
        context.alertId = ScenarioContext.id(alert);
    }

    private java.net.http.HttpResponse<String> updateStatus(String status, String notes) throws Exception {
        var resolutionNotes = notes == null ? "null" : "\"" + notes + "\"";
        return context.send("PATCH", "/deviation-alerts/" + context.alertId, context.manager.token(), """
                {"status":"%s","performedBy":%d,"resolutionNotes":%s}
                """.formatted(status, context.manager.userId(), resolutionNotes));
    }

    private String storedAlert() throws Exception {
        return context.send("GET", "/deviation-alerts/" + context.alertId, context.manager.token(), null).body();
    }
}
