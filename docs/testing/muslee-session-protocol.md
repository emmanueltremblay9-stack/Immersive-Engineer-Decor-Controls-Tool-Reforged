# Muslee GUI protocol and session recognition foundation

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
  1, session UUID then command UUID. Unknown versions and sessions do not recognize
  IDs. The caller must enforce the same bound before allocating the incoming network
  buffer; this byte-array boundary alone cannot constrain transport allocations.
- TOOLS-304–310/319: type at byte 34, uint16 BE parameter length at bytes 35–36.
  INSERT is type 1, one byte source inventory index 0–35, exactly 38 bytes.
  WITHDRAW is type 2, one byte compartment 1 bread / 2 apple / 3 seeds, 38 bytes.
  PRESS is type 3, zero parameters, exactly 37 bytes. It is recognized as valid wire
  grammar only; no pressing implementation or activation is included.
- UUIDv4 uses RFC variant 2 as represented by Java UUID.variant(). UUIDs use most
  significant 64 bits followed by least significant 64 bits, both big-endian.
- At the 4096th distinct recognized action, the state becomes INVALIDATING and that
  command cannot proceed to FIFO. This conservative ordering enforces invalidation
  at the approved bound, not after a 4097th command. The IDs remain until the owner
  confirms safe invalidation, and CLOSED can never reopen or reuse its session ID.

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
2. TOOLS-309–347: complete source/compartment/destination captures plus reliable
   revisions including internal component/count changes and mutate-then-restore ABA.
   Existing ItemStack equality, menu state IDs or setChanged alone are not sufficient.
3. TOOLS-348–368: bounded capture reservations (256 KiB per press), exclusive commit
   ownership, safe release, bounded queue scanning and lifecycle invalidation.
4. Atomic simulation/revalidation/commit of press reserve and real inventory.
5. Register payload/menu/screen and prove load/withdraw end-to-end only when all
   prerequisites above are present. Pressing, repair and durability migration remain
   separate work; historical immediate pressing and durability 128 are unchanged.

No existing networking/menu/item registration or gameplay file is modified.

## Validation

JUnit tests exercise wire layout, all parameter boundaries, malformed envelopes,
strict sizes, authoritative-check ordering, UUID validation, replay retention,
saturation and lifecycle, packet copying and re-entrancy. Existing NeoForge
compilation/GameTests/client CI must pass on the exact PR head; PR description holds
terminal evidence. Production sources compiled locally with the installed Java 21 compiler module
(`java -m jdk.compiler/com.sun.tools.javac.Main -source 21 -target 21`). This is
ordinary Java compilation, not NeoForge or JUnit execution. No software was installed.
