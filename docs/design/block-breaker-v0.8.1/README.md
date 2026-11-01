# Small Block Breaker GUI v0.8.1

This document records the bounded client-side correction for issue #131. The GUI remains an IEDCTR extension and uses the existing server-backed menu values. This change does not alter breaking, drops, FE billing, inventory migration, redstone behavior, packets, menu data, mod version, or dependencies.

## Layout and behavior

- Canvas and inventory geometry remain 209 × 235 logical units. The target preview remains read-only at `(22,46)`. The single output remains menu slot 0 at `(118,38)`; the player inventory remains 27 slots plus 9 hotbar slots at its existing origins.
- The v0.8.1 background removes the decorative manual controls in the local region from `(138,75)` through `(197,102)`, inclusive. The rest of the worn-steel texture remains pixel-identical to v0.8.
- GUI labels, seven server states, explanations, and tooltips use matching EN/FR keys. Hardness is passed to Minecraft's translation component as a preformatted string; the numeric string follows the selected language's decimal separator.
- Target names and other bounded labels use the existing width-fitting renderer and ellipsis behavior; tooltips retain the full target name and explanatory text.
- Tooltip bounds abut at the front-face/hardness, rear-output/legacy-pending, redstone/energy, energy/input, and input/status boundaries.
- When a target has no item preview, the screen estimates FE from the synchronized hardness and existing cost formula. The label and tooltip call this value approximate.

## Texture provenance

The original `block_breaker_v08/background.png` remains unchanged and is still included in the runtime JAR.

| Resource | Dimensions | SHA-256 |
|---|---:|---|
| `textures/gui/block_breaker_v08/background.png` | 209 × 235 | `C7C2911D7E176B4D60A9B7E6B8DDD6BD695F5FA612759D31205DEEFB231F8255` |
| `textures/gui/block_breaker_v081/background.png` | 209 × 235 | `252F45D9EDD2A59A850F5C98B39A9A6CD23C672321BA995BD78161ABF40EEAAE` |

Both PNGs decoded successfully. A pixel comparison found 1,590 changed pixels inside the inclusive cleanup rectangle and none outside it. The v0.8.1 runtime resource path is `assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/block_breaker_v081/background.png`.

## Validation evidence

Validation ran in the issue-131 worktree based on prior PR head `be9eced502eaf22822d9aa648434e0f30f6eadb5`, with the Task A test correction present in the working tree. The passing compile, focused/full JUnit, and GameTest results were produced before the corrected test was committed as `3cae852a39db1e4540e05f1f97100a5b9828b3f5`; the test content is unchanged in that commit. Candidate implementation inputs are identified by a SHA-256 manifest over the eight changed source, resource, and test files (sorted repository-relative path, NUL separator, per-file SHA-256; this README is excluded):

- Change-set content manifest SHA-256: `C46AAA09381824318AB263EA4A351A5E5618F97525E3AACD09A1CACFBDA2126E`

The runtime JAR was refreshed from the current production source/resources after the implementation and regression tests stabilized. Its hash identifies the build output, not a Git commit:

- `build/libs/immersive_engineer_decor_controls_tool_reforged-1.1.65-reconstructed.jar`
- Size: 21,299,172 bytes
- SHA-256: `83067DC42CA071900F9052E000F11E98886213A43EF4845D6DC4D1A3E239059A`
- JAR metadata: mod id `immersive_engineer_decor_controls_tool_reforged`, version `1.1.65-reconstructed`, NeoForge `21.1.230`, Minecraft `1.21.1`.
- JAR inspection confirmed the screen class, both locale files, and both v0.8 and v0.8.1 background resources are packaged.

| Check | Result | Evidence |
|---|---|---|
| `compileJava processResources --stacktrace` | PASS, exit 0 | Checked-in Windows wrapper. Initial implementation pass: 5m55s; Task A follow-up: 19s, compile/resource tasks up-to-date. |
| Focused `MachineLayoutBlockBreakerTest` and `MachineScreenBlockBreakerTextTest` | PASS, 9 tests, 0 failures/errors/skips | `build/test-results/test/`. The screen test loads both locale JSON resources through Minecraft `Language.loadFromJson` and resolves `H: 0.50` / `D : 0,50`. The FE case now mirrors production `Math.round(hardness * 100.0F)` serialization and asserts both 0.504 and 0.506 rounding directions. |
| Full JUnit suite | PASS, 145 tests, 0 failures/errors/skips | `build/test-results/test/`, 33 XML suites; rerun after Task A correction. |
| `runGameTestServer --stacktrace` | PASS, 703 required tests | Latest `run/logs/debug.log`: 703 complete in 35.83s; all required tests passed. |
| `build --stacktrace` | PASS, exit 0 | Runtime JAR identified and inspected above. |
| `jar --rerun-tasks -x createMinecraftArtifacts --stacktrace` | PASS, exit 0 | Recompiled and repackaged against the already prepared Minecraft artifacts; refreshed the runtime JAR above. |
| EN/FR localization validation | PASS | 51 matching GUI keys per locale; 0 duplicate keys, key-set differences, or placeholder mismatches. The focused test additionally checks Minecraft's translation loader and resolved hardness text. |
| Texture validation | PASS | Dimensions, decoding, source/packaged hashes, preserved original, and cleanup-mask comparison recorded above. |
| Scope/layout review | PASS | Layout regression test retained; no menu, server, packet, dependency, version, or workflow files changed. |

### FE precision remains open

The server stores `Math.round(hardness * 100.0F)` hundredths and the menu reads those hundredths divided by `100.0F`; server billing still uses the actual hardness. `synchronizedHardnessRoundingCanEstimateAboveOrBelowServerBilling()` mirrors that production rounding and demonstrates both directions: `0.504F` serializes to 50 hundredths (`0.50F`), estimating 240 FE versus 241 FE billed; `0.506F` serializes to 51 (`0.51F`), estimating 242 FE versus 241 FE billed. No synchronization or server change is included. The exact-cost acceptance criterion remains open.

## Runtime visual QA and remaining gates

The owner-provided screenshot shows the GUI in an English single-player session with JEI visible, no target, and zero stored FE. It also shows the detail line clipped at the behavior panel's lower edge. The screenshot is feedback for the correction, but it is not bound to this candidate build and is not included in the repository.

No post-change interactive Minecraft capture is available for this candidate. The screenshot does not cover French, JEI absent, GUI-scale variants, long target names, target-bearing states 1–6, or tooltip behavior. The existing native-client gameplay and mixin smoke scripts are Linux-only and were not run in this Windows environment; their unrelated gameplay/rendering scenarios would not establish this GUI's visual acceptance. Owner visual acceptance remains pending.

The JAR was built and inspected locally; it was not installed into a launcher instance. The Task A follow-up changes only a test and this README, so it did not rebuild the runtime JAR; the production source/resource identity and previously measured JAR remain unchanged.

### PR #134 follow-up: test fidelity and visual QA feasibility

Task A is validated above. Task B is `BLOCKED-BY-UNSAFE-DEFAULT-CLIENT-GAME-DIRECTORY`: `build.gradle` configures `client()` without an isolated `gameDirectory`, so the generated client uses `--gameDir .`. The worktree's existing `run/` contains `options.txt`, `config`, `mods`, `saves`, and `world`; launching against it could mutate pre-existing saved state. No Block Breaker-specific Windows client-QA tool exposes a separate game directory. The supplied Windows renderer matrix creates an isolated run directory, but it queries/downloads unrelated renderer dependencies and validates fluid/renderer scenarios rather than this GUI. It was not run. No candidate client was started and no post-change screenshot was produced.

#### Reproducible owner visual-QA checklist

Run this only from an authorized disposable Minecraft 1.21.1 / NeoForge 21.1.230 client with its game directory isolated from this worktree's `run/`, personal launchers, and saved worlds. Use the PR's exact current commit and record its runtime JAR SHA-256 (or exact development-client commit), timestamp, GUI scale, language, JEI state, target, and machine state for every capture.

1. Create a disposable creative test world. Place the Small Block Breaker with air in front for state 0. Place a breakable target and capture states 1–6 using, respectively: redstone HIGH; no FE; blocked output; sufficient bottom-face FE while work is in progress; an immediate post-break cooldown capture; and bedrock/unbreakable target.
2. Capture English and French in each actually supported GUI scale (including 1×/2×/3× only where the client offers them), with JEI present and absent. Include a long target name and hover captures for target, hardness, output, cost, energy, and status. Mark any unavailable case `NOT_PERFORMED`.
3. Check that the full status detail fits inside Behavior & Operation, decorative faux-controls do not look interactive, and `Rear OUT` remains legible without moving the true output slot. Also inspect target preview, hardness, FE stored/capacity, read-only redstone, output slot 0, inventory 27+9, truncation, tooltips, and JEI overlap.
4. Preserve each relevant screenshot outside publication assets and record its path or artifact ID and SHA-256. Owner visual acceptance remains a separate pending decision.

Compilation, JUnit, GameTests, JAR inspection, and static checks do not prove that the Minecraft client renders the revised screen correctly. Merge, ready-for-review, auto-merge, tagging, release, publication, launcher installation, and issue closure are outside this change's authorization.
