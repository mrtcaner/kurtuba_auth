# API Reference

## Overview

`kurtuba-auth` exposes endpoints under `/auth` for registration, activation, authentication, token refresh, account recovery, user account operations, SMS testing hooks, public key discovery, and administrative operations.

This document is organized by endpoint group so API consumers can understand the public surface and the most important behavioral details.

## Conventions

Unless otherwise noted:

- request bodies are JSON
- successful responses use standard HTTP status codes
- validation and business-rule failures are typically returned as `400 Bad Request`
- protected endpoints require a valid access token
- some responses differ depending on registered-client behavior
- hosted browser flows return HTML rather than JSON

## Authentication Requirements

The API surface falls into three categories:

- public endpoints, such as registration, activation, login, password-reset initiation, and JWKS
- authenticated user endpoints, which require a valid user token
- privileged/admin/service endpoints, which require appropriate scopes or authorities

## 1. Registration Endpoints

Base path: `/auth/registration`

### `POST /auth/registration`

Creates a new Kurtuba-native user account and starts the activation flow.

Typical request fields:
- `name`
- `surname`
- `username`
- `email`
- `mobile`
- `password`
- `preferredVerificationContact`
- `verificationByCode`
- `languageCode`
- `countryCode`

Success response:
- `201 Created`
- returns activation metadata and the created user payload

Important behavior:
- at least one contact method must exist
- preferred verification contact must be consistent with provided contact data
- duplicate email/mobile/username is rejected
- if `username` is omitted, the service generates a unique username and marks it as changeable once
- locale must already exist in supported locales
- the default `USER` role is assigned

### `POST /auth/registration/other-provider`

Registers or logs in a user through an external identity provider path, then issues local application tokens.

Supported providers in current code:
- `GOOGLE`
- `FACEBOOK`

Request fields:
- `provider`
- `providerClientId`
- either `token` or `authorizationCode`
- `redirectUri` when using `authorizationCode`
- optional `registeredClientId`
- optional `registeredClientSecret`
- `languageCode`
- `countryCode`

Current provider behavior:
- Google accepts either a direct ID token or an authorization code that the server exchanges for an ID token
- Facebook accepts either an access token or an authorization code that the server exchanges for an access token, then uses `/me` to fetch user data
- if `registeredClientId` is omitted, the endpoint falls back to a `DEFAULT` registered client
- if an account already exists with the same email, that account is reused
- the endpoint returns local auth tokens, not just provider user data

Success response:
- `201 Created`
- returns local `TokensResponseDto`

### `GET /auth/registration/username/available/{username}`

Checks whether a username is available.

### `GET /auth/registration/email/available/{email}`

Checks whether an email is available.

### `GET /auth/registration/mobile/available/{mobile}`

Checks whether a mobile number is available.

### `POST /auth/registration/activation`

Resends an activation message for an unactivated account.

Request:
- `emailMobile`
- `byCode`

Success response:
- `201 Created`
- returns the new activation-operation identifier

### `PUT /auth/registration/activation`

Activates an account by verification code.

Request:
- `emailMobile`
- `code`
- optional `clientId`
- optional `clientSecret`

Success responses:
- `200 OK` when activation succeeds without token issuance
- `201 Created` when activation succeeds and returns tokens

### `GET /auth/registration/activation/link/{linkParam}`

Activates an account by verification link and returns a rendered success/failure page.

## 2. Authentication Endpoints

Base path: `/auth`

### `POST /auth/login`

Authenticates an end user and returns tokens.

Request:
- `emailMobile`
- `password`
- optional `clientId`
- optional `clientSecret`

Success responses:
- `200 OK` with token JSON for standard clients
- `204 No Content` with `Set-Cookie: jwt=...` for clients configured with `sendTokenInCookie=true`

Important behavior:
- if no client id is supplied, the service may use the configured `DEFAULT` client
- user must be activated
- lockout and failed-login rules apply
- cookie responses currently use an HTTP-only `jwt` cookie

### `POST /auth/service/login`

Authenticates a `SERVICE` registered client and returns a short-lived access token.

Request:
- `clientId`
- `clientSecret`

Success response:
- `200 OK`
- returns `TokensResponseDto` with an access token only

Important behavior:
- only `SERVICE` clients are accepted
- client secret must match the stored registered-client secret

## 3. Token Refresh Endpoints

Base path: `/auth`

### `POST /auth/token`

Refreshes tokens for non-web clients.

Request:
- `accessToken`
- `refreshToken`
- `clientId`
- optional `clientSecret`

Success response:
- `201 Created`
- returns a new token pair

Important behavior:
- refresh is tied to the original access token and persisted token record
- the refresh token is base64-decoded and checked against the stored BCrypt hash
- refresh tokens are one-time use
- blocked, expired, reused, or mismatched token state is rejected
- client identity must match the original token context

### `POST /auth/web/token`

Refreshes tokens for `WEB` clients that keep the access token in the `jwt` cookie.

Request body:
- `clientId`
- optional `clientSecret`

Request requirements:
- `jwt` cookie must be present

Success response:
- `200 OK`
- rewrites the `jwt` cookie

Important behavior:
- only valid for `WEB` clients
- the registered client must also support refresh tokens
- missing cookie is rejected

## 4. JWKS Endpoint

### `GET /auth/oauth2/jwks`

Returns the public signing keys used to verify JWT signatures.

Success response:
- `200 OK`
- JWKS document containing the public portion of all loaded signing keys

## 5. User Endpoints

Base path: `/auth/user`

### `GET /auth/user/{id}`

Service-only endpoint to fetch user data by id.

Authorization:
- requires `SCOPE_SERVICE`

### `GET /auth/user/info`

Returns information about the authenticated user.

Important behavior:
- service principals are rejected here

### `GET /auth/user/locale`

Returns the locale of the authenticated user.

Important behavior:
- service principals are rejected here

### `PUT /auth/user/password`

Changes the password of the authenticated user.

### `POST /auth/user/password/reset`

Creates a password-reset request by code or link.

### `PUT /auth/user/password/reset/code`

Completes password reset using a code.

Success responses:
- `204 No Content` when password reset succeeds without token issuance
- `201 Created` when password reset also returns tokens

### `GET /auth/user/password/reset/password-reset/{linkParam}`

Returns the hosted password-reset page for a valid reset link.

### `POST /auth/user/password/reset/password-reset`

Handles the hosted password-reset form submission.

### `GET /auth/user/password/reset/forgot-password`

Returns the forgot-password page.

### `POST /auth/user/password/reset/forgot-password`

Handles forgot-password form submission and sends a reset link.

### `PUT /auth/user/email`

Initiates an email-change flow.

### `DELETE /auth/user/email`

Deletes the authenticated user's email contact.

Important behavior:
- the account must remain with at least one contact method
- deleting a verified contact requires another verified contact to remain
- pending email-change metadata is removed

### `PUT /auth/user/email/verify`

Verifies email by code.

### `GET /auth/user/email/verify/{linkParam}`

Verifies email by link when that flow is used.

### `PUT /auth/user/mobile`

Initiates a mobile-change flow.

### `DELETE /auth/user/mobile`

Deletes the authenticated user's mobile contact.

Important behavior:
- the account must remain with at least one contact method
- deleting a verified contact requires another verified contact to remain
- pending mobile-change metadata is removed

### `PUT /auth/user/mobile/verify`

Verifies mobile by code.

### `PUT /auth/user/personal-info`

Updates user personal/profile fields.

### `PUT /auth/user/username`

Updates the authenticated user's username.

Request body:
- `username`

Important behavior:
- the requested username must pass username validation
- duplicate usernames are rejected
- the update is allowed only when the user's settings still permit username change
- after a successful change, the user cannot change username again unless that flag is reset administratively

### `PUT /auth/user/lang`

Updates user language preference.

### `POST /auth/user/fcm-token`

Registers or updates a user FCM token.

### `GET /auth/user/fcm-token`

Returns the user’s stored FCM tokens.

### `POST /auth/user/logout`

Logs out the current user by blocking the current token server-side.

### `POST /auth/user/logout/firebase`

Logs out an FCM installation association.

## 6. SMS Endpoints

Base path: `/auth/sms`

Current behavior:
- this controller is only active in the `dev` profile
- it is intended as a convenience/testing surface, not a stable production API

Endpoints:
- `POST /auth/sms`
- `POST /auth/sms/verification`
- `PUT /auth/sms/verification`
- `DELETE /auth/sms/verification`
- `POST /auth/sms/message-status`

## 7. Admin Endpoints

Administrative endpoints are grouped under `/auth/adm` and should be treated as security-sensitive.

Current groups include:
- `/auth/adm/localization`
- `/auth/adm/user`
- `/auth/adm/token-management`

These endpoints require appropriate authorities/scopes and are intended for privileged operational use.

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

## 9. Status Code Patterns

Common status patterns include:

- `200 OK` for successful reads and standard successful operations
- `201 Created` for registration, activation-with-token, refresh, and similar creation/token-issuance flows
- `204 No Content` for successful cookie-return login flows and some mutation endpoints without response bodies
- `400 Bad Request` for validation failures and business-rule failures
- `401 Unauthorized` for missing or invalid authentication
- `403 Forbidden` for principals lacking required authority
- `404 Not Found` in selected lookup flows
