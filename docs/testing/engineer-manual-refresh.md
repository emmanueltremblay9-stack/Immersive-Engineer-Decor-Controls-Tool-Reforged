# Engineer's Manual integrated-feature audit

Baseline: `main` at `b9fc2b44d41eab7cb0513775b27ac7c231aa6e20` (1.1.67-reconstructed).
This is a documentation/resource-only draft, not a release or complete visual acceptance.

## Scope and evidence

The audit inspected the 31 first-parent changes since the latest manual-affecting
crate upgrade (`af80be57`), current machine implementation, existing manual pages,
and duplicated category entries. Current source, rather than historical documents
still labelled "draft", determines which behavior is integrated.

| Area | Integrated evidence | Manual treatment |
| --- | --- | --- |
| Freezer | #172; MachineBlockEntity, FreezerFluidAutomationGameTests | Tank/pipe input, costs, bucket recovery, input priority, interruptions, comparator, no automatic phase upgrade |
| Tree Cutter | #173; tickTreeCutter, migration and recovery code | Single output, real vertical harvesting scope, no FE or deferred Tree Fell, old-save deletion warning |
| Solar | #141/#167; tickSolarPanel, SOLAR_OUTPUT_DIRECTIONS | Actual production cadence, weather, storage and shared item/neighbor transfer budget |
| Barrel | #136/#151/#165/#166; fluidHandler, BARREL_CAPACITY | Supported fluids, capacity, top/bottom ports, no invented horizontal connection |
| Block Breaker | #139/#145/#170; tickBlockBreaker and energy methods | Bottom FE input, per-block cost, output and stop conditions |
| Mineral Smelter | #132/#144; tickMineralSmelter, public capabilities | Three modes, mode queue, actual accepted stone examples, ports, reserved lava and hot-contact warning |
| Milking | Existing merged behavior including #130/#143; tickMilkingMachine, TfcMilkingCompat | Bucket/tank production and native TFC constraints only; does not import unmerged #169 |
| Double-T supports | #150; DoubleTSupportItem, VerticalDoubleTSupportBlock | Canonical item wall/floor placement, cross-orientation connections, legacy item handling |
| Crates and older changes | Current individual pages | Synchronize category copies for crate upgrade/storage, hopper, gates, lamps, poles, stairs, dining, rain, valves and other previously updated text |
| GUI-only changes | #138/#140/#151/#166/#167/#170/#171/#173 | Describe mechanics without inventing new controls or claiming full visual acceptance |
| Non-player behavior | #154 shape cache; #155-157 tooling; #175 evidence; #146-149/#177 release metadata | No invented gameplay instructions |

The English/French category copies previously contradicted several individual pages:
for example, the crate category still described a generic label click instead of
54-slot storage and shift-right-click editing, and rain collection details were missing.
The refresh synchronizes all eligible existing English/French category anchors with
their individual pages, not just the newly rewritten machines.

## Localization boundary

There are 120 locale directories, including `en_us` and `fr_fr`. This draft updates
English and French, preserving all other 118 locale directories byte-for-byte.
It does **not** claim global translation completion. Those localized files can still
show older instructions; they are not automatically replaced by the English fallback.
A follow-up needs translation review against this corrected source, retaining proper
names, amounts, recipe anchors and category parity. Do not bulk overwrite translations
with English or claim languages have been reviewed when they have not.

## Tests and remaining acceptance

`validateManualResources` checks autoload parity, entry presence and crafting widgets.
`EngineerManualContentTest` checks the rewritten entries, all eligible EN/FR category
copies, and critical Freezer/Tree/Solar wording. These are resource tests, not gameplay.

Before calling the manual complete, open the individual and category pages in the
ordinary packaged client with Immersive Engineering, in English and French. Check
pagination/overflow, recipe widgets, item shortcuts, links, font accents and relevant
GUI scales. Longer explanations may need explicit page splits after native inspection.
The bounded native review below covers selected pages only, not full 120-language review.

## Follow-up mandate

1. Rebase only after checking the then-current main and any merged gameplay changes.
2. Review every manual entry against registry/code/recipes, prioritizing older placeholder
   entries, and preserve distinctions between implemented behavior and pending PRs.
3. Validate EN/FR rendering in an isolated disposable client and fix only demonstrated
   pagination/layout problems. Do not touch a release worktree or user world.
4. Translate the verified changes into each supported locale with an explicit reviewed
   coverage list, and extend category-parity checks as locales are updated.
5. Run resource checks, affected tests and exact-head CI. Keep the PR draft until review;
   no merge, tag, release or production installation is authorized by this document.

## Second pass: older integrated behavior

The broader history pass found two additional stale descriptions predating the latest
crate edit. EN/FR Funnel now documents #112 connected-source drainage (16 connected
edges, four unpaid work quanta before an actual source is removed, 3,000 mB capacity,
output cadence and separate weather collection). Door Sensor now describes its
actual `SensorKind.PLAYER` behavior and #101 support-relative placement, instead of
the old erroneous scheduled-pulse boilerplate. Both category copies are synchronized.

The new JUnit class was compiled with Java 21 and run directly using already present
JUnit 4.13.2/Hamcrest artifacts: four tests pass. This is the actual committed resource
test class, not a stub or substitute assertion implementation. The complete Gradle
suite remains separate from this focused test run; the earlier Maven resolution
failure does not mean these focused tests are still unexecuted.

## Third pass: campfire requirement and native pagination

Sleeping Bag EN/FR now documents the integrated #66 campfire prerequisite, verified
against `SleepingBagItem`: a lit vanilla campfire in one of eight horizontal neighbor
cells at foot height (diagonals count), not a soul campfire. Both category copies
were synchronized. The fifth focused test guards this requirement and the compact
French category labels.

Native review used an isolated disposable client/world, existing Java/NeoForge/IE
libraries and independently built JARs, not a release installation or CI artifact.
At GUI scale 2 with a 1180-by-812 client window:

- Freezer EN/FR: all three pages each inspected, recipe widget and final text visible.
- Tree Cutter EN/FR: all three pages each inspected, including complete migration text.
- French Fluid & Power navigation and category title: a pre-existing overflow was
  reproduced, then corrected by shortening two labels to `Fluides et énergie`.
  Both resulting screens fit inside the manual in the second native run.
- Updated Sleeping Bag FR: both pages inspected; campfire restrictions, spawn-point
  behavior, cooldown and wear text are visible. The recipe widget is present.

Baseline native JAR (resources at PR commit `0049535b`): SHA-256
`5bd56525c74d6d024ecf051a50d84e8ddecaa6722a3c0cab635e9448204e4cf7`.
Revised native JAR (Sleeping Bag and French label corrections): SHA-256
`7d59295f668e148143c9916a45ec7d17bbeee18311034d8761f075fea81b2011`.
Native captures and separate logs were retained for both runs. No resource-pack
substitution was used. Both clients were shut down normally after review.

Focused JUnit: **5/5 pass** using existing JUnit 4.13.2/Hamcrest artifacts and Java 21.
Offline Gradle `validateManualResources jar`: **PASS**, 223 source pages and 424
crafting widgets. The previous PR head's 67 successful CI checks do not certify a
subsequent commit; exact-head CI must be checked after this update.

Remaining: every other entry's code-to-text audit, additional GUI scales, category
page pagination, item shortcuts/links, Sleeping Bag EN native rendering, and all
118 other locales. This review is deliberately not an exhaustive completion claim.

## Fourth pass: access, fluids, tools and controls

Main was fetched again and remains `b9fc2b44`. This pass extends the source audit
beyond recent machine PRs. These new pages have static/resource verification only;
the native screenshots above still cover only their explicitly named earlier pages.

- Catwalk stairs: endpoint attachment and continuous flight placement from
  `CatwalkStairsPlacement`; separate raised stair and contextual V from
  `RaisedCatwalkStairsBlock`/`RaisedCatwalkBlock`. No invented raised-stair recipe.
- Double-T lights: four compatible lamp identities, fit restrictions, one attachment,
  separate mining and paid ownership from `SupportLights` and `DoubleTSupportItem`.
  Current steel wall placement remains upright; older wall-click-zone documentation
  was not copied over the newer implementation. The separate wood beam retains its
  own click zones and has no crafting recipe.
- Inset Light: existing raised-catwalk underside installation/removal and separate
  mining from `RaisedCatwalkBlock`, including the plain-variant admission limit.
- Overview: exterior fluid cells, source/current distinction, machine tank separation,
  lava hazards and the bounded vanilla/IE extension from `NativeFluidCells`,
  `NativeFluidloggedBlock`, `ExternalFluidAdapter` and `MachineBlocks`.
- Tools/controls: 43 entries corrected as listed below. Recipe anchors remain intact; contact subtitles now say occupancy rather than
  pulse. All matching EN/FR category copies are synchronized.

| Entries | Demonstrated contract | Source |
| --- | --- | --- |
| Mind Squeezer | Reusable, no wear; health/XP/lapis costs | ChargedLapisSqueezerItem; EngineerToolsGameTests |
| Dimmer | Click position selects output, not an incrementing cycle | ControlsBlockTypes.dimmerPowerFromHit |
| Two interval timers | Manual, non-repeating 40-tick pulse | ControlsModule; PulseBlock |
| Two linear detectors | Forward corridor, not omnidirectional | SensorBlock entity scan |
| Industrial Switch | Two-block cabinet, synchronized halves | IndustrialSwitchBlock |
| Six gauges, eleven indicators | Read supporting block power/comparator | readAttachedSignal; ControlRedstoneGameTests |
| Seventeen contacts | Stay active while occupied, 12-tick rescan | ContactBlock |
| Iron/gold grit FR | Match integrated hammer outputs already documented EN | CrushingHammerItem |

Counterexamples were deliberately preserved: ordinary entity detectors scan around
themselves; sensitive glass reads nearby power; the siren already describes its
support correctly. The Foot Button is not floor-only like the other contact variants.
Muslee reserve/cursor/protocol helpers are not a live menu: no future GUI is promised.

### Machine and storage continuation

Nine entries were compared with `MachineBlockEntity`, `MachineLayout` and current
menus/screens: crate, hopper, placer, dropper, lab furnace, electric furnace,
incinerator, metal crafting table and passive accumulator. Corrections include:

- Crate FR: restored 54-slot storage, real display item, sneak-label gesture and
  component-aware transfer shortcut. Both languages explain nested-storage exclusions.
- Hopper FR: missing direction and redstone behavior restored; both languages
  distinguish loose-item range from inventory adjacency and item count from stacks.
- Placer: 18-slot rotation, actual ignored-redstone default and the conditional
  failed-placement ejection risk (manual or allowed signal change, not every retry).
- Dropper: actual filter stacks/components/quantities and separate filter/redstone gates.
- Furnaces: optional lab heater still requires furnace fuel; electrical speed costs,
  optional hopper feeder and OFF not disabling its feeder transfers.
- Incinerator: irreversible disposal starts before a full queue; FE accelerates it,
  redstone does not stop it. Metal table: single occupied material grid slot for plates.
- Accumulator FR: missing rain/thunder collection restored from the verified source.

Valve limits/direction and dining bonuses were checked without rewriting their
already correct individual text. The runtime code remains unchanged. Existing GUI
help strings outside these manual pages can still contradict the corrected manual;
these are tracked as follow-up rather than silently changing all translations.

The Block Breaker page also identifies its rear-only automated output face.


### Additional native review

The independently packaged JAR for `ef084163` (SHA-256
`a35b50cc8f1f1456955c6d227fa04dc6ec55ca947b09331320d91482291e5974`)
was checked in the same isolated French client at GUI scale 2. All six Overview
pages and all three revised Labeled Crate pages were read through to their final
text. The crate recipe widget rendered, its IE material link opened, and back
navigation returned to the crate. This does not certify every updated page.

That review reproduced another pre-existing navigation overflow: the French
straight-valve entry title extended past the book edge in Storage and Transport.
Only its manual title and matching category heading are shortened to `Clapet
anti-retour`; the registered item's translated name is unchanged. A focused
regression checks the shortened title. The valve mechanics/body are unchanged.

## Remaining access and decor pass

A further source review adds existing operation instructions to eleven EN/FR entries:
chimney intensity and stack behavior (`ChimneyBlock`, `ChimneyTrunkBlock`), independent
catwalk/railing sides and selective mining (`PortedBlocks`, native railing GameTests),
paired sliding-door versus manual-only wood-door/gate behavior (`SynchronizedAccessways`),
and aligned open hatch/ladder climbing. Tracker clearing now explicitly requires use
in the air; using it on a block instead stores that block even while sneaking.
These are largely omitted instructions, not eleven broken implementations.

Four fluid-machine clarifications cover existing behavior: Freezer interruption resets
paid progress without refunding FE; upright Barrel automatic downward output; Milking
bucket ports and transfers while production is redstone-paused; and the Smelter's
mineral-to-lava-to-Obsidian path with heating-only redstone inhibition. Evidence is in
`MachineBlockEntity` and the existing Freezer/FluidRouting GameTests. No unmerged
Milking behavior is included.

The unchanged Funnel EN text was additionally read through all four native pages at
GUI scale 2 on `e54736e9`, including its final rain paragraph. Its pagination was intact.
New access/decor text still requires its own bounded native review; this pass does not
certify every page, language or GUI scale. Non-EN/FR locales remain unchanged.

Native inspection of the new French Sliding Door and Mesh Gate text read both pages
of each to the end. It reproduced the pre-existing Mesh Gate title overflowing both
navigation and page heading. The manual-only title/category heading now use `Portail
grillagé en acier`; the item's registered translated name is unchanged. The body
pagination was intact. JUnit now contains ten targeted resource tests.

Final native recheck on the independent package for `b734977f` (SHA-256
`d56c727f804697fe5c26b88705d5b84c29e5a418e6eee572d8605260b2de4659`)
confirmed the compact French gate label and heading fit. All four French Funnel
pages were read to the final paragraph without clipping. The isolated client was
saved and closed. A separate source review then tightened the wood-door pairing
instruction: opposite hinges must be on the outer edges of the opening, as enforced
by `SynchronizedAccessways.partner`; opposite inward hinges do not pair.
