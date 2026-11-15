# Milking manual native pagination

This PR182 follow-up qualifies the longer EN/FR Milking text using IE's real
ManualScreen, native pagination, current font renderer, crafting special and the
ordinary packaged IEDCTR JAR. The separate opt-in harness is not distributed.

Profiles: 1280×720 GUI scale 2 and 1280×480 GUI scale 1, each in en_us/fr_fr.
Every page in the individual entry and the Milking interval of workshop_machines
is traversed using IE's next/previous scroll handlers. Captures are taken after
native screen rendering. Source text completeness, line widths, text-plus-special
height, viewport containment and crafting special presence fail closed.

The native result records source SHA, nonce, exact JAR/resource/class identities,
all rendered lines and page intervals, and screenshot hashes. The Python verifier
rechecks identities and page coverage. A fresh screenshot review is required;
passing counts are not themselves visual acceptance. This is not OS/GLFW input,
all-manual qualification, or coverage of other locales.

Run:
```
python tools/validation/manual_packaged_native.py --language en_us --height 720 --scale 2
```
Use a clean committed source and Linux Java/Xvfb dependencies. The workflow shares
one production artifact among all four profiles and verifies its bytes again.
No resource pack substitution, user world, dedicated server or production install.

## Observed qualification, 2026-11-13

Run `43470334855`, source `e22682addb31698e92dd42eaf89a8746011afeb7`:
ordinary JAR SHA-256 `75d6a77f0386d8beb6381e31780062d421a11e7efa6f869bd135f44c64c56b51`.
EN and FR 1280×480 passed. All 21 screenshots were reviewed: EN 5 individual +
5 category pages; FR 5 individual + 6 category pages. Recipe and body are visible;
forward/back navigation and source completeness passed. FR category subtitle is
very wide and the last category page contains only the closing sentence fragment.

1280×720 failed in both locales with the upstream IE default `manualGuiScale=4`.
IE's Unicode-even scaling selects a 320×180 internal viewport for a 186×198 book,
placing its top at -9. The book occupies 744×792 physical pixels and clips 36px
above/below the 720px window. This is NOT a successful full-profile qualification.

Diagnostic follow-up `55b98ab1f9cf274b7a283bd825717e4b0eecc63d`, run `43471672628`,
preserves a screenshot even on failure. The EN 720 capture confirms the clipped
book; reported geometry is left67/top-9, factor2, GUI viewport640×360. The 480
profiles passed again. No assertion or production/config behavior was relaxed.

The branch is being reconciled with main after PR181/174; earlier exact-head
results do not qualify that later merge. Keep this PR draft pending exact-head
checks and resolution of the native 720 default-scale limitation.
