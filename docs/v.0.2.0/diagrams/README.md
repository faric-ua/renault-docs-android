# v0.2.0 Flow / Test Diagram

```mermaid
flowchart TD
    A[Library] -->|Tap Convert| B[Converter screen]
    B -->|Choose source| C[Android SAF source picker]
    C -->|Cancel| B
    C -->|Selected| D[Persist source URI]
    D --> B
    B -->|Choose destination| E[Android SAF destination picker]
    E -->|Cancel| B
    E -->|Selected| F[Persist destination URI]
    F --> B
    B --> G{Both selected?}
    G -- No --> B
    G -- Yes --> H[Validate conversion plan]
    H -->|same URI| I[Show local validation error]
    H -->|valid| J[Ready state: no copy yet in v0.2.0]

    B -->|Rotate| K[Activity recreation]
    K --> L[Restore semantic draft]
    L --> B

    B -->|Back| A
```

Critical lifecycle invariant:

```text
selected source/destination
        ↓ rotation
Activity recreation
        ↓
same draft restored
        ↓
NO automatic picker launch
        ↓
NO conversion start
```
