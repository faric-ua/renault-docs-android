# Technical Blue — Implementation Mapping

Status: planned.

## Separation

Do not implement a skin by duplicating whole Activities.

Preferred model:

- behavior/state in existing Activities/controllers;
- reusable UI components;
- a `SkinTokens`/theme abstraction for visual values;
- layout variants selected independently where practical.

## Suggested runtime model

Future conceptual API:

```kotlin
enum class SkinId {
    TECHNICAL_BLUE,
}

data class SkinTokens(
    val backgroundApp: Int,
    val surfacePrimary: Int,
    val surfaceRaised: Int,
    val surfaceSelected: Int,
    val accentPrimary: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val divider: Int,
    val controlHeightDp: Int,
    val controlRadiusDp: Int,
)
```

This is a direction, not a requirement to introduce exactly these classes in one commit.

## Components to extract progressively

- AppTopBar;
- DocumentModeTabs;
- SectionRow;
- DocumentMenuSheet;
- PdfToolbar;
- SearchBar;
- ClassicModernSwitch.

## Preference storage

When skins become runtime-selectable:
- store only stable skin ID;
- fallback to default if an old/removed skin ID is found;
- do not persist raw colors in SharedPreferences.

Suggested default initially:
`technical-blue` after implementation and phone approval.

## Design artifact status

Raster concepts are references.
Before pixel-sensitive implementation, maintain at least one canonical SVG or code mockup for each approved screen.

## QA gates for a skin

Before status changes to `implemented`:

- portrait phone;
- landscape phone;
- large text/font scale;
- dark system mode;
- PDF controls;
- Classic → Modern;
- Modern → Classic fallback;
- rotation;
- Back;
- Search;
- Settings;
- Fast Pack performance unchanged.
