# v0.5.90 — measured progress and semantic color

Phone finding (2026-10-09): during successful Megane II NT8275A preparation, status showed ZIP extraction count `3432 / 5357` but stage transitions sometimes showed no counter and an unmoving bar; terminal SHA-256 text ran off the landscape card. User requests stage-specific measured counts and green working/success, red errors.

Plan:
- Unmeasured stages use visible **green indeterminate activity** rather than a static zero bar. Never fake progress percentage.
- Measured stage counters reset to 0/N when known and are driven by real callbacks, not carried across stages.
- Runtime IR compiling sections exposes per-section count; Fast Pack exposes actual processed/total source files.
- Extraction and archive-copy retain real existing progress; eliminate conflicting unstructured status writes.
- Terminal status gets semantic success/failed/cancelled color and wraps long SHA display without mutating copied text.
- Use the same thin bar contract in Project/Home/Drive/Converter; preserve data, navigation and lifecycle.

No APK or device acceptance until exact CI plus signed main build and user phone QA.
