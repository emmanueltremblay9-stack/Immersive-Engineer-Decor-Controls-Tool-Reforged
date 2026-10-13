package com.oblixorprime.engineersdecorreforged.gametest;

import blusunrize.immersiveengineering.api.wires.Connection;
import blusunrize.immersiveengineering.api.wires.ConnectionPoint;
import blusunrize.immersiveengineering.api.wires.WireType;
import blusunrize.immersiveengineering.api.wires.utils.WireUtils;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RoofWireConduitGameTests {
   private static final BlockPos CENTER = new BlockPos(1, 1, 1);

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void vertical_ie_wires_pass_through_center_in_every_orientation(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      helper.assertTrue(WireUtils.preventsConnection(helper.getLevel(), pos, Blocks.STONE_STAIRS.defaultBlockState(),
         new Vec3(0.5, 0, 0.5), new Vec3(0.5, 1, 0.5)), "regression fixture: the former ordinary stair blocks this cable");
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (Half half : Half.values()) {
            for (boolean wet : new boolean[] {false, true}) {
               BlockState state = ModBlocks.DARK_SHINGLE_ROOF_WIRECONDUIT.get().defaultBlockState()
                  .setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, half)
                  .setValue(StairBlock.WATERLOGGED, wet);
               helper.getLevel().setBlock(pos, state, 2);
               helper.assertFalse(WireUtils.preventsConnection(helper.getLevel(), pos, state,
                  new Vec3(0.5, 0, 0.5), new Vec3(0.5, 1, 0.5)), "center must remain open from bottom to top");
               helper.assertFalse(WireUtils.preventsConnection(helper.getLevel(), pos, state,
                  new Vec3(0.5, 1, 0.5), new Vec3(0.5, 0, 0.5)), "reverse traversal must also remain open");
               helper.assertTrue(state.getShape(helper.getLevel(), pos).equals(state.getCollisionShape(helper.getLevel(), pos)),
                  "selection and wire collision must use the same hollow geometry");
               for (WireType type : new WireType[] {WireType.COPPER, WireType.ELECTRUM, WireType.STEEL,
                     WireType.COPPER_INSULATED, WireType.ELECTRUM_INSULATED, WireType.REDSTONE,
                     WireType.STRUCTURE_ROPE, WireType.STRUCTURE_STEEL}) {
                  Connection connection = new Connection(type, new ConnectionPoint(pos.below(), 0),
                     new ConnectionPoint(pos.above(), 0), new Vec3(0.5, 0.5, 0.5), new Vec3(0.5, 0.5, 0.5));
                  helper.assertFalse(WireUtils.findObstructingBlocks(helper.getLevel(), connection,
                     Set.of(pos.below(), pos.above())).contains(pos), "IE catenary must pass the fitting: " + type.getUniqueName());
               }
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void one_ie_wire_passes_through_multiple_stacked_conduits(GameTestHelper helper) {
      BlockPos bottom = helper.absolutePos(new BlockPos(2, 1, 2));
      for (int count : new int[] {2, 3, 5}) {
         for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (boolean wet : new boolean[] {false, true}) {
               for (int i = 0; i <= 5; i++) {
                  helper.getLevel().setBlock(bottom.above(i), Blocks.AIR.defaultBlockState(), 2);
               }
               for (int i = 0; i < count; i++) {
                  BlockState state = ModBlocks.DARK_SHINGLE_ROOF_WIRECONDUIT.get().defaultBlockState()
                     .setValue(StairBlock.FACING, (i % 2 == 0) ? facing : facing.getClockWise())
                     .setValue(StairBlock.WATERLOGGED, wet);
                  helper.getLevel().setBlock(bottom.above(i), state, 2);
               }
               for (WireType type : new WireType[] {WireType.COPPER, WireType.ELECTRUM, WireType.STEEL,
                     WireType.COPPER_INSULATED, WireType.ELECTRUM_INSULATED, WireType.REDSTONE,
                     WireType.STRUCTURE_ROPE, WireType.STRUCTURE_STEEL}) {
                  // A single wire spans the entire stack; the fittings are not endpoints or relays.
                  Connection connection = new Connection(type, new ConnectionPoint(bottom.below(), 0),
                     new ConnectionPoint(bottom.above(count), 0), new Vec3(0.5, 0.5, 0.5), new Vec3(0.5, 0.5, 0.5));
                  Set<BlockPos> ignoredEndpoints = Set.of(bottom.below(), bottom.above(count));
                  helper.assertTrue(WireUtils.findObstructingBlocks(helper.getLevel(), connection, ignoredEndpoints).isEmpty(),
                     "one " + type.getUniqueName() + " wire must traverse all " + count + " aligned conduits");
                  BlockPos middle = bottom.above(count / 2);
                  BlockState saved = helper.getLevel().getBlockState(middle);
                  helper.getLevel().setBlock(middle, Blocks.STONE.defaultBlockState(), 2);
                  helper.assertTrue(WireUtils.findObstructingBlocks(helper.getLevel(), connection, ignoredEndpoints).contains(middle),
                     "a solid obstruction inside the stack must still be detected");
                  helper.getLevel().setBlock(middle, saved, 2);
               }
            }
         }
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void asymmetric_roof_slope_follows_model_rotation(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         // Samples along the roof edge, away from the symmetric central fitting.
         Vec3 high = switch (facing) {
            case EAST -> new Vec3(0.875, 0.75, 0.0625);
            case SOUTH -> new Vec3(0.9375, 0.75, 0.875);
            case WEST -> new Vec3(0.125, 0.75, 0.9375);
            case NORTH -> new Vec3(0.0625, 0.75, 0.125);
            default -> throw new IllegalArgumentException();
         };
         Vec3 low = switch (facing) {
            case EAST -> new Vec3(0.125, 0.75, 0.0625);
            case SOUTH -> new Vec3(0.9375, 0.75, 0.125);
            case WEST -> new Vec3(0.875, 0.75, 0.9375);
            case NORTH -> new Vec3(0.0625, 0.75, 0.875);
            default -> throw new IllegalArgumentException();
         };
         BlockState state = ModBlocks.DARK_SHINGLE_ROOF_WIRECONDUIT.get().defaultBlockState()
            .setValue(StairBlock.FACING, facing);
         helper.assertTrue(WireUtils.preventsConnection(helper.getLevel(), pos, state, high, high),
            "the model's raised roof edge must remain solid for " + facing);
         helper.assertFalse(WireUtils.preventsConnection(helper.getLevel(), pos, state, low, low),
            "air above the model's low roof edge must remain empty for " + facing);
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 40)
   public static void conduit_walls_still_block_wires_and_new_placement_stays_straight(GameTestHelper helper) {
      BlockPos pos = helper.absolutePos(CENTER);
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         for (Half half : Half.values()) {
            BlockState state = ModBlocks.DARK_SHINGLE_ROOF_WIRECONDUIT.get().defaultBlockState()
               .setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, half);
            double capY = half == Half.BOTTOM ? 0.95 : 0.05;
            helper.assertTrue(WireUtils.preventsConnection(helper.getLevel(), pos, state,
               new Vec3(0, capY, 0.5), new Vec3(1, capY, 0.5)), "horizontal wire must hit the cap wall");
            helper.assertTrue(WireUtils.preventsConnection(helper.getLevel(), pos, state,
               new Vec3(0.4, 0, 0.5), new Vec3(0.4, 1, 0.5)), "off-center vertical wire must hit the cap");
         }
         var player = helper.makeMockPlayer(GameType.CREATIVE);
         player.setYRot(facing.toYRot());
         for (Direction clickedFace : Direction.values()) {
            helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            BlockPos support = pos.relative(clickedFace.getOpposite());
            helper.getLevel().setBlock(support, Blocks.STONE.defaultBlockState(), 2);
            var context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND,
               new ItemStack(ModBlocks.DARK_SHINGLE_ROOF_WIRECONDUIT.get()),
               new BlockHitResult(Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(clickedFace.getNormal()).scale(0.5)),
                  clickedFace, support, false));
            BlockState placed = ModBlocks.DARK_SHINGLE_ROOF_WIRECONDUIT.get().getStateForPlacement(context);
            helper.assertTrue(placed != null, "fitting placement state must exist");
            helper.assertValueEqual(placed.getValue(StairBlock.HALF), Half.BOTTOM, "new fitting stays upright");
            helper.assertValueEqual(placed.getValue(StairBlock.SHAPE), StairsShape.STRAIGHT, "new fitting keeps its conduit model");
            for (Direction neighbor : Direction.Plane.HORIZONTAL) {
               BlockState updated = placed.updateShape(neighbor, Blocks.STONE_STAIRS.defaultBlockState()
                  .setValue(StairBlock.FACING, facing.getClockWise()), helper.getLevel(), pos, pos.relative(neighbor));
               helper.assertValueEqual(updated.getValue(StairBlock.SHAPE), StairsShape.STRAIGHT, "neighbor must not turn fitting into an unperforated corner");
            }
            helper.getLevel().setBlock(support, Blocks.AIR.defaultBlockState(), 2);
         }
      }
      helper.succeed();
   }
}
