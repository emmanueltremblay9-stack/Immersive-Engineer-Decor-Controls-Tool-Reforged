# Wood beams and fluid routing: development contract

# Historical PR111 checkpoint

The unfinished Funnel scheduling described below is the historical PR111 state. The subsequent Funnel-only runtime and valve-manual follow-up is documented in [funnel-connected-source-drain.md](funnel-connected-source-drain.md). The other three feature families are unchanged by that follow-up.

## Current completion boundary

This is an unfinished four-part development lot. It must not be presented as a released feature.
The wood beam, valve transport and Barrel changes are implemented. The Funnel's component-aware
tank and parameterized source-payment transaction are implemented and tested, but its new source
capture is **not yet called by the ordinary machine tick**. The existing direct-source tick remains
until the maximum connected-path distance is decided and the scheduler/weather tests are updated.
No recipe has been selected for the wood beam; it is available in creative mode only.

## Wood geometry and placement

`treated_wood_beam` is a new wood item with three saved mounts: ceiling, wall vertical and wall
horizontal. Existing steel horizontal and vertical IDs are unchanged. All three source cuboids,
textures and per-vertex UVs come from `models/block/pole/straight_pole_model.json`; they are rotated
and translated without scaling. Its five-pixel stepped profile is preserved. The new outline and
collision follow those actual cuboids; the existing pole's outline is not changed.

On the four walls, the nearest horizontal edge selects a horizontal beam; the nearest top/bottom
edge selects a vertical beam. Centre and equal distances select vertical. Floor/ceiling clicks use
the existing horizontal Double-T placement convention. Same-plane wall joints and ceiling joints
follow the existing support-neighbour semantics. No separate L/diagonal item is introduced.
A vertical straight treated-wood pole below a ceiling beam gets a matching wood connection.
Neighbour removal clears the connector; fluids and the existing steel paid-lamp state remain intact.

Reproduce generated resources with `python tools/models/generate_treated_wood_beam.py --check`.

## Valve transport

FACING denotes the outlet. Its opposite face accepts fluid and proxies directly to the adjacent
outlet destination. The outlet exposes a blocking handler with one virtual tank and zero capacity,
allowing IE to render the axial connection while rejecting reverse fill/drain. Lateral and unsided
capabilities are absent. There is no transit tank, ticker or fluid amount saved in the valve.

The selected reference limits are 1000 mB per fill operation. Controlled valves close at signal 0.
Analog levels 1 through 14 use the integer slope 66 mB per level; signal 15 opens to 1000. These are
operation ceilings, not a guaranteed tick throughput. The historical reference field initialized
66, while its configuration later supplied 20; this implementation uses 66 explicitly. It does not
claim that66 was the effective configured default in every historical installation.

A per-entity guard rejects recursive forwarding and releases in `finally`, including destination
exceptions. The same SIMULATE/EXECUTE action and FluidStack components are forwarded. Cached face
handlers recheck the current FACING, and orientation changes invalidate capabilities. Redstone
connector state excludes the pipe axis. Vanilla weak-power neighbourhoods must be available before
reading their signal; otherwise controlled valves close. This does not make guarantees about
arbitrary third-party redstone readers or IE's internal traversal outside the immediate route.

The implementation uses IE's public `IFluidPipe` defaults and does not manufacture pressure or FE.
Unpressurized IE 12.4.2-194 accepted 50 mB in the captured route. Destination acceptance can therefore
be smaller than the valve ceiling or the machines' offered 250 mB.

## Barrel

Capacity is 32000 mB consistently in the server handler, menu, world gauge and comparator. The
legacy `FluidAmount`/`FluidType` schema and supported water/lava/milk types remain. This does not
extend the Barrel to arbitrary component-bearing fluids. Capability access is bidirectional on
world UP/DOWN, absent on four lateral faces, and preserved for unsided/manual bucket access.
Standing auto-output still offers at most 250 mB every 40 ticks.

The recipe is `PPP/PBP/PPP`, with steel-plate tag `c:plates/steel` and
`immersiveengineering:metal_barrel` at the centre; output is one IEDCTR Fluid Barrel.

## Funnel preparation and pending scheduler

Only a non-source fluid in the immediately-above cell can start the new graph search. Search
follows the same fluid through physically open faces, using position-aware fluid reads. It never
forces an unavailable chunk. An incomplete nearer frontier suspends collection. Equal shortest
paths choose ascending world X/Y/Z coordinates. Distance is the number of connected fluid edges
from the opening, not straight-line distance through a wall.

The L1 lattice bound is `(4*r^3 + 6*r^2 + 8*r + 3)/3`: 833 connected cells for 8 edges, 6017 for 16.
Each cell has at most six neighbouring directions; the final payment revalidates with a second
search. These are work-volume bounds, not measured timing claims. The helper's defensive maximum
of 32 is not an approved gameplay range. No production range is selected yet.

Four 250 mB work increments produce one atomic 1000 mB payment. Work is non-extractable: the tank
receives nothing until a connected source is actually removed, the whole 1000 mB fits and the
nearest source is revalidated. Thus the planned source mode averages 250/40 ticks in 1000 mB lots,
not 250 mB available immediately in its first cycle. Lost/replaced sources reset unpaid work.
Two collectors cannot credit the same source. A returned bucket alone is insufficient: the source
must no longer remain at that position after the pickup and its callbacks.

Qualified adapters use the standard liquid pickup, verified native delegations or the owned
fluid-cell attachment. They never replace an arbitrary decorative block with air. Unrecognized
pickup overrides and liquid blocks with block entities are refused. Vanilla water/lava, standard
IE creosote, native waterlogged shapes and owned attachments have targeted coverage; universal
compatibility with every modded fluid is not claimed.

The Funnel alone uses a component-preserving FluidStack tank. Different fluids or components do
not mix. Emptying permits a type change. Legacy water/lava/milk NBT migrates without bucket rounding;
unknown payloads and associated legacy values are retained while fluid operations are locked.
Version 1 stores paid fluid and optional unpaid `Pending` work separately. Manual/GUI bucket
conversion refuses to discard fluid components. Re-entry is guarded through output and bucket IO.

The new transaction is deliberately not wired into the tick until the path range is chosen.
The existing weather contract remains 50/100 mB per 80 ticks in rain/thunder, with native exposure,
biome and anti-mixing checks. Activation must preserve those checks and update the obsolete
Funnel direct-source assertions, while retaining the Accumulator's separate source behaviour.

## Evidence scope

GameTests exercise registered machines, real level fluids, recipes, capability forwarding and
pinned IE pipes. Adversarial capability targets are registered only in a development GameTest
server, never in an ordinary runtime. Transaction-only tests invoke the parameterized private
method; they must not be called proof of the unfinished 40-tick scheduler.

The explicit native diagnostic runs only in an existing nonce-marked cloud test world. It checks
baked source vertices/UVs and captures scenes; its reload mode requires a separate process restart.
Images show geometry and UI, while server/client measurements prove volumes and serialized data.
No user modpack, personal world, alternate renderer, new wood crafting recipe or completed Funnel
world-capture schedule is qualified by the current local captures.
