# Steel Catwalk Stairs attachment regression checks

## Contract

The existing tread/structural connectors and saved `facing` values are unchanged: `facing` points uphill. Railing height alignment is documented separately in [the landing junction correction](../fixes/catwalk-stair-landing-junction.md). A click on an open platform edge places the stair's high end against that edge. The flight extends downhill away from the platform. Clicking the low end adds the next high end one horizontal block and one vertical block lower, preserving the same facing.

- Bottom-mounted Steel Catwalk: first stair is adjacent and one cell below
- Top-Aligned Steel Catwalk / Steel Floor Grating: first stair is adjacent at the same cell height
- Existing stair: low-end continuation is adjacent downhill and one cell below; high-end continuation is adjacent uphill and one cell above
- On vertical platform faces, the clicked face selects the edge; top/bottom border hits use the selected border, independently of player yaw
- On top, bottom or lateral stair surfaces, the half under the cursor selects the endpoint; clicking an endpoint face selects that endpoint directly
- Center placement on an ordinary support retains normal player-relative facing

The original model's high structural connectors are centered at local Y=16 and its low connectors at local Y=0. Its walking treads are at Y=12/16 and Y=4/16. Collision now uses those tread heights so a chained flight has half-block steps, including the join between segments.

## Automated coverage

`CatwalkStairPlacementGameTests` exercises the registered item through `ServerPlayerGameMode.useItemOn` and real `Level.clip` outline hits:

1. North/east/south/west attachment, all three platform types, side/top/underside clicks and all four player look directions
2. Three consecutive descending segments, from front/top/underside and both lateral low-end surfaces, in every rotation
3. Collision tread heights and model connector/rotation contracts
4. Obstructed destination, no alternate-cell fallback, and a valid diagonal destination when the ordinary above-cell is occupied
5. Waterlogging and NBT block-state save/load before continuing the flight
6. Real `EntityPlaceEvent` cancellation, restoring destination water and survival inventory; early interaction and item-phase cancellation
7. Redirected-target `mayUseItemAt` denial, adventure/spectator restrictions, creative retention, survival consumption and offhand cost

The special item only redirects the placement context. Vanilla `BlockItem.place` still performs replacement, collision, block state, callbacks, sounds and item consumption. NeoForge's surrounding `ItemStack.useOn` transaction still captures the changed target and rolls it back on canceled placement. Additional permission/build-height/world-border checks cover the redirected destination.

Run the required project checks:

```
./gradlew compileJava processResources test runGameTestServer build --stacktrace
```

## Manual in-game acceptance

Use the built JAR whose exact commit passed CI. Test in a disposable world, in all four cardinal directions:

- From below/outside a railless platform edge, click it with Steel Catwalk Stairs: high structural endpoint joins the platform and the two treads descend toward you
- Click the low tread or low end twice more. Confirm one continuous straight three-segment flight, with matching stringers and a one-cell drop per segment
- Walk the entire flight both up and down; there must be no jump required at segment joins
- Repeat while looking steeply down and when approaching from either side
- Save/reload, then extend the same flight once more
- Repeat in survival, offhand and water; count one item per successful placement
- Block the next target cell and try again: no replacement, alternate-cell stair or item loss

## Verification in the cloud checkout

Java 21 syntax parsing and `git diff --check` passed for the new files. Full local Gradle compilation/GameTests could not start because the Gradle 9.2.1 distribution was not cached and downloading it returned `java.net.SocketException: Network is unreachable`. This document does not claim that runtime tests or manual in-game acceptance have passed; CI on the final combined commit and later user-authorized gameplay verification remain required.
