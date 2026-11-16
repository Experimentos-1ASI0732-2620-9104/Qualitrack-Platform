# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-11-16

### Added
- Equipment and IAM: an Edge Gateway is registered as equipment of the laboratory with `POST /laboratories/{laboratoryId}/edge-gateways`, located in an environment like any equipment, and receives its API key once from `POST /laboratories/{laboratoryId}/edge-gateways/{equipmentId}/api-key`. `GET` on the same path tells whether the Gateway already has a key, without returning it.
- IAM: the Edge authenticates with its identifier and the `X-API-Key` header, and reads its IoT devices from `GET /edge-gateways/{deviceId}/iot-devices`.
- Equipment: an Edge Gateway serves 15 IoT devices at most. The devices of an environment are distributed among its operational Gateways by identifier, and `GET /laboratories/{laboratoryId}/environments/{environmentId}/edge-gateway-coverage` tells how many devices each Gateway serves and how many are left without one.
- Equipment: `GET /laboratories/{laboratoryId}/environments/{environmentId}/equipments` lists the equipment located in one environment, and `?iot=true` keeps only its IoT devices.
- Tracking: telemetry, device profiles and actuation results synchronized by an authenticated Edge Gateway, only for the devices it serves; a device another Gateway serves is refused with `403`.
- Shared: error responses in Spanish for `Accept-Language: es`, with the details kept; English responses do not change.
- Batch, Equipment, Laboratory and Compliance: their facades share the batches and recorded material uses, the laboratory equipment and the equipment log, the laboratory name, and the alerts of a batch or a set of devices.
- `.env.example` with the environment variables of the API and example values.
- Tests by bounded context and layer for batches, alerts, equipment, Edge Gateways, IAM, inventory, laboratory, profile, reporting, subscription and tracking. The suite grows from 143 to 294 tests and keeps the unit, integration and BDD suites of version 1.0.0.

### Changed
- Reporting and audit: the batch report, the equipment log report and the operational counts of the indicators read the other bounded contexts through their facades instead of their repositories, and the audit action is taken as text.
- The facades of Compliance, IAM, Profile and Subscription are interfaces implemented in `application/acl`, and every bounded context calls the facades of the others only from its ACL services.
- Batch and Inventory: Batch locks and checks the batch before Inventory consumes a lot, so Inventory no longer calls Batch.
- Profile and Laboratory: Profile renames the staff member of the account through the Laboratory facade instead of Laboratory listening to the profile event.
- IAM, Laboratory and Subscription: the controllers read the authenticated user through the shared `CurrentUser` port.
- Inventory and Laboratory: the legacy import and the laboratory onboarding return a `Result`; the JPA entities of Inventory are named `*PersistenceEntity`; the stock summaries of one request share a single reference date.
- Compliance: compliance events and alert dates come from the shared clock, and the deviation alerts have indexes for the open incident of a device and for the alerts of an environment.
- Tracking, Compliance and Reporting: the endpoints document their fields, examples and responses in Swagger, and their classes have Javadoc. The API does not change.
- Tests of the original suite are organized in the package of the bounded context and layer they cover; the shared helpers are in the `testing` package.
- Project version set to `1.1.0`.
- Release: the production profile keeps `ddl-auto=none`, so the platform does not create the table `iam_edge_gateway_api_keys` nor the indexes `idx_deviation_alerts_open_incident` and `idx_deviation_alerts_environment`. Create them on the production database before this version is deployed.

### Fixed
- IAM: the password recovery is locked while a wrong code is counted, so wrong codes sent at the same time can no longer exceed the limit of 5 attempts.
- Subscription: the payment of each renewal reaches the payment history. `invoice.paid` records each renewal once per invoice, dated when Stripe was paid.
- Compliance: a deviation event with invalid data is logged and ignored instead of failing the reading Tracking is storing, and a resolution note longer than 500 characters, a variable longer than 100 or a unit longer than 20 are refused.

### Removed
- Laboratory and Compliance: the unused compliance ACL service of Laboratory and the low stock method of the Compliance facade; the low stock reaches Compliance through its integration event.

## [1.0.0] - 2026-10-10

### Added
- Identity and Access Management: sign-up with e-mail, JWT sign-in, password recovery, password changes, roles and the onboarding flow (`SUBSCRIPTION`, `LABORATORY`, `PASSWORD_CHANGE`, `READY`).
- Subscription management: plan catalog with monthly and yearly billing, Stripe checkout sessions and webhooks, and cancellation of the renewal with access kept until the end of the paid period.
- Laboratory management: laboratories, environments with a main usage, staff members with temporary credentials by e-mail, and user and staff profiles.
- Equipment management: equipment registered per laboratory and located in an environment, status changes, maintenance records performed by a staff member, BPM parameter limits and IoT devices (environmental devices and container monitors).
- Tracking: environmental profiles with thresholds and actuation rules per environment and container monitor, device measurements evaluated against the profile, actuation events and device connection status.
- Inventory: raw materials and supplier lots per environment, quality review of lots (`RELEASED`, `OBSERVED`, `REJECTED`), append-only movements, container assignment, near-expiry listing, explicit import of legacy materials and idempotent, atomic consumption by operation id.
- Batch management: pharmaceutical products and production batches per environment, batch release with a digital signature and rejection, raw material, equipment and staff usages, container assignment and batch traceability.
- Compliance and alerts: deviation alerts of environments and containers grouped into incidents, acknowledgement and resolution with notes, e-mail notifications, in-app notifications with preferences and compliance events.
- Reporting and audit: batch traceability reports, environmental period reports, equipment log reports and inventory reports in PDF and CSV, report history and download, KPI dashboards, deviation trends and audit logs.
- Configuration for Stripe plan prices, transactional e-mail (Resend or SMTP), front-end URL and CORS origins.
- Test suite of 143 tests: unit, integration over HTTP with H2, and BDD scenarios with Cucumber.
- PlantUML backend and database diagrams per bounded context in `docs/diagrams`.
- `README.md` and MIT `LICENSE.md`.

### Changed
- REST API organized by laboratory and environment under `/api/v1/laboratories/{laboratoryId}/environments/{environmentId}/...`.
- Releasing a batch, acknowledging or resolving an alert, reviewing a lot and creating a report now answer `201`; rejecting a released batch answers `409`.
- Generated reports are created first and downloaded afterwards from `/api/v1/reports/{reportId}/content`.
- Requires Java 26 and Spring Boot 4.0.5.
- Project version set to `1.0.0`.

### Removed
- Flat endpoints replaced by the environment-scoped API (`/equipments`, `/batches` updates by `PATCH` and `/laboratories/{id}/products`, among others).
- GitHub Actions workflows.
