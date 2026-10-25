# IEDCTR 1.1.65-reconstructed

Corrective update for the startup crash with **Lithium mc1.21.1-0.15.4 on NeoForge**.

## Fix

- Correct the Mixin application order between IEDCTR's fluid-neighbor hooks and Lithium's fluid-flow optimization. Lithium's optimization remains enabled; the mandatory injection checks remain active.
- Preserve existing fluid storage, natural flow, source removal, chunk reload and client synchronization behavior. This update adds no block-state or saved-data migration.

The correction is [PR #96](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/96). The separate shape-cache and external-fluidlogging proposals are not included.

## Compatibility

Minecraft **1.21.1**, Java **21**, NeoForge **21.1.230 or later**. Required: **Immersive Engineering 12.4.2-194 or later**. JEI **19.32.0.359 or later** is optional. Lithium is optional; the specific compatibility target is **mc1.21.1-0.15.4 NeoForge**. Other Lithium versions and complete third-party modpacks have not been exhaustively qualified.

Replace the previous IEDCTR JAR with this version; keep only one IEDCTR JAR installed. Existing worlds retain their current format. Back up a world before updating.

## Provenance and support

MIT notices and credits for Stefan Wilhelm's Engineer's Decor, RsGauges and Engineer's Tools, and the attributed RsGauges port contributors, remain packaged in `LICENSE`, `NOTICE.md` and `CREDITS.md`. Immersive Engineering is a separate required dependency and is not bundled.

For a remaining crash, submit the full crash report or `latest.log`, exact loader/mod versions and reproduction steps through [GitHub Issues](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/issues). A startup fix does not establish compatibility with every mod combination.
