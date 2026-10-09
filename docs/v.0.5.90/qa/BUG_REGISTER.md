# Open finding — 2026-10-09

User device evidence in chat: NT8275A/333 native successful creation, Megane II volume count 12 → 13. Running extraction count 3432/5357; intermittent empty counter and stationary bar on other phases. Terminal SHA-256 long line visually clipped. Code candidate and device QA pending.

- New user finding: on switching away from the RDPKG import/extraction stage, its bar was still partially filled (last throttled update). Root cause in `RdpkgImporter.extract`: no guaranteed final typed progress after ZIP EOF; unread ZIP central directory may also prevent byte-weighted progress reaching 100%. v0.5.90 candidate emits a trustworthy N/N after EOF and shows extracted-count summary while separate validation/install phases remain busy. Phone QA pending.
