# Proposed Muslee durable transaction design

Status: NEW ENGINEERING PROPOSAL, not a previously approved schema and not a
runtime activation permit. It preserves the fixed user requirements and identifies
where a complete implementation still needs proof. No private conversation extracts
belong in source control.

## Fixed constraints

- Cursor absorption and four-bar cursor output are all-or-nothing server operations.
- Durably correlate a random transaction UUID, durable press UUID and independent
  monotone durable writer generation. Do not persist ephemeral server generation.
- Journal is minimal outcome evidence, not full inventory snapshots or a replay log.
- Recovery is read-only reconciliation. Never replay, truncate, silently repair or
  infer cancellation from elapsed time. Unknown outcomes suspend dependent work.
- Journal and outcomes must agree with atomically persisted gameplay mutations.
- Single serialized writer; all authoritative mutations fenced at the mutation point.
- Immutable archived segments and coherent manifest. Rotate before a new entry
  reaches/exceeds the validated threshold; publish/read back before resuming.
- JCS RFC8785 UTF-8, exact Unicode, duplicate rejection; fixed u32 BE lengths;
  total entry at most 262144 bytes before allocation; SHA256 excluding digest field.

## Proposed bounded codec, independently implementable

Define journal envelope format version 1, all integers big-endian:

| Offset | Bytes | Field |
| --- | --- | --- |
| 0 | 8 | Magic: ASCII IEDMUSJ followed by NUL |
| 8 | 4 | Unsigned total entry length |
| 12 | 2 | Unsigned format version, 1 |
| 14 | 2 | Unsigned event kind: 1 engaged, 2 executed, 3 cancelled-no-effect |
| 16 | 4 | Unsigned header length, exactly 144 |
| 20 | 4 | Unsigned UTF-8 payload length, total minus 144 |
| 24 | 16 | Transaction UUID, RFC4122 random version4 |
| 40 | 16 | Durable press UUID, RFC4122 random version4 |
| 56 | 8 | Positive durable writer generation, signed-positive long range |
| 64 | 16 | Segment UUID, RFC4122 random version4 |
| 80 | 32 | Previous complete-record SHA256, zero only for verified genesis |
| 112 | 32 | Entry SHA256 over bytes 0..111 and payload 144..end |
| 144 | variable | Strict canonical UTF-8 JCS payload |

Identity domains must differ (transaction, press and segment); all required identities
are structural inputs, never authority grants. No additional global transaction
counter is introduced. The validated manifest binds segment identity, predecessor
digest and byte extent; chain continuity is an external reader obligation.
Predecessor means SHA256 of the preceding COMPLETE encoded entry, including its
stored digest field. At segment rotation the first successor entry references the
last entry of the preceding sealed segment. A zero predecessor is merely a structural
value; only independently verified genesis authorizes it. UUID binary form is the
network-order most-significant 64 bits followed by least-significant 64 bits, RFC4122
variant 2; transaction/press/segment are version4. Generation is 1..Long.MAX_VALUE;
unsigned values beyond that range are rejected and generation advancement at maximum
must fail, never wrap. Reject unknown versions/kinds,
non-exact lengths, overflowing arithmetic, bad UUID forms and digest mismatches.
Readers read the fixed header first, validate total/payload length, and only then
allocate the bounded remainder. No parser normalization or guessed trailing data.

For v1, use a fixed minimal string-only JCS object containing worldId, playerId,
beforeHash, afterHash and receiptHash, in UTF16 property order. UUID strings use
lowercase canonical spelling; hash strings use exactly 64 lowercase hexadecimal
characters. before/after identify complete affected-state encodings, not serialized
inventories. receiptHash is empty for engaged, mandatory for terminal outcomes.
worldId and playerId are canonical lowercase, hyphenated, nonzero RFC4122-variant
UUID strings supplied by verified world/player authorities. Their version is not
restricted to4 (offline player identifiers can be version3). No codec creates identity.
For engaged records afterHash describes the intended after-state. Executed records
must later match that intended state through external complete-history validation.
Cancelled records use afterHash equal to beforeHash, describing unchanged actual
state; equality is necessary, not proof of no-effect. receiptHash is the ONLY empty
value allowed, and only on engaged records.
Reject malformed UTF-8, BOM, unpaired surrogates, noncanonical escapes/order, duplicate,
unknown or missing properties and trailing bytes. The fixed ASCII UUID/hash schema
can be validated by exact canonical regeneration instead of general JSON parsing.
Decoded objects and exposed arrays must be detached/immutable.
Large numeric JSON ambiguity is avoided: future numeric counters must be explicitly
schema-versioned; no arbitrary JSON fields or numbers are accepted in v1.

This codec can prove byte framing/canonicality/integrity only. It cannot prove
writer authority, identity uniqueness, history continuity or actual execution.
Do not wire it to a filesystem writer until the remaining design is qualified.

## Proposed transaction ordering, not yet a qualified commit engine

1. Acquire the qualified writer authority and server serialization domain. Validate
   identity, session, queue, memory bounds, complete source/cursor/reserve/durability
   snapshots, revisions and output capacity. No mutation before every gate passes.
2. Persist and verify a minimal engaged record. Its expected before/after hashes
   do not themselves assert a gameplay outcome.
3. Under the same authoritative domain, stage the complete affected player state
   and a transaction receipt. Commit BOTH cursor/press/reserves/durability and
   receipt through ONE qualified persistence boundary. Protect against stale asynchronous
   saves, other inventory owners, receipt loss in later saves and crash backup selection. Publish client state only
   after the outcome is verified.
4. Append/force/read back the executed record referencing the persisted receipt.
   If terminal journal recording fails, do not repeat gameplay: suspend and reconcile
   read-only against the atomically persisted receipt.
5. A cancelled-no-effect record is allowed only if authoritative evidence proves
   no mutation occurred. A timeout or absent terminal record never supplies that proof.

## Remaining material architectural gap: Minecraft persistence

Minecraft menu cursor state is not automatically a participant in the ordinary
playerdata save contract. A coordinated durable cursor/press receipt is needed;
writing playerdata plus an unrelated sidecar is NOT atomic. An append-only journal
alone cannot make it atomic. A rollback after an uncertain disk write can duplicate
items and is forbidden without proof of the old durable state.

Proposed research route: a server-owned player persistence participant that saves
only affected state/receipt alongside the normal player snapshot through one atomic
save boundary; investigate native save/load, carried cursor close/death/logout,
capability persistence, third-party mutation hooks, stale snapshots and save failure.
Normal game saves are not journal inventory snapshots, but this distinction must
be documented. Do not yet assume that loading a custom cursor participant is allowed
recovery replay or that it integrates with vanilla close/drop semantics.

A complete supported-platform durability argument must cover file replace, fsync,
directory persistence, backup selection and readback on both target operating systems.
No arbitrary platform can be declared qualified from one Linux CI run.

## Fail-closed recovery and writer lifecycle

Rotation must reserve/qualify enough space for the whole multi-record transaction
before engagement; a segment cannot rotate between its atomic transaction records.

On startup/lost writer/uncertain save, read verified manifest, sealed/active segments,
block registry and persisted gameplay receipts without modifying them. Classify only
provable executed or no-effect outcomes. A missing/corrupt/divergent authority or
receipt leaves the affected domain blocked; no automatic writer reset, history rewrite
or generation reuse. Reconciliation must not append a missing terminal record.
An unverifiable UUID block registry globally blocks dependent transactions. Authority
loss must quietly close affected GUIs without item mutation, with only the previously
approved minimal diagnostics. Readback alone is not a durability proof. Administrative repair is a separate explicit operation, not a
normal recovery branch.

## Review and acceptance

Review the proposed format and ordering against fixed requirements before code.
The bounded codec may then be implemented and tested independently, explicitly
marked new and inactive. Mutation ordering and durable writer cannot be activated
until real engine failure-injection tests and source-level atomicity/ownership proof
close the persistence and memory/mutation-tracking gaps. This proposal does not
claim an end-to-end working Muslee GUI.

## Codec implementation boundary and local evidence

`MusleeJournalEnvelope` implements only the proposed fixed v1 structural codec.
The five fixed JCS properties are UUID/hash strings, so accepted payloads are an
ASCII subset of canonical UTF-8 JCS: 276 bytes for engagement and 340 for terminal
records. There is no general JSON parser, arbitrary number conversion or extension
field acceptance. A future schema extension requires a distinct version.

The bounded stream reader validates the 144-byte header, unsigned lengths and
exact kind-specific payload length before reading the remainder. It reads one record
and leaves the next record in the stream. Explicit ENTRY, END_OF_STREAM and INVALID
statuses distinguish a clean boundary EOF from a truncated or corrupt record. Even
clean EOF is not proof of a complete authoritative history: the future reader must
verify the manifest's exact segment extent and expected final digest.
Invalid/truncated input is not resynchronized;
transport I/O failures propagate. There is no file write, truncation or recovery action.
Array decode rejects trailing bytes; the returned entry and array getters are detached.
A zero predecessor is accepted structurally but does not prove genesis. A matching
checksum or receipt hash is never an outcome, trust, continuity or writer-authority grant.

Local Java 21 compiler-module build with `-Xlint:all -Werror` and standalone cases
passed. The stripped environment lacks the executable javac and its `--release 21`
symbol archive; actual compiler reports 21.0.13. Real Gradle/JUnit integration is
separately required through repository CI. No codec-memory subtotal is presented
as the complete aggregate retained/peak-heap qualification.
