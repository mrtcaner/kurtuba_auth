# Demo Defaults vs Production Requirements

## Overview

`kurtuba-auth` includes a meaningful set of authentication and account-management capabilities, but its current default configuration is oriented toward local development and demo usage rather than hardened production deployment.

This document explains the difference between:

- capabilities that exist in the service
- defaults that make local or demo runs easier
- changes required before the service should be considered production-ready

The goal is to prevent a common failure mode: assuming that because the service runs successfully, it is also configured safely and completely for real-world deployment.

## Why This Distinction Matters

Authentication systems are unusually sensitive to configuration quality. Demo-friendly defaults can be acceptable for a local developer machine, but the same defaults can become dangerous in production.

In this service, that distinction matters in areas such as:

- database provisioning and role setup
- signing-key management
- cookie security
- provider credentials
- message delivery
- actuator exposure
- feature enablement
- external URL correctness

A production deployment should therefore be treated as a configuration-hardening exercise, not just a packaging exercise.

## Demo-Oriented Defaults in the Current Setup

The current application configuration includes several values and behaviors that are appropriate for local testing or demonstration but should not be used unchanged in production.

### PostgreSQL still needs local-vs-production treatment

The repository is PostgreSQL-first, but local PostgreSQL convenience and production PostgreSQL hardening are still different things.

Why local PostgreSQL defaults are fine for development:
- easier onboarding
- realistic SQL behavior
- compatibility with Flyway and Testcontainers

Why production still needs more:
- database credentials and privileges must be reviewed
- backup and recovery expectations must exist
- ownership, grants, and migration discipline still matter

### Flyway disabled

When Flyway is disabled and schema creation is handled through development-oriented JPA settings, startup is easier but schema management is weaker.

Why this is fine for demos:
- no migration discipline required
- quick startup for a disposable environment

Why this is risky in production:
- schema history is not controlled
- upgrades become error-prone
- destructive or inconsistent schema behavior may occur

Production expectation:
- enable Flyway
- manage schema evolution through reviewed migrations

### `ddl-auto=create-drop` or similar development schema behavior

Development schema automation is useful for local runs but not for real environments.

Production expectation:
- use managed migrations
- avoid destructive schema recreation
- prefer `ddl-auto=validate` when Flyway is enabled

### PostgreSQL setup posture

The repository now documents two PostgreSQL postures:

- a simple developer path using one database account for both Flyway and the app
- a more production-oriented path using the checked-in `init_db.sql` role split

This distinction matters because teams often need a middle ground between “local PostgreSQL” and “full production hardening.”

Practical guidance:
- use the shared-user approach for local integration and CI if you want simplicity
- use the split-role approach when you want clearer privilege boundaries

### Localhost-based URLs

Defaults such as `http://localhost:8080` are convenient in development, but they break production flows that generate URLs or identify the issuer.

Affected areas include:
- issuer URL
- activation links
- password reset links
- JWKS location expectations

Production expectation:
- set the real externally reachable HTTPS base URL
- ensure generated links are valid for end users
- ensure issuer and JWKS URLs are stable and correct

### Example OAuth provider credentials

Placeholder Google client credentials may exist for demo readability.

Why this is fine for demos:
- shows intended configuration shape
- avoids blocking local startup

Why this is not acceptable for production:
- the values are not real
- redirect flows will not work correctly
- secrets must be environment-specific and protected

Production expectation:
- use real provider-issued credentials
- align redirect URIs with deployed routes
- inject secrets through secure mechanisms

### Example SMTP credentials

Mail settings may be configured with sample or local values.

Why this is acceptable for demos:
- avoids hard dependency on real mail infrastructure
- allows local integration with mail-catching tools

Why this is not sufficient for production:
- activation and password-reset mail are core auth features
- fake mail settings make important flows nonfunctional
- poor mail configuration can result in broken onboarding and recovery

Production expectation:
- configure a real SMTP or transactional mail provider
- secure credentials properly
- verify sender identity and deliverability

### Example Twilio credentials

Twilio-related configuration may contain example SIDs and auth tokens.

Why this is fine for demos:
- demonstrates intended integration points
- allows the app to boot with config placeholders

Why this is not acceptable for production:
- SMS activation and verification flows require live provider configuration
- wrong environment pairing between test and live credentials can break messaging
- secrets must not remain in committed configuration

Production expectation:
- configure live Twilio credentials where SMS flows are required
- keep secrets out of source control
- validate messaging and verification service configuration end-to-end

### Jobs disabled

Background jobs may be disabled by default.

Why this is fine for demos:
- simpler startup
- avoids needing external messaging or scheduling dependencies
- reduces noise during development

Why this may break production expectations:
- queued mail/SMS behavior may never run
- notifications may remain pending indefinitely
- capabilities may appear present but remain operationally inactive

Production expectation:
- enable jobs if your deployment relies on scheduled message processing
- verify retry and failure handling

### Rate limiting disabled

Rate limiting may be disabled in demo mode.

Why this is fine for local development:
- fewer moving parts
- avoids configuration burden
- easier manual testing

Why this is risky in production:
- auth endpoints are high-value abuse targets
- login, activation, reset, and registration flows can be brute-forced or spammed
- rate limiting is part of practical auth hardening

Production expectation:
- enable rate limiting
- verify the backing implementation and thresholds
- tune for both abuse resistance and legitimate traffic

### Redis health checks disabled

Redis-related health checks may be disabled in demo runs.

Why this is fine for demos:
- avoids surfacing failures for optional integrations
- simplifies local bootstrapping

Why this matters in production:
- optional systems often become operational dependencies in real deployments
- missing health visibility can hide real integration problems

Production expectation:
- enable and validate health checks for required dependencies

## Code-Level Behaviors That Are Demo-Friendly But Need Review

Some concerns are not just in configuration files. They are reflected in application behavior and should be reviewed explicitly for production readiness.

### Non-secure cookie configuration

The login and refresh flows create cookies with `secure(false)` in the current code path.

Why this is acceptable for local HTTP testing:
- works without HTTPS on localhost

Why this is unsafe in production:
- cookies can be transmitted over plain HTTP if infrastructure permits
- weakens browser-side token transport guarantees

Production expectation:
- use secure cookies in HTTPS environments
- review SameSite, domain, path, and CSRF strategy as part of the deployment model

### Default-client fallback behavior

The login flow can fall back to a default registered client when no client is explicitly supplied.

Why this is convenient in demos:
- simpler requests
- easier manual testing
- fewer moving pieces when bootstrapping a client

Why this needs care in production:
- hides client-selection behavior
- can produce unexpected token semantics
- can blur responsibility between client integrations

Production expectation:
- document whether default fallback is allowed
- consider requiring explicit client identity for production consumers

### Local or stub-like service implementations

The codebase includes local implementations for some integrations such as email or SMS behavior.

Why this is useful in development:
- allows flows to be exercised without paid providers or external services

Why this needs production review:
- local implementations may not provide reliable delivery
- operational semantics may differ from provider-backed implementations
- logging-only or no-op implementations can create false confidence

Production expectation:
- confirm which implementation beans are active in production
- ensure real providers are wired and tested

## Capabilities Present But Operationally Dependent

A number of capabilities exist in the codebase but depend on proper environment setup before they are truly usable.

These include:

- account activation mail delivery
- SMS-based activation
- password reset messaging
- service login
- cookie-based web auth
- JWKS-backed token verification by downstream systems
- scheduled message processing
- localization-aware messaging
- admin token operations
- rate limiting
- PostgreSQL-backed migration and persistence

In documentation, these should never be described only as “supported.” They should be described as:

- implemented in code
- dependent on correct production configuration and operational wiring

## Production Requirements by Category

### Database

Production requires:
- persistent relational database
- controlled schema migrations
- durable storage for users, clients, token state, and recovery metadata
- a conscious choice between shared-user simplicity and split-role privilege separation

### Key management

Production requires:
- real signing keys
- controlled access to key material
- documented rotation approach
- separation of demo/sample key material from real deployment secrets

### URLs and issuer metadata

Production requires:
- correct public issuer URL
- correct public server URL
- valid activation and reset links
- correct JWKS discoverability

### Client management

Production requires:
- reviewed registered-client entries
- strong client secrets where applicable
- explicit TTL and refresh behavior decisions
- clear separation between web, mobile, service, and default clients

### Cookie security

Production requires:
- HTTPS
- secure cookies
- reviewed browser security behavior
- CSRF-aware deployment design if cookies are used for auth transport

### Messaging

Production requires:
- real SMTP setup
- real SMS provider setup where SMS flows are used
- tested activation and recovery delivery
- retry and failure monitoring

### Feature enablement

Production requires deliberate decisions about:
- jobs
- rate limiting
- health checks
- actuator exposure
- Redis usage
- session behavior

### Secret management

Production requires:
- no real secrets committed in source control
- use of environment variables, secret stores, or deployment-managed injection
- rotation procedures for provider credentials and signing keys

## Recommended Production Hardening Checklist

Before calling a deployment production-ready, verify at least the following:

- replace demo datasource with persistent production database
- enable controlled schema migration
- set correct public issuer URL
- set correct public server URL
- replace sample OAuth credentials
- replace sample SMTP credentials
- replace sample Twilio credentials
- review and protect actuator exposure
- avoid exposing database-specific admin tooling over HTTP
- enable and test rate limiting
- enable required background jobs
- review cookie security settings
- verify signing keys and JWKS behavior
- confirm real provider-backed email/SMS implementations are active
- validate activation, login, refresh, logout, and reset flows end-to-end
- validate your PostgreSQL privilege model and migration strategy

## Recommended Documentation Language

When describing features publicly or internally, use language like:

- “Implemented in code”
- “Available when configured”
- “Enabled in production only when…”
- “Demo default is not production-safe”

Avoid language that implies production readiness unless the full dependency chain has been verified.

## Relationship to Other Documents

This document should be read with:

- `docs/overview.md`
- `docs/capabilities.md`
- `docs/auth-model.md`
- `docs/configuration.md`
- `docs/postgresql.md`

It is especially important as a companion to `docs/configuration.md`, because configuration lists alone do not clearly communicate operational risk.
