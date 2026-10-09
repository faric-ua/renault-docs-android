# Follow-up UX findings from v0.5.90 phone

- #100 — stage label baseline/bottom clipped by rigid running-row 20dp even though new measured progress is good. Fix in v0.5.91: dynamic 32dp min WRAP_CONTENT and vertical padding, full SHA text layout. Phone QA pending.
- #102 — a foreground "Перевірка .rdpkg / Імпортую…" progress notification remained beside terminal per-volume "готовий"; fix in v0.5.91 to remove foreground atomically and prevent stale reposts. History child result must stay.
- Notification tint: request green accent where supported, no guarantee of green system progress indicator on Samsung.
