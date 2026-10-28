# IEDCTR 1.1.65-reconstructed

Release candidate from source `52639384ea90aebe9d563b966027421cebe52b45`, following public 1.1.64.
Automated source GameTests, reproducible JAR builds and a packaged-server baseline smoke passed. Full client-gameplay and visual acceptance of this packaged JAR remains unverified.

## Compatibility and rendering

- Correct the startup Mixin conflict with Lithium `mc1.21.1-0.15.4` NeoForge, retaining its fluid-flow optimization and the mandatory injection checks (#96).
- Extend fluidlogging to the explicitly supported vanilla and Immersive Engineering blocks, not every third-party block (#95).
- Cache final catwalk-stair and Double-T shapes (#94), preserve lighting in chunkless block previews (#98), and remove internal seams between adjacent Panzer Glass blocks (#102).

## Construction and lights

- Correct Door Sensor wall and ceiling contact with the clicked supporting face; sensing behavior and collision-free placement remain unchanged (#101).
- Correct raised-catwalk support selection for the upright treated-wood support (#99) and catwalk-stair railing junction heights (#105).
- Add V-supported Steel Catwalk Stairs and correct their joins for wood, thin-steel and thick-steel poles (#107, #110). No crafting recipe was added by that feature.
- Add wall-mounted Steel Double T supports, horizontal/vertical wall placement, and compatible same-plane junctions (#108, #114).
- Support internal attachment of the four existing light types: Iron Bulb Light, Ceiling Edge Light, Inset Floor Edge Light and Inset Light. An attached light can be selected and removed separately. This does not mean four lights occupy one attachment slot (#106, #114).
- Preserve ordinary adjacent lamp placement beside occupied supports and correct perpendicular wood/steel support junctions (#115, #116).
- Add the Treated Wood Beam with ceiling and wall placement. It remains creative-only pending a crafting-recipe decision (#111).

## Fluids and machinery

- Add exposed rain/thunder water collection to the Small Fluid Collection Funnel and Passive Fluid Accumulator, retaining their exposure, fluid-compatibility and capacity checks (#103).
- Connect the Funnel's actual source-drain transaction to its server ticker. Flow must reach the top inlet; the search follows a connected path for at most 16 edges. A real source is removed before one bucket is credited. Unpaid virtual work is not extractable fluid (#112).
- Increase Fluid Barrel capacity to 32,000 mB. Its recipe uses eight steel plates around an Immersive Engineering metal barrel; vertical fluid access and existing manual bucket handling are retained (#111).
- Add axial, one-way fluid capabilities to the three existing valves. The valves are not pumps and have no transit tank; the analog limit is per call, not a guaranteed per-tick throughput (#111, #112).
- Update the Passive Fluid Accumulator recipe to use sponge and piston (#100).

## Requirements and update precautions

Minecraft **1.21.1**, **Java 21**, NeoForge **21.1.230 minimum**.
Immersive Engineering **12.4.2-194 minimum** is required; JEI **19.32.0.359 minimum** is optional.
These are declared dependency ranges, not proof that every later dependency version has been tested.
The specifically exercised Lithium target is **mc1.21.1-0.15.4 NeoForge**; Lithium remains optional.

Back up worlds and replace the prior IEDCTR JAR; keep only one IEDCTR version installed.
Do not assume exhaustive compatibility with arbitrary modpacks, renderers, legacy saves or all fluid/block combinations.
The exact packaged JAR passed a dedicated-server baseline smoke. The source CI also exercises native client gameplay paths, but full in-game client rendering and new-feature acceptance of the packaged JAR have not been independently completed.

## Provenance and support

Preserve `LICENSE`, `NOTICE.md` and `CREDITS.md`, including the original Engineer's Decor, RsGauges and Engineer's Tools credits and the credited RsGauges port contributors.
Immersive Engineering remains a separate required dependency, not an included mod.

Report problems at [GitHub Issues](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/issues), with the crash report or `latest.log`, exact loader/mod versions and reproduction steps.
