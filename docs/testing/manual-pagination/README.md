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

Initial status: NOT_RUN. Exact-head CI and captures must be recorded after execution.
