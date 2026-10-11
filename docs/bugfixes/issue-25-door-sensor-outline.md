# Issue #25: Door Sensor outline alignment

## Cause and scope

The Door Sensor was registered as a generic player sensor and inherited the
centered, 8-by-8-by-4 switch outline. Its shipped model instead occupies a
6-by-1-by-1.5 strip along the bottom edge. For wall facings, the inherited shape
was also at the opposite edge from that model.

Register a dedicated `DoorSensorSwitchBlock` extending `SensorSwitchBlock` with
`SensorKind.PLAYER`. Override only the selection outline with a cached bounding
box for each model rotation. Both powered states share the same geometry.
`NORTH`, `UP` and `DOWN` use the unrotated model's bounds, exactly as the existing
blockstate assets specify; `EAST`, `SOUTH` and `WEST` follow their Y rotations.

Models and placement/support semantics are unchanged, including the existing
unrotated appearance on upward/downward support faces. This fix aligns the
hitbox with that design; it does not redesign mounting orientation. Sensor
polling, player detection, manual refresh, signal strength and empty collision
remain inherited. Other player sensors keep their existing generic outline.
No state migration is needed for already placed Door Sensors.

## Regression coverage

Two required `DoorSensorShapeGameTests` are committed before the implementation:

- Compare outlines for all six facings and both powered states with bounds
  derived independently from the shipped blockstate variants and model elements,
  resolving the powered model's parent and applying each actual Y rotation
- Exercise real `BlockItem.place` on each of the six support faces and compare
  the resulting outline to the shipped model, retaining clicked-face placement
- Check empty collision and 0/15 redstone output in every state

The resource oracle rejects unsupported nonzero element/X rotations rather
than silently ignoring them. Existing control tests retain scheduled nearby
player detection coverage. Outlines use the model's enclosing bounds, not
per-pixel geometry or its tiny decorative cutouts.

## Qualification limits

Dedicated GameTests cover server-side placement and model-resource bounds, not
client visual inspection, the reporter's full modpack or a packaged dedicated
server. The build workflow is unchanged and does not upload runtime artifacts.
No license, release metadata, publication or merge is included.
