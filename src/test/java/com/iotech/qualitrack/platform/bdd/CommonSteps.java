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
        var equipment = context.send("POST", "/equipments", context.manager.token(), """
                {"laboratoryId":%d,"name":"%s","type":"REFRIGERATOR","model":"R-200","serialNumber":"SN-%s"}
                """.formatted(context.manager.laboratoryId(), name, UUID.randomUUID()));
        assertThat(equipment.statusCode()).withFailMessage(equipment.body()).isEqualTo(201);
        context.equipmentId = ScenarioContext.id(equipment);
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
