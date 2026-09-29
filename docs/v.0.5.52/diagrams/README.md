# v0.5.52 dialog/status lifecycle

```mermaid
stateDiagram-v2
    [*] --> PREPARING
    PREPARING --> PREPARING: live state.message refresh every 750 ms
    PREPARING --> CANCELLED: user cancels
    PREPARING --> IMPORTING: preparation complete
    IMPORTING --> COMPLETE: import succeeds
    IMPORTING --> FAILED: import fails
    CANCELLED --> DISMISSED: tap ×
    COMPLETE --> DISMISSED: tap ×
    FAILED --> DISMISSED: tap ×
    DISMISSED --> DISMISSED: rotation / reopen
```

Contract:
- active PREPARING / IMPORTING: no terminal dismiss `×`;
- if the active progress dialog is open, its message follows persisted run state;
- terminal states use the separate dismissible status row;
- dismissal hides presentation only and is scoped to the exact finished run.
