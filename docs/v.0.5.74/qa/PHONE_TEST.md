# v0.5.74 Phone Test

Status: **PENDING**

Visual gate:
1. open Megane II with Add collapsed;
2. confirm pin is neutral/monochrome and chevron is heavier;
3. expand Add;
4. confirm no tap/long-press guidance line exists;
5. cancel an Auto .rdpkg picker;
6. confirm `Імпорт .rdpkg скасовано.` appears inside a subtle warm status card at the bottom of Add, not as loose text between Add and the volume list.

Pin gate:
1. pin while expanded;
2. pin icon becomes red;
3. leave/reopen Project; panel remains expanded;
4. unpin; icon returns neutral; collapse works.

Rotation gate:
- unpinned expanded state survives rotation.

Operation gate:
- start a safe operation and confirm progress remains visible outside Add even if Add is collapsed.
