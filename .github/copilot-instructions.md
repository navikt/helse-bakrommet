# Copilot Instructions for helse-bakrommet

## Repository Overview

**Purpose**: Backend service ("Bakrommet") for the Spillerom frontend. Stores case processing data, daily overviews, sick pay basis, and other facts entered by case handlers.

**Tech Stack**: Kotlin + Gradle multi-module project (24 modules, ~515 Kotlin files, ~199MB)
- **Language**: Kotlin 2.2.20
- **Build Tool**: Gradle 9.1.0 with Kotlin DSL
- **Runtime**: Java 21 (JVM toolchain)
- **Web Framework**: Ktor 3.3.2
- **Database**: PostgreSQL with Flyway migrations
- **Testing**: JUnit 5, Testcontainers
- **Linting**: ktlint (plugin version 14.0.1)

## Essential Build Commands

**CRITICAL**: Always use Java 21 for this project. The build will fail with other Java versions.

### Setup Java 21
```bash
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

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
- **GitHub Token Required**: Build needs `ORG_GRADLE_PROJECT_githubPassword` environment variable for private NAV dependencies from GitHub Packages. In CI, this is set to `${{ secrets.GITHUB_TOKEN }}`. Locally, builds will work without it for most tasks but may fail when resolving certain dependencies.
- **Build Time**: Full clean build takes ~1m 30s. Incremental builds are much faster (~3-10s).
- **ktlint Warnings**: Build shows ktlint warnings but doesn't fail (ignoreFailures = true). These are informational.
- **Parallel Test Execution**: Tests run with max 4 parallel forks in CI, 1 fork locally.
- **Excluded from ktlint**: The `sykepenger-utbetaling`, `sykepenger-primitiver`, and `sykepenger-model` modules are excluded from ktlint checks.

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
├── bakrommet-common/           # Common utilities and shared code
├── bakrommet-demo/             # Demo application (separate entry point)
├── bakrommet-dependencies/     # Platform dependency management (BOM)
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
- **Root Build**: `build.gradle.kts` (project-wide Kotlin/ktlint/test config)
- **Dependencies**: `bakrommet-dependencies/build.gradle.kts` (version constraints)
- **Database Migrations**: `bakrommet-services/src/main/resources/db/migration/`
- **Dockerfiles**: `Dockerfile` (main), `Dockerfile.demo` (demo app)

## Common Issues & Workarounds

### Issue: Build Fails with Wrong Java Version
**Symptom**: Build errors about Java version mismatch
**Solution**: Ensure Java 21 is active (see "Setup Java 21" above)

### Issue: ktlint Formatting Errors
**Symptom**: Code fails ktlint checks
**Solution**: Run `./gradlew ktlintFormat` to auto-format code. Note: Build won't fail on ktlint errors (ignoreFailures = true), but CI shows warnings.

### Issue: Missing Pre-commit Hook Warning
**Symptom**: Warning after local build about missing pre-commit hook
**Solution**: Run `./gradlew addKtlintFormatGitPreCommitHook` to install the hook (optional)

### Issue: GitHub Package Resolution Fails
**Symptom**: Build fails resolving NAV dependencies
**Solution**: Set `ORG_GRADLE_PROJECT_githubPassword` environment variable with a GitHub token that has package read access

### Issue: Demo Build in CI
**Note**: The demo logback.xml file is intentionally deleted in CI before building. This is a known workflow step to prevent local logging config from affecting the deployed demo application.

## Code Style

- **ktlint Configuration**: `.editorconfig` disables wildcard import checks and max line length
- **Wildcard Imports**: Allowed (`ktlint_standard_no-wildcard-imports = disabled`)
- **Max Line Length**: No limit (`ktlint_standard_max-line-length = disabled`)
- **Formatting**: Use `./gradlew ktlintFormat` to auto-format before committing

## Testing

- **Framework**: JUnit 5 (configured via junit-bom 5.10.0)
- **Database Tests**: Use Testcontainers with PostgreSQL
- **Auth Mocking**: mock-oauth2-server (3.0.0)
- **HTTP Testing**: Ktor server test host
- **Test Fixtures**: Multiple modules provide test fixtures via `java-test-fixtures` plugin
- **Test Execution**: Configured for parallel execution in CI (max 4 forks), serial locally
- **Run Tests**: `./gradlew test` (takes ~30s with cached build)

## Important Conventions

1. **Always build with Java 21** - The project uses `kotlin.jvmToolchain(21)`
2. **Use Gradle wrapper** - Always use `./gradlew`, never system Gradle
3. **Run ktlintFormat before committing** - Keeps code style consistent
4. **Check build.gradle.kts hierarchy** - Root defines common config, subprojects override as needed
5. **bakrommet-dependencies module** - Defines version constraints for all dependencies (BOM pattern)
6. **Test fixtures pattern** - Many modules expose test utilities via testFixtures source set
7. **Multi-module dependencies** - Check `settings.gradle.kts` for module structure before adding cross-module dependencies

