# Renault Docs v0.5.80 — safe archive intake

Status: MERGED / MAIN CI PASS / SIGNED APK READY / PHONE QA PENDING

- PR #86 merged; runtime `5a6ddc060edb4ff452eb1e3e96c1423cc042ddf9`.
- Main Tests #563 PASS; Android Debug APK #143 PASS.
- Signed artifact `Renault-Docs-v0.5.80-Debug` (id `11557433214`) SHA256 `0da82672e308148e5a2178f9fb147705b5ab8f2b96c39da15220e09dde6223b6`.

- Deny already-generated .rdpkg under ZIP/7Z/RAR creation, including internal prepared dataset manifest in ZIP.
- Block explicit archive model mismatches Kangoo II/Megane II/Laguna II, but do not infer vehicles from opaque X61/NT codes.
- Exact SAF Document ID/name confirmation before destination, with possible NT matches across all registered ProjectStore projects. Metadata match does not prove identical binary files.
- Never move/delete original archives, generated packages, installed tomes. No automatic cross-project rerouting or storage-wide crawl.
- Build96; later Home-panel issue #79 separate.
