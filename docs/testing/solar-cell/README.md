# Solar empty-slot hint (issue #152)

The issue reports untranslated “Cellule” / “Réserve” and asks what belongs in the cell slot. Its author's exact binary version is unknown. The retained current faceplate has no baked labels; this does not establish which build the reporter saw, nor resolve the historical report by itself.

The remaining empty-slot hint was the literal layout role `Cell`. Replace only that empty slot hint with a wrapped translatable explanation, with explicit en_us, fr_fr and fr_ca entries. Filled slots keep vanilla item tooltips. No art, slots, acceptance, charging or other gameplay changes.

The explanation follows MachineBlockEntity.isEnergyItem (EnergyStorage.ITEM capability) and the receiveEnergy charging path. It does not claim universal item charging. Existing native acceptance/charging checks use the IE LV capacitor and reject stone.

The existing EN/FR 320×260 scale-1 FE tooltip profiles now continue with an empty-cell XTest pointer hover, observe the actual tooltip render components for at least three consecutive frames, save a separate framebuffer capture and proof, and independently compare the observed text with the selected locale resource. Bounds, style, capture hash, viewport and nonce/source identity gates remain. fr_ca has explicit text regression coverage, not a separate native profile.

This changes production code/resources and therefore requires a new candidate JAR hash and exact-head native CI. Historical candidate 53ff03f30cb0c8921f4a8fdb205788cba567aab515804c7d2c7a0ab2daf6f98f proofs do not qualify this changed binary. Python/synthetic mutation and extracted Java-body tests are regressions, not a native PASS. No issue closure or reporter-version claim is made.

## Preservation reconciliation

`solar_cell_integration.py` is an additive identity layer over main 630cf7ba. It pins both previous and new bytes for each locale, then removes only the exact appended entry and rechecks the previous byte hash. Existing Solar/Tree/Milking/Freezer historical manifests and hash constants remain unchanged. The Freezer preservation chain projects this layer before its existing historical checks; its current-byte lookup admits only the recorded baseline-to-current transition. Negative tests reject altered prior text, altered new keys, extra bytes and unknown baseline hashes.
