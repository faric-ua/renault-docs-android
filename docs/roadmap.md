# Roadmap

## Phase 1 — Canonical content

- [x] Первинний аудит архіву.
- [x] Виявлення case-sensitive конфліктів.
- [x] Реалізувати deterministic converter.
- [x] Перевірити converter на повній копії Laguna II.
- [x] Додати unit tests і CI.
- [ ] Додати окремий post-conversion link checker/report як стабільний gate. *(read-only checker real-phone validated on full Laguna dataset: 264779 refs / 0 missing; blocking converter integration is next)*

## Phase 2 — Web baseline

- [x] Додати конфігурацію поточного Android-шляху.
- [x] Додати helper для створення normalized build.
- [x] Згенерувати normalized build безпосередньо на телефоні.
- [x] Додати local HTTP server.
- [x] Додати dataset manifest + entrypoint.
- [x] Перевірити базові frames/menu/navigation/GIF на реальному наборі.
- [x] Підтвердити Android Chrome PDF problem.
- [x] Реалізувати server-side PDF interception.
- [x] Додати локальний PDF.js browser viewer.
- [x] Зберегти legacy `#viewrect` як viewer input.
- [ ] Реальний phone test PDF viewer.
- [ ] Перевірити representative `#viewrect` на схемах.
- [ ] Зафіксувати browser compatibility report.

## Phase 3 — Universal dataset library

- [x] Додати `renault-dataset.json` contract.
- [x] Додати library scanner.
- [x] Laguna II оформити як перший universal dataset.
- [ ] Перевірити другий Renault dataset, щоб довести model-independent contract.
- [ ] Додати schema/version migration policy для manifest.

## Phase 4 — Android shell

- [ ] Створити Android Gradle project.
- [ ] Library screen + dataset tiles.
- [ ] SAF persistable folder access.
- [ ] Dataset validation/reconnect.
- [ ] WebView controlled local-origin layer.
- [ ] Android PDF layer.
- [ ] Back/forward navigation.
- [ ] Зберігати last opened state.
- [ ] Lifecycle/rotation phone QA.

## Phase 5 — Packaging

- [ ] Release documentation package.
- [ ] Signed GitHub Actions APK.
- [ ] apksigner/zipalign/aapt verification.
- [ ] SHA-256 artifact handoff.
- [ ] Real-phone release QA.
