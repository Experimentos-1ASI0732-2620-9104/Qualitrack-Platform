Feature: Consult equipment telemetry
  As a quality and supervision manager
  I want to consult the latest measurements and the telemetry history of an equipment
  So that I can review the recorded conditions without an on-site inspection

  Background:
    Given I am a quality manager with an active laboratory
    And my laboratory has an equipment named "Cold storage A"

  @US04
  Scenario: Consult the latest measurements of an equipment
    Given the equipment sent the following measurements:
      | parameter   | value | unit    | timestamp            |
      | TEMPERATURE | 4.2   | Celsius | 2026-05-12T10:00:00Z |
      | HUMIDITY    | 55.0  | %       | 2026-05-12T10:00:00Z |
    When I consult the latest measurements of the equipment
    Then I see 2 measurements
    And I see a "TEMPERATURE" measurement of 4.2 "Celsius"
    And I see a "HUMIDITY" measurement of 55.0 "%"

  @US06
  Scenario: Filter the telemetry history by dates
    Given the equipment has the following telemetry history:
      | parameter   | value | timestamp            | anomaly |
      | TEMPERATURE | 4.0   | 2026-05-10T08:00:00Z | false   |
      | TEMPERATURE | 9.1   | 2026-05-12T08:00:00Z | true    |
      | TEMPERATURE | 5.0   | 2026-05-20T08:00:00Z | false   |
    When I consult the telemetry history from "2026-05-11T00:00:00Z" to "2026-05-13T00:00:00Z"
    Then I see 1 history points
    And the point recorded at "2026-05-12T08:00:00Z" is classified as deviated

  @US06
  Scenario: Telemetry history without results
    Given the equipment has the following telemetry history:
      | parameter   | value | timestamp            | anomaly |
      | TEMPERATURE | 4.0   | 2026-05-10T08:00:00Z | false   |
    When I consult the telemetry history from "2025-01-01T00:00:00Z" to "2025-01-31T00:00:00Z"
    Then I see 0 history points
