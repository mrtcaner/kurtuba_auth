# Backend Quickstart

## Who This Is For

This guide is for backend developers who want to evaluate `kurtuba-auth` as a self-hosted authentication server for their own services.

If your question is basically “Can I run this, issue tokens, verify them from my backend, and use it against a real PostgreSQL database?” this is the right starting point.

## What You Get

Out of the box, the repository is structured to support:

- user registration and activation
- email/mobile login
- JWT access tokens
- refresh-token rotation
- service-client login
- logout with server-side token blocking
- password reset flows
- JWKS publishing at `/auth/oauth2/jwks`

## Fastest Evaluation Path

The simplest way to evaluate the project is:

1. Run it with the checked-in PostgreSQL-backed local defaults.
2. Review the docs for token/key handling.
3. Verify JWT/JWKS behavior from your backend.
4. Tighten config for your own environment.

The checked-in config is still local-development oriented, but runtime persistence now assumes PostgreSQL from the start.

## Step 1: Run the Server with PostgreSQL

The fastest realistic startup path is the checked-in Docker Compose stack together with the checked-in defaults.

Typical run command:

```bash
docker compose up -d
./scripts/bootstrap-local-db.sh
mvn spring-boot:run
```

You will need a reachable PostgreSQL database and valid JWK decryption config. See [postgresql.md](/Users/murat/projects/kurtuba-auth/docs/postgresql.md) for the database side.

If you only want the database:

```bash
docker compose up -d postgres
./scripts/bootstrap-local-db.sh
```

## Step 2: Understand the Minimum Config You Must Care About

Even for a quick evaluation, there are four config areas worth understanding early:

- `auth.server.issuer-url`
- `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`
- `kurtuba.jwk.file`
- `kurtuba.jwk.keys`

The first two control issuer/JWKS behavior.

The second two control signing-key loading.

## Step 3: Provide Signing Keys

The server signs JWTs using encrypted JWK entries loaded from:

- `kurtuba.jwk.file`
- `kurtuba.jwk.keys`

The public repo keeps this safe by using placeholders in config. That means a serious startup path needs you to provide real values.

### JWK file

This property is a Spring `Resource`, so values like these are valid:

- `classpath:jwk.json`
- `file:/absolute/path/to/jwk.json`

### JWK secrets

`kurtuba.jwk.keys` is a JSON object mapping JWK entry id to base64 decryption secret.

Example shape:

```yaml
kurtuba:
  jwk:
    file: file:/absolute/path/to/jwk.json
    keys: '{"your-key-id":"your-base64-secret"}'
```

If the key map does not contain a matching secret for an entry in the JWK file, startup will fail.

If you need the full operational model, read [key-management.md](/Users/murat/projects/kurtuba-auth/docs/key-management.md).

## Step 4: Use It From Your Backend

If you are integrating this as an auth server for another backend, the normal pattern is:

1. Authenticate users or service clients against `kurtuba-auth`.
2. Receive JWT access tokens.
3. Verify those JWTs from your backend using the JWKS endpoint.
4. Trust the token issuer and scopes according to your authorization model.

Current JWKS endpoint:

- `/auth/oauth2/jwks`

That endpoint publishes the public portion of all loaded signing keys, which also supports key rotation overlap.

## Step 5: Understand the Client Model

This server is not one global login flow. Token behavior depends on the registered client.

Registered clients control things like:

- access-token TTL
- refresh-token TTL
- whether refresh is enabled
- whether scopes are added
- whether tokens are returned in cookies
- cookie max age
- audiences
- client type

That means if you adopt this server, you should think about client definitions early instead of treating them as optional metadata.

## Useful Auth Flows to Evaluate First

If you are deciding whether to adopt the repo, these are the best first checks:

### Native user login

Check:
- registration
- activation
- login
- refresh
- logout

This tells you whether the default user lifecycle matches your needs.

### Service-client login

Check:
- `/auth/service/login`
- resulting scopes/audiences
- JWT verification from your own backend

This tells you whether it can support backend-to-backend trust in your environment.

### JWKS verification

Check:
- that your backend can fetch `/auth/oauth2/jwks`
- that issued JWTs validate against the published keys
- that issuer and audience expectations are aligned

This tells you whether it fits your backend’s resource-server model.

## Why PostgreSQL Is the Baseline

Use PostgreSQL from the start when:

- you want realistic relational behavior
- you want persistent users and token state
- you want Flyway-managed schema migration
- you want something close to deployment reality

The PostgreSQL path has already been validated in this public repo with:

- `init_db.sql`-style setup
- Flyway enabled
- Hibernate set to `validate`

Use [postgresql.md](/Users/murat/projects/kurtuba-auth/docs/postgresql.md) for setup details.

## Two PostgreSQL Modes

You do not have to start with the production-style role split.

### Structured local mode

Use the checked-in bootstrap script, which creates the database plus separate Flyway and runtime roles from the existing [init_db.sql](/Users/murat/projects/kurtuba-auth/src/main/resources/db/sql/init_db.sql) model.

```bash
docker compose up -d
./scripts/bootstrap-local-db.sh
mvn spring-boot:run
```

This keeps local startup close to the production-shaped privilege split while still being easy to repeat.

## Things You Should Notice Before Adopting It

This repo is useful, but it is opinionated.

Important adoption notes:

- it is a self-hosted auth server, not a thin wrapper around a third-party identity platform
- token state is persisted, so refresh and revocation are stronger than in simplistic stateless-JWT demos
- key handling is flexible, but not “magic”; you need to own JWK material intentionally
- cookie handling is currently demo-friendly and must be hardened for production
- external-provider entry exists, but the server still issues and owns the local application tokens

## Good Fit

This project is a good fit if you want:

- a Spring Boot auth server you can actually read and modify
- control over user accounts, token lifecycle, and signing keys
- a backend-friendly JWT/JWKS model
- a path from local PostgreSQL startup to deployment

## Less Ideal Fit

This project is less ideal if you want:

- a zero-thinking hosted identity solution
- a turnkey OIDC platform with broad ecosystem features already packaged for you
- a system where you never have to care about keys, clients, or database posture

## Recommended Next Reading

After this quickstart, read these in order:

- [overview.md](/Users/murat/projects/kurtuba-auth/docs/overview.md)
- [configuration.md](/Users/murat/projects/kurtuba-auth/docs/configuration.md)
- [auth-model.md](/Users/murat/projects/kurtuba-auth/docs/auth-model.md)
- [postgresql.md](/Users/murat/projects/kurtuba-auth/docs/postgresql.md)
- [key-management.md](/Users/murat/projects/kurtuba-auth/docs/key-management.md)
- [api.md](/Users/murat/projects/kurtuba-auth/docs/api.md)
