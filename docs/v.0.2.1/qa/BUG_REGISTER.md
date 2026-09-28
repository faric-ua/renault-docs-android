# v0.2.1 Bug Register

## FIX-001 — Dataset tile opens placeholder instead of documentation

Observed on real phone in v0.2.0.

Actual:
- Library discovers Renault Laguna II 2001–2006;
- manifest volume count = 10;
- tapping dataset opens placeholder text with entrypoint and SAF URI;
- no catalog/volume HTML is rendered.

Root cause:
`ViewerActivity` was intentionally a placeholder and had no WebView/Saf resource bridge.

Expected:
tap dataset → render `catalog_entrypoint` → tap volume → render legacy documentation.


## PHONE-001 — Core SAF WebView path confirmed

Real-phone v0.2.1 evidence confirms:
- generated catalog renders 10 volumes;
- NT8236 legacy frame/menu/image content renders;
- PDF navigation resolves to the controlled viewer.

The placeholder-viewer defect is therefore fixed for the tested HTML path.

Still not a full v0.2.1 release PASS:
- Back/rotation coverage was not completed;
- PDF rendering was intentionally not implemented in v0.2.1;
- user reported noticeable viewer latency, tracked as v0.2.2 PERF-001.
