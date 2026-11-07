# Factory Placer V2 native validation

This is a bounded, opt-in integrated-client fixture, not a report of tests already run.
A passing source test does not establish a passing Minecraft run. The exact-head CI
artifacts and native captures must be reviewed before making that claim.

## Isolation and build boundaries

The fixture lives in `src/factoryPlacerNativeValidation/java`, outside `main`.
Only `-Pinclude_factory_placer_native_validation=true` adds its source set to the
development mod. It is not included in the normal JAR, even when this source set
is enabled. The presentation itself is independently default-off under
`iedctr.dev.factoryPlacerV2Reference`.

The Python runner refuses a dirty checkout, records the full executed commit,
verifies the retained artwork's SHA-256, and checks the checkout again after the
client exits. It creates a fresh temporary game directory under `build`, a random
nonce ownership marker, and a fresh peaceful flat integrated world. The Java
fixture refuses existing saves, an existing world, an external connection,
stale screenshots, or a different nonce. The existing gameplay harness stays
disabled. No user saves or desktop are used.

## Reproducible command

From a clean committed Linux checkout with Java 21, Xvfb, xvfb-run and xauth:

```sh
python tools/validation/factory_placer_v2_reference_native.py \
  --language en_us --width 1280 --height 720 --gui-scale 1 \
  --prepare-timeout 900 --timeout 900
```

The dedicated workflow also covers French at 1280x260 scale1 and English at
1920x1080 scales1,2,3. The framebuffer, selected language, requested GUI scale,
actual GUI scale, scaled GUI dimensions and transformed public bounds must agree.
The existing default-off build, JUnit tests and GameTests run separately first.
The native client uses Gradle's development source classpath, not the packaged
JAR. CI retains the separately built normal JAR, its hash and class-entry list
for independent fixture-exclusion checks; this is not packaged-JAR native proof.

## What a passing native result establishes

- All 54 visible menu slots are picked up and returned through the actual
  screen's mouse handlers. Each pickup and return is independently observed on
  the integrated server. Return clicks wait beyond vanilla's double-click window.
  A stone and all nine hidden sentinels are conserved at every step.
- The original 18 machine slots, player inventory27+9, and saved machine
  storage27 remain distinct. Hidden slots18..26 retain gold-block counts1..9,
  totaling45; they are never projected into the menu or cleared between cases.
- Row middle/bottom/top is selected twice in changing order. Redstone inversion
  cycles0/1/4/0 twice; trigger mode toggles0/2/0 twice. Every step changes the
  observed server state. These use existing actions22 and20, not fixture-set
  substitutes. Three empty-inventory manual actions21 are observed as real
  synchronized signal-on/signal-off cycles.
- Under logic0 and no redstone signal, one manual trigger places an actual stone
  block into the clear target at0,80,1. The server's slot0 decreases2->1,
  backing inventory total47->46, selected slot advances0->1, and no item entity
  is dropped. A separate native world screenshot follows closing the menu.
- Closing reaches the server. Reopening creates a new menu ID and preserves the
  stone, hidden storage and logic/selection. A real `reloadResourcePacks()` call
  then completes, its overlay disappears, and15 further client ticks settle
  before a final frame is captured. The same reopened menu and state persist.

Fixture setup directly seeds only the isolated server's inventory/state. During
routes and control cases, clicks and resulting authoritative changes are real.
Visible inventory is empty during repeated control tests: legacy logic4/6 may
otherwise place automatically, and manual placement into an occupied target may
intentionally spit an item. The test does not change those gameplay semantics.

## Evidence and acceptance limits

The result JSON, logs and screenshots are retained under
`build/factory-placer-v2-reference-native-*/`. Eight GUI states cover normal,
selected middle/bottom rows, inverted, continuous, placed, reopened and reloaded.
Each has a controls crop copied directly from the native framebuffer, dimensions,
SHA-256 and exterior-artwork digest. The validator decodes PNGs, checks chunk CRCs,
links crop pixels to the full frame, checks the genuine status paint, verifies
retained-art pixel correspondence, and requires unchanged surrounding artwork.
Selected-slot outline pixels are checked for the captured selections0,1,6,12
in all five profiles. A strict pixel-center rule avoids inventing coverage for
subpixel edges; it does not establish outline visibility for every possible slot.
The approved source image itself must remain byte-identical.

This is screen-handler event validation through vanilla packets, **not physical
OS pointer validation**. GLFW only parks the cursor outside the panel. The suite
does not claim drag splitting, shift-click, double-click collection, outside-slot
carried drops, tooltip hover/physical cursor testing, or general visual approval.
Settled captures check the dark signal lamp pixels. Manual-trigger flashes are
verified as synchronized on/off state cycles; the lit lamp pixels are not captured.
The world screenshot is a native frame accompanying observed client/server block
state, not an image-recognition claim that JSON alone proves the block is visible.
Human review of the actual captures remains required. No merge or release occurs.

The Python tests use deliberately synthetic test pixels to test the evidence
validator. They are never runtime screenshots or accepted native evidence.
