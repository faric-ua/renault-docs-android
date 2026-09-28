# v0.5.15 flow diagrams

## Structured table render/export

```text
Runtime IR structured-html
        ↓
structuredTables()
        ↓
NativeTableData
        ↓
NativeTableLayout.columnFractions()
        ├─ native Android grid
        └─ NativeTablePdfExporter
              ↓
        ACTION_CREATE_DOCUMENT
              ↓
        saved PDF
```

## Connector composite

```text
Runtime IR composite-document
        ├─ dessin → PDF
        └─ alveoles → structured-html

Modern connector menu
        ├─ Схема + піни розʼєма
        │      ↓
        │  original composite path
        │      ↓
        │  app ViewerActivity
        │
        ├─ Схема розʼєму
        │      ↓
        │    PDF
        │
        └─ Опис контактів
               ↓
           native table
```

The combined route is intentionally a narrow legacy-composite bridge, not a return to full Classic navigation.
