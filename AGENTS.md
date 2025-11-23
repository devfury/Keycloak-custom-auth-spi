# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/com/example/keycloak` hosts the SPI entry points (`CustomAuthenticator`, factory, external API client) aligned with Keycloak packaging.
- `src/main/java/com/example/keycloak/dto` contains Lombok-backed DTOs that mirror external API payloads.
- `src/main/resources` is reserved for Keycloak descriptors (e.g., `META-INF/services/org.keycloak.authentication.AuthenticatorFactory`) when new providers are added.
- `src/static` stores admin-console walkthrough screenshots referenced by `README.md`.
- `target/` is Maven's output directory; the built provider JAR is `target/keycloak.auth-0.0.2.jar`.

## Build, Test, and Development Commands
- `./mvnw clean package` compiles against Java 17 and produces the deployable SPI JAR.
- `./mvnw clean package -DskipTests` speeds up packaging when test fixtures are unavailable.
- `cp target/keycloak.auth-0.0.2.jar $KEYCLOAK_HOME/providers/ && $KEYCLOAK_HOME/bin/kc.sh build` installs the provider into a local Keycloak distribution.
- Use `$KEYCLOAK_HOME/bin/kc.sh start --http-port=8080` to spin up Keycloak for manual verification.

## Coding Style & Naming Conventions
- Follow standard Java 17 style: 4-space indentation, `UpperCamelCase` for classes, `lowerCamelCase` for methods and fields.
- Keep new types under `dev.windfury.keycloak` subpackages (`dto`, `auth`, `service`) to preserve SPI discovery.
- Prefer Lombok for DTOs and plain constructors; avoid complex logic inside Lombok-annotated classes.
- Use SLF4J for logging; inject `LoggerFactory.getLogger(CurrentClass.class)` or `@Slf4j`.

## Testing Guidelines
- Place unit and integration tests under `src/test/java`; prefer JUnit 5 with Maven Surefire.
- Mock external HTTP calls using WireMock or REST-assured to validate authentication flows without hitting live services.
- Name test classes `*Test` or `*IT` so Maven discovers them automatically.
- Run `./mvnw test` locally before packaging to keep the provider stable.

## Commit & Pull Request Guidelines
- Follow the existing history format: emoji-based prefix (`:construction:`) followed by a concise imperative description.
- Scope each commit to one logical change and include relevant updates to docs or assets.
- PRs should describe the change, list verification steps (e.g., `./mvnw clean package`), and link to any tracking issues.
- Attach screenshots or curl transcripts when altering authentication behaviour or admin console flows.

## Environment Configuration
- Set `EXTERNAL_API_URL` before building or running tests; it defines the upstream legacy service base URL.
- Avoid hard-coding credentials in code; rely on Keycloak secrets or environment variables for sensitive data.
- Document any additional required environment variables in `README.md` when new integrations are introduced.
