# v0.5.73 Phone Test — compact Add panel

Status: **PENDING**

Primary visual gate:
1. open Megane II;
2. confirm only one compact full-width `Додати` header is visible before the volume list;
3. expand it and verify Auto / Manual / raw / archive appear;
4. collapse it and verify the volume list moves upward.

Pin gate:
1. expand;
2. pin;
3. leave and reopen the Project;
4. panel must reopen expanded;
5. unpin, collapse, leave and reopen;
6. unpinned panel must return compact/collapsed.

Rotation gate:
- while unpinned and expanded, rotate; current expansion state must survive recreation.

Operation gate:
- start a safe add/create flow and confirm operation/progress status is not hidden by collapsing Add.
