# kurtuba-auth

`kurtuba-auth` is a Java 21 Spring Boot authentication server. It provides user registration, account activation, login, JWT access-token issuance, refresh-token rotation, logout/token blocking, password reset, JWKS publishing, service-client authentication, and supporting account-security flows.

Technically, the service is built around Spring Security, Spring Data JPA, JWT/JWKS, persisted token state, JDBC-backed session support, relational persistence, and optional integrations for mail, SMS, Redis, and scheduled message handling. The service also has a GraalVM native-image transition path that keeps the important runtime surface active, including admin pages, Swagger UI, rate limiting, Redis integration, mail, SMS, and logging.

This repository is relevant if you are looking for:

- a Spring Boot authentication server
- a Java JWT auth service with JWKS
- refresh-token rotation with persisted token state
- account activation and password reset flows
- cookie-based JWT handling for web clients
- service-to-service token issuance
- JWK-based signing key storage and rotation
- a Spring Boot auth server backed by PostgreSQL and Flyway
- a Spring Boot auth service with a documented GraalVM native-image migration path

## Documentation

- [Overview](docs/overview.md)
- [Backend Quickstart](docs/backend-quickstart.md)
- [Capabilities](docs/capabilities.md)
- [Authentication Model](docs/auth-model.md)
- [Admin Pages](docs/admin-pages.md)
- [Key Management](docs/key-management.md)
- [Configuration Reference](docs/configuration.md)
- [PostgreSQL Setup](docs/postgresql.md)
- [GraalVM Native Transition](docs/graalvm-transition.md)
- [Demo Defaults vs Production Requirements](docs/demo-vs-production.md)
- [API Reference](docs/api.md)

## License

This repository is licensed under the Apache License 2.0. See [LICENSE](LICENSE).

## Key Technical Topics

- JWT access tokens with JWKS publication
- Refresh-token rotation and one-time-use semantics
- Registered-client-aware token behavior
- Account activation by code or link
- Password reset by code or hosted link
- Token blocking and revocation
- RSA-oriented JWK/JWE signing-key packaging with rollover support
- PostgreSQL-backed local, test, and deployment paths
- GraalVM native-image build support with documented migration notes

## Notes

The current repository contains documentation for both implemented capabilities and operational caveats. The service is PostgreSQL-first, and local/demo usage should still be treated as a configuration concern rather than a separate in-memory database mode. Some defaults remain suitable only for local development, especially around provider credentials, jobs, rate limiting, cookie security, and signing-key handling.

## Local Infra

A minimal local stack is provided in [docker-compose.yml](/Users/murat/projects/kurtuba-auth/docker-compose.yml).

- Start the full local stack: `docker compose up -d`
- Start PostgreSQL only: `docker compose up -d postgres`
- Bootstrap the local database and roles: `./scripts/bootstrap-local-db.sh`

With the checked-in local defaults, the default local database credentials are:

- database: `kurtuba_auth`
- application username: `kurtuba_auth_user`
- application password: `12345`
- flyway username: `kurtuba_auth_migrator`
- flyway password: `12345`

With your current checked-in defaults, the intended local run path is:

```bash
docker compose up -d
./scripts/bootstrap-local-db.sh
mvn spring-boot:run
```

Start with [Backend Quickstart](docs/backend-quickstart.md) if you are evaluating the repo as an auth server for your backend. Then read [Overview](docs/overview.md), [Configuration Reference](docs/configuration.md), [PostgreSQL Setup](docs/postgresql.md), [Demo Defaults vs Production Requirements](docs/demo-vs-production.md), and [Key Management](docs/key-management.md) before using the service in a real deployment.
