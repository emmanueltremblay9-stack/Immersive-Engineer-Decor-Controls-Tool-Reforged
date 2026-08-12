# GameTest results

Baseline: 179 required GameTests.

Current: 182 required GameTests; three permanent collision tests were added and the prior hatch test was expanded rather than removed.

Final qualifying command (executed twice consecutively on the stabilized 1.1.54 source):

```powershell
.\gradlew.bat runGameTestServer --rerun-tasks --console=plain
```

Results:

- pass 1: exit 0; `All 182 required tests passed :)` in 3.218 seconds;
- pass 2: exit 0; `All 182 required tests passed :)` in 3.208 seconds;
- final `latest.log` SHA-256: `5F330C2F331FC310EE7ED5350478EE96CEC79A597CA40D5DB063F7456E65AF8A`;
- no failed required test and no orphaned Java process.

The only keyword-audit matches were the two known NeoForge development asset-URL schema warnings; no collision, VoxelShape, AABB, registry, datapack, capability, client-only-class, fatal, or exception record was found.
