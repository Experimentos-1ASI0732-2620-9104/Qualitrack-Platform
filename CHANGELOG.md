# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
