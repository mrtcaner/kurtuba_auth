# Admin Pages

## Overview

`kurtuba-auth` includes a browser-oriented admin UI built with Spring MVC and Thymeleaf.

These pages live under `/auth/adm` and are intended for operational/admin use rather than public client integration.

The admin UI sits alongside the JSON admin endpoints. The pages are useful when you want to inspect or update auth state manually from a browser.

## Access Model

Admin page access is security-sensitive.

Current behavior in code:

- the admin login page is exposed at `/auth/adm/login`
- the main admin dashboard is exposed at `/auth/adm`
- most admin page controllers require `SCOPE_ADMIN`
- the OAuth client page controller accepts `ADMIN` or `SCOPE_ADMIN`

The admin login flow uses a browser-oriented registered client named `adm-web-client`, authenticates an admin user, and writes the access token into the `jwt` cookie before redirecting into the admin area.

Seeded bootstrap account note:

- the baseline migration seeds a user with email `user@user.com`
- the seeded password is `a.1234`

This is useful for initial evaluation, but it should be treated as a bootstrap credential rather than a production-safe default.

Relevant code:

- [AdmLoginController.java](/Users/murat/projects/kurtuba-auth/src/main/java/com/kurtuba/adm/controller/AdmLoginController.java)
- [AdmPageController.java](/Users/murat/projects/kurtuba-auth/src/main/java/com/kurtuba/adm/controller/AdmPageController.java)
- [DefaultSecurityConfig.java](/Users/murat/projects/kurtuba-auth/src/main/java/com/kurtuba/auth/config/DefaultSecurityConfig.java)

## Route Map

Main page routes:

- `GET /auth/adm/login`
- `POST /auth/adm/login`
- `GET /auth/adm`
- `GET /auth/adm/pages/users`
- `GET /auth/adm/pages/users/{id}`
- `GET /auth/adm/pages/tokens`
- `GET /auth/adm/pages/localization`
- `GET /auth/adm/pages/localization/messages`
- `GET /auth/adm/pages/localization/languages`
- `GET /auth/adm/pages/localization/countries`
- `GET /auth/adm/oauth-clients`
- `GET /auth/adm/oauth-clients/new`
- `GET /auth/adm/oauth-clients/{id}`
- `GET /auth/adm/pages/push-notifications`

The shared admin shell and navigation live in:

- [adm/fragments.html](/Users/murat/projects/kurtuba-auth/src/main/resources/templates/adm/fragments.html)

## Dashboard

The admin dashboard is the landing page after successful admin login.

Current quick links from the dashboard include:

- OAuth Clients
- Localization
- Token Management

Relevant files:

- [AdmPageController.java](/Users/murat/projects/kurtuba-auth/src/main/java/com/kurtuba/adm/controller/AdmPageController.java)
- [adm/index.html](/Users/murat/projects/kurtuba-auth/src/main/resources/templates/adm/index.html)

## User Management Pages

The user admin pages support:

- searching users by id, username, email, mobile, name, surname, auth provider, locale, and role
- filtering by activated, locked, blocked, captcha, email-verified, and mobile-verified state
- paging user search results with selectable page sizes
- viewing a user detail page
- adding roles to a user
- removing roles from a user
- updating user security/activity flags
- generating usernames for existing users that do not have one

Relevant files:

- [UserAdminPageController.java](/Users/murat/projects/kurtuba-auth/src/main/java/com/kurtuba/adm/controller/UserAdminPageController.java)
- [adm/users/index.html](/Users/murat/projects/kurtuba-auth/src/main/resources/templates/adm/users/index.html)
- [adm/users/detail.html](/Users/murat/projects/kurtuba-auth/src/main/resources/templates/adm/users/detail.html)

## Token Management Pages

The token admin page supports:

- listing tokens for a user
- filtering by client id, blocked state, and refresh-token-used state
- sorting token results
- looking up token state by JTI
- blocking all tokens for a user
- blocking a single token by JTI

Relevant files:

- [TokenAdminPageController.java](/Users/murat/projects/kurtuba-auth/src/main/java/com/kurtuba/adm/controller/TokenAdminPageController.java)
- [adm/tokens/index.html](/Users/murat/projects/kurtuba-auth/src/main/resources/templates/adm/tokens/index.html)

## Localization Pages

The localization pages support:

- searching localization messages by language, key, and message content
- creating localization messages
- updating localization messages
- deleting localization messages
- viewing supported languages
- adding or deleting supported languages
- viewing supported countries
- adding or deleting supported countries

Relevant files:

- [LocalizationAdminPageController.java](/Users/murat/projects/kurtuba-auth/src/main/java/com/kurtuba/adm/controller/LocalizationAdminPageController.java)
- [adm/localization/messages.html](/Users/murat/projects/kurtuba-auth/src/main/resources/templates/adm/localization/messages.html)
- [adm/localization/languages.html](/Users/murat/projects/kurtuba-auth/src/main/resources/templates/adm/localization/languages.html)
- [adm/localization/countries.html](/Users/murat/projects/kurtuba-auth/src/main/resources/templates/adm/localization/countries.html)

## OAuth Client Pages

The OAuth client pages support:

- listing registered clients
- creating a new client
- editing an existing client
- configuring client type, scopes, token TTLs, refresh behavior, and cookie behavior
- preserving an existing secret on edit when a new raw secret is not entered

Service-client creation currently enforces a raw client secret on create.

Relevant files:

- [OAuthClientAdminPageController.java](/Users/murat/projects/kurtuba-auth/src/main/java/com/kurtuba/adm/controller/OAuthClientAdminPageController.java)
- [RegisteredClientFormDto.java](/Users/murat/projects/kurtuba-auth/src/main/java/com/kurtuba/adm/data/dto/RegisteredClientFormDto.java)
- [adm/oauth-clients/list.html](/Users/murat/projects/kurtuba-auth/src/main/resources/templates/adm/oauth-clients/list.html)
- [adm/oauth-clients/form.html](/Users/murat/projects/kurtuba-auth/src/main/resources/templates/adm/oauth-clients/form.html)

## Push Notification Pages

The push-notification admin page is a search/inspection page for stored FCM-related records.

Current filters include:

- user id
- user email
- user mobile
- user role
- Firebase installation id
- FCM token

Relevant files:

- [PushNotificationAdminPageController.java](/Users/murat/projects/kurtuba-auth/src/main/java/com/kurtuba/adm/controller/PushNotificationAdminPageController.java)
- [adm/push-notifications/index.html](/Users/murat/projects/kurtuba-auth/src/main/resources/templates/adm/push-notifications/index.html)

## Relationship to Admin APIs

The admin pages are not the only admin surface.

There are also JSON admin endpoints under `/auth/adm` for things like:

- localization operations
- user admin operations
- token-management operations

See [api.md](/Users/murat/projects/kurtuba-auth/docs/api.md) for the API-oriented side.

## Current State

The admin UI is present and usable, but the polish level varies by section.

Some parts feel more operational and internal than public-productized. That is consistent with the current repo state: production-shaped functionality is present, but the admin UI should still be treated as an operator tool rather than a fully documented end-user product surface.
