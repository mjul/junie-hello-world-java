# junie-hello-world-java
Testing the JetBrains Junie PR agent

## OAuth2 Configuration

Set the following environment variables when running the application:

- AZURE_CLIENT_ID
- AZURE_CLIENT_SECRET
- AZURE_TENANT_ID (defaults to "common" if not set)
- GITHUB_CLIENT_ID
- GITHUB_CLIENT_SECRET

The base `application.yml` defines OAuth2 clients for Azure and GitHub using these env vars. In development (`dev` profile), GitHub's redirect URI is overridden to use a friendlier path.

## Development OAuth callback URL

For GitHub OAuth in development, configure your GitHub OAuth App callback URL to:

- http://localhost:3000/auth/callback/github

App configuration notes (example `application-dev.yml`):

```yaml
server:
  port: 3000
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            redirect-uri: "{baseUrl}/auth/callback/{registrationId}"
```

Ensure your Spring Security OAuth2 login redirection endpoint accepts this path (e.g., base URI `/auth/callback/*`).

## Code formatting and pre-commit hook

This project uses Spotless (Google Java Format) to ensure consistent code layout.

- Format code manually anytime:
  - mvn spotless:apply
- Check formatting without changing files:
  - mvn spotless:check

To automatically format code before each commit, enable the provided Git hook once per clone:

1. git config core.hooksPath .githooks
2. (Optional) Make sure the hook is executable on your system:
   - chmod +x .githooks/pre-commit

The pre-commit hook will:
- Run mvn -DskipTests=true spotless:apply
- Stage any changes it makes
- Run mvn -DskipTests=true spotless:check and block the commit if formatting still fails

## Production configuration (PostgreSQL)

The base application.yml is set up for production defaults using PostgreSQL. Configure via environment variables:

- SPRING_DATASOURCE_URL (e.g., jdbc:postgresql://localhost:5432/app)
- SPRING_DATASOURCE_USERNAME (e.g., app)
- SPRING_DATASOURCE_PASSWORD or DB_PASSWORD

Flyway migrations are enabled by default and run on startup.

## Build and Run

- Build and test: mvn -B verify
- Package fat JAR: mvn -DskipTests package
- Run (prod defaults): java -jar target/hello-sso-0.0.1-SNAPSHOT.jar
- Run in development (port 3000, H2): java -jar target/hello-sso-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

HTTPS is required in production. If running behind a reverse proxy/ingress, ensure proper forwarding headers are configured (e.g., server.forward-headers-strategy=framework) and cookies are marked Secure when using HTTPS.
