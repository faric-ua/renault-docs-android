# Renault Docs v0.5.80 — safe archive intake

Status: CI / PHONE QA PENDING

- Deny already-generated .rdpkg under ZIP/7Z/RAR creation, including internal prepared dataset manifest in ZIP.
- Block explicit archive model mismatches Kangoo II/Megane II/Laguna II, but do not infer vehicles from opaque X61/NT codes.
- Exact SAF Document ID/name confirmation before destination, with possible NT matches across all registered ProjectStore projects. Metadata match does not prove identical binary files.
- Never move/delete original archives, generated packages, installed tomes. No automatic cross-project rerouting or storage-wide crawl.
- Build96; later Home-panel issue #79 separate.
