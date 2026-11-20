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

### Second exact pin: Dropper-only MachineLayout inverse

The first corrective CI identified a second **historical full-file preservation guard** on `MachineLayout.java`. No historical pin was changed. `factory_r04_layout_inverse_edits.json` restores the complete `main` layout with four precisely anchored inverses: `playerSlotX`, `playerSlotY`, `hotbarSlotX`, and `dropper()`. The projection accepts only the committed layout Git blob `606ff668080cf82adcf256e309ae15bcede16f99` and requires reconstructed original Git blob `710069d40e28dba9caa13e54d47f5e6ad573937f`. All other paths remain raw. Negative tests mutate Hopper, Metal Crafting Table and Dropper lookup plus the layout recipe. This is additive to the five-edit screen-only inverse; it does not update global historical pin values.

First corrective commit `d810f007d7204368df6243bf0ac0294ed3cec058` showed Milking and Solar preservation preflight stopping on `MachineLayout.java` after the screen projection, while its Java compile and JUnit test stages had passed and the GameTest stage was still executing when this note was prepared. These historical observations do not certify the later commit.

### Fail-closed historical fixture compatibility; real native Dropper hover

The first layout inverse run `ced5230db28f7dfa4c4404fd785d82bd37a6adc7` advanced Solar/Milking until a historical test-only `unittest.mock.patch.object(Path, 'read_bytes', ...)` intercepted the R04 recipe file and raised `KeyError`. The exact recipes are now loaded via a context-managed `Path.open('rb')`, which is not part of the mocked list of historical production bytes. Both recipes' Git blob hashes remain checked exactly. Existing historical test mocks and pins are unchanged, and tampered recipes remain rejected by explicit negative tests.

The packaged Dropper factory tooltip harness from `main` timed out in `HOVER` on `d810f007`; its first clean capture was present and logs identified `IEDCTR_FACTORY_CONTROL FAILED at HOVER`. The old harness targeted `(135,20)`, outside the current R04 Dropper velocity hitbox. Reuse the three **Factory-only test/validator/fixture files** from `#196@09c8a0e8` to align XTest hover points and require true synchronized 2→3→1→17 Dropper switch transitions and three Hopper redstone modes, with unchanged negative proof gates. These fixtures do not enter the ordinary mod JAR or alter production interactions. This is a new candidate CI test, **not** transfer of historical #196/#204 native evidence.

All red CI families remain separately qualified on the new exact commit. A prior success on an old JAR, unchanged graphics, or historical screenshots cannot establish a post-fix PASS.
