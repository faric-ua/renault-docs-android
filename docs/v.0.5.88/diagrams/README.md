# Shared progress presentation flow

```mermaid
flowchart TD
  A[Operation worker] --> B[Structured OperationProgress]
  B --> C[Persistent run store]
  C --> D{App-owned UI surface}
  D --> E[OperationStatusView on Home Project Drive]
  D --> F[Converter]
  E --> G[SharedOperationProgressBar]
  F --> G
  E --> H[Stable stage and count slots]
  C --> I[Lifecycle reattachment reads run state]
  I --> D
```

Status rendering never owns or restarts a worker. File counters must be sourced from measured progress, and operation completion remains separate from the live view.
