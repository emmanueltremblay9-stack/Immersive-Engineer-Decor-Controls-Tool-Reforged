# Inactive native snapshot candidate I/O

This writes real compressed native player NBT containing the cursor participant to
an unselected candidate file. It is not a journal inventory snapshot. It never
writes a vanilla player filename, replaces an existing file, selects authority,
restores gameplay, publishes a manifest or registers a live save/load hook.

## Operation and outcomes

`MusleeSnapshotCandidateIO.writeNewCandidate` requires a trusted isolated candidate
directory, a new filename ending `.muslee-candidate.nbt.gz`, a complete native player
snapshot and independently supplied expected world/player identities. It validates
the participant, detaches the snapshot, revalidates, and then:

1. Opens with CREATE_NEW and NOFOLLOW_LINKS. Existing file content is never changed.
2. Writes native NBT through gzip, finishes/flushes compression, then forces the file.
3. Closes wrappers/channel, opens and forces the containing directory. Unsupported
   directory forcing is a failure, not a silent durability downgrade.
4. Reads back with the supplied NBT accounting limit. It checks complete compressed
   length and SHA256, no extra decompressed content, exact native NBT equality and
   expected participant identity. Raw compressed suffix bytes participate in hashing.

Outcomes:
- NOT_CREATED: rejection before the create attempt, or explicit CREATE_NEW
  FileAlreadyExistsException. The existing-file case means no file was created by
  this operation, not that the path is absent.
- UNKNOWN: any other creation error or any failure after creation. Partial or complete
  files remain untouched. No cleanup, fallback, rollback, gameplay retry or recovery
  write is attempted.
- VERIFIED_CANDIDATE_NOT_PUBLISHED: the candidate completed these checks. A detached
  file-size/hash receipt describes bytes, not transaction execution or authority.

Successful readback and force calls are NOT a complete power-loss durability proof.
"Immutable candidate" describes this method's no-overwrite behavior, not permission
protection against other actors. The future writer must own the trusted directory,
prevent concurrent mutation and establish authoritative snapshot selection.

## Real fault injection

The package-private test seam injects failures before creation, after actual first
compressed output bytes, before gzip completion, after serialization, around file
and directory force, and around readback. Tests use actual temporary files rather
than an in-memory storage simulation. Candidate creation/write/close errors are
conservatively uncertain unless a specific no-create outcome is established.

The API does not parse attacker-selected paths, prove parent-directory ownership,
or qualify arbitrary filesystem providers. Source snapshot copying and serialization
are not aggregate heap qualification. The supplied NBT accounting limit bounds the
readback parser only, not every codec or player-serialization allocation.

## Integration boundary

The caller still owes a complete native snapshot, writer fencing, required participant
state retention, stale-save protection, receipt history and authoritative selection.
No `.dat_old` fallback can be inferred from this operation. A future publication
protocol must establish which verified candidate is authoritative without reviving
an older carried cursor. World-item transfer, integrated-host snapshots, runtime
memory bounds and supported-platform crash testing remain separate unresolved gates.

## Engine/filesystem tests

Six GameTests use native ServerPlayer snapshots and fresh real temporary directories.
They independently hash compressed files, read native NBT and load inventory into a
same-identity fixture; test cursor-to-inventory EMPTY candidates without rewriting
older files; preserve existing sentinel files; exercise every failure seam; retain
corrupt/truncated/appended readbacks; and reject wrong identities, missing participant,
wrong suffix and insufficient parser budget. Only test-owned temporary files are
cleaned after assertions. Production failure paths never delete candidates.

DURING_WRITE fires after the gzip header's first actual write, before AFTER_OPEN.
It proves retained partial compressed output, not interruption halfway through NBT
payload encoding. These are process-level injected I/O failures, not abrupt power-loss
or actual filesystem crash qualification. Successful test execution is required via
CI; it must not be inferred from source review.
