# Renault Docs v0.5.71 — top-level archive raw-root duplicate fast-path fix

Status: **DEVELOPMENT**

## Phone evidence that triggered this release

v0.5.70 / build 86 was installed and the same duplicate NT8266A ZIP still entered full ZIP extraction before ending in `Том уже є`.

## Root cause

For an archive root such as:

`NT8266A_2004-06-28/INDEX.HTM`

archive inspection stored parent `NT8266A_2004-06-28`, but calculated its leaf name with:

`substringAfterLast('/', "")`

Because the parent had no slash, the explicit fallback returned an empty string. The hint was filtered out, so the early duplicate matcher never received it.

## Fix

Use the parent itself when no slash exists by relying on the normal `substringAfterLast('/')` behavior.

A regression test now verifies that a top-level Renault raw root keeps:
- relative path;
- leaf name;
- NT document code;
- no extraction side effect.

Phone acceptance requires the exact same NT8266A ZIP to reach `Том уже є` without `Розпаковую ZIP...`.
