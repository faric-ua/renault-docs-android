# v0.5.81 Dialog flow and test diagram

```mermaid
flowchart TD
    A[Select source ZIP in Android SAF] --> B{ArchiveSourceGuard valid?}
    B -- No --> C[DialogUi HELP: reject source]
    B -- Yes --> D[Archive confirmation: compact summary]
    D --> E[Expand technical details]
    E --> D
    D --> F{Decision}
    F -- Cancel --> G[Clear pending selection; no job]
    F -- Continue --> H[Verify exact URI and name]
    H --> I[Choose destination]
    D -- Rotation --> J[Restore same preview and detail state]
    J --> D
```

Global styling wrapper:
`AlertDialog.Builder(...).create() → show() → DialogUi.apply(dialog, DialogRole.X)`.
This existing pattern is shared by 20 audited app-owned AlertDialogs. The source/decision state lives with each screen; styling never executes actions.
