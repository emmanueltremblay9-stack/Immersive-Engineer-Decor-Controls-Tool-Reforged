# Small Mineral Smelter Hybrid — implementation candidate

Status: **PROPOSED / DRAFT PR**. This branch implements a playable candidate based on the merged *design draft* `docs/development/small-mineral-smelter-hybrid-draft-v0.4.md`; merging the draft was **not** owner approval of numerical tuning.

## Runtime scope

- **Lava (default):** accepted mineral + paid heating progression => 1,000 mB lava, no free Magma item.
- **Magma:** same input + paid heating to 90% => 1 Magma Block, no lava byproduct.
- **Obsidian:** use at least 1,000 mB lava from top-fill or a prior mineral heat lot, then cool over a separate period => 1 Obsidian. No water is consumed.
- Mode requests are sent through existing `MachineActionPayload` action 31, with expected revision and server-side menu validity checks. Switching during a batch queues the next mode, without refunding or duplicating the current batch.
- A real internal 4,000 mB tank accepts only lava. Top `IItemHandler` / `IFluidHandler` receive, bottom handlers extract, and only the rear-facing energy capability receives FE. The backing legacy "Heat" slot index 1 is retained for save migration safety but is **not** a virtual fuel slot.
- Worldly-container and NeoForge capabilities use the same side contract; lava and item output can be pushed downward when a compatible receiver is present.
- The server updates temperature, phase/0-5-10-15 comparator, FE use, tank volume, progress, status, and contact heat state. The surface inflicts candidate damage when contacted while hot; no remote damage aura.
- The screen is a real Minecraft `AbstractContainerScreen` with **two actual machine slots and 27+9 vanilla slots**; it reads server state and sends only validated intents.

## Balancing still proposed, not canon

The work cadence is **1 progress unit per 10 paid active heat ticks**, at 16 FE per paid tick, with output at 90 (Magma) or 100 (Lava); cooling advances 1 unit every 10 game ticks without FE. Up to 16,000 FE and 4,000 mB lava follow the existing IEDCTR scale. Contact damage is 1 health point (half a heart) no faster than once per 20 entity ticks. These values require owner review and actual-world balancing. Fluids/items must not be lost or duplicated under simultaneous transfers.

Each heat progress unit now requires ten successfully paid, unredstoned active ticks. Pauses do not advance that counter, and its partial remainder is saved with an active batch so reloads cannot shorten the proposed cost or duration.

## GUI artwork and provenance — outstanding integration gate

The original conversation-generated **1168×1346** faceplate (`Small Mineral Smelter Control Panel(1).png`) and the 44 MB v0.5 artifact package exist in the ChatGPT conversation, but their binary bytes were **not imported in this GitHub PR**. This draft uses the already committed IEDCTR steel texture `textures/gui/block_breaker_v08/background.png` to keep the screen runnable. The per-machine panel geometry, modes, server values and vanilla slots are real Java GUI work; **the exact custom faceplate is still missing** and must not be described as pixel-perfect in Minecraft until the 1168×1346 artwork is committed, visually reviewed and rendered at several GUI scales. Do not silently substitute a lower-fidelity generic screenshot as "exact original".

Toolkit used for source-format guidance: `emmanueltremblay9-stack/pixel-art-skill-toolkit@98eecf77190d5716820ec659f8aeb790513c8e93`, `skill/pixel-art-json/SKILL.md`. The toolkit is an **authoring dependency**, not a runtime library added to the mod.

## Save compatibility

Preserve the full 27-entry backing inventory, including legacy input 0, unused 1 and output 2, FE and fluid. A new NBT version identifies the hybrid. Legacy saves reset obsolete *work* (not items, FE, lava); revision/selected batch/temp/new mode are saved separately. A future migration must account for pre-existing partially progressed cycles and fluid on dismantle; this PR does not decide where dismantled lava should go.

## Acceptance gates

- Static review: exact diff paths, JSON tag, Java compile, GameTests and unchanged other machine behavior.
- GameTests: three modes, anti-dupe output/tank preflight, mode queue, input tag, port direction, disabled/FE drop and comparator phases, persistence, 27+9 layout, thermal contact.
- Minecraft client: open/close/reopen, GUI scales, shift-click, keyboard focus, screenshots vs chosen artwork, tooltips/locales, realistic chunk transitions, actual cables and pipes.
- Protected: **no merge, tag, release, publication, permissions or new dependency** from this draft request.

**Do not mark gameplay PASS from static checks or a CI compilation alone.** The user must review the proposed balancing and visual mapping before integration.
