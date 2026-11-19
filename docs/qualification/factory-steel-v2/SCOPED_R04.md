# IEDCTR Factory Hopper + Factory Dropper R04 — scoped candidate

**PROPOSED / NOT APPROVED / NOT RELEASED**. Parent `main@e8b21c91d5a7a3a6beb9d0b12c71e8bccc0f8d2c`; Factory source reference `#196@09c8a0e8ebb231dd399a525d615c9eedc6516dc4`; exact artwork/provenance `#204@4e5c1b68c0fed9e091438e26c0c6d3e2ff262f2a`. This branch does not contain other #196 GUI changes.

Only Factory-specific code is selected. `MachineScreen.java` imports the Factory render paths, exact Hopper row pitch, native widget paths, hitboxes and tooltip methods without the furnace/Metal Crafting Table/Crate changes. `MachineLayout.java` adds only Dropper per-slot lookups and factory dropper layout; Hopper and non-Factory layouts remain identical to main.

`FactorySteelFrame.java` is deliberately trimmed: only `slots`, `inset`, `thumb`, `controlSymbol`, `part`. **No `frame()`, `divider()` or out-of-bounds geometry** is transferred.

The two 6× backgrounds use immutable Git blobs from #204 and must have SHA-256:
- Hopper: `14d2a06b9d434a722e67a28c7a3ea1c889d52cbf45236b3678d1248e140622e3` (1056×996)
- Dropper: `9c8517f4a8c6c138b899038de3bb72ed4856676351d6b1dd4972c90b1787baaf` (1368×1500)

`retained_dropper/background.png` is included as an existing layout resource reference, **not** as approved Dropper artwork. The R04 background is loaded from `factory_steel_v2/dropper.png` at runtime. `factory_placer_v2_reference/background.png` and legacy Hopper atlas are reused from main without copies.

**Excluded**: R05, PR #199, the crate/label editor, Metal Crafting Table, both retained furnaces, `MachineMenu.java`, `MachineBlockEntity.java`, `FactorySteelFrame.frame()`, other production gameplay/registration, release packaging and publishing.

Historical R04 evidence belongs to #204/#205 and the original JAR `d34fb6b0be27f45eabacb2c968853487edf48e6f92582951b7ad99ae196fd664`. Any new build here receives a new JAR SHA and its **own** native proof. Existing 35-case matrix remains open: state extremes, persistence, dispensing, scales 3/4, partial tooltip/focus/input checks, and owner visual approval.

Targeted checks: `python -m unittest discover -s tools/validation -p 'test_factory*' -v`; `./gradlew compileJava test build` (Java 21). Full build and game tests are independently recorded, not implied by this source-only preparation. No merge or publication.

## CORRECTIF-206-R04 — exact regression reconciliation (proposed, unqualified)

Base candidate: `5b2453eca29ad06378dab81b1500f23486713185`.
The baseline GameTest's **single** Factory Dropper parent texture expectation now matches the actual unchanged `RetainedDropperLayout.TEXTURE` (`retained_dropper/background`). The test still checks the real menu, slot count and parent texture. `tools/validation/test_factory_r04_inverse_projection.py` compares the one-line reversal to the immutable original complete `MachineGuiGameTests.java` Git blob; unrelated mutations fail.

Historical Factory tooltip, Milking/Freezer, Solar, Tree and Barrel SHA-256 pins remain unchanged. Their MachineScreen checks normalize **only** through `factory_r04_inverse_projection.py`: the exact R04 screen Git blob must match, five explicitly stored inverse edits must be unique and byte-identical to the anchored source, and the reconstructed complete screen must match the immutable `main` Git blob. Only then are the existing historical SHA-256 assertions run. Source changes outside Factory, altered controls, an altered recipe or altered history fail closed. No global allowlist, wildcard or ignored hash mismatch is introduced.

`factory_r04_inverse_edits.json` contains the exact five inverse edits: `renderBg`, Hopper widgets, Dropper click path, Dropper tooltip and removal of the added Dropper rendered overlay method. The immutable original and candidate Git blobs are recorded inside the projection code. Negative mutation tests cover unrelated Milking/Freezer changes, Factory selector/tooltip/action changes, recipe tampering and arbitrary GameTest edits.

Observed pre-fix CI (head `5b2453ec`): `compileJava processResources` PASS; JUnit `test` PASS; `runGameTestServer` FAIL (only `all_machine_blocks_open_machine_menu` parent texture mismatch); Factory controls preflight FAIL at the complete historical SHA guard; Solar/Milking/Barrel preservation preflight FAIL at `MachineScreen.java`; some other jobs stop at the same GameTest before native capture. The French milking pagination job additionally had an unrelated apt mirror sync failure. All post-fix CI, build and native outcomes are **NOT_PERFORMED** until new exact commit evidence exists.

No new artwork, mechanics, slot positions, packets, public release metadata, or edits to #196/#199/#204/#205.
