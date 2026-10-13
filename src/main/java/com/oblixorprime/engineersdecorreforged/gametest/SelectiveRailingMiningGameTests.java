package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Mining regressions through the native server game mode, using only pre-fix APIs. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class SelectiveRailingMiningGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 1, 3);
   private static final Direction[] EDGES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
   private static final BooleanProperty[] SIDES = {PortedBlocks.NORTH, PortedBlocks.EAST, PortedBlocks.SOUTH, PortedBlocks.WEST};

   private SelectiveRailingMiningGameTests() {
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_mining_removes_only_aimed_standalone_side_in_every_direction(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      for (int edge = 0; edge < EDGES.length; edge++) {
         put(helper, sides(railing(), 15));
         mine(helper, player, EDGES[edge]);
         assertSides(helper, railing(), 15 & ~(1 << edge), "mining one standalone side must preserve the other three");
      }
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_catwalk_mining_distinguishes_a_railing_from_the_floor(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, sides(catwalk(), 15));
      mine(helper, player, Direction.NORTH);
      assertSides(helper, catwalk(), 14, "mining the north rail must preserve the catwalk floor and other rails");
      mine(helper, player, null);
      helper.assertTrue(helper.getBlockState(CENTER).isAir(), "mining the actual catwalk floor must remove the whole catwalk");
      helper.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void native_standalone_mining_removes_four_sides_one_at_a_time_until_empty(GameTestHelper helper) {
      ServerPlayer player = player(helper);
      put(helper, sides(railing(), 15));
      int mask = 15;
      for (int edge = 0; edge < EDGES.length; edge++) {
         mine(helper, player, EDGES[edge]);
         mask &= ~(1 << edge);
         if (mask == 0) {
            helper.assertTrue(helper.getBlockState(CENTER).isAir(), "mining the final standalone railing must remove the block");
         } else {
            assertSides(helper, railing(), mask, "each completed mine must remove exactly one standalone railing");
         }
      }
      helper.succeed();
   }

   private static ServerPlayer player(GameTestHelper helper) {
      ServerPlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "RailMiningTest"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
      helper.getLevel().setBlock(helper.absolutePos(CENTER.below()), Blocks.STONE.defaultBlockState(), 2);
      return player;
   }

   private static Block catwalk() {
      return ModBlocks.STEEL_CATWALK.get();
   }

   private static Block railing() {
      return ModBlocks.STEEL_RAILING.get();
   }

   private static BlockState sides(Block block, int mask) {
      BlockState state = block.defaultBlockState();
      for (int edge = 0; edge < SIDES.length; edge++) {
         state = state.setValue(SIDES[edge], (mask & (1 << edge)) != 0);
      }
      return state;
   }

   private static void put(GameTestHelper helper, BlockState state) {
      helper.getLevel().setBlock(helper.absolutePos(CENTER), state, 2);
   }

   private static void assertSides(GameTestHelper helper, Block block, int mask, String context) {
      BlockState state = helper.getBlockState(CENTER);
      helper.assertTrue(state.is(block), context + ": expected " + block + ", got " + state);
      for (int edge = 0; edge < SIDES.length; edge++) {
         helper.assertValueEqual(state.getValue(SIDES[edge]), (mask & (1 << edge)) != 0,
            context + ": " + EDGES[edge]);
      }
   }

   /** null selects the real, unobstructed floor at the center of a catwalk. */
   private static BlockHitResult aim(GameTestHelper helper, ServerPlayer player, Direction edge) {
      BlockPos pos = helper.absolutePos(CENTER);
      double x = pos.getX() + 0.5 + (edge == null ? 0.0 : edge.getStepX() * 0.45);
      double z = pos.getZ() + 0.5 + (edge == null ? 0.0 : edge.getStepZ() * 0.45);
      player.setPos(x, pos.getY() + 2.5 - player.getEyeHeight(), z);
      player.setYRot(0.0F);
      player.setXRot(90.0F);
      Vec3 eye = player.getEyePosition();
      BlockHitResult hit = helper.getLevel().clip(new ClipContext(eye,
         eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange())),
         ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
      helper.assertValueEqual(hit.getType(), HitResult.Type.BLOCK, "the player's actual eye ray must hit a physical surface");
      helper.assertValueEqual(hit.getBlockPos(), pos, "the player's actual eye ray must target the railing or catwalk");
      helper.assertValueEqual(hit.getDirection(), Direction.UP, "the downward eye ray must hit the selected top surface");
      return hit;
   }

   private static void start(GameTestHelper helper, ServerPlayer player, BlockHitResult hit) {
      player.gameMode.handleBlockBreakAction(hit.getBlockPos(), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
         hit.getDirection(), helper.getLevel().getMaxBuildHeight(), 0);
   }

   private static void mine(GameTestHelper helper, ServerPlayer player, Direction edge) {
      BlockState before = helper.getBlockState(CENTER);
      start(helper, player, aim(helper, player, edge));
      helper.assertValueEqual(helper.getBlockState(CENTER), before,
         "an ordinary survival START packet must not instantly remove a railing or floor");
      // Completion enters the vanilla late-break path: BreakEvent, tool use, onDestroyedByPlayer and loot.
      // Selective removal intentionally returns false to suppress whole-block destruction and whole-block loot.
      player.gameMode.destroyBlock(helper.absolutePos(CENTER));
   }
}
