# Technical Blue — Design Tokens

Status: approved concept values. Exact runtime values may be tuned after phone QA.

## Colors

| Token | Suggested value | Use |
| --- | --- | --- |
| `background-app` | `#0B1118` | app/window background |
| `surface-primary` | `#101822` | navigation and large dark surfaces |
| `surface-raised` | `#151F2A` | buttons/cards |
| `surface-selected` | `#0D4D89` | selected section row |
| `accent-primary` | `#1597FF` | outlines, active tab, highlights |
| `accent-soft` | `#5FB7FF` | icon/text highlight |
| `divider` | `#26384A` | list separators |
| `text-primary` | `#F4F7FA` | primary text |
| `text-secondary` | `#B1C0CF` | secondary metadata |
| `text-muted` | `#8397AA` | inactive labels |
| `document-paper` | `#FFFFFF` | PDF page |

## Typography

Use Android system font by default.

Suggested roles:

- App title: 20–22sp semibold/bold;
- section code: 17–19sp bold;
- section label: 16–18sp regular/medium;
- toolbar label: 14–16sp medium;
- metadata: 13–14sp;
- helper/status: 12–13sp.

Do not hardcode condensed fonts unless bundled/licensed later.

## Spacing

Base spacing unit: 4dp.

Common values:

- control gap: 4–6dp;
- toolbar padding: 4–8dp;
- row vertical padding: 10–14dp;
- row horizontal padding: 12–16dp;
- section list outer margin: 8–12dp;
- major block gap: 12–16dp.

## Sizes

- minimum primary touch target: 44dp;
- preferred toolbar control height: 42–46dp;
- compact icon control width: 42–46dp;
- zoom field width: enough for `200%` plus comfortable internal padding;
- Fit width control: text must never touch edges;
- list row target height: 58–72dp depending on icon use.

## Radius

- toolbar controls: 8–10dp;
- list cards/selected rows: 10–14dp;
- large panels: 12–16dp.

## Alignment contract

Every toolbar button must use true centered content.

Especially:
- zoom percentage;
- dropdown arrow;
- Fit width;
- Save PDF;
- plus/minus;
- page navigation.

Text must not be baseline-shifted relative to neighboring icon buttons.

## Selected states

Selected tab:
- accent text/icon;
- accent underline or border.

Selected list row:
- stronger blue surface;
- optional blue outline;
- white label;
- code remains prominent.

Focused input:
- accent outline;
- no ambiguous glow that hides text.

## Motion

Keep motion minimal:
- 120–220ms transitions;
- drawer/sheet slide;
- selected state fade;
- no decorative animation over technical content.
