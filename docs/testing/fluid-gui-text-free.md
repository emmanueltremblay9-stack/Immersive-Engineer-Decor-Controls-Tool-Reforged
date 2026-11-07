# Text-free Funnel and Accumulator

This follow-up is stacked on PR170 head
`90b9b5d800bdc90695d169b0da00763323d0f6f5`, tree
`37e5cd7aabf5c22e07d41224cef04345267460b8`.

## Presentation scope

The Small Fluid Funnel and Passive Fluid Accumulator no longer draw permanent
titles, bucket captions, fluid names/amounts or inventory captions. Their source
PNGs are unchanged. Existing texture patches continue to cover baked examples;
the Funnel's four bucket/outlet/inventory patches now apply in French as well.
Accumulator mask calculations and source samples remain unchanged.

Names are available on nameplate hover. Funnel fluid identity and exact tank
amount/capacity remain on tank hover, also available over the former readout.
The lower outlet now has a hover label. Accumulator collection instructions are
available over the visible rain-cloud icon and former collection caption; its
water amount/capacity also remain accessible over the tank and former readout.
Existing empty-slot role tooltips and ordinary occupied-item tooltips remain.

No slot/layout, menu, network, gauge calculation, fluid collection, bucket transfer
or machine-mechanic change is included. Static decorative fluid streams/rain are
still artwork, not newly qualified operational indicators. Other GUI branches
retain their separate changes and qualification.

## Offline regression checks

Run `python -W error::ResourceWarning -m unittest discover -s tools/validation -p 'test_*.py' -v`.
The new fixture compiles the exact production renderer and tooltip methods with
recording Minecraft-facing doubles and the actual accumulator gauge helper.
It checks both artwork-locale branches, empty/half/full/overflow amounts including
the Accumulator's actual 4000 mB capacity, all five
Funnel fluid codes, three screen origins, every integer point of the previous
modeled tank hover areas, and every modeled padded machine-slot point.
Negative controls restore permanent text, omit a French caption patch, or remove
the outlet hover; each must fail. Both original PNG hashes and required EN/FR
translation keys are checked. Existing Block Breaker regression tests remain.

Local checks passed: all 155 Python validation tests, syntax parsing of 312 Java
sources, and `git diff --check`. The two gauge blocks, accumulator mask helper and
slot-tooltip method were also compared byte-for-byte with the parent source and
are unchanged. These local results do not replace mapped compilation.

The doubles do not decode textures, resolve Minecraft fonts or establish native
input behavior. Patch coverage and slot restoration are recorded calls, not GPU
pixel proof. The one-pixel hover padding is explicitly a model.

## Native qualification gate

Fresh mapped compilation, JUnit, GameTests, and native English 1280x720/French
1280x260 screenshots are required from the existing PR workflows. Check that all
captions are gone while the live tank states, slot alignment and artwork remain.
Prior screenshots with text do not qualify this source. New nameplate, outlet,
cloud and readout hover targets are not captured by the existing native fixture.
Physical pointer input, custom packs, reload, complex gestures, full reporter
modpack gameplay and owner visual acceptance remain outside this bounded check.
No merge or release.
