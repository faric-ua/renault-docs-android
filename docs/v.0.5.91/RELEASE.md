# Renault Docs v0.5.91 — status text and notification lifecycle

2026-10-09 screenshot-driven follow-up to v0.5.90: ZIP and Runtime IR/Fast Pack have correct green real progress and successful NT8299A · 2005-11-28 · 341 native (Megane II list 14 to 15). Remaining defects on Samsung: bottom of live phase letters including "Розпаковую ZIP…" visually clipped, some terminal SHA-256 long lines overflow, and a stale "Перевірка .rdpkg" foreground progress notification remains alongside distinct "готовий" result in Android shade.

Fix candidate:
- `OperationStatusView` uses intrinsic WRAP_CONTENT with 32dp minimum and small padding rather than fixed 20dp for all running stage/count labels. Higher font sizes may grow without cutting bottom glyphs.
- Terminal text also receives safe vertical padding and explicit SHA-256 line breaks (24 characters per display row); the original full detail string remains unchanged when copied.
- Native preparation removes the completed foreground notification atomically with `STOP_FOREGROUND_REMOVE`, plus a defensive explicit cancellation; block stale progress updates after terminal and handle redelivery/cancel. Preserve WAITING_SELECTION's separate actionable status.
- Standalone importer likewise removes its foreground progress with REMOVE.
- Android `Notification.Builder.setColor` requests green success/running accent and semantic failure/cancel colors where supported. Android/Samsung owns system progress bar rendering, so its actual line may remain white.

Do not delete or reimport existing volumes. Retain grouped last-ten completed result notifications. No claim of phone PASS until exact stable signer build and device evidence.
