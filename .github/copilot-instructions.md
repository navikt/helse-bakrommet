# Copilot Instructions for helse-bakrommet

## Repository Overview

**Purpose**: Backend service ("Bakrommet") for the Spillerom frontend. Stores case processing data, daily overviews, sick pay basis, and other facts entered by case handlers.

**Tech Stack**: Kotlin + Gradle multi-module project (24 modules, ~515 Kotlin files, ~199MB)
- **Language**: Kotlin (version from the sykepenger Gradle plugins)
- **Build Tool**: Gradle 9.1.0 with Kotlin DSL
- **Runtime**: Java 25 (JVM toolchain from `no.nav.sykepenger.kotlin`)
- **Web Framework**: Ktor (BOM from `no.nav.sykepenger.kotlin`)
- **Database**: PostgreSQL with Flyway migrations
- **Testing**: JUnit 5, Testcontainers
- **Linting**: ktlint via the sykepenger Gradle plugins (`check` runs `ktlintFormat`)

## Essential Build Commands

**CRITICAL**: Always use Java 25 for this project.

### Build Commands
```bash
# Full build with tests (takes ~1.5 minutes)
./gradlew build

# Build without tests (takes ~2 minutes, use when you only need compilation)
./gradlew build -x test

# Run tests only
./gradlew test

# Clean build
./gradlew clean build

# Format code with ktlint
./gradlew ktlintFormat

# Check code style (ktlint runs automatically during build)
./gradlew ktlintCheck
```

### Important Build Notes
- **Repositories**: Set up in `settings.gradle.kts`. Locally and for agents, NAV packages come from the GitHub Package Registry mirror without a token.
- **Build Time**: Full clean build takes ~1m 30s. Incremental builds are much faster (~3-10s).
- **ktlint Warnings**: Build shows ktlint warnings but doesn't fail (ignoreFailures = true). These are informational.
- **Excluded from ktlint**: The `sykepenger-*` modules are excluded from ktlint in their own `build.gradle.kts`.

## Project Structure

### Module Organization
```
helse-bakrommet/
├── bakrommet-api/              # API routes and HTTP layer
├── bakrommet-api-dto/          # API data transfer objects
├── bakrommet-bootstrap/        # Main application entry point (App.kt)
├── bakrommet-clients/          # External service clients (7 submodules)
│   ├── bakrommet-client-aareg/
│   ├── bakrommet-client-ainntekt/
│   ├── bakrommet-client-ereg/
│   ├── bakrommet-client-inntektsmelding/
│   ├── bakrommet-client-pdl/
│   ├── bakrommet-client-sigrun/
│   └── bakrommet-client-sykepengesoknad/
├── bakrommet-demo/             # Demo application with in-memory storage (the only deployed image)
├── bakrommet-e2e-tests/        # End-to-end integration tests
├── bakrommet-kafka-dto/        # Kafka message DTOs
├── bakrommet-services/         # Business logic and database layer
├── sykepenger-model/           # Sick pay domain model (imported)
├── sykepenger-model-dto/       # Domain model DTOs
├── sykepenger-primitiver/      # Time/date/period primitives
├── sykepenger-primitiver-dto/  # Primitives DTOs
├── sykepenger-utbetaling/      # Payment logic (imported)
└── sykepenger-utbetaling-dto/  # Payment DTOs
```

### Key Files
- **Main Entry Point**: `bakrommet-bootstrap/src/main/kotlin/no/nav/helse/bakrommet/App.kt`
- **Demo Entry Point**: `bakrommet-demo/src/main/kotlin/no/nav/helse/bakrommet/StartDemoApp.kt`
- **Root Build**: `build.gradle.kts` (applies `no.nav.sykepenger.root`; shared Kotlin/ktlint/test config comes from the sykepenger Gradle plugins)
- **Dependencies**: `gradle/libs.versions.toml` (version catalog)
- **Database Migrations**: `bakrommet-services/src/main/resources/db/migration/`
- **Image**: `bakrommet-demo` is built with jib (`no.nav.sykepenger.deployable`) as `sp-bakrommet`
- **Nais manifest**: `.nais/sp-bakrommet.yaml` (dev-gcp only)

## Common Issues & Workarounds

### Issue: Build Fails with Wrong Java Version
**Symptom**: Build errors about Java version mismatch
**Solution**: Ensure Java 25 is active

### Issue: ktlint Formatting Errors
**Symptom**: Code fails ktlint checks
**Solution**: Run `./gradlew ktlintFormat` to auto-format code. Note: Build won't fail on ktlint errors (ignoreFailures = true), but CI shows warnings.

### Local Logging in the Demo App
**Note**: The demo app uses the JSON logging in `bakrommet-services/src/main/resources/logback.xml`. The `Start demo app` run configuration sets `-Dlogback.configurationFile=logback-lokal.xml` for readable local logs.

## Code Style

- **ktlint Configuration**: `.editorconfig` disables wildcard import checks and max line length
- **Wildcard Imports**: Allowed (`ktlint_standard_no-wildcard-imports = disabled`)
- **Max Line Length**: No limit (`ktlint_standard_max-line-length = disabled`)
- **Formatting**: Use `./gradlew ktlintFormat` to auto-format before committing

## Testing

- **Framework**: JUnit (BOM from `no.nav.sykepenger.kotlin`)
- **Database Tests**: Use Testcontainers with PostgreSQL
- **Auth Mocking**: mock-oauth2-server (3.0.0)
- **HTTP Testing**: Ktor server test host
- **Test Fixtures**: Multiple modules provide test fixtures via `java-test-fixtures` plugin
- **Run Tests**: `./gradlew test` (takes ~30s with cached build)

## Important Conventions

1. **Always build with Java 25** - The toolchain comes from `no.nav.sykepenger.kotlin`
2. **Use Gradle wrapper** - Always use `./gradlew`, never system Gradle
3. **Run ktlintFormat before committing** - Keeps code style consistent
4. **Sykepenger Gradle plugins** - Each module applies `no.nav.sykepenger.kotlin` (or `.deployable` for `bakrommet-demo`)
5. **Version catalog** - Add dependencies to `gradle/libs.versions.toml`
6. **Test fixtures pattern** - Many modules expose test utilities via testFixtures source set
7. **Multi-module dependencies** - Check `settings.gradle.kts` for module structure before adding cross-module dependencies

