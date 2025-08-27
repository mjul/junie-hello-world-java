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
