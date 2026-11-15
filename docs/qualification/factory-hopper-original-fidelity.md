# Factory Hopper fidelity work (not qualified yet)

Reference: Stefan Wilhelm, Engineers Decor 1.3.29, MIT,
[`EdHopper.java`, a561e61e35ce0367088356967e1ea4cad5c1f27d](https://github.com/stfwi/engineers-decor/blob/a561e61e35ce0367088356967e1ea4cad5c1f27d/src/main/java/wile/engineersdecor/blocks/EdHopper.java).

No models, textures, blockstates or GUI artwork are changed. The horizontal model remains the original elbow; inventory intake is above, except output UP takes from below. Placement retains opposite clicked face and UP-to-DOWN clamp. Existing saved inventories/settings are retained.

## Runtime contract

- Base work interval 10 ticks; neighbor update wakes after 1 tick; inventory slot changes shorten the remaining work interval to 8.
- Inventory input runs when the effective signal allows it OR pulse mode is selected, independently of output delay. Output requires its trigger and delay expiry; configured delay is 10 + period*2 ticks.
- Manual control is the original simulated powered edge, not a universal inversion/cooldown bypass.
- One selected source stack per output cycle. Matching destination piles are completed first. A fully accepted batch retains the cursor; an unaccepted remainder advances it. Opposing vanilla/Factory Hoppers reject backflow.
- Input block capability, legacy container fallback, then first inventory entity (including minecart); output block entity without capability is authoritative and does not fall through. Missing output handler sets delay12.
- Loose items: alive and on ground; collection every50 work ticks without input handler, pickup delay honored outside direct radius squared <0.7; at most3 distant entities per sweep, direct ones do not consume this limit. Fall-on wakes collection. Range geometry matches original vertical mouth.
- Work/collection/delay/edge/manual transient state is reset on reload. Cursor, settings, items and sampled powered state survive world save.
- Normal destruction returns one hopper, carrying items/settings/cursor only when nonempty; empty configured hoppers lose settings as upstream. Explosion emits contents without a hopper. Portable payload is Hopper-only custom data; no arbitrary block entity id is imported.

## Deliberate defensive adaptations

| Original risk | Retained modern protection |
|---|---|
| Container extraction may exceed count | Bound each extraction by configured budget |
| Simulated stack inserted before real extraction | Insert only actual executed extraction; preserve unexpected remainder |
| Old Forge capabilities | NeoForge block/entity capabilities plus sided and canTakeItem checks |
| Unconditional neighboring block access | Check loaded chunks, never force-load |
| Unlimited malformed NBT values | Clamp settings/cursor; preserve normal saved contents |
| Stateful loot evaluation | Pure drop preview, separate player-break lifecycle state |

Local source changes and GameTests are not native evidence. Full compilation, GameTests, drops lifecycle, minecart paths and packaged runtime qualification remain required. Previous e35 JAR proofs do NOT qualify this new binary; do not update pinned hashes without a new exact build and evidence.

## Explicit owner upgrade (separate from original fidelity)

Owner approved Factory Hopper ONLY blast resistance 12→15, hardness remains2. The shared machineMetal default and all other machines remain12. Reference IE1.21.1 commit75a27f03e4243544243567e8d5c38d336f4f10f4, IEBlocks.java metal properties/steel structures; this is structural steel resistance15, not steel storage-block resistance10. Existing recipe is already five c:plates/steel plus one vanilla hopper, unchanged. This is resistance, not immunity: real TNT tests still exercise destruction and content release in a contained bedrock cavity, with drop-decay variants and doTileDrops=false.

CI first pass de4a799d compiled Java successfully. Five legacy GameTest expectations used the old ten-tick phase/input coupling or persisted cooldown. Updated tests now inspect the exact first work tick, the inventory wakeups at1/9/17, input continuing while output is backed off, and upstream transient cooldown reset. No production timing relaxed to satisfy those assertions. Manual EN/FR category copies are synchronized with individual entries.
