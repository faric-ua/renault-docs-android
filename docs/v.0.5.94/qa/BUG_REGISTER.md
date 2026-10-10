# #118 — nested compressed archives were not recognized

The user's screenshot proves `Megane IIx.zip` has three compressed ZIP members (NT8266, NT8228A, NT8274); each contains a folder with `index.html` one level deeper. Outer-only raw-path scanning found no `INDEX/ACCUEIL` and terminal returned "В архіві не знайдено raw Renault тому …". Candidate v0.5.94 expands one nested ZIP level safely with explicit bounds and multi-root chooser, preserving source/installed files. CI and phone QA PENDING.
