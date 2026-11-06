# Existing Small Milking Machine qualification

This is a **test-only cloud qualification candidate, execution pending**, not a new GUI integration.
The existing `MachineScreen.SmallMilkingMachineScreen`, menu, entity, layout,
active artwork renderer, English/French strings, and current three source textures
remain byte-for-byte unchanged. `production-pins.json` records their baseline SHA-256
values. The active background is `milking_redesign_v1/background.png`
(`871f7c4512227153a4c49591c5facdbc95b1443114cc3206659d6dd524d1326b`),
with milk layer `f6f0507deb081a2a83b6bb455abc6237ac2b41f4ab21d43c833e356fb2d9bbe5`.
The supplied glass stays disabled. There is no assembled Milking reference PNG in
this qualification. Older Industrial Milk Tank art/toolkit is not an input.

## Implemented scope

- A separate opt-in `NativeMilkingQualificationValidation` opens a fresh,
  nonce-owned, integrated flat world. It refuses existing saves, wrong source or
  nonce, concurrent legacy/Barrel harnesses, wrong locale, and wrong viewport.
  Pre-isolation refusal only logs and disables this fixture; it cannot write a
  result or stop an existing client. No ordinary gameplay activation or production
  menu/screen behavior is changed.
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
  except the tank and the actual amount-label region `(146,72)-(210,89)`.
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
visual evidence. Syntax parsing likewise does not check mapped API types.

The Python fixtures are explicitly synthetic and test evidence rejection. They
must never be presented as screenshots or proof of Minecraft gameplay. They cover
all five profiles, 38 routes, server/client conservation, missing or malformed
proof, stale/wrong identities, numeric bool aliases, duplicate/unsafe capture
paths, symlinks, corrupted PNGs, different encodings of identical pixels,
top-down fill, wrong crop, and changes outside the allowed live label region.

## Native execution, pending

No native run or mapped compilation has yet been performed for this candidate.
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
proof, not silently represented by the route/capture cases. No synchronized animal
name/selection, FE, extraction cycle, or cream state exists here; this fixture
neither adds nor invents those gameplay features. The fixture does not qualify
cow production, multiplayer networking, persistence across world saves, or a
release artifact.
