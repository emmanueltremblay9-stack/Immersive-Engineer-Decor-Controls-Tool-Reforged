# Collision shape matrix

| Registry ID | Block class | State properties | Expected passable region | Prior collision | Corrected collision | Orientations | Severity |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `immersive_engineer_decor_controls_tool_reforged:iron_hatch` | `PortedBlocks.HatchBlock` | `HORIZONTAL_FACING`, `OPEN`, `POWERED` | Center path when open | Closed plate at `Y=14..16`; open leaf only 2 px deep | Closed plate `Y=0..3`; open 2.875 px upright leaf, center clear | north/east/south/west | Critical closed mismatch; high open mismatch |
| `immersive_engineer_decor_controls_tool_reforged:metal_sliding_door` | `PortedBlocks.SlidingDoorBlock` | `FACING`, `HINGE`, `HALF`, `OPEN`, `POWERED`, `PAIR_SIDE` | 0.6 x 1.8 player sweep through open doorway | Open tracks collided; `HALF` and some `HINGE` rotations ignored | Model-exact outline; open collision contains only solid retracted components | all facing/hinge/half combinations | High |

## Neighbor-family audit

| Registry ID | Intended full cube | Actual full cube | State-dependent | Rotated | Runtime checked | Test added | Result |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `iron_hatch` | no | no | yes | yes | automated all-state GameTest | yes | two fresh 182-test runs PASS |
| `metal_sliding_door` | no | no | yes | yes | automated real-player AABB sweep | yes | two fresh 182-test runs PASS |
| `steel_mesh_fence_gate` | no | no | yes | yes | no new live check | existing all-state collision tests | PASS in 182-test run |
| `old_industrial_wood_door` | no | no | vanilla `DoorBlock` | vanilla | targeted prior runtime | existing open/redstone tests | PASS in 182-test run |

`HatchBlock` and `SlidingDoorBlock` each have exactly one public registry user. `SimpleDoorLikeBlock` has no registration. Legitimate full-cube blocks were not altered. Client walking was not used as a gate because this closure prohibited visual interaction; the collision substitute is the real-world `Level.noCollision` player sweep plus solid-component probes.
