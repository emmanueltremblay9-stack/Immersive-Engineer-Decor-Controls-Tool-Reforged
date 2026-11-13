# Freezer V5 opt-in reconciliation with current main

This is a preservation/requalification change, not production activation or a
finished dynamic-frost GUI.

Parents: Freezer V5 `f214af10595d340d37610888f23c099ec739834e` and main
`28a70f534323511e38dcc99453d4019af9576a77`. The additive
`freezer_v5_reconciliation` record in both existing preservation manifests pins
these identities, their merge base, and the exact reconciled bytes of
`MachineMenu`, `ClientModEvents`, EN/FR language resources and the Barrel wiring
guard. The guard was absent from the old Freezer parent, represented by JSON null.

All previous manifest sections, including current-main pins, remain identical to
the main parent. They are historical parent evidence, not silently reassigned
current-byte claims. Guards require the new record independently, check its fixed
canonical digest and current file bytes, and admit only the five exact paths and
known main-parent hashes. Coordinated edits to source plus editable JSON hashes
are rejected. The existing Tree, Freezer-fluid and Milking integration records
retain their independent digests and historical reconstruction tests.

The menu-tail normalization admits exactly the Freezer presentation accessor and
reserved-slot visibility override, each once. Everything else in the historical
transfer/data tail remains byte-pinned. The selection-expression test now includes
both Freezer flags and all Barrel/Solar option combinations, client/server sides
and five machine kinds. The public-constructor path still explicitly passes four
false presentation options. Production defaults for the existing Barrel, Solar,
Tree and Placer routes are preserved; Freezer V5 remains explicit opt-in.

Local validation is not mapped compilation, native execution, or final visual
acceptance. Fresh exact-head CI and native evidence are required after publication
of the reconciled branch. Static frost and the missing aligned clean/frost layers
remain unchanged constraints.
