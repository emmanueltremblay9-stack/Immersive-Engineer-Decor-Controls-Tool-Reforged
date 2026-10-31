# Native TFC cow milking (issue 109)

This draft targets the reported operation: a TFC cow supplies the Small Milking
Machine's tank without requiring an input bucket. The reporter did not supply
their exact TFC/Crossover versions. The explicit development target is the
official TFC 4.2.12 release for Minecraft 1.21.1, not an inferred reporter stack.

## Transaction and existing behavior

- Only the registered entity `tfc:cow` is eligible for the optional adapter.
  No general livestock tag, new species or new redstone mode is introduced.
- Native `isReadyForAnimalProduct` checks the configured familiarity threshold,
  sex, age and calendar cooldown. Readiness and range are checked again after
  callbacks. The unchanged vanilla `Cow` path remains independent.
- A noncreative, machine-specific FakePlayer emits
  `CommonHooks.onInteractEntity` once per candidate attempt. It is empty-handed:
  no synthetic bucket is created. The public native fluid `AnimalProductEvent`
  also receives an empty tool, representing automatic tank production rather
  than a player-held vessel. Protection cancellation stops the operation.
- The event's actual fluid amount and uses are honored. The machine's legacy
  storage represents plain NeoForge milk only. Non-milk fluids, component-bearing
  milk, empty/item products, excessive amounts and insufficient capacity are
  rejected before our cooldown/uses update or input debit. No incompatible
  product is converted into ordinary milk, partially consumed or discarded.
- A complete 1000 mB plain-milk product can use one real input bucket if its output
  slot accepts the result. Other representable complete amounts go to the tank.
  Local inventory/tank fields are committed before native cooldown/uses, without
  neighbor callbacks in between. Normal synchronization/output follows afterward.
- A guard covers the producing animal across machines and blocks same-machine
  callback recursion, including bucket I/O. Hook-produced actor inventory items
  are retained as actual world drops; the machine supplied no items to that actor.
- The existing 4000 mB capacity, 120-tick unpowered production check, facing/range,
  20-tick container I/O, 250 mB fluid output and vanilla bucket/tank behavior remain.
  No new persistence schema is required; existing scalar milk saves remain valid.

This is a server-thread transaction, not an atomic save transaction across two
independently saved entities. It does not roll back arbitrary third-party event
side effects or promise crash atomicity between animal and block saves.

## Optional dependency boundary and pins

The common machine calls a boundary with no TFC types in its signatures. A nested
holder links the adapter only when TFC version 4.2.12 is loaded. Other versions
continue loading normally with this preview adapter inactive. No TFC code is
copied, bundled or made a mandatory runtime dependency.

| Dependency | Official release pin | SHA-256 |
| --- | --- | --- |
| TFC 4.2.12 | `maven.modrinth:JaCEZUhg:2gXJo8Ol` | `3c2d80e47c78cfbd617823ebe0d3d5e2f9ba9e7f04c1b34867dbab3894584440` |
| Crossover 2.1.3 | `maven.modrinth:IsYwQTKM:q5cvg8JU` | `9c10394225be5e9cdab845cfb6192b88beb4cb6b21b66e3caeedf9c4982b486b` |

Gradle verifies these exact binary hashes before compilation. TFC's actual release
metadata requires NeoForge >=21.1.234 and Patchouli >=1.21.1-92-NEOFORGE.
Only the dedicated development profile selects NeoForge 21.1.234 and fixed
Patchouli 1.21.1-92-NEOFORGE. The ordinary project profile remains on 21.1.230,
without TFC, Patchouli or Crossover in its runtime.

Sources:

- [TFC official release](https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/tag/v4.2.12)
- [Native dairy source](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/c3b4d9536b82e5a9746dd793c586720a74108057/src/main/java/net/dries007/tfc/common/entities/livestock/DairyAnimal.java)
- [Native product event](https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/c3b4d9536b82e5a9746dd793c586720a74108057/src/main/java/net/dries007/tfc/util/events/AnimalProductEvent.java)
- [Official Crossover release](https://modrinth.com/mod/tfc-ie-crossover/version/2.1.3)

## Validation

The ordinary Build workflow retains the absent-TFC startup and vanilla regression
coverage. The separate TFC workflow runs twelve real GameTests in both profiles:

```text
./gradlew runGameTestServer -Pinclude_tfc_milking_runtime=true
./gradlew runGameTestServer -Pinclude_tfc_milking_runtime=true -Pinclude_tfc_crossover_runtime=true
```

The development profile adds `src/tfcGameTest/java`, copies the existing 110-byte
empty structure into its dedicated template namespace, and selects the
`immersive_engineer_decor_controls_tool_reforged_tfc` test namespace.
The workflow also requires evidence of the twelve-test `tfc_milking` batch and
successful required tests, so discovering zero native tests cannot count as success.

Cases cover native tank/cooldown, native eligibility, strict familiarity threshold,
both cancellation hooks, changed amounts/uses, incompatible fluid/components/item
products, capacity/power/animal changes during callbacks, two-machine recursion,
bucket conservation, real entity and block save/reload, range/redstone/vanilla,
and foreign actor output retention without fabricated buckets.

Local common-source compilation with the official TFC API passed, followed by
compilation of all twelve real GameTests in the NeoForge 21.1.234 profile.
The local server preflight was interrupted by an executor disconnection during
resource preparation: no server log or passing test result was produced, and its
Gradle daemon was confirmed stopped before publication. Real TFC/Crossover server
tests and full absent-TFC CI remain required on the published draft commit.
No reporter-modpack gameplay or compatibility with other TFC versions is claimed.
No Prism instance, installed mod, personal world or release was changed.
