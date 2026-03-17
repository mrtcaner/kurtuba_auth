# API Reference

## Overview

`kurtuba-auth` exposes endpoints for user registration, activation, authentication, token refresh, account recovery, user account operations, public key discovery, SMS-related flows, and administrative operations.

This document is organized by endpoint group rather than by controller class so it is easier for API consumers to follow.

## Conventions

Unless otherwise noted:

- request bodies are JSON
- successful responses use standard HTTP status codes
- validation and business-rule failures are typically returned as `400 Bad Request`
- protected endpoints require a valid access token
- some responses differ depending on registered-client behavior

Error responses are generally returned in a structured error DTO.

## Authentication Requirements

The API surface falls into three categories:

- public endpoints, such as registration, login, activation, reset initiation, and JWKS
- authenticated user endpoints, which require a valid user token
- privileged/admin/service endpoints, which require appropriate scopes or authorities

## 1. Registration Endpoints

Base path: `/registration`

### `POST /registration`

Creates a new user and starts the activation process.

Purpose:
- register a Kurtuba-native user account
- assign default user role
- create an activation operation
- trigger activation through the preferred contact method

Typical request fields:
- name
- surname
- username
- email
- mobile
- password
- auth provider
- preferred verification contact
- verification-by-code flag
- language code
- country code

Success response:
- `201 Created`
- returns activation-operation identifier and user payload

Important behavior:
- at least one contact method must exist
- preferred verification contact must be consistent with provided contact data
- locale must be supported
- duplicate email/mobile/username is rejected

### `POST /registration/other-provider`

Registers or logs in a user through an external identity provider path.

Purpose:
- decode upstream provider identity
- create or update local user state
- issue local application tokens

Success response:
- `201 Created`
- returns token response

Important behavior:
- depends on supported provider logic
- depends on existence of a valid default registered client
- may create a new local user or update an existing one

### `GET /registration/username/available/{username}`

Checks whether a username is available.

Success response:
- `200 OK`
- boolean result

### `GET /registration/email/available/{email}`

Checks whether an email is available.

Success response:
- `200 OK`
- boolean result

### `GET /registration/mobile/available/{mobile}`

Checks whether a mobile number is available.

Success response:
- `200 OK`
- boolean result

### `POST /registration/activation`

Resends an activation message.

Purpose:
- generate and send a fresh activation message for an unactivated account

Request:
- email or mobile
- whether activation should be by code

Success response:
- `201 Created`
- returns new activation-operation identifier

### `PUT /registration/activation`

Activates an account by verification code.

Purpose:
- verify activation code
- activate the account
- optionally issue tokens if client info is provided

Request:
- email/mobile
- code
- optional client id
- optional client secret

Success responses:
- `200 OK` if activation succeeds without token issuance
- `201 Created` if activation succeeds and tokens are issued

### `GET /registration/activation/link/{linkParam}`

Activates an account by verification link.

Purpose:
- validate activation link token
- activate the account
- return a rendered success/failure page

Success response:
- HTML page rather than JSON

## 2. Authentication Endpoints

Base path: `/auth`

### `POST /auth/login`

Authenticates an end user and returns tokens.

Purpose:
- authenticate by email/mobile plus password
- resolve registered client
- issue client-aware tokens

Request:
- email/mobile
- password
- optional client id
- optional client secret

Success responses:
- `200 OK` with token JSON for JSON-return clients
- `204 No Content` with `Set-Cookie` header for cookie-return clients

Important behavior:
- if no client id is supplied, the service may use the configured default client
- user must be activated
- lockout and failed-login rules apply
- cookie behavior depends on registered-client configuration

### `POST /auth/service/login`

Authenticates a service client.

Purpose:
- issue a short-lived access token for machine-to-machine use

Request:
- client id
- client secret

Success response:
- `200 OK`
- returns token JSON

Important behavior:
- only valid for `SERVICE` clients
- client secret must match stored client credentials

## 3. Token Refresh Endpoints

Base path: `/auth`

### `POST /auth/token`

Refreshes tokens for non-web clients.

Purpose:
- validate token state and refresh-token state
- consume the refresh token
- issue a new token pair

Request:
- access token
- refresh token
- client id
- optional client secret

Success response:
- `201 Created`
- returns new token pair

Important behavior:
- refresh token is one-time use
- blocked, expired, reused, or mismatched token state is rejected
- client identity must match the original token context

### `POST /auth/web/token`

Refreshes tokens for web clients using a JWT cookie.

Purpose:
- read access token from `jwt` cookie
- validate client and token state
- issue a replacement access token in a cookie

Request body:
- client id
- optional client secret

Request requirements:
- `jwt` cookie must be present

Success response:
- `200 OK`
- rewrites `jwt` cookie

Important behavior:
- only valid for appropriate web-client configurations
- missing cookie is rejected
- token-state validation still applies

## 4. JWKS Endpoint

### `GET /oauth2/jwks`

Returns public signing keys.

Purpose:
- allow token consumers to verify JWT signatures

Success response:
- `200 OK`
- JWKS document

Consumers:
- downstream services
- resource servers
- any verifier that trusts this auth service as issuer

## 5. User Endpoints

Base path: `/user`

These endpoints generally require an authenticated user unless otherwise noted.

### `GET /user/info`

Returns information about the authenticated user.

Success response:
- `200 OK`
- user DTO

Important behavior:
- unauthenticated requests are rejected
- service principals are not treated as normal users here

### `GET /user/locale`

Returns the locale settings of the authenticated user.

Success response:
- `200 OK`
- locale DTO

### `PUT /user/password`

Changes the password of the authenticated user.

Request:
- old password
- new password
- repeat/confirmation fields if required by DTO rules

Success response:
- `204 No Content`

### `POST /user/password/reset`

Creates a password-reset request.

Purpose:
- generate a reset code or reset link
- trigger outbound reset communication

Request:
- email/mobile
- by-code flag

Success response:
- `201 Created`
- returns reset-operation identifier

### `PUT /user/password/reset/code`

Completes password reset using a code.

Purpose:
- validate reset operation
- set new password
- optionally issue tokens

Success responses:
- `204 No Content` when password reset completes without token issuance
- `201 Created` when password reset completes and tokens are issued

### `GET /user/password/reset/password-reset/{linkParam}`

Renders the hosted password-reset page for a valid reset link.

Success response:
- HTML page

### `POST /user/password/reset/password-reset`

Submits the hosted password-reset form.

Success response:
- rendered success/failure page

### `GET /user/password/reset/forgot-password`

Returns the forgot-password page.

Success response:
- HTML page

### `POST /user/password/reset/forgot-password`

Submits forgot-password form input and initiates reset-link delivery.

Success response:
- rendered success/failure page

### `PUT /user/email`

Initiates user email update flow.

Purpose:
- request an email change through user-meta-change mechanics

### `PUT /user/email/verify`

Verifies email change or email verification using a code.

### `PUT /user/mobile`

Initiates user mobile update flow.

### `PUT /user/mobile/verify`

Verifies mobile change or mobile verification using a code.

### `PUT /user/personal-info`

Updates user profile/personal fields.

Success response:
- `200 OK`

### `PUT /user/lang`

Updates user language preference.

Success response:
- `200 OK`

### `POST /user/fcm-token`

Registers or updates a user FCM token.

Success response:
- `200 OK`

### `GET /user/fcm-token`

Returns the user’s FCM tokens.

Success responses:
- `200 OK` when tokens exist
- `404 Not Found` when none exist

### `POST /user/logout`

Logs out the current user.

Purpose:
- block the current token server-side

Success response:
- `200 OK`

### `POST /user/logout/firebase`

Logs out an FCM installation association.

Purpose:
- delete FCM token registration linked to a user/install context

Success response:
- `200 OK`

## 6. SMS Endpoints

Base path: `/sms`

This controller should be documented separately based on the exact supported SMS verification flows.

Likely capabilities include:
- starting SMS verification
- checking SMS verification
- deleting/canceling verification state

For final documentation, each SMS endpoint should describe:
- whether it uses local SMS logic or Twilio-backed verification
- whether it is public or authenticated
- how it interacts with activation or verification state

## 7. Admin Endpoints

Administrative endpoints are grouped under `/adm`.

These are security-sensitive and should be documented with required authorities.

## 7.1 Localization Admin

Base path: `/adm/localization`

Purpose:
- manage localization messages and supported locales

Typical admin capabilities:
- create localization entries
- update localization entries
- fetch localization resources

Documentation requirements:
- required authority
- input validation rules
- conflict behavior for existing localization keys

## 7.2 Admin User Operations

Base path: `/adm/user`

Purpose:
- perform privileged user-management operations

Typical capabilities should be documented from the controller implementation, such as:
- querying users
- reviewing state
- administrative updates

## 7.3 Token Management

Base path: `/adm/token-management`

Purpose:
- block or unblock tokens
- query token block state
- perform admin token control operations

Important behavior:
- these endpoints directly affect access control
- they should be treated as privileged security operations

## 8. Error Model

The service generally returns structured error responses for validation and business-rule failures.

Common error categories include:
- invalid parameters
- invalid credentials
- invalid client
- invalid or blocked tokens
- expired or reused refresh tokens
- user not found
- invalid user state
- unsupported locale or invalid operation state

Typical fields include:
- numeric error code
- message
- detail
- timestamp

## 9. Status Code Patterns

Common status patterns include:

- `200 OK`
  successful reads or successful non-creation operations

- `201 Created`
  successful resource creation or token creation in flows that issue new auth artifacts

- `204 No Content`
  successful operations with no response body, especially cookie-return login flows or password changes

- `400 Bad Request`
  validation failures, business-rule failures, token/state mismatches

- `401 Unauthorized`
  missing or invalid authentication for protected endpoints

- `403 Forbidden`
  authenticated principal lacks required authority

- `404 Not Found`
  resource not found in selected endpoints

## 10. Final API Documentation Recommendation

For the final version of this document, each endpoint should eventually be expanded into a consistent format:

- Method and path
- Purpose
- Authentication required
- Request body / path params / query params
- Success response
- Error cases
- Side effects
- Notes on demo vs production behavior

This API document should be read together with:

- `docs/capabilities.md`
- `docs/auth-model.md`
- `docs/configuration.md`
- `docs/demo-vs-production.md`
