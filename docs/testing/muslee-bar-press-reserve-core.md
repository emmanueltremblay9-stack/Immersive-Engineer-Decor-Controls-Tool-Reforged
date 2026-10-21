# Muslee Bar Press — internal reserve core (candidate PR)

Status: **DRAFT / UNMERGED / CORE ONLY**. Base: IEDCTR implementation repository `main` at `7485e6a580b32fc55a4f874924a18bacacf7d9ac` (live base verified before this PR).

## Scope

The new `MusliBarPressReserve` is a self-contained, read-only/pure-planning reserve layer for an individual `musli_bar_press` ItemStack. It implements independent capacities of 8 bread, 8 apple and 8 accepted seeds; single concurrent seed variety; exact ItemStack-component preservation; immutable detached snapshots; safe insertion/withdrawal simulations; and detached persistence staging into a versioned namespaced field of `CUSTOM_DATA`. Unrelated `CUSTOM_DATA` and other ItemStack components are never replaced by the reserve codec.

`stage` only returns a detached copy; **it does not replace an inventory slot or commit a transaction**. Future server-side integration must provide the validated atomic transaction, ownership fencing, destination transfer, GUI, protocol and FIFO, including TOOLS-313–368 anti-ABA revision tracking and memory limits. Any invalid/ambiguous stored payload or non-roundtrippable component encoding fails closed, without rewriting existing data.

## Candidate file additions

- `src/main/java/com/oblixorprime/engineersdecorreforged/tools/MusliBarPressReserve.java`
- `src/main/java/com/oblixorprime/engineersdecorreforged/gametest/MusliBarPressReserveGameTests.java`
- `src/main/java/com/oblixorprime/engineersdecorreforged/gametest/MusliBarPressReserveTagGameTests.java` (separate real-tag regression)
- `docs/testing/muslee-bar-press-reserve-core.md`

Existing `MusliBarPressItem.java`, `EngineerToolsModule.java`, recipes and existing GameTests are intentionally unchanged; the current direct-inventory press mechanic and 128-durability registration remain **historical noncompliant behavior** until a later PR safely replaces them. No source-file modification is disguised as implementation of the approved GUI/FIFO/repair workflow. TOOLS-137 is approved A (four IE iron plates, zero XP/energy) but repair is **outside this PR**.

## Acceptance tests (NeoForge target required)

- Read pristine legacy item as empty; enforce 8/8/8 independently, refuse overflow.
- Reject mixing seed variants; clear seed identity on empty withdrawal.
- Encode/decode all nested ItemStack components, counts and variant; preserve damage and unrelated keys.
- Refuse and preserve malformed payload and stale expected snapshot.
- Prove independence of two presses and defensive copying of offered stacks and exposed state.
- Verify no press operation, inventory transfer, automatic repair or new network packet became active.

Intended commands on a checkout matching its HEAD (Java 21/NeoForge 21.1.230/Minecraft 1.21.1):

```sh
./gradlew compileJava
./gradlew runGameTestServer
./gradlew clean build
```

On Windows, respect root `AGENTS.md` if a `PipeImpl`/`UnixDomainSockets` loopback failure occurs (`scripts/invoke-gradle.ps1`).

## Qualification boundary

The source proposal had only isolated stub compilation and a six-case stub harness.
Those are historical, non-NeoForge results and are not qualification evidence for this PR.
The existing repository CI must compile and execute the new GameTests on the exact
PR commit. Its run links and terminal results belong in the PR description.
No new runtime or local server installation is required by this change.

## Revoked seed policy (TOOLS-194/195)

Persisted stock has structural validity independent of current tag membership.
The versioned codec requires known, losslessly round-trippable ItemStacks and a count
of 1–8. Bread and apple retain their fixed identities. A saved seed entry cannot
prove historical tag membership, so it remains recoverable even if that item is no
longer in the tag. This is not permission to insert arbitrary items.

`isSeedAdmissible` and `planInsert` check the current accepted-seeds tag;
`hasAdmissibleIngredients` checks current ingredient eligibility only, not transaction
permission. Future press integration must revalidate authoritative tags at execution.
Tag revocation cannot erase or lock stock; re-admission is evaluated from live tags,
not a cached boolean. The existing five-seed tag resource is unchanged.

The tag regression temporarily rebinds the real item registry's accepted-seeds tag,
restoring it in a finally block. It exercises admission, persistence, revocation,
manual withdrawal planning and re-admission under real NeoForge types. This is not
an end-to-end datapack file reload or synchronized GUI test.

`stage` performs content comparison, not anti-ABA revision fencing, admission or an
inventory commit. In particular it may persist or reduce existing revoked stock.
Do not wire it into gameplay without the separately approved transaction protocol.

No GUI, FIFO, pressing, repair, durability migration, release or merge is supplied
by this core PR. The existing direct-inventory press and durability 128 are untouched.
