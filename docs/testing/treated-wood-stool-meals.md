# Treated Wood Stool seating and meal saturation

## Contract

- The existing `treated_wood_stool` block, item, recipe, model, ordinary collision and save-state schema remain in place
- Normal right-click seats one player; sneaking bypasses seating and native sneak-dismount stands up
- A temporary invisible, unpickable, noncollidable `stool_seat` entity carries the player. It cannot be summoned or saved with chunks/player root vehicles
- Native block interaction permissions, build permission, interaction reach, clear headroom and NeoForge mount-event vetoes remain effective
- Block replacement/breakage and logout eject the passenger before removing the mount, preserving native safe dismount. Ordinary dismount leaves an empty seat that is removed on the next server tick; invalid, dead-player and defensively loaded seats are also cleaned up
- Only actual native held-item completion while still on a valid stool awards the meal bonus, on the logical server
- The immediate addition is `food.saturation() * 0.20`, capped to `FoodData.getFoodLevel()`. In Minecraft 1.21.1, `FoodProperties.saturation()` is already the normal gain `nutrition * saturationModifier * 2`
- The handler does not call `eat`, heal, add status effects, adjust exhaustion, modify hunger, shrink food or replay food effects/containers
- A native Start-to-Finish token prevents replaying the same completion. Separate meals can both succeed during the same world tick

There is no passive Reposed/rest mechanic, thirty-second delay, two-minute effect, periodic Saturation effect, or hunger/health benefit from sitting alone.

## Automated coverage

`StoolMealGameTests` exercises the registered block's right-click interaction and actual `Player.startRiding`/`stopRiding`. Food tests call the actual held stack's `use`, then tick the mock player's native use countdown until completion. They do not create synthetic successful Finish events or directly call `finishUsingItem`, which would bypass NeoForge's completion wrapper.

The suite covers:

1. One occupant, repeated clicks, unchanged public collision, no extra entity hitbox and clear native dismount
2. Sitting beyond thirty seconds without hunger, saturation, health, exhaustion or status-effect changes
3. Bread, cooked beef and the mod's müsli bar, both hands, exact native hunger, exact +20% saturation and one consumed item
4. A deliberately repeated handler invocation inside the native Finish event, two genuine successive meals in the same world tick and no later repeat
5. Saturation cap and native creative item retention
6. Last-item stew bowl, golden apple's two native effects, exact bonus and no duplicated food or container
7. Standing and a real native minecart as negative controls
8. Leaving the stool before completion, or sitting down during consumption
9. Cancelled Start, interrupted use, nonfood milk completion and recovery on the next real meal
10. Adventure/spectator restrictions, reach, headroom and cancelled native EntityMountEvent
11. Block-break cleanup, logout-event cleanup, chunk/root-vehicle save rejection and defensive NBT reload cleanup

Run the repository's normal `test`, resource validators and `runGameTestServer` against the exact candidate commit. Local Gradle execution in the current cloud workspace is blocked by the previously verified wrapper download restriction; CI is the authoritative compile/runtime result. Syntax parsing and resource checks alone are not a runtime pass.

## Client/dedicated-server verification checklist

These visual/network checks still require a compatible playable Minecraft instance; automated server tests do not establish them:

- In first and third person, right-click the stool with an empty hand and with food held; confirm seated pose aligns with the existing 10/16-block-high seat
- While looking at the stool, hold food use again; confirm normal eating animation and one item consumed, then sneak-dismount without clipping the stool, ceiling or nearby blocks
- Observe from another connected client: no visible seat, nameplate, extra selectable hitbox or extra occupants; only the original stool is targeted by mining
- With hunger/saturation controlled, verify bread adds native 5 hunger and 6 + 1.2 saturation when uncapped, with no extra health/status-effect change
- Check eating while standing, riding another entity, releasing use early and standing up just before finishing
- Check two players attempting the same stool; break it while occupied; leave/rejoin the server; unload/reload the chunk and restart the server. No orphan mount or restored passenger should survive
- Confirm protected/claimed block interaction denial prevents seating through the server's ordinary interaction pipeline
- Confirm English and French tooltips/manual pages describe the immediate meal bonus and sneak controls

## API evidence

The exact NeoForge 21.1.230 source/userdev patches place `LivingEntityUseItemEvent.Finish` after `useItem.finishUsingItem(level, this)` and pass a copy of the pre-use stack. Food properties are read through `ItemStack.getFoodProperties(player)`, including NeoForge's stack/entity-sensitive override.

- https://github.com/neoforged/NeoForge/blob/1.21.1/patches/net/minecraft/world/entity/LivingEntity.java.patch
- https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/event/entity/living/LivingEntityUseItemEvent.java
- https://docs.neoforged.net/docs/1.21.1/items/#food

Compatibility boundary: a third-party item whose custom completion ignores its declared food properties is not an ordinary food consumption contract. Food properties are resolved from the completion event's pre-use stack after native consumption; unusual properties that vary based on newly applied player effects need separate integration testing.
