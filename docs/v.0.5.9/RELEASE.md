# Renault Docs v0.5.9 — Runtime IR OOM hotfix

Date: 2026-09-24

## Real phone failure

v0.5.8 native Modern opened section 101 and then either:

- stayed on `Читаю Runtime IR v2…`, or
- failed with:

```text
Failed to allocate a 268501000 byte allocation
...
growth limit 268435456
```

## Root cause

`RuntimeIrReader` in v0.5.8 opened:

```text
_renault/runtime-tree.json
```

and called `readText()` on the complete Runtime IR for all 10 volumes / 2174 sections.

The real dataset made that file large enough to require an approximately 268.5 MB allocation, effectively colliding with the Android heap growth limit.

The problem is architectural, not a bad 101 section and not corrupted Renault data.

## Fix

The full `runtime-tree.json` remains a compiler/debug artifact.

Runtime data is now sharded:

```text
_renault/
├── runtime-tree.json              # compiler/debug only
├── runtime-ir-index.json          # small lookup index
└── runtime-ir/
    └── sections/
        ├── <volume-id>/
        │   ├── 101.json
        │   ├── 103.json
        │   └── ...
        └── ...
```

Android now:

1. reads only the small `runtime-ir-index.json`;
2. resolves the selected volume + section;
3. opens exactly one section JSON shard;
4. parses only that section.

Bounded UTF-8 readers are used so a future accidental giant runtime file fails cleanly instead of trying to consume the full app heap.

## Fast Pack

Native/compiler JSON under `_renault` is excluded from the legacy Fast Pack.

This prevents the huge full Runtime IR tree and thousands of section shards from being duplicated inside the legacy web ZIP.

## Version

- versionName: `0.5.9`
- versionCode: `25`

Dataset/package regeneration is required because the sharded files are new.
