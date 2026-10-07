# v0.5.69 Archive Intake flow

```mermaid
flowchart TD
    A[SAF archive: ZIP / 7Z / RAR] --> B[Read-only source]
    B --> C[Private staging]
    C --> D[Inspect archive entries]
    D --> E{All exact volumes already installed?}
    E -- yes --> F[Skip extraction / ALREADY_PRESENT]
    E -- no --> G[Safe extraction]
    G --> H[Discover Renault raw roots]
    H --> I{How many candidates?}
    I -- one new --> J[Prepare canonical .rdpkg]
    I -- multiple --> K[WAITING_SELECTION chooser]
    K --> L[Select one or more new volumes]
    L --> J
    J --> M[Existing Rdpkg validation/import]
    M --> N[Project upsert]
    N --> O[Cleanup private staging]
    F --> O
```

```mermaid
stateDiagram-v2
    [*] --> PREPARING
    PREPARING --> WAITING_SELECTION: multiple roots
    WAITING_SELECTION --> PREPARING: continue selected
    PREPARING --> COMPLETE
    PREPARING --> ALREADY_PRESENT
    PREPARING --> FAILED
    PREPARING --> CANCELLED
    WAITING_SELECTION --> CANCELLED
    COMPLETE --> [*]
    ALREADY_PRESENT --> [*]
    FAILED --> [*]
    CANCELLED --> [*]
```

Hard boundaries:
- source archive is never mutated;
- extraction writes only to app-private staging;
- output writes only to the explicit destination;
- Force Stop / device reboot remain hard lifecycle boundaries.
