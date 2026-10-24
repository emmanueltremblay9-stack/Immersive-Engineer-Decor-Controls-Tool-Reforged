# Qualified external fluidlogging

This extension starts at main `98d15a3f89b4e4bf889ab2f77792ed7f31b2469f`, independently of the geometry-cache PR #94. It keeps the original 200 IEDCTR registrations, properties and native adapter.

## Block scope

`ExternalFluidAdapter` admits only registered vanilla blocks whose **exact runtime class** is StairBlock, SlabBlock, FenceBlock, WallBlock, IronBarsBlock, StainedGlassPaneBlock, LadderBlock, TrapDoorBlock or one of the three vanilla WeatheringCopper stair/slab/trapdoor classes. Double slabs and block entities are excluded. No blockstate property is added. Ordinary glass panes use IronBarsBlock; stained panes have their own admitted class.

Immersive Engineering 1.21.1-12.4.2-194 has ten explicit ID/class pairs:

- `stairs_treated_wood_{horizontal,vertical,packaged}`: IEStairsBlock.
- `slab_treated_wood_{horizontal,vertical,packaged}`: BlockIESlab, single only.
- `wall_clinker_brick`: IEWallBlock.
- `treated_trapdoor`, `treated_trapdoor_framed`, `steel_trapdoor`: IETrapDoorBlock.

The dependency's existing bytecode was inspected with Java 21 `javap`: stairs/walls inherit the standard container contract; slabs delegate that contract to SlabBlock and customize climbing/light occlusion; trapdoors preserve waterlogging through their redstone/interaction state changes. The tests assert the exact registered IDs/classes and exercise all ten. Other namespaces and subclasses require a separate reviewed adapter and regression fixture. IE metal ladders, catwalks, other IE slabs/stairs, machines, tanks, third-party block entities, fence gates, full cubes and multipart doors are excluded from this extension. Existing IEDCTR machines retain their pre-existing exterior-cell behavior.

## Fluids and ownership

The external adapter admits water, lava and the pinned IE creosote, ethanol, biodiesel and plant-oil world fluids. Milk and unqualified custom world-fluid reactions are excluded. Existing IEDCTR generic-fluid eligibility is unchanged.

Canonical bucket water sources continue to use native WATERLOGGED. Existing saved waterlogged sources are not migrated or demoted. Natural entering water and other flowing fluids use the existing registry-name chunk attachment with exact amount and FALLING. Natural source promotion is suppressed; supply removal drains the current. Intentional non-water sources and noncanonical source states also use the attachment. Native water and attachment data never both own an active cell. Unknown persisted entries remain reserved and serializable.

Vanilla buckets and vanilla fluid spread call the real SimpleWaterloggedBlock methods through a scoped interface mixin, including SlabBlock's double-slab guard. Occupied sources reject duplicate insertion; same-fluid currents may be replaced by an intentional source bucket. Currents cannot fill buckets. Vanilla wall/shape occlusion, fluid speed and tick delays remain authoritative. Positionless `BlockState.getFluidState()` cannot expose attachment fluids; external consumers need position-aware world reads.

Different fluids cannot overwrite an occupied external cell. Reactions that would replace the whole decorative block are refused inside that cell: no stone/obsidian product or second bucket is generated there, and the resident fluid remains. Free neighboring liquid blocks retain their native reactions. This is a container boundary, not a universal compatibility promise for other fluids. Lava's random ticking, fire and entity hazards are not disabled; wood containing lava is not advertised as safe.

## Lifecycle

Block placement captures an admitted world fluid without converting currents to sources. Destruction and loss of ladder support release the existing fluid into the world through Level.destroyBlock; the attachment is cleared. Orientation/connection changes keep the cell. A slab containing native water, an attachment fluid or an unresolved reservation refuses player merging before consuming the second slab: drain it or wait for recession first. Dry merging creates an ordinary unsupported double slab. Explicit commands replacing a container with an incompatible solid clear its attachment, as an intentional world replacement; no hidden reservoir survives.

Neighbor shape changes wake attachment fluids. Registry-name compressed chunk serialization, palette/delta/removal synchronization, authoritative client prediction restoration, immutable native/Embeddium render snapshots and auxiliary light reuse the existing architecture. External light getters use auxiliary light without requesting a FULL chunk on light workers.

## Verification and limits

`ExternalFluidloggingGameTests` covers exact eligibility/negative families, the unchanged 200 native opt-ins, real survival source buckets for 22 representative blocks and six fluids, natural propagation/recession by family, no source promotion, legacy sources, falling/occluded entry, wet/dry slab merging, destruction/support loss, incompatible insertion, compressed reload, palettes/deltas/removal and lava light. Dense snapshot mutation samples log timings for 256/1024/4096 cells and 64 mutations. They measure the existing map-copy implementation; no negligible-cost or performance improvement claim is made.

`ExternalFluidClientValidation` adds 264 external cases to the existing isolated renderer harness: 22 blocks, six source/current states, update and initial packet paths. It requires matching server/client/chunk/render snapshots, textured native liquid meshes, prediction restoration and light/drain cleanup. The existing Python gate rejects missing, stale, duplicated or incomplete external evidence. The existing native/Embeddium/ModernFix/combined CI profiles execute it and retain it in `client-generic-fluid-result.json`; no workflow or dependency is added.

CI must qualify the exact final commit. Test definitions are not pass evidence. Final run IDs, counts and any failures belong in the draft PR qualification record. No interactive in-game visual inspection, personal-world load, Prism installation, merge or release is part of this work. Automated meshes do not establish visual quality for every material, orientation or modpack.
