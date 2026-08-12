# Collision root cause

## Iron Hatch

`HatchBlock.getShape` used a closed `Block.box(0,14,0,16,16,16)` while the closed model envelope is `Y=0..3`. The open shape used a two-pixel plate while the moving leaf reaches 2.875 pixels. `getCollisionShape` returned the same geometry, so the coordinate mismatch affected movement as well as selection.

Correction:

- closed outline/collision: `[0,0,0] -> [16,3,16]`;
- open outline/collision: rotated upright 2.875-pixel leaf;
- the open center remains passable, but the visible leaf remains solid;
- `POWERED` continues to control state transitions only and does not alter geometry.

The earlier 1.1.53 candidate suggesting an entirely empty open-hatch collision was rejected because the execution mandate requires visible solid parts to remain collisionable and forbids fixing an opening by deleting all collision.

## Metal Sliding Door

The prior implementation reused one lower closed shape for both halves, rotated closed geometry by axis only, reused one open aggregate across both halves, and inherited `getCollisionShape` from `DoorBlock`. As a result:

- only 4 of 32 `FACING x HINGE x HALF x OPEN` visual states exactly matched model occupancy;
- upper halves contained lower floor-track geometry;
- four hinge/facing combinations placed rails on the wrong side;
- open decorative tracks intersected a standing player AABB.

Correction:

- lower/upper and open/closed model shapes are encoded from the four JSON models;
- all four rotations are cached once and selected by the blockstate's exact model rotation;
- closed collision matches the solid model;
- open collision retains retracted solid components but excludes decorative lower/upper tracks;
- outline/selection retains model-exact track geometry.

No changes were made to sounds, energy, manual content, drops, mining tags, placement/support behavior, or pathfinding overrides.
