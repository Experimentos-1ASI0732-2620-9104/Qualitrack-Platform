package com.iotech.qualitrack.platform.bdd;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchReportSteps {
    private final ScenarioContext context;

    public BatchReportSteps(ScenarioContext context) {
        this.context = context;
    }

    @Given("my laboratory has a batch {string} of the product {string}")
    public void myLaboratoryHasABatchOfTheProduct(String batchNumber, String productName) throws Exception {
        var product = context.send("POST", context.environmentPath() + "/products", context.manager.token(), """
                {"name":"%s","code":"P-%s","description":"Tablets","specifications":"400mg"}
                """.formatted(productName, UUID.randomUUID()));
        assertThat(product.statusCode()).withFailMessage(product.body()).isEqualTo(201);
        context.productId = ScenarioContext.id(product);

        var batch = context.send("POST", context.environmentPath() + "/products/" + context.productId + "/batches",
                context.manager.token(), """
                {"batchNumber":"%s-%s","quantity":5000,"unit":"units","startDate":"2026-05-12","notes":"Standard production run"}
                """.formatted(batchNumber, UUID.randomUUID().toString().substring(0, 8)));
        assertThat(batch.statusCode()).withFailMessage(batch.body()).isEqualTo(201);
        context.batchId = ScenarioContext.id(batch);
    }

    @When("I generate the PDF report of the batch")
    public void iGenerateThePdfReport() throws Exception {
        context.lastDocument = generateReport(context.manager, false);
    }

    @When("I generate the PDF report of the batch including deviations")
    public void iGenerateThePdfReportIncludingDeviations() throws Exception {
        context.lastDocument = generateReport(context.manager, true);
    }

    @When("a quality manager from another laboratory generates the PDF report of the batch")
    public void anotherLaboratoryGeneratesThePdfReport() throws Exception {
        context.lastDocument = generateReport(context.newManagerWithLaboratory(), false);
    }

    @Then("I receive a PDF document")
    public void iReceiveAPdfDocument() {
        assertThat(context.lastDocument.statusCode()).isEqualTo(200);
        assertThat(context.lastDocument.headers().firstValue("content-type").orElseThrow()).contains("application/pdf");
    }

    @And("the document mentions {string}")
    public void theDocumentMentions(String text) throws Exception {
        try (var pdf = Loader.loadPDF(context.lastDocument.body())) {
            assertThat(new PDFTextStripper().getText(pdf)).contains(text);
        }
    }

    private java.net.http.HttpResponse<byte[]> generateReport(ScenarioContext.Manager actor, boolean includeDeviations)
            throws Exception {
        return context.download("POST", "/batches/" + context.batchId + "/reports", actor.token(), """
                {"includeDeviations":%s,"format":"PDF"}
                """.formatted(includeDeviations));
    }
}
