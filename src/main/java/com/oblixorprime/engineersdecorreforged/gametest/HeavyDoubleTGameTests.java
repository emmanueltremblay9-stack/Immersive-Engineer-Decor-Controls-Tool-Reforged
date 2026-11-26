package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.HeavyDoubleTOrientation;
import com.oblixorprime.engineersdecorreforged.block.HeavyDoubleTSupportBlock;
import com.oblixorprime.engineersdecorreforged.block.HeavyDoubleTTopology;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.block.HeavyDoubleTTopology.Contact;
import com.oblixorprime.engineersdecorreforged.block.SteelDoubleTBulbBlock;
import com.oblixorprime.engineersdecorreforged.block.VerticalDoubleTSupportBlock;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HeavyDoubleTGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 3, 3);

   private static BlockState heavy(Direction face, int roll) {
      return ModBlocks.HEAVY_INDUSTRIAL_DOUBLE_T_SUPPORT.get().defaultBlockState()
         .setValue(HeavyDoubleTSupportBlock.FACING, face)
         .setValue(HeavyDoubleTSupportBlock.ROLL, roll);
   }

   private static void clear(GameTestHelper h, BlockPos pos) {
      var level = h.getLevel();
      level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
      for (Direction d : Direction.values())
         level.setBlock(pos.relative(d), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
   }

   private static BlockHitResult hitFace(BlockPos pos, Direction face, double u, double v) {
      double x = pos.getX() + .5;
      double y = pos.getY() + .5;
      double z = pos.getZ() + .5;
      switch (face.getAxis()) {
         case X -> { y += u; z += v; }
         case Y -> { x += u; z += v; }
         case Z -> { x += u; y += v; }
      }
      return new BlockHitResult(new Vec3(x + face.getStepX() * .5,
         y + face.getStepY() * .5, z + face.getStepZ() * .5), face, pos, false);
   }

   private static net.minecraft.world.InteractionResult useBlockItem(GameTestHelper h, BlockPos source,
      Direction face, ItemStack stack, net.minecraft.world.entity.player.Player player, double u, double v) {
      player.setItemInHand(InteractionHand.MAIN_HAND, stack);
      return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hitFace(source, face, u, v)));
   }

   @GameTest(template = "empty")
   public static void all_24_rotations_and_registry_item(GameTestHelper h) {
      var block = ModBlocks.HEAVY_INDUSTRIAL_DOUBLE_T_SUPPORT.get();
      h.assertTrue(block.asItem() instanceof BlockItem, "one separate Heavy Double-T BlockItem");
      Set<String> rotations = new HashSet<>();
      for (Direction facing : Direction.values()) for (int roll = 0; roll < 4; roll++) {
         BlockState state = heavy(facing, roll);
         Direction right = HeavyDoubleTOrientation.right(facing, roll);
         Direction depth = HeavyDoubleTOrientation.depth(facing, roll);
         h.assertValueEqual(HeavyDoubleTOrientation.cross(right, facing), depth,
            "proper rotation keeps the local right-hand frame");
         rotations.add(facing + ":" + right + ":" + depth);
         BlockState restored = state.rotate(Rotation.CLOCKWISE_90).rotate(Rotation.COUNTERCLOCKWISE_90);
         h.assertValueEqual(restored.getValue(HeavyDoubleTSupportBlock.FACING), facing,
            "inverse rotation restores facing");
         h.assertValueEqual(restored.getValue(HeavyDoubleTSupportBlock.ROLL), roll,
            "inverse rotation restores roll");
      }
      h.assertValueEqual(rotations.size(), 24, "all six placement faces and four rolls are distinct");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void outline_and_collision_match_the_i_profile(GameTestHelper h) {
      var level = h.getLevel();
      var pos = h.absolutePos(CENTER);
      var local = Shapes.or(
         Block.box(2, 0, 2, 14, 16, 4),
         Block.box(7, 0, 4, 9, 16, 12),
         Block.box(2, 0, 12, 14, 16, 14)
      );
      for (Direction facing : Direction.values()) for (int roll = 0; roll < 4; roll++) {
         BlockState state = heavy(facing, roll);
         var expected = HeavyDoubleTOrientation.shape(local, facing, roll);
         String orientation = facing + "/" + roll;
         h.assertTrue(!Shapes.joinIsNotEmpty(expected, state.getCollisionShape(level, pos), BooleanOp.NOT_SAME),
            "collision matches all three rotated prisms at " + orientation);
         h.assertTrue(!Shapes.joinIsNotEmpty(expected, state.getShape(level, pos), BooleanOp.NOT_SAME),
            "outline matches all three rotated prisms at " + orientation);
      }
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void compact_fluid_schema_keeps_all_24_orientation_states(GameTestHelper h) {
      var block = ModBlocks.HEAVY_INDUSTRIAL_DOUBLE_T_SUPPORT.get();
      h.assertTrue(block instanceof NativeFluidloggedBlock, "Heavy is native-fluid compatible");
      h.assertTrue(block instanceof NativeFluidloggedBlock.CompactState, "compact schema avoids redundant fluid props");
      h.assertValueEqual(block.getStateDefinition().getPossibleStates().size(), 24 * 19,
         "24 orientations have precisely 19 compact fluid states each");
      for (Direction direction : Direction.values()) for (int roll=0; roll<4; roll++) {
         var dry = heavy(direction,roll);
         h.assertValueEqual(dry.getFluidState().isEmpty(),true,"default support stays dry");
         var water = NativeFluidloggedBlock.placement(dry,Fluids.WATER.getSource(false));
         h.assertValueEqual(water.getFluidState(),Fluids.WATER.getSource(false),
            "water source retained without changing axis or roll");
         h.assertValueEqual(water.getValue(HeavyDoubleTSupportBlock.FACING),direction,
            "water retains facing");
         h.assertValueEqual(water.getValue(HeavyDoubleTSupportBlock.ROLL),roll,
            "water retains roll");
      }
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void item_place_on_each_of_six_faces(GameTestHelper h) {
      var level = h.getLevel(); var pos = h.absolutePos(CENTER);
      var player = h.makeMockPlayer(GameType.CREATIVE);
      player.setPos(pos.getX() + 3, pos.getY() + 2, pos.getZ() + 3);
      Block block = ModBlocks.HEAVY_INDUSTRIAL_DOUBLE_T_SUPPORT.get();
      for (Direction clickedFace : Direction.values()) {
         clear(h,pos);
         BlockPos behind = pos.relative(clickedFace.getOpposite());
         level.setBlock(behind, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
         var hit = new BlockHitResult(
            Vec3.atCenterOf(behind).add(Vec3.atLowerCornerOf(clickedFace.getNormal()).scale(0.5)),
            clickedFace,behind,false);
         var stack = new ItemStack(block, 1);
         player.setItemInHand(InteractionHand.MAIN_HAND, stack);
         var context = new BlockPlaceContext(level,player,InteractionHand.MAIN_HAND,stack,hit);
         var selected = block.getStateForPlacement(context);
         h.assertTrue(selected != null,"all faces yield an orientation");
         h.assertValueEqual(selected.getValue(HeavyDoubleTSupportBlock.FACING), clickedFace,
            "surface click chooses correct structural axis");
         h.assertTrue(((BlockItem)block.asItem()).place(context).consumesAction(),
            "real item placement accepted on every face");
         h.assertTrue(level.getBlockState(pos).is(block), "one correct registered Heavy block placed");
      }
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void same_item_block_item_use_on_inherits_all_orientations_on_axial_extension(GameTestHelper h) {
      var level = h.getLevel();
      var sourcePos = h.absolutePos(CENTER);
      var player = h.makeMockPlayer(GameType.SURVIVAL);
      player.setPos(sourcePos.getX() + 3, sourcePos.getY() + 2, sourcePos.getZ() + 3);
      Block block = ModBlocks.HEAVY_INDUSTRIAL_DOUBLE_T_SUPPORT.get();
      for (Direction axis : Direction.values()) for (int roll = 0; roll < 4; roll++) {
         clear(h, sourcePos);
         level.setBlock(sourcePos, heavy(axis, roll), Block.UPDATE_ALL);
         ItemStack stack = new ItemStack(block, 2);
         var result = useBlockItem(h, sourcePos, axis, stack, player, 0, 0);
         BlockPos placedPos = sourcePos.relative(axis);
         h.assertTrue(result.consumesAction(), "real BlockItem.useOn accepts axial extension " + axis + "/" + roll);
         h.assertTrue(level.getBlockState(placedPos).is(block), "new support remains at vanilla axial destination");
         h.assertValueEqual(level.getBlockState(placedPos).getValue(HeavyDoubleTSupportBlock.FACING), axis,
            "axis sign follows clicked support " + axis + "/" + roll);
         h.assertValueEqual(level.getBlockState(placedPos).getValue(HeavyDoubleTSupportBlock.ROLL), roll,
            "roll is copied from clicked support " + axis + "/" + roll);
         h.assertValueEqual(stack.getCount(), 1, "successful survival placement consumes one item");
         h.assertValueEqual(HeavyDoubleTTopology.resolve(level, sourcePos, level.getBlockState(sourcePos)).contact(axis),
            Contact.AXIAL, "axial endpoint becomes a real compatible joint");
      }
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void side_click_inherits_orientation_without_redirecting_to_axis(GameTestHelper h) {
      var level = h.getLevel();
      var sourcePos = h.absolutePos(CENTER);
      var player = h.makeMockPlayer(GameType.SURVIVAL);
      player.setPos(sourcePos.getX() + 3, sourcePos.getY() + 2, sourcePos.getZ() + 3);
      clear(h, sourcePos);
      level.setBlock(sourcePos, heavy(Direction.EAST, 3), Block.UPDATE_ALL);
      ItemStack stack = new ItemStack(ModBlocks.HEAVY_INDUSTRIAL_DOUBLE_T_SUPPORT.get(), 2);
      // A deliberately off-centre top click would select a different roll for a fresh block.
      var result = useBlockItem(h, sourcePos, Direction.UP, stack, player, -.38, .39);
      BlockPos above = sourcePos.above();
      h.assertTrue(result.consumesAction(), "same-item side click follows ordinary BlockItem path");
      h.assertTrue(level.getBlockState(above).is(ModBlocks.HEAVY_INDUSTRIAL_DOUBLE_T_SUPPORT.get()),
         "side click keeps the vanilla above target");
      h.assertValueEqual(level.getBlockState(above).getValue(HeavyDoubleTSupportBlock.FACING), Direction.EAST,
         "off-axis click inherits signed source facing instead of hit face");
      h.assertValueEqual(level.getBlockState(above).getValue(HeavyDoubleTSupportBlock.ROLL), 3,
         "off-axis click inherits source roll instead of click quadrant");
      h.assertValueEqual(level.getBlockState(sourcePos.east()), Blocks.AIR.defaultBlockState(),
         "no redirection to the source axis");
      h.assertValueEqual(HeavyDoubleTTopology.resolve(level, sourcePos, level.getBlockState(sourcePos)).contact(Direction.UP),
         Contact.NONE, "side-by-side click is not misreported as an axial joint");
      h.assertValueEqual(stack.getCount(), 1, "successful side click consumes exactly one item");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void blocked_vertical_destination_does_not_fallback_or_consume(GameTestHelper h) {
      var level = h.getLevel();
      var sourcePos = h.absolutePos(CENTER);
      var player = h.makeMockPlayer(GameType.SURVIVAL);
      player.setPos(sourcePos.getX() + 3, sourcePos.getY() + 2, sourcePos.getZ() + 3);
      clear(h, sourcePos);
      BlockState source = heavy(Direction.SOUTH, 2);
      level.setBlock(sourcePos, source, Block.UPDATE_ALL);
      level.setBlock(sourcePos.above(), Blocks.OBSIDIAN.defaultBlockState(), Block.UPDATE_ALL);
      ItemStack stack = new ItemStack(ModBlocks.HEAVY_INDUSTRIAL_DOUBLE_T_SUPPORT.get(), 2);
      var result = useBlockItem(h, sourcePos, Direction.UP, stack, player, 0, 0);
      h.assertFalse(result.consumesAction(), "blocked target rejects ordinary BlockItem placement");
      h.assertValueEqual(stack.getCount(), 2, "blocked target consumes no item");
      h.assertValueEqual(level.getBlockState(sourcePos), source, "source support is unchanged");
      h.assertValueEqual(level.getBlockState(sourcePos.above()), Blocks.OBSIDIAN.defaultBlockState(),
         "obstruction is preserved");
      h.assertValueEqual(level.getBlockState(sourcePos.east()), Blocks.AIR.defaultBlockState(),
         "blocked click has no alternate lateral placement");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void clicked_source_fluid_is_not_copied_and_destination_water_is_kept(GameTestHelper h) {
      var level = h.getLevel();
      var sourcePos = h.absolutePos(CENTER);
      var player = h.makeMockPlayer(GameType.SURVIVAL);
      player.setPos(sourcePos.getX() + 3, sourcePos.getY() + 2, sourcePos.getZ() + 3);
      var block = ModBlocks.HEAVY_INDUSTRIAL_DOUBLE_T_SUPPORT.get();
      clear(h, sourcePos);
      level.setBlock(sourcePos, heavy(Direction.EAST, 1), Block.UPDATE_ALL);
      level.setBlock(sourcePos.above(), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
      ItemStack intoWater = new ItemStack(block, 2);
      var placedInWater = useBlockItem(h, sourcePos, Direction.UP, intoWater, player, 0, 0);
      BlockState waterlogged = level.getBlockState(sourcePos.above());
      h.assertTrue(placedInWater.consumesAction(), "same-item placement into replaceable water succeeds");
      h.assertTrue(waterlogged.is(block), "water is replaced by the new support");
      h.assertValueEqual(waterlogged.getFluidState(), Fluids.WATER.getSource(false),
         "destination source water is retained");
      h.assertValueEqual(waterlogged.getValue(HeavyDoubleTSupportBlock.FACING), Direction.EAST,
         "destination water does not alter inherited axis");
      clear(h, sourcePos);
      BlockState wetSource = NativeFluidloggedBlock.placement(heavy(Direction.SOUTH, 2), Fluids.WATER.getSource(false));
      level.setBlock(sourcePos, wetSource, Block.UPDATE_ALL);
      ItemStack fromWetSource = new ItemStack(block, 2);
      var placedDry = useBlockItem(h, sourcePos, Direction.UP, fromWetSource, player, 0, 0);
      BlockState dryDestination = level.getBlockState(sourcePos.above());
      h.assertTrue(placedDry.consumesAction(), "same-item placement from a waterlogged source succeeds");
      h.assertTrue(level.getBlockState(sourcePos).getFluidState().isSource(), "source retains its own water");
      h.assertTrue(dryDestination.is(block), "upper destination receives the new support");
      h.assertTrue(dryDestination.getFluidState().isEmpty(), "source fluid is not copied into dry destination");
      h.assertValueEqual(dryDestination.getValue(HeavyDoubleTSupportBlock.FACING), Direction.SOUTH,
         "wet source orientation is inherited independently of its fluid");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void other_block_item_keeps_native_placement_behavior_on_heavy(GameTestHelper h) {
      var level = h.getLevel();
      var sourcePos = h.absolutePos(CENTER);
      var player = h.makeMockPlayer(GameType.SURVIVAL);
      player.setPos(sourcePos.getX() + 3, sourcePos.getY() + 2, sourcePos.getZ() + 3);
      clear(h, sourcePos);
      level.setBlock(sourcePos, heavy(Direction.WEST, 1), Block.UPDATE_ALL);
      ItemStack dirt = new ItemStack(Items.DIRT, 2);
      var result = useBlockItem(h, sourcePos, Direction.UP, dirt, player, 0, 0);
      h.assertTrue(result.consumesAction(), "unrelated vanilla BlockItem still places normally");
      h.assertTrue(level.getBlockState(sourcePos.above()).is(Blocks.DIRT), "unrelated block uses vanilla clicked-face destination");
      h.assertTrue(level.getBlockState(sourcePos).is(ModBlocks.HEAVY_INDUSTRIAL_DOUBLE_T_SUPPORT.get()),
         "unrelated item does not replace or mutate Heavy source");
      h.assertValueEqual(dirt.getCount(), 1, "unrelated successful placement consumes one item");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void six_face_anchors_update_from_backing(GameTestHelper h) {
      var level=h.getLevel(); var pos=h.absolutePos(CENTER);
      for (Direction side: Direction.values()) {
         clear(h,pos);
         level.setBlock(pos, heavy(Direction.UP,0), Block.UPDATE_ALL);
         level.setBlock(pos.relative(side), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
         var with = HeavyDoubleTTopology.resolve(level,pos,level.getBlockState(pos));
         h.assertValueEqual(with.contact(side),Contact.ANCHOR,"real sturdy face gains its own plate");
         level.setBlock(pos.relative(side),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
         var without=HeavyDoubleTTopology.resolve(level,pos,level.getBlockState(pos));
         h.assertValueEqual(without.contact(side),Contact.NONE,"removed backing clears anchor");
      }
      clear(h,pos);level.setBlock(pos,heavy(Direction.UP,0),Block.UPDATE_ALL);
      level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
      level.setBlock(pos.east(),Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
      var both=HeavyDoubleTTopology.resolve(level,pos,level.getBlockState(pos));
      h.assertValueEqual(both.contact(Direction.DOWN),Contact.ANCHOR,"bottom anchor independent");
      h.assertValueEqual(both.contact(Direction.EAST),Contact.ANCHOR,"wall anchor independent");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void axial_connection_never_creates_a_middle_plate(GameTestHelper h) {
      var level=h.getLevel(); var pos=h.absolutePos(CENTER);
      clear(h,pos);
      level.setBlock(pos, heavy(Direction.UP,0), Block.UPDATE_ALL);
      level.setBlock(pos.above(), heavy(Direction.DOWN,0), Block.UPDATE_ALL);
      var paired=HeavyDoubleTTopology.resolve(level,pos,level.getBlockState(pos));
      h.assertValueEqual(paired.contact(Direction.UP),Contact.AXIAL,"aligned axes connect");
      h.assertValueEqual(paired.anchor() & (1 << Direction.UP.ordinal()),0,
         "no anchor between compatible Heavy sections");
      level.setBlock(pos.above(), heavy(Direction.EAST,0), Block.UPDATE_ALL);
      var rotated=HeavyDoubleTTopology.resolve(level,pos,level.getBlockState(pos));
      h.assertValueEqual(rotated.contact(Direction.UP),Contact.NONE,"wrong axis does not fake a seam");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void legacy_beams_connect_only_where_collision_faces_meet(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);
      clear(h,pos);
      level.setBlock(pos,heavy(Direction.UP,0),Block.UPDATE_ALL);
      BlockState legacyWall=ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState()
         .setValue(VerticalDoubleTSupportBlock.FACING,Direction.NORTH)
         .setValue(VerticalDoubleTSupportBlock.WALL_HORIZONTAL,false);
      level.setBlock(pos.above(),legacyWall,Block.UPDATE_ALL);
      var touching=HeavyDoubleTTopology.resolve(level,pos,level.getBlockState(pos));
      h.assertValueEqual(touching.contact(Direction.UP),Contact.JUNCTION,
         "legacy wall-mounted profile gets a junction, never a false axial seam");
      level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
      level.setBlock(pos.east(),legacyWall,Block.UPDATE_ALL);
      var detached=HeavyDoubleTTopology.resolve(level,pos,level.getBlockState(pos));
      h.assertValueEqual(detached.contact(Direction.EAST),Contact.NONE,
         "legacy beam without shared face area does not receive a floating junction");
      level.setBlock(pos.east(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
      var old=ModBlocks.STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState()
         .setValue(SteelDoubleTBulbBlock.BULB_LIGHT, SteelDoubleTBulbBlock.Bulb.PAID);
      level.setBlock(pos.above(),old,Block.UPDATE_ALL);
      h.assertValueEqual(level.getBlockState(pos.above()).getValue(SteelDoubleTBulbBlock.BULB_LIGHT),
         SteelDoubleTBulbBlock.Bulb.PAID,"existing purchased legacy bulb is unchanged");
      h.succeed();
   }
}
