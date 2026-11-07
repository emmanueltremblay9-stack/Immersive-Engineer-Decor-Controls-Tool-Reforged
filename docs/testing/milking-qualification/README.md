# Small Milking Machine recovery and qualification

PR169 now continues the user's request for individual cow recovery. This is no
longer a test-only patch. The screen/menu/animal adapter and server projection
intentionally evolve; the original qualification remains a regression suite.
`production-pins.json` retains baseline hashes and pins the current production
snapshot separately, including the last qualified renderer at `750731ef` before
permanent machine text was removed. Layout, slot geometry, client registration
and all three source textures remain byte-for-byte unchanged. The active
background is `milking_redesign_v1/background.png`
(`871f7c4512227153a4c49591c5facdbc95b1443114cc3206659d6dd524d1326b`),
with milk layer `f6f0507deb081a2a83b6bb455abc6237ac2b41f4ab21d43c833e356fb2d9bbe5`.
The supplied glass stays disabled. There is no assembled Milking reference PNG in
this qualification. Older Industrial Milk Tank art/toolkit is not an input.

## Native recovery contract

- TFC 4.2.12 selects a real server animal: ready cows first, otherwise the next
  eligible cow to recover, otherwise a temporarily refused or ineligible cow.
  Ties use entity IDs. UUID, entity ID, readiness, `getProductsCooldown()` and
  current configured production duration are projected read-only. No machine
  save copies or decrements the animal's cooldown.
- Native readiness is checked every server tick. A ready TFC cow can produce
  immediately, including on the exact expiry boundary outside the old 120-tick
  phase. Rejected transactions have a separate per-animal 120-tick retry throttle.
  Native recovery/departure clears that animal's refusal. A different ready cow
  does not inherit it. This cache retains at most 64 rejections, evicting the
  oldest if necessary; at most one native attempt occurs per machine tick.
- The attempted UUID is revalidated through existing protection/product hooks
  and complete-storage commit. Cooldown/wear update only after accepted storage.
  The post-event projection rereads that same cow.
- Existing vanilla-first priority and its 120-tick production check remain.
  Vanilla Java cows have no native recovery: no fictitious duration is added.
  Other mods' modifications to vanilla `Cow` remain outside this pinned TFC API.
- The menu appends 18 words after its unchanged 16 common fields. Unsigned-short
  reconstruction preserves UUIDs, full IDs, long native remaining time and
  durations above 32767. Clients never choose a nearby animal or interpolate time.
- Portrait and tooltip identity require both entity ID and UUID. Permanent
  machine text is not drawn: the header, status band and amount/capacity area
  retain their original artwork. Hovering the header or portrait shows the name
  and full UUID. An unavailable client entity shows the localized unavailable
  description and the same full server UUID instead of another cow.
- Tank amount/capacity, machine state and native recovery remain available through
  bounded localized hover tooltips. Recovery uses TFC calendar ticks, not seconds.
  Redstone, storage and retry waits remain separate. FE, cream and extraction
  animations are not invented. Vanilla inventory stack counts and item tooltips
  remain unchanged. No opaque text masks or source PNG edits are used.

TFC/Crossover GameTests qualify the backend separately. The five GUI profiles
below contain no cows, so they qualify slots/tank/no-animal presentation, not
a TFC portrait or recovery tooltip in a rendered client.

## Implemented scope

- A separate opt-in `NativeMilkingQualificationValidation` opens a fresh,
  nonce-owned, integrated flat world. It refuses existing saves, wrong source or
  nonce, concurrent legacy/Barrel harnesses, wrong locale, and wrong viewport.
  Pre-isolation refusal only logs and disables this fixture; it cannot write a
  result or stop an existing client. The fixture has no ordinary gameplay
  activation; the recovery implementation separately changes production behavior.
- All 38 production menu slot coordinates and backing inventory indices are checked.
  Every source slot is exercised through `Screen.mouseClicked/mouseReleased` and
  vanilla client packets, followed by independent integrated-server observations.
  Input and player items return to their original slots. Output slot 1 returns to
  player slot 2 because output refuses insertion. Each route conserves one item
  and leaves the tank empty. Monotonic intervals of at least 300 ms between clicks
  avoid vanilla double-click/PICKUP_ALL ambiguity; double-click is not qualified.
- Input routing uses an empty bucket at zero fluid. Output routing uses **actual
  native bucket conversion**, never a seeded milk-bucket output: the authoritative
  handler receives 1000 mB milk, input receives one empty bucket, and native server
  ticking produces one output milk bucket while the tank falls to zero. Before
  and after snapshots cover fluid, input, output, player inventory, and cursor.
  The bounded world contains no fixture cows or adjacent inventories. Animal
  extraction/production is not qualified by this fixture.
- Empty, half (2000 mB), and full (4000 mB) captures use the real server fluid
  handler, empty slots/cursor, synchronized client amount, and authoritative server
  fluid identity. Captures require at least 12 consecutive ticks of matching
  synchronized client state after server scheduling, not merely elapsed ticks
  since a fill request. Milking `menu.storedFluid()` is deliberately not used: that API
  returns empty for this machine. ResourceManager reads verify the actual loaded
  background and milk bytes, not just source filenames.
- Screenshot crops use **actual public integer screen bounds**, fixing the old
  corrected-machine fixture's fractional-origin mismatch without editing it.
  The panel is 228 x 228. Tank local rectangle is `(104,49,22,51)`.
- Python independently decodes RGB/RGBA PNG pixels, checks file integrity and
  exact screenshot/crop correspondence, and verifies bottom-up filling: the top
  26 GUI rows of half equal empty, and the bottom 25 equal full. Each full interior row must
  differ substantially from empty. The pinned milk sprite has
  near-transparent antialiased edges, so two GUI rows at the top/bottom are exempt
  only from that density threshold, never from crop or half/full pixel equality.
  A pinned-source alpha/compositing regression covers this boundary at scales 1/2/3.
  Encoded PNG hashes alone never establish state
  differences. All opaque exterior panel pixels must stay equal across states,
  except the tank. The former amount-label region `(146,72)-(210,89)` is no
  longer exempt; the original artwork must stay unchanged there too.
  Tooltips are disabled and the pointer is outside the panel for captures; there
  is no arbitrary hover mask or broad exterior-art exclusion.

## Five planned native profiles

| Profile | Locale | Framebuffer | Requested/actual GUI scale | Public panel origin | Tank framebuffer rectangle |
|---|---|---|---|---|---|
| en-1280x720-scale1 | en_us | 1280 x 720 | 1 | 526,246 | 630,295,22,51 |
| fr-1280x260-scale1 | fr_fr | 1280 x 260 | 1 | 526,16 | 630,65,22,51 |
| en-1920x1080-scale1 | en_us | 1920 x 1080 | 1 | 846,426 | 950,475,22,51 |
| en-1920x1080-scale2 | en_us | 1920 x 1080 | 2 | 366,156 | 940,410,44,102 |
| en-1920x1080-scale3 | en_us | 1920 x 1080 | 3 | 206,66 | 930,345,66,153 |

Impossible GUI-scale requests and scale > 1 framebuffers not divisible by scale
are rejected. This suite does not quietly reinterpret a requested scale as a
successful clamped profile. Actual locale, GUI dimensions, framebuffer, bounds,
source SHA, current nonce, clean exit, completion marker, and all required
server/client evidence are checked for each run.

## Local checks

```sh
python tools/validation/check_milking_qualification.py
python -m unittest discover -s tools/validation -p 'test_milking*.py' -v
java tools/validation/ParseBarrelSources.java
```

The renderer test compiles the **actual active** `RedesignedMachineArtwork` and
`MilkingRedesignLayout` against recording doubles and checks 14 amount/capacity
cases, exact UVs, draw order, bottom-up scissor, cleanup, and absence of glass or
invented state. This is not a mapped Minecraft compile, GPU execution, or native
visual evidence. A separate source-routing guard requires Milking's label pass
to return without text or mask drawing and rejects regressions in that branch.
Syntax parsing likewise does not check mapped API types.

The Python fixtures are explicitly synthetic and test evidence rejection. They
must never be presented as screenshots or proof of Minecraft gameplay. They cover
all five profiles, 38 routes, server/client conservation, missing or malformed
proof, stale/wrong identities, numeric bool aliases, duplicate/unsafe capture
paths, symlinks, corrupted PNGs, different encodings of identical pixels,
top-down fill, wrong crop, and changes anywhere outside the tank, including all
former text bands at scales 1/2/3.

## Native execution of the text-free display, pending

The previous `750731efa1899230cea896e20d137d325bd34f21` candidate was qualified
before this display-only change. That result does not qualify the changed
renderer. No native run or mapped compilation has yet been performed for this
text-free candidate.
The current local preparation environment lacks `Xvfb`/`xvfb-run` and a prepared
mapped Gradle runtime. Cloud qualification is prepared in
`.github/workflows/milking-qualification-validation.yml`; it is not evidence of a
successful run until the workflow actually executes and its outputs are reviewed.
The native runner refuses an uncommitted worktree by design: a base commit SHA
must not identify execution of uncommitted fixture changes.

The dedicated `pull_request` workflow checks out and verifies the exact PR head,
never GitHub's synthetic merge commit. It has only `contents: read` permissions,
does not persist checkout credentials, and reuses the pinned action revisions
from the existing Barrel/Solar workflows. Its build gate runs the production
preservation checks, existing GUI guards, full Python regressions, mapped Java
compilation, JUnit, existing GameTests, and JAR build. Only after that gate passes
do five independent native profile jobs run the existing strict Python runner.
Each job verifies the clean source identity before and after execution.

Build artifacts retain the exact source SHA, built JAR and SHA-256, test reports,
and build logs. Native artifacts retain the exact source SHA, profile-specific
proof, framebuffer/tank PNGs, preparation/launcher/runtime logs and any crash
reports for 14 days, including on failure. The built JAR hash identifies the
packaged build artifact; native `runClient` executes the source runtime classpath,
not that packaged JAR. All five native jobs must pass for the same exact head;
synthetic test success or a completed build gate cannot stand in for native proof.

Once a clean committed candidate and a permitted Linux/Java 21/Gradle/Xvfb runtime
are available, first compile/type-check and run the normal regression checks:

```sh
bash ./gradlew compileJava processResources test runGameTestServer jar --stacktrace
```

Then run each profile separately, for example:

```sh
python tools/validation/milking_qualification_native.py --profile en-1280x720-scale1
python tools/validation/milking_qualification_native.py --profile fr-1280x260-scale1
python tools/validation/milking_qualification_native.py --profile en-1920x1080-scale1
python tools/validation/milking_qualification_native.py --profile en-1920x1080-scale2
python tools/validation/milking_qualification_native.py --profile en-1920x1080-scale3
```

Each run writes local proof, PNGs and logs below
`build/milking-qualification-native-*/`. The runner records and rechecks the clean
source SHA before/after execution. The preparation step only adds the workflow
and updates these instructions locally. It does not commit, push, dispatch a
workflow, change PR metadata, upload files, merge, deploy or publish a release.
The workflow is a candidate for reviewed cloud execution, not a claim that any
remote action or in-game verification has already occurred.
No native completion is claimed until those runs actually pass and their outputs
are inspected.

## Explicitly deferred and unclaimed

Resource reload, close/reopen, shift-click, complex gestures, physical OS input,
full JEI actions, and full visual acceptance are deferred. They are named in the
proof, not silently represented by the route/capture cases. The GUI fixture does
not qualify the synchronized animal portrait/recovery presentation, multiplayer
networking, persistence across world saves, or a release artifact. Native TFC
production and projection are covered by separate server GameTests. FE, extraction
animations and cream remain unsupported.


## Targeted vanilla Mooshroom native proof

`tools/validation/mooshroom_milking_native.py` adds one isolated client run to
the existing TFC recovery GUI workflow, using TFC 4.2.12 loaded but the real
`minecraft:mooshroom` entity and its vanilla `Cow` subclass path. It changes no
gameplay, recipe, animal cooldown, cream, or stew behavior.

For each RED and BROWN adult, a server post-tick observer checks a real milk
bucket, removes that verified output, then requires exactly 1000 mB plain milk
120 normal world ticks later with no earlier production. This is machine
cadence, not animal recovery. The fixture pauses the machine only after that
second product, then captures the native portrait and actual hovered identity
tooltip with synchronized entity ID, UUID, and custom name. Identity is tooltip-only;
the header remains unlettered. The fixture's client/server source identity checks
remain mandatory and do not depend on permanently visible labels. Both baby variants
are observed for at least 240 ticks and must retain the input bucket, produce
no milk, and have no selected source or live portrait.

Six full screenshots and exact portrait crops are retained with their hashes,
source SHA, nonce, viewport, runtime version, native cursor coordinates, and
server/client state. Red and brown adult portrait crops must differ. Screenshot
validation is bounded evidence and does not replace owner visual acceptance.
Synthetic validator tests are not evidence that the native fixture ran.

Run the targeted validator tests with:

```sh
python -m unittest discover -s tools/validation -p test_mooshroom_milking_native.py -v
python tools/validation/mooshroom_milking_native.py --profile fr-1280x720-scale2
```
