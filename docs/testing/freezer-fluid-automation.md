# Small Water Freezer fluid and bucket automation

## Problem and scope

On baseline `fdf26aadc45580b7ca4211a32323125f3c276d5b`, the Freezer exposes item and energy capabilities but no fluid capability. IE pipes therefore cannot connect. The water-bucket item recipe leaves an empty bucket in input slot 0, while item capability extraction permits only output slot 2.

This patch repairs those two automation paths. It does not import Freezer V5 or Barrel V5 presentation work, change their defaults, change recipes/dependencies, increment a version, or authorize a release.

## Contract and rationale

- Accept plain vanilla water through all six faces and unsided capability access. Capacity is 2,000 mB, matching the original Engineer's Decor Freezer. Reject other fluids and component-bearing fluid stacks rather than silently losing their data.
- When input slot 0 is empty, a complete 1,000 mB water batch may freeze into one Ice in output slot 2. Water is consumed only on completion; each cycle retains the current 180 ticks at 8 FE/tick. Partial water waits without energy use. Two full batches yield two ordinary Ice blocks.
- An occupied slot 0 retains priority. Water Bucket, Ice and Packed Ice keep their current recipes; invalid input and the empty-bucket remainder block the fluid recipe until removed. Tank water is not also charged for item recipes. Slot 1 remains reserved for recovery of legacy contents.
- Automation may extract output slot 2 and only an empty bucket from slot 0, on every face. Both item capability and vanilla container paths follow this rule, so a recycling hopper cannot remove an unprocessed water bucket. This intentionally narrows previously unrestricted vanilla extraction of Freezer input/reserved slots; manual GUI recovery remains available.
- Redstone HIGH, insufficient FE and blocked output retain the current reset behavior. Switching between item and tank sources starts a fresh paid cycle. Draining an active fluid batch below 1,000 mB invalidates its work immediately, even if refilled before another server tick. Simulated transfers do not mutate state.
- The additive `FreezerFluidProcess` save marker identifies tank-funded progress. Older saves without it retain valid item progress. Fluid amount/type are capacity- and type-checked on load. There is no registry, slot-index or blockstate migration.
- Existing menu field 2 carries the bounded tank amount; field count remains 16 and slot count 39. Preview/status use the same source selection as the server. No new presentation controls, artwork or protocol length are introduced.
- Direct hand use of a water bucket still spends 1,440 FE immediately, returns the player's empty bucket and inserts Ice independently of the tank. The comparator still indicates freezing phase, not tank fullness.
- The original mod's automatic Ice/Packed Ice/Blue Ice phase progression is not imported: it would change the current reconstructed mod's explicit item recipes. This is fluid-input restoration, not a recipe redesign.

## Source references

- [Original Engineer's Decor Freezer, pinned 1.19 source](https://github.com/wilechaote/EngineersDecor/blob/a561e61e35ce0367088356967e1ea4cad5c1f27d/src/main/java/wile/engineersdecor/blocks/EdFreezer.java): 2,000 mB water tank and fluid capability independent of face.
- [IE pipe, pinned 12.4.2-194 source](https://github.com/BluSunrize/ImmersiveEngineering/blob/583a182549d284a35c813b55ac2a0d1fddcf945a/src/main/java/blusunrize/immersiveengineering/common/blocks/metal/FluidPipeBlockEntity.java): neighboring fluid capability with a nonzero tank count determines connection availability.
- [Baseline machine implementation](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/blob/fdf26aadc45580b7ca4211a32323125f3c276d5b/src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineBlockEntity.java).

## Automated qualification

`FreezerFluidAutomationGameTests` contains 13 tests using registered public capabilities and real server ticks. Coverage includes every side, capacity/partial/overflow handling, rejected fluids/components, defensive stack copies, both drain APIs, simulation, exact resource costs, interruptions, source changes, saved-state normalization, legacy item progress and contents, real hopper bucket/ice recovery, unchanged direct use and menu fields.

The IE integration test places the actual registered IE pipe, checks its connection bit, transfers water through its real fluid handler on each face, saves/replaces/reloads the Freezer, finishes the paid cycle, extracts Ice and verifies that the pipe can route to the replacement block entity. This is not a natural chunk-unload test, a physical pump test, or a conveyor motion test.

Run the normal Java/unit/dedicated GameTest suite and existing regression validators. Qualify the exact branch commit, not only the PR merge ref. At authoring time the local Gradle wrapper could not download its distribution due to network unavailability; syntax-only inspection is not compilation and CI results must be reported separately.

## Remaining manual client checklist

On a disposable world with the candidate JAR and pinned IE dependency:

1. Connect a powered IE fluid pump and pipe to each Freezer face. Confirm the pipe's visible connection, water delivery and Ice production with FE supplied.
2. Use the normal GUI to verify the Ice preview, progress, insufficient-energy, redstone and output-blocked states while filling by pipe. Check the existing presentation without opt-in flags.
3. Feed Water Buckets with an IE conveyor. Recover empty buckets and Ice with configured extraction; verify full buckets are not removed before processing and repeat multiple cycles.
4. Save/quit/reload and naturally unload/reload the chunk mid-cycle. Confirm stored water, paid progress, pipe reconnection and one-time consumption/output.
5. Compare direct hand-bucket behavior, Ice/Packed Ice item recipes, comparator, reserved-slot manual recovery and existing saves against the baseline.

Record the tested commit, JAR SHA-256, Minecraft/NeoForge/IE versions and actual observations. Do not claim this client checklist passed solely because server GameTests passed.
