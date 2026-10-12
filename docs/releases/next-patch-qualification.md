# Next patch: qualification record and draft release notes

Status: preparation only, not a release authorization or completed qualification.
Observed 2026-10-12 UTC. The proposed next patch number is
`1.1.57-reconstructed`; it is not reserved, configured, or published here.

## Source and evidence boundary

- Inspected main commit: `e9beae1ba5bc5c1800f59ba1787f77268b6555b5`, after PRs
  [35](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/35),
  [36](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/36),
  [37](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/37),
  [38](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/38),
  [39](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/39), and
  [40](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/40).
- [Build run 38187269438](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/38187269438)
  is verified completed/success for that exact main commit. Its reported results
  are 216 required GameTests and 32 publisher tests. These are CI observations,
  not a new local client or packaged-server run.
- CI-reported runtime SHA-256:
  `b04793dbe9c2f705f8b2206f8843b9293e1d5f3ff9b8a3e9cfa2e4170c6d49b7`.
  The bytes were not obtained and independently hashed during this preparation.
  The build still uses version `1.1.56-reconstructed`; this digest must not be
  reused as the identity of a future differently versioned release.
- Latest public baseline observed on the [CurseForge project](https://www.curseforge.com/minecraft/mc-mods/immersive-engineers-decor-controls-tools-reforged)
  is [1.1.56, file 9042960](https://www.curseforge.com/minecraft/mc-mods/immersive-engineers-decor-controls-tools-reforged/files/9042960).
  No new upload, release, project edit, or issue closure is part of this work.

## Draft player-facing notes

This patch fixes several placement, collision, selection, and rendering problems:

- Factory Hopper placement/output direction and extraction from adjacent vanilla
  inventories (#22)
- Steel Floor Grating collision height (#23) and Steel Double T Support collision
  geometry (#24)
- Panzer Glass Slab translucent render declarations (#21) and Exit Sign selection
  outline, retaining its empty collision (#33)
- Connections between decorative walls and Steel Mesh Fence, including mixed
  connections and vanilla walls (#18)
- Iron Hatch hinge selection when placing against floors, ceilings, and walls (#19)
- Structured Vertical Clinker Slab orientation, removing the unwanted gap for
  placement against a wall directly ahead (#20)
- Door Sensor selection outline aligned with its model (#25)
- Industrial Switch cabinet placement, outward facing, two-block selection and
  collision, linked interaction/redstone, and single-item drops (#26)

### Existing worlds and limitations

Back up a world before updating. Existing Industrial Switch cabinets keep their
legacy state and are not automatically converted. Break and re-place an old
cabinet in clear two-block space to obtain the paired layout and corrected
outward orientation. Legacy cabinets cannot provide upper-half selection without
a real upper block. The update does not scan worlds or overwrite occupied space.

Already placed hatches and vertical slabs are not automatically rotated. Existing
wall connections may need a neighbor update to refresh. Steel Mesh Fence remains
a wall-based block. Hopper extraction supports adjacent vanilla `Container`
inventories; inventories exposing only NeoForge `IItemHandler` are not supported
by this extraction path. Door Sensor mounting appearance and empty collision are
unchanged. No exhaustive reporter-modpack compatibility claim is made.

## Remaining release gates

1. Resolve the public license discrepancy before declaring rights verified:
   repository `LICENSE`, packaged metadata, and README say MIT, while CurseForge
   presently says All Rights Reserved. [Issue #34](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/issues/34)
   records the mismatch. Preserve all upstream notices; this patch does not change
   licenses or establish legal clearance. Authenticated authority to upload to
   project 1555214 also requires a separate actual verification.
2. Select the final version and source commit; run the full Build workflow on that
   exact commit. Independently hash the actual private release JAR, check size,
   embedded mod/version and packaged notices, and create the reviewed manifest
   from those bytes. Do not infer an artifact identity from an earlier CI log.
3. Run client inspection using those bytes: transparent slab rendering, grating
   and support traversal, hopper input/output, mixed wall connections, hatch and
   slab orientations, Door Sensor selection, and new/legacy Industrial Switches.
   Include save/reload and break/re-place behavior. Preserve logs/screenshots
   labelled with the exact JAR hash.
4. Run an actual packaged dedicated server with the same JAR and required
   Immersive Engineering dependency; check startup, placement/redstone/loot,
   save/restart, and absence of client-only class-loading failures. Development
   GameTests do not replace this gate.
5. Review the final notes and hash-pin them. Run the CurseForge-only local-source
   dry-run using the reviewed source/CI/byte identities. A dry-run is not a
   publication or a client/server validation.

The current cloud workspace has Java 21, but no installed Gradle distribution or
populated Gradle dependency cache was found. No dependency installation or user
computer access was attempted in this preparation. Client visuals and packaged
server validation therefore remain unperformed here.
