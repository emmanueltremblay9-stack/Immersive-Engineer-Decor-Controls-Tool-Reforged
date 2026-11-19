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
