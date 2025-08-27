# Task List — Implementation Plan (Java + Spring Boot)

This task list breaks down the steps to implement the application described in `docs/requirements.md` and `docs/design.md`. The plan prioritizes getting a working skeleton with build/test infrastructure and a static page running on port 3000 in development, then iterates to full functionality.

Legend:
- [ ] Not started
- [~] In progress
- [x] Done

Prerequisites:
- JDK 17+
- GitHub account (for OAuth app); Microsoft Entra ID tenant/app registration (optional initially)
- PostgreSQL available for production (H2 used in dev/test)

---

## Phase 0 — Repository hygiene and scaffolding
- [ ] Ensure LICENSE and minimal README exist and reference Java 17 + Spring Boot
- [ ] Decide build tool: Maven (recommended for this project)
- [ ] Establish standard project layout: `src/main/java`, `src/main/resources`, `src/test/java`

## Phase 1 — Build and test infrastructure (skeleton first)
- [x] Initialize Maven Spring Boot project (Java 17)
  - Group/Artifact: e.g., `com.example:hello-sso`
  - Add Spring Boot 3.x parent and plugins (spring-boot-maven-plugin)
- [x] Dependencies
  - [x] spring-boot-starter-web
  - [x] spring-boot-starter-thymeleaf
  - [x] spring-boot-starter-security
  - [x] spring-boot-starter-oauth2-client
  - [x] spring-boot-starter-data-jpa
  - [x] flyway-core
  - [x] database drivers: `com.h2database:h2` (test+dev), `org.postgresql:postgresql` (prod)
  - [x] testing: spring-boot-starter-test (JUnit 5), Mockito
  - [ ] optional: spring-boot-starter-actuator (health/info)
- [x] Maven config
  - [x] Java 17 toolchain or `maven-compiler-plugin` set to 17
  - [x] surefire/failsafe setup (JUnit Platform)
  - [x] Reproducible builds, dependency management via Spring BOM
- [x] CI (GitHub Actions)
  - [x] Workflow: checkout → setup-java (temurin 17) → cache maven → mvn -B verify
  - [x] Fail on test or style errors
- [ ] Code quality (optional early or later)
  - [ ] Spotless/Checkstyle or formatting policy

## Phase 2 — Minimal runnable app (static page on port 3000 in dev)
- [x] Create `Application` main class to start Spring Boot
- [x] Add `application.yml` (base) and `application-dev.yml` (development)
  - [x] Base: enable Flyway, thymeleaf cache=true, JPA `open-in-view=false`, ddl-auto=validate
  - [x] Dev: `server.port: 3000`, `spring.thymeleaf.cache: false`; H2 datasource
- [x] Controller for `/` (and `/login` alias) that serves a simple static Thymeleaf page
- [x] Template `templates/login.html` with placeholder content (no auth yet)
- [x] Verify: `mvn spring-boot:run -Dspring-boot.run.profiles=dev` serves http://localhost:3000/

## Phase 3 — Security baseline (no external OAuth yet)
- [x] Add Spring Security configuration class
  - [x] Permit `/`, `/login`, `/error`, and static assets (`/css/**`, `/js/**`, `/images/**`)
  - [x] Require auth for all other endpoints
  - [x] Enable CSRF (default)
- [x] Ensure logout endpoint configured: POST `/logout` → redirect `/login?logout`
- [x] Add login page buttons that link to `/oauth2/authorization/azure` and `/oauth2/authorization/github` (links can be inert until OAuth is configured)

## Phase 4 — Persistence foundation
- [x] Define JPA `User` entity (fields per requirements: provider, externalId, username, displayName, email, avatarUrl, timestamps)
- [x] Create `UserRepository` with lookup by provider + externalId
- [x] Add Flyway migration `V1__init.sql` to create `app_user` with unique constraint `(provider, external_id)`
- [x] Configure H2 for dev/test, PostgreSQL for prod
- [x] Add basic data access test against H2

## Phase 5 — OAuth2 client configuration
- [x] Base `application.yml` OAuth2 client sections (Azure and GitHub) using env vars
  - [x] Azure: issuer-uri, scopes, redirect-uri default `{baseUrl}/login/oauth2/code/{registrationId}`
  - [x] GitHub: authorization/token/user-info URIs, user-name-attribute=id
- [x] Dev override in `application-dev.yml`
  - [x] `spring.security.oauth2.client.registration.github.redirect-uri: {baseUrl}/auth/callback/{registrationId}`
  - [ ] Optionally set Azure similarly if needed
- [x] SecurityConfig: set redirection endpoint base URI for dev `"/auth/callback/*"`
- [x] Document required environment variables in README
- [x] Configure a GitHub OAuth App with callback: `http://localhost:3000/auth/callback/github`

## Phase 6 — Provider profile mapping and user onboarding
- [x] Implement `ProviderProfile` record to normalize user data
- [x] Implement `CustomOAuth2UserService`
  - [x] Extend `DefaultOAuth2UserService`
  - [x] For GitHub: fetch `/user`, and if email is null and scope granted, fetch `/user/emails` to find primary verified
  - [x] For Azure: map Graph `/v1.0/me` fields (displayName, mailNickname, userPrincipalName)
  - [x] Build `ProviderProfile` with name resolution priority
- [x] Implement `UserService`
  - [x] `getOrCreateFromProviderProfile` upserts `User`, updates `lastLoginAt`, changed attributes
  - [x] Transactional boundaries
- [x] Wire CustomOAuth2UserService into Security config `.userInfoEndpoint(...userService(...))`

## Phase 7 — Authenticated experience and logout
- [ ] Implement `MeController` (`GET /me`)
  - [ ] Resolve authenticated principal and load local `User`
  - [ ] Pass user model (display name, avatar) to view
- [ ] Thymeleaf templates
  - [ ] `login.html`: provider buttons linking to `/oauth2/authorization/{id}`; show errors, logout message
  - [ ] `me.html`: "hello {user_name}", avatar, logout form with CSRF hidden input
  - [ ] `error.html`: friendly error page with a retry link
- [ ] Verify access control
  - [ ] Anonymous to `/me` → redirected to `/login`
  - [ ] POST `/logout` terminates session and redirects to `/login?logout`

## Phase 8 — Error handling and UX polish
- [ ] Global error handling (`@ControllerAdvice` or `ErrorController`) to render friendly error page
- [ ] Handle OAuth failures and consent denials with redirect to `/login?error`
- [ ] Input validation and safe defaults

## Phase 9 — Logging, observability, and hardening
- [ ] Configure SLF4J/Logback; mask secrets
- [ ] Optional: add Actuator (`/actuator/health`, `/actuator/info`)
- [ ] Cookie/session hardening (HttpOnly, Secure in HTTPS, SameSite=Lax)
- [ ] Review security headers (Spring defaults; consider additional headers)

## Phase 10 — Testing
- [ ] Unit tests
  - [ ] Name resolution logic and provider mapping (GitHub/Azure)
  - [ ] `UserService` upsert and timestamp updates
- [ ] Integration tests (Spring Security/MVC)
  - [ ] Access control: `/me` requires auth; `/` is public
  - [ ] OAuth2 login success/failure flows using Spring Security test support or WireMock
  - [ ] Persistence with H2; optional Testcontainers for PostgreSQL
- [ ] Template tests (optional): verify greeting and CSRF field presence

## Phase 11 — Production configuration and packaging
- [ ] Ensure production `application.yml` uses PostgreSQL and Flyway
- [ ] Build fat JAR via Maven
- [ ] Document environment variables and deployment instructions (HTTPS required in prod)
- [ ] Optional: Dockerfile and containerization instructions

## Phase 12 — Documentation
- [ ] Update README with:
  - [ ] Quickstart (dev): run on port 3000, profiles, OAuth setup
  - [ ] Configuration reference (env vars)
  - [ ] Development workflow (run, test, lint)
  - [ ] Troubleshooting (common OAuth errors)

---

## Acceptance Checklist (from requirements)
- [ ] Login page offers Microsoft 365 and GitHub buttons linking to `/oauth2/authorization/{id}`
- [ ] Dev callback for GitHub: `http://localhost:3000/auth/callback/github` works; redirection base URI aligned in security config
- [ ] Successful login creates/updates local `User` and redirects to `/me`
- [ ] `/me` displays `hello {user_name}` with proper name resolution
- [ ] Logout via POST `/logout` clears session and redirects to `/login?logout`
- [ ] H2 (dev/test) and PostgreSQL (prod) with Flyway migrations
- [ ] Thymeleaf used for server-side rendering; cache disabled in dev, enabled in prod
- [ ] CSRF enabled; logout form includes CSRF token
- [ ] Friendly error page and safe handling of OAuth errors
- [ ] Tests cover units and key integrations
