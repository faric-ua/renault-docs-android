# Renault Docs v0.5.69 — Regression Checklist

- [ ] Install over v0.5.68 without clearing data.
- [ ] Existing projects/volumes remain present.
- [ ] Existing raw-folder → .rdpkg flow still opens normally.
- [ ] Archive action is visible and explicitly supports ZIP/7Z/RAR.
- [ ] Single-volume ZIP reaches canonical .rdpkg/import exactly once.
- [ ] Exact duplicate ZIP is skipped without duplicate install.
- [ ] Multi-volume archive shows explicit chooser; no silent first-volume selection.
- [ ] Installed candidates are disabled/unselected.
- [ ] One or multiple new candidates can be selected.
- [ ] Rotation while chooser is open restores state without re-extraction.
- [ ] Background + screen lock keeps active extraction/conversion alive.
- [ ] Cancel ends cleanly and does not install a partial volume.
- [ ] Source archive remains present and unchanged.
- [ ] Terminal success/cancel/failure does not leave private staging active.
- [ ] Existing Project/Home/Viewer navigation still opens normally.
