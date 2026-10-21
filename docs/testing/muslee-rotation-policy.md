# Evaluate Muslee rotation limits with overflow-safe arithmetic

## Scope

TOOLS-237-240, 252-254: default 32 MiB, inclusive integer range
16-128 MiB, retain the last validated threshold on explicit invalid input or suspend
when none exists. A missing setting uses the default. Integer parsing/type errors
remain the configuration adapter responsibility; this API never treats invalid
text as omitted.

Before a new complete entry would reach or exceed the threshold, require rotation.
Refuse invalid entry bounds or arithmetic overflow. These decisions do not rotate
a segment, reconfigure an active writer, terminate engaged work or clear any
blockage. Hot changes, deferred changes and safe publication/readback still require
TOOLS-235/240-243 and all durable-authority protections. No new diagnostic is emitted.

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
