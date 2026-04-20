# Overview

## What This Service Is

`kurtuba-auth` is the authentication and identity service for Kurtuba applications. It is responsible for user registration, account activation, credential-based login, token issuance, token refresh, logout, password reset, and related account-security operations.

The service acts as the central authority for both user authentication and client authentication. It supports direct end-user flows as well as service-to-service authentication flows.

From a technical perspective, this repository is a Java 21 Spring Boot authentication server built with Spring Security, Spring Data JPA, JWT-based access tokens, persisted refresh-token state, JDBC-backed session support, and optional Redis-backed infrastructure hooks.

It is relevant to readers searching for terms such as:

- Spring Boot authentication server
- Java JWT auth server
- OAuth2 resource server with JWKS
- refresh token rotation in Spring Boot
- cookie-based JWT authentication
- user registration and account activation service
- password reset and token revocation service
- Spring Boot PostgreSQL auth server with Flyway

## Who It Serves

This service is designed for several kinds of consumers:

- End users signing up, activating accounts, logging in, refreshing sessions, and managing account security
- Web clients that may receive tokens through cookies
- Mobile clients that typically consume JSON token responses
- Internal or backend services that need short-lived service tokens
- Administrative clients managing localization, users, and token state

## High-Level Responsibilities

At a high level, the service provides the following responsibilities:

- Create and validate user accounts
- Manage account activation through email or mobile verification
- Authenticate users with credentials
- Issue JWT access tokens and refresh tokens
- Rotate refresh tokens and detect reuse
- Revoke or block tokens
- Support password reset flows by code or hosted link
- Expose a JWKS endpoint for token verification
- Maintain client definitions and client-specific token behavior
- Support localization and notification-related workflows

## Supported Client Modes

The service supports multiple client interaction models.

### Bearer-token clients

Clients can authenticate users and receive access tokens and refresh tokens in JSON responses. This is the standard mode for mobile apps and many API consumers.

### Cookie-based web clients

Web clients can be configured so the access token is returned in an HTTP-only cookie instead of in the response body. This allows browser-based applications to use cookie-based session-like behavior while still relying on JWT issuance internally.

### Service clients

Service clients can authenticate using client credentials and receive short-lived access tokens intended for machine-to-machine use.

### Default-client fallback

If a login request does not specify a client, the service can fall back to a configured default registered client. This is convenient for local/demo usage but should be documented carefully for production deployments.

## Main Functional Areas

The service’s capabilities can be grouped into the following areas:

- Registration and account activation
- User login and token issuance
- Token refresh and token revocation
- Password reset and account recovery
- User account maintenance
- Administrative token and localization operations
- Public key discovery through JWKS

## Core Capabilities

The service currently includes support for:

- User registration with email and/or mobile details
- Account activation by verification code or verification link
- Registration via external identity providers
- User login with client-aware token issuance
- Service login for backend systems
- JWT access token generation
- Refresh token generation and rotation
- Web token refresh using cookie-based access tokens
- Logout with token blocking
- Password reset by code and by link
- Email and mobile verification flows
- User locale and profile-related updates
- FCM token registration and removal
- Admin token block management
- Localization-related administration
- Rate-limit hooks and scheduled messaging support

## Technical Profile

The repository currently centers on the following technologies and architectural choices:

- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Spring Session JDBC
- PostgreSQL for local, test, and production-oriented relational persistence
- JWT access tokens with JWKS publishing
- JWK/JWE-based signing-key packaging and key rollover support
- BCrypt password hashing
- Flyway support for schema migration
- Redis-related integration points
- Twilio integration hooks for SMS flows
- SMTP mail integration for activation and recovery flows
- Thymeleaf-backed hosted pages for selected browser-based flows

## Repository Keywords

People evaluating the repository should expect to find code and documentation related to:

- authentication
- authorization
- identity
- JWT
- JWKS
- refresh tokens
- token rotation
- token revocation
- Spring Security
- account activation
- password reset
- service-to-service auth
- cookie auth
- role and scope based access control
- PostgreSQL migration
- Flyway

## Architecture in Brief

The application is implemented as a Spring Boot service organized around standard layers:

- Controllers expose HTTP endpoints
- Services implement authentication and account logic
- Repositories persist users, tokens, clients, and supporting entities
- Security configuration defines public and protected routes, JWT validation, and authentication behavior
- Scheduled jobs and messaging services support email/SMS delivery flows

Persistence is centered around relational storage for users, clients, tokens, and metadata. The service also includes configuration hooks for Redis and session handling.

## Major Integrations

The service is designed to sit in the middle of several surrounding systems:

- relational database for users, clients, tokens, and workflow metadata
- SMTP provider for account activation and password-reset mail
- SMS provider integration for verification and activation flows
- downstream services that verify JWTs via JWKS
- web and mobile clients consuming token-based authentication
- internal services using service-client login

This makes the repository useful not only as an app-specific auth service, but also as a reference point for teams looking for a Spring Boot implementation of account lifecycle management with JWT issuance and persisted token controls.

## Signing Keys at a Glance

JWT signing is backed by JWK key material loaded at startup. Public verification keys are exposed through `/auth/oauth2/jwks`, while private signing keys are stored indirectly: the repository contains encrypted JWK payloads in `jwk.json`, and the decryption secrets are supplied separately through `kurtuba.jwk.keys`.

The current codebase is prepared to verify tokens signed with either `RS256` or `ES256`, but the bundled helper used for generating encrypted signing material currently defaults to RSA key generation. In practice, the current repository layout is therefore oriented around RSA signing with JWKS publication and staged key rollover.

## Security Model Summary

The service uses JWT-based access tokens and persisted refresh-token state.

Important properties of the model include:

- Access tokens are signed and can be verified using the exposed JWKS endpoint
- Refresh tokens are stored in hashed form
- Refresh tokens are intended for one-time use
- Token refresh validates both token state and registered client identity
- Tokens can be blocked in persistent storage
- User login behavior includes failed-attempt counting, captcha signaling, and account lockout logic

## Database Posture

The repository is now PostgreSQL-first.

The service runs against PostgreSQL with Flyway enabled in local, test, and deployment-oriented usage. This path has been validated in the public repository against a real PostgreSQL instance using a throwaway database, the checked-in migrations, and Hibernate schema validation.

For PostgreSQL setup guidance, see `docs/postgresql.md`.

## Environment Model

The current configuration is suitable for local and demo runs, but not all defaults are appropriate for production.

Examples of local-development-oriented behavior include:

- Example mail and provider credentials
- Localhost-oriented URLs
- Disabled jobs and optional integrations
- Non-secure cookie settings in code paths intended for development convenience

Because of that, documentation should clearly separate:

- what the service is capable of
- what is enabled by default for demo runs
- what must be changed before a real deployment

## Production Readiness

The service contains substantial authentication functionality, but some behavior and configuration values are clearly designed for development or demonstration environments.

Production documentation should explicitly identify:

- required external dependencies
- required secret management
- required client configuration
- secure cookie and transport settings
- real mail/SMS provider setup
- key management and signing-key rotation expectations
- real relational-database setup and migration ownership expectations
- any flows that are present but still rely on placeholder assumptions or partial implementations

## Recommended Reading

This overview should be read together with the following documents:

- `docs/capabilities.md` for a feature-by-feature breakdown
- `docs/auth-model.md` for token, client, and security behavior
- `docs/key-management.md` for signing algorithms, JWK storage, generation, and rotation
- `docs/configuration.md` for property reference and runtime expectations
- `docs/postgresql.md` for PostgreSQL setup and role-split database options
- `docs/demo-vs-production.md` for deployment caveats and hardening notes
