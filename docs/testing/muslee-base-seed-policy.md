# Muslee reserve: five base seeds plus datapack extensions

## Contract

TOOLS-192/193 authorize the server datapack tag to extend the ingredient list while
preserving the five approved base seeds: wheat, melon, pumpkin, beetroot and the
exact registered `immersiveengineering:seed` item. Admission is their union with
the current `musli_bar_press_seeds` tag. A replacement/empty tag cannot revoke a
base seed. Arbitrary items outside that union are still refused.

This patch updates only the inactive reserve-core eligibility method. The existing
historical direct-inventory pressing path and its tag handling remain untouched.
No GUI, FIFO, network payload, inventory transfer or repaired press is activated.

Persistence stays separate from admission. TOOLS-194/195 still require stored
extension items to remain readable/withdrawable after removal from the live tag.
Such stock cannot qualify for new pressing or insertion until the extension is
readmitted; readmission uses current tags without rewriting or reinserting stock.
The five-base rule does not grant generic inventory/pressing permission.

## Tests and limits

`MusliBarPressBaseSeedGameTests` adds three synchronous NeoForge GameTests:

- Remove all tag members; prove admission and ingredient eligibility for all five
  base seeds, including the exact registered IE item.
- Replace the tag with one extension; revoke it, preserve persisted contents and
  partial withdrawal, then reallow it without reinsertion.
- Keep null, empty and unrelated untagged items inadmissible.

The fixture rebinds real item-registry tags on the GameTest server thread, does not
yield/schedule ticks while modified, and restores every original tag in `finally`.
This exercises registry membership changes, not end-to-end datapack-file reload,
GUI synchronization, transaction execution or arbitrary component immutability.
Existing reserve/tag tests remain unchanged and must also pass.

Reviewed base: `5b4c05de6d17ab19199bed0e942fb0cbae07c3cb`.
Run existing exact-head CI: compileJava/processResources, test, runGameTestServer,
and build, with applicable AGENTS.md safeguards. Local model tests are not engine
qualification. Record observed CI results in the PR; none are predeclared here.
No merge, release, installation, permission change or new design decision.
