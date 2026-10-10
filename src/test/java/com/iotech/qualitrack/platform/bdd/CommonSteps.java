package com.iotech.qualitrack.platform.bdd;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonSteps {
    private final ScenarioContext context;

    public CommonSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("I am a quality manager with an active laboratory")
    public void iAmAQualityManagerWithAnActiveLaboratory() throws Exception {
        context.manager = context.newManagerWithLaboratory();
    }

    @And("my laboratory has an equipment named {string}")
    public void myLaboratoryHasAnEquipmentNamed(String name) throws Exception {
        var monitor = context.send("POST", "/laboratories/" + context.manager.laboratoryId() + "/devices/container-monitors",
                context.manager.token(), """
                {"name":"%s","sensorExternalId":"CNT-%s","serialNumber":"MAC-%s","model":"ESP32-WROOM-32","firmwareVersion":"1.0.3"}
                """.formatted(name, UUID.randomUUID().toString().substring(0, 8), UUID.randomUUID().toString().substring(0, 8)));
        assertThat(monitor.statusCode()).withFailMessage(monitor.body()).isEqualTo(201);
        context.equipmentId = ScenarioContext.id(monitor);
        var located = context.send("POST", context.environmentPath() + "/container-monitors", context.manager.token(),
                "{\"deviceId\":" + context.equipmentId + "}");
        assertThat(located.statusCode()).withFailMessage(located.body()).isEqualTo(201);
    }

    @Then("the request is rejected")
    public void theRequestIsRejected() {
        assertThat(context.lastResponse.statusCode()).isBetween(400, 499);
    }

    @Then("the access is denied")
    public void theAccessIsDenied() {
        assertThat(context.lastStatus).isEqualTo(403);
    }
}
