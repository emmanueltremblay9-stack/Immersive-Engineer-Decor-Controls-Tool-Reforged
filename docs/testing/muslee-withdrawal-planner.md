# Plan Muslee withdrawals in approved destination order

## Scope

TOOLS-319/320 and 326-331: simulate the maximum withdrawal of 1-8 units across
exactly 36 authorized ordinary player slots. Complete compatible piles first, then
empty slots; preserve ascending 0-35 ordering within each phase. Skip inadmissible
destinations and return no plan for zero movement or malformed descriptors.

The immutable numeric descriptors are NOT full ItemStack/revision captures. The
adapter must prove compatibility from complete components and native stack limits,
and bind the resulting moves to complete source/destination read sets and revisions.
No shortcut equality or transaction permission is supplied by this planner.

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
