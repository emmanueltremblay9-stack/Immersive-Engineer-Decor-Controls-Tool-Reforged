# Credits

## Original Projects

- Stefan Wilhelm / wilechaote / Wile / stfwi: original Engineer's Decor, original Gauges and Switches / RsGauges, and original Engineer's Tools.
- Labeled Storage Crate GUI, block model/state and textures are restored from Engineer's Decor 1.1.28 at commit 837b8678f4fb5ebd818d14c5cbb476273be8405b; the original MIT attribution is retained.

Source references:

- https://github.com/stfwi/engineers-decor
- https://github.com/stfwi/rsgauges
- https://github.com/stfwi/engineers-tools
- https://github.com/ialwaysaskedforthat/rsgauges

## Community Port Work Used

- Waffle / ialwaysaskedforthat: RsGauges 1.20.1 port lineage credited by the 1.21.1 port source.
- MCFabikDE / MCFabik: RsGauges 1.21.1 NeoForge port resources used for the controls module.

Source reference:

- https://github.com/MCFabik/rsgauges-port

## Modernization Work

- Emmanuel / OblixorPrime: NeoForge 1.21.1 modernization, package/namespace migration, fusion architecture, build setup, resource cleanup, documentation, and local Prism test packaging.

## Runtime Dependency

- BluSunrize and Immersive Engineering contributors: Immersive Engineering, required at runtime. This fork depends on IE for referenced industrial assets, manual behavior, and FE compatibility; IE remains a separate upstream project under its own license.

## User-provided GUI artwork

- Small Fluid Funnel reference artwork supplied by the repository owner for this integration. The Toolkit code is MIT-licensed at the pinned commit, but no separate artwork license statement was supplied; this attribution records provenance without claiming a separate license.
- Passive Fluid Accumulator reference artwork supplied by the repository owner for this integration. No separate artwork license statement accompanied the PNG; this records provenance without asserting a separate license.

## Reviewed fluidlogging reference

- Leximon, NightEpiphany / MoreThanHidden, and KoalaNalle / PricelessKoala:
  Fluidloggable and its PR15 modded-fluid synchronization corrections, reviewed
  during the native implementation. No upstream code, assets or mixins are copied.
- Exact commits, verified MIT license and reuse inventory:
  [native-fluidlogging.md](docs/testing/native-fluidlogging.md).

## User-provided GUI artwork

- The Fluid Barrel reference image (`fluid barrel.png`) was supplied by the repository owner for exact GUI integration. No separate artwork license statement accompanied the source. The original reference, pixel-exact Toolkit JSON, native Toolkit render, and validation record are kept in `docs/gui-art/fluid-barrel/`.
## Small Water Freezer GUI artwork

- Artwork: `Small Water Freezer.png`, supplied in the local IEDCTR image folder, 941×1672, SHA-256 `be8066887498aa01d029123da7a33ad32a1e935907ae3a3e706ccdd09bfaa3eb`. The resource copy is byte-for-byte identical; the source file was left unchanged.
- Design reference: [GUI Architect Designer DEV PR #3](https://github.com/emmanueltremblay9-stack/Master-Generalist-GUI-Architect-Designer-DEV/pull/3), observed source commit `42215126e1c777073644658518af1ef7840b00c9`.
- The panel crop is `(259, 614, 422, 443)` from the source image and is mapped into the 209×235 Freezer menu. Example item art, text, values, and gauge fills are replaced with live menu data.
- No separate artwork license was present in the accessible source material; this integration follows the project owner’s explicit request.
