# Renault Docs — what's actually left (2026-10-09)

**Installed code:** v0.5.82 / build 98, runtime source `f17b319c4e17d8f7005ae20221dd7bd642cb172b`, signed APK #145 CI PASS. **QA PAUSED by user request** after video Test 8 PASS. Do not delete source ZIP, prepared RDPKG or installed volumes; no new conversions just for tests.

## Actual code/feature gaps

1. **#79 Home → «Новий том» route — CONFIRMED NOT FIXED.** The current code still calls `MainActivity.chooseProjectForVolume()` → `ProjectChooserActivity` → `ProjectActivity.intent(..., openPicker=true)`; `ProjectActivity.onCreate()` auto-launches the ready `.rdpkg` document picker. Expected: after choosing a project, display its `Додати` panel with the **Auto / Manual / raw / archive** choices and *no automatic picker*. This is the most concrete next development task.
2. **#40 background lifecycle across ALL long-running services — open architectural completeness task.** Multiple foreground/progress flows exist, but repository issue still requires per-service lifecycle/wakelock/redelivery coverage. Do not describe the feature as entirely missing or already complete until audited.
3. **#51 archive ZIP/7Z/RAR intake — functional flows implemented but full release acceptance still open.** Need later examine multi-volume, cancel/cleanup, lock/background and source preservation. User explicitly asked to postpone phone tests.
4. **#30 Windows-source → prepared catalogue publishing pipeline — separate ongoing data/tooling integration**, not a blocker for reading already-installed tomes.

## Optional/polish, not blockers for normal use

- **#21** Support/Donate link in Settings, no target URL selected.
- **#50** Simplify Termux item 19 (tooling UI only).

## Implemented features with open/stale tracking issues

- **#68** Project/Home compact pinned Add panels: implemented and phone accepted.
- **#23** landscape system bars / portrait restore: implemented and phone accepted on Home, Help, Settings, native Modern sections.
- **#85 / #82** prepared `.rdpkg` source rejection and wrong-vehicle model safeguard: implemented v0.5.80; user confirmed disabled picker entries and Kangoo→Megane blocking, plus duplicate warning and preflight rotation. GitHub issues still open; reconcile evidence before deciding whether to close.
- **#81 / #83** original archive immutability / historical Android picker trail: retain as data-safety/provenance follow-ups; not proof of new data loss or absent UI functionality.
- Previous progress/status phone QA and exact archived source formats remain unaccepted, not necessarily missing code.

## Last phone evidence

- **Test 7**: no active Home status; `N/A / DEFERRED`, not PASS or FAIL.
- **Test 8**: user video `607185.mp4` (~32 s) demonstrates Home `Додати` collapse/expand and accessible `Мої Renault` project tiles (Megane II 10, Laguna II 10, Kangoo II 1), plus Project `Додати` collapsed/expanded with existing tomes. **VIDEO PASS**. Earlier visual overlap suspicion not reproduced as an accessibility failure.
- All additional phone QA **paused** until user explicitly resumes. No new app build or code change was made as part of this review.

**Suggested next code scope when user chooses:** implement #79 in a focused PR; preserve intentional direct-`.rdpkg` picker callers and activity/rotation state. Then ask before resuming manual QA.
