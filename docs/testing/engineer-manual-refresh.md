# Engineer's Manual integrated-feature audit

Baseline: `main` at `b9fc2b44d41eab7cb0513775b27ac7c231aa6e20` (1.1.67-reconstructed).
This is a documentation-only draft, not a release or proof of native visual acceptance.

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
No native manual-page screenshots or full 120-language review are claimed here.

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
