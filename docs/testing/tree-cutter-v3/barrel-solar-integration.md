# Tree Cutter V3 integration with Barrel V5 and Solar V2

This bounded reconciliation combines Tree PR173 head
`96ac07aac200e10e9936b0ae4ae99f9053fae11c` with main
`890958ff300e9a88dde2a77fc6c145d19c0f98f9` (Barrel PR166 and Solar PR167).
It does not authorize merging the PR or publishing a release.

## Production scope

The three `MachineMenu` projection sites retain both Tree and Solar geometry
blocks. Their existing client-only, machine-kind-specific flags remain
independent. Barrel V5 and Solar V2 remain the ordinary default presentations,
with their explicit-false diagnostic fallback. Factory Placer V2 and Tree V3
remain their ordinary client presentations.

Tree's single output, 27-position backing save, versioned narrow migration,
drain-only recovery, artwork, physical-input fixture and evidence validator are
unchanged from the Tree head above. No FE, Milking or Freezer mechanics are added
or redesigned by this integration.

## Preservation and evidence guards

The corrected-machine GUI validator now requires both branches' declarations:
Tree schema 2 with independent Tree qualification, plus explicit legacy-only
Barrel and Solar diagnostic labels. Its fixture includes all of those keys.
The old Tree badge-only proof is still rejected.

The original Solar preservation manifest's `files` map, baseline commit and
baseline tree are retained. `tree_cutter_integration` records only three exact
exceptions with their old and new hashes and reasons:

- `MachineBlockEntity.java` and `MachineLayout.java` are byte-identical to the
  pre-integration Tree head, including its already-reviewed Tree-only changes.
- The Barrel wiring guard permits only the exact new Tree presentation accessor.

Both wiring guards normalize exactly one complete Tree accessor from the menu
suffix and still require the original suffix hash. They do not exempt general
methods or whole menu sections. The preservation helper independently pins all
three integrated file identities and the historical manifest map. Changing a
file and refreshing its manifest entry cannot admit different integration code.

Regression tests cover missing/changed/duplicated accessors, unrelated menu
changes, mutations in every preserved file, rewritten/dropped historical pins,
missing/extra exceptions, forged source provenance, and changed integration
files with recomputed manifest hashes. The routing matrix includes Tree on both
sides and with all Barrel/Solar selector combinations. Each of the three
projection blocks is checked against its corresponding geometry and indices.

## Qualification boundary

Source and dependency-free tests are not native gameplay qualification. The
existing workflows must compile, run mapped JUnit/GameTests, and qualify the
combined final head with its own JAR hash and evidence. Successful runs and
artifact identities from the pre-integration Tree head remain historical and
must not be presented as verification of this integrated head.

No claims are added for drag/shift-click, natural world/chunk reload, actual
user-world migration, rendered tooltip qualification or owner visual acceptance.
