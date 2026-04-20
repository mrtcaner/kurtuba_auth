# Configuration Reference

## Overview

`kurtuba-auth` is highly configuration-driven. Runtime behavior depends on Spring Boot settings, client definitions, token key material, message-delivery configuration, database settings, and feature toggles.

This document describes the main configuration groups, what they control, which defaults are suitable only for demo or local development, and what should be reviewed before production deployment.

## Configuration Philosophy

The current configuration includes a mix of:

- core runtime settings
- development-friendly defaults
- example third-party credentials
- feature flags that disable optional behavior
- environment-specific values that should not be used unchanged in production

In practice, configuration should be documented in two layers:

- what each property does
- whether the current default is safe for real deployment

The checked-in base config is intentionally local-development oriented. For real startup, use PostgreSQL-backed checked-in defaults or externalized environment-specific config rather than relying on ad hoc overrides.

## `server.*`

### `server.port`

Controls the HTTP port used by the application.

Typical use:
- local development port binding
- container port exposure
- reverse-proxy integration

### `server.servlet.session.tracking-modes`

Controls how servlet sessions are tracked.

Current configuration is cookie-based, which is the expected modern default.

### `server.servlet.session.persistent`

Controls session persistence behavior at the servlet container level.

This should be reviewed together with the application’s Spring Session setup.

## `auth.server.*`

### `auth.server.issuer-url`

Defines the issuer base URL used by the auth service.

This value is especially important because it affects token and JWKS-related behavior. It should reflect the externally reachable URL of the auth server in production, not just an internal container hostname.

Production requirements:
- should match the real public issuer URL
- should be stable
- should use HTTPS in production

Demo/local default:
- often points to `http://localhost:8080`

## `spring.datasource.*`

### `spring.datasource.url`

Defines the JDBC connection string.

Runtime datasource configuration should point at PostgreSQL. Local, test, and production usage now share the same database family even if the surrounding operational setup differs.

### `spring.datasource.driver-class-name`

The JDBC driver implementation to use.

### `spring.datasource.username`

Database username.

### `spring.datasource.password`

Database password.

Production requirements:
- use a dedicated database user
- avoid embedding secrets directly in committed config
- inject through environment variables or secret management

### PostgreSQL note

The public repo has been validated against a real PostgreSQL instance using:

- a separate throwaway database
- Flyway enabled
- Hibernate `ddl-auto=validate`

That confirms the checked-in baseline migration is usable in a PostgreSQL-backed startup path when the database is prepared correctly.

## `spring.jpa.*`

### `spring.jpa.show-sql`

Controls SQL logging.

Useful in development, but often noisy or unsafe in production if query contents are sensitive.

### `spring.jpa.properties.hibernate.format_sql`

Controls SQL formatting.

Mostly a developer convenience option.

### `spring.jpa.properties.hibernate.default_batch_fetch_size`

Controls Hibernate batch-fetch behavior.

This is a performance-oriented setting that is usually fine to keep across environments unless profiling shows otherwise.

### `spring.jpa.open-in-view`

Controls Open Session in View behavior.

The current configuration disables it, which is usually the better default for API-oriented services.

### `spring.jpa.hibernate.ddl-auto`

Controls schema generation behavior.

Values such as `create-drop` are useful for tests and demo runs but are not safe for production databases.

Production expectation:
- use controlled schema migration rather than destructive automatic schema generation
- when Flyway is enabled, prefer `validate`

## `spring.flyway.*`

### `spring.flyway.enabled`

Controls whether Flyway migrations are applied at startup.

In normal usage this should be enabled and paired with managed migrations.

Production recommendation:
- enable Flyway
- rely on migration scripts instead of `ddl-auto=create-drop`

### Migration ownership note

`src/main/resources/db/migration/V1__baseline.sql` follows a more production-oriented PostgreSQL model and includes explicit ownership and grant assumptions.

That means:
- it works best when your database roles are prepared intentionally
- a simple developer setup can still use one shared database user for both Flyway and the app
- a stricter production setup can separate migrator and runtime roles

See `docs/postgresql.md` for both modes.

## `spring.security.oauth2.resourceserver.jwt.*`

### `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`

Defines the JWKS URI used for JWT verification.

In this service, the public JWKS endpoint is exposed at `/auth/oauth2/jwks`, so the default value should align with the auth server issuer plus that path.

Production requirements:
- must resolve correctly from the runtime environment
- should use the real issuer/JWKS endpoint
- must align with actual signing keys used by the service

## `spring.security.oauth2.client.registration.google.*`

These properties define Google OAuth client registration details.

Typical properties include:
- `clientId`
- `clientSecret`
- `redirect-uri`
- `scope`

Current defaults are examples/placeholders.

Production requirements:
- use real provider-issued credentials
- configure redirect URIs that match actual deployed frontend/backend routes
- treat the client secret as sensitive

## `spring.mail.*`

### `spring.mail.host`

SMTP host.

### `spring.mail.port`

SMTP port.

### `spring.mail.username`

SMTP username.

### `spring.mail.password`

SMTP password.

### `spring.mail.protocol`

Mail transport protocol.

These settings affect all email-based capabilities such as:
- account activation mail
- password reset mail
- other account-notification messages

Production requirements:
- real SMTP provider or transactional mail service
- valid credentials
- secure secret handling
- proper sender/domain alignment outside of code config where applicable

## `logging.level.*`

Controls log verbosity.

Current defaults may be intentionally simple for local runs. In production, logging should be tuned to support troubleshooting without exposing excessive sensitive detail.

Recommended production review:
- root log level
- SQL logging
- stack trace verbosity
- security event logging strategy

## `management.*`

### `management.health.mail.enabled`

Controls whether mail health checks are enabled.

### `management.health.redis.enabled`

Controls whether Redis health checks are enabled.

### `management.endpoints.web.exposure.include`

Defines which actuator endpoints are exposed over HTTP.

Production requirements:
- expose only the minimum needed set
- protect sensitive actuator endpoints
- avoid exposing `env` publicly

## `kurtuba.jobs.*`

### `kurtuba.jobs.enabled`

Feature flag controlling scheduled/background job behavior.

This affects capabilities such as message processing or scheduled delivery if they depend on jobs.

Demo behavior:
- often disabled to simplify local runs

Production expectation:
- enable if the deployment relies on scheduled message handling

## `kurtuba.rate-limit.*`

### `kurtuba.rate-limit.enabled`

Enables or disables rate-limiting features.

This setting matters for public-facing authentication endpoints such as registration, login, activation, or reset flows.

Production expectation:
- strongly consider enabling
- pair with a correctly configured backing store if required

## `kurtuba.server.*`

### `kurtuba.server.url`

Defines the base application/server URL used in generated links such as activation or password-reset links.

This is critical for any flow that sends clickable links to users.

Production requirements:
- must be the externally reachable HTTPS URL
- must match the frontend/backend route expectations
- must not be left as `localhost`

## `kurtuba.mail.*`

### `kurtuba.mail.from-address`

Logical sender address used in outbound mail.

### `kurtuba.mail.support-address`

Support/contact address referenced in templates and account messages.

These are separate from SMTP credentials and should also be set appropriately for real deployments.

## `kurtuba.jwk.*`

### `kurtuba.jwk.file`

Defines the JWK file location used by the service.

### `kurtuba.jwk.keys`

Defines configured signing-key decryption material or related key mappings.

This configuration is one of the most security-sensitive areas of the application.

Production requirements:
- use real managed signing keys
- control access tightly
- define a rotation strategy
- avoid committing production key material in source control
- ensure JWKS output matches the active signing keys

The checked-in config intentionally uses placeholders here so the public repo remains safe to share.

## `kurtuba.meta-change.*`

This group controls user meta-change workflows such as activation and password-reset operations.

These settings define how verification and recovery flows behave.

### `kurtuba.meta-change.email-max-try-count`

Maximum retry count for email-based user meta-change operations.

### `kurtuba.meta-change.sms-max-try-count`

Maximum retry count for SMS-based user meta-change operations.

### `kurtuba.meta-change.validity.password-reset-code.minutes`

Validity duration for password reset codes.

### `kurtuba.meta-change.validity.email.activation-code.minutes`

Validity duration for email activation codes.

### `kurtuba.meta-change.validity.email.change-code.minutes`

Validity duration for email change codes.

### `kurtuba.meta-change.validity.sms.activation-code.minutes`

Validity duration for SMS activation codes.

### `kurtuba.meta-change.validity.sms.change-code.minutes`

Validity duration for SMS change codes.

Operational impact:
- affects user experience
- affects security window length
- affects support burden for expired codes
- should be tuned deliberately for production

## `kurtuba.job.*`

These properties define message-delivery retry and scheduling behavior.

### `kurtuba.job.email.send.max-try-count`

Maximum number of retries for email sending.

### `kurtuba.job.sms.send.max-try-count`

Maximum number of retries for SMS sending.

These settings are operationally important for reliable delivery and should align with queueing, retry, and monitoring strategy.

## `kurtuba.auth-provider.*`

These properties configure provider-specific validation details used during registration or login through other identity providers.

Examples include:
- Google client id and secret
- Facebook client id and secret

These should be treated as environment-specific integration values, not demo defaults for real deployments.

## `kurtuba.twilio.*`

These properties configure Twilio integration for SMS/verification features.

Examples include:
- live account credentials
- messaging service SID
- verify service SID

These values are highly sensitive and environment-specific.

Production requirements:
- use real credentials
- store secrets outside committed config
- separate test and live credentials correctly
- ensure region/service selection matches the actual deployment and messaging strategy

Demo defaults:
- example account SIDs
- example auth tokens
- non-functional placeholder values

## Other Configuration Areas to Document

Depending on the rest of the codebase and profiles, the following should also be documented where applicable:

- Redis configuration
- JDBC session configuration
- cache configuration
- OAuth provider-specific settings beyond Google
- rate-limit backend configuration
- environment variable overrides

## Required vs Optional Settings

For documentation purposes, every configuration key should be classified as one of:

- required for all environments
- required only in production
- optional with a safe default
- optional but feature-enabling
- demo-only or placeholder

This classification is more useful than simply listing properties.

## Recommended Production Review Checklist

Before deploying this service, review at least the following areas:

- issuer URL
- public server URL
- datasource settings
- migration strategy
- JWK/signing-key management
- client credentials
- cookie security behavior
- mail provider configuration
- Twilio/SMS configuration
- actuator exposure
- rate limiting
- job enablement
- secret injection strategy
- PostgreSQL role and ownership model if not using the simple shared-user setup

## Relationship to Other Documents

This document should be read together with:

- `docs/overview.md`
- `docs/capabilities.md`
- `docs/auth-model.md`
- `docs/postgresql.md`
- `docs/demo-vs-production.md`
- `docs/key-management.md`
