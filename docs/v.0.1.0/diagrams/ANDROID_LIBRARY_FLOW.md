# Android Library Flow — v0.1.0

```mermaid
flowchart TD
    A[Launch app] --> B[Library]
    B --> C[Add dataset]
    C --> D[Android ACTION_OPEN_DOCUMENT_TREE]
    D -->|Cancel| B
    D -->|Folder selected| E[Persist read URI permission]
    E --> F[Read renault-dataset.json]
    F -->|Invalid| G[Show error state]
    G --> B
    F -->|Valid| H[Store dataset registration]
    H --> I[Render dataset tile]
    I --> J[Tap tile]
    J --> K[Viewer placeholder]
    K -->|Back| B
```

## Rotation contract

```text
Library / Viewer
      ↓ rotate
Activity recreation
      ↓
same semantic screen
      ↓
NO automatic SAF launch
NO duplicate registration
```
