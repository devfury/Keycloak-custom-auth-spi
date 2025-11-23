# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is a Keycloak Service Provider Interface (SPI) implementation that enables authentication against external legacy REST APIs. The project provides custom authenticators that integrate with Keycloak's authentication flows, allowing users from legacy systems (Bizbox and Ezcaretech) to authenticate through Keycloak by validating their credentials against external services.

## Core Architecture

### Multi-Provider Design
The codebase implements **two separate authentication providers** for different legacy systems:

1. **Bizbox Authenticator** (`dev.windfury.keycloak.bizbox.*`)
   - Provider ID: `bizbox-authenticator`
   - Display name: "Bizbox Authenticator"
   - External API configured via `BIZBOX_API_URL` environment variable

2. **Ezcaretech Authenticator** (`dev.windfury.keycloak.ez.*`)
   - Provider ID: `ezcaretech-authenticator` 
   - Display name: "Ezcaretech Authenticator"
   - External API configured via separate environment variable

Both providers share the same DTO models in `dev.windfury.keycloak.bizbox.dto` package.

### Authentication Flow
Each authenticator follows this pattern:

1. **Extract credentials** from HTTP form parameters (`username`, `password`)
2. **Call external API** via dedicated `*ExternalApi` client class:
   - First POST to legacy login endpoint (with AES encryption for Bizbox)
   - Extract session token/JSESSIONID from response
   - Second GET to user profile endpoint using the token
3. **Parse user profile** from external API response (JSON to DTO mapping)
4. **Create or update Keycloak user**:
   - Search for existing user by username
   - Create new user if not found
   - Update user attributes (name, email, phone numbers)
   - Grant roles from external system
5. **Handle special name parsing** for Korean (Hangul) names vs. Western names

### SPI Discovery Mechanism
Keycloak discovers authenticator factories via Java ServiceLoader using:
- `src/main/resources/META-INF/services/org.keycloak.authentication.AuthenticatorFactory`
- This file lists fully-qualified class names of all factory implementations

When adding a new authenticator, you **must** register its factory class in this file.

## Development Commands

### Building
```powershell
# Standard build (on Windows with PowerShell)
.\mvnw.cmd clean package

# Skip tests for faster builds
.\mvnw.cmd clean package -DskipTests

# Output JAR location
# target/keycloak.auth-0.0.2.jar
```

### Testing
```powershell
# Run all tests
.\mvnw.cmd test
```

### Deployment to Keycloak
```bash
# Copy provider JAR to Keycloak installation (adjust paths for Windows)
cp target/keycloak.auth-0.0.2.jar $KEYCLOAK_HOME/providers/

# Rebuild Keycloak to register the provider
$KEYCLOAK_HOME/bin/kc.bat build

# Start Keycloak for testing
$KEYCLOAK_HOME/bin/kc.bat start --http-port=8080
```

### Manual Testing
Test authentication flow using curl (adjust for PowerShell if needed):
```bash
curl -X POST \
  http://localhost:8080/auth/realms/yourrealm/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=yourclientid&username=yourusername&password=yourpassword'
```

## Environment Configuration

### Required Environment Variables
- **`BIZBOX_API_URL`**: Base URL for Bizbox external API (e.g., `https://api.bizbox.example.com`)
  - Used by `BizboxExternalApi` class to construct endpoint URLs
  - Must be set before building or deployment

### Password Handling
The Bizbox authenticator expects passwords to be **Base64-encoded** when received from Keycloak. The authenticator decodes them before encryption and transmission to the external API.

## Code Organization

### Package Structure
```
dev.windfury.keycloak/
├── bizbox/                    # Bizbox authenticator implementation
│   ├── BizboxAuthenticator.java          # Main authenticator logic
│   ├── BizboxAuthenticatorFactory.java   # SPI factory for Keycloak
│   ├── BizboxExternalApi.java            # HTTP client for Bizbox API
│   └── dto/                              # Shared DTOs for all authenticators
│       ├── User.java                     # Internal user model
│       ├── AuthRequestDTO.java           # Login request payload
│       ├── AuthResponseDTO.java          # Login response payload
│       ├── UserResponseDTO.java          # User profile response
│       ├── UserMemberDTO.java            # User member details
│       └── RoleDTO.java                  # Role information
└── ez/                        # Ezcaretech authenticator (separate provider)
    ├── EzcaretechAuthenticator.java
    ├── EzcaretechAuthenticatorFactory.java
    └── EzcaretechExternalApi.java
```

### Key Implementation Details

#### Bizbox External API Client
`BizboxExternalApi` implements a complex multi-step authentication flow:
- Custom AES/CBC/PKCS5Padding encryption for credentials
- HTML form parsing to extract Spring Security CSRF tokens
- Cookie-based session management with JSESSIONID
- User-Agent spoofing to mimic browser requests

#### Name Parsing Logic
Both authenticators include `splitName()` and `isHangul()` methods that intelligently parse user display names:
- **Korean names**: First character (surname) becomes `lastName`, remainder becomes `firstName`
- **Western names**: Last space-separated token becomes `lastName`, rest becomes `firstName`

This handles the different name ordering conventions between cultures.

## Coding Standards

Follow guidelines from AGENTS.md:
- Java 17 style: 4-space indentation, `UpperCamelCase` classes, `lowerCamelCase` methods
- Keep new types under `dev.windfury.keycloak` subpackages for SPI discovery
- Use Lombok for DTOs (`@Getter`, `@Setter`)
- Use SLF4J for logging (`LoggerFactory.getLogger()`)
- Emoji-prefixed commit messages (`:construction:`, etc.)

## Keycloak Admin Configuration

After deploying the provider:

1. **Create Authentication Flow**:
   - Go to Keycloak Admin Console → Realm → Authentication
   - Create new flow (type: Basic flow)
   - Add execution step, select "Bizbox Authenticator" or "Ezcaretech Authenticator"
   - Set requirement to `REQUIRED`

2. **Associate Flow with Client**:
   - Go to Client Configuration → Advanced Settings → Authentication Flow Overrides
   - Select the custom authentication flow
   - Save configuration

3. **Verify Provider Registration**:
   - Check Admin Console → Realm → Provider Info
   - Look for custom authenticator entries

## Dependencies

Key external dependencies (from pom.xml):
- **Keycloak**: 26.4.2 (services, core) — provided scope
- **Keycloak Model Legacy**: 25.0.3 — for older API compatibility
- **Lombok**: 1.18.26 — DTO generation
- **Apache HttpClient**: 4.5.14 — HTTP communication with external APIs
- **Jackson**: For JSON parsing (via Keycloak dependencies)

All Keycloak dependencies use `provided` scope since they're available in the Keycloak runtime.
