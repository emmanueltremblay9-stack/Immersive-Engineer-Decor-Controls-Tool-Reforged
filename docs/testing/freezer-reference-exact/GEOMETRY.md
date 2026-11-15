# Freezer V5 independently measured geometry

Local development experiment only. V3 coordinates were not copied unchanged.
Source: freezer-mixed-rp-v5-preview.png, 1254×1254 RGB, unchanged bytes.

The central dark-pixel runs were inspected on V5 rows 790, 890, 985 and 1120
and inventory column centers. Conservative interiors deliberately exclude bevels:
- Machine menu 0: (154,502,130,120), item (47,133).
- Machine menu 2: (964,502,130,120), item (250,133).
- Inventory source x: 164,270,376,482,588,694,800,906,1012.
- Source y: 758,854,952,1082; width 80, height 72.
- Item x: 43,70,96,123,149,176,202,229,255.
- Item y: 191,215,239,272.

These are conservative subsets, not assertions of exact semantic bevel boundaries.
All their pixels have max RGB <=60, independently checked against the untouched PNG.
The item origin is Math.round(sourceCenter * 314/1254 - 8). All 38 vanilla
16×16 item boxes fit. Padding of ordinary 18×18 hover envelopes exceeds all
36 inventory subsets. The inherited half-open clipping intersects the envelope
with the subset while retaining each full item. No items or hover regions overlap.
The row-2 bottom is 926*314/1254 = 231.8692185 local units: 231.8691 is inside,
231.8693 and 231.99 outside. Floored 231 would incorrectly appear inside, so raw
mouse coordinates remain authoritative for hover while rendering uses floor.

The inherited full-image uniform 314-square canvas fits the viewport with 6-pixel
margins, scale <=1, no crop/reflow/stretch. Tiny positive viewports relax only the
margin to keep one pixel; zero/negative sizes are rejected. At 320×240 the image
is 228-square and a 16-pixel item displays at 11.62 GUI pixels. This may be too
small for readability and requires actual client review. No global GUI-scale change.
All click/release/move/scroll pointer inputs use the inverse; drag deltas scale;
wheel units do not. Vanilla slot drawing, overlays, tooltips and packets remain.

Run bash tools/validation/check_freezer_reference_geometry.sh offline with Java21.
It compiles only dependency-free geometry/checks, no Gradle or Minecraft.
Six groups cover mapping, item/hover containment, 703 pairs, half-open boundaries,
307200 viewport sizes, extreme aspect ratios, inverse/delta transforms, negative
floor, raw-hover parity, immutable lists, invalid arguments and PNG SHA256.
JUnit wrapper exists but was not executed. No mapped compilation or native capture.
The manifest records all 38 rectangles and required native capture conditions.
