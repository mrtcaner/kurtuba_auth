# Key Management

## Overview

`kurtuba-auth` signs JWT access tokens with private keys loaded from JWK material at startup and publishes the corresponding public keys through `/oauth2/jwks`.

This document explains:

- which signing algorithms are supported
- how keys are stored in this repository
- how the active signing key is selected
- how to generate new signing keys
- how to rotate keys safely
- how to retire old keys without breaking token verification

## Supported Algorithms

The application is configured to verify JWTs signed with:

- `RS256`
- `ES256`

However, the helper path currently bundled in the repository generates RSA JWKs by default, so the practical default deployment model is RSA signing with JWKS publication.

## Current Storage Model

The current implementation uses a split storage model for signing keys:

- `src/main/resources/jwk.json` contains encrypted JWK payloads
- `kurtuba.jwk.keys` contains a JSON map from key entry id to base64-encoded decryption secret

This means the private signing key is not stored directly in plaintext in `jwk.json`. Instead, the application reconstructs it at startup by decrypting the stored JWE payload.

## How Keys Are Loaded

At startup, `TokenUtils` performs the following sequence:

1. Read the encrypted key entries from `jwk.json`
2. Read the decryption-secret map from `kurtuba.jwk.keys`
3. Match each entry by `id`
4. Decrypt each `encryptedKey` payload
5. Reconstruct the full JWK, including private key material
6. Sort all keys by `order` in descending order
7. Use the highest-ordered key as the active signing key

All loaded keys remain available for verification and JWKS publication.

## Entry Format

Each encrypted entry in `jwk.json` is expected to contain:

- `id`
- `order`
- `encryptedKey`

The corresponding property value for `kurtuba.jwk.keys` is a JSON object where:

- each property name is the same entry `id`
- each property value is the base64-encoded decryption secret for that entry

## Active Signing Key Selection

The active key is selected purely by ordering.

Rules:

- the highest `order` value becomes the active signing key
- that key is used to sign all new JWTs
- older loaded keys remain valid for verification
- JWKS exposes all loaded public keys

This makes rollover possible without breaking existing tokens, provided old keys are not removed too early.

## Encryption Used for Stored Keys

The helper in `JwkGenerator` encrypts JWK payloads as JWE before storage.

The current helper uses:

- key management algorithm: `A256GCMKW`
- content encryption method: `AES_256_GCM`

This encryption protects the serialized private JWK payload at rest, assuming the decryption secret is stored separately and securely.

## Generating a New Signing Key

The repository contains helper methods in `JwkGenerator` for:

- generating RSA JWKs
- generating EC JWKs
- encrypting generated JWK payloads into the storage format used by the app

The current default helper path uses RSA generation.

### Default generation path

The helper method `createJwe()` currently:

1. Generates a new RSA JWK
2. Generates an AES key used to encrypt that JWK payload
3. Produces a compact JWE string
4. Prints the generated secret and encrypted payload

### Important limitation

`JwkGenerator` is currently a developer helper, not a production-grade operational tool.

Before using it in a real environment, review:

- how the encryption secret is generated
- where helper output is printed
- how artifacts are transferred into production config
- whether RSA or EC is the intended real algorithm for your deployment

## Recommended Generation Procedure

For a controlled rollout, the recommended process is:

1. Generate a new key pair using the helper or an external secure process.
2. Produce an encrypted JWK payload in the same format expected by `jwk.json`.
3. Assign a new unique `id`.
4. Assign an `order` greater than the current highest order.
5. Store the encrypted payload in `jwk.json`.
6. Store the matching base64 decryption secret under the same `id` in `kurtuba.jwk.keys`.
7. Deploy with both old and new keys present.

## Safe Rotation Procedure

Use this rotation sequence:

1. Add the new encrypted key entry to `jwk.json`.
2. Add the new secret to `kurtuba.jwk.keys`.
3. Set the new key’s `order` higher than the current active key.
4. Deploy the service.
5. Confirm new tokens are issued with the new `kid`.
6. Confirm `/oauth2/jwks` exposes both old and new public keys.
7. Wait until all tokens signed by the old key are expired.
8. Remove the old encrypted key entry and its matching secret in a later deployment.

## Why Rotation Works

Rotation works because the service:

- signs with only the highest-ordered key
- verifies with any loaded matching key
- publishes all loaded public keys via JWKS

This allows:

- new tokens to move to the new key immediately
- old tokens to continue verifying until expiry

## How to Verify Rotation

After deployment, check the following:

1. Issue a fresh token and inspect its JWT header.
2. Confirm the `kid` matches the new active key.
3. Call `/oauth2/jwks`.
4. Confirm both old and new public keys are present during the overlap window.
5. Verify an old token still validates.
6. Verify a newly issued token validates.

## Retiring Old Keys

Do not remove an old key immediately after introducing a new one.

Only retire a key after:

- all access tokens signed with it are expired
- any downstream cache window for JWKS has passed
- any operational grace window you require has passed

Removing a key too early will cause still-valid tokens to fail verification.

## Storage Recommendations for Production

The current split model is acceptable as an application-managed mechanism, but it is not a substitute for stronger key custody controls.

Minimum production guidance:

- keep encrypted JWKs and decryption secrets separate
- do not commit real production secrets into source control
- inject `kurtuba.jwk.keys` via environment or secret manager
- restrict access to both the encrypted JWK artifact and the secret source

Stronger production guidance:

- use a dedicated secret manager for decryption secrets
- avoid bundling real encrypted key artifacts in a widely shared image if possible
- consider KMS- or HSM-backed signing if your security requirements demand stronger key protection

## Risks and Failure Modes

Common rotation and storage mistakes include:

- adding a new encrypted key without its matching secret
- assigning the wrong `order` and activating the wrong key
- removing old keys before old tokens expire
- changing issuer/JWKS behavior during rotation
- leaving real secrets in committed configuration
- generating keys in an insecure or non-repeatable way

## Operational Checklist

Before rotating keys:

- confirm current active key id
- confirm current highest `order`
- define token-expiry overlap window
- prepare new encrypted key and secret

During deployment:

- verify service startup succeeds
- verify JWKS includes expected keys
- verify new tokens use the expected `kid`

After overlap window:

- remove old key entry
- remove old secret
- redeploy
- verify only intended public keys remain in JWKS

## Related Documents

Read this together with:

- `docs/auth-model.md`
- `docs/configuration.md`
- `docs/demo-vs-production.md`
- `docs/overview.md`
