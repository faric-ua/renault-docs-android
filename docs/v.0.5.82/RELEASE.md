# Renault Docs v0.5.82 / build 98 — Home Add parity and landscape system bars

Status: DEVELOPMENT / PHONE QA PENDING.

Goals: globally hide Android status/navigation system bars in landscape and restore them in portrait (do not modify IME or SAF system UI); Home has only a collapsible/pinnable Add panel and My Renault project list. Add panel holds New volume, New project, Ready projects, legacy converter/tools, project-model explanation, local legacy entries, plus operation status outside collapsed actions. Keep every function/data safely reachable. Preserve portrait pin preference during landscape.

Safety: no project/volume/archival mutation or migration. Existing workflows and callbacks remain unchanged. No user data deleted.
