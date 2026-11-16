# Proposed player-snapshot selection reader

NEW ENGINEERING PROPOSAL, not a previously approved on-disk selection format.
This separate player-snapshot metadata is not the durable journal or its segment
manifest. The journal's canonical JSON envelope remains unchanged. Only a pure
selection codec and a read-only candidate validator are implemented; no publication,
writer lease, live save/load registration or activation is supplied.

## Proposed exact format

All numbers big-endian, exactly176 bytes:

| Offset | Bytes | Meaning |
| --- | --- | --- |
| 0 | 8 | ASCII IEDMUSP and NUL |
| 8 | 4 | u32 total length176 |
| 12 | 2 | u16 version1 |
| 14 | 2 | reserved flags0 |
| 16 | 16 | world UUID |
| 32 | 16 | player UUID |
| 48 | 8 | positive signed-long durable selection writer generation |
| 56 | 16 | random version4 snapshot UUID |
| 72 | 8 | positive signed-long compressed candidate size |
| 80 | 32 | SHA256 of every compressed candidate byte |
| 112 | 32 | predecessor selection digest |
| 144 | 32 | SHA256 of bytes0–143 |

World/player UUIDs are nonzero RFC-variant2; offline player version3 is supported.
Snapshot UUID is version4. No additional global transaction counter is introduced.
The selection digest and predecessor reference mean the stored SHA256 of the first
144 bytes, not the journal's complete-entry chain hash. A zero predecessor is only
a structural value; verified genesis remains an external obligation.

Unknown versions/flags, extra/truncated bytes, malformed IDs, nonpositive generation
or lengths, and invalid digests reject. Candidate filename is derived only from the
canonical UUID plus `.muslee-candidate.nbt.gz`; no path string is accepted on disk.

## Reader and trust boundary

`readExpected` requires independently supplied world/player, expected CURRENT digest,
expected predecessor/genesis context, expected selection generation, trusted candidate
directory, and finite compressed-byte/NBT parser limits. These cannot be obtained by
copying fields from the file currently being checked and then calling them trusted.

Predecessor plus generation alone is insufficient: two different valid successors
can share both. The expected current digest distinguishes them. The reader validates
bounded exact manifest framing before resolving its UUID-derived candidate. It checks
actual file size before gzip decoding, complete compressed SHA256, full decompressed
NBT consumption and strict native participant identity/structure. Missing, corrupt or
unavailable data yields no snapshot; there is no backup search, repair or write.

A candidate SHA256 binds the complete snapshot and its participant receipt, so another
receipt hash is not needed for byte integrity here. A retained transaction receipt can
predate the selection's writer generation; equality is not invented as an invariant.
Returned native snapshots are detached. The result deliberately says
VALIDATED_CANDIDATE_NOT_AUTHORITY: it neither verifies the source of the expected
anchor nor grants restoration or execution permission.

## Unresolved publication/history contract

No code here establishes the authoritative current digest, complete selection history,
writer exclusivity or atomic manifest publication. Journal/outcome/selection linkage
must be explicitly designed and qualified before load integration. The new proposal
must not be mistaken for proof that an older carried snapshot cannot be resurrected.
Existing receipt schema has no snapshot identifier; any new binding must be explicit
and versioned rather than creating a circular hash reference or silently reinterpreting
old receipts. The journal and normal snapshot remain separate storage purposes.

Trusted path ownership/exclusion, parent-directory races, stale saves, integrated-host
load selection, participant receipt retention, cross-domain drops, full memory bounds
and platform power-loss durability remain gates. No journal replay or automatic
reconstruction of missing authority is introduced.

## Validation coverage

The pure-Java suite passes1704 synthetic assertions: exact framing/digest offsets,
all single-bit corruptions, every truncation, resigned invalid versions/flags/UUIDs/
numeric boundaries and immutable accessors. A JUnit wrapper invokes the same suite.
Six real-file GameTests cover native candidate reads, competing valid heads with the
same predecessor/generation, wrong independent expectations, finite limits, missing/
corrupt/truncated/trailing files, strict participant validation even under a matching
file hash, detached results, older retained receipt generation and invalid API inputs.
Successful and rejected reads compare directory filenames/content hashes for no writes.
Actual engine/filesystem execution remains separately required through CI.

Read limits rely on the explicit trusted exclusive-directory precondition; concurrent
file growth is not independently bounded by the initial size check. Gzip byte streams
are checked against the externally expected complete digest, not a claim of canonical
gzip encoding. No parser success establishes complete history or a trusted anchor.
