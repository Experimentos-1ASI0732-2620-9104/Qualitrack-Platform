@US08
Feature: Generate a batch traceability report
  As a quality and supervision manager
  I want to generate a PDF report of a production batch
  So that I can review its production data and the deviations associated with it

  Background:
    Given I am a quality manager with an active laboratory
    And my laboratory has a batch "LOT-2026-A" of the product "Ibuprofen 400mg"

  Scenario: Generate the PDF report of a batch
    When I generate the PDF report of the batch
    Then I receive a PDF document
    And the document mentions "LOT-2026-A"
    And the document mentions "Ibuprofen 400mg"

  Scenario: Include the deviations linked to the batch
    Given my laboratory has an equipment named "Production room"
    And the equipment reported a "WARNING" deviation of "HUMIDITY" linked to the batch
    When I generate the PDF report of the batch including deviations
    Then I receive a PDF document
    And the document mentions "Counts from 1 linked deviation record(s)."
    And the document mentions "humidity 72 %"

  Scenario: Another laboratory cannot generate the report
    When a quality manager from another laboratory generates the PDF report of the batch
    Then the access is denied
