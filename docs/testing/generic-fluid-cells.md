# Generic native fluid cells (work in progress)

The original native adapter applies to the 200 explicitly registered decorative blocks already covered by native fluidlogging. Ten full cubes accept sources; the other190 accept transient fluid. A separate [qualified external adapter](external-fluidlogging.md) extends selected vanilla families and ten pinned Immersive Engineering IDs without adding blockstate properties; it does not broaden the native adapter's property assumptions.

Supported fluids must be FlowingFluid instances whose normal world block actually exposes that fluid. Water, lava, IE creosote, ethanol, biodiesel and plant oil are qualification samples. NeoForge milk has no world liquid block and is rejected. Fluid-specific third-party reactions are not universally qualified.

Legacy waterlogged, fluid_cell and water_flow properties retain precedence and save compatibility. Canonical existing sources keep those paths. Other source and flowing states use an immutable per-chunk fluid_cells attachment, encoded with registry names and all FluidState properties. Unknown or unsupported saved identities remain inert, reserved and re-serializable; a bucket cannot overwrite them.

The initial network snapshot uses a palette of named encoded states and complete block positions; subsequent mutations send a one-cell update or explicit removal. Decoding bounds positions to the target chunk/build height and rejects malformed counts, duplicate positions and palette references. NeoForge attachment synchronization supplies the ordered initial/update stream. Runtime registry IDs are not persisted. Mesh workers receive immutable snapshots. Vanilla chunk mesh reads and optional Embeddium cloned sections/world slices read the position-aware fluid state.

Ticking delegates to each fluid's normal computation and tick delay; updates preserve the occupied block. Source creation inside transient cells is suppressed using that fluid's drop-off and connected-above geometry. Flowing cells do not yield buckets. Dynamic emission uses NeoForge auxiliary lighting populated on mutations/load/sync, avoiding chunk loads from a light worker. Derived random-tick flags are rebuilt after mutations/load.

The Passive Fluid Accumulator retains its independent exterior and4000mB tank: normal bucket for tank, sneak bucket for exterior;250mB/80ticks only from adjacent source water, never its own cell, lava or currents. Initial placement in flowing water stays dry. All ten original regression tests remain unchanged.

No upstream code was copied for this extension. The independently verified upstream source, license and PR15 audit in native-fluidlogging.md remains the provenance record for the prior native integration. This extension introduces no upstream binary dependency.

Qualification must include server GameTests, actual client update and initial payloads, drain cleanup, textured meshes, six immersed catwalk variants and native/Embeddium/ModernFix/combined profiles. A passing server suite alone is not completion. See build/generic-fourth-validation.log for497passing GameTests and clean shutdown before the final anti-promotion corner adjustment; final commit qualification remains pending.
