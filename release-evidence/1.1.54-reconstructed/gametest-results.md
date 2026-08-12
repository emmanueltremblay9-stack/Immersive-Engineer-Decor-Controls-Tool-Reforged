# GameTest results

Baseline: 179 required GameTests.

Current: 182 required GameTests; three permanent collision tests were added and the prior hatch test was expanded rather than removed.

Fresh qualifying command, run twice from clean `main` at `0d766573`:

```powershell
.\gradlew.bat runGameTestServer --rerun-tasks --console=plain
```

Results:

- pass 1: exit 0; `All 182 required tests passed :)` in 3.615 seconds;
- pass 2: exit 0; `All 182 required tests passed :)` in 3.593 seconds;
- validation ledger SHA-256: `4F669BE86C6757C2A31D20AF127837968055B52C340D5B5150CF70E56A120B1F`;
- no failed required test and no orphaned Java process.

The same suite covers manual resources, creative routing under default configuration, conventional mining tags/tools, iron door and trapdoor sound families, solar FE transfer/conservation, and optional JEI compilation/runtime wiring. Client visual or audio quality is not inferred from these tests.
