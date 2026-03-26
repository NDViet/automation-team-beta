# automation-team-beta

End-to-end API automation suite for the `aut-api-testing` backend service.

| | |
|---|---|
| **Tech stack** | Java 21 · Gradle · TestNG · Rest Assured |
| **Framework** | `test-libraries-api` from `test-automation-fwk` |
| **Platform** | `platform-testkit-java` (`PlatformTestNGBase` and structured logging) |
| **Execution** | Local JVM · Docker container · GitHub Actions |
| **AUT** | `aut-api-testing` backend with REST, GraphQL, PostgreSQL, and Bearer-token auth |

## Prerequisites

- JDK 21
- Docker 24+
- GitHub PAT with `read:packages` to resolve framework and platform artifacts

If you are developing the framework locally, install it first:

```bash
cd /Users/viet.dnguyen/code/test-automation-project/test-automation-fwk
mvn install -DskipTests
```

## Local run

Start the AUT stack from `aut-api-testing` first, or let this project run it via Docker Compose:

```bash
cd /Users/viet.dnguyen/code/automation-team-beta
./gradlew test \
  -Dapi.base.url=http://localhost:8180 \
  -Dapi.auth.bearer.token=local-api-token --rerun
```

## Container run

Build the test-runner image:

```bash
docker build \
  --build-arg GITHUB_ACTOR=$GITHUB_ACTOR \
  --build-arg GITHUB_TOKEN=$GITHUB_TOKEN \
  -t automation-team-beta:local \
  -f containers/Dockerfile .
```

Run the full stack including PostgreSQL, the AUT image, and the test runner:

```bash
AUT_IMAGE=ghcr.io/ndviet/aut-api-testing:latest \
APP_BEARER_TOKEN=local-api-token \
docker compose --profile test up --abort-on-container-exit --exit-code-from test-runner
```

Run only the AUT services and execute tests from the local JVM:

```bash
docker compose up -d postgres aut-backend
./gradlew test -Dapi.base.url=http://localhost:8180 -Dapi.auth.bearer.token=local-api-token
```

If those host ports are already in use, override them when starting the stack:

```bash
POSTGRES_HOST_PORT=15432 AUT_HOST_PORT=18180 docker compose up -d postgres aut-backend
./gradlew test -Dapi.base.url=http://localhost:18180 -Dapi.auth.bearer.token=local-api-token
```

## Reports

- `build/reports/tests/test/index.html`
- `build/test-results/test/`

## Platform ingestion

Set these environment variables to enable test-result ingestion:

- `PLATFORM_URL`
- `PLATFORM_API_KEY`
- `TEST_ENV`
