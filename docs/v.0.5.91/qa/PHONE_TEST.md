# Phone QA — PENDING

After CI and a verified stable-signed main APK installed **over existing Renault Docs** (do not uninstall/clear storage):

1. Check existing Megane II volumes remain including NT8299A.
2. During next legitimately needed archive, inspect "Розпаковую", "Готую дані", "Зберігаю індекси", "Пакую" and live numeric counter labels in landscape and portrait. Verify all descenders/text baselines fully visible with current system font size. If possible also test enlarged font scale safely.
3. Check terminal status SHA-256 is fully visible on multiple rows, progress is green, and copied text matches original without inserted line breaks inside digest.
4. After completion, only the independent swipeable "готовий" result remains; the live "Перевірка .rdpkg" notification disappears. Previously dismissed/overwritten Android notifications are not recoverable.
5. Check last-ten completed results grouping still works, older results tappable, no per-file notification spam.
6. On Samsung, note actual notification accent/progress line color. Android may ignore setColor on standard progress bar; this is not necessarily a feature failure if everything else is correct.

No repeated conversion of existing tomes solely for QA; issues #100/#102 remain open until device verification.
