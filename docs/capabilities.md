# Service Capabilities

## Capability Overview

`kurtuba-auth` provides authentication, token management, account lifecycle management, and supporting operational features for Kurtuba applications.

The service is not limited to username/password login. It also covers account activation, client-aware token behavior, refresh-token rotation, password reset, token blocking, localization-aware messaging, external-provider-assisted account entry, and selected administrative operations.

## Registration

The service supports native user registration for Kurtuba-managed accounts.

Registration includes:

- creating the user record
- creating user settings
- assigning the default `USER` role
- validating uniqueness of email, mobile number, and username where provided
- associating the user with a supported locale
- initiating account activation through the preferred verification channel

A registration request may contain email, mobile number, or both, but at least one contact method must be present.

### Registration outputs

Successful native registration creates both:

- the user account
- a tracked activation operation stored as a user-meta-change entry

That activation operation is later used for code-based or link-based verification.

### External-provider registration and login

The service also includes a provider-assisted entry flow under `/auth/registration/other-provider`.

Current capabilities:

- supports `GOOGLE` and `FACEBOOK`
- accepts either a provider token or an authorization code
- can exchange authorization codes server-side when provider credentials are configured
- creates a new local user when no matching email exists
- reuses an existing local account when the provider email already exists
- issues normal local application tokens after provider identity is accepted

This means the external-provider path is not just “social signup.” It is a local-account entry path that still results in Kurtuba-issued tokens.

## Account Activation

Account activation is a core part of the user lifecycle.

The service supports:

- activation by verification code
- activation by verification link
- resending activation messages
- email-based activation
- mobile-based activation

When activation succeeds, the service:

- marks the account as activated
- marks the verified contact as verified
- marks the corresponding activation operation as executed
- optionally issues tokens if client credentials are supplied

## User Login

The service supports login for end users using `emailMobile` plus password.

Login behavior includes:

- authenticating against the stored BCrypt password hash
- validating user activation state
- checking account lock state
- issuing tokens through a registered client definition
- returning tokens either in JSON or in cookies depending on client configuration

### Default client fallback

If a login request does not specify a client, the service can fall back to the configured `DEFAULT` client.

### Cookie-based login responses

For clients configured with `sendTokenInCookie=true`, login returns the access token in the `jwt` HTTP-only cookie and responds with `204 No Content`.

### Service login

The service also supports login for service clients. In that path, a `SERVICE` registered client authenticates with client credentials and receives a short-lived access token intended for machine-to-machine use.

## Token Issuance

Token issuance is driven by registered-client configuration.

The service can issue:

- JWT access tokens
- refresh tokens for eligible clients

Client configuration influences:

- access-token TTL
- refresh-token TTL
- whether refresh is enabled
- whether scopes are included
- whether access tokens are returned in cookies
- which audiences are embedded in tokens

## Token Refresh

The service supports refresh-token-based session continuation.

Refresh support includes:

- validating the persisted access-token record
- validating the requesting client
- checking refresh-token expiration
- checking refresh-token-used state
- blocking refresh for blocked tokens
- enforcing client/token match
- issuing a fresh token set after consuming the old refresh token

### Refresh-token storage model

Refresh tokens are not stored raw. The raw token returned to the client is base64-decoded and stored as a BCrypt hash.

### Refresh rotation

Refresh tokens are single-use. The service marks the old refresh token as used through an atomic update before issuing a new token pair.

### Web refresh flow

The service also exposes a refresh flow for `WEB` clients that keep the access token in the `jwt` cookie. That path reads the cookie, validates the client and token state, and writes a new access token back into the cookie.

## Logout and Token Revocation

The service supports logout by marking the current token as blocked in persistent storage.

Related capabilities include:

- blocking a token during logout
- checking whether a token is blocked
- blocking all tokens for a user
- administrative token block/unblock operations by JTI

## Password Reset and Account Recovery

The service supports password recovery through multiple flows:

- requesting a password reset
- receiving a reset code or reset link
- resetting the password by code
- resetting the password through a hosted password-reset page

Successful password reset can optionally issue tokens when the request includes client credentials for that path.

## User Account Operations

Once authenticated, users can perform a range of account-related operations.

These capabilities include:

- fetching current user info
- fetching locale info
- changing password
- updating personal info
- updating language preference
- verifying email
- verifying mobile
- managing FCM tokens
- logout and FCM logout cleanup

The service-only endpoint `/auth/user/{id}` is also available for callers with `SCOPE_SERVICE`.

## Localization Support

The service includes localization-related capabilities used by activation and notification flows.

These include:

- selecting supported locales during registration
- storing localization messages
- exposing administrative localization operations

## Notification and Messaging Support

The auth service includes messaging support for account-related communication.

Examples include:

- account activation emails
- account activation SMS messages
- password reset messages
- user meta-change notifications

There is also support for queued or scheduled message delivery through message-job entities and scheduled processing.

## SMS Testing Surface

The repository includes SMS endpoints under `/auth/sms`, but they are only active in the `dev` profile.

These endpoints are convenience/testing hooks rather than a stable production API surface.

## JWKS Publishing

The service publishes a JWKS endpoint at `/auth/oauth2/jwks`.

This endpoint exposes the public portion of all loaded signing keys, which allows:

- downstream services to verify JWT signatures
- overlap windows during signing-key rotation
- distributed verification without exposing private key material

## Security Controls

The service includes several built-in security behaviors around authentication and token handling.

These include:

- failed login counting
- captcha signaling after repeated failed attempts
- account lockout after repeated failures
- exponential time-based backoff for locked accounts
- validation of registered-client credentials
- refresh-token reuse detection
- persistent blocked-token checks
- scope- and authority-based access restrictions

## Operational and Platform Features

In addition to end-user auth features, the service includes a number of operational capabilities:

- actuator endpoints
- JDBC-backed session support
- Redis-related configuration hooks
- rate-limit integration points
- scheduled job execution for message sending
- centralized exception handling
- PostgreSQL/Flyway-backed runtime and deployment path
