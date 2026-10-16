# Catalogue native-fluidlogging matrix

Runtime inventory: all 200 registered IEDCTR blocks on main `e41a4b8a` plus the catalogue extension at this commit. The registry GameTest checks this list against the actual mod namespace. Item-only content is outside this block matrix. No block, including full bricks and sensitive glass, is silently excluded.

`W` means existing water storage; `N` means water/lava/IE-creosote native source storage; `T` means transient water with vanilla levels/recession. Storage is independent of geometric passage. The collision column describes only the default state at the test fixture; it does not certify every orientation/variant or visible fluid surface. Full collision is a storage implementation requirement, not permission to remove collision or occlusion.

Every pending row requires source placement, real survival bucket balance, legacy/default state load, compressed chunk reload and client updates for water/lava/creosote; natural water arrival/recession with no source promotion; shape/targeting/item drops retained. Test all meaningful orientations/variants, not only the default state. Controls additionally require signal strength, tick timing, support loss and switch behavior; multipart devices require independent cells. Machines require separate exterior cells and unchanged inventory/tank/capability contracts. The accumulator remains protected by its existing dedicated contract suite.

All 200 registrations store water/lava/IE-creosote sources; 190 also expose transient water. The 485-GameTest suite covers real source bucket balances (Ariadne uses its tool), legacy defaults, save/chunk/palette codecs and all geometry-state shape parity. Machine tests additionally cover entity NBT/reload, ticking and separate tank/cell priorities. Full cubes retain collision and occlusion; natural fluid passage still obeys geometry. The earlier 40-scene/eight-representative client suite passed on `448ee647`; the current 80-scene/16-representative gate must pass on the exact commit. Per-model/per-orientation screenshot certification is not claimed.

| Block ID (mod namespace omitted) | Runtime class | States | Before | Current | Default collision | Next validation / lot |
|---|---|---:|---|---|---|---|
| `clinker_brick_block` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions; full geometry retained, storage distinct from passage |
| `clinker_brick_slab` | `PortedBlocks$VariantSlabBlock` | 1152 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `clinker_brick_stairs` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `clinker_brick_wall` | `NativeDecorativeBlocks$NativeWallBlock` | 15552 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `clinker_brick_stained_block` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions; full geometry retained, storage distinct from passage |
| `clinker_brick_stained_slab` | `PortedBlocks$VariantSlabBlock` | 1152 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `clinker_brick_stained_stairs` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `clinker_brick_sastor_corner_block` | `PortedBlocks$DirectionalPortBlock` | 576 | none | N + T | full | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions; full geometry retained, storage distinct from passage |
| `clinker_brick_recessed` | `PortedBlocks$HorizontalFacingBlock` | 384 | none | N + T | full | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions; full geometry retained, storage distinct from passage |
| `clinker_brick_vertically_slit` | `PortedBlocks$HorizontalFacingBlock` | 384 | none | N + T | full | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions; full geometry retained, storage distinct from passage |
| `clinker_brick_vertical_slab_structured` | `PortedBlocks$VerticalSlabBlock` | 384 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `halfslab_clinker_brick` | `PortedBlocks$PartsBlock` | 1440 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `slag_brick_block` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions; full geometry retained, storage distinct from passage |
| `slag_brick_slab` | `PortedBlocks$VariantSlabBlock` | 1152 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `slag_brick_stairs` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `slag_brick_wall` | `NativeDecorativeBlocks$NativeWallBlock` | 15552 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `rebar_concrete` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions; full geometry retained, storage distinct from passage |
| `rebar_concrete_slab` | `PortedBlocks$VariantSlabBlock` | 1152 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `rebar_concrete_stairs` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `rebar_concrete_wall` | `NativeDecorativeBlocks$NativeWallBlock` | 15552 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `halfslab_rebar_concrete` | `PortedBlocks$PartsBlock` | 1440 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `rebar_concrete_tile` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions; full geometry retained, storage distinct from passage |
| `rebar_concrete_tile_slab` | `PortedBlocks$VariantSlabBlock` | 1152 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `rebar_concrete_tile_stairs` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `panzerglass_block` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions; full geometry retained, storage distinct from passage |
| `panzerglass_slab` | `PortedBlocks$VariantSlabBlock` | 1152 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `dark_shingle_roof` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `dark_shingle_roof_metallized` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `dark_shingle_roof_skylight` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `dark_shingle_roof_chimneytrunk` | `ChimneyTrunkBlock` | 3840 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `dark_shingle_roof_wireconduit` | `RoofWireConduitBlock` | 3840 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `dark_shingle_roof_chimney` | `ChimneyBlock` | 49152 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `dark_shingle_roof_block` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions; full geometry retained, storage distinct from passage |
| `dark_shingle_roof_slab` | `PortedBlocks$VariantSlabBlock` | 1152 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `dark_shingle_roof_slabslice` | `PortedBlocks$PartsBlock` | 1440 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `dense_grit_dirt_block` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions; full geometry retained, storage distinct from passage |
| `dense_grit_sand_block` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions; full geometry retained, storage distinct from passage |
| `ariadne_marker` | `PortedBlocks$AriadneMarkerBlock` | 4608 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `old_industrial_wood_planks` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions; full geometry retained, storage distinct from passage |
| `old_industrial_wood_slab` | `PortedBlocks$VariantSlabBlock` | 1152 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `old_industrial_wood_stairs` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `old_industrial_wood_slabslice` | `PortedBlocks$PartsBlock` | 1440 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `old_industrial_wood_door` | `NativeDecorativeBlocks$NativeDoorBlock` | 6144 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `thick_steel_pole` | `PortedBlocks$CenteredPoleBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `thick_steel_pole_head` | `PoleHeadBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `thin_steel_pole` | `PortedBlocks$CenteredPoleBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `thin_steel_pole_head` | `PoleHeadBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `treated_wood_pole` | `PortedBlocks$CenteredPoleBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `treated_wood_pole_head` | `PoleHeadBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `treated_wood_pole_support` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `steel_double_t_support` | `PortedBlocks$SupportBlock` | 2304 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `steel_floor_grating` | `PortedBlocks$FloorGratingBlock` | 96 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `steel_catwalk` | `PortedBlocks$CatwalkBlock` | 7680 | N | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `steel_catwalk_stairs` | `PortedBlocks$CatwalkStairsBlock` | 30720 | N + T | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `steel_catwalk_ta` | `RaisedCatwalkBlock` | 1152 | N | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `steel_railing` | `PortedBlocks$RailingBlock` | 7680 | N | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `steel_mesh_fence` | `SteelMeshFenceBlock` | 15552 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `steel_mesh_fence_gate` | `PortedBlocks$FenceGateSegmentBlock` | 1536 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `metal_rung_ladder` | `NativeDecorativeBlocks$NativeLadderBlock` | 384 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `metal_rung_steps` | `NativeDecorativeBlocks$NativeLadderBlock` | 384 | W | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `steel_table` | `PortedBlocks$TableBlock` | 96 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `treated_wood_table` | `PortedBlocks$TableBlock` | 96 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `treated_wood_stool` | `TreatedWoodStoolBlock` | 96 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `metal_crafting_table` | `MachineBlocks$MetalCraftingTableBlock` | 96 | none | N + T | full | Sources/save/palette + entity contents/reload/ticks + separate tank/cell; full geometry retained, storage distinct from passage |
| `labeled_crate` | `MachineBlocks$LabeledCrateBlock` | 384 | none | N + T | full | Sources/save/palette + entity contents/reload/ticks + separate tank/cell; full geometry retained, storage distinct from passage |
| `factory_hopper` | `MachineBlocks$DirectionalMachineBlock` | 576 | none | N + T | full | Sources/save/palette + entity contents/reload/ticks + separate tank/cell; full geometry retained, storage distinct from passage |
| `factory_dropper` | `MachineBlocks$OpenDirectionalMachineBlock` | 1152 | none | N + T | full | Sources/save/palette + entity contents/reload/ticks + separate tank/cell; full geometry retained, storage distinct from passage |
| `factory_placer` | `MachineBlocks$DirectionalMachineBlock` | 576 | none | N + T | full | Sources/save/palette + entity contents/reload/ticks + separate tank/cell; full geometry retained, storage distinct from passage |
| `small_block_breaker` | `MachineBlocks$ActiveHorizontalMachineBlock` | 768 | none | N + T | partial/empty | Sources/save/palette + entity contents/reload/ticks + separate tank/cell |
| `small_waste_incinerator` | `MachineBlocks$LitMachineBlock` | 192 | none | N + T | full | Sources/save/palette + entity contents/reload/ticks + separate tank/cell; full geometry retained, storage distinct from passage |
| `small_lab_furnace` | `MachineBlocks$LitHorizontalMachineBlock` | 768 | none | N + T | full | Sources/save/palette + entity contents/reload/ticks + separate tank/cell; full geometry retained, storage distinct from passage |
| `small_electrical_furnace` | `MachineBlocks$LitHorizontalMachineBlock` | 768 | none | N + T | full | Sources/save/palette + entity contents/reload/ticks + separate tank/cell; full geometry retained, storage distinct from passage |
| `small_mineral_smelter` | `MachineBlocks$Phase3HorizontalMachineBlock` | 1536 | none | N + T | full | Sources/save/palette + entity contents/reload/ticks + separate tank/cell; full geometry retained, storage distinct from passage |
| `small_freezer` | `MachineBlocks$Phase4HorizontalMachineBlock` | 1920 | none | N + T | full | Sources/save/palette + entity contents/reload/ticks + separate tank/cell; full geometry retained, storage distinct from passage |
| `fluid_barrel` | `MachineBlocks$LevelDirectionalMachineBlock` | 2880 | none | N + T | full | Sources/save/palette + entity contents/reload/ticks + separate tank/cell; full geometry retained, storage distinct from passage |
| `small_fluid_funnel` | `MachineBlocks$LevelOnlyMachineBlock` | 384 | none | N + T | full | Sources/save/palette + entity contents/reload/ticks + separate tank/cell; full geometry retained, storage distinct from passage |
| `passive_fluid_accumulator` | `MachineBlocks$PassiveFluidAccumulatorBlock` | 96 | W | N + T | partial/empty | Sources/save/palette + entity contents/reload/ticks + separate tank/cell |
| `small_solar_panel` | `MachineBlocks$SolarPanelBlock` | 480 | none | N + T | full | Sources/save/palette + entity contents/reload/ticks + separate tank/cell; full geometry retained, storage distinct from passage |
| `small_milking_machine` | `MachineBlocks$MilkingMachineBlock` | 1536 | none | N + T | partial/empty | Sources/save/palette + entity contents/reload/ticks + separate tank/cell |
| `small_tree_cutter` | `MachineBlocks$ActiveHorizontalMachineBlock` | 768 | none | N + T | partial/empty | Sources/save/palette + entity contents/reload/ticks + separate tank/cell |
| `straight_pipe_valve` | `MachineBlocks$PipeValveBlock` | 36864 | none | N + T | partial/empty | Valve sources/geometry + redstone regressions |
| `straight_pipe_valve_redstone` | `MachineBlocks$PipeValveBlock` | 36864 | none | N + T | partial/empty | Valve sources/geometry + redstone regressions |
| `straight_pipe_valve_redstone_analog` | `MachineBlocks$PipeValveBlock` | 36864 | none | N + T | partial/empty | Valve sources/geometry + redstone regressions |
| `steel_framed_window` | `PortedBlocks$SteelFramedWindowBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `metal_sliding_door` | `PortedBlocks$SlidingDoorBlock` | 18432 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `iron_hatch` | `PortedBlocks$HatchBlock` | 1536 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `iron_bulb_light` | `PortedBlocks$OppositeFaceLightBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + native client light representative |
| `iron_ceiling_edge_light` | `PortedBlocks$HorizontalLookLightBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + native client light representative |
| `iron_floor_edge_light` | `PortedBlocks$HorizontalLookLightBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + native client light representative |
| `iron_inset_light` | `PortedBlocks$OppositeFaceLightBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + native client light representative |
| `sign_caution` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `sign_danger` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `sign_decor` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `sign_defense` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `sign_exit` | `PortedBlocks$ExitSignBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `sign_firehazard` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `sign_frost` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `sign_hotsurface` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `sign_hotwire` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `sign_laser` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `sign_magichazard` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `sign_magneticfield` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `sign_radioactive` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Sources/save/palette + every geometry-state shape parity; paid mechanics regressions |
| `arrow_target` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `door_sensor_switch` | `ControlsBlockTypes$DoorSensorSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `elevator_button` | `ControlsBlockTypes$ElevatorButtonBlock` | 3456 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `glass_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `glass_contact_mat` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `glass_day_timer` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `glass_door_contact_mat` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `glass_entity_detector` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `glass_interval_timer` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `glass_linear_entity_detector` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `glass_rotary_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `glass_small_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `glass_touch_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `glass_touch_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `glass_vertical_bar_gauge` | `ControlsBlockTypes$GaugeBlock` | 9216 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_alarm_lamp` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_alarm_siren` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_analog_angular_gauge` | `ControlsBlockTypes$GaugeBlock` | 9216 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_analog_horizontal_gauge` | `ControlsBlockTypes$GaugeBlock` | 9216 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_blinking_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_block_detector` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_comparator_switch` | `ControlsBlockTypes$ComparatorSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_contact_mat` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_day_timer` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_dimmer` | `ControlsBlockTypes$DimmerBlock` | 18432 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_door_contact_mat` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_double_pole_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_entity_detector` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_estop_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_fallthrough_detector` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_fenced_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_foot_button` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_green_blinking_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_green_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_high_sensitive_trapdoor` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_hopper_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_interval_timer` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_knock_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_knock_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_lever` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_light_sensor` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_lightning_sensor` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_linear_entity_detector` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_machine_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_pull_handle` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_rain_sensor` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_red_blinking_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_red_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_rotary_lever` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_rotary_machine_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_shock_sensitive_contact_mat` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_shock_sensitive_trapdoor` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_small_digital_gauge` | `ControlsBlockTypes$GaugeBlock` | 9216 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_small_lever` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_switchlink_cased_pulse_receiver` | `ControlsBlockTypes$CasedSwitchLinkPulseReceiverBlock` | 1152 | none | N + T | full | Sources/save/palette + inherited wet control regression; full geometry retained, storage distinct from passage |
| `industrial_switchlink_cased_receiver` | `ControlsBlockTypes$CasedSwitchLinkReceiverBlock` | 1152 | none | N + T | full | Sources/save/palette + inherited wet control regression; full geometry retained, storage distinct from passage |
| `industrial_switchlink_pulse_receiver` | `ControlsBlockTypes$SwitchLinkPulseReceiverBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_switchlink_pulse_relay` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_switchlink_receiver` | `ControlsBlockTypes$SwitchLinkReceiverBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_switchlink_receiver_analog` | `ControlsBlockTypes$SwitchLinkReceiverBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_switchlink_relay` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_switchlink_relay_analog` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_tube_gauge` | `ControlsBlockTypes$GaugeBlock` | 9216 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_vertical_bar_gauge` | `ControlsBlockTypes$GaugeBlock` | 9216 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_white_blinking_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_white_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_yellow_blinking_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrial_yellow_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `industrialswitch` | `IndustrialSwitchBlock` | 3456 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `light_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `oldfancy_bistableswitch1` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `oldfancy_bistableswitch2` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `oldfancy_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `oldfancy_small_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `oldfancy_spring_reset_chain` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `orange_sensitiveglass` | `ControlsBlockTypes$SensitiveGlassBlock` | 192 | none | N + T | full | Sources/save/palette + inherited wet control regression; full geometry retained, storage distinct from passage |
| `red_power_plant` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `rustic_angular_lever` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `rustic_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `rustic_contact_plate` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `rustic_door_contact_plate` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `rustic_fallthrough_detector` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `rustic_high_sensitive_trapdoor` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `rustic_lever` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `rustic_nail_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `rustic_nail_lever` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `rustic_semaphore` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `rustic_shock_sensitive_plate` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `rustic_shock_sensitive_trapdoor` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `rustic_small_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `rustic_spring_reset_chain` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `rustic_two_hinge_lever` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `sensitive_glass_block` | `ControlsBlockTypes$SensitiveGlassBlock` | 192 | none | N + T | full | Sources/save/palette + inherited wet control regression; full geometry retained, storage distinct from passage |
| `stained_sensitiveglass` | `ControlsBlockTypes$SensitiveGlassBlock` | 192 | none | N + T | full | Sources/save/palette + inherited wet control regression; full geometry retained, storage distinct from passage |
| `valve_wheel_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
| `yellow_power_plant` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Sources/save/palette + inherited wet control regression |
