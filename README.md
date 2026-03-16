# kurtuba_auth

`kurtuba_auth` is a Java 21 Spring Boot authentication server for Kurtuba applications. It provides user registration, account activation, login, JWT access-token issuance, refresh-token rotation, logout/token blocking, password reset, JWKS publishing, service-client authentication, and supporting account-security flows.

Technically, the service is built around Spring Security, Spring Data JPA, JWT/JWKS, persisted token state, JDBC-backed session support, relational persistence, and optional integrations for mail, SMS, Redis, and scheduled message handling.

This repository is relevant if you are looking for:

- a Spring Boot authentication server
- a Java JWT auth service with JWKS
- refresh-token rotation with persisted token state
- account activation and password reset flows
- cookie-based JWT handling for web clients
- service-to-service token issuance
- JWK-based signing key storage and rotation

## Documentation

- [Overview](docs/overview.md)
- [Capabilities](docs/capabilities.md)
- [Authentication Model](docs/auth-model.md)
- [Key Management](docs/key-management.md)
- [Configuration Reference](docs/configuration.md)
- [Demo Defaults vs Production Requirements](docs/demo-vs-production.md)
- [API Reference](docs/api.md)

## Key Technical Topics

- JWT access tokens with JWKS publication
- Refresh-token rotation and one-time-use semantics
- Registered-client-aware token behavior
- Account activation by code or link
- Password reset by code or hosted link
- Token blocking and revocation
- RSA-oriented JWK/JWE signing-key packaging with rollover support

## Notes

The current repository contains documentation for both implemented capabilities and operational caveats. Some defaults are suitable for demo or local development only, especially around datasource choice, provider credentials, jobs, rate limiting, cookie security, and signing-key handling. Start with [Overview](docs/overview.md), then read [Demo Defaults vs Production Requirements](docs/demo-vs-production.md) and [Key Management](docs/key-management.md) before using the service in a real deployment.
