package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.fluid.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.*;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExternalFluidloggingGameTests {
   private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
   private static final List<String> IE_IDS = List.of("stairs_treated_wood_horizontal", "stairs_treated_wood_vertical",
      "stairs_treated_wood_packaged", "slab_treated_wood_horizontal", "slab_treated_wood_vertical", "slab_treated_wood_packaged",
      "wall_clinker_brick", "treated_trapdoor", "treated_trapdoor_framed", "steel_trapdoor");

   private static Block ie(String id) { return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering", id)); }
   private static List<Block> samples() {
      var blocks = new ArrayList<Block>(List.of(Blocks.OAK_STAIRS, Blocks.STONE_SLAB, Blocks.OAK_FENCE,
         Blocks.COBBLESTONE_WALL, Blocks.GLASS_PANE, Blocks.RED_STAINED_GLASS_PANE, Blocks.IRON_BARS,
         Blocks.LADDER, Blocks.OAK_TRAPDOOR, Blocks.CUT_COPPER_STAIRS, Blocks.CUT_COPPER_SLAB, Blocks.COPPER_TRAPDOOR));
      IE_IDS.forEach(id -> blocks.add(ie(id)));
      return blocks;
   }
   private static List<FlowingFluid> fluids() {
      var result = new ArrayList<FlowingFluid>(List.of(Fluids.WATER, Fluids.LAVA));
      for (var id : List.of("creosote", "ethanol", "biodiesel", "plantoil"))
         result.add((FlowingFluid)BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("immersiveengineering", id)));
      return result;
   }
   private static BlockState prepare(GameTestHelper h, BlockPos pos, Block block) {
      var level = h.getLevel();
      for (var p : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 3, 1))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
      level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);
      if (block == Blocks.LADDER) level.setBlock(pos.south(), Blocks.STONE.defaultBlockState(), 3);
      level.setBlock(pos, block.defaultBlockState(), 3);
      return level.getBlockState(pos);
   }
   private static FakePlayer player(GameTestHelper h, BlockPos pos, boolean ladder) {
      var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "ExternalFluidTests"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      player.setPos(pos.getX()+.5, pos.getY()+2, pos.getZ()+(ladder ? .9 : .5));
      player.setYRot(0); player.setXRot(90);
      return player;
   }

   @GameTest(template="empty")
   public static void external_eligibility_is_exact_and_preserves_native_schema(GameTestHelper h) {
      for (var block : samples()) {
         h.assertTrue(ExternalFluidAdapter.supported(block.defaultBlockState()), "qualified block " + BuiltInRegistries.BLOCK.getKey(block));
         for (var state : block.getStateDefinition().getPossibleStates()) {
            boolean doubleSlab = state.hasProperty(SlabBlock.TYPE) && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE;
            h.assertValueEqual(ExternalFluidAdapter.supported(state), !doubleSlab, "all external states except double slabs");
            h.assertFalse(state.hasProperty(NativeFluidloggedBlock.FLUID_CELL), "no external fluid_cell property");
            h.assertFalse(state.hasProperty(com.oblixorprime.engineersdecorreforged.block.StairWaterFlow.WATER_FLOW), "no external water_flow property");
         }
      }
      for (var block : List.of(Blocks.OAK_DOOR, Blocks.CHEST, Blocks.FURNACE, Blocks.CAULDRON, Blocks.OAK_FENCE_GATE,
            Blocks.STONE, Blocks.SEA_PICKLE, ie("metal_ladder_none"), ie("steel_catwalk_stairs"), ie("capacitor_lv"),
            ie("stairs_concrete"), ie("slab_steel_scaffolding_standard")))
         h.assertFalse(ExternalFluidAdapter.supported(block.defaultBlockState()), "unqualified block stays excluded " + block);
      h.assertValueEqual(ModBlocks.ORDERED_BLOCKS.size(), 203, "native catalogue, contextual V stair and treated wood beam retained");
      for (var holder : ModBlocks.ORDERED_BLOCKS) {
         h.assertTrue(NativeFluidCells.supported(holder.get().defaultBlockState()), "native opt-in retained");
         h.assertFalse(ExternalFluidAdapter.supported(holder.get().defaultBlockState()), "native block uses its original adapter");
      }
      h.assertFalse(ExternalFluidAdapter.supportsFluid(BuiltInRegistries.FLUID.get(ResourceLocation.parse("minecraft:milk"))), "non-world fluid rejected");
      LOGGER.info("IEDCTR_EXTERNAL_ELIGIBILITY_PASS representatives={} ieIds={}", samples().size(), IE_IDS);
      h.succeed();
   }

   @GameTest(template="empty", timeoutTicks=200)
   public static void external_real_survival_buckets_conserve_sources(GameTestHelper h) {
      var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(3,2,3)); int cases = 0;
      for (var block : samples()) for (var fluid : fluids()) {
         var dry = prepare(h, pos, block); var player = player(h, pos, block == Blocks.LADDER);
         var container = (LiquidBlockContainer)block;
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(fluid.getBucket()));
         h.assertTrue(player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND).consumesAction(), "real fill " + block + " / " + fluid);
         h.assertTrue(player.getMainHandItem().is(Items.BUCKET), "one source bucket consumed");
         h.assertValueEqual(level.getFluidState(pos), fluid.getSource(false), "exact intentional source");
         var wet = level.getBlockState(pos);
         h.assertValueEqual(wet.setValue(BlockStateProperties.WATERLOGGED, false), dry, "geometry and properties retained");
         h.assertValueEqual(NativeFluidCells.snapshot(level.getChunkAt(pos)).contains(pos), fluid != Fluids.WATER, "single storage owner");
         h.assertFalse(container.canPlaceLiquid(player, level, pos, wet, fluid), "duplicate insertion rejected");
         player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
         h.assertTrue(player.gameMode.useItem(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND).consumesAction(), "real source pickup " + block);
         h.assertTrue(player.getMainHandItem().is(fluid.getBucket()), "exact source bucket returned");
         h.assertTrue(level.getFluidState(pos).isEmpty() && !NativeFluidCells.reserved(level,pos), "no hidden second fluid");
         h.assertTrue(((BucketPickup)block).pickupBlock(player,level,pos,level.getBlockState(pos)).isEmpty(), "no duplicate pickup");
         cases++;
      }
      LOGGER.info("IEDCTR_EXTERNAL_BUCKET_PASS cases={}", cases); h.succeed();
   }

   private static void channel(GameTestHelper h, Block block, FlowingFluid fluid) {
      var level=h.getLevel(); var source=h.absolutePos(new BlockPos(1,2,1)); var control=source.south(3);
      for(var p:BlockPos.betweenClosed(source.offset(-1,-1,-1),source.offset(5,2,4)))
         level.setBlock(p,p.getY()==source.getY()-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
      for(int x=-1;x<=5;x++)for(int z:new int[]{-1,1,2,4}) level.setBlock(source.offset(x,0,z),Blocks.BRICKS.defaultBlockState(),3);
      for(var p:List.of(source.west(),control.west(),source.east(5),control.east(5))) level.setBlock(p,Blocks.BRICKS.defaultBlockState(),3);
      var cell=source.east(); level.setBlock(cell,block.defaultBlockState(),3);
      var expectedBlock=level.getBlockState(cell);
      level.setBlock(source,fluid.getSource(false).createLegacyBlock(),3);
      level.setBlock(control,fluid.getSource(false).createLegacyBlock(),3);
      h.runAfterDelay(400,()->{
         for(int i=1;i<=3;i++) {
            var expected=level.getFluidState(control.east(i));
            h.assertFalse(expected.isEmpty(),"control reached "+i);
            h.assertValueEqual(level.getFluidState(source.east(i)),expected,"natural amount/falling at "+i+" "+block);
         }
         h.assertValueEqual(level.getBlockState(cell),expectedBlock,"flow preserves container state");
         h.assertFalse(level.getBlockState(cell).getValue(BlockStateProperties.WATERLOGGED),"arrival is transient");
         h.assertTrue(((BucketPickup)block).pickupBlock(null,level,cell,level.getBlockState(cell)).isEmpty(),"current cannot become a bucket");
         level.setBlock(source,Blocks.AIR.defaultBlockState(),3);level.setBlock(control,Blocks.AIR.defaultBlockState(),3);
         h.runAfterDelay(600,()->{
            for(int i=1;i<=3;i++) h.assertTrue(level.getFluidState(source.east(i)).isEmpty(),"flow fully recedes at "+i);
            h.assertValueEqual(level.getBlockState(cell),expectedBlock,"recession preserves block");
            h.assertFalse(NativeFluidCells.reserved(level,cell),"recession removes storage");
            LOGGER.info("IEDCTR_EXTERNAL_FLOW_PASS block={} fluid={} recession=true",block,fluid);h.succeed();
         });
      });
   }
   @GameTest(template="empty",timeoutTicks=1100) public static void external_flow_stairs(GameTestHelper h){channel(h,Blocks.OAK_STAIRS,Fluids.WATER);}
   @GameTest(template="empty",timeoutTicks=1100) public static void external_flow_slabs(GameTestHelper h){channel(h,Blocks.STONE_SLAB,Fluids.WATER);}
   @GameTest(template="empty",timeoutTicks=1100) public static void external_flow_fences(GameTestHelper h){channel(h,Blocks.OAK_FENCE,Fluids.WATER);}
   @GameTest(template="empty",timeoutTicks=1100) public static void external_flow_walls(GameTestHelper h){channel(h,Blocks.COBBLESTONE_WALL,Fluids.WATER);}
   @GameTest(template="empty",timeoutTicks=1100) public static void external_flow_panes(GameTestHelper h){channel(h,Blocks.GLASS_PANE,Fluids.WATER);}
   @GameTest(template="empty",timeoutTicks=1100) public static void external_flow_bars(GameTestHelper h){channel(h,Blocks.IRON_BARS,Fluids.WATER);}
   @GameTest(template="empty",timeoutTicks=1100) public static void external_flow_ladders(GameTestHelper h){channel(h,Blocks.LADDER,Fluids.WATER);}
   @GameTest(template="empty",timeoutTicks=1100) public static void external_flow_trapdoors(GameTestHelper h){channel(h,Blocks.OAK_TRAPDOOR,Fluids.WATER);}
   @GameTest(template="empty",timeoutTicks=1100) public static void external_flow_ie_stairs(GameTestHelper h){channel(h,ie("stairs_treated_wood_horizontal"),Fluids.WATER);}
   @GameTest(template="empty",timeoutTicks=1100) public static void external_flow_lava(GameTestHelper h){channel(h,Blocks.IRON_BARS,Fluids.LAVA);}
   @GameTest(template="empty",timeoutTicks=1100) public static void external_flow_ie_ethanol(GameTestHelper h){channel(h,Blocks.IRON_BARS,fluids().get(3));}

   @GameTest(template="empty",timeoutTicks=160)
   public static void external_no_source_promotion_and_legacy_source_preserved(GameTestHelper h) {
      var level=h.getLevel(); var pos=h.absolutePos(new BlockPos(3,2,3));prepare(h,pos,Blocks.IRON_BARS);
      // Keep both source cells on a solid floor: an open downward outlet makes vanilla prefer falling
      // and would never exercise the horizontal two-source promotion path.
      for(int x=-1;x<=1;x++) {
         level.setBlock(pos.offset(x,-1,0),Blocks.STONE.defaultBlockState(),3);
         level.setBlock(pos.offset(x,0,-1),Blocks.STONE.defaultBlockState(),3);
         level.setBlock(pos.offset(x,0,1),Blocks.STONE.defaultBlockState(),3);
      }
      level.setBlock(pos.west(2),Blocks.STONE.defaultBlockState(),3);level.setBlock(pos.east(2),Blocks.STONE.defaultBlockState(),3);
      level.setBlock(pos.west(),Blocks.WATER.defaultBlockState(),3); level.setBlock(pos.east(),Blocks.WATER.defaultBlockState(),3);
      h.runAfterDelay(30,()->{
         h.assertTrue(level.getFluidState(pos.west()).isSource() && level.getFluidState(pos.east()).isSource(),"both source controls remain supplied");
         h.assertFalse(level.getFluidState(pos).isEmpty(),"two sources feed the enclosed cell");
         h.assertFalse(level.getFluidState(pos).isSource(),"two neighbors do not promote current");
         level.setBlock(pos.west(),Blocks.STONE.defaultBlockState(),3);level.setBlock(pos.east(),Blocks.STONE.defaultBlockState(),3);
         // This is the exact pre-existing waterlogged save representation, with no attachment migration.
         var legacy=Blocks.STONE_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true);
         level.setBlock(pos,legacy,3);
         h.runAfterDelay(90,()->{h.assertValueEqual(level.getBlockState(pos),legacy,"legacy source survives supply removal");
            h.assertTrue(level.getFluidState(pos).isSource() && !NativeFluidCells.reserved(level,pos),"legacy has one native owner");h.succeed();});
      });
   }

   @GameTest(template="empty",timeoutTicks=100)
   public static void external_falling_and_closed_faces_obey_geometry(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(new BlockPos(2,2,2));var blocked=pos.east(3);
      prepare(h,pos,Blocks.STONE_SLAB);prepare(h,blocked,Blocks.STONE_SLAB);
      level.setBlock(blocked,Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP),3);
      for(var cell:List.of(pos,blocked)) {
         for(var direction:Direction.Plane.HORIZONTAL) {level.setBlock(cell.relative(direction),Blocks.STONE.defaultBlockState(),3);level.setBlock(cell.above().relative(direction),Blocks.STONE.defaultBlockState(),3);}
         level.setBlock(cell.above(),Blocks.WATER.defaultBlockState(),3);
      }
      h.runAfterDelay(30,()->{
         h.assertValueEqual(level.getFluidState(pos),Fluids.FLOWING_WATER.getFlowing(8,true),"falling retains level eight and FALLING");
         h.assertTrue(level.getFluidState(blocked).isEmpty(),"top slab full upper face blocks entry");h.succeed();
      });
   }

   @GameTest(template="empty",timeoutTicks=200)
   public static void external_orientations_and_trapdoor_changes_preserve_cells(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,2,3));int count=0;
      for(var block:List.of(Blocks.OAK_STAIRS,Blocks.STONE_SLAB,Blocks.OAK_TRAPDOOR,ie("steel_trapdoor"))) {
         for(var variant:block.getStateDefinition().getPossibleStates()) {
            if(!ExternalFluidAdapter.supported(variant) || variant.getValue(BlockStateProperties.WATERLOGGED))continue;
            prepare(h,pos,block);level.setBlock(pos,variant,2);
            var actual=level.getBlockState(pos);var shape=actual.getCollisionShape(level,pos);
            h.assertTrue(ExternalFluidAdapter.place(level,pos,actual,Fluids.FLOWING_WATER.getFlowing(4,true)),"oriented current accepted");
            var after=level.getBlockState(pos);
            h.assertValueEqual(after,actual,"attachment does not change orientation/shape properties");
            h.assertFalse(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape,after.getCollisionShape(level,pos),net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME),"collision unchanged");
            if(after.hasProperty(BlockStateProperties.OPEN)) {
               level.setBlock(pos,after.cycle(BlockStateProperties.OPEN),2);
               h.assertValueEqual(level.getFluidState(pos),Fluids.FLOWING_WATER.getFlowing(4,true),"trapdoor toggle keeps exact cell");
            }
            NativeFluidCells.set(level,pos,Fluids.EMPTY.defaultFluidState());count++;
         }
      }
      LOGGER.info("IEDCTR_EXTERNAL_ORIENTATION_PASS states={}",count);h.succeed();
   }

   @GameTest(template="empty")
   public static void external_wet_slab_merge_refused_and_dry_merge_works(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,2,3));var player=player(h,pos,false);
      for(var fluid:List.of(Fluids.WATER,Fluids.LAVA)) {
         prepare(h,pos,Blocks.STONE_SLAB);((LiquidBlockContainer)Blocks.STONE_SLAB).placeLiquid(level,pos,level.getBlockState(pos),fluid.getSource(false));
         player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE_SLAB,2));
         var hit=new BlockHitResult(new Vec3(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5),Direction.UP,pos,false);
         var context=new UseOnContext(player,InteractionHand.MAIN_HAND,hit);
         h.assertFalse(Items.STONE_SLAB.useOn(context).consumesAction(),"wet merge explicitly refuses");
         h.assertValueEqual(player.getMainHandItem().getCount(),2,"failed merge consumes no slab");
         h.assertValueEqual(level.getFluidState(pos),fluid.getSource(false),"failed merge loses no fluid");
         ((BucketPickup)Blocks.STONE_SLAB).pickupBlock(player,level,pos,level.getBlockState(pos));
         h.assertTrue(Items.STONE_SLAB.useOn(context).consumesAction(),"drained slab merges");
         h.assertValueEqual(level.getBlockState(pos).getValue(SlabBlock.TYPE),SlabType.DOUBLE,"double slab created");
         h.assertFalse(NativeFluidCells.supported(level.getBlockState(pos)),"double slab excluded");
         h.assertFalse(NativeFluidCells.reserved(level,pos),"double slab has no residual cell");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void external_destruction_replacement_and_incompatible_fluids(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,2,3));
      for(var fluid:fluids()) {
         var state=prepare(h,pos,Blocks.IRON_BARS);var container=(LiquidBlockContainer)Blocks.IRON_BARS;
         h.assertTrue(container.placeLiquid(level,pos,state,fluid.getSource(false)),"source admitted");
         var incompatible=fluid==Fluids.WATER?Fluids.LAVA:Fluids.WATER;
         h.assertFalse(container.placeLiquid(level,pos,level.getBlockState(pos),incompatible.getSource(false)),"different fluid cannot erase resident");
         h.assertValueEqual(level.getFluidState(pos),fluid.getSource(false),"rejected mixing conserves resident");
         h.assertTrue(level.destroyBlock(pos,false),"normal destruction succeeds");
         h.assertValueEqual(level.getFluidState(pos),fluid.getSource(false),"destruction releases fluid into world");
         h.assertFalse(NativeFluidCells.reserved(level,pos),"released fluid is not duplicated");
         level.setBlock(pos,Blocks.IRON_BARS.defaultBlockState(),3);
         h.assertValueEqual(level.getFluidState(pos),fluid.getSource(false),"replacement captures world fluid exactly");
         level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
         h.assertTrue(level.getFluidState(pos).isEmpty() && !NativeFluidCells.reserved(level,pos),"explicit solid replacement clears cell");
      }
      prepare(h,pos,Blocks.LADDER);ExternalFluidAdapter.place(level,pos,level.getBlockState(pos),Fluids.LAVA.getSource(false));
      level.setBlock(pos.south(),Blocks.AIR.defaultBlockState(),3);
      h.assertTrue(level.getBlockState(pos).getBlock() instanceof LiquidBlock,"lost ladder support releases fluid");
      h.assertTrue(!NativeFluidCells.reserved(level,pos) && level.getFluidState(pos).isSource(),"support loss keeps a single source");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void external_real_block_placement_preserves_world_fluid(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,2,3));var player=player(h,pos,false);
      for(var block:List.of(Blocks.OAK_STAIRS,Blocks.STONE_SLAB,Blocks.GLASS_PANE,ie("slab_treated_wood_horizontal")))
         for(var fluid:List.of(Fluids.WATER.getSource(false),Fluids.FLOWING_WATER.getFlowing(5,false),
               Fluids.FLOWING_WATER.getFlowing(8,true),Fluids.LAVA.getSource(false),Fluids.FLOWING_LAVA.getFlowing(5,false))) {
            prepare(h,pos,block);level.setBlock(pos,fluid.createLegacyBlock(),3);
            var expected=level.getFluidState(pos);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(block.asItem(),2));
            var hit=new BlockHitResult(new Vec3(pos.getX()+.5,pos.getY(),pos.getZ()+.5),Direction.UP,pos.below(),false);
            var context=new UseOnContext(player,InteractionHand.MAIN_HAND,hit);
            h.assertTrue(block.asItem().useOn(context).consumesAction(),"real block item placed into fluid "+block+" / "+expected);
            h.assertTrue(level.getBlockState(pos).is(block),"placement retains expected block");
            h.assertValueEqual(player.getMainHandItem().getCount(),1,"placement consumes exactly one item");
            h.assertValueEqual(level.getFluidState(pos),expected,"placement retains exact world level and falling");
            h.assertValueEqual(level.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED),expected.equals(Fluids.WATER.getSource(false)),"placement never promotes currents");
         }
      h.succeed();
   }

   @GameTest(template="empty",timeoutTicks=180)
   public static void external_natural_mixing_conserves_container_and_native_products(GameTestHelper h) {
      var level=h.getLevel();var water=h.absolutePos(new BlockPos(2,2,2));var lava=water.south(3);
      for(var pos:List.of(water,lava)) {
         prepare(h,pos,Blocks.IRON_BARS);
         for(int x=-1;x<=2;x++) {
            level.setBlock(pos.offset(x,-1,0),Blocks.STONE.defaultBlockState(),3);
            level.setBlock(pos.offset(x,0,-1),Blocks.STONE.defaultBlockState(),3);
            level.setBlock(pos.offset(x,0,1),Blocks.STONE.defaultBlockState(),3);
         }
         level.setBlock(pos.west(),Blocks.STONE.defaultBlockState(),3);level.setBlock(pos.east(2),Blocks.STONE.defaultBlockState(),3);
      }
      ExternalFluidAdapter.place(level,water,level.getBlockState(water),Fluids.WATER.getSource(false));
      level.setBlock(water.east(),Blocks.LAVA.defaultBlockState(),3);
      h.assertTrue(level.getBlockState(water.east()).is(Blocks.OBSIDIAN),"free lava keeps native contact reaction beside stored water");
      ExternalFluidAdapter.place(level,lava,level.getBlockState(lava),Fluids.LAVA.getSource(false));
      level.setBlock(lava.east(),Blocks.WATER.defaultBlockState(),3);
      h.runAfterDelay(120,()->{
         h.assertTrue(level.getBlockState(water).is(Blocks.IRON_BARS) && level.getFluidState(water).equals(Fluids.WATER.getSource(false)),"external water survives neighboring native reaction");
         h.assertTrue(level.getBlockState(lava).is(Blocks.IRON_BARS) && level.getFluidState(lava).equals(Fluids.LAVA.getSource(false)),"incoming water does not overwrite resident lava or its container");
         h.assertTrue(ExternalFluidAdapter.pickup(level,lava,level.getBlockState(lava)).is(Items.LAVA_BUCKET),"resident lava remains recoverable once");
         h.assertTrue(ExternalFluidAdapter.pickup(level,lava,level.getBlockState(lava)).isEmpty(),"mixing produces no second bucket");
         h.succeed();
      });
   }

   @GameTest(template="empty")
   public static void external_compressed_reload_palette_delta_and_light(GameTestHelper h)throws Exception {
      var level=h.getLevel();var cp=level.getChunkAt(h.absolutePos(new BlockPos(3,2,3))).getPos();var saved=new LevelChunk(level,cp);
      var cells=FluidCellData.EMPTY;int i=0;
      for(var block:samples())for(int amount=1;amount<=8;amount++) {
         var pos=new BlockPos(cp.getMinBlockX()+i%16,80+i/256,cp.getMinBlockZ()+(i/16)%16);i++;
         saved.setBlockState(pos,block.defaultBlockState(),false);cells=cells.with(pos,Fluids.FLOWING_WATER.getFlowing(amount,(amount&1)==0));
      }
      saved.setData(NativeFluidCells.CELLS.get(),cells);var bytes=new java.io.ByteArrayOutputStream();
      NbtIo.writeCompressed(ChunkSerializer.write(level,saved),bytes);
      var disk=NbtIo.readCompressed(new java.io.ByteArrayInputStream(bytes.toByteArray()),NbtAccounter.unlimitedHeap());
      var loaded=ChunkSerializer.read(level,level.getPoiManager(),new RegionStorageInfo("external-fluid-test",level.dimension(),"chunk"),cp,disk);
      h.assertValueEqual(loaded.getData(NativeFluidCells.CELLS.get()).encode(),cells.encode(),"compressed attachment roundtrip");
      h.assertTrue(loaded instanceof ImposterProtoChunk,"loaded chunk exposes runtime reads");
      var restored=((ImposterProtoChunk)loaded).getWrapped();
      for(long packed:cells.positions())h.assertValueEqual(restored.getFluidState(BlockPos.of(packed)),cells.get(BlockPos.of(packed)),"exact external state after reload");
      var raw=io.netty.buffer.Unpooled.buffer();var buf=new net.minecraft.network.RegistryFriendlyByteBuf(raw,level.registryAccess());
      try {
         cells.writeNetwork(buf,true);var received=FluidCellData.readNetwork(buf,null,saved);
         h.assertValueEqual(received.encode(),cells.encode(),"initial palette");
         var pos=BlockPos.of(cells.positions().iterator().next());var next=cells.with(pos,Fluids.LAVA.getSource(true));
         buf.clear();next.writeNetwork(buf,false);received=FluidCellData.readNetwork(buf,received,saved);
         h.assertValueEqual(received.get(pos),Fluids.LAVA.getSource(true),"source falling delta");
         buf.clear();next.with(pos,Fluids.EMPTY.defaultFluidState()).writeNetwork(buf,false);
         received=FluidCellData.readNetwork(buf,received,saved);h.assertFalse(received.contains(pos),"removal delta");
      }finally{raw.release();}
      var pos=h.absolutePos(new BlockPos(3,2,3));var state=prepare(h,pos,Blocks.IRON_BARS);
      h.assertTrue(state.hasDynamicLightEmission(),"external state has dynamic light");
      ExternalFluidAdapter.place(level,pos,state,Fluids.LAVA.getSource(false));
      h.assertValueEqual(level.getAuxLightManager(pos).getLightAt(pos),15,"auxiliary lava light");
      h.assertValueEqual(level.getBlockState(pos).getLightEmission(level,pos),15,"external light getter");
      ExternalFluidAdapter.pickup(level,pos,level.getBlockState(pos));h.assertValueEqual(state.getLightEmission(level,pos),0,"drain clears light");
      h.succeed();
   }

   @GameTest(template="empty",timeoutTicks=200)
   public static void external_dense_snapshot_mutation_measurement(GameTestHelper h) {
      for(int size:new int[]{256,1024,4096}) {
         var encoded=new CompoundTag();
         for(int i=0;i<size;i++)encoded.put(Long.toString(new BlockPos(i%16,80+i/256,(i/16)%16).asLong()),
            FluidState.CODEC.encodeStart(NbtOps.INSTANCE,Fluids.FLOWING_WATER.getFlowing(3,false)).getOrThrow());
         var original=FluidCellData.decode(encoded);var current=original;long begin=System.nanoTime();
         for(int i=0;i<64;i++)current=current.with(new BlockPos(i%16,80,(i/16)%16),Fluids.FLOWING_WATER.getFlowing(4,false));
         long elapsed=System.nanoTime()-begin;
         h.assertValueEqual(original.encode(),encoded,"published snapshot remains immutable under dense mutation");
         h.assertValueEqual(current.positions().size(),size,"dense updates preserve cell count");
         LOGGER.info("IEDCTR_EXTERNAL_DENSE_SAMPLE cells={} mutations=64 elapsedNanos={} mapCopyCost=true",size,elapsed);
      }
      h.succeed();
   }
}
