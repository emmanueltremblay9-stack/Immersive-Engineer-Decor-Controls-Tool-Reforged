# Muslee cursor-only protocol v2 and session recognition

This increment implements production wire-format and bounded recognition classes.
It does not register a payload, open a GUI, queue work or transfer items. It builds
on the approved reserve-core merge `6bf88902fd95bcb44e57202631b2cccd33686e02`.
No runtime activation is safe until the remaining transaction prerequisites exist.

## Implemented contract

- TOOLS-288–295: server-generated session UUIDv4, authoritative access check before
  action recognition, binary UUIDv4 command IDs, session-long deduplication. Invalid
  command UUIDs do not consume history; valid recognized IDs do even when command
  parameters/type are malformed. No refusal produces logging or player feedback.
- TOOLS-296–303: maximum 8192 bytes before copying/parsing, fixed big-endian version
  2, session UUID then command UUID. Unknown versions and sessions do not recognize
  IDs. The caller must enforce the same bound before allocating the incoming network
  buffer; this byte-array boundary alone cannot constrain transport allocations.
- Cursor-only replacement of the former TOOLS-304–310/319 wire grammar:
  type at byte 34, uint16 BE parameter length at bytes 35–36.
  ABSORB is type 1 and PRESS is type 3. Both have zero parameter bytes and are
  exactly 37 bytes long. The Java command has only action ID and type: no client
  inventory slot, material selector, amount, stack or reserve value is accepted.
  Type 2 (legacy WITHDRAW) is reserved and always rejected, including with zero
  parameters. Version 1 packets are rejected before authorization or recognition.
  A version-2 packet carrying any old INSERT/WITHDRAW argument or extra bytes is
  malformed, and its valid recognized action ID stays consumed.
  These are intent commands only; future execution must read the authoritative
  carried cursor stack and server reserves within the qualified transaction.
- UUIDv4 uses RFC variant 2 as represented by Java UUID.variant(). UUIDs use most
  significant 64 bits followed by least significant 64 bits, both big-endian.
- At the 4096th distinct recognized action, the state becomes INVALIDATING and that
  command cannot proceed to FIFO. This conservative ordering enforces invalidation
  at the approved bound, not after a 4097th command. The IDs remain until the owner
  confirms safe invalidation, and CLOSED can never reopen or reuse its session ID.

## Upgrade boundary and dormant status

This replaces the existing protocol rather than introducing a parallel helper.
The previous v1 foundation was dormant: repository production callers of
`MusleeCommandProtocol` are limited to `MusleeSessionIngress`, and ingress has no
production caller outside its own definition. There is no registered payload or
live gameplay invocation to migrate, so rejecting v1 does not strand clients of
a shipped GUI. This is not permission to register v2 or enable queue admission.

The existing usable `MusliBarPressItem` gameplay is untouched. It must remain
available until the cursor-only replacement passes every runtime safety gate.
No GUI art, networking, queue gates or release configuration changes here.

## Boundaries that must not be bypassed

`RECOGNIZED` means a valid decoded command, not authorized inventory mutation or
queue admission. `receive` calls a live authoritative-access callback every time;
it does not implement that callback or substitute a cached permission. Its local
monitor serializes recognition, but does not make player inventory/GUI state atomic.
Integration must run on the server thread and supply the complete session/ownership
validation. Packet copies prevent callback-side changes from changing parsed input.

`invalidate` stops recognition but cannot close a GUI, purge a queue or protect an
engaged transaction by itself. `finishInvalidation` must be called only after those
external steps are safely complete. The class retains IDs until that acknowledgement
and never replays commands. Re-entrant receive attempts are refused without history
mutation; closing during recognition cannot release history.

## Remaining dependencies before GUI activation

1. TOOLS-272–287: actual synchronized GUI session/ownership lifecycle and per-press
   FIFO, 32 waiting commands, 30-second monotonic expiration, protection of active work.
2. TOOLS-309–347: complete cursor/reserve captures plus reliable
   revisions including internal component/count changes and mutate-then-restore ABA.
   Existing ItemStack equality, menu state IDs or setChanged alone are not sufficient.
3. TOOLS-348–368: bounded capture reservations (256 KiB per press), exclusive commit
   ownership, safe release, bounded queue scanning and lifecycle invalidation.
4. Atomic simulation/revalidation/commit of press reserve and real inventory.
5. Register payload/menu/screen and prove cursor absorption/pressing end-to-end only when all
   prerequisites above are present. Pressing, repair and durability migration remain
   separate work; historical immediate pressing and durability 128 are unchanged.

No existing networking/menu/item registration or gameplay file is modified.

## Validation

JUnit tests exercise exact v2 wire layout, all 65536 version values, every type
byte, all 65536 parameter-length values for both cursor commands, rejection of
every legacy one-byte payload, strict packet sizes, authoritative-check ordering,
UUID validation, replay retention, saturation and lifecycle, packet copying and
re-entrancy. Test-only ingress/FIFO composition also rejects legacy withdrawal
without allocating a queue ticket or releasing the action ID for replay.

This increment is source-reviewed with `git diff --check` and repository caller
searches. No local Java compilation, JUnit execution or runtime qualification is
claimed. Existing NeoForge compilation, JUnit, GameTests and client CI must pass
on the exact PR head before merging. In-game qualification remains a separate
activation gate even after green CI.
