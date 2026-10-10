# QualiTrack Platform

REST API of **QualiTrack**, the IoT platform by IoTech for quality management in laboratories. The web front is hosted on Firebase Hosting and this backend is deployed on Azure Container Apps.

## Team

**IoTech**

## Tech stack

- Java 26
- Spring Boot 4.0.5 (Web MVC, Data JPA, Security, Validation, Mail)
- MySQL (production and development), H2 (tests)
- JWT authentication (jjwt)
- Stripe for subscription payments
- PDFBox for PDF generation
- springdoc-openapi (Swagger UI) for API documentation
- Docker and GitHub Actions

## Architecture

The code is organized by bounded context under `com.iotech.qualitrack.platform`. Each context follows a layered DDD structure: `domain`, `application`, `infrastructure` and `interfaces`.

| Bounded context | Description |
|-----------------|-------------|
| `iam` | Identity and access management: sign-in, JWT, roles |
| `profile` | User and staff profiles |
| `subscription` | Subscription plans and Stripe billing |
| `laboratory` | Laboratories |
| `equipment` | Laboratory equipment |
| `inventory` | Inventory |
| `batch` | Batches |
| `tracking` | Tracking |
| `ca` | Compliance alerts: deviation alerts and notifications |
| `ra` | Reports and audit: KPI dashboard, audit log and exports |
| `shared` | Common infrastructure shared by all contexts |

Diagrams of each context are in [`docs/diagrams`](docs/diagrams).

## Getting started

### Requirements

- JDK 26
- Maven
- MySQL running locally

### Run in development

The `dev` profile is active by default.

```bash
export DATABASE_USER=root
export DATABASE_PASSWORD=your_password
mvn spring-boot:run
```

The API starts on `http://localhost:8080`. With springdoc, the Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.

### Tests

```bash
mvn test
```

### Docker

```bash
docker build -t qualitrack-platform .
docker run -p 8080:8080 --env-file .env qualitrack-platform
```

The image runs with `SPRING_PROFILES_ACTIVE=prod`.

## Configuration

Profiles live in `src/main/resources`: `application.properties` (shared), `application-dev.properties` and `application-prod.properties`.

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | Database host | `localhost` (dev) |
| `DATABASE_PORT` | Database port | `3306` |
| `DATABASE_NAME` | Database name | `qualitrack` (dev) |
| `DATABASE_USER` | Database user | |
| `DATABASE_PASSWORD` | Database password | |
| `PORT` | HTTP port | `8080` |
| `JWT_SECRET` | JWT signing secret | |
| `STRIPE_SECRET_KEY` | Stripe secret key | |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook secret | |
| `STRIPE_PRICE_*` | Stripe price ids of the plans (`BASIC_MONTHLY`, `BASIC_YEARLY`, `ENTERPRISE_MONTHLY`, `ENTERPRISE_YEARLY`) | Stripe test prices |
| `APPLICATION_FRONTEND_URL` | Web front URL, used in e-mail links and Stripe return URLs | `https://qualitrack-iotech.web.app` (prod) |
| `APPLICATION_CORS_ALLOWED_ORIGINS` | Comma-separated CORS allowed origins | `https://qualitrack-iotech.web.app`, `https://qualitrack-iotech.firebaseapp.com` (prod) |
| `RESEND_API_KEY` | Resend API key for transactional e-mails | |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | SMTP fallback when there is no Resend key | |
| `QUALITRACK_MAIL_FROM` | E-mail sender | |

### CORS

CORS is applied by `WebSecurityConfiguration` and reads its origins from `application.cors.allowed-origins`. In production, set `APPLICATION_CORS_ALLOWED_ORIGINS` to allow another domain.

## Deployment

Pushes to `main` trigger the workflow in `.github/workflows/deploy.yml`, which runs the tests, builds the Docker image, pushes it to Azure Container Registry and updates the Azure Container App.

## Branching

- `main`: production
- `develop`: integration
- `feature/*`, `hotfix/*`, `release/*`: short-lived branches merged by pull request
