# Compute Muslee all-or-nothing press batch deltas

## Scope

TOOLS-113/115, 129/130 and 333-338: one bread, apple and admissible seed
produce four bars and use one durability point. Evaluate current approved 0-64
durability, refuse without a complete output capacity of four, and clear the seed
type only when the successful batch leaves zero seeds. The original tool at zero
remains present; no destruction operation is supplied.

This computes quantities only. A verified output-room number must already include
complete destination compatibility, stack limits and authorization. The actual
source/destination snapshots, anti-ABA tracking, seed tags, ownership, durability
identity, journal and atomic commit are external requirements. Legacy durability
128 is not silently migrated. There is no output ItemStack creation or live use hook.

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
