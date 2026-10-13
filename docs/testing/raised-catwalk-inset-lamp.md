# Raised catwalk: independently selectable inset lamp

## Change and invariants

The recovered `steel_catwalk_ta4_model.json` puts the lamp glass and eight mounting tabs at Y=14.625–15, inside the old solid Y=14–16 deck outline. The lit variant now exposes the model's recess: its upper grating envelope starts at Y=15, its outer rim still reaches Y=14, and its lamp outline uses the existing nine lamp cuboids. Model resources are unchanged. Physical collision remains the original continuous Y=14–16 deck, preserving walking and headroom behavior.

The shared highlight and mining selector first checks the real world eye ray. It selects the nearest base or lamp surface, with the base winning exact ties. An explicit `INSET_LIGHT` component distinguishes a lamp from the base even though neither has a horizontal railing side. Stair and ordinary catwalk railing components retain their existing constructors and targeting.

Lamp removal runs only inside the completed native `onDestroyedByPlayer` path after the shared mining target, native permission checks and `BreakEvent`. It clears `paid_light`, reevaluates the current pole support, preserves water, and prevents whole-block loot. One paid lamp is harvested through the standard standalone lamp loot/drop hook only in non-creative authorized mining with a harvesting tool. Creative, legacy/unpaid, wrong-tool and disabled/canceled-drop paths grant no lamp item. The remaining state has zero light emission. A fresh start is required to mine the remaining deck; a stale lamp completion cannot become a base break.

## Automated regression coverage

`RaisedCatwalkLampGameTests` covers:

- Actual eye rays below and diagonally below all four sides, with highlight geometry independently checked against the nine existing model cuboids
- Top-deck, underside-away-from-lamp and outer-rim hits remaining base hits, plus unchanged walking collision
- Native survival START and completed break from every approach, exactly one tool use, one paid lamp and no catwalk drop
- Full native timed START/early STOP/delayed completion, and creative immediate START
- Legacy NBT without `paid_light`, paid NBT, and a harvesting pickaxe on its final durability
- Wrong tools, `doTileDrops=false`, and native `BlockDropsEvent` cancellation
- Early and same-priority late BreakEvent cancellation, START cancellation and useItem denial
- Exact native adventure `CAN_BREAK` data-component authorization and spectator denial
- Lamp-to-deck and deck-to-lamp retargeting, ABORT, replacement blocks, changed paid state, held item/component changes, the native mending damage-only exception, and creative-mode swaps
- World occlusion shared by highlight and mining
- Water, air/thick-steel/thin-steel/wood supports, railings above, neighbor changes and NBT/codec reload without restored credits
- Repeated genuine item installation and native lamp removal, followed by ordinary base mining without duplicate refunds

The fake-player ray fixture sets both entity yaw and head yaw, since server-side `LivingEntity.getViewYRot(1)` uses the latter. It independently asserts the normalized actual eye vector aligns with the intended target before testing any shape or mining outcome. Normalization applies only to this angular assertion: Minecraft's float trigonometry lookup can yield a view-vector magnitude slightly below one, and the real world/mining ray remains unchanged. This makes the north/east/south/west cases genuine distinct view directions.

Existing `RaisedCatwalkGameTests` now starts its whole-deck paid-lamp mining through a real top-deck eye ray/native START. It still verifies one base item plus one paid lamp. Its lit outline expectation follows the model recess while its collision expectation remains unchanged.

## Manual client acceptance

1. Install one inset light under a plain raised catwalk in survival. Look up directly and diagonally from north/east/south/west. Only the lamp and its mounting tabs should outline when targeted; the deck should outline when targeted elsewhere or from above.
2. Mine the lamp with a suitable pickaxe. Ordinary mining progress must complete before it disappears. Confirm one lamp drop, one tool durability use, the deck remaining in place and light emission returning to zero.
3. Stop mining and start again on the remaining deck. It should mine normally and produce one catwalk without another lamp.
4. While holding mine, move the crosshair between the lamp and deck before completion. Progress must restart for the newly selected component, and the old target must not destroy the other component.
5. Repeat with waterlogging, a pole added under an installed lamp, and railings above the deck. Check that these survive lamp removal and that the appropriate pole head is restored.
6. Repeat in creative and with a legacy/unpaid light: removal must produce no lamp item. Save/reload before and after removal; the lamp and its credit must not reappear.

Local Java 21 parse-only and whitespace checks are available in this environment. Compilation, runtime GameTests, and client visual verification require the grouped CI/client validation; they are not claimed by parse-only checks.

## Native packet pose and held-input regression

A client turn and attack can occur before the ordinary end-of-tick movement packet. Mining actions carry a block and face, not a component or look vector. The client now flushes an ordinary rotation packet before component START/HOLD; the server uses its accepted current yaw/pitch instead of the delayed rendering head yaw. Reach, occlusion, protection and component selection remain server-authoritative.

`PacketLampMiningGameTests` passes rotation and mining actions through the real server packet listener, with only outbound transport stubbed. It deliberately keeps the previous rendering head yaw and verifies that this old ray hits the deck while the newly accepted aim hits the lamp. Creative immediate removal and survival timed completion cover the underside/four approaches, support and railing preservation, water, saved state, light removal, paid refunds and tool wear. This closes a gap in the older synchronized-head FakePlayer fixture.

After successful client lamp-only prediction, a small input latch prevents the same held attack from starting to mine the retained deck. Suppression occurs before native game-mode input and outgoing mining actions; canceling LeftClickBlock alone does not prevent creative START packets. Releasing the bound attack mouse/key, retargeting another block or air, a restored lit state after denial, or leaving/changing the world clears the latch. A subsequent deliberate click can mine the deck normally. Pure helper tests cover these transitions; they do not by themselves establish real client prediction or server acknowledgement.

## Isolated native-client gameplay qualification

`tools/validation/client_gameplay.py` starts the normal development client under Xvfb in a fresh script-owned game directory. A nonce-gated, default-inactive client fixture creates its own flat integrated world; it refuses existing saves or a remote connection. The ten-case matrix uses the real LocalPlayer, MultiPlayerGameMode START/HOLD, NeoForge input gate, ClientLevel prediction, native packets and processed acknowledgements. It checks immediate lamp-only prediction, authoritative state and loot, sustained held-input suppression without extra mining packets, then release and normal deck removal, in creative/survival from below and four sides. An ordinary catch platform retains physical item drops for counting. No user world or options are imported.

The runner requires the complete matching per-case evidence, a clean exit and a current-run completion marker. Timeout, title-screen loading, crashes, stale/missing ACKs, incomplete cases or unexpected packet/loot counts fail. Its Python unit tests validate this evidence gate; only successful execution of the gameplay step establishes the client result. The fixture drives native game-mode/input methods rather than physical OS mouse events, and makes no screenshot/rendering assertions. The stair movement GameTests are likewise not a claim that LocalPlayer's auto-jump input routine has been exercised.
