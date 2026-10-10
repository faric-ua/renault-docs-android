# Seven-operator dataSync budget matrix (v0.5.96 candidate)

| Operation | Service | Timeout terminal | Cooperative checkpoint |
|---|---|---|---|
| Legacy conversion | ConversionService | ConversionRunStore FAILED | progress, registration, completion |
| Native raw/ZIP/7Z/RAR | NativeRdpkgPreparationService | NativeRdpkgRunStore FAILED | isCancelled in engine/stager/importer; final COMPLETE guard |
| Catalog import | CatalogImportService | CatalogImportRunStore FAILED | per-item/download/import callback, importer extraction |
| RDPKG import | RdpkgImportService | RdpkgImportRunStore FAILED | importer ZIP byte read + progress, completion |
| RDPKG export | RdpkgExportService | RdpkgExportRunStore FAILED | progress callback and before complete |
| RDPKG share | RdpkgShareService | RdpkgShareRunStore FAILED | progress callback and before complete |
| RDPROJECT share | RdprojectShareService | RdprojectShareRunStore FAILED | progress callback and before complete |

Shared six-hour `dataSync` budget; the **wake lock** has its own timeout and is not an extension. No automatic replay after quota hit.
