# Technical Blue — UX Contract

This document defines behavior that must survive visual refactors.

## Global top navigation

The document viewer must expose from the upper UI:

1. Back;
2. Home / main library;
3. Search;
4. Settings;
5. overflow/context actions.

No critical navigation should require scrolling to the bottom of a document.

## Classic mode contract

Classic is a compatibility mode.

Required:
- visible quick action: **Modern**;
- switching Classic → Modern must not require returning to Library first;
- preserve dataset and volume;
- preserve section when a mapping exists;
- otherwise land in the Modern section list for the same volume.

Classic must never silently change the user's default mode merely because the user used the Modern quick action once.

## Modern mode contract

Modern is the preferred native navigation.

Required:
- compact single-column section menu on phones;
- section code + human-readable title;
- current section visibly selected;
- Classic fallback remains accessible until legacy coverage is complete.

## Section migration contract

Legacy frames are migrated incrementally.

For each migrated section:
- native list owns navigation;
- legacy WebView is used only for still-unported content;
- Back returns to the same Modern parent context;
- rotation restores selected section;
- no automatic action should fire after rotation.

## Search contract

Phase 1: current page/document text search.

Search action:
- opens from top bar;
- autofocuses the query field;
- previous/next match controls;
- visible match count;
- close returns to exact previous document position.

Future search scopes must be explicit, not silently mixed.

## PDF contract

Upper PDF toolbar:
- page counter;
- previous/next;
- minus;
- editable percentage;
- presets;
- Fit width;
- plus;
- Save original PDF.

Fit width means:
- calculate width from available content frame;
- reset horizontal offset;
- keep page centered when narrower than viewport.

All labels centered.

## Menu contract

Phone menu presentation:
- compact one-column drawer or modal sheet;
- does not permanently reduce content width when closed.

Initial entries:
- Main menu;
- Search;
- Sections;
- PDF files;
- Settings;
- Modern/Classic switch where relevant.

## Accessibility

- meaningful content descriptions for icon-only controls;
- minimum 44dp touch targets;
- selected state cannot depend on color alone;
- text scaling should not clip button labels;
- all actions remain reachable in portrait.
