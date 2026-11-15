# Freezer V5 bounded native interaction fixture

Status: LOCAL SOURCE PREPARED. NOT COMPILED WITH NEOFORGE. NOT RUN IN MINECRAFT.

This adds an isolated native-client test path to the existing default-off live-energy
Freezer candidate at base 8c8dd2f0d6dc018bd874c961e2703d85f6bd4dee. It does not make the
candidate a release, change the production default, or establish a passing CI run.
All existing V5 pixels, geometry, 39 menu indices, inventory transfer rules, recipes,
energy mechanics and synchronized data fields remain unchanged. Frost remains static.
The recovery and static-art/live-energy explanations now have English and French keys.

## What the fixture will exercise when actually run

- Create a fresh nonce-marked isolated world and a real Freezer block entity.
- Open the registered FreezerReferenceScreen through the integrated server's actual menu.
- Verify all 39 protocol indices and all 38 visible slot-center hitboxes.
- Charge the actual server FE storage to 0, 8000 and 16000 FE, wait for synchronized
  state plus eight settle ticks, and save full native framebuffer PNGs (0, 5, 11 cells).
- Use actual screen-coordinate click/release events, not a direct menu click injection:
  pick up two ice from the hotbar, right-click one into the input, reject output insertion,
  then return the remaining carried ice to the hotbar.
- Verify effects on the authoritative server, not just client-side prediction.
- Populate a named historical reserved stack as explicit fixture setup. Exercise recovery
  with a full inventory, then with room, then a repeated click after the control disappears.
  Verify exactly five items and their custom name survive, with no duplicate or carried stack.
  Any observed dropped dirt fails immediately, rather than waiting until it falls away.
- Close the screen, wait for server closure, reopen with a new menu ID, and check inventory
  and geometry persist.
- Supply exactly 1440 FE and let the actual machine tick 180 times to turn one ice into
  packed ice. Do not inject progress, advance an artificial client clock, or paint frost.
- Record ten bounded interaction cases, four full native screenshots and their meter crops.
  The three energy meter crops must differ even if the world backdrop is animated. Capture hashes and
  exact clean source revision are checked. The report explicitly declines full visual acceptance.

## Run after this candidate is committed in an authorized Linux execution environment

Prerequisites: Java 21, checked-in Gradle wrapper and resolvable project dependencies,
Xvfb, xvfb-run, xauth and software OpenGL. No existing Minecraft installation is used.

    python tools/validation/freezer_reference_native.py --language en_us --height 720
    python tools/validation/freezer_reference_native.py --language fr_fr --height 260

The launcher refuses an uncommitted checkout. It checks clean HEAD both before and after
execution, hashes the original V5 art, starts its own temporary run directory under build,
and uses the existing guarded fresh-world harness. It does not use the user's PC, connect
to a remote game server, alter a player world, publish, upload, or trigger GitHub workflows.
No workflow change or remote Git write is included.

Outputs, only if a native run actually occurs:

- build/freezer-reference-native-*/run/client-freezer-reference-result.json
- build/freezer-reference-native-*/run/client-gameplay-result.json
- build/freezer-reference-native-*/run/freezer-reference-screenshots/*.png
- bounded preparation/client logs under the same run directory

Missing or stale nonce, wrong source or artwork, missing interaction, duplicate energy
frames, wrong window/view scale, unchanged menu ID after reopening, missing screenshot,
changed screenshot bytes, crash, timeout, dirty code or absent completion marker all fail.
The synthetic unit-test PNGs exist only inside temporary test directories and are never
native evidence.

## Checks performed locally

- 14 Python evidence-validator unit tests including large and fitted small windows.
- 30 existing client-gameplay validator tests.
- 131443 inherited pure Java model/source-pixel assertions.
- Inherited geometry, V5 source SHA, 38 anchors/39 indices, event-routing and source-wiring checks.
- Java syntax parsing for the fixture and dispatcher. This is not mapped Java compilation.
- Exact preservation of original artwork and previous deliveries.
- Clean-checkout refusal tested: exits before starting Minecraft on the present dirty candidate.

Actual build attempt: the Gradle 9.2.1 wrapper could not download its distribution from
services.gradle.org: java.net.SocketException: Network is unreachable. No Gradle tasks
were run. Xvfb and xvfb-run are also unavailable. No alternate network route was used.

## Explicit remaining limits

No NeoForge compile, JUnit/GameTest integration, native client execution, actual screenshots,
JAR or exact-commit CI result exists for these new changes. The fixture still needs mapped
compilation and runtime debugging in an authorized environment before anyone relies on it.
Even a passing bounded run would not qualify drag splitting, number keys, shift-click in both
directions, every slot interaction, keyboard recovery, redstone/output-blocked transitions,
continuous resize, all GUI scales, real translated tooltip pixels, or human art acceptance.
The clean chamber and aligned transparent frost layer are still required for live frosting.


## Draft PR cloud continuation

The Freezer reference workflow now runs on pull requests and selects the exact PR
head SHA, rather than borrowing the synthetic merge identity. It compiles/tests,
then invokes the bounded English 720px and French 260px native fixtures and retains
source-bound screenshots/reports. Both must actually finish successfully before
claiming native validation. LanguageManager's selected locale is now required in
the proof and matched against the requested language. Local synthetic tests cover
missing/mismatched languages. This does not prove translated glyph readability.

All 22 Barrel changes from PR151 head 8718a089 were preserved byte-for-byte during
reconciliation. These workflow additions have not yet executed at authoring time.
The older static-stage validation-status file is historical, not current CI proof.
