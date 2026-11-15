# Milking ordinary-JAR qualification (not yet native-qualified)

Target runtime: main `97883bec49afbc65bb4cc8270987f44686a6a7e1`, ordinary JAR SHA-256
`53ff03f30cb0c8921f4a8fdb205788cba567aab515804c7d2c7a0ab2daf6f98f`.
The future workflow head is the tooling revision, not a claim of new production code.

Existing PR188 source-native Milking evidence does not establish loaded JAR provenance.
This separate development mod adapts the same fixture, preserving every 38-slot route,
conversion/server acknowledgement, single-click interval, milk-state capture and pixel gate.
The original fixture, renderer, menus, gameplay and source-native workflow are unchanged.
`test_milking_packaged_driver.py` reverse-normalizes only namespace/selector and provenance additions
and requires full source equality with the original fixture.

The normal JAR is built first without the harness profile. A distinct source set compiles
against it and excludes main output from the runtime classpath. The verifier rejects
split packages, leaked new harness classes, duplicate ZIP entries, additional development
mods and any supplied JAR differing from the normal build. The five-profile CI additionally
requires the target hash above, rather than silently qualifying a changed JAR.

At boot after nonce/directory validation, and again after all cases, runtime proof checks
actual production ModFile, JAR bytes, six class origins and both active textures against
the same archive. The separate harness module and absence of all production selectors
are mandatory. Historical dormant main validation classes may remain in the ordinary JAR;
“fixture_free_jar” means the new `iedctrtest` namespace is absent, not removal of old classes.

Run only from a clean committed checkout with existing Linux/Java21/Xvfb prerequisites:

```
python tools/validation/milking_packaged_native.py --profile fr-1280x260-scale1 --expected-jar-sha256 53ff03f30cb0c8921f4a8fdb205788cba567aab515804c7d2c7a0ab2daf6f98f --prepare-timeout 900 --timeout 900
```

The dedicated workflow covers all five original profiles. It records ordinary JAR, full
runtime report, source, PNGs, proof and logs. No server installation, merge or publication.
Input remains `Screen.mouseClicked/mouseReleased` plus vanilla packets/server observations,
NOT physical OS input. Reload/reopen, shift-click, complex gestures, JEI actions, full visual
acceptance, TFC compatibility and Smelter qualification remain outside this cycle.

Local Python/mutation tests do not establish Java compilation or native pass. Those remain
required on the exact workflow head; no old PR188 source run is relabelled as packaged proof.
