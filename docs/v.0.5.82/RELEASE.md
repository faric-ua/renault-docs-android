# Renault Docs v0.5.82 / build 98 — Home Add parity and landscape system bars

Status: **PR #88 MERGED / MAIN CI PASS / STABLE-SIGNED APK READY / PHONE QA PENDING (NOT CLOSED)**.\n\n- Runtime source/merge: `f17b319c4e17d8f7005ae20221dd7bd642cb172b`.\n- PR Python Tests #571 PASS, Android PR Check #456 PASS; main Tests #572 PASS, signed Android Debug APK #145 PASS.\n- Artifact: `Renault-Docs-v0.5.82-Debug` ID `11563839781`, sha256 `94cadb5caf5b1af93a2a959e674819c31f0571b479c66f7b9a492af6ae6586be`. Expires 2026-10-11 16:39:51 UTC.\n- Install only over current app via Termux Renault menu `5 → 19 → 8 → 13`; phone QA still required.

Goals: globally hide Android status/navigation system bars in landscape and restore them in portrait (do not modify IME or SAF system UI); Home has only a collapsible/pinnable Add panel and My Renault project list. Add panel holds New volume, New project, Ready projects, legacy converter/tools, project-model explanation, local legacy entries, plus operation status outside collapsed actions. Keep every function/data safely reachable. Preserve portrait pin preference during landscape.

Safety: no project/volume/archival mutation or migration. Existing workflows and callbacks remain unchanged. No user data deleted.
