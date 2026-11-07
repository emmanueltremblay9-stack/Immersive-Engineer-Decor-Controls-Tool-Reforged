# Block Breaker text-free nameplate

PR 170 removes the permanent Block Breaker title and retains localized identity
on nameplate hover. Its branch is reconciled with main `1eccac0a39665ffdfcdfe80c264be2bd6455921f`,
including Factory Placer V2 as the normal client screen. Production changes remain
limited to the title draw and hover region. Both retained v3 images are unchanged.

## Independent native qualification

Run `python tools/validation/block_breaker_packaged_native.py --language en_us`
and `python tools/validation/block_breaker_packaged_native.py --language fr_fr --height 260`
from a clean committed checkout. The new exact-head workflow runs both profiles.
The default runtime JAR is built first without test inclusion. The driver lives
in a separate development test mod, with main output excluded from the runtime
classpath. Loaded class/resource origins, normal JAR hash and both active v3
artwork hashes are checked. No production reference/render selector is set.
`iedctr.blockBreakerPackagedNative` activates only this isolated test driver.

The driver creates a nonce-owned survival world on its own Xvfb display. It seeds
a machine and dirt target, then uses a physical XTest right-click on a verified
in-reach block raycast. It never constructs a screen or calls server openMenu.
Native title, FE, unavailable fluid/power, target, static-scene and work tooltips
are observed through actual native render events after XTest pointer motion.
The title translation is checked in English and French. The normal, hover,
worked and returned-item screenshots remain available for visual comparison
against the retained v3 artwork, including the unlettered nameplate.

The gameplay setup admits exactly 240FE through the underside capability. Real
server ticks must break the dirt target, debit that energy and retain one dirt
in the visible output without a world drop. Physical XTest output pickup and placement into a verified empty player-inventory
slot must receive matching independent server acknowledgements. The output rejects
insertion. Final conservation requires an empty machine/cursor, exactly one player
dirt and no world drops, verified on both sides and serialized in the proof. The neutral artwork
controls are not invented commands; their explanatory tooltips remain present.

## Results and boundaries

Local independent text-free and evidence-validator tests pass, including negative
controls for restoring the title or removing hover. Native evidence guards reject
wrong identities, missing captures, unlocalized title, clipped tooltip and missing
server acknowledgements. Local Gradle cannot download its absent distribution
because the network is unavailable. Mapped compilation and actual native runs
must therefore pass fresh exact-head CI before any qualification claim.

No Freezer fixture is modified or reused. This is a bounded normal-JAR test in a
NeoForge development launcher, not a separately installed consumer launcher or
full-modpack test. Resize/reload, all complex inventory gestures, owner visual
acceptance and release are not established by this bounded suite. No merge or
release is authorized by the test result alone.
