# Fourteen GUI backgrounds with exterior transparency

PR #196 now uses the exact fourteen PNG exports from
`IEDCTR-14-GUI-alpha-transparent.zip` at the existing runtime resource paths.
Only exterior alpha changes: decoded RGB, protected interior RGBA, metal,
dimensions, alignment, menus and machine behavior are preserved. The outermost
canvas edges are fully transparent; the narrow frame shadows retain graded
opacity. The Fluid Barrel retains its existing interior alpha.

The parent is `09c8a0e8ebb231dd399a525d615c9eedc6516dc4` in
`emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged`.
All fourteen parent PNG byte hashes matched the source archive before copying.
The original versions remain available at that immutable commit.

[manifest.json](manifest.json) records the source and export hashes, resource
paths, dimensions, decoded pixel identities, measured alpha-only checks, archive
identities and verified GUI GOV source revisions. It also records every active
hash-verification text change, with reversible exact hunks. Existing historical
geometry, provenance and preservation manifests retain their source pins.
`SOURCE_SHA256` in the geometry classes continues to identify the deployed PNG;
the original PNG identity is preserved separately in this manifest.

`gui_exterior_alpha.py` permits only those exact export and text identities.
Its narrow reconciliation is used by the existing historical preservation
guards; it neither exempts unrelated files nor accepts arbitrary alpha edits.
All Java changes in this follow-up are asset hash literals, with a regression
check that reconstructs the preceding source and compares the remaining bytes.

Factory Hopper, Factory Dropper, Small Lab Furnace and Small Mineral Smelter
are excluded. No original or reference PNG is regenerated or rescaled. Changes
to asset expectations in native validators require fresh matching runtime
evidence; historical native screenshots, workflow results and JAR hashes do
not qualify these exports.

Validation of this integration is recorded in [validation.json](validation.json).
Local mapped compilation and Minecraft execution are not established by image
checks or the Python suite. The Gradle wrapper could not download its missing
distribution in the local environment. Existing PR workflows must qualify the
new source and JAR. No merge or release is part of this follow-up.
