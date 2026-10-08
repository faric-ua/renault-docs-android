# v0.5.81 Regression Checklist

## Automated
- [x] PR #87 Python Tests #567 PASS.
- [x] PR #87 Android PR Check #453 PASS (unit tests / build using PR signer).
- [x] Main Python Tests #568 PASS.
- [x] Main signed Android Debug APK #144 PASS; artifact ID 11560786817, SHA256 5711b9b6181edd4edaa53a575bc23fa13186a68aea925544de71b2b78b1afe79.

## On device — pending
- [ ] Install build97 **over** existing installation with stable signer; no uninstall/clear.
- [ ] All app-owned dialogs share canonical surface/title/button hierarchy (Project, Home, Settings, Help, Viewer).
- [ ] Destructive confirmation remains red and does not act without explicit positive tap.
- [ ] Compact confirmation and detail expansion verbally accepted (`++`, `Пасс`); long text and small-screen scrolling not separately checked.
- [x] Preview with technical details remains open across rotation, no auto-run — user PHONE PASS (2026-10-08).
- [ ] Cancel preview: **no native preparation started — user PHONE PASS**; preservation of previously saved diagnostics not separately checked.
- [ ] Viewer navigator / frame debug dialogs retain original search/copy/close semantics.
- [ ] Original .rdpkg rejection and wrong-model archive block from v0.5.80 remain intact.
- [ ] Inspect keyboard behavior under modal and log separately if still present.
- [ ] No original ZIP, generated packages, installed volumes automatically modified.

## Known boundaries
Current-project duplicate hit (NT8341A) is phone-observed; a true different-project duplicate is not yet phone-observed. Matching NT is metadata similarity, not byte identity. SAF system picker is not an app-owned dialog.
