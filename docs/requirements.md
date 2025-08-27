# Requirements Document (Java + Spring Boot)

## Introduction
This project is a small web application built with Java and Spring Boot that provides user authentication via Single Sign-On (SSO) with Microsoft 365 (Microsoft Entra ID/Azure AD) and GitHub accounts. The application features a simple login flow where users authenticate with their preferred SSO provider, then access a personalized dashboard showing a greeting with their username and a logout control. Server-side HTML rendering is done with Thymeleaf. Data is persisted using Spring Data JPA with PostgreSQL in production and H2 in development/test environments. Sessions are handled by Spring Security using secure HTTP session cookies.

- Language/Runtime: Java 17+
- Frameworks/Libraries:
  - Spring Boot 3.x (spring-boot-starter-web, spring-boot-starter-thymeleaf)
  - Spring Security 6 (spring-boot-starter-security, spring-boot-starter-oauth2-client)
  - Spring Data JPA (spring-boot-starter-data-jpa)
  - Flyway (database migrations)
  - Database: PostgreSQL (prod), H2 (dev/test). Any JDBC-compatible DB can be used via JPA.
- Templating: Thymeleaf with template caching enabled in production
- Build: Maven or Gradle

## Requirements

### Requirement 1 — Authenticate via Microsoft 365 or GitHub
**User Story:** As a user, I want to log in using my Microsoft 365 or GitHub account, so that I can access the application without creating a separate account.

#### Acceptance Criteria
1. WHEN a user visits the application root URL (e.g., `/`) THEN the system SHALL display a login page with clearly labeled Microsoft 365 and GitHub login options.
2. WHEN a user clicks the Microsoft 365 login button THEN the system SHALL redirect to Spring Security’s OAuth2 client authorization endpoint, e.g., `/oauth2/authorization/azure` (registrationId configurable).
3. WHEN a user clicks the GitHub login button THEN the system SHALL redirect to `/oauth2/authorization/github`.
4. WHEN OAuth2/OIDC authorization is successful THEN the system SHALL receive an authorization code at `/login/oauth2/code/{registrationId}` and exchange it for an access token using Spring Security OAuth2 Client. In development for GitHub, the callback URL SHALL be `http://localhost:3000/auth/callback/github` (i.e., `/auth/callback/{registrationId}`), and the security configuration SHALL be aligned accordingly.
5. WHEN the access token is obtained THEN the system SHALL retrieve the user’s profile from the respective provider:
   - Microsoft 365: Microsoft Graph `/v1.0/me` (requires appropriate scope such as `User.Read`).
   - GitHub: `https://api.github.com/user` (and `.../emails` if primary email is not provided and email access is permitted).
6. IF the user does not exist in the application database THEN the system SHALL create a new user record with profile fields (see Data Model) mapped from the provider response.
7. WHEN authentication is complete THEN the system SHALL create a secured HTTP session and redirect the user to their personal page (e.g., `/me`).
8. IF the user denies consent or an OAuth error occurs THEN the system SHALL redirect back to the login page with a user-friendly error message.

### Requirement 2 — Personalized page and access control
**User Story:** As an authenticated user, I want to see my personal page with a greeting, so that I know I’m successfully logged in and can see my identity.

#### Acceptance Criteria
1. WHEN an authenticated user accesses their personal page (e.g., `/me`) THEN the system SHALL display `hello {user_name}` where `user_name` is the resolved display name (see Name Resolution below).
2. WHEN the personal page loads THEN the system SHALL prominently display the user’s username/display name.
3. WHEN the personal page loads THEN the system SHALL display a logout button that submits a POST to `/logout` with a CSRF token.
4. IF a user tries to access the personal page without authentication THEN the system SHALL redirect them to the login page.

Name Resolution priority:
- Microsoft 365: use `displayName`; fallback to `mailNickname` or `userPrincipalName`.
- GitHub: use `name`; fallback to `login` (handle if `name` is null/private).

### Requirement 3 — Logout
**User Story:** As an authenticated user, I want to log out of the application, so that I can securely end my session.

#### Acceptance Criteria
1. WHEN a user clicks the logout button THEN the system SHALL invalidate the server-side session.
2. WHEN logout is complete THEN the system SHALL redirect the user to the login page (e.g., `/login?logout`).
3. WHEN a user is logged out THEN the system SHALL clear the `JSESSIONID` cookie (unset/expired) and any app-specific cookies.
4. WHEN a logged-out user tries to access protected pages THEN the system SHALL redirect them to the login page.
5. Optional: IF configured, the system MAY revoke provider access tokens where applicable or remove authorized client state from the session.

### Requirement 4 — Persistence
**User Story:** As a system administrator, I want user data to be persisted in a database, so that user sessions and profiles are maintained across application restarts.

#### Acceptance Criteria
1. WHEN a new user authenticates THEN the system SHALL store their profile information using Spring Data JPA in the configured database (PostgreSQL in production; H2 for dev/test by default).
2. WHEN a user logs in THEN the system SHALL retrieve their existing profile from the database and update last login timestamp and any changed profile attributes.
3. WHEN the application starts THEN the system SHALL run Flyway migrations to initialize/upgrade the database schema if needed.
4. WHEN user sessions are created THEN the system SHALL store session data securely server-side (HTTP session). For distributed deployments, Spring Session (JDBC or Redis) MAY be used.

### Requirement 5 — Server-side rendering
**User Story:** As a developer, I want the web pages to be rendered using a templating system, so that the HTML can be generated dynamically with user data.

#### Acceptance Criteria
1. WHEN any page is requested THEN the system SHALL use Thymeleaf to render HTML on the server side.
2. WHEN the login page is rendered THEN the system SHALL use a Thymeleaf template to generate the HTML structure (including buttons/links for Microsoft 365 and GitHub).
3. WHEN the personal page is rendered THEN the system SHALL inject the user’s data (e.g., display name, avatar) into the template.
4. WHEN templates are processed THEN the system SHALL handle template errors gracefully and show a friendly error page.
5. WHEN the application runs in production THEN template caching SHALL be enabled for performance (note: Thymeleaf templates are interpreted at runtime; build-time compilation is not typical in Spring Boot and is not required).

## Functional Details

### URL Mapping (typical Spring Security & MVC)
- GET `/` → Login page (public)
- GET `/login` → Login page (public; optional alias for `/`)
- GET `/oauth2/authorization/{registrationId}` → Initiates OAuth2 flow (e.g., `azure`, `github`)
- GET `/login/oauth2/code/{registrationId}` → OAuth2 redirect URI (handled by Spring Security; default in production)
- GET `/auth/callback/{registrationId}` → OAuth2 redirect URI (development override; e.g., `http://localhost:3000/auth/callback/github`)
- GET `/me` → Personal page (authenticated)
- POST `/logout` → Logout (authenticated; requires CSRF token)
- GET `/error` → Error page (public)

### Data Model (JPA)
- Entity: `User`
  - `id` (UUID or generated Long)
  - `provider` (enum/string: `AZURE`, `GITHUB`)
  - `externalId` (string; Azure objectId or GitHub id)
  - `username` (string)
  - `displayName` (string)
  - `email` (string; nullable if unavailable)
  - `avatarUrl` (string; nullable)
  - `createdAt`, `updatedAt`, `lastLoginAt` (timestamps)
  - Unique constraint on (`provider`, `externalId`)

Notes:
- Only minimal profile data is stored; no access/refresh tokens are persisted by default.
- Additional tables may be added for audit logs or sessions if Spring Session is adopted.

### OAuth2 Providers
- Microsoft 365 (Entra ID/Azure AD)
  - Authorization Code with PKCE recommended
  - Scopes: `openid`, `profile`, `email`, `User.Read`
  - User Info: Microsoft Graph `/v1.0/me`
- GitHub
  - Authorization Code
  - Scopes: `read:user`, `user:email` (optional for primary email)
  - User Info: `https://api.github.com/user`, `.../emails`

### Security
- Spring Security configuration:
  - All endpoints require authentication except `/`, `/login`, `/error`, static assets.
  - CSRF protection enabled; logout via POST with CSRF token.
  - Session cookie attributes: HttpOnly, Secure (in HTTPS), SameSite=Lax by default.
  - OAuth2 state and nonce are enforced by Spring Security; PKCE enabled where supported.
- Input validation and robust error handling on all controllers.
- Secrets (client IDs/secrets, DB credentials) are not committed to source control and are provided via environment variables or externalized config.

### Configuration (application.yml examples)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/app
    username: app
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  thymeleaf:
    cache: true
  flyway:
    enabled: true

  security:
    oauth2:
      client:
        registration:
          azure:
            client-id: ${AZURE_CLIENT_ID}
            client-secret: ${AZURE_CLIENT_SECRET}
            scope: openid, profile, email, User.Read
            provider: azure
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: read:user, user:email
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
        provider:
          azure:
            issuer-uri: https://login.microsoftonline.com/${AZURE_TENANT_ID}/v2.0
          github:
            authorization-uri: https://github.com/login/oauth/authorize
            token-uri: https://github.com/login/oauth/access_token
            user-info-uri: https://api.github.com/user
            user-name-attribute: id
```

Development profile specifics (example `application-dev.yml`):
```yaml
server:
  port: 3000

spring:
  thymeleaf:
    cache: false
  security:
    oauth2:
      client:
        registration:
          github:
            redirect-uri: "{baseUrl}/auth/callback/{registrationId}"
```

Security configuration note (for Java config): ensure the OAuth2 login redirection endpoint base URI matches the development callback path. For example:

```text
http
  .authorizeHttpRequests(auth -> auth
    .requestMatchers("/", "/login", "/error", "/css/**", "/js/**").permitAll()
    .anyRequest().authenticated()
  )
  .oauth2Login(oauth -> oauth
    .redirectionEndpoint(redir -> redir.baseUri("/auth/callback/*"))
  );
```

Also configure your GitHub OAuth App with the callback URL:
- http://localhost:3000/auth/callback/github

For development, replace the datasource with H2, enable Thymeleaf cache=false, and optionally set `server.ssl.enabled=false` if not using HTTPS locally.

### Error Handling
- Show a friendly error page on authentication failures with a short, actionable message and a link back to the login page.
- Log technical details (stack traces, OAuth errors) server-side only.

### Logging and Observability
- Use SLF4J/Logback with structured logging. Mask secrets in logs.
- Expose health endpoints via Spring Boot Actuator (optional): `/actuator/health`, `/actuator/info`.

## Non-Functional Requirements
- Performance: Initial page load < 1s on typical development hardware; OAuth roundtrip depends on provider latency.
- Security: Follows Spring Security best practices; HTTPS is required in production; cookies flagged `Secure` in production.
- Portability: Runs on Java 17+; packaged as a fat JAR.
- Maintainability: Configuration via externalized properties; database migrations via Flyway.
- Testability: Unit tests for controllers/services; integration tests for OAuth2 login success and failure flows using WireMock or Spring Security test support.

## Out of Scope
- Role-based authorization beyond login (only basic authentication guard is required).
- Multi-tenancy and advanced consent experience.
- Persisting access/refresh tokens long-term or using them beyond immediate profile retrieval.

---
This document replaces the earlier Rust-based requirements with a Java + Spring Boot stack and aligns acceptance criteria with common Spring Security OAuth2 client flows, Thymeleaf server-side rendering, and Spring Data JPA persistence.
