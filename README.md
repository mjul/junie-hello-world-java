# junie-hello-world-java
Testing the JetBrains Junie PR agent

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
