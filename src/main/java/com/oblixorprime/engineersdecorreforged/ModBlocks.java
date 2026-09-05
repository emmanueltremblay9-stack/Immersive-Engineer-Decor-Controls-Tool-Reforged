package com.oblixorprime.engineersdecorreforged;

import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineBlocks;
import com.oblixorprime.engineersdecorreforged.utility.MachineKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;

public final class ModBlocks {
   public static final Blocks BLOCKS = DeferredRegister.createBlocks("immersive_engineer_decor_controls_tool_reforged");
   private static final List<DeferredBlock<? extends Block>> MUTABLE_ORDERED_BLOCKS = new ArrayList<>();
   public static final List<DeferredBlock<? extends Block>> ORDERED_BLOCKS = Collections.unmodifiableList(MUTABLE_ORDERED_BLOCKS);
   public static final DeferredBlock<Block> CLINKER_BRICK_BLOCK = registerWithItem("clinker_brick_block", () -> new Block(stone(0.5F, 7.0F)));
   public static final DeferredBlock<PortedBlocks.VariantSlabBlock> CLINKER_BRICK_SLAB = registerWithItem(
      "clinker_brick_slab", () -> new PortedBlocks.VariantSlabBlock(stone(0.5F, 7.0F))
   );
   public static final DeferredBlock<StairBlock> CLINKER_BRICK_STAIRS = registerWithItem("clinker_brick_stairs", () -> stairs(stone(0.5F, 7.0F)));
   public static final DeferredBlock<WallBlock> CLINKER_BRICK_WALL = registerWithItem(
      "clinker_brick_wall", () -> new WallBlock(stone(0.5F, 7.0F).forceSolidOn())
   );
   public static final DeferredBlock<Block> CLINKER_BRICK_STAINED_BLOCK = registerWithItem("clinker_brick_stained_block", () -> new Block(stone(0.5F, 7.0F)));
   public static final DeferredBlock<PortedBlocks.VariantSlabBlock> CLINKER_BRICK_STAINED_SLAB = registerWithItem(
      "clinker_brick_stained_slab", () -> new PortedBlocks.VariantSlabBlock(stone(0.5F, 7.0F))
   );
   public static final DeferredBlock<StairBlock> CLINKER_BRICK_STAINED_STAIRS = registerWithItem(
      "clinker_brick_stained_stairs", () -> stairs(stone(0.5F, 7.0F))
   );
   public static final DeferredBlock<PortedBlocks.DirectionalPortBlock> CLINKER_BRICK_SASTOR_CORNER_BLOCK = registerWithItem(
      "clinker_brick_sastor_corner_block", () -> new PortedBlocks.DirectionalPortBlock(stone(0.5F, 7.0F).noOcclusion())
   );
   public static final DeferredBlock<PortedBlocks.HorizontalFacingBlock> CLINKER_BRICK_RECESSED = registerWithItem(
      "clinker_brick_recessed", () -> new PortedBlocks.HorizontalFacingBlock(stone(0.5F, 7.0F).noOcclusion())
   );
   public static final DeferredBlock<PortedBlocks.HorizontalFacingBlock> CLINKER_BRICK_VERTICALLY_SLIT = registerWithItem(
      "clinker_brick_vertically_slit", () -> new PortedBlocks.HorizontalFacingBlock(stone(0.5F, 7.0F).noOcclusion())
   );
   public static final DeferredBlock<PortedBlocks.VerticalSlabBlock> CLINKER_BRICK_VERTICAL_SLAB_STRUCTURED = registerWithItem(
      "clinker_brick_vertical_slab_structured", () -> new PortedBlocks.VerticalSlabBlock(stone(0.5F, 7.0F).noOcclusion())
   );
   public static final DeferredBlock<PortedBlocks.PartsBlock> HALFSLAB_CLINKER_BRICK = registerWithItem(
      "halfslab_clinker_brick", () -> new PortedBlocks.PartsBlock(stone(0.5F, 7.0F).noOcclusion())
   );
   public static final DeferredBlock<Block> SLAG_BRICK_BLOCK = registerWithItem("slag_brick_block", () -> new Block(stone(0.5F, 7.0F)));
   public static final DeferredBlock<PortedBlocks.VariantSlabBlock> SLAG_BRICK_SLAB = registerWithItem(
      "slag_brick_slab", () -> new PortedBlocks.VariantSlabBlock(stone(0.5F, 7.0F))
   );
   public static final DeferredBlock<StairBlock> SLAG_BRICK_STAIRS = registerWithItem("slag_brick_stairs", () -> stairs(stone(0.5F, 7.0F)));
   public static final DeferredBlock<WallBlock> SLAG_BRICK_WALL = registerWithItem("slag_brick_wall", () -> new WallBlock(stone(0.5F, 7.0F).forceSolidOn()));
   public static final DeferredBlock<Block> REBAR_CONCRETE = registerWithItem(
      "rebar_concrete", () -> new Block(stone(1.0F, 2000.0F).requiresCorrectToolForDrops())
   );
   public static final DeferredBlock<PortedBlocks.VariantSlabBlock> REBAR_CONCRETE_SLAB = registerWithItem(
      "rebar_concrete_slab", () -> new PortedBlocks.VariantSlabBlock(stone(1.0F, 2000.0F).requiresCorrectToolForDrops())
   );
   public static final DeferredBlock<StairBlock> REBAR_CONCRETE_STAIRS = registerWithItem(
      "rebar_concrete_stairs", () -> stairs(stone(1.0F, 2000.0F).requiresCorrectToolForDrops())
   );
   public static final DeferredBlock<WallBlock> REBAR_CONCRETE_WALL = registerWithItem(
      "rebar_concrete_wall", () -> new WallBlock(stone(1.0F, 2000.0F).requiresCorrectToolForDrops().forceSolidOn())
   );
   public static final DeferredBlock<PortedBlocks.PartsBlock> HALFSLAB_REBAR_CONCRETE = registerWithItem(
      "halfslab_rebar_concrete", () -> new PortedBlocks.PartsBlock(stone(1.0F, 2000.0F).noOcclusion().requiresCorrectToolForDrops())
   );
   public static final DeferredBlock<Block> REBAR_CONCRETE_TILE = registerWithItem(
      "rebar_concrete_tile", () -> new Block(stone(1.0F, 2000.0F).requiresCorrectToolForDrops())
   );
   public static final DeferredBlock<PortedBlocks.VariantSlabBlock> REBAR_CONCRETE_TILE_SLAB = registerWithItem(
      "rebar_concrete_tile_slab", () -> new PortedBlocks.VariantSlabBlock(stone(1.0F, 2000.0F).requiresCorrectToolForDrops())
   );
   public static final DeferredBlock<StairBlock> REBAR_CONCRETE_TILE_STAIRS = registerWithItem(
      "rebar_concrete_tile_stairs", () -> stairs(stone(1.0F, 2000.0F).requiresCorrectToolForDrops())
   );
   public static final DeferredBlock<Block> PANZERGLASS_BLOCK = registerWithItem("panzerglass_block", () -> new Block(glass()));
   public static final DeferredBlock<PortedBlocks.VariantSlabBlock> PANZERGLASS_SLAB = registerWithItem(
      "panzerglass_slab", () -> new PortedBlocks.VariantSlabBlock(glass())
   );
   public static final DeferredBlock<StairBlock> DARK_SHINGLE_ROOF = registerWithItem("dark_shingle_roof", () -> stairs(stone(0.5F, 6.0F).noOcclusion()));
   public static final DeferredBlock<StairBlock> DARK_SHINGLE_ROOF_METALLIZED = registerWithItem(
      "dark_shingle_roof_metallized", () -> stairs(stone(0.5F, 6.0F).noOcclusion())
   );
   public static final DeferredBlock<StairBlock> DARK_SHINGLE_ROOF_SKYLIGHT = registerWithItem(
      "dark_shingle_roof_skylight", () -> stairs(glass().strength(0.5F, 6.0F))
   );
   public static final DeferredBlock<StairBlock> DARK_SHINGLE_ROOF_CHIMNEYTRUNK = registerWithItem(
      "dark_shingle_roof_chimneytrunk", () -> stairs(stone(0.5F, 6.0F).noOcclusion())
   );
   public static final DeferredBlock<StairBlock> DARK_SHINGLE_ROOF_WIRECONDUIT = registerWithItem(
      "dark_shingle_roof_wireconduit", () -> stairs(stone(0.5F, 6.0F).noOcclusion())
   );
   public static final DeferredBlock<PortedBlocks.FixedShapeBlock> DARK_SHINGLE_ROOF_CHIMNEY = registerWithItem(
      "dark_shingle_roof_chimney", () -> new PortedBlocks.FixedShapeBlock(stone(0.5F, 6.0F).noOcclusion(), Block.box(3.0, 0.0, 3.0, 13.0, 6.0, 13.0))
   );
   public static final DeferredBlock<Block> DARK_SHINGLE_ROOF_BLOCK = registerWithItem("dark_shingle_roof_block", () -> new Block(stone(0.5F, 6.0F)));
   public static final DeferredBlock<PortedBlocks.VariantSlabBlock> DARK_SHINGLE_ROOF_SLAB = registerWithItem(
      "dark_shingle_roof_slab", () -> new PortedBlocks.VariantSlabBlock(stone(0.5F, 6.0F))
   );
   public static final DeferredBlock<PortedBlocks.PartsBlock> DARK_SHINGLE_ROOF_SLABSLICE = registerWithItem(
      "dark_shingle_roof_slabslice", () -> new PortedBlocks.PartsBlock(stone(0.5F, 6.0F).noOcclusion())
   );
   public static final DeferredBlock<Block> DENSE_GRIT_DIRT_BLOCK = registerWithItem("dense_grit_dirt_block", () -> new Block(earth()));
   public static final DeferredBlock<Block> DENSE_GRIT_SAND_BLOCK = registerWithItem("dense_grit_sand_block", () -> new Block(earth()));
   public static final DeferredBlock<PortedBlocks.AriadneMarkerBlock> ARIADNE_MARKER = register(
      "ariadne_marker",
      () -> new PortedBlocks.AriadneMarkerBlock(
         Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.1F, 0.1F).sound(SoundType.STONE).noCollission().noOcclusion()
      )
   );
   public static final DeferredBlock<Block> OLD_INDUSTRIAL_WOOD_PLANKS = registerWithItem("old_industrial_wood_planks", () -> new Block(wood()));
   public static final DeferredBlock<PortedBlocks.VariantSlabBlock> OLD_INDUSTRIAL_WOOD_SLAB = registerWithItem(
      "old_industrial_wood_slab", () -> new PortedBlocks.VariantSlabBlock(wood())
   );
   public static final DeferredBlock<StairBlock> OLD_INDUSTRIAL_WOOD_STAIRS = registerWithItem("old_industrial_wood_stairs", () -> stairs(wood()));
   public static final DeferredBlock<PortedBlocks.PartsBlock> OLD_INDUSTRIAL_WOOD_SLABSLICE = registerWithItem(
      "old_industrial_wood_slabslice", () -> new PortedBlocks.PartsBlock(wood().noOcclusion())
   );
   public static final DeferredBlock<DoorBlock> OLD_INDUSTRIAL_WOOD_DOOR = registerWithItem(
      "old_industrial_wood_door", () -> new DoorBlock(BlockSetType.OAK, wood().noOcclusion())
   );
   public static final DeferredBlock<PortedBlocks.CenteredPoleBlock> THICK_STEEL_POLE = registerWithItem(
      "thick_steel_pole", () -> new PortedBlocks.CenteredPoleBlock(metal().noOcclusion(), 4.0, 12.0)
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> THICK_STEEL_POLE_HEAD = registerWithItem(
      "thick_steel_pole_head", () -> new PortedBlocks.SurfaceMountedBlock(metal().noOcclusion(), 4.0, 12.0, 4.0, false)
   );
   public static final DeferredBlock<PortedBlocks.CenteredPoleBlock> THIN_STEEL_POLE = registerWithItem(
      "thin_steel_pole", () -> new PortedBlocks.CenteredPoleBlock(metal().noOcclusion(), 6.0, 10.0)
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> THIN_STEEL_POLE_HEAD = registerWithItem(
      "thin_steel_pole_head", () -> new PortedBlocks.SurfaceMountedBlock(metal().noOcclusion(), 5.0, 11.0, 3.0, false)
   );
   public static final DeferredBlock<PortedBlocks.CenteredPoleBlock> TREATED_WOOD_POLE = registerWithItem(
      "treated_wood_pole", () -> new PortedBlocks.CenteredPoleBlock(wood().noOcclusion(), 5.0, 11.0)
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> TREATED_WOOD_POLE_HEAD = registerWithItem(
      "treated_wood_pole_head", () -> new PortedBlocks.SurfaceMountedBlock(wood().noOcclusion(), 4.0, 12.0, 4.0, false)
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> TREATED_WOOD_POLE_SUPPORT = registerWithItem(
      "treated_wood_pole_support", () -> new PortedBlocks.SurfaceMountedBlock(wood().noOcclusion(), 3.0, 13.0, 5.0, false)
   );
   public static final DeferredBlock<PortedBlocks.SupportBlock> STEEL_DOUBLE_T_SUPPORT = registerWithItem(
      "steel_double_t_support", () -> new PortedBlocks.SupportBlock(metal().noOcclusion())
   );
   public static final DeferredBlock<PortedBlocks.FloorGratingBlock> STEEL_FLOOR_GRATING = registerWithItem(
      "steel_floor_grating", () -> new PortedBlocks.FloorGratingBlock(metal().noOcclusion())
   );
   public static final DeferredBlock<PortedBlocks.CatwalkBlock> STEEL_CATWALK = registerWithItem(
      "steel_catwalk", () -> new PortedBlocks.CatwalkBlock(metal().noOcclusion())
   );
   public static final DeferredBlock<PortedBlocks.CatwalkStairsBlock> STEEL_CATWALK_STAIRS = registerWithItem(
      "steel_catwalk_stairs", () -> new PortedBlocks.CatwalkStairsBlock(metal().noOcclusion())
   );
   public static final DeferredBlock<PortedBlocks.VariantOnlyBlock> STEEL_CATWALK_TA = registerWithItem(
      "steel_catwalk_ta", () -> new PortedBlocks.VariantOnlyBlock(metal().noOcclusion())
   );
   public static final DeferredBlock<PortedBlocks.RailingBlock> STEEL_RAILING = registerWithItem(
      "steel_railing", () -> new PortedBlocks.RailingBlock(metal().noOcclusion())
   );
   public static final DeferredBlock<WallBlock> STEEL_MESH_FENCE = registerWithItem(
      "steel_mesh_fence", () -> new WallBlock(metal().noOcclusion().forceSolidOn())
   );
   public static final DeferredBlock<PortedBlocks.FenceGateSegmentBlock> STEEL_MESH_FENCE_GATE = registerWithItem(
      "steel_mesh_fence_gate", () -> new PortedBlocks.FenceGateSegmentBlock(metal().noOcclusion())
   );
   public static final DeferredBlock<LadderBlock> METAL_RUNG_LADDER = registerWithItem(
      "metal_rung_ladder", () -> new LadderBlock(metal().noCollission().noOcclusion())
   );
   public static final DeferredBlock<LadderBlock> METAL_RUNG_STEPS = registerWithItem(
      "metal_rung_steps", () -> new LadderBlock(metal().noCollission().noOcclusion())
   );
   public static final DeferredBlock<PortedBlocks.TableBlock> STEEL_TABLE = registerWithItem(
      "steel_table", () -> new PortedBlocks.TableBlock(metal().noOcclusion())
   );
   public static final DeferredBlock<PortedBlocks.TableBlock> TREATED_WOOD_TABLE = registerWithItem(
      "treated_wood_table", () -> new PortedBlocks.TableBlock(wood().noOcclusion())
   );
   public static final DeferredBlock<PortedBlocks.StoolBlock> TREATED_WOOD_STOOL = registerWithItem(
      "treated_wood_stool", () -> new PortedBlocks.StoolBlock(wood().noOcclusion())
   );
   public static final DeferredBlock<MachineBlocks.MetalCraftingTableBlock> METAL_CRAFTING_TABLE = registerWithItem(
      "metal_crafting_table", () -> new MachineBlocks.MetalCraftingTableBlock(machineMetal())
   );
   public static final DeferredBlock<MachineBlocks.LabeledCrateBlock> LABELED_CRATE = registerWithItem(
      "labeled_crate", () -> new MachineBlocks.LabeledCrateBlock(wood())
   );
   public static final DeferredBlock<MachineBlocks.DirectionalMachineBlock> FACTORY_HOPPER = registerWithItem(
      "factory_hopper", () -> new MachineBlocks.DirectionalMachineBlock(machineMetal(), MachineKind.FACTORY_HOPPER)
   );
   public static final DeferredBlock<MachineBlocks.OpenDirectionalMachineBlock> FACTORY_DROPPER = registerWithItem(
      "factory_dropper", () -> new MachineBlocks.OpenDirectionalMachineBlock(machineMetal(), MachineKind.FACTORY_DROPPER)
   );
   public static final DeferredBlock<MachineBlocks.DirectionalMachineBlock> FACTORY_PLACER = registerWithItem(
      "factory_placer", () -> new MachineBlocks.DirectionalMachineBlock(machineMetal(), MachineKind.FACTORY_PLACER)
   );
   public static final DeferredBlock<MachineBlocks.ActiveHorizontalMachineBlock> SMALL_BLOCK_BREAKER = registerWithItem(
      "small_block_breaker", () -> new MachineBlocks.ActiveHorizontalMachineBlock(machineMetal(), MachineKind.SMALL_BLOCK_BREAKER)
   );
   public static final DeferredBlock<MachineBlocks.LitMachineBlock> SMALL_WASTE_INCINERATOR = registerWithItem(
      "small_waste_incinerator", () -> new MachineBlocks.LitMachineBlock(litMachine(), MachineKind.SMALL_WASTE_INCINERATOR)
   );
   public static final DeferredBlock<MachineBlocks.LitHorizontalMachineBlock> SMALL_LAB_FURNACE = registerWithItem(
      "small_lab_furnace", () -> new MachineBlocks.LitHorizontalMachineBlock(litMachine(), MachineKind.SMALL_LAB_FURNACE)
   );
   public static final DeferredBlock<MachineBlocks.LitHorizontalMachineBlock> SMALL_ELECTRICAL_FURNACE = registerWithItem(
      "small_electrical_furnace", () -> new MachineBlocks.LitHorizontalMachineBlock(litMachine(), MachineKind.SMALL_ELECTRICAL_FURNACE)
   );
   public static final DeferredBlock<MachineBlocks.Phase3HorizontalMachineBlock> SMALL_MINERAL_SMELTER = registerWithItem(
      "small_mineral_smelter", () -> new MachineBlocks.Phase3HorizontalMachineBlock(litMachine(), MachineKind.SMALL_MINERAL_SMELTER)
   );
   public static final DeferredBlock<MachineBlocks.Phase4HorizontalMachineBlock> SMALL_FREEZER = registerWithItem(
      "small_freezer", () -> new MachineBlocks.Phase4HorizontalMachineBlock(machineMetal(), MachineKind.SMALL_FREEZER)
   );
   public static final DeferredBlock<MachineBlocks.LevelDirectionalMachineBlock> FLUID_BARREL = registerWithItem(
      "fluid_barrel", () -> new MachineBlocks.LevelDirectionalMachineBlock(machineMetal(), MachineKind.FLUID_BARREL)
   );
   public static final DeferredBlock<MachineBlocks.LevelOnlyMachineBlock> SMALL_FLUID_FUNNEL = registerWithItem(
      "small_fluid_funnel", () -> new MachineBlocks.LevelOnlyMachineBlock(machineMetal(), MachineKind.SMALL_FLUID_FUNNEL)
   );
   public static final DeferredBlock<MachineBlocks.MachineBlock> PASSIVE_FLUID_ACCUMULATOR = registerWithItem(
      "passive_fluid_accumulator", () -> new MachineBlocks.MachineBlock(machineMetal(), MachineKind.PASSIVE_FLUID_ACCUMULATOR)
   );
   public static final DeferredBlock<MachineBlocks.SolarPanelBlock> SMALL_SOLAR_PANEL = registerWithItem(
      "small_solar_panel", () -> new MachineBlocks.SolarPanelBlock(machineMetal())
   );
   public static final DeferredBlock<MachineBlocks.MilkingMachineBlock> SMALL_MILKING_MACHINE = registerWithItem(
      "small_milking_machine", () -> new MachineBlocks.MilkingMachineBlock(machineMetal())
   );
   public static final DeferredBlock<MachineBlocks.ActiveHorizontalMachineBlock> SMALL_TREE_CUTTER = registerWithItem(
      "small_tree_cutter", () -> new MachineBlocks.ActiveHorizontalMachineBlock(machineMetal(), MachineKind.SMALL_TREE_CUTTER)
   );
   public static final DeferredBlock<MachineBlocks.PipeValveBlock> STRAIGHT_PIPE_VALVE = registerWithItem(
      "straight_pipe_valve", () -> new MachineBlocks.PipeValveBlock(machineMetal(), false)
   );
   public static final DeferredBlock<MachineBlocks.PipeValveBlock> STRAIGHT_PIPE_VALVE_REDSTONE = registerWithItem(
      "straight_pipe_valve_redstone", () -> new MachineBlocks.PipeValveBlock(machineMetal(), true)
   );
   public static final DeferredBlock<MachineBlocks.PipeValveBlock> STRAIGHT_PIPE_VALVE_REDSTONE_ANALOG = registerWithItem(
      "straight_pipe_valve_redstone_analog", () -> new MachineBlocks.PipeValveBlock(machineMetal(), true)
   );
   public static final DeferredBlock<PortedBlocks.SteelFramedWindowBlock> STEEL_FRAMED_WINDOW = registerWithItem(
      "steel_framed_window", () -> new PortedBlocks.SteelFramedWindowBlock(glass())
   );
   public static final DeferredBlock<DoorBlock> METAL_SLIDING_DOOR = registerWithItem(
      "metal_sliding_door", () -> new PortedBlocks.SlidingDoorBlock(BlockSetType.IRON, metal().noOcclusion())
   );
   public static final DeferredBlock<PortedBlocks.HatchBlock> IRON_HATCH = registerWithItem(
      "iron_hatch", () -> new PortedBlocks.HatchBlock(BlockSetType.IRON, metal().noOcclusion())
   );
   public static final DeferredBlock<PortedBlocks.OppositeFaceLightBlock> IRON_BULB_LIGHT = registerWithItem(
      "iron_bulb_light",
      () -> new PortedBlocks.OppositeFaceLightBlock(
         light(), Shapes.or(Block.box(6.5, 6.5, 1.0, 9.5, 9.5, 4.0), Block.box(6.0, 6.0, 0.0, 10.0, 10.0, 1.0))
      )
   );
   public static final DeferredBlock<PortedBlocks.HorizontalLookLightBlock> IRON_CEILING_EDGE_LIGHT = registerWithItem(
      "iron_ceiling_edge_light",
      () -> new PortedBlocks.HorizontalLookLightBlock(
         light(),
         Shapes.or(
            Block.box(0.0, 15.5, 0.0, 16.0, 16.0, 2.0),
            Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 0.5),
            Block.box(0.0, 14.0, 0.0, 1.0, 16.0, 2.0),
            Block.box(15.0, 14.0, 0.0, 16.0, 16.0, 2.0)
         )
      )
   );
   public static final DeferredBlock<PortedBlocks.HorizontalLookLightBlock> IRON_FLOOR_EDGE_LIGHT = registerWithItem(
      "iron_floor_edge_light", () -> new PortedBlocks.HorizontalLookLightBlock(light(), Block.box(5.0, 0.0, 0.0, 11.0, 2.0, 0.5))
   );
   public static final DeferredBlock<PortedBlocks.OppositeFaceLightBlock> IRON_INSET_LIGHT = registerWithItem(
      "iron_inset_light", () -> new PortedBlocks.OppositeFaceLightBlock(light(), Block.box(5.2, 5.2, 0.0, 10.8, 10.8, 0.3))
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> SIGN_CAUTION = registerWithItem(
      "sign_caution", () -> new PortedBlocks.SurfaceMountedBlock(sign(), 1.0, 15.0, 1.0, true)
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> SIGN_DANGER = registerWithItem(
      "sign_danger", () -> new PortedBlocks.SurfaceMountedBlock(sign(), 1.0, 15.0, 1.0, true)
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> SIGN_DECOR = registerWithItem(
      "sign_decor", () -> new PortedBlocks.SurfaceMountedBlock(sign(), 1.0, 15.0, 1.0, true)
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> SIGN_DEFENSE = registerWithItem(
      "sign_defense", () -> new PortedBlocks.SurfaceMountedBlock(sign(), 1.0, 15.0, 1.0, true)
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> SIGN_EXIT = registerWithItem(
      "sign_exit", () -> new PortedBlocks.SurfaceMountedBlock(sign(), 1.0, 15.0, 1.0, true)
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> SIGN_FIREHAZARD = registerWithItem(
      "sign_firehazard", () -> new PortedBlocks.SurfaceMountedBlock(sign(), 1.0, 15.0, 1.0, true)
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> SIGN_FROST = registerWithItem(
      "sign_frost", () -> new PortedBlocks.SurfaceMountedBlock(sign(), 1.0, 15.0, 1.0, true)
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> SIGN_HOTSURFACE = registerWithItem(
      "sign_hotsurface", () -> new PortedBlocks.SurfaceMountedBlock(sign(), 1.0, 15.0, 1.0, true)
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> SIGN_HOTWIRE = registerWithItem(
      "sign_hotwire", () -> new PortedBlocks.SurfaceMountedBlock(sign(), 1.0, 15.0, 1.0, true)
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> SIGN_LASER = registerWithItem(
      "sign_laser", () -> new PortedBlocks.SurfaceMountedBlock(sign(), 1.0, 15.0, 1.0, true)
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> SIGN_MAGICHazard = registerWithItem(
      "sign_magichazard", () -> new PortedBlocks.SurfaceMountedBlock(sign(), 1.0, 15.0, 1.0, true)
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> SIGN_MAGNETICFIELD = registerWithItem(
      "sign_magneticfield", () -> new PortedBlocks.SurfaceMountedBlock(sign(), 1.0, 15.0, 1.0, true)
   );
   public static final DeferredBlock<PortedBlocks.SurfaceMountedBlock> SIGN_RADIOACTIVE = registerWithItem(
      "sign_radioactive", () -> new PortedBlocks.SurfaceMountedBlock(sign(), 1.0, 15.0, 1.0, true)
   );

   private ModBlocks() {
   }

   public static <T extends Block> DeferredBlock<T> registerWithItem(String name, Supplier<T> supplier) {
      DeferredBlock<T> block = register(name, supplier);
      ModItems.registerBlockItem(name, block);
      return block;
   }

   public static <T extends Block> DeferredBlock<T> register(String name, Supplier<T> supplier) {
      DeferredBlock<T> block = BLOCKS.register(name, supplier);
      MUTABLE_ORDERED_BLOCKS.add(block);
      return block;
   }

   private static StairBlock stairs(Properties properties) {
      return new StairBlock(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), properties);
   }

   private static Properties stone(float strength, float resistance) {
      return Properties.of().mapColor(MapColor.STONE).strength(strength, resistance).sound(SoundType.STONE);
   }

   private static Properties metal() {
      return Properties.of().mapColor(MapColor.METAL).strength(1.5F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
   }

   private static Properties machineMetal() {
      return metal().strength(2.0F, 12.0F).noOcclusion();
   }

   private static Properties litMachine() {
      return machineMetal().lightLevel(state -> state.hasProperty(MachineBlocks.LIT) && state.getValue(MachineBlocks.LIT) ? 12 : 0);
   }

   private static Properties wood() {
      return Properties.of().mapColor(MapColor.WOOD).strength(1.5F, 4.0F).sound(SoundType.WOOD);
   }

   private static Properties earth() {
      return Properties.of().mapColor(MapColor.DIRT).strength(0.6F, 0.6F).sound(SoundType.GRAVEL);
   }

   private static Properties glass() {
      return Properties.of().mapColor(MapColor.NONE).strength(0.5F, 2000.0F).sound(SoundType.GLASS).noOcclusion().requiresCorrectToolForDrops();
   }

   private static Properties light() {
      return metal().lightLevel(state -> 14).noOcclusion();
   }

   private static Properties sign() {
      return metal().noOcclusion();
   }
}
