# Renault Docs — Assistant Kit

Ця папка містить переносимі правила для безпечної Android-розробки.

Вона адаптована з перевіреного portable skeleton у YTM Importer, але правила тут уже є контрактами саме Renault Docs.

Основні файли:

- `SYSTEM_BEHAVIOR_CONTRACT.md` — lifecycle/navigation/modal/SAF/operations;
- `UI_CONTRACT.md` — tiles, screens, dialogs, actions;
- `APK_BUILD_CONTRACT.md` — build/sign/verify/artifact rules;
- `RELEASE_DOCUMENTATION_CONTRACT.md` — що має бути в кожному release;
- `TEST_DIAGRAM_STANDARD.md` — як документувати маршрути та phone QA;
- `CONTEXT_FILES.txt` — що нова сесія повинна прочитати перед змінами.

- `PROJECT_LEDGER.md` — canonical consolidated state: architecture, accepted phone behavior, completed work, open bugs, future work and release baseline.

Rule: chat history is not a project database. Any new architectural decision, phone finding, implementation, open task or status change must be written into the repository documentation in the same work package.

