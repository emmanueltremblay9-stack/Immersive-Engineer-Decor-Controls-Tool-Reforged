# CurseForge upload notes — 1.1.55-reconstructed

Release type: Release

Game version: Minecraft 1.21.1

Mod loader: NeoForge

Required dependency: Immersive Engineering 12.4.2-194 or newer compatible 1.21.1 build

Optional in-mod integration: JEI 19.32.0.359 or newer compatible 1.21.1 build

This release fixes the placement and outline shapes of four Engineer's Decor lights and restores the centered, independently surviving Steel Framed Window. The Decor and Defense sign item definitions changed from volumetric world-model parents to flat `item/generated` front-texture models, removing the inherited block-model side geometry associated with the report. One obsolete generic support-removal GameTest was replaced with seven targeted required GameTests, increasing the required suite from 182 to 188, and an objective sign-item resource validator was added.

Engineer's Decor, Engineer's Tools, and Redstone Gauges and Switches were ported into this single mod; they are not dependencies to install. Their provenance is documented in the project description, `CREDITS.md`, and `NOTICE.md`. Immersive Engineering is the sole CurseForge `RequiredDependency`.

Patched-client inventory and JEI visual screenshots are `NOT_PERFORMED`; no visual-runtime result is claimed.

Canonical JAR SHA-256: `956FC45E04675427AB98A79BB82F22E28F55E7D13EBDCD54F86260515A63167C`.

License: MIT

Source: https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged

Issues: https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/issues
