# Design Document (Java + Spring Boot)

## 1. Overview
This document describes the technical design for a small Java 17+ Spring Boot web application that provides Single Sign-On (SSO) authentication with Microsoft 365 (Entra ID/Azure AD) and GitHub, renders pages server‑side with Thymeleaf, and persists minimal user profile data using Spring Data JPA. It implements the requirements defined in `docs/requirements.md`.

Goals:
- Simple, secure OAuth2/OIDC login via Microsoft 365 and GitHub
- Personalized page for the authenticated user with logout
- Server-side rendering with Thymeleaf
- Persistence of minimal user profiles and timestamps via JPA
- Production-ready defaults; flexible configuration for development

Non-goals (see Out of Scope in requirements): Role-based authorization beyond basic authentication, multi-tenancy, long-term token storage.

## 2. Architecture Overview
Layers/components:
- Presentation (Web MVC + Thymeleaf): Controllers and server-side HTML templates
- Security (Spring Security OAuth2 Client): Login/logout, sessions, CSRF, OAuth redirect handling
- Application Services: User onboarding/update logic; profile mapping from providers
- Persistence (JPA + Flyway): User entity, repository, migrations, PostgreSQL/H2
- Configuration: Externalized properties for providers/DB; profiles for dev/prod

Key runtime interactions:
1) Anonymous user -> GET / → Login page with provider options
2) User selects provider → redirect to /oauth2/authorization/{registrationId}
3) Provider authenticates user → redirects to app’s redirect URI
4) Spring Security exchanges code for token; delegates to a custom OAuth2UserService to fetch/compose profile(s)
5) Application maps external profile to local User; creates/updates record; authenticates session
6) User redirected to /me → Thymeleaf renders greeting and logout button
7) Logout via POST /logout → session invalidated; redirect to /login?logout

## 3. Main Components and Responsibilities
- SecurityConfig (Java config)
  - Defines authorization rules: public endpoints (/, /login, /error, static), everything else authenticated
  - Enables OAuth2 login and configures redirection endpoint base URI (supports dev override `/auth/callback/*`)
  - CSRF enabled by default; logout via POST /logout
  - Session cookie hardening (HttpOnly, Secure in HTTPS, SameSite=Lax)

- CustomOAuth2UserService (Service)
  - Extends DefaultOAuth2UserService (or implements OAuth2UserService) to:
    - Call provider user info (and, for GitHub, emails endpoint if needed)
    - Normalize and map to a ProviderProfile abstraction
    - Invoke UserService to upsert the local User entity
    - Return an OAuth2User principal with authorities and attributes as needed

- UserService (Service)
  - getOrCreateFromProviderProfile(ProviderProfile profile)
  - Update lastLoginAt and changed attributes (display name, email, avatar)

- UserRepository (JPA Repository)
  - CRUD for User entity; lookup by provider + externalId

- Controllers (Web MVC)
  - LoginController: GET / and GET /login → renders login page with provider links
  - MeController: GET /me → resolves authenticated principal, loads local User, renders greeting
  - Error handling: GlobalExceptionHandler or ErrorController → renders friendly error page

- Thymeleaf Templates
  - templates/login.html — provider buttons/links
  - templates/me.html — greeting, avatar, logout form (POST), CSRF token
  - templates/error.html — friendly error

- Flyway Migrations
  - V1__init.sql — create table user and constraints

## 4. URL Mapping
- GET `/` → Login page (public)
- GET `/login` → Login page (public)
- GET `/oauth2/authorization/{registrationId}` → Initiate OAuth2
- GET `/login/oauth2/code/{registrationId}` → OAuth2 redirect (default)
- GET `/auth/callback/{registrationId}` → OAuth2 redirect (dev override)
- GET `/me` → Personal page (authenticated)
- POST `/logout` → Logout (authenticated, CSRF)
- GET `/error` → Error page (public)

## 5. Data Model
Entity: User
- id: UUID (or generated Long)
- provider: enum/string {AZURE, GITHUB}
- externalId: String (Azure objectId or GitHub id)
- username: String
- displayName: String
- email: String (nullable)
- avatarUrl: String (nullable)
- createdAt, updatedAt, lastLoginAt: timestamps
- Unique constraint: (provider, externalId)

JPA sketch:
```text
@Entity
@Table(name = "app_user", uniqueConstraints = @UniqueConstraint(name = "uk_provider_external_id", columnNames = {"provider", "external_id"}))
public class User {
  @Id
  @GeneratedValue
  private UUID id; // or @GeneratedValue(strategy = GenerationType.IDENTITY) Long

  @Column(nullable = false)
  private String provider; // or @Enumerated(EnumType.STRING)

  @Column(name = "external_id", nullable = false)
  private String externalId;

  @Column(nullable = false)
  private String username;

  private String displayName;
  private String email;
  private String avatarUrl;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;
  @Column(nullable = false)
  private Instant updatedAt;
  private Instant lastLoginAt;

  // setters/getters; @PrePersist/@PreUpdate to maintain timestamps
}
```

Flyway V1__init.sql sketch:
```sql
CREATE TABLE app_user (
  id UUID PRIMARY KEY,
  provider VARCHAR(32) NOT NULL,
  external_id VARCHAR(191) NOT NULL,
  username VARCHAR(191) NOT NULL,
  display_name VARCHAR(191),
  email VARCHAR(191),
  avatar_url VARCHAR(512),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  last_login_at TIMESTAMP WITH TIME ZONE,
  CONSTRAINT uk_provider_external_id UNIQUE (provider, external_id)
);
```

Notes:
- Use appropriate types for H2 vs PostgreSQL; Flyway handles evolution.
- Do not store access/refresh tokens by default.

## 6. OAuth2 Providers and Profile Mapping
Provider abstraction:
```text
public record ProviderProfile(
  String provider, // "AZURE" or "GITHUB"
  String externalId,
  String username,
  String displayName,
  String email,
  String avatarUrl
) {}
```

Microsoft 365 (Graph /v1.0/me):
- externalId: `id` (objectId in tenant)
- username: prefer `mailNickname` or `userPrincipalName`
- displayName: `displayName`
- email: `mail` (may be null; fallback to `userPrincipalName` if mail-like)
- avatarUrl: optional (Graph photo endpoint requires another call; commonly omit or derive later)
- Scopes: openid, profile, email, User.Read
- PKCE recommended; Spring Security can enable PKCE automatically for public clients

GitHub (https://api.github.com/user and /user/emails):
- externalId: `id`
- username: `login`
- displayName: `name` (may be null/private → fallback to `login`)
- email: `email` may be null; if scope `user:email` is granted, call `/user/emails` to find primary verified email
- avatarUrl: `avatar_url`
- Scopes: read:user (+ user:email optional)

Name resolution priority (as requirements):
- Microsoft 365: displayName → mailNickname → userPrincipalName
- GitHub: name → login

## 7. Security Configuration
- Authorization rules:
  - Permit: `/`, `/login`, `/error`, static assets (`/css/**`, `/js/**`, `/images/**`)
  - Authenticated: all other endpoints (e.g., `/me`, `/logout`)
- OAuth2 login:
  - Default redirect base: `/login/oauth2/code/{registrationId}`
  - Dev override supported: `.oauth2Login(oauth -> oauth.redirectionEndpoint(r -> r.baseUri("/auth/callback/*")))`
- CSRF:
  - Enabled; include Thymeleaf `<input type="hidden" name="_csrf" ...>` in logout form
- Sessions:
  - Server-side HTTP sessions; cookies HttpOnly, Secure (in HTTPS), SameSite=Lax
  - For distributed deployments, consider Spring Session (JDBC/Redis) — optional
- Secrets:
  - Client secrets and DB credentials via environment variables or external config

Example Java config excerpt:
```text
http
  .authorizeHttpRequests(auth -> auth
    .requestMatchers("/", "/login", "/error", "/css/**", "/js/**", "/images/**").permitAll()
    .anyRequest().authenticated()
  )
  .oauth2Login(oauth -> oauth
    .redirectionEndpoint(redir -> redir.baseUri("/auth/callback/*")) // dev override; use default in prod
    .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
  )
  .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout"));
```

## 8. Configuration and Profiles
application.yml (prod defaults):
- DataSource: PostgreSQL
- JPA: ddl-auto: validate, open-in-view=false
- Thymeleaf: cache=true
- Flyway: enabled=true
- Spring Security OAuth2 clients for Azure and GitHub defined via environment variables

application-dev.yml (development):
- server.port: 3000
- Thymeleaf: cache=false
- GitHub client redirect-uri: `{baseUrl}/auth/callback/{registrationId}`
- Optionally use H2 datasource; disable HTTPS locally as needed

The configuration blocks in `docs/requirements.md` are the source of truth.

## 9. Controllers and Views
- LoginController
  - Renders `login.html` for `/` and `/login`
  - Provides links/buttons to `/oauth2/authorization/azure` and `/oauth2/authorization/github`
- MeController
  - `@GetMapping("/me")`
  - Extracts the authenticated principal, resolves local User, derives display name, model.addAttribute("user", ...)
  - Renders `me.html` with greeting `hello {user_name}`, avatar (if any), and logout form
- ErrorController / ExceptionHandler
  - Renders `error.html` with friendly message; logs details server-side

Thymeleaf snippets:
```html
<!-- logout form in me.html -->
<form th:action="@{/logout}" method="post">
  <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />
  <button type="submit">Logout</button>
</form>
```

## 10. Persistence and Transactions
- UserService operations are transactional when creating/updating a user
- Unique constraint on (provider, externalId) prevents duplicates on concurrent logins
- Timestamps maintained via entity callbacks (@PrePersist/@PreUpdate) or in service code

## 11. Error Handling Strategy
- Authentication/OAuth errors: redirect to `/login?error` and render friendly text with a link to retry
- Server/logging: use SLF4J/Logback; include correlation IDs if available
- Input validation in controllers; fail fast with appropriate HTTP status for non-auth flows

## 12. Logging and Observability
- Log structured events for login success/failure, user creation/update, logout
- Mask secrets in logs
- Optionally include Spring Boot Actuator (`/actuator/health`, `/actuator/info`) for health checks

## 13. Testing Strategy
- Unit tests
  - Name resolution and ProviderProfile mapping for both providers
  - UserService upsert logic; timestamp updates
- Integration tests
  - Spring Security test: access control (anonymous redirected; authenticated sees /me)
  - OAuth2 login success/failure using Spring Security test support or WireMock to simulate provider responses
  - Persistence with H2; optional Testcontainers for PostgreSQL
- Template tests (optional) with Spring MVC Test: verify greeting and CSRF in logout form

## 14. Deployment
- Packaged as a fat JAR
- Run with Java 17+
- Provide environment variables for DB and OAuth2 client credentials
- HTTPS required in production; ensure `server.forward-headers-strategy` is configured properly behind reverse proxies
- Database migrations run automatically via Flyway

## 15. Risks and Trade-offs
- Provider data variability (missing email/name): handled via fallbacks and optional emails call (GitHub)
- Dev vs prod redirect URIs: mitigated by configurable redirection endpoint base URI
- Not persisting tokens: simplest approach; advanced scenarios would require secure storage and rotation
- Session stickiness vs Spring Session: for multi-node deployments, consider Spring Session

## 16. Sequence Flows (Text)
Login (GitHub example, dev):
1) GET / → login.html
2) Click "Login with GitHub" → GET /oauth2/authorization/github
3) Redirect to GitHub → user authenticates/consents
4) GitHub redirects back to http://localhost:3000/auth/callback/github with code+state
5) Spring Security exchanges code for access token
6) CustomOAuth2UserService loads `https://api.github.com/user`; if email null and scope present, loads `/user/emails`
7) Map to ProviderProfile; UserService upsert; authenticate session
8) Redirect to /me → Thymeleaf renders greeting and logout

Logout:
1) POST /logout with CSRF token
2) Spring Security invalidates session; clears JSESSIONID
3) Redirect to /login?logout

## 17. Requirement Traceability
- Requirement 1 (Authenticate via M365 or GitHub)
  - OAuth2 client config; `/oauth2/authorization/{id}` links; CustomOAuth2UserService; dev redirect override; user creation on first login; error redirection
- Requirement 2 (Personalized page and access control)
  - MeController + Thymeleaf template; name resolution priority; access control rules; logout button
- Requirement 3 (Logout)
  - POST `/logout` with CSRF; session invalidation; redirect to `/login?logout`; cookie cleared by framework
- Requirement 4 (Persistence)
  - User entity + repository; Flyway migrations; update lastLoginAt on each login; JPA with PostgreSQL/H2
- Requirement 5 (Server-side rendering)
  - Thymeleaf templates; server-side injection of user data; template caching in prod; friendly error page

## 18. Future Extensions
- Avatar retrieval for Microsoft Graph photo (optional separate call)
- Add Spring Session (JDBC/Redis) for clustered deployments
- Add audit logging and login history
- Role-based authorization and application features beyond login

---
This design aligns with `docs/requirements.md`, provides clear component boundaries, and ensures secure, testable, and maintainable implementation details for both development and production environments.
