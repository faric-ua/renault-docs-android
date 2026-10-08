# Renault Docs — Release Documentation Contract

## Public GitHub Release of verified signed APK

Use `docs/assistant-kit/GITHUB_RELEASES_CONTRACT.md`: publish only from an explicit reviewed version promotion file `docs/release-promotions/vX.Y.Z.json` and its pinned, successful developer-signed main run. Never auto-publish on every main APK build, never substitute a PR-signed build, never overwrite an existing release. Public debug prerelease assets contain **only APK + .sha256**; installed application remains functional when the Actions artifact expires. Phone-QA acceptance is tracked independently.

---

Документаційний skeleton створюється **до першої feature-code зміни релізу**.

```text
docs/v.X.Y.Z/
├── RELEASE_META.json
├── RELEASE.md
├── REGRESSION_CHECKLIST.md
├── diagrams/
│   └── README.md
└── qa/
    ├── BUG_REGISTER.md
    ├── PHONE_TEST.md
    └── EVIDENCE_MANIFEST.md
```

Після виконаного phone QA додаються:

```text
qa/TEST_RUN_YYYY-MM-DD.md
qa/PHONE_TEST_REPORT_YYYY-MM-DD.md
```

## RELEASE_META

Машинозчитувані поля:
- versionName;
- versionCode;
- phase: planned/development/final;
- feature;
- branch;
- tested source SHA;
- signed Actions run;
- QA status;
- release tag;
- optional checkpoint;
- phone-test date.

Secrets не записувати.

## Якщо змінюється system behavior

Для змін:
- rotation;
- navigation;
- dialogs/help/result;
- progress;
- long operations;
- SAF;
- PDF lifecycle;
- updater;
- themes;

оновлюється reusable contract/audit і додається реальна flow/test diagram.

## Evidence

Не вигадувати screenshot/video evidence.
Відсутній старий артефакт — це gap, а не reconstructed proof.

## Final closeout

Перед final release перевірити:
- exact tested app source;
- exact signed run;
- versionName/versionCode;
- tag;
- checksum;
- regression checklist;
- executed phone test/report;
- known bugs;
- handoff/status;
- diagrams for changed system flows.

## Canonical project ledger

Every release/change package must also consider:

`docs/assistant-kit/PROJECT_LEDGER.md`

Update it whenever the change affects:
- architecture;
- converter/data contracts;
- accepted phone behavior/UI;
- Classic/Modern parity;
- workflow/tooling;
- open/pending work;
- project direction;
- release baseline.

A change is not considered fully documented if its only record is the chat, commit message, or PR description.

For every real-phone finding:
1. record the finding before it can be forgotten;
2. assign/update a status in `OPEN_FINDINGS.md` when appropriate;
3. update `PROJECT_LEDGER.md` if it changes the long-term contract or pending work;
4. update phone QA evidence/report when validated.

For every implementation:
1. release notes/metadata;
2. handoff/checkpoint;
3. relevant finding status;
4. ledger if architecture/product state changed;
5. CI/artifact/checksum after successful build.



## Active plan closeout rule

`ACTIVE_PLAN.md` must remain synchronized with actual project progress.

For every successful project-progress step:
1. mark the verified checkbox complete;
2. update release/QA/finding evidence when applicable;
3. update `CURRENT_HANDOFF.md` when the resume point changes;
4. leave the next action as the first unchecked item.

A release/change is not fully handed off if code/QA advanced but the active plan still points to an already completed step.
