# Renault Docs — Assistant Context Index

Це канонічна карта контексту для нової сесії/асистента.

Мета: проєкт повинен відновлюватися з репозиторію без залежності від старого чату.

## Перед змінами коду

1. Прочитати `CURRENT_HANDOFF.md`.
2. Прочитати `ACTIVE_PLAN.md` і взяти перший невиконаний checkbox як default resume point.
3. Прочитати цей файл.
4. Прочитати всі шляхи з `docs/assistant-kit/CONTEXT_FILES.txt`.
5. Перевірити живу Git-гілку та HEAD.
6. Подивитися точні файли, яких стосується задача.
7. Запустити релевантні тести/аудити перед merge.

## Джерела істини

Коли джерела суперечать одне одному:

1. реальна поведінка на телефоні;
2. перевірений signed build / CI evidence;
3. поточний код GitHub;
4. поточні contracts/status docs;
5. історичні release docs;
6. пам'ять старого чату.

## Основні контракти

- `docs/assistant-kit/SYSTEM_BEHAVIOR_CONTRACT.md`
- `docs/assistant-kit/UI_CONTRACT.md`
- `docs/assistant-kit/APK_BUILD_CONTRACT.md`
- `docs/assistant-kit/RELEASE_DOCUMENTATION_CONTRACT.md`
- `docs/assistant-kit/TEST_DIAGRAM_STANDARD.md`

## Dataset / library

- `docs/architecture.md`
- `docs/ua/LIBRARY.md`
- `core/dataset_manifest.py`
- `core/library.py`

## Головний принцип

Конвертер і datasets є окремими від Android UI.

Android-застосунок — універсальна бібліотека/viewer, а не Laguna-specific програма.


## Живий план / crash recovery

`ACTIVE_PLAN.md` — обов'язковий mutable TODO-план.

- перед багатокроковою feature/release/research/QA роботою оновити план;
- після кожного успішного кроку проєктного прогресу одразу відмітити лише реально підтверджений checkbox;
- після phone PASS/FAIL разом оновити plan + QA/finding/handoff;
- перший невиконаний checkbox — точка продовження після втрати чату;
- не відмічати крок завершеним лише через code/CI без потрібного phone evidence;
- при зміні scope переписати майбутні невиконані кроки, а не залишати застарілий план.

Чисті консультаційні відповіді без зміни стану проєкту не потребують окремого commit.
