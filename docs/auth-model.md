# Authentication Model

## Overview

`kurtuba-auth` uses a client-aware authentication model built around JWT access tokens, persisted token records, refresh-token rotation, and server-side token blocking.

The service does not treat authentication as a single generic login operation. Instead, authentication behavior depends on both:

- the authenticated principal
- the registered client through which authentication is performed

This means token format, refresh behavior, cookie behavior, scopes, and TTL values are controlled by client configuration rather than by a single global policy.

## Principal Types

The service works with two main principal categories:

- end users
- service clients

These two categories are authenticated differently and receive tokens for different purposes.

### End users

End users authenticate with credentials such as email or mobile plus password. Their tokens represent a user identity and may include user-role-derived scopes depending on client configuration.

### Service clients

Service clients authenticate with client credentials. Their tokens represent the client itself rather than a human user. These tokens are intended for machine-to-machine communication and are issued through a separate service login path.

## Registered Clients

A registered client defines how tokens should be issued and handled for a consuming application or system.

Client definitions influence:

- whether refresh tokens are enabled
- access token TTL
- refresh token TTL
- whether scopes are included
- whether tokens are returned in cookies
- cookie max age
- intended audiences
- client type

In practice, the registered client is a central part of the authentication model. The same user may receive different token behavior depending on the client used during login.

## Client Types

The service recognizes multiple client types through `RegisteredClientType`.

### `DEFAULT`

A fallback client used when login requests do not specify a client explicitly.

This is useful for local development and simplified integrations, but in production it should be documented carefully because it creates implicit behavior.

### `WEB`

A web-oriented client type. These clients may use cookie-based access-token handling and a dedicated web refresh flow.

### `MOBILE`

A client type intended for mobile applications, typically using JSON token responses and refresh-token-based session continuation.

### `SERVICE`

A machine-to-machine client type. These clients authenticate with client credentials and receive short-lived access tokens without going through normal user login.

### `GENERIC`

A general-purpose client type for integrations that do not fit one of the more specific categories.

## User Authentication Flow

For end users, authentication starts with credential validation.

The authentication process includes:

- finding the user by email or mobile
- checking whether the account is currently locked
- validating the password
- updating failed-attempt state on failure
- validating activation state
- resetting lock/captcha counters on success
- passing the authenticated user into client-aware token issuance

This means successful password verification alone is not enough. The user must also be in a valid account state.

## Failed Login and Lockout Behavior

The service tracks authentication failures per user.

Current behavior includes:

- incrementing `failedLoginCount` on incorrect password
- enabling `showCaptcha` after repeated failures
- locking the account after a higher threshold of failures
- applying a time-based backoff window for locked accounts

This is part of the authentication model because login is stateful at the user level, not just a stateless credential check.

A user who is locked cannot authenticate again until the lock window has expired.

## Activation Requirement

A correct password is not sufficient if the account has not been activated.

For end-user logins, authentication succeeds only if the account is already activated. This ties the login model directly to the registration and activation lifecycle.

## Access Tokens

The service issues JWT access tokens.

These tokens are signed using configured signing keys, and the corresponding public keys are exposed through the JWKS endpoint.

Access tokens are used to authorize protected API requests and may include:

- subject identity
- token identifier (`jti`)
- audience information
- client identifier
- scope-related claims
- expiry information

The exact contents and meaning depend on the client and the token-generation logic.

## JWT Verification Model

Access tokens are signed by the auth service and can be validated by consumers using the public keys published through JWKS.

Within the service itself, access tokens are also checked against persisted token state during refresh and revocation-sensitive operations.

This means the model is not purely stateless JWT validation. A token may be cryptographically valid but still rejected because of server-side token state.

## Persisted Token State

When the service issues tokens for a user, it also saves a `UserToken` record.

This persisted state includes fields such as:

- token identifier (`jti`)
- user id
- client id
- audiences
- scopes
- access-token expiration
- hashed refresh token
- refresh-token expiration
- blocked state
- refresh-token-used state

This persistent token record is critical to the security model because it enables:

- revocation
- refresh-token rotation
- refresh-token reuse detection
- client matching during refresh

## Refresh Tokens

Refresh tokens are supported only for clients that have refresh enabled.

Refresh tokens are generated at token-issuance time and stored in hashed form in persistent storage. The raw refresh token is returned to the client, but the database stores only a hash for later verification.

This means the service treats refresh tokens more like secrets than simple opaque identifiers.

## Refresh Token Rotation

Refresh tokens are single-use.

When a refresh request succeeds, the service:

- validates the access token and persisted token record
- verifies the submitted refresh token against the stored hash
- marks the refresh token as used
- issues a new token pair

If the same refresh token is used again, the request is rejected.

This is one of the most important parts of the model because it reduces replay risk and allows refresh-token theft to be detected more reliably.

## Refresh Validation Rules

Refresh is not based on the refresh token alone.

The service validates:

- the access token
- the persisted token record
- the refresh-token expiration
- whether the refresh token was already used
- whether the token is blocked
- whether the requesting client matches the token’s original client
- whether the client credentials are valid when required

This makes refresh a tightly controlled, stateful operation.

## Web Refresh Model

For `WEB` clients, refresh follows a cookie-oriented pattern.

In that model:

- the access token is read from the `jwt` cookie
- the client is validated
- token state is checked
- a new access token is issued
- the cookie is rewritten with the new token

This provides a browser-oriented authentication experience while still relying on the same core token-state model.

## Cookie-Based Authentication

Some clients can be configured to receive the access token in an HTTP-only cookie rather than in the JSON response body.

This is controlled by registered-client configuration.

The cookie-based model is intended for web applications, but its security properties depend heavily on deployment settings such as:

- secure cookie usage
- HTTPS enforcement
- domain and path policy
- CSRF strategy
- frontend deployment model

Because of that, cookie behavior should be documented together with production deployment guidance.

## Scope and Authority Model

The service supports authority/scoping behavior through token claims and Spring Security authorization.

For user tokens, scopes may be derived from user roles when the registered client has scope support enabled.

For service tokens, scopes represent client-level permissions and are used to restrict internal or backend access.

This allows authorization decisions to be driven by token claims while still keeping issuance rules tied to client configuration.

## Client Credential Validation

The registered client is not just metadata. It is part of the security boundary.

When tokens are issued or refreshed, the service validates the client and, when configured, checks the submitted client secret against the stored client secret.

This ensures that:

- tokens are issued only for valid clients
- refresh operations cannot be replayed under a different client
- service login is restricted to `SERVICE` clients with valid credentials

## Token Blocking and Revocation

The service supports server-side token revocation by marking tokens as blocked in persistent storage.

A blocked token is treated as invalid for operations that consult server-side token state.

This is especially important because JWTs are otherwise self-contained and valid until expiry unless the server adds a revocation mechanism.

The service therefore combines:

- signed JWTs for portable authorization
- persistent token state for revocation and refresh safety

## Logout Model

Logout is implemented by blocking the current token rather than simply forgetting client-side state.

This means logout has server-side effect. After logout, the token’s persisted state reflects that it should no longer be accepted for protected token-state-aware operations.

This is a stronger model than client-side token deletion alone.

## Account State and Token Issuance

Token issuance depends not only on successful authentication but also on valid account state.

Examples of invalid user state include:

- account not activated
- account locked
- account flagged in a way that prevents token issuance

This ensures that the service does not issue fresh tokens for users whose state should prevent access.

## Service Token Model

Service tokens differ from user tokens in several ways:

- they are issued to a registered client identity
- they do not represent a human user
- they are obtained through service login
- they are intended to be short-lived
- they are used for machine-to-machine authorization

This separation is important because internal-service authentication should not be treated the same way as end-user session management.

## Public Key Model

The service exposes public signing keys through JWKS.

This allows downstream consumers to validate JWT signatures without direct access to private key material.

The authentication model therefore depends on:

- private signing keys held by the auth service
- public verification keys published to consumers
- key identifiers that allow correct key selection during verification

Key handling is a critical operational part of the model and should be documented further in configuration and deployment docs.

## Signing Algorithms

The resource-server JWT decoder is configured to accept public keys for:

- `RS256`
- `ES256`

However, the current signing-key helper flow in the repository generates RSA JWKs by default. The practical result is:

- the service can verify RSA- and EC-signed tokens when matching public keys are available
- the built-in key-generation path is currently oriented toward RSA signing material
- current deployments based on the provided helper classes are expected to use RSA-backed JWT signing

Within token generation, the application signs JWTs using the private key loaded from the active JWK. The exact JWS algorithm is inferred from the key type by the signing library rather than hard-coded in the token builder.

## How Signing Keys Are Loaded

Signing keys are loaded by `TokenUtils` at startup.

The loading model is:

- encrypted signing-key entries are stored in `classpath:jwk.json`
- decryption secrets are supplied through the `kurtuba.jwk.keys` property
- each encrypted entry has an `id` and an `order`
- all encrypted entries are decrypted into JWKs at startup
- keys are sorted by `order` in descending order
- the first key in that sorted list becomes the active signing key
- all loaded keys remain available for verification and JWKS publication

This means the service supports a simple key-rollover model where one key is active for signing while older keys can remain available for verification.

## Active Signing Key Selection

The service does not choose the active key by creation date or by a separate “current” flag.

Instead, the active signing key is selected by ordering:

- the highest `order` value wins
- the first key after descending sort is used for signing new JWTs
- lower-ordered keys remain valid for verifying previously issued tokens

This is a simple and effective rollout mechanism as long as operators manage `order` values carefully.

## JWKS Publication Behavior

The JWKS endpoint publishes the public portion of all loaded signing keys, not only the active one.

This is important for rotation because downstream verifiers need access to:

- the current key for newly issued tokens
- older public keys for tokens that are still valid but were signed before rotation

In other words, rotation safety depends on keeping previous public keys available long enough for all previously issued JWTs to expire.

## Encrypted JWK Storage Model

Private signing keys are not stored in plain JSON in the main runtime path.

Instead, the service uses a two-part model:

- `jwk.json` stores encrypted JWK payloads
- `kurtuba.jwk.keys` stores the corresponding decryption secrets

At startup, the application:

- reads the encrypted JWK entries
- looks up the decryption secret by entry `id`
- decrypts the stored JWE payload
- reconstructs the full JWK, including private key material

This is better than storing private JWKs in plaintext, but it is still not equivalent to using an HSM, KMS, or dedicated secret-management platform. Production use should therefore treat this as an application-managed key-protection scheme, not as hardened enterprise key custody.

## Encryption Used for Stored JWKs

The helper flow in `JwkGenerator` encrypts signing JWK payloads as JWE before they are stored.

The current helper uses:

- key management algorithm: `A256GCMKW`
- content encryption method: `AES_256_GCM`

This encryption step protects the serialized private JWK payload at rest in the repository-managed file format, provided the decryption secret is stored separately and securely.

## Key Generation Helpers

The codebase includes helper methods for generating signing material:

- RSA JWK generation
- EC JWK generation for `ES256`
- JWE wrapping of generated JWK payloads

The currently wired helper path uses RSA JWK generation when producing encrypted signing entries. There is also helper support for generating EC keys, but that is not the default path used by the bundled JWE generator.

## Recommended Rotation Model

The current implementation supports staged signing-key rotation.

A safe rotation sequence is:

1. Generate a new signing key pair and encrypt it into the same storage format used by `jwk.json`.
2. Add the new encrypted entry to `jwk.json` with a higher `order` than the current active key.
3. Add the matching decryption secret under the same `id` in `kurtuba.jwk.keys`.
4. Deploy the service with both old and new keys present.
5. Allow new tokens to be signed by the new highest-ordered key while old tokens continue to verify against older published public keys.
6. Wait until all tokens signed by the old key are expired, including any operational grace window you require.
7. Remove the old encrypted key entry and its decryption secret in a later deployment.

This preserves verification continuity during rollout and avoids invalidating still-active tokens prematurely.

## Operational Risks Around Rotation

Key rotation will fail or cause outages if any of the following happen:

- the new encrypted key is added without its matching decryption secret
- the `order` values are incorrect and the wrong key becomes active
- old public keys are removed before previously issued tokens expire
- `auth.server.issuer-url` or JWKS publication changes unexpectedly during rotation
- downstream services cache JWKS too aggressively without respecting rollover

Because of that, key rotation should be treated as a planned operational change, not just a config edit.

## Security Characteristics

The authentication model is designed around the following properties:

- signed access tokens
- persisted token-state tracking
- hashed refresh tokens
- one-time-use refresh semantics
- client-aware issuance rules
- account-state-aware login
- server-side token blocking
- role/scope-based authorization

These properties together make the service more than a simple stateless JWT issuer.

## Important Documentation Follow-Ups

This document should be read together with:

- `docs/capabilities.md` for user-facing and system-facing features
- `docs/key-management.md` for signing algorithms, active-key selection, storage, and rotation procedures
- `docs/configuration.md` for client, token, JWK, mail, SMS, and database settings
- `docs/demo-vs-production.md` for security-sensitive defaults and required hardening
- `docs/api.md` for endpoint-level request and response behavior
