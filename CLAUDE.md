# CLAUDE.md – stock-alert

## Project Overview

**stock-alert** is a Spring Boot application that monitors stock/securities prices via the [FCS API](https://fcsapi.com/) and sends configurable alerts (e.g. email) when defined price thresholds or percentage changes are exceeded.

- **Group ID:** `com.github.arburk.stockalert`
- **Artifact:** `stock-alert`
- **Version:** `0.5.1-SNAPSHOT`
- **Java:** 25
- **Spring Boot:** 4.0.3
- **Spring Cloud:** 2025.1.1

---

## Build & Test

```bash
# Build and run all tests
mvn -B clean verify --file pom.xml

# Build Docker image
docker build -t stock-alert:0.5.1-SNAPSHOT .
```

Tests use **JUnit 5**, **WireMock** (via `wiremock-spring-boot`), **GreenMail** (email), and **Testcontainers** (MinIO for S3).  
Code coverage is reported via **JaCoCo** (`target/jacoco/`).  
Surefire reports go to `target/surefire/`.

---

## Key Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-quartz` | Scheduled stock checks |
| `spring-boot-starter-mail` | Email notifications |
| `spring-cloud-starter-openfeign` | HTTP client for FCS API |
| `jackson-databind` (tools.jackson) | JSON config parsing |
| `software.amazon.awssdk:s3` | S3-compatible storage |
| `mapstruct` | DTO mapping |
| `lombok` | Boilerplate reduction |

---

## Runtime Configuration

The application is configured entirely via **environment variables**:

| Variable | Description | Default |
|---|---|---|
| `FCS-API-KEY` | API key for https://fcsapi.com/ | *(required)* |
| `UPDATE-CRON` | Cron expression for scheduled updates | `0 16 9-21 * * MON-FRI` |
| `UPDATE-ON-STARTUP` | Run update on startup | `false` |
| `CONFIG-URL` | URL or file path to `config.json` | *(required)* |
| `STORAGE` | Storage provider: `default` (local FS) or `s3` | `default` |

**S3 Storage:**

| Variable | Default |
|---|---|
| `S3-ENDPOINT` | *(required)* |
| `S3-ACCESS-KEY` | *(required)* |
| `S3-SECRET-KEY` | *(required)* |
| `S3-BUCKET` | `stock-alert` |
| `S3-REGION` | *(optional)* |
| `S3-ENDPOINT-FORCE-PATH-STYLE` | `true` |

**Email Gateway:**

| Variable | Default |
|---|---|
| `GATEWAY-EMAIL-HOST` | `localhost` |
| `GATEWAY-EMAIL-PORT` | `587` |
| `GATEWAY-EMAIL-AUTH` | `true` |
| `GATEWAY-EMAIL-USER` | *(required)* |
| `GATEWAY-EMAIL-PWD` | *(required)* |
| `GATEWAY-EMAIL-TLS` | `true` |
| `GATEWAY-EMAIL-SSL` | `false` |
| `GATEWAY-EMAIL-SENDER-ADDRESS` | `stock-alert@arburk.github.com` |
| `GATEWAY-EMAIL-DEBUG` | `false` |

---

## Stock Alert Config (`config.json`)

The monitored securities and alert rules are defined in a JSON file referenced by `CONFIG-URL`. See [`src/main/resources/config-example.json`](src/main/resources/config-example.json) for the full schema.

Key fields:
- `silence-duration` – Minimum time between repeated alerts (e.g. `"6h"`)
- `percentage-alert` – Global percentage change threshold (e.g. `"5%"`)
- `notification-channels` – List of channels (`email`, etc.) with recipients
- `securities` – List of securities with `symbol`, `exchange`, `isin`, and `alerts` (price thresholds)

---

## Architecture Notes

- **Scheduler** – Quartz-based scheduler triggers stock price fetching on the configured cron.
- **FCS API client** – OpenFeign client under `infrastructure/provider/fcsapi`.
- **Storage** – Pluggable persistence layer; `default` uses local filesystem, `s3` uses AWS SDK v2.
- **Notifications** – Email notifications via Spring Mail; extensible to other channels.
- **Mapping** – MapStruct mappers convert between API DTOs and domain objects.
- **Config** – Application YAML at `src/main/resources/application.yml`; domain config loaded from `CONFIG-URL`.

---

## Docker

```bash
# Run with remote config
docker run -e FCS-API-KEY=your-api-key \
           -e CONFIG-URL=https://raw.githubusercontent.com/arburk/stock-alert/refs/heads/main/src/main/resources/config-example.json \
           -e GATEWAY-EMAIL-HOST=smtp.provider.com \
           -e GATEWAY-EMAIL-USER=you@provider.com \
           -e GATEWAY-EMAIL-PWD=<your-secret-password> \
           arburk/stock-alert:0.5.0

# Run with a mounted local config file
docker run -e FCS-API-KEY=your-api-key \
           -e GATEWAY-EMAIL-HOST=smtp.provider.com \
           -e GATEWAY-EMAIL-USER=you@provider.com \
           -e GATEWAY-EMAIL-PWD=<your-secret-password> \
           -v /home/user/my-config:/config \
           -e CONFIG-URL=/config/my-config.json \
           arburk/stock-alert:0.5.0
```

Pre-built images are available on [Docker Hub](https://hub.docker.com/r/arburk/stock-alert).

---

## CI / Quality

- CI: GitHub Actions (`.github/workflows/ci.yml`)
- Code quality: [SonarCloud](https://sonarcloud.io/summary/new_code?id=arburk_stock-alert) (`sonar.organization=arburk`)
- Coverage: JaCoCo XML report at `target/jacoco/jacoco.xml`

