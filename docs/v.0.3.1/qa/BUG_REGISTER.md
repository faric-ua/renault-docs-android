# v0.3.1 Bug Register

## PERF-002 — Legacy navigation still slow after v0.3.0

Real-phone finding.

Observed before v0.3.1:
- PDF works;
- PDF export works;
- overall speed did not materially improve.

Cause:
v0.3.0 Modern mode removed only the top-level catalog from WebView. Internal legacy pages still requested many individual resources through SAF.

v0.3.1 fix:
Fast Pack local ZIP runtime for non-PDF web resources.

Real-phone result on 2026-09-24:
- Fast Pack built successfully with 49,077 web files;
- archive size: 40,323,133 bytes;
- Modern screen reported Fast Pack ready;
- user reports navigation is now fast and responsive.

Status: PERF PASS.

## UI-003 — Modern volume tiles are not chronological

Observed:
top-level volumes are displayed in folder/name order rather than release-date order.

Fix target:
sort by normalized release date ascending in both package generation and Android runtime.

## PDF-004 — PDF +/- zoom does not change visible page size

Observed:
the controls request a different render width, but CSS keeps each page at 100% width, so only render resolution changes.

Fix target:
make zoom change the actual displayed page width, keep rerendering for sharpness, and show the current zoom percentage.
