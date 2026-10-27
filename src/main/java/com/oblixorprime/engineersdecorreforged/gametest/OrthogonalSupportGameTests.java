package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.SteelDoubleTBulbBlock;
import com.oblixorprime.engineersdecorreforged.block.TreatedWoodBeamBlock;
import com.oblixorprime.engineersdecorreforged.block.VerticalDoubleTSupportBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OrthogonalSupportGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 3, 3);

   private static BlockState wall(boolean wood, boolean horizontal, Direction facing) {
      return wood ? ModBlocks.TREATED_WOOD_BEAM.get().defaultBlockState()
         .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
         .setValue(TreatedWoodBeamBlock.MOUNT, horizontal ? TreatedWoodBeamBlock.Mount.WALL_HORIZONTAL : TreatedWoodBeamBlock.Mount.WALL_VERTICAL)
         : ModBlocks.VERTICAL_STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState()
         .setValue(VerticalDoubleTSupportBlock.FACING, facing).setValue(VerticalDoubleTSupportBlock.WALL_HORIZONTAL, horizontal);
   }

   private static boolean joined(BlockState state) {
      if(state.getBlock() instanceof TreatedWoodBeamBlock)
         return state.getValue(TreatedWoodBeamBlock.JOIN_NEGATIVE)||state.getValue(TreatedWoodBeamBlock.JOIN_POSITIVE);
      if(state.getBlock() instanceof VerticalDoubleTSupportBlock)
         return state.getValue(VerticalDoubleTSupportBlock.JOIN_NEGATIVE)||state.getValue(VerticalDoubleTSupportBlock.JOIN_POSITIVE);
      return false;
   }

   private static void clear(GameTestHelper h, BlockPos pos) {
      for (int x=-2; x<=2; x++) for (int y=-1; y<=2; y++) for (int z=-2; z<=2; z++)
         h.getLevel().setBlock(pos.offset(x,y,z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
   }

   private static void pair(GameTestHelper h, BlockPos pos, BlockState a, BlockPos other, BlockState b, boolean reverse) {
      var level=h.getLevel();
      level.setBlock(reverse ? other : pos, reverse ? b : a, Block.UPDATE_ALL);
      level.setBlock(reverse ? pos : other, reverse ? a : b, Block.UPDATE_ALL);
      // Match placement's neighbour-state resolution, also for the block placed last.
      level.setBlock(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos), Block.UPDATE_ALL);
      level.setBlock(other, Block.updateFromNeighbourShapes(level.getBlockState(other), level, other), Block.UPDATE_ALL);
   }

   @GameTest(template="empty")
   public static void parallel_beams_do_not_grow_crossbars(GameTestHelper h) {
      var pos=h.absolutePos(CENTER);
      for (boolean wood:List.of(false,true)) for (Direction face:Direction.Plane.HORIZONTAL)
         for (boolean horizontal:List.of(false,true)) {
            clear(h,pos);
            var state=wall(wood,horizontal,face);
            var direction=horizontal ? Direction.UP : face.getClockWise();
            pair(h,pos,state,pos.relative(direction),state,false);
            h.assertFalse(joined(h.getLevel().getBlockState(pos)), "parallel wall profiles must not manufacture a ladder of crossbars");
         }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void wood_steel_corners_share_a_face_in_both_orders_and_clear_on_rotation(GameTestHelper h) {
      var level=h.getLevel(); var pos=h.absolutePos(CENTER);
      for (Direction face:Direction.Plane.HORIZONTAL) for (boolean wood:List.of(false,true))
         for (boolean horizontal:List.of(false,true)) for (boolean negative:List.of(false,true)) for (boolean reverse:List.of(false,true)) {
            clear(h,pos);
            var direction=horizontal ? (wood ? Direction.UP : Direction.DOWN) : face.getClockWise();
            if (negative) direction=direction.getOpposite();
            var other=pos.relative(direction); var a=wall(wood,horizontal,face); var b=wall(!wood,!horizontal,face);
            pair(h,pos,a,other,b,reverse);
            var joined=level.getBlockState(pos);
            h.assertTrue(joined(joined), "orthogonal wood/steel profiles in the same wall plane join in either order");
            var first=joined.getCollisionShape(level,pos).getFaceShape(direction);
            var second=level.getBlockState(other).getCollisionShape(level,other).getFaceShape(direction.getOpposite());
            h.assertTrue(Shapes.joinIsNotEmpty(first,second,BooleanOp.AND), "material junction has physical contact at the shared cell face");
            h.assertFalse(Shapes.joinIsNotEmpty(joined.getShape(level,pos),joined.getCollisionShape(level,pos),BooleanOp.NOT_SAME), "unlit outline and collision include the same connector");
            level.setBlock(other,b.rotate(Rotation.CLOCKWISE_90),Block.UPDATE_ALL);
            h.assertFalse(joined(level.getBlockState(pos)), "rotating the neighbour out of the wall plane removes the connector");
            level.setBlock(other,Blocks.BRICKS.defaultBlockState(),Block.UPDATE_ALL);
            h.assertFalse(joined(level.getBlockState(pos)), "solid wall is not a beam");
            level.setBlock(other,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            level.setBlock(other.relative(face),b,Block.UPDATE_ALL);
            h.assertFalse(joined(level.getBlockState(pos)), "diagonal beam does not create a connector");
         }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void upright_steel_reaches_only_the_adjacent_aligned_ceiling_beam(GameTestHelper h) {
      var level=h.getLevel(); var pos=h.absolutePos(CENTER);
      for (Direction face:Direction.Plane.HORIZONTAL) for (boolean wood:List.of(false,true)) for (boolean reverse:List.of(false,true)) {
         clear(h,pos); var upright=wall(false,false,face);
         var ceiling=wood ? ModBlocks.TREATED_WOOD_BEAM.get().defaultBlockState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING,face)
            : ModBlocks.STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState()
               .setValue(PortedBlocks.EASTWEST,face.getAxis()==Direction.Axis.X)
               .setValue(SteelDoubleTBulbBlock.BULB_LIGHT,SteelDoubleTBulbBlock.Bulb.PAID);
         pair(h,pos,upright,pos.above(),ceiling,reverse);
         var shape=level.getBlockState(pos).getCollisionShape(level,pos);
         h.assertValueEqual(shape.max(Direction.Axis.Y),27./16., "eleven-pixel terminal segment reaches the ceiling profile's underside");
         if (!wood) h.assertTrue(SteelDoubleTBulbBlock.paidBulb(level.getBlockState(pos.above())), "existing paid ceiling lamp survives connection");
         level.setBlock(pos.above(),wood ? ceiling.rotate(Rotation.CLOCKWISE_90) : ceiling.setValue(PortedBlocks.EASTWEST,!ceiling.getValue(PortedBlocks.EASTWEST)),Block.UPDATE_ALL);
         h.assertValueEqual(level.getBlockState(pos).getCollisionShape(level,pos).max(Direction.Axis.Y),1., "misaligned ceiling beam is not joined");
         level.setBlock(pos.above(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
         level.setBlock(pos.above().relative(face),ceiling,Block.UPDATE_ALL);
         h.assertValueEqual(level.getBlockState(pos).getCollisionShape(level,pos).max(Direction.Axis.Y),1., "offset ceiling beam cannot bridge a diagonal gap");
      }
      h.succeed();
   }
}
