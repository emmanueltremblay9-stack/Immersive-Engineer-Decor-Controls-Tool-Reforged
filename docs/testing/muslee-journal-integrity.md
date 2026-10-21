# Verify bounded Muslee journal digests without authorizing writes

## Scope

TOOLS-248/250/252: SHA-256 covers all exact supplied envelope bytes
except the 32-byte digest field itself. Bound the full input to 262144 bytes before
cloning and reject impossible digest offsets without arithmetic overflow.

No new binary magic, field offsets, envelope version, JCS schema or file format is
chosen. A caller must supply the digest offset from the separately approved schema
and prove exact framing/canonicality, complete identity, continuity and authority.
Synthetic test buffers are not valid journal entries. Matching SHA-256 is neither
authenticity nor transaction-execution proof. The caller must serialize access to
the input: cloning is not proof against concurrent mutation or disk changes.
No journal file, manifest, recovery tool or write path is created.

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
