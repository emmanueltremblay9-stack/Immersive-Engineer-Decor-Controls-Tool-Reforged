# K/X MK2 common-code integration — 2026-11-17

## Scope

Recovered engine and transaction bridge are now compiled as common Java code and
covered by JUnit. `KXRuntimePlacement` recomputes canonical occupancy before writes,
keeps `KXPlacement` as the only placement/economy transaction owner, and uses the
bridge's bounded invalidation footprint for cells and remote endpoints. It reports
sync failure separately and does not retry item consumption after a sync failure.

This is NOT a playable block. No registry ID, BlockEntity, inventory/world adapter,
model bake, game interaction, or client renderer is registered. No historical
Double-T source, registry, resource, native fluid schema or pinned validator is
changed. PR #199 Heavy Double-T remains separate.

## Provenance

Original engine archive: `IEDCTR_KX_MK2_PLACEMENT_MECHANIC_CANDIDATE.zip`
SHA-256: `e62c75e72cb30d65104404ad926635bab06469cf34fd25c8f4e83a76ac9c634a`

Original bridge archive: `IEDCTR_KX_MK2_PLACEMENT_BRIDGE_CANDIDATE.zip`
SHA-256: `b56bbb2de73e4e62c53e4452fbf1c711a4ab84d81e631ecde99d7c24de1f4be6`

Recovered from the author's New Block Model Idea and Créer mécanique KX Java
conversations. Original archives are preserved unchanged. Engine sources are copied
with one explicit inventory contract comment. Harnesses are renamed to `*Checks`
and invoked by JUnit, avoiding accidental discovery of non-JUnit `*Test` classes.
The generic coordinator remains unchanged. New seam is `KXRuntimePlacement`.

## Executed validation

- Java 21.0.13 compilation of all common classes and harnesses: PASS.
- JUnit 4.13.2 `KXIntegrationTest`: 3 tests PASS, 451 assertions
  (222 recovered engine + 24 recovered bridge + 205 new seam).
- Separate recovered fixture parity run: 67 assertions PASS, 8 models/24 rotations.
- Independent static seam review: no blocking defect found.
- Full Gradle compile/test: BLOCKED before tasks by network-unreachable download
  of the wrapper distribution. This is NOT a successful NeoForge build.
- Minecraft GameTests, block registration, placement, persistence, client visuals,
  textures/CTM and new JAR: NOT_RUN / NOT_IMPLEMENTED as applicable.

## Remaining integration gates

1. Choose/review independent K/X identity and persistence schema without replacing
   existing Double-T IDs. No choice is implied by this common-code staging.
2. Implement actual world/inventory adapter: permissions, loaded chunks only,
   complete state/BlockEntity/fluid snapshots, safe drops/events and compensation.
3. `consumeItems` MUST be atomic: false/exception leaves inventory unchanged.
   The existing core rollback only restores world cells. Qualify inventory behavior
   with failure-injection tests before using it in Minecraft.
4. Persist reciprocal edges; notify both ends on removal and reload; defer unloaded
   endpoints without forcing chunks.
5. Qualify 24-way rendered geometry and exact owned segment collisions separately.
6. Run exact-commit Gradle, Minecraft GameTests and client checks before calling
   integration complete. This draft must not be counted as a released K/X feature.
