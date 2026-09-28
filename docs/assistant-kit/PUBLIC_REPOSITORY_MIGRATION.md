# Renault Docs: private archive and public repository migration

## Goal

Preserve the exact development history while making the active project safe to publish and cheap to run on GitHub Actions.

## Chosen model

The pre-public repository remains a private historical archive. It keeps the exact Git history, old branches, commit topology, development notes, and the old development signing material. It is not converted to public.

A separate public repository should be created from a clean root commit based on the reviewed current source tree. This prevents old private Git objects from becoming reachable merely because repository visibility changes.

## Backups

Before any remote branch cleanup, run the Termux helpers generate-dev-signing-key.sh and backup-before-public.sh.

The backup helper creates an AES-256 encrypted archive under Documents/Renault/backups. It contains a full git bundle --all, ref/branch/commit inventories, a current source snapshot, and the new development signer material when present.

The encryption password is not stored in Git. Keep it separately.

## Development signing

The legacy tracked file .github/signing/renault-docs-dev.jks.b64 must never enter the public repository.

The public workflow reads these GitHub Actions Secrets:

- RENAULT_DEV_KEYSTORE_B64
- RENAULT_DEV_STORE_PASSWORD
- RENAULT_DEV_KEY_ALIAS
- RENAULT_DEV_KEY_PASSWORD
- RENAULT_DEV_CERT_SHA256

android/app/build.gradle.kts reads the corresponding values from environment variables. No signing password or keystore payload is committed.

Fork pull requests can build with the default ephemeral debug signer because GitHub does not expose repository secrets to untrusted forks. Trusted main and manual builds require the stable signer.

## Assistant access

The full private history remains available for analysis as long as the GitHub connector continues to have access to the private archive repository.

A backup that exists only on the phone is not directly accessible to ChatGPT. If the private archive is later removed from GitHub, the encrypted Git bundle can still be used after it is uploaded to a chat or otherwise made accessible together with its decryption password.

The active public repository remains directly inspectable and contains the safe versioned documentation used for future development and educational material.

## Do not delete old remote branches yet

Old branches are intentionally left untouched until:

- the encrypted full-history backup reports PASS;
- the new signer is generated;
- GitHub Actions Secrets are configured in the future public repository;
- the clean public seed is reviewed.

Only after those checks should the old remote branch set be pruned or the private archive repository be archived.
