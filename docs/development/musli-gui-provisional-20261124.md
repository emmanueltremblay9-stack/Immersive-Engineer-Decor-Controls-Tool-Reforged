# Musli Bar Press GUI   isolated runtime candidate

Status: **DRAFT / PROVISIONAL SCREEN / TRANSACTION GATE CLOSED**.

## Provenance and isolation

- IMPL repository: `emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged`
- Base `main`: `e8b21c91d5a7a3a6beb9d0b12c71e8bccc0f8d2c`
- Dedicated branch: `feat/musli-gui-runtime-20261124`
- No dependency on PR #196, #209 or #210; no Hopper/Dropper/Lab artwork replacement.
- `AGENTS.md` at the exact base was consulted.

## Approved concept (external, not yet imported)

GUI reference: [approved retained concept](https://github.com/emmanueltremblay9-stack/Master-Generalist-GUI-Architect-Designer-DEV/blob/0d25b9baa1e25db736340180fe81c789f4d89eca/candidates/iedctr/retained-concepts/2026-11-05/MANIFEST.json).

- `musli-approved.png`, 1536 x 1024, 1,971,553 bytes.
- SHA-256 `b9f5204c7eb2da0b8b64450a4cf065cf0fa4762d4d54a490c8eb482c0303faa9`.
- Git blob SHA `786982f9b670a7b67e810dac0797536753c8963a`.
- Owner status: **OWNER_APPROVED_VISUAL_CONCEPT_NOT_RUNTIME_ASSET**.
- The source image is fully painted. Its original bytes must remain unchanged; deriving
  a neutral background requires access to those exact bytes, erasure of painted gauge
  values and inspection of pixel positions. This worktree intentionally does not
  contain a guessed replacement or a fabricated copy. The currently rendered screen
  is a *code-drawn, explicitly marked provisional layout*.

## Runtime boundary implemented

- Main-hand/offhand right-click enters a real server-side menu via NeoForge's
  `ServerPlayer.openMenu` and extended `IMenuTypeExtension` client opening data.
- The server generates a UUIDv4 MusleeSessionIngress per opening. The client receives
  only the hand/session identifier, not inventory or reserve ownership.
- Menu has exactly 36 ordinary player slots, no ingredient/output slot, and three
  passive reserve values derived from the held press on the server (0 8).
- Funnel-plus-above-opening = ABSORB intent; depicted bar = PRESS intent.
  No withdrawal, visible machine inventory or animation.
- Network input is bounded to 8,192 bytes and uses existing v2 UUIDv4 session,
  deduplication, zero-parameter cursor-only intent decoder.
- Access is revalidated against the currently open menu, owning player, bound hand
  and item before intent recognition. Closed menu invalidates its session.
- **No live transaction is executed**: `MusliBarPressMenu.TRANSACTIONS_QUALIFIED=false`.
  The source-backed reserve and cursor batch planning components are unchanged.
  In particular no inventory, cursor, durability, press reserve or world item is
  mutated by an ABSORB or PRESS packet.

## Why the write gate remains closed

The original `MusleeCommandQueue.admitCapture` deliberately returns
`INVALID_BOUND`; approved source states the complete retained-memory reservation
and atomic player snapshot/journal participant have not been qualified. The native
player serialization tests establish that the menu cursor is not in the player NBT,
and close/full-inventory drops cross into world persistence. A vanilla return to
inventory on ordinary close is accepted, **but cannot establish crash durability**.
No fabricated durable receipt, guessed persistable cursor or login-blocking fallback
is introduced. No unapproved 128-to-64 durability migration is made.

## Validation required before changing TRANSACTIONS_QUALIFIED

1. Exact binary artwork import with SHA-256 and 1536x1024 check; preserve original,
   derive and verify neutral plate and server-controlled gauges.
2. Prove real capture memory bounds, ownership, revision-fence, FIFO/timeouts,
   all-or-nothing commit, and journal/complete player persistence.
3. Execute real GameTests for successful ABSORB/PRESS, capacity and incompatible
   cursor rejection, no partial consumption, stable UUID deduplication,
   close/logout/death/drop/restart and uncertainty recovery.
4. Verify client-screen opening and native screenshots. A successful Java
   compile alone is not a client or full durability proof.

Existing six immediate-press GameTests remain historical fixtures and call the
old algorithm explicitly. They are not used as evidence that the *new* right-click
entrypoint creates items.
