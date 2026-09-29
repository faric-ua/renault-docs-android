# v0.5.51 Phone Test Report — 2026-09-28

Final result: **PASS**

Device test target:
Renault Docs v0.5.51 candidate.

Reference source:
`NT8340A · 2006-04-18`.

## End-to-end result

Android/Kotlin successfully converted the raw volume to a final `.rdpkg`, validated/imported it, updated the existing project volume, and reopened it natively.

Metrics:
- raw source files: `7653`;
- Runtime IR sections: `347`;
- Fast Pack files: `6138`;
- outer package files: `8012`;
- final package SHA-256:
  `7d6cc758b329adcd1ef46680de4d9c6815ff48b053f4e3550b06dd92b229b77e`.

A second successful run produced the same package SHA-256.

## Lifecycle result

PASS.

Verified:
- post-completion rotation/app switching does not replay the old request;
- active PREPARING survives rotation/background/external-app handoff;
- same copy operation progressed `500/7653 → 1400/7653 → 2300/7653`;
- no second conversion starts;
- Cancel during PREPARING leaves source unchanged and cleans private staging.

## Import / reopen result

PASS.

Verified:
- automatic `RdpkgImporter.install()`;
- no duplicate NT8340A;
- project volume count stays at 2;
- reopen title `NT8340A · 2006-04-18`;
- Modern `347 · native`;
- no compatibility fallback;
- Classic remains available explicitly.

## Storage result

PASS for the current Kotlin-native v0.5.51 flow.

No new public NT8340A/current-flow `*_android` intermediate was created.

Existing historical `*_android` folders are outside this release gate and require separate audit before cleanup.

## Verdict

**v0.5.51 PHONE PASS.**
