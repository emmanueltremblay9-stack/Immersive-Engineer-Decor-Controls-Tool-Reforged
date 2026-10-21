# Muslee detached captures and real ownership

This lot follows merged reserve, protocol and FIFO/revision foundations. It adds
concrete detached capture storage and actual lifetime handling; it does not activate
menus, networking, inventory transfers, pressing, repair or durability migration.

## Complete representation is fail-closed

An ItemStack shallow copy is not proof that arbitrary component objects are detached.
The disk codec omits transient components, and network codecs may deliberately omit
values. Successful serialization alone therefore cannot qualify a complete capture.
Only representations whose complete effective components and explicit removals can
be verified are accepted by this development capture layer. Unsupported or lossy
representations must fail rather than silently discard data. Exact coverage is
specified by the production class and its GameTests, not by a universal-codec claim.

The initial support is deliberately narrow: primitive durability/stack/repair values,
rarity/glint, simple food without effects or conversion items, audited empty
lore/enchantment/attribute defaults, and explicit CUSTOM_DATA with no mutable item
prototype. Rich text, nonempty attribute/enchantment graphs, unknown/transient
components and mutable defaults are refused. These are safety refusals in an
inactive adapter, not approved removal of future gameplay support requirements.

Private serialized buffers must not retain source ItemStacks, mutable component
objects or mutable NBT graphs. Reconstruction must create a fresh independent graph;
release must clear owned buffers and remove references exactly once. Logical item
identity is independent of current seed-tag admissibility, so revoked seed stock
remains representable. Capturing is not insertion or pressing authorization.

## Actual ownership, not just accounting markers

The queue owns its accepted capture directly, not through a side map updated after
admission. Admission, ownership transfer and terminal cleanup use its serialization
boundary. Engagement transfers ownership without copying; session/clock invalidation
preserves engaged work. Unverified finalization cannot release an engaged capture.
Expired, refused or invalidated waiting work must release actual buffers as well as
the reservation ledger. No transaction is committed by these primitives.

## Explicit unresolved memory qualification

TOOLS-350/351 require a reliable upper bound including nested components, immutable
copies, capture holders, structures and memory overhead, cumulatively at most 262144
bytes for non-engaged commands per press. Encoded byte length alone is **not** that
bound. Neither guessed JVM constants nor a number supplied by a test establishes it.

No qualified allocation profile is supplied by this lot. Runtime capture admission
therefore remains refused. Package-private development fixtures exercise real
ownership transitions but are not a production proof provider or permission to
admit commands. They must never be wired to gameplay. No instrumentation, JVM setup
change or new installation is included.

The owner approved investigating a Linux x86_64 / exact Java 21 server profile,
without installation, JVM changes, instrumentation or activation. Existing CI run
39558731667 reports Eclipse Adoptium OpenJDK 64-Bit Server VM
21.0.12.1+1-LTS on Linux amd64 (Ubuntu 24.04 runner image 20261012.334),
Minecraft 1.21.1 / NeoForge 21.1.230. This identifies a candidate, not a qualified
allocation profile. The workflow still selects a floating Java 21 toolchain.

A later qualification must identify a supported runtime build and object-layout
configuration, enumerate the complete retained graph and peak copies, justify a
conservative bound and reject unqualified configurations. Java's getObjectSize is
specified as an implementation-specific approximation; inspecting it alone cannot
replace that argument. Runtime-profile approval is distinct from installation,
configuration changes and GUI activation.

## Remaining activation gates

- Qualified memory bounds before FIFO admission, with atomic reservation/adoption.
- Exhaustive mutation tracking for all source/destination/compartment origins,
  including in-place nested changes and ABA, plus revision reset exclusion.
- Authoritative session/press binding, complete destination/read-set validation,
  simulation and atomic inventory/reserve commit.
- Safe GUI/network/scheduler registration only after those guarantees are proved.

## Sources

- TOOLS-351: https://chatgpt.com/c/6ad47cfc-7324-83ea-841b-252d80630231?messageId=7b719a36-d64f-4478-a8c9-7cac7e9cabe1
- NeoForge components: https://docs.neoforged.net/docs/1.21.1/items/datacomponents/
- Java 21 instrumentation limitations: https://docs.oracle.com/en/java/javase/21/docs/api/java.instrument/java/lang/instrument/Instrumentation.html

Validation and exact-commit CI evidence are recorded in the PR. No result here is
proof of an active loading/withdrawal GUI or qualified runtime memory budget.
