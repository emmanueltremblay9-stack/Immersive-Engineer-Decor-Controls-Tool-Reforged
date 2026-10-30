# Factory Hopper transfer and redstone state

Issue: https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/issues/22

Base: `7fc9eeb51aaac5fc57b8952eee67f2eda7da55e6` (`main`, 1.1.65-reconstructed).
The placement rotation was already corrected by #35. The remaining current defects are established in
`MachineBlockEntity.pushItems` / `pullItems`: they only inspect neighboring block entities implementing
`Container`, excluding capability-only inventories, and access container slots without checking the
contacted face. The existing chest/barrel regression is not evidence of a current general vanilla
transfer failure. No claim is made about reproducing the reporter's entire 1.1.58 modpack.

## Change

- Query `Capabilities.ItemHandler.BLOCK` on the neighbor face pointing toward the hopper. Re-query
  each cycle so replacement/changed capabilities are respected; do not load an absent neighbor chunk.
- Preserve legacy Container-only inventories through `InvWrapper` / `SidedInvWrapper`, retaining
  `canTakeItem` and sided slot restrictions.
- Simulate source extraction and internal capacity before extracting. Insert the actual extraction
  result, and subtract only what the destination actually accepts. Preserve output if a handler changes
  the extracted item type between simulation and execution by dropping any unexpected internal remainder.
- Keep the existing transfer count, round-robin output cursor, collection, timing, defaults and redstone
  bit flags. No new redstone mode or pending-pulse queue is introduced.
- Show the current inversion and trigger state in English/French tooltips (including French Canadian).
  Other locales receive the same six English fallback values explicitly to preserve the repository's
  locale-key parity requirement. Explain the existing controls in the English manual.

## Contract

Output follows `FACING`; inventory intake is opposite it. Each neighboring capability receives the
opposite of the direction from the hopper to that neighbor. Each cycle can extract and insert up to the
configured count (1–32). The existing period is `10 + 2 * HopperPeriod` ticks and is checked every ten
machine ticks, so non-multiples of ten round up to the next check.

The default logic value is 3: inverted + continuous, operating while unpowered. Values 0/1 are
normal/inverted pulse, 2/3 normal/inverted continuous, and bit 4 ignores external redstone and repeats
regardless of the pulse toggle. Pulse requires an active rising/falling edge at the ten-tick check and
an expired cooldown; edges during cooldown are consumed, not queued. The manual signal button bypasses
redstone and shortens remaining cooldown to at most one tick. These behaviors are preserved.

## Regressions

`FactoryHopperTransferGameTests` adds six real NeoForge GameTests:

- `vanilla_chest_to_barrel_preserves_default_mode_and_partial_stacks`
- `capability_only_transfer_uses_facing_and_opposite_neighbor_faces`
- `capability_faces_rejection_and_replacement_are_respected`
- `partial_handler_execution_and_full_storage_preserve_items`
- `furnace_sided_slots_prevent_output_insertion_and_top_output_extraction`
- `redstone_modes_manual_trigger_and_cooldown_keep_existing_contract`

Capability fixtures use bedrock with no block entity and a face-restricted provider. The provider is
registered only when `neoforge.gameTestServer=true`, following the existing fluid fixture pattern.
Each installed handler is removed in try-with-resources. No normal-world provider is registered.

Existing `MachineGuiGameTests` retain coverage for placement, chest/barrel transfer, full storage,
comparator updates and loose-item redstone collection. The unchanged Build workflow runs all tests.

## Validation status

Local Java 21 / NeoForge 21.1.230 dedicated-server comparison on 2026-10-29:

- Initial corrected replay: 12/12 required tests passed (five new hopper tests plus seven IE tests).
- Before: substitute only `MachineBlockEntity.java` with the exact main blob
  `bd9202be97bcc93b0abfbcf0fa2685742f2a919a`. All six new hopper tests and seven IE tests ran;
  five required tests failed, while the positive vanilla transfer passed. Gradle exited 1.
- After: restore corrected production blob `1d020dd46a66891dd397729eadea627c62c97c2f`.
  All 13 required tests passed; Gradle exited 0. This includes all six new hopper tests.
- The final replay compiled the restored source and reused the resources prepared during the prior
  run (`-x processResources`); those resources were unchanged. Java compilation passed.
- Locale JSON parsing and `git diff --check` passed. The repository localization validator exited 1
  on both candidate and baseline: identical sets of 1,064 pre-existing errors (943 missing keys,
  117 newline mismatches and four other errors), with zero errors on the new hopper-state keys.
  It was not treated as a passing global localization gate, and unrelated errors were not modified.

The local replay uses the existing `runGameTestServer` task with an untracked Gradle init script
under `build/hopper-validation/`. It copies compiled production classes unchanged to a test runtime
directory and excludes unrelated mod GameTest classes from discovery, retaining only
`FactoryHopperTransferGameTests` and `ItemHandlerTestTargets`. IE's seven dependency tests remain
present. The game directory is `build/hopper-validation/run`; no installed instance is used.
The main-source substitution for the before replay was restored in a PowerShell `finally` block,
then verified by blob hash. The init script, before/after console output and server logs are local
evidence delivered with the change, not repository or CI configuration changes.

Full repository tests, clean build, renderer checks, GUI tooltips and gameplay in the reporter's
modpack were not validated locally. Full CI remains required on the actual published PR commit.
Remote publication was denied by automatic approval review: the instruction requiring separate
approval for new publication was interpreted as covering a new branch even for the authorized draft PR.
No branch was pushed, no PR was created and no GitHub CI result is claimed. Issue #22 stays open.
No Prism instance, installed mod, personal world, release, workflow or PR #88 was modified.
