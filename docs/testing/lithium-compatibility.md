# Lithium 0.15.4 compatibility

IEDCTR 1.1.64 fails during client bootstrap with the official Lithium
`mc1.21.1-0.15.4-neoforge` build. Both mixins use priority 1000:
Lithium overwrites `FlowingFluid.isWaterHole`, and Mixin rejects IEDCTR's
mandatory `iedctr$hole` redirect into that overwrite with
`InvalidInjectionException` ("cannot inject into ... merged by ... with priority 1000").

The production change gives `FluidNeighbourReadsMixin` priority 1100, allowing
its positional fluid lookup to apply to the overwritten method. The redirects
remain mandatory. No Lithium option, fluid storage format, save data, block
property, or production dependency changes.

## Reproduction identities

The isolated cloud reproduction uses Minecraft 1.21.1, NeoForge 21.1.230,
Immersive Engineering 12.4.2-194 and Java 21. The source baseline is main
`98d15a3f89b4e4bf889ab2f77792ed7f31b2469f`, independent of PRs #94 and #95.

The rebuilt baseline JAR has SHA-256
`1de75eabdca230144d8683c7913c49c260c3b43b0c2c923bebfd00ff0a631c80`,
matching the 1.1.64 CI identity recorded in
[PR #92](https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/pull/92).
This is a reconstructed source artifact; the reporter's downloaded release
JAR and crash log have not been supplied or independently matched.

Lithium comes from the
[official NeoForge version](https://modrinth.com/mod/lithium/version/mc1.21.1-0.15.4-neoforge),
version ID `DDUrRVCA`. The 774220-byte JAR has recalculated SHA-256
`488f33216030c1cb0baa33e59de3d657d08d5795a6c2080c8c8bbbc133fc8757`.
The test helper also checks the SHA-512 published by Modrinth before loading it.

## Regression coverage

Bootstrap alone is insufficient: Lithium's branching fluid search bypasses
vanilla slope-search lambdas. `FluidPathCompatibilityGameTests` compares
positional fluid cells with free-fluid controls for six fluids (water, lava,
creosote, ethanol, biodiesel and plant oil), each with an existing source,
a current, and a source below the closest route. Its branching fixture checks
both the expected route and exact output fluid states. The stored fixture uses
an open pole, so a solid catwalk deck cannot introduce a geometry difference.

The Lithium test profile requires the exact loaded version and the transformed
`calculateComplexFluidFlowDirections` method. It fails if the fluid optimization
is disabled. The existing full GameTest suite also covers natural propagation,
recession, survival buckets, block preservation and compressed chunk save/reload
for sources and generic fluid attachments.

The isolated client profile checks ten natural-water source-distance cases with
source removal, 60 generic-fluid initial/update synchronization and drain cases,
18 submerged support variants and 13 tracker render checks. It captures actual
framebuffers, checks the normal evidence gates and requires a clean process exit.
Machine-readable assertions are functional evidence; they do not establish a
general visual or performance benchmark.

The local candidate runtime JAR has SHA-256
`522bd69b1e534dd8678b9b2874fba4bff14b7146799ca2f86d173ca10dcc9400`.
On 2026-10-24, the isolated cloud run passed all 540 required GameTests both with
and without Lithium, and all client profile evidence gates with Lithium. The native build
also passed 119 JUnit tests and the Python evidence-gate suite passed 74 tests.
The canal captures at source distance four were inspected before and after
removal: the current receded and the stair remained. This is a focused visual
check, not full renderer qualification.

## Repeat the checks

Use a fresh checkout with Java 21 and the repository's normal dependencies.
The helper creates a separate GameTest directory and refuses to reuse an
existing world. Lithium is injected only into the explicitly requested test
run. Do not change a normal installation's `lithium.properties`.

```sh
python tools/validation/lithium_profile.py build/lithium-compatibility
./gradlew --init-script build/lithium-compatibility/gametest.init.gradle runGameTestServer
python tools/validation/client_gameplay.py --lithium --renderer-profile native --flow-distance-only --prepare-timeout 600 --timeout 420
```

Run the normal `test`, `runGameTestServer` and native client profile without the
Lithium init script as the control. CI retains the pinned dependency identity,
server/client logs, result JSON and client captures in the
`lithium-compatibility` artifact. It does not upload a release JAR.

The scope is the pinned Lithium version with native rendering. Other Lithium
versions, the reporter's full modpack, and combined third-party renderer stacks
require their own qualification.
