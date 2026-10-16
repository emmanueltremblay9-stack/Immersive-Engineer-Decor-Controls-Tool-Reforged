# Sleeping Bag campfire and hemp recipes

The Sleeping Bag now requires the player to occupy one of the eight horizontal neighboring cells of a lit `minecraft:campfire`. Diagonals count. The campfire cell itself, distance two, a different Y, and an extinguished campfire do not qualify. Soul/modded campfires are not added by this change. Existing Overworld, night, monster-safety, durability4096 and respawn behavior remain in place.

Recipes (top to bottom):

- Sleeping Bag: `FFF / HHH / CCC`, with `c:fabric_hemp`, `c:fiber_hemp`, and `minecraft:wool_carpets` respectively. IE12.4.2-194 resolves the first two tags to `immersiveengineering:hemp_fabric` and `immersiveengineering:hemp_fiber`.
- Material Box: `RIR / ICI / RCR`, with `immersiveengineering:wirecoil_structure_rope`, `c:plates/iron`, and `immersiveengineering:crate` respectively. The crate is IE's treated-wood storage crate. This consumes four rope coils, three iron plates and two crates.

IDs/tags were checked against the locally resolved IE1.21.1-12.4.2-194 JAR, including its own recipes. Material Box storage remains512 of one unmodified material type.

GameTests:

- `CampfireToolRedesignGameTests`: eight lit neighbors, unlit neighbors, missing/center/distant/vertical fires, failure preserving time/durability/cooldown, monster safety, exact Box recipe, each missing Box ingredient and rejection of both old grids.
- `EngineerToolsGameTests`: successful night rest adapted to campfire; daytime and durability tests retained; all16 wool carpets tested against the new Sleeping Bag recipe; existing Material Box conservation/invalid-data tests retained.

Run `scripts/invoke-gradle.ps1 compileJava test runGameTestServer build --stacktrace` with the repository wrapper. Tests run in the development environment, never user Prism/world directories. Runtime installation is outside this PR.

The Stim and Diving redesign is deferred: potion-preserving rechargeable state and equipped-only Curios automation are separate work. Upgrade capacities/costs, Curios slot/manual command, exact Stimpak steel form and final Diving grid still require reconciliation. Diving dose capacity is not inferred from Stim's approved two-dose base.
