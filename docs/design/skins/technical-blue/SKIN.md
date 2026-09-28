# Technical Blue

Skin ID: `technical-blue`

Status: **approved-concept**

Target: Renault Docs document viewer and future native replacement for legacy Renault frame navigation.

## Visual direction

Technical Blue is a dark, high-contrast technical-documentation skin with:

- near-black/navy application background;
- dark elevated control surfaces;
- Renault-style technical blue accents;
- thin blue outlines for active controls;
- white primary text and muted steel-blue secondary text;
- compact one-column navigation;
- rectangular controls with moderate corner radius;
- strong selected state without excessive gradients.

The selected concept is the tabbed/one-column version with the document content below the navigation block on narrow phone screens.

## Selected screen structure

### Top app bar

Persistent controls:

- Back;
- document/volume title;
- Home;
- Search;
- Settings;
- overflow menu.

All primary navigation controls should live in the upper area of the screen.

### Document mode row

Preferred native sections:

- Sections;
- Illustrations;
- PDF;
- Contents.

Not every Renault volume must expose every item. Missing capabilities should be hidden, not shown disabled without explanation.

### Native section list

Legacy frame menus such as:

- 101;
- 103;
- 105;
- 107;
- 119;
- 123;
- etc.

should migrate to a compact single-column native list.

Each row contains:

- optional category icon;
- section code;
- localized/normalized label where known;
- navigation chevron;
- clear selected state.

The list should be searchable/filterable later.

### PDF toolbar

The toolbar remains in the upper content region.

Controls:

- current page / total pages;
- previous page;
- next page;
- minus;
- editable zoom percentage;
- zoom preset dropdown;
- Fit width;
- plus;
- Save original PDF.

All button labels and glyphs must be visually centered both vertically and horizontally.

## Classic ↔ Modern bridge

Classic remains a fallback, not a dead end.

When Classic is active:

- header must expose a fast **Modern** action;
- returning to Modern should preserve the same dataset/volume whenever possible;
- once section mapping exists, the bridge should preserve the same section/topic;
- if an exact Modern destination does not exist yet, open the same volume in Modern at its native section list.

When Modern is active:

- a Classic fallback remains available through the document menu/overflow while legacy content is still required.

## Search

Top-bar Search is a first-class action.

Initial scope:
- search text in the currently displayed HTML/PDF page when technically available.

Later scope:
- current section;
- current volume;
- all volumes/datasets.

Search UI should open as an inline top search field or compact overlay without replacing the document context.

## Main menu

Home returns to the Renault Docs library/main menu.

The main document menu should eventually expose:

- Main menu;
- Search;
- Sections;
- PDF files;
- Settings;
- Modern / Classic switch when relevant.

On phones this menu should be a compact drawer/sheet or one-column panel, not a permanent desktop sidebar.

## Responsive layout

### Narrow phones

Preferred:
1. top app bar;
2. mode/tabs;
3. one-column section list or current navigation panel;
4. PDF/content controls;
5. document content.

No permanent left sidebar should consume half the screen.

### Wide phones / tablets / landscape

A collapsible left navigation pane is allowed:
- nav list on the left;
- active content on the right;
- same control semantics as phone layout.

## Implementation priority

1. polish existing PDF toolbar alignment;
2. add Classic → Modern quick switch;
3. add persistent top navigation actions;
4. implement page text search;
5. replace legacy 101/103/105/... frame navigation with native one-column sections;
6. move remaining legacy frames behind fallback paths;
7. later expose Technical Blue as a selectable skin after skin infrastructure exists.
