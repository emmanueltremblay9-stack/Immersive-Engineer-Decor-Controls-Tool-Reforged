package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.SteelDoubleTBulbBlock;
import com.oblixorprime.engineersdecorreforged.block.TreatedWoodBeamBlock;
import com.oblixorprime.engineersdecorreforged.block.TreatedWoodBeamBlock.Mount;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TreatedWoodBeamGameTests {
   private static final BlockPos CENTER=new BlockPos(3,3,3);
   private static BlockState state(Mount mount,Direction face) {
      return ModBlocks.TREATED_WOOD_BEAM.get().defaultBlockState().setValue(TreatedWoodBeamBlock.MOUNT,mount).setValue(BlockStateProperties.HORIZONTAL_FACING,face);
   }
   private static void clear(GameTestHelper h,BlockPos pos) {
      h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);
      for(Direction face:Direction.values())h.getLevel().setBlock(pos.relative(face),Blocks.AIR.defaultBlockState(),3);
   }

   @GameTest(template="empty")
   public static void wood_beam_native_wall_click_edges_corners_centre_and_both_hands(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);
      var player=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"WoodBeamTests"));player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      player.setPos(pos.getX()+3,pos.getY()+2,pos.getZ()+3);
      double[][] points={{.5,.9},{.5,.1},{.1,.5},{.9,.5},{.5,.5},{.1,.1},{.95,.8},{.8,.95}};
      Mount[] expected={Mount.WALL_VERTICAL,Mount.WALL_VERTICAL,Mount.WALL_HORIZONTAL,Mount.WALL_HORIZONTAL,
         Mount.WALL_VERTICAL,Mount.WALL_VERTICAL,Mount.WALL_HORIZONTAL,Mount.WALL_VERTICAL};
      for(Direction face:Direction.Plane.HORIZONTAL)for(InteractionHand hand:InteractionHand.values())for(int i=0;i<points.length;i++) {
         clear(h,pos);var wall=pos.relative(face.getOpposite());level.setBlock(wall,Blocks.BRICKS.defaultBlockState(),3);
         double u=points[i][0],v=points[i][1];var hit=Vec3.atLowerCornerOf(wall).add(face.getAxis()==Direction.Axis.Z?u:.5,v,face.getAxis()==Direction.Axis.X?u:.5)
            .add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5));
         var stack=new ItemStack(ModBlocks.TREATED_WOOD_BEAM.get(),2);player.setItemInHand(hand,stack);
         h.assertTrue(stack.useOn(new UseOnContext(player,hand,new BlockHitResult(hit,face,wall,false))).consumesAction(),"native item placement succeeds");
         var placed=level.getBlockState(pos);h.assertTrue(placed.is(ModBlocks.TREATED_WOOD_BEAM.get()),"one wood item selects the requested wall orientation");
         h.assertValueEqual(placed.getValue(TreatedWoodBeamBlock.MOUNT),expected[i],"nearest edge, centre and equal-distance contract");
         h.assertValueEqual(placed.getValue(BlockStateProperties.HORIZONTAL_FACING),face,"backs the clicked wall, not player look");
         h.assertValueEqual(stack.getCount(),1,"exactly one item consumed");h.assertTrue(level.getBlockState(wall).is(Blocks.BRICKS),"supporting wall retained");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void wood_beam_rigid_profile_volume_wall_contact_and_full_length(GameTestHelper h) {
      var pos=h.absolutePos(CENTER);
      for(Mount mount:Mount.values())for(Direction face:Direction.Plane.HORIZONTAL) {
         var state=state(mount,face);var shape=state.getCollisionShape(h.getLevel(),pos);var b=shape.bounds();
         double volume=shape.toAabbs().stream().mapToDouble(a->a.getXsize()*a.getYsize()*a.getZsize()).sum();
         h.assertValueEqual(volume,396./4096,"same three source cuboids: 24.75 square pixels times sixteen, no scaling");
         if(mount==Mount.CEILING){h.assertValueEqual(b.maxY,1.,"existing horizontal ceiling contact");h.assertValueEqual(b.minY,11./16,"exact five-pixel thickness");}
         else {
            double back=switch(face){case NORTH->b.maxZ;case EAST->b.minX;case SOUTH->b.minZ;default->b.maxX;};
            h.assertValueEqual(back,face==Direction.NORTH||face==Direction.WEST?1.:0.,"back of wood profile touches wall plane exactly");
            if(mount==Mount.WALL_VERTICAL){h.assertValueEqual(b.minY,0.,"vertical starts at floor");h.assertValueEqual(b.maxY,1.,"full one-block vertical length");}
            else {h.assertValueEqual(b.minY,5.5/16,"wall horizontal centre is unchanged");h.assertValueEqual(b.maxY,10.5/16,"wall horizontal cross-section matches pole");}
         }
         var rotated=state;for(int i=0;i<4;i++)rotated=rotated.rotate(Rotation.CLOCKWISE_90);
         h.assertValueEqual(rotated,state,"four rotations preserve all properties");
         h.assertValueEqual(state.mirror(Mirror.LEFT_RIGHT).mirror(Mirror.LEFT_RIGHT),state,"mirror is reversible");
         var saved=NbtUtils.writeBlockState(state);var loaded=NbtUtils.readBlockState(h.getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK),saved);
         h.assertValueEqual(loaded,state,"state orientation survives native NBT codec");
      }
      h.succeed();
   }

   @GameTest(template="empty")
   public static void wood_beam_connections_update_and_preserve_existing_steel_paid_lamp(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);clear(h,pos);
      level.setBlock(pos,state(Mount.CEILING,Direction.NORTH),3);
      var steel=ModBlocks.STEEL_DOUBLE_T_SUPPORT.get().defaultBlockState().setValue(SteelDoubleTBulbBlock.BULB_LIGHT,SteelDoubleTBulbBlock.Bulb.PAID);
      level.setBlock(pos.east(),steel,3);
      h.assertTrue(level.getBlockState(pos).getValue(TreatedWoodBeamBlock.JOIN_POSITIVE),"wood joins neighbouring ceiling support");
      h.assertTrue(level.getBlockState(pos.east()).getValue(PortedBlocks.LEFTBEAM),"existing steel support recognises the new ceiling beam");
      h.assertTrue(SteelDoubleTBulbBlock.paidBulb(level.getBlockState(pos.east())),"paid steel lamp is retained");
      level.setBlock(pos.east(),Blocks.AIR.defaultBlockState(),3);
      h.assertFalse(level.getBlockState(pos).getValue(TreatedWoodBeamBlock.JOIN_POSITIVE),"connector removed with neighbour");
      level.setBlock(pos.below(),ModBlocks.TREATED_WOOD_POLE.get().defaultBlockState().setValue(BlockStateProperties.FACING,Direction.UP),3);
      h.assertTrue(level.getBlockState(pos).getValue(TreatedWoodBeamBlock.POLE),"vertical straight wood pole connects below");
      level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),3);
      h.assertFalse(level.getBlockState(pos).getValue(TreatedWoodBeamBlock.POLE),"pole removal restores original beam profile");
      clear(h,pos);level.setBlock(pos,state(Mount.WALL_VERTICAL,Direction.NORTH),3);
      level.setBlock(pos.east(),state(Mount.WALL_HORIZONTAL,Direction.EAST),3);
      h.assertFalse(level.getBlockState(pos).getValue(TreatedWoodBeamBlock.JOIN_POSITIVE),"different wall planes do not invent a corner piece");
      level.setBlock(pos.east(),state(Mount.WALL_HORIZONTAL,Direction.NORTH),3);
      h.assertTrue(level.getBlockState(pos).getValue(TreatedWoodBeamBlock.JOIN_POSITIVE),"same wall plane joins");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void wood_join_faces_match_and_keep_fluid_through_updates_and_mirrors(GameTestHelper h) {
      var level=h.getLevel();var pos=h.absolutePos(CENTER);
      for(Mount mount:Mount.values())for(Direction facing:Direction.Plane.HORIZONTAL) {
         clear(h,pos);var base=state(mount,facing).setValue(BlockStateProperties.WATERLOGGED,true);
         level.setBlock(pos,base,3);Direction positive=mount==Mount.WALL_HORIZONTAL?Direction.UP:facing.getClockWise();
         level.setBlock(pos.relative(positive),state(mount,facing),3);var joined=level.getBlockState(pos);
         h.assertTrue(joined.getValue(TreatedWoodBeamBlock.JOIN_POSITIVE),"one-sided joint is active");
         var first=joined.getCollisionShape(level,pos).getFaceShape(positive);
         var neighbor=level.getBlockState(pos.relative(positive));var second=neighbor.getCollisionShape(level,pos.relative(positive)).getFaceShape(positive.getOpposite());
         h.assertFalse(first.isEmpty(),"joint reaches the shared cell boundary");
         h.assertFalse(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(first,second,net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME),"joint face contours match, rather than only total area");
         h.assertTrue(level.getFluidState(pos).isSource()&&joined.getValue(BlockStateProperties.WATERLOGGED),"neighbor change preserves native source water");
         h.assertValueEqual(joined.mirror(Mirror.LEFT_RIGHT).mirror(Mirror.LEFT_RIGHT),joined,"asymmetric joint and fluid survive mirror round-trip");
         level.setBlock(pos.relative(positive),Blocks.AIR.defaultBlockState(),3);
         h.assertFalse(level.getBlockState(pos).getValue(TreatedWoodBeamBlock.JOIN_POSITIVE),"joint clears after removal");
         h.assertTrue(level.getFluidState(pos).isSource(),"removal does not drain the beam's fluid");
      }
      h.succeed();
   }
}
