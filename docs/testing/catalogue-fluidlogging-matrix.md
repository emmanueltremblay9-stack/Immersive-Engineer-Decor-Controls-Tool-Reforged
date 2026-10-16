# Catalogue native-fluidlogging matrix

Runtime inventory: all 200 registered IEDCTR blocks on main `e41a4b8a` plus the catalogue extension at this commit. The registry GameTest checks this list against the actual mod namespace. Item-only content is outside this block matrix. No block, including full bricks and sensitive glass, is silently excluded.

`W` means existing water storage; `N` means water/lava/IE-creosote native source storage; `T` means transient water with vanilla levels/recession. Storage is independent of geometric passage. The collision column describes only the default state at the test fixture; it does not certify every orientation/variant or visible fluid surface. Full collision is a storage implementation requirement, not permission to remove collision or occlusion.

Every pending row requires source placement, real survival bucket balance, legacy/default state load, compressed chunk reload and client updates for water/lava/creosote; natural water arrival/recession with no source promotion; shape/targeting/item drops retained. Test all meaningful orientations/variants, not only the default state. Controls additionally require signal strength, tick timing, support loss and switch behavior; multipart devices require independent cells. Machines require separate exterior cells and unchanged inventory/tank/capability contracts. The accumulator remains protected by its existing dedicated contract suite.

Lighting and controls lot: native source contracts cover all opted-in blocks, including all 97 controls. Raised inserted-light install/removal tests cover water/lava/creosote and transient water. Dedicated client screenshots exercise four lights, lever/button/contact and inserted light before and after drain/recession; the 40-scene client gate passed on `448ee647` and its 80 captures were inspected. All 180 decorative registrations now have source storage; full cubes retain occlusion and partial geometry gains transient water. Machines remain the next explicit lot. Per-model/orientation visual certification remains bounded to the representatives.

| Block ID (mod namespace omitted) | Runtime class | States | Before | Current | Default collision | Next validation / lot |
|---|---|---:|---|---|---|---|
| `clinker_brick_block` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Retain regression + wet inserted-light test; full-block storage/passage proof |
| `clinker_brick_slab` | `PortedBlocks$VariantSlabBlock` | 1152 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `clinker_brick_stairs` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `clinker_brick_wall` | `NativeDecorativeBlocks$NativeWallBlock` | 15552 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `clinker_brick_stained_block` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Retain regression + wet inserted-light test; full-block storage/passage proof |
| `clinker_brick_stained_slab` | `PortedBlocks$VariantSlabBlock` | 1152 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `clinker_brick_stained_stairs` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `clinker_brick_sastor_corner_block` | `PortedBlocks$DirectionalPortBlock` | 576 | none | N + T | full | Retain regression + wet inserted-light test; full-block storage/passage proof |
| `clinker_brick_recessed` | `PortedBlocks$HorizontalFacingBlock` | 384 | none | N + T | full | Retain regression + wet inserted-light test; full-block storage/passage proof |
| `clinker_brick_vertically_slit` | `PortedBlocks$HorizontalFacingBlock` | 384 | none | N + T | full | Retain regression + wet inserted-light test; full-block storage/passage proof |
| `clinker_brick_vertical_slab_structured` | `PortedBlocks$VerticalSlabBlock` | 384 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `halfslab_clinker_brick` | `PortedBlocks$PartsBlock` | 1440 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `slag_brick_block` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Retain regression + wet inserted-light test; full-block storage/passage proof |
| `slag_brick_slab` | `PortedBlocks$VariantSlabBlock` | 1152 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `slag_brick_stairs` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `slag_brick_wall` | `NativeDecorativeBlocks$NativeWallBlock` | 15552 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `rebar_concrete` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Retain regression + wet inserted-light test; full-block storage/passage proof |
| `rebar_concrete_slab` | `PortedBlocks$VariantSlabBlock` | 1152 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `rebar_concrete_stairs` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `rebar_concrete_wall` | `NativeDecorativeBlocks$NativeWallBlock` | 15552 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `halfslab_rebar_concrete` | `PortedBlocks$PartsBlock` | 1440 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `rebar_concrete_tile` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Retain regression + wet inserted-light test; full-block storage/passage proof |
| `rebar_concrete_tile_slab` | `PortedBlocks$VariantSlabBlock` | 1152 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `rebar_concrete_tile_stairs` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `panzerglass_block` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Retain regression + wet inserted-light test; full-block storage/passage proof |
| `panzerglass_slab` | `PortedBlocks$VariantSlabBlock` | 1152 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `dark_shingle_roof` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `dark_shingle_roof_metallized` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `dark_shingle_roof_skylight` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `dark_shingle_roof_chimneytrunk` | `ChimneyTrunkBlock` | 3840 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `dark_shingle_roof_wireconduit` | `RoofWireConduitBlock` | 3840 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `dark_shingle_roof_chimney` | `ChimneyBlock` | 49152 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `dark_shingle_roof_block` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Retain regression + wet inserted-light test; full-block storage/passage proof |
| `dark_shingle_roof_slab` | `PortedBlocks$VariantSlabBlock` | 1152 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `dark_shingle_roof_slabslice` | `PortedBlocks$PartsBlock` | 1440 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `dense_grit_dirt_block` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Retain regression + wet inserted-light test; full-block storage/passage proof |
| `dense_grit_sand_block` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Retain regression + wet inserted-light test; full-block storage/passage proof |
| `ariadne_marker` | `PortedBlocks$AriadneMarkerBlock` | 4608 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `old_industrial_wood_planks` | `NativeDecorativeBlocks$NativeSolidBlock` | 6 | none | N | full | Retain regression + wet inserted-light test; full-block storage/passage proof |
| `old_industrial_wood_slab` | `PortedBlocks$VariantSlabBlock` | 1152 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `old_industrial_wood_stairs` | `NativeDecorativeBlocks$NativeStairBlock` | 3840 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `old_industrial_wood_slabslice` | `PortedBlocks$PartsBlock` | 1440 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `old_industrial_wood_door` | `NativeDecorativeBlocks$NativeDoorBlock` | 6144 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `thick_steel_pole` | `PortedBlocks$CenteredPoleBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `thick_steel_pole_head` | `PoleHeadBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `thin_steel_pole` | `PortedBlocks$CenteredPoleBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `thin_steel_pole_head` | `PoleHeadBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `treated_wood_pole` | `PortedBlocks$CenteredPoleBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `treated_wood_pole_head` | `PoleHeadBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `treated_wood_pole_support` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `steel_double_t_support` | `PortedBlocks$SupportBlock` | 2304 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `steel_floor_grating` | `PortedBlocks$FloorGratingBlock` | 96 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `steel_catwalk` | `PortedBlocks$CatwalkBlock` | 7680 | N | N + T | partial/empty | Retain regression + wet inserted-light test |
| `steel_catwalk_stairs` | `PortedBlocks$CatwalkStairsBlock` | 30720 | N + T | N + T | partial/empty | Retain regression + wet inserted-light test |
| `steel_catwalk_ta` | `RaisedCatwalkBlock` | 1152 | N | N + T | partial/empty | Retain regression + wet inserted-light test |
| `steel_railing` | `PortedBlocks$RailingBlock` | 7680 | N | N + T | partial/empty | Retain regression + wet inserted-light test |
| `steel_mesh_fence` | `SteelMeshFenceBlock` | 15552 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `steel_mesh_fence_gate` | `PortedBlocks$FenceGateSegmentBlock` | 1536 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `metal_rung_ladder` | `NativeDecorativeBlocks$NativeLadderBlock` | 384 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `metal_rung_steps` | `NativeDecorativeBlocks$NativeLadderBlock` | 384 | W | N + T | partial/empty | Retain regression + wet inserted-light test |
| `steel_table` | `PortedBlocks$TableBlock` | 96 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `treated_wood_table` | `PortedBlocks$TableBlock` | 96 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `treated_wood_stool` | `TreatedWoodStoolBlock` | 96 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `metal_crafting_table` | `MachineBlocks$MetalCraftingTableBlock` | 1 | none | none | full | Machines: exterior cell vs inventory/tank; full-block storage/passage proof |
| `labeled_crate` | `MachineBlocks$LabeledCrateBlock` | 4 | none | none | full | Machines: exterior cell vs inventory/tank; full-block storage/passage proof |
| `factory_hopper` | `MachineBlocks$DirectionalMachineBlock` | 6 | none | none | full | Machines: exterior cell vs inventory/tank; full-block storage/passage proof |
| `factory_dropper` | `MachineBlocks$OpenDirectionalMachineBlock` | 12 | none | none | full | Machines: exterior cell vs inventory/tank; full-block storage/passage proof |
| `factory_placer` | `MachineBlocks$DirectionalMachineBlock` | 6 | none | none | full | Machines: exterior cell vs inventory/tank; full-block storage/passage proof |
| `small_block_breaker` | `MachineBlocks$ActiveHorizontalMachineBlock` | 8 | none | none | partial/empty | Machines: exterior cell vs inventory/tank |
| `small_waste_incinerator` | `MachineBlocks$LitMachineBlock` | 2 | none | none | full | Machines: exterior cell vs inventory/tank; full-block storage/passage proof |
| `small_lab_furnace` | `MachineBlocks$LitHorizontalMachineBlock` | 8 | none | none | full | Machines: exterior cell vs inventory/tank; full-block storage/passage proof |
| `small_electrical_furnace` | `MachineBlocks$LitHorizontalMachineBlock` | 8 | none | none | full | Machines: exterior cell vs inventory/tank; full-block storage/passage proof |
| `small_mineral_smelter` | `MachineBlocks$Phase3HorizontalMachineBlock` | 16 | none | none | full | Machines: exterior cell vs inventory/tank; full-block storage/passage proof |
| `small_freezer` | `MachineBlocks$Phase4HorizontalMachineBlock` | 20 | none | none | full | Machines: exterior cell vs inventory/tank; full-block storage/passage proof |
| `fluid_barrel` | `MachineBlocks$LevelDirectionalMachineBlock` | 30 | none | none | full | Machines: exterior cell vs inventory/tank; full-block storage/passage proof |
| `small_fluid_funnel` | `MachineBlocks$LevelOnlyMachineBlock` | 4 | none | none | full | Machines: exterior cell vs inventory/tank; full-block storage/passage proof |
| `passive_fluid_accumulator` | `MachineBlocks$PassiveFluidAccumulatorBlock` | 2 | W | W | partial/empty | Machines: exterior cell vs inventory/tank |
| `small_solar_panel` | `MachineBlocks$SolarPanelBlock` | 5 | none | none | full | Machines: exterior cell vs inventory/tank; full-block storage/passage proof |
| `small_milking_machine` | `MachineBlocks$MilkingMachineBlock` | 16 | none | none | partial/empty | Machines: exterior cell vs inventory/tank |
| `small_tree_cutter` | `MachineBlocks$ActiveHorizontalMachineBlock` | 8 | none | none | partial/empty | Machines: exterior cell vs inventory/tank |
| `straight_pipe_valve` | `MachineBlocks$PipeValveBlock` | 384 | none | none | partial/empty | Machines: exterior cell vs inventory/tank |
| `straight_pipe_valve_redstone` | `MachineBlocks$PipeValveBlock` | 384 | none | none | partial/empty | Machines: exterior cell vs inventory/tank |
| `straight_pipe_valve_redstone_analog` | `MachineBlocks$PipeValveBlock` | 384 | none | none | partial/empty | Machines: exterior cell vs inventory/tank |
| `steel_framed_window` | `PortedBlocks$SteelFramedWindowBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `metal_sliding_door` | `PortedBlocks$SlidingDoorBlock` | 18432 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `iron_hatch` | `PortedBlocks$HatchBlock` | 1536 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `iron_bulb_light` | `PortedBlocks$OppositeFaceLightBlock` | 576 | none | N + T | partial/empty | Light: client render, all orientations |
| `iron_ceiling_edge_light` | `PortedBlocks$HorizontalLookLightBlock` | 576 | none | N + T | partial/empty | Light: client render, all orientations |
| `iron_floor_edge_light` | `PortedBlocks$HorizontalLookLightBlock` | 576 | none | N + T | partial/empty | Light: client render, all orientations |
| `iron_inset_light` | `PortedBlocks$OppositeFaceLightBlock` | 576 | none | N + T | partial/empty | Light: client render, all orientations |
| `sign_caution` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `sign_danger` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `sign_decor` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `sign_defense` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `sign_exit` | `PortedBlocks$ExitSignBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `sign_firehazard` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `sign_frost` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `sign_hotsurface` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `sign_hotwire` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `sign_laser` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `sign_magichazard` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `sign_magneticfield` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `sign_radioactive` | `PortedBlocks$SurfaceMountedBlock` | 576 | none | N + T | partial/empty | Retain regression + wet inserted-light test |
| `arrow_target` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `door_sensor_switch` | `ControlsBlockTypes$DoorSensorSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `elevator_button` | `ControlsBlockTypes$ElevatorButtonBlock` | 3456 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `glass_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `glass_contact_mat` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `glass_day_timer` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `glass_door_contact_mat` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `glass_entity_detector` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `glass_interval_timer` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `glass_linear_entity_detector` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `glass_rotary_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `glass_small_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `glass_touch_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `glass_touch_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `glass_vertical_bar_gauge` | `ControlsBlockTypes$GaugeBlock` | 9216 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_alarm_lamp` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_alarm_siren` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_analog_angular_gauge` | `ControlsBlockTypes$GaugeBlock` | 9216 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_analog_horizontal_gauge` | `ControlsBlockTypes$GaugeBlock` | 9216 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_blinking_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_block_detector` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_comparator_switch` | `ControlsBlockTypes$ComparatorSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_contact_mat` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_day_timer` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_dimmer` | `ControlsBlockTypes$DimmerBlock` | 18432 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_door_contact_mat` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_double_pole_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_entity_detector` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_estop_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_fallthrough_detector` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_fenced_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_foot_button` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_green_blinking_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_green_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_high_sensitive_trapdoor` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_hopper_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_interval_timer` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_knock_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_knock_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_lever` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_light_sensor` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_lightning_sensor` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_linear_entity_detector` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_machine_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_pull_handle` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_rain_sensor` | `ControlsBlockTypes$SensorSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_red_blinking_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_red_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_rotary_lever` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_rotary_machine_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_shock_sensitive_contact_mat` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_shock_sensitive_trapdoor` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_small_digital_gauge` | `ControlsBlockTypes$GaugeBlock` | 9216 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_small_lever` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_switchlink_cased_pulse_receiver` | `ControlsBlockTypes$CasedSwitchLinkPulseReceiverBlock` | 1152 | none | N + T | full | Controls: wet signals/support/multipart covered; class visual proof; full-block storage/passage proof |
| `industrial_switchlink_cased_receiver` | `ControlsBlockTypes$CasedSwitchLinkReceiverBlock` | 1152 | none | N + T | full | Controls: wet signals/support/multipart covered; class visual proof; full-block storage/passage proof |
| `industrial_switchlink_pulse_receiver` | `ControlsBlockTypes$SwitchLinkPulseReceiverBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_switchlink_pulse_relay` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_switchlink_receiver` | `ControlsBlockTypes$SwitchLinkReceiverBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_switchlink_receiver_analog` | `ControlsBlockTypes$SwitchLinkReceiverBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_switchlink_relay` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_switchlink_relay_analog` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_tube_gauge` | `ControlsBlockTypes$GaugeBlock` | 9216 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_vertical_bar_gauge` | `ControlsBlockTypes$GaugeBlock` | 9216 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_white_blinking_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_white_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_yellow_blinking_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrial_yellow_led` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `industrialswitch` | `IndustrialSwitchBlock` | 3456 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `light_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `oldfancy_bistableswitch1` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `oldfancy_bistableswitch2` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `oldfancy_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `oldfancy_small_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `oldfancy_spring_reset_chain` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `orange_sensitiveglass` | `ControlsBlockTypes$SensitiveGlassBlock` | 192 | none | N + T | full | Controls: wet signals/support/multipart covered; class visual proof; full-block storage/passage proof |
| `red_power_plant` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `rustic_angular_lever` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `rustic_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `rustic_contact_plate` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `rustic_door_contact_plate` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `rustic_fallthrough_detector` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `rustic_high_sensitive_trapdoor` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `rustic_lever` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `rustic_nail_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `rustic_nail_lever` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `rustic_semaphore` | `ControlsBlockTypes$BooleanIndicatorBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `rustic_shock_sensitive_plate` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `rustic_shock_sensitive_trapdoor` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `rustic_small_button` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `rustic_spring_reset_chain` | `ControlsBlockTypes$PulseSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `rustic_two_hinge_lever` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `sensitive_glass_block` | `ControlsBlockTypes$SensitiveGlassBlock` | 192 | none | N + T | full | Controls: wet signals/support/multipart covered; class visual proof; full-block storage/passage proof |
| `stained_sensitiveglass` | `ControlsBlockTypes$SensitiveGlassBlock` | 192 | none | N + T | full | Controls: wet signals/support/multipart covered; class visual proof; full-block storage/passage proof |
| `valve_wheel_switch` | `ControlsBlockTypes$ToggleSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
| `yellow_power_plant` | `ControlsBlockTypes$ContactSwitchBlock` | 1152 | none | N + T | partial/empty | Controls: wet signals/support/multipart covered; class visual proof |
