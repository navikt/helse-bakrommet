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

## Local Development Setup

### First Time Setup
1. **Install Java 21**: Required for building and running the project
   ```bash
   # On Ubuntu/Debian with apt
   sudo apt-get update
   sudo apt-get install openjdk-21-jdk
   
   # Or download from https://adoptium.net/
   ```

2. **Set up environment variables** (if working with GitHub Packages):
   ```bash
   export ORG_GRADLE_PROJECT_githubPassword=<your-github-token>
   ```

3. **Initial build** to verify setup:
   ```bash
   ./gradlew build
   ```

4. **Optional: Install ktlint pre-commit hook**:
   ```bash
   ./gradlew addKtlintFormatGitPreCommitHook
   ```

### Running the Application Locally
- **Main application**: Run `App.kt` in `bakrommet-bootstrap` module
- **Demo application**: Run `StartDemoApp.kt` in `bakrommet-demo` module
- **Database**: Tests use Testcontainers with PostgreSQL (Docker required)

## Pull Request Workflow

### Creating Pull Requests
1. Create a descriptive branch name (e.g., `feature/add-new-endpoint`, `fix/null-pointer-bug`)
2. Make focused, incremental commits
3. Run tests and linting before pushing:
   ```bash
   ./gradlew ktlintFormat build
   ```
4. Write clear commit messages (see conventions below)
5. Keep PRs small and focused on a single concern

### Commit Message Conventions
- Use emoji prefixes (observed from dependabot): `⬆` for dependency updates
- Use descriptive messages that explain the "why" not just the "what"
- Examples:
  - "Add authentication endpoint for user login"
  - "Fix null pointer exception in payment calculation"
  - "Refactor date handling to use sykepenger-primitiver"

### Code Review Guidelines
- PRs are reviewed by team members in #team-bømlo-værsågod Slack channel
- Address review comments promptly
- Update tests when changing functionality
- Ensure CI passes before requesting review

## Security and Dependency Management

### Security Scanning
- **CodeQL**: Runs automatically on main branch and PRs
- **Dependabot**: Monitors dependencies (GitHub Actions, Docker, Gradle)
- **Update cooldown**: All dependency updates delayed by 7 days for stability

### Handling Vulnerabilities
1. Check Dependabot alerts regularly
2. When adding dependencies, prefer well-maintained libraries
3. Override vulnerable transitive dependencies in root `build.gradle.kts`:
   ```kotlin
   constraints {
       implementation("org.apache.commons:commons-compress:1.28.0") {
           because("org.testcontainers:postgresql:1.21.0 -> 1.24.0 har en sårbarhet")
       }
   }
   ```
4. Document security overrides with `because()` clause explaining the reason

### Adding New Dependencies
1. Check if dependency is already managed in `bakrommet-dependencies/build.gradle.kts`
2. Use version constraints from the dependencies module
3. For new dependencies, add version to `bakrommet-dependencies` first
4. Test thoroughly after adding dependencies
5. Consider security implications and maintainability

## Debugging and Troubleshooting

### Common Debugging Approaches
1. **Build issues**: Check Java version first (`java -version`)
2. **Test failures**: Run individual test with `./gradlew :module-name:test --tests ClassName`
3. **Dependency issues**: Try `./gradlew clean build --refresh-dependencies`
4. **ktlint issues**: Run `./gradlew ktlintFormat` to auto-fix formatting

### Useful Debugging Commands
```bash
# See dependency tree for a module
./gradlew :module-name:dependencies

# Run specific test class
./gradlew :module-name:test --tests "no.nav.helse.ClassName"

# Run tests with detailed output
./gradlew test --info

# Check for outdated dependencies
./gradlew dependencyUpdates

# Clean and rebuild from scratch
./gradlew clean build --no-build-cache
```

### Module-Specific Issues
- **bakrommet-demo**: The logback.xml is intentionally deleted in CI
- **sykepenger-* modules**: Excluded from ktlint checks (imported code)
- **GitHub Packages**: May need token for resolving NAV dependencies

## Custom Agents

The repository includes custom Copilot agents for specialized tasks:

### scenario-creator Agent
- **Purpose**: Create test scenario files for the demo application
- **Location**: `.github/agents/scenario.agent.md`
- **Usage**: Use when creating new test scenarios in Norwegian for demo app
- **Conventions**: Follow existing patterns, use Norwegian text, update `AlleScenarioer.kt`

