# Inactive integrated authority-chain experiment

This is a test-only composition of real participant1 NBT, compressed native candidate
files, selection1 framing and the read-only selection reader. It adds no production
owner, writer, publisher, format upgrade or player-loading policy.

HEAD, original outcome-snapshot linkage and terminal evidence are deliberately
SIMULATED Java records/test bytes. They are not implementations of proposed HEAD1,
receipt2 or journal2. In particular, existing participant1 does not suddenly contain
an originalOutcomeSnapshotUUID: an immutable external test claim models that missing
future binding without changing v1 bytes.

Six GameTests compose:
- Native snapshot -> candidate file -> selection -> modeled HEAD/terminal -> test-only
  fresh-player load and detached cursor assignment.
- Candidate-only, HEAD-without-terminal and missing-candidate cuts, modeled UNKNOWN.
- Competing and stale valid selections under a separately fixed test HEAD.
- Incorrect terminal/original-snapshot references and missing ancestry.
- An EMPTY inventory descendant retaining original receipt and rejecting an older
  carried request under the selected newer HEAD.
- Generation regression, foreign enrollment and digest-key ancestry alias rejection.

Reconciliation uses read-only APIs and compares directory filenames/content hashes.
UNKNOWN performs zero player loads/cursor assignments and preserves target sentinels.
Positive targets start empty; no occupied cursor is replaced. Source fixtures remain
unchanged. This demonstrates data reconstruction, not exclusive live ownership:
source and target are separate disposable objects in the test.

The enrolled origin, complete modeled registry and fixed HEAD are explicit premises.
If an actor rewrites the authoritative HEAD itself to an old prefix, checksums alone
cannot detect that loss of authority. No tested atomic publisher or durable writer
prevents it here. Directory hashes establish unchanged names/bytes, not absence of
all metadata changes. Synthetic cuts are not killed processes or power-loss tests.

These tests inform the integrated design while lifecycle/failure-policy decisions
remain unresolved. They authorize no world/player blocking behavior, deployment,
automatic recovery repair, journal replay or runtime activation.

The modeled ancestry contains one outcome and later ordinary-save descendants only:
every ancestor retains the same fixture receipt. INITIAL enrollment, a second
transaction and cancellation transitions are not covered. Terminal evidence exists
only in memory; these cuts test missing evidence, not durable journal crashes.
The retirement fixture manually constructs inventory/EMPTY state rather than testing
a native transfer or atomic installation. Separate native-removal tests cover that
existing engine behavior without qualifying the proposed owner.
