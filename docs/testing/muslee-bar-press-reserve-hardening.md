# Muslee reserve native stack limits

## Scope and baseline

Independent follow-up to reserve core PR #68 and protocol/session PR #69.
Reviewed base: `5b4c05de6d17ab19199bed0e942fb0cbae07c3cb`.
Only the inactive reserve model, a dedicated GameTest class and this document change.
No GUI, payload, FIFO, transaction adapter, durability migration or repair is activated.

Each compartment keeps its nominal capacity of eight. A stored ItemStack must also
respect its own native/component-defined maximum: effective capacity is the smaller
of eight and `getMaxStackSize()`. Insertion returns the actual transferable amount
without shrinking the offered stack. Malformed offered or persisted stacks exceeding
their own maximum fail closed; no count is clamped and no saved bytes are rewritten.
A rejected persisted payload remains intact for separately controlled recovery;
this patch does not introduce a migration or an automatic repair.

Tag membership remains independent of persisted stock validity. Revoked extension
seeds with structurally valid stacks remain readable and withdrawable under the
existing TOOLS-194/195 contract. Current ingredient eligibility is still evaluated
separately from persistence. Ordinary bread/apple/seed capacities remain 8/8/8.

## Regression cases

`MusliBarPressReserveHardeningGameTests` contains four required cases:

- Several insertions into a component-limited stack accept only remaining capacity.
- An offered stack exceeding its own maximum is refused without source mutation.
- An over-limit stored payload is refused without modifying the original press.
- Supported vanilla CUSTOM_DATA replacement does not alter an earlier snapshot.

The last case is deliberately narrow. It does not prove deep immutability for all
third-party component values, live mutation tracking, anti-ABA protection, complete
heap accounting or atomic inventory commits (TOOLS-346/347 and later integration).
The existing ItemStack-copy snapshots must not be advertised as that proof.

## Qualification

Required on the exact PR head using the existing Java 21 / NeoForge build:

```sh
./gradlew compileJava processResources --stacktrace
./gradlew test --stacktrace
./gradlew runGameTestServer --stacktrace
./gradlew build --stacktrace
```

Follow applicable AGENTS.md safeguards. Isolated stub/model tests do not substitute
for these engine tests. CI links and observed terminal results belong in the PR
receipt; this document does not predeclare them successful. No merge, release,
installation, workflow change or additional authority is part of this patch.
