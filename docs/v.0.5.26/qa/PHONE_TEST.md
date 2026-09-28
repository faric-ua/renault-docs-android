# v0.5.26 Phone test — BUG-004 Classic catalog parity

## Prepare

1. Renault Menu → 5.
2. Renault Menu → 8.
3. Install v0.5.26.
4. Renault Menu → 9 — REQUIRED.
5. Wait for the full Fast/Modern package rebuild to finish.

## Gate A — representative non-3-digit entries

Open the same Laguna II volume where Classic showed the wider catalog.

In Modern search for several representative identifiers:
- 1405;
- R325;
- MAH;
- NT;
- NU.

Expected:
- each valid Classic entry that exists in that volume is discoverable in Modern;
- opening it loads its own section rather than a nearby 3-digit section;
- no crash from non-numeric codes.

## Gate B — source order

Clear search and compare a visible stretch of the Modern catalog against Classic.

Expected:
- Modern follows Classic navigation order;
- it is not numerically re-sorted;
- alphabetic/R-prefixed items remain in their Classic position.

## Gate C — ordinary section regression

Open 101 and one previously tested section such as 108.

Expected:
- existing native menu/rendering still works;
- connector/PDF/table behavior is unchanged.

## Gate D — duplicate-code safety

If a volume visibly contains the same display code more than once with different destinations/configurations, open both.

Expected:
- both entries exist;
- each opens its own legacy target/Runtime IR;
- one does not overwrite the other.

If no convenient real duplicate is available on phone, mark this gate NOT TESTED; automated contracts cover the index collision case.

## Gate E — search

Search by:
- code;
- title fragment.

Expected:
- numeric, alphabetic and mixed identifiers all remain searchable.

## Closeout

BUG-004 can close after representative real-phone parity confirms the regenerated dataset. Exact enumeration of every Classic identifier is not required if the converter contract and representative families pass.
