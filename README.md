# junie-hello-world-java
A small Spring Boot (Java 17) web app that demonstrates SSO login with Microsoft 365 (Entra ID) and GitHub, renders pages with Thymeleaf, and stores a minimal user profile with JPA. Includes security hardening, Flyway migrations, and tests.

## Quickstart (Development)

Prerequisites:
- Java 17+
- Maven 3.9+
- A GitHub OAuth App (for local dev)

Steps:
1. Create a GitHub OAuth App:
   - Homepage URL: http://localhost:3000/
   - Authorization callback URL: http://localhost:3000/auth/callback/github
2. Export environment variables (example):
   - GITHUB_CLIENT_ID=... 
   - GITHUB_CLIENT_SECRET=...
   - Optional for Microsoft 365 if you also test Azure locally:
     - AZURE_CLIENT_ID=...
     - AZURE_CLIENT_SECRET=...
     - AZURE_TENANT_ID=common
3. Run in dev profile (port 3000; H2):
   - mvn -Dspring-boot.run.profiles=dev spring-boot:run
   - Or: mvn -B -DskipTests package && java -jar target/hello-sso-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
4. Open http://localhost:3000/ and click the GitHub (or Microsoft) button to log in.

Notes:
- In dev, the redirect endpoint is /auth/callback/{registrationId}. The security config and application-dev.yml are aligned for this.
- In production, the default redirect is /login/oauth2/code/{registrationId}.

## Configuration Reference

OAuth 2.0 providers (read from environment variables):
- GITHUB_CLIENT_ID
- GITHUB_CLIENT_SECRET
- AZURE_CLIENT_ID
- AZURE_CLIENT_SECRET
- AZURE_TENANT_ID (defaults to "common" if not set)

Database (production via PostgreSQL; dev/test use H2 automatically):
- SPRING_DATASOURCE_URL (e.g., jdbc:postgresql://localhost:5432/app)
- SPRING_DATASOURCE_USERNAME (e.g., app)
- SPRING_DATASOURCE_PASSWORD or DB_PASSWORD

Profiles:
- dev — Port 3000, Thymeleaf cache disabled, H2 datasource, GitHub redirect uses /auth/callback/{registrationId}
- test — Used by the test suite
- prod (default) — PostgreSQL, Thymeleaf cache enabled

## Development Workflow

Common commands:
- Run dev server: mvn -Dspring-boot.run.profiles=dev spring-boot:run
- Run tests: mvn -B test (or mvn -B verify)
- Format code (Spotless): mvn spotless:apply
- Check formatting: mvn spotless:check
- Package JAR: mvn -B -DskipTests package

Git hook (optional):
- Enable automatic formatting before commit:
  - git config core.hooksPath .githooks
  - chmod +x .githooks/pre-commit (if needed)

## Troubleshooting (OAuth and Dev)

- Redirect URI mismatch: Ensure your GitHub OAuth App callback is exactly http://localhost:3000/auth/callback/github when using the dev profile.
- Invalid client or secret: Verify GITHUB_CLIENT_ID/GITHUB_CLIENT_SECRET (and Azure equivalents) are exported in the same shell running the app.
- Missing email from GitHub: Grant the user:email scope in your app; the application will call /user/emails when needed.
- CSRF on logout: Logout is POST /logout with CSRF. Use the logout form on /me; do not call it via GET.
- H2 memory DB: Dev/test use in‑memory DB by default; data resets on restart. For persistence, configure a file URL or use PostgreSQL locally.
- Cookies in production: Run behind HTTPS and set correct proxy headers. Consider server.forward-headers-strategy=framework.

## OAuth2 Configuration (Details)

The base application.yml defines OAuth2 clients for Azure and GitHub using the environment variables above. In development (dev profile), GitHub's redirect URI is overridden to use a friendlier path.

Development callback URL (GitHub):
- http://localhost:3000/auth/callback/github

Ensure your Spring Security OAuth2 login redirection endpoint accepts this path (base URI /auth/callback/* in dev).

## Production configuration (PostgreSQL)

The base application.yml is set up with production defaults using PostgreSQL. Configure via environment variables:
- SPRING_DATASOURCE_URL (e.g., jdbc:postgresql://localhost:5432/app)
- SPRING_DATASOURCE_USERNAME (e.g., app)
- SPRING_DATASOURCE_PASSWORD or DB_PASSWORD

Flyway migrations are enabled and run on startup.

## Build and Run

- Build and test: mvn -B verify
- Package fat JAR: mvn -DskipTests package
- Run (prod defaults): java -jar target/hello-sso-0.0.1-SNAPSHOT.jar
- Run in development (port 3000, H2): java -jar target/hello-sso-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

HTTPS is required in production. If running behind a reverse proxy/ingress, ensure proper forwarding headers are configured (e.g., server.forward-headers-strategy=framework) and cookies are marked Secure when using HTTPS.
