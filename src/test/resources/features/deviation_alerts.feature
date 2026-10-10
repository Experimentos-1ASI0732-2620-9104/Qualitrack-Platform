@US07
Feature: Review and resolve deviation alerts
  As a quality and supervision manager
  I want to review the detail of a deviation alert and record its resolution notes
  So that the attention given to every deviation is documented in QualiTrack

  Background:
    Given I am a quality manager with an active laboratory
    And my laboratory has an equipment named "Cold storage A"
    And the equipment reported a "CRITICAL" deviation of "TEMPERATURE" with value 9.5 over the threshold 8.0 "Celsius"

  Scenario: Review the detail of a deviation alert
    When I open the alert detail
    Then I see the parameter "TEMPERATURE" with value 9.5, threshold 8.0 and unit "Celsius"
    And the alert severity is "CRITICAL"
    And the alert status is "UNRESOLVED"

  Scenario: Acknowledge a pending alert
    When I acknowledge the alert
    Then the alert status is "ACKNOWLEDGED"
    And the alert is registered as acknowledged by me

  Scenario: Resolve a pending alert with notes
    When I resolve the alert with the notes "Door closed and equipment recalibrated"
    Then the alert status is "RESOLVED"
    And the resolution notes are "Door closed and equipment recalibrated"

  Scenario: Resolution notes are mandatory
    When I resolve the alert with the notes " "
    Then the request is rejected
    And the alert status is "UNRESOLVED"

  Scenario: A resolved alert cannot be resolved again
    Given I resolved the alert with the notes "First corrective action"
    When I resolve the alert with the notes "Second corrective action"
    Then the request is rejected
    And the resolution notes are "First corrective action"

  Scenario: Another laboratory cannot see the alert
    When a quality manager from another laboratory opens the alert detail
    Then the access is denied
