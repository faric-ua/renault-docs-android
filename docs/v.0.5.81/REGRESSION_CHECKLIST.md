# v0.5.81 Regression Checklist

## Automated
- [x] PR #87 Python Tests #567 PASS.
- [x] PR #87 Android PR Check #453 PASS (unit tests / build using PR signer).
- [x] Main Python Tests #568 PASS.
- [ ] Main signed Android Debug APK #144 CI success and digest recorded.

## On device — pending
- [ ] Install build97 **over** existing installation with stable signer; no uninstall/clear.
- [ ] All app-owned dialogs share canonical surface/title/button hierarchy (Project, Home, Settings, Help, Viewer).
- [ ] Destructive confirmation remains red and does not act without explicit positive tap.
- [ ] Archive preview summary concise; expandable provider/Document ID; scrolls on small screen.
- [ ] Expanded preview survives portrait/landscape rotation with same selection and no auto-run.
- [ ] Cancel preview leaves previously saved diagnostics intact; no native preparation started.
- [ ] Viewer navigator / frame debug dialogs retain original search/copy/close semantics.
- [ ] Original .rdpkg rejection and wrong-model archive block from v0.5.80 remain intact.
- [ ] Inspect keyboard behavior under modal and log separately if still present.
- [ ] No original ZIP, generated packages, installed volumes automatically modified.

## Known boundaries
Current-project duplicate hit (NT8341A) is phone-observed; a true different-project duplicate is not yet phone-observed. Matching NT is metadata similarity, not byte identity. SAF system picker is not an app-owned dialog.
