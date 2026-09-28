# Renault Docs — Classic vs Modern architecture

This document compares the current Classic runtime with the current Modern implementation.

## Side-by-side architecture

```mermaid
flowchart LR
    subgraph CLASSIC[CLASSIC]
        direction TB
        C1[Open volume]
        C2[INDEX.HTM]
        C3[ENTREE.HTM]
        C4[org / CODE.HTM<br/>section list]
        C5[menu]
        C6[nav]
        C7[doc / PDF]

        C1 --> C2
        C2 --> C3
        C3 --> C4
        C4 --> C5
        C4 --> C6
        C4 --> C7
    end

    subgraph MODERN[MODERN — CURRENT]
        direction TB
        M1[Native catalog]
        M2[modern-sections.json]
        M3[ViewerActivity]
        M4[INDEX.HTM]
        M5[ENTREE.HTM]
        M6[hidden org / CODE.HTM]
        M7[menu]
        M8[nav]
        M9[doc / PDF]
        M10[Native 'Розділи' search panel]

        M1 --> M2
        M2 --> M3
        M3 --> M4
        M4 --> M5
        M5 --> M6
        M6 --> M7
        M6 --> M8
        M6 --> M9
        M10 --> M6
    end

    C4 -. same legacy section mechanism .-> M6
```

## Functional comparison

| Area | Classic | Modern now |
|---|---|---|
| Volume entry | Legacy HTML | Native Android |
| Section list | `org / CODE.HTM` | `modern-sections.json` |
| User section navigation | Visible Classic left panel | Native `Розділи` panel |
| Runtime section switch | Click in `org` | Android triggers hidden `org` |
| Main runtime | Full Classic frameset | Still full Classic frameset |
| `titre` | Visible | Loaded but projected to width 0 |
| `org` | Visible | Loaded but projected to width 0 |
| `menu` | Visible | Visible |
| `nav` | Visible | Visible |
| `doc` | Visible | Visible |
| PDF | Legacy target + Android PDF handling | Same |
| Top-level reload for live section switch | No | No in v0.5.7 |
| Dependency on legacy frame JS | High | Still high |

## What is actually different today

```mermaid
flowchart TD
    DATA[Same Renault dataset] --> CLASSICUI[Classic UI]
    DATA --> MODERNUI[Modern native UI]

    CLASSICUI --> LEGACY[Full legacy frameset]
    MODERNUI --> LEGACY2[Full legacy frameset]

    LEGACY --> VISIBLE[All Classic frames visible]

    LEGACY2 --> PROJECT[Projection / control layer]
    PROJECT --> HIDDEN[Hide titre + org]
    PROJECT --> WORKING[Keep menu + nav + doc]
```

The current Modern branch changes **presentation and navigation**, but does not yet replace the legacy runtime core.

## Proposed target architecture

The cleaner long-term direction is to move structural knowledge into conversion output.

```mermaid
flowchart TD
    SRC[Original Renault dataset] --> CONV[Converter / packager]

    CONV --> TREE[runtime-tree.json]
    CONV --> LEGACYFILES[Required legacy section assets]

    APP[Native Modern UI] --> TREE
    APP --> SHELL[One generic runtime-shell]

    TREE --> SHELL

    SHELL --> MENU[menu legacy component]
    SHELL --> NAV[nav legacy component]
    SHELL --> DOC[doc / PDF]

    MENU --> DOC
    NAV --> DOC
```

The target is to stop launching these layers for Modern:

```text
INDEX.HTM
ENTREE.HTM
CTITRE.HTM
CODE.HTM
Classic left frameset
```

Their useful information would instead be normalized during conversion into one manifest, for example:

```json
{
  "volumes": [
    {
      "id": "NT8183A",
      "runtime": {
        "menuFrame": "menu",
        "navFrame": "nav",
        "docFrame": "doc"
      },
      "sections": [
        {
          "code": "101",
          "title": "ПРИКУРИВАТЕЛЬ",
          "menu": "RUS/HTM/MENU/101.HTM",
          "nav": "COMMUN/HTM/PC/101.HTM"
        }
      ]
    }
  ]
}
```

## Migration strategy

```mermaid
flowchart LR
    A[Current v0.5.7] --> B[Measure real startup/readiness]
    B --> C[Converter generates richer runtime manifest]
    C --> D[Generic Modern runtime shell]
    D --> E[Remove INDEX / ENTREE / titre / org from Modern startup]
    E --> F[Gradually replace remaining legacy menu/nav logic if useful]
```

This keeps the proven Classic implementation intact while Modern is migrated component by component instead of attempting one large rewrite.
