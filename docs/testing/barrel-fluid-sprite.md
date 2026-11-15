# Barrel live fluid sprite correction

## Scope

Base: `8c8dd2f0d6dc018bd874c961e2703d85f6bd4dee` (PR 151).
Only the Barrel's existing live fluid interior changes. The 201×226 artwork,
25×74 backing at (92,35), 21×70 fluid mask at (94,37), frame, glass highlights,
meniscus, gauge, labels, slots and server/menu/packet mechanics are preserved.

`FluidBarrelSpriteRenderer` resolves `IClientFluidTypeExtensions.of(fluid)` and
its stack-aware still texture and ARGB tint, then looks up that sprite in the
current `InventoryMenu.BLOCK_ATLAS` on each draw. No sprite survives a resource
reload in a static cache. Water keeps its fluid tint; lava and milk keep their
native texture colors rather than receiving a second orange or cream tint.
All four extension color channels are passed through the colored sprite blit.
No global shader tint, pose or raw OpenGL state is modified by this helper.
The GUI scissor is paired in `finally`, including when drawing fails.

Sprites are tiled at 16 logical GUI pixels and anchored to the bottom. Full
21×70 fill uses two columns and five rows, with the 5px right column and 6px
top row clipped rather than rescaled. Empty/nonpositive stacks, empty geometry,
null still textures and missing atlas sprites leave the dark backing and glass,
without a false fluid surface. The existing floor-based fill calculation stays
unchanged: 1–457mB = 0px, 458mB = 1px, 16000mB = 35px, 31999mB = 69px,
32000mB = 70px. No minimum-pixel rule was added.

The renderer itself is fluid-extension-generic. This does **not** add arbitrary
modded-fluid or data-component storage: Barrel gameplay still accepts only
water, lava and the registered NeoForge milk under its existing sync contract.

## API evidence and limits

- Minecraft 1.21.1 mapped signature index:
  https://mappings.dev/1.21.1/net/minecraft/client/gui/GuiGraphics.html
  confirms public `blit(int,int,int,int,int,TextureAtlasSprite,float,float,float,float)`.
- NeoForge 1.21.1 source, commit `624c929f49920c250166e2a8df7af87f16d220ef`:
  https://github.com/neoforged/NeoForge/blob/624c929f49920c250166e2a8df7af87f16d220ef/src/main/java/net/neoforged/neoforge/client/extensions/common/IClientFluidTypeExtensions.java
  confirms stack-aware `getStillTexture`/`getTintColor` and `of(Fluid)`.
- These are signature/source checks, **not** a mapped compile against the exact
  project dependencies (NeoForge 21.1.230, Minecraft 1.21.1).

## Offline regression

Run from repository root:

```sh
python tools/validation/test_barrel_fluid_sprite.py
python -W error::ResourceWarning -m unittest discover -s tools/validation -p 'test_*.py' -v
```

The first test compiles the **actual production helper** plus the unchanged,
extracted `drawFluidBarrelWidgets` method against recording Minecraft-facing
test doubles. It checks coverage/UV clipping, all 0..32000mB fill boundaries,
bottom anchoring, empty/missing sprites, all ARGB channels, stack-aware extension
arguments, refreshed atlas lookups, nesting/error cleanup and glass draw order.
The fake modded fluid proves only generic renderer behavior, not tank acceptance.
It uses Java 21's installed `jdk.compiler` module; no Gradle/download is needed.
The optional `--screen-source`, `--renderer-source` and `--native-source` arguments support negative
controls without changing production files. The original flat fill, squeezed UV,
missing-scissor-cleanup, forced-alpha and rounded-half-pixel capture mutations must fail.

## Native coverage prepared, not executed locally

The existing corrected-GUI native fixture now samples the Barrel interior away
from the meniscus, glass edges/highlight and amount label:

- Empty: (99,38), size 16×69, exactly the dark backing.
- Half water: (99,73), size 16×34, more than one framebuffer color.
- Full water: (99,38), size 16×69, more than one framebuffer color.

Only the Barrel capture origin is aligned to the screen's integer `leftPos`/`topPos`
before framebuffer scaling. For a 201px panel in a 1280px window, x=539 rather
than round(539.5)=540. The old offset included the right glass edge in the sample,
which could make a flat fill appear nonuniform. The production crop helper is
covered by odd/even width/height and scale 1/2/3 regression cases. Other machines'
capture paths remain unchanged.

It verifies synchronized fluid identity and resolves the real block-atlas still
sprite/tint for the nonempty frames. The evidence gate requires those fields and
rejects old flat-fill proof. This tests elimination of the old solid rectangle;
color variation alone is **not** complete evidence of correct tint, animation,
resource reload, arbitrary resource packs or all GUI scales.

No native run, full mapped build, GameTest run, CI run or JAR was produced here.
Do not call this candidate validated for release.

## Remaining acceptance gates

1. Compile/process resources/test/build and dedicated GameTests on the exact
   final uploaded commit. Revalidate after any later conflict resolution.
2. Run the corrected native fixture in EN 720px and FR 260px. Review real crops,
   glyph/item colors after the fluid draw and full-window hover captures.
3. In an isolated test world, verify lava and NeoForge milk empty/partial/full,
   plus water at 458mB, 1000mB, 17500mB and 31999mB. Check motion across several
   frames, the clipped top/right edges, and unaltered frame/glass layering.
4. Press F3+T while the Barrel is open, resize/reopen it, and check GUI scales
   1/2/3 and automatic. Verify atlas replacement and no clip/tint leakage into
   slots, tooltips, other screens or the world. Check a replacement resource
   pack if available; do not treat a deliberately uniform pack as a vanilla
   native-fixture pass.
5. Identify the resulting JAR by SHA-256 and match its commit/CI evidence before
   any authorized release. No merge, release or permission change is included.

## Existing CI trigger behavior (unchanged)

`.github/workflows/build.yml` has `push` for main/tags, `pull_request` and
`workflow_dispatch`. A feature-branch push alone does not start this workflow.
A merge-conflicting PR cannot rely on pull-request merge-ref validation.
An authorized manual dispatch at an uploaded feature ref can run the core build
against that ref; verify the run's exact `head_sha` and checkout revision.
However `native-smelter-gui` (the corrected GUI capture job) and other native
jobs are conditioned on `pull_request`, so dispatch skips them. PR runs normally
check a synthetic merge checkout and must not be mislabeled exact feature-head
builds. No workflow was changed to bypass the conflict or dispatch/tool limits.
