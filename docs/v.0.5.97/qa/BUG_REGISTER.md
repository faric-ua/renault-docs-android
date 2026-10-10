# v0.5.97 source safety — issue tracking

- #82: explicit archive model guard implemented in v0.5.80; **raw SAF folder guard added in v0.5.97 candidate**, phone QA pending. User’s existing misclassified Kangoo/Megane result preserved unchanged.
- #85: .rdpkg as archive source blocked with source name and document ID preflight since v0.5.80; real-phone negative test not claimed PASS.
- #83: historical Android SAF navigation path cannot be proven from stored URI; do not assert user picked the wrong folder.
- #51: broader archive ZIP/7Z/RAR, cancel and partial results remain independent.
- #40: dataSync 6-hour quota emulator callback still not run; user has no computer and has deferred emulator QA.
- #102 and #123: per-volume result notifications / complete reports depend on future *legitimate* new work, no forced imports.
- #133: Google-account allowlist, one-time Play purchase, encrypted docs remain separate future TODO; not in this release.
