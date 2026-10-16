# Native decorative fluidlogging: first cycle

This draft implements source cells for four existing waterloggable registrations:
`steel_catwalk`, `steel_catwalk_stairs`, `steel_catwalk_ta`, and `steel_railing`.
Supported fluids are source water, source lava, and Immersive Engineering source
creosote. Other decorative families require explicit opt-in and further tests;
vanilla blocks, other mods and machines are not globally patched.

## Storage, compatibility and synchronization

The original `waterlogged` boolean retains its name and behavior. A distinct
`fluid_cell` enum stores `empty`, `lava` or `creosote`; water never lives in
that enum. Old states missing the property default to `empty`, including old
states with `waterlogged=true`. Placement accepts sources only. Buckets reject
occupied cells, pickup clears both properties, and repeated pickup yields nothing.
Existing rail credits, geometry variants, paid lights, rotation and mining
transactions continue to operate on the same state.

The ordinary block-state save codec, chunk palette and native block/chunk update
packets carry both properties. There is no additional per-chunk attachment,
custom fluid packet or raw `Fluid.FLUID_STATE_REGISTRY` ID. Server and client
`BlockState.getFluidState`, level and chunk lookups therefore agree. Creosote
uses its registered resource identity, not an assumed numeric fluid-state ID.
Malformed states containing both properties prefer legacy water, including light
emission. Lava emits level 15; raised deck lamps retain level 14 when otherwise
dry or wet with non-lava fluid.

Native `LiquidBlockContainer` / `BucketPickup` contracts handle actual survival
bucket transactions. NeoForge's real fluid renderer uses the cell's fluid state
and registered IE textures. Neighbor changes schedule the stored fluid's ticks;
normal destruction leaves its exact source using the vanilla fluid restoration
path. This is a world source, not a tank or pipe capability.

The Passive Fluid Accumulator is unchanged. Its exterior waterlogging remains
independent of its 4000 mB tank; normal bucket interaction targets the tank and
sneaking targets the exterior cell. Collection remains 250 mB per 80 ticks from
adjacent source water only, never its own cell. Its ten existing GameTests and
[contract](passive-accumulator-waterlogging.md) remain regression requirements.

## Upstream verification and reuse inventory

Reviewed source: [NightEpiphany/Fluidloggable v2-1.21-refilled](https://github.com/NightEpiphany/Fluidloggable/tree/eb1bf833a4acbeea609fd553991acb8cd9fe93fc),
commit `eb1bf833a4acbeea609fd553991acb8cd9fe93fc`.
The [actual LICENSE](https://github.com/NightEpiphany/Fluidloggable/blob/eb1bf833a4acbeea609fd553991acb8cd9fe93fc/LICENSE)
is MIT, Copyright (c) 2022 - 2023 Leximon.

Also reviewed [PR15](https://github.com/NightEpiphany/Fluidloggable/pull/15),
open/unmerged at verification, head `df2eb4faed117dbf2c4da4c5aa71d9d9d2a49de7`
in KoalaNalle/Fluidloggable. Its LICENSE is the same MIT notice.
The changes repair NeoForge modded-fluid mapping in initial, individual and
batched synchronization and initialize storage in both section constructors.
These corrections motivate avoiding numeric fluid-state IDs and testing initial
chunk synchronization separately from block updates.

| Material | Use in this implementation |
| --- | --- |
| Fluidloggable code, resources and mixins | Reviewed reference only; no files or code copied or ported |
| PR15 mapper, packet and section patches | Reviewed reference only; no custom mapper or section mixin reused |
| NativeFluidloggedBlock and tests | Newly authored native implementation in this repository |
| IE fluid textures and buckets | Existing required IE runtime dependency; not bundled |

Credit to Leximon, NightEpiphany / MoreThanHidden, and KoalaNalle / PricelessKoala
for the reviewed work. No Fluidloggable runtime dependency is added. Historical
license claims were not used as proof.

## Automated checks

Nine `NativeFluidloggingGameTests` cover:

- NBT/save codec and legacy wet/dry default compatibility across all four blocks;
- actual ChunkSerializer write/read through a compressed NBT stream for all 12 block/fluid cases;
- initial chunk-section palette and single-block ID transport including IE, then
  removal of stale source data;
- actual survival bucket insert/pickup cycles and rejection of duplicate storage;
- actual BlockItem placement into sources and flowing variants of all three fluids;
- scheduled lava and creosote outflow, source preservation, and no regeneration after drain;
- world destruction, exact remaining source pickup and no second bucket;
- unchanged accumulator collection with adjacent native water/lava/creosote, including its own exterior water.

The nonce-guarded integrated client fixture adds 24 cases: four blocks times
three fluids times native block updates and full initial chunk packets. Each
case requires equal client block/world/chunk fluid identity, valid still/flow
sprites, nonzero textured quads from the actual native fluid tessellator and a
subsequent client drain update. The proof validator rejects missing, duplicate,
stale, untextured or uncleared cases. Existing native lamp mining checks still
run before this matrix.

Reproduce with the checked-in wrapper and Java 21:

```powershell
.\scripts\invoke-gradle.ps1 compileJava runGameTestServer
.\scripts\invoke-gradle.ps1 clean build
python -m unittest discover -s tools/validation -p 'test_client_fluidlogging.py' -v
python -m unittest discover -s tools/validation -p 'test_client_gameplay.py' -v
```

The full Python evidence suite includes two Linux-only process teardown tests;
run `python -m unittest discover -s tools/validation -p 'test_*.py' -v` on Linux.
The Linux CI additionally runs `python tools/validation/client_gameplay.py` in
its fresh temporary integrated world. That runner requires Xvfb and is not a
Windows visual validation. Successful tessellation is not a human screenshot
review. No installed Prism JAR or user world is part of any of these checks.


## Targeted stair flow follow-up

Steel Catwalk Stairs also accept transient naturally flowing water. Its amount and falling flag are stored separately from the legacy WATERLOGGED source and persist/synchronize as ordinary block state. It recedes when supply disappears and cannot fill an empty bucket. Intentional source filling remains available and clears transient state. Natural flow never promotes the stair to a permanent source. Other blocks and non-water cells retain the source-only scope above. See [distance and recession qualification](water-canal-visual-investigation.md).

## Catalogue expansion: controls and inserted lights

The native source contract now also applies to all 97 registered controls: water, lava and IE creosote are independent exterior cells, serialized with block-state palettes. Existing controls load dry when the three new fluid properties are absent. Signal strength, timers and support rules remain their original state-based operations; fluid ticks are scheduled in addition to block ticks.

The two halves of `industrialswitch` initialize from their own world positions. Power/facing synchronization copies the upper half's own water/source/current properties back onto the synchronized geometry. Normal shape-based pair removal uses vanilla `Block.updateOrDestroy` and `Level.destroyBlock`, retaining drops and releasing each half's own fluid. Creative upper-part removal restores the lower fluid explicitly.

Raised catwalks now accept transient water as well as their existing sources. Paid inset-light installation, selective mining, refund and final deck drops are tested with water/lava/creosote sources and level-5 current. Removing a lamp retains lava's own emission of 15 while removing the lamp's emission.

`NativeFluidloggingGameTests` enumerates all opted-in registered blocks rather than a fixed sample. The controls lot brings this to 105 blocks; source survival bucket cycles, actual BlockItem placement, compressed chunk reload and initial/update palette roundtrips cover that entire set. Dedicated tests cover 23 wet latching controls, pulse reset while water recedes, asymmetric cabinet cells, and real server-player destruction of either wet half in survival/creative with exact item balance. Released vanilla lava/water may react normally; storage does not invent or suppress chemistry.

The new `NativeCatalogueValidation` runs 40 scenes in the existing nonce-guarded synthetic integrated world: four standalone light types, lever, button, contact mat and inserted-light deck, each with three stored sources, natural water and a fully submerged water cell. It records matching server/client states, fluid mesh vertices and 80 before/after screenshots, retaining geometry, signal and paid-light state through source pickup or recession. These are eight visual representatives, not visual certification of every block. `catalogue_visual.py` checks exact scenario IDs, source/flow amount, recession, filenames and full-size PNG integrity; actual captures must still be inspected.

The expanded full client fixture has a 10-minute internal deadline and 12-minute process bound, allowing its 4480 settling/recession ticks plus the earlier suites. The PR62 flow-only internal deadline remains 6 minutes. No test uses a personal world or installed Prism runtime. The exhaustive per-block plan and remaining families are in `catalogue-fluidlogging-matrix.md`.
