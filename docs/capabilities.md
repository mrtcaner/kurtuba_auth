# Service Capabilities

## Capability Overview

`kurtuba_auth` provides authentication, token management, account lifecycle management, and supporting operational features for Kurtuba applications.

The service is not limited to simple username/password login. It also manages account activation, client-aware token behavior, refresh-token rotation, password reset, token blocking, localization-aware messaging, and selected administrative operations.

This document describes the main capability areas exposed by the service and explains what each one is intended to do.

## Registration

The service supports user registration for accounts authenticated by the Kurtuba platform itself.

Registration includes:

- creating a user record
- creating user settings
- assigning the default user role
- validating uniqueness of email, mobile number, and username where provided
- associating the user with a supported locale
- initiating account activation through the preferred verification channel

A registration request may contain email, mobile number, or both, but at least one contact method must be present. The service also tracks the preferred verification contact so activation can proceed through email or mobile.

### Registration outputs

Successful registration does not simply create a user. It also creates an activation action, represented internally as a user-meta-change entry, which is then used for code-based or link-based verification.

### External-provider registration

The service also includes a path for registration through external identity providers. In that flow, provider-issued user information is decoded, an account is created or updated, and the user may be issued application tokens through the normal client-based token issuance path.

This allows the service to act as the local identity authority even when upstream identity data originates from another provider.

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

This means activation is both a verification step and, in some flows, an entry point into an authenticated session.

### Contact-aware behavior

The service distinguishes between email and mobile activation. The verified flag that is updated depends on which contact type was used in the activation process.

### Resend behavior

If a user has not yet activated the account, the service can generate and resend the relevant activation message. This capability is important for real-world signup flows where messages may be delayed, lost, or expired.

## User Login

The service supports login for end users using credentials such as email or mobile plus password.

Login behavior includes several important capabilities:

- authenticating against persisted user credentials
- validating user activation state
- checking account lock state
- issuing tokens through a registered client definition
- returning tokens either in JSON or in cookies depending on client configuration

### Default client fallback

If a login request does not specify a client, the service can fall back to the configured default client. This is convenient for development and simplified integrations, but it is also an important behavior to document because it changes how tokens are issued.

### Cookie-based login responses

For web-oriented clients, the service can return the access token in an HTTP-only cookie instead of returning token data in the response body. This supports browser-based integrations that rely on cookie transport rather than manual bearer-token handling in frontend code.

### Service login

The service also supports login for service clients. In that path, a registered client authenticates with client credentials and receives a short-lived access token intended for machine-to-machine use rather than end-user interaction.

## Token Issuance

Token issuance is a central capability of the service.

The service can issue:

- JWT access tokens
- refresh tokens for eligible client types

Token generation is driven by registered-client configuration. That means token behavior is not global; it depends on the client definition used during authentication.

Client configuration influences:

- token TTL values
- whether refresh tokens are enabled
- whether scopes are included
- whether tokens are returned in cookies
- which audiences are embedded in tokens

This design allows the same auth server to support different types of applications with different token-handling requirements.

## Token Refresh

The service supports refresh-token-based session continuation.

Refresh support includes:

- validating the access token against persisted token state
- validating the requesting client
- checking that the refresh token has not expired
- checking that the refresh token has not already been used
- consuming the refresh token so it cannot be reused
- issuing a fresh access token and refresh token set

### Refresh rotation

Refresh tokens are rotated. A refresh token is intended for one-time use. Once consumed successfully, it cannot be reused. This is one of the most important security capabilities in the service and should be treated as a core property of the system.

### Web refresh flow

The service also exposes a refresh flow for web clients that keep the access token in a cookie. In that mode, the controller reads the JWT cookie, validates the client, refreshes tokens, and writes the new access token back into a cookie.

## Logout and Token Revocation

The service supports logout by marking the current token as blocked in persistent storage.

This gives the system a server-side revocation mechanism even though access tokens themselves are JWTs.

Related capabilities include:

- blocking a token during logout
- checking whether a token is blocked
- blocking all tokens for a user
- administrative block and unblock operations for token JTIs

This makes token revocation an explicit server-side concern rather than relying only on token expiry.

### FCM logout support

The service also includes a logout-related capability for deleting FCM token registrations associated with a user installation. This is relevant for mobile notification cleanup when a session ends or a device is disassociated.

## Password Reset and Account Recovery

The service supports password recovery through multiple flows:

- requesting a password reset
- receiving a reset code or reset link
- resetting the password by code
- resetting the password through a hosted password-reset page

This dual-mode approach allows both API-driven clients and browser-driven users to complete password recovery.

### Recovery workflow behavior

The password reset flow creates a tracked reset operation, validates the operation at completion time, and either changes the password directly or renders the appropriate hosted UI flow for link-based resets.

In some cases, successful password reset may also be able to return tokens depending on the specific flow and request content.

## User Account Operations

Once authenticated, users can perform a range of account-related operations through the user APIs.

These capabilities include:

- fetching user profile information
- fetching locale information
- changing password
- updating personal info
- updating language preference
- verifying email
- verifying mobile
- managing FCM tokens

These capabilities make the auth service responsible not only for sign-in and token issuance, but also for a meaningful part of the user security and identity profile lifecycle.

## Localization Support

The service includes localization-related capabilities used by activation and notification flows.

These include:

- selecting supported locales during registration
- storing localization messages
- exposing administrative localization operations

This matters because user onboarding and recovery flows often depend on sending region- and language-aware messages.

## Notification and Messaging Support

The auth service includes messaging support for account-related communication.

Examples include:

- account activation emails
- account activation SMS messages
- password reset messages
- user meta-change notifications

There is also support for queued or scheduled message delivery through message-job entities and scheduled processing.

This means some user lifecycle actions do not merely update the database; they also create outbound communication work that must be handled reliably by the configured messaging implementation.

## JWKS Publishing

The service publishes a JWKS endpoint that exposes the public signing keys needed for JWT verification.

This capability is essential for:

- downstream services validating tokens
- external systems trusting issued JWTs
- supporting signed-token verification without exposing private key material

JWKS publishing is one of the foundational capabilities that makes the service usable as a real token issuer in a distributed system.

## Administrative Capabilities

The service also exposes administrative features beyond end-user authentication.

These include capabilities related to:

- user administration
- localization message administration
- token management

Administrative token management is particularly important because it enables security operations such as checking whether tokens are blocked or forcing block/unblock behavior.

These features should be documented with extra care because they have stronger security implications than normal user-facing endpoints.

## Security Controls

The service includes several built-in security behaviors around authentication and token handling.

These include:

- failed login counting
- captcha signaling after repeated failed attempts
- account lockout after repeated failures
- time-based backoff for locked accounts
- validation of registered-client credentials
- refresh-token reuse detection
- persistent blocked-token checks
- scope- and authority-based access restrictions

These are not optional side features. They are part of the service’s actual security model and should be treated as first-class capabilities in the documentation.

## Operational and Platform Features

In addition to end-user auth features, the service includes a number of operational capabilities:

- actuator endpoints
- JDBC-backed session support
- Redis-related configuration hooks
- rate-limit integration points
- scheduled job execution for message sending
- centralized exception handling

These are important because they affect how the service behaves in production and how operators observe and manage it.

## Current Caveats

Although the service exposes a broad capability set, not every capability is necessarily production-hardened in its current default form.

Several flows depend on configuration that is demo-friendly by default, and some integrations use example or placeholder settings. For that reason, capability documentation should always be paired with configuration and deployment documentation.

In practice, each capability should be understood in two dimensions:

- what the service is designed to support
- what additional production configuration is required for that capability to be safely and fully usable
