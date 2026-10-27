# Small Fluid Collection Funnel: connected-source drainage

## Contract and runtime

The approved limit is **16 connected-fluid edges from the inlet cell immediately above the Funnel**, not a 16-cube or a radius that can cross dry blocks. That inlet must contain moving fluid and have an open lower face. A source directly above is not an inlet. Closed shared collision faces block the graph. The closest source uses shortest connected path distance; equal paths use ascending X, Y, Z coordinates.

The ordinary existing machine ticker now runs source work every 40 ticks. Each work quantum represents 250 mB. The first three quanta are saved virtual work, **not fluid in the tank** and not extractible by a bucket or pipe. At the fourth quantum, the graph, selected source, compatible fluid/components, complete 1000 mB tank space and loaded removal neighbourhood are revalidated. The source must actually be removed before 1000 mB are credited. Changing or losing the selected source resets unpaid work. Two collectors cannot pay the same source twice. No FE or transit reservation is added.

No source within the limit means **zero drainage credit**, even if a long waterfall continues to touch the inlet. Water's ordinary world regeneration is not disabled; every credited bucket still requires a source present and successfully removed at payment time.

Existing output remains at most 250 mB downward every 40 ticks, reduced by actual destination acceptance. The tank holds 3000 mB of one component-compatible fluid at a time. Fluid type may change after emptying. Existing pipe/manual tank access is retained; top-only refers to intake from the world.

Rainfall remains separate:50 mB in rain or 100 mB in thunder every 80 ticks, using the existing native `isRainingAt` exposure and anti-mixing checks. A successful source-work cycle takes precedence over a weather quantum. No rainfall is inferred from global weather alone, and a source-free stream is never converted into drainage credit. The Accumulator, Barrel and valve runtime are unchanged.

## Bounds and fluid adapters

Breadth-first search visits at most 6017 connected cells at 16 edges, with at most six neighbour checks per visited cell. A payment may perform a second bounded search. An unavailable frontier suspends the search. Source removal is refused when its standard neighbour/shape-read neighbourhood is not loaded. Search, machine redstone bypass and I/O do not request unavailable chunks. This is a structural work bound, not a CPU or heap benchmark.

The adapters accept verified ordinary liquid bucket pickup, qualified native fluidlogged blocks and owned exterior fluid cells. Bucket round-trip identity/components and post-removal source state are checked. Unsupported/custom pickup overrides fail closed; no universal third-party-fluid claim is made. Water, lava and pinned IE creosote have dedicated coverage. Fluidlogging drainage preserves the decorative block and removes only its source fluid.

## BlockEntity persistence and integration

- Existing machine registration and server ticker are reused; no new BlockEntity type or client renderer is required for gameplay.
- `FunnelFluid.Version=1` retains the component-preserving tank. Existing `FluidAmount` and `FluidType` saves still migrate; unknown payloads remain preserved and fluid I/O is locked.
- `FunnelFluid.Pending` stores virtual progress, source position, source block snapshot and source fluid. It is always revalidated before another work quantum or payment.
- Existing `TickCounter` preserves the 40-tick phase across reload. No fluid is granted merely by loading saved work.
- Existing level/comparator synchronization reflects paid tank fluid, not virtual work. Reentrant fluid callbacks cannot advance this machine ticker or access locked tank/bucket operations.
- Dropping/removing a Funnel does not turn unpaid progress into an item or fluid.

## Evidence scope

`FunnelRuntimeGameTests` exercises the real machine `serverTick` entry point, including 16/17 edges, no-source flow, side/dry-gap refusal, source loss, capacity/simulation, simultaneous collectors, saved work and phase, qualified fluids, closed inlet and missing chunks. These synchronous tests do not pretend to advance the world's fluid scheduler.

The opt-in `NativeFunnelRuntimeValidation` observes ordinary world BlockEntity ticks in an existing nonce-guarded cloud world. It never calls the machine ticker manually. It saves at 750 mB unpaid work, stops the client, reloads the same saved world in another process and checks source payment, continued conservation, real IE-pipe output and a 17-edge source control. Its flags are absent during normal play. Debug clock freezing is used only to capture/save an already observed state.

Valve manual pages are also corrected in FR/EN. They distinguish check, controlled and analog roles, per-call limits, axial ports and required source/destination. The IE Hammer action is verified through the pinned implementation and a GameTest; the text says Use rather than assuming the player's mouse bindings. No valve logic is changed by this follow-up.

Exact local, native and CI results, artifact hashes and any limits are recorded in the task evidence report. Native restart assertions are not replaced by NBT-only tests, and no personal modpack/Prism qualification is inferred.
