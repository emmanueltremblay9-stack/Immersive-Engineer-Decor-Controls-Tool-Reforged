# Validate Muslee repair pickup shape and zero-XP cost

## Scope

TOOLS-132-142, including the final approval of TOOLS-137 A: only the
exact IEDCTR Metal Crafting Table is eligible. The same press occupies the center,
one IE iron plate each cardinal cell, four corners empty. A valid pickup restores
remaining durability 0-63 to 64, consumes four plates and no XP or energy.

This evaluates pickup conditions, not whether a preview may be displayed. Cell
classifications are adapter assertions, not item-ID or rights authentication.
The adapter must check exact registry identities, the original unique tool and
all components/reserves, protected session ownership, destination and transaction
evidence. The result contains numbers only, never a second press or mutable copy.
No runtime repair recipe, grant, material consumption or restitution is installed.

## Integration boundary

This is an inactive, side-effect-free production helper. Nothing registers a menu,
payload, scheduler, recipe or automatic operation. It does not modify live objects,
make a commit, persist data, emit diagnostics, authorize recovery or prove complete
transaction safety. The server adapter must provide current authoritative inputs
under its serialization domain and revalidate all session, FIFO, revision, memory,
writer-authority and transaction requirements at engagement. A successful plan or
checksum is not permission to mutate an inventory or journal.

No existing gameplay path, approved decision, workflow, dependency, mod version,
repair permission or release setting is changed. TOOLS-137 A remains approved;
there is no new design question or TOOLS-369. This change neither duplicates nor
modifies the FIFO/revision work in PR #71 or reserve changes in PR #72/#73.

## Validation

Pinned reviewed base: `5b4c05de6d17ab19199bed0e942fb0cbae07c3cb`.
The adjacent `*Cases` program executes assertions against this exact production
class using Java 21 with no Minecraft stubs. The JUnit 4 wrapper calls those same
cases during the existing Gradle test task. Local execution is not evidence of
Minecraft integration; existing exact-head CI must compile and run JUnit,
GameTests, build and client/renderer regressions. No engine PASS is predeclared.
Root AGENTS.md applies. No merge, release or installation is included.
