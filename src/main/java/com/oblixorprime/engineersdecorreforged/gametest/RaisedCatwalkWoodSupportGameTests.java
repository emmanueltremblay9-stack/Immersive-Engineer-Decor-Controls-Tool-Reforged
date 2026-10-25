package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.block.RaisedCatwalkBlock;
import com.oblixorprime.engineersdecorreforged.block.StairWaterFlow;
import com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RaisedCatwalkWoodSupportGameTests {
   private static final BlockPos CENTER = new BlockPos(3, 4, 3);

   @GameTest(template="empty")
   public static void raised_catwalk_connects_after_upright_wood_support_placement(GameTestHelper h) {
      for (Direction face : Direction.values()) placeBoth(h, true, face);
      h.succeed();
   }

   @GameTest(template="empty")
   public static void upright_wood_support_connects_below_existing_raised_catwalk(GameTestHelper h) {
      for (Direction face : Direction.values()) placeBoth(h, false, face);
      h.succeed();
   }

   @GameTest(template="empty")
   public static void raised_catwalk_wood_support_tracks_anchor_removal_and_replacement(GameTestHelper h) {
      placeBoth(h, true, Direction.SOUTH);
      var level = h.getLevel();
      var pos = h.absolutePos(CENTER);
      level.setBlock(pos.below(2), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
      h.assertTrue(level.getBlockState(pos.below()).isAir(), "losing the floor removes the unsupported mount");
      assertVariant(h, 0, "loss of the actual mount disconnects the wooden head");
      level.setBlock(pos.below(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
      place(h, player(h), ModBlocks.TREATED_WOOD_POLE_SUPPORT.get(), pos.below(), Direction.UP);
      assertVariant(h, 5, "replacing the anchored mount reconnects");
      level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
      assertVariant(h, 0, "unrelated solid replacement does not create a wood head or frame");
      level.setBlock(pos.below(), ModBlocks.TREATED_WOOD_POLE.get().defaultBlockState(), Block.UPDATE_ALL);
      assertVariant(h, 5, "existing straight wood pole keeps the same head");
      level.setBlock(pos.below(), ModBlocks.THIN_STEEL_POLE.get().defaultBlockState(), Block.UPDATE_ALL);
      assertVariant(h, 2, "replacing wood with steel changes to the existing steel head");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void raised_catwalk_rejects_sideways_and_suspended_wood_supports(GameTestHelper h) {
      var level = h.getLevel();
      var pos = h.absolutePos(CENTER);
      var mount = ModBlocks.TREATED_WOOD_POLE_SUPPORT.get();
      for (Direction facing : Direction.Plane.HORIZONTAL) {
         clear(h);
         level.setBlock(pos, ModBlocks.STEEL_CATWALK_TA.get().defaultBlockState(), Block.UPDATE_ALL);
         level.setBlock(pos.below(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
         level.setBlock(pos.below().relative(facing.getOpposite()), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
         place(h, player(h), mount, pos.below(), facing);
         var sideways = level.getBlockState(pos.below());
         h.assertValueEqual(sideways.getValue(DirectionalBlock.FACING), facing, "wall placement uses the clicked direction");
         h.assertTrue(sideways.canSurvive(level, pos.below()), "negative fixture is genuinely wall-anchored");
         assertVariant(h, 0, "sideways shaft does not reach the deck above");
         // Real below-neighbor state changes exercise each horizontal rotation, with both anchors present.
         level.setBlock(pos.below(), sideways.setValue(DirectionalBlock.FACING, Direction.UP), Block.UPDATE_ALL);
         assertVariant(h, 5, "turning the shaft upward connects");
         level.setBlock(pos.below(), sideways, Block.UPDATE_ALL);
         assertVariant(h, 0, "turning it sideways disconnects");
      }
      clear(h);
      level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
      place(h, player(h), ModBlocks.STEEL_CATWALK_TA.get(), pos, Direction.UP);
      assertVariant(h, 3, "explicit top placement selects the existing frame");
      level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
      place(h, player(h), mount, pos.below(), Direction.DOWN);
      h.assertTrue(level.getBlockState(pos.below()).canSurvive(level, pos.below()), "suspended support is anchored to the frame bottom");
      assertVariant(h, 3, "suspended support must not replace its anchoring frame with a wooden head");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void raised_catwalk_wood_support_preserves_sources_currents_and_saved_state(GameTestHelper h) {
      var level = h.getLevel();
      var pos = h.absolutePos(CENTER);
      var block = ModBlocks.STEEL_CATWALK_TA.get();
      int cases = 0;
      for (String id : new String[] {"minecraft:water", "minecraft:lava", "immersiveengineering:creosote",
            "immersiveengineering:ethanol", "immersiveengineering:biodiesel", "immersiveengineering:plantoil"}) {
         var fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(id));
         h.assertTrue(fluid instanceof FlowingFluid, "required installed fluid is registered: " + id);
         var flowing = (FlowingFluid) fluid;
         for (int kind = 0; kind < 3; kind++) {
            clear(h);
            level.setBlock(pos.below(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
            var expected = kind == 0 ? flowing.getSource(false) : flowing.getFlowing(kind == 1 ? 4 : 8, kind == 2);
            if (kind == 0) {
               h.assertTrue(((NativeFluidloggedBlock) block).placeLiquid(level, pos, level.getBlockState(pos), expected),
                  "existing insertion path accepts source " + id);
            } else if (fluid == Fluids.WATER) {
               level.setBlock(pos, StairWaterFlow.withFlow(level.getBlockState(pos), expected), Block.UPDATE_ALL);
            } else {
               h.assertTrue(NativeFluidCells.place(level, pos, level.getBlockState(pos), expected), "existing cell path accepts current " + id);
            }
            var before = level.getBlockState(pos);
            int light = before.getLightEmission(level, pos);
            h.assertValueEqual(level.getFluidState(pos), expected, "fixture contains exact source/amount/falling state");
            place(h, player(h), ModBlocks.TREATED_WOOD_POLE_SUPPORT.get(), pos.below(), Direction.UP);
            var connected = before.setValue(RaisedCatwalkBlock.VARIANT, 5);
            h.assertValueEqual(level.getBlockState(pos), connected, "connection changes only the head variant");
            level.setBlock(pos, NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), NbtUtils.writeBlockState(connected)), Block.UPDATE_ALL);
            level.setBlock(pos.east(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos.east(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            h.assertValueEqual(level.getBlockState(pos), connected, "state codec round-trip and side updates retain connection");
            h.assertValueEqual(level.getFluidState(pos), expected, "connection and state round-trip retain exact fluid");
            level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            h.assertValueEqual(level.getBlockState(pos), before, "support removal restores every original state property");
            h.assertValueEqual(level.getFluidState(pos), expected, "support removal neither drains nor promotes current to source");
            h.assertValueEqual(level.getBlockState(pos).getLightEmission(level, pos), light, "fluid lighting remains unchanged");
            cases++;
         }
      }
      System.out.println("CATWALK_SUPPORT_FLUIDS cases=" + cases + " source/current/falling preserved");
      h.succeed();
   }

   @GameTest(template="empty")
   public static void raised_catwalk_wood_support_keeps_paid_and_creative_inset_light_contract(GameTestHelper h) {
      var level = h.getLevel();
      var pos = h.absolutePos(CENTER);
      var lamp = ModBlocks.IRON_INSET_LIGHT.get().asItem();
      for (GameType mode : new GameType[] {GameType.SURVIVAL, GameType.CREATIVE}) {
         for (InteractionHand hand : InteractionHand.values()) {
            clear(h);
            level.setBlock(pos.below(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos, ModBlocks.STEEL_CATWALK_TA.get().defaultBlockState()
               .setValue(RaisedCatwalkBlock.WATERLOGGED, true), Block.UPDATE_ALL);
            var owner = player(h);
            owner.gameMode.changeGameModeForPlayer(mode);
            owner.setItemInHand(hand, new ItemStack(lamp, 2));
            clickLamp(h, owner, hand);
            var lit = level.getBlockState(pos);
            h.assertValueEqual(lit.getValue(RaisedCatwalkBlock.VARIANT), 4, "actual inset-light use installs the lamp");
            h.assertValueEqual(lit.getValue(RaisedCatwalkBlock.PAID_LIGHT), mode == GameType.SURVIVAL, "only survival installation owns a lamp");
            h.assertValueEqual(ownedLamps(h, owner), mode == GameType.SURVIVAL ? 1 : 2, "installation conserves lamp items");
            var placer = player(h);
            place(h, placer, ModBlocks.TREATED_WOOD_POLE_SUPPORT.get(), pos.below(), Direction.UP);
            h.assertValueEqual(level.getBlockState(pos), lit, "new support cannot erase the installed lamp or its credit");
            level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            place(h, placer, ModBlocks.TREATED_WOOD_POLE_SUPPORT.get(), pos.below(), Direction.UP);
            h.assertValueEqual(level.getBlockState(pos), lit, "removal and replacement preserve the complete lit state");
            level.setBlock(pos, NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), NbtUtils.writeBlockState(lit)), Block.UPDATE_ALL);
            clickLamp(h, owner, hand);
            h.assertValueEqual(level.getBlockState(pos), lit.setValue(RaisedCatwalkBlock.VARIANT, 5)
               .setValue(RaisedCatwalkBlock.PAID_LIGHT, false), "manual lamp removal discovers the support and preserves water");
            h.assertValueEqual(ownedLamps(h, owner), 2, "exactly the paid lamp is refunded, with no creative duplication");
            clickLamp(h, owner, hand);
            assertVariant(h, 5, "existing wood-head variant cannot be overwritten by installing a lamp");
            h.assertValueEqual(ownedLamps(h, owner), 2, "rejected installation consumes no lamp");
         }
      }
      h.succeed();
   }

   private static void placeBoth(GameTestHelper h, boolean supportFirst, Direction face) {
      var level = h.getLevel();
      var pos = h.absolutePos(CENTER);
      clear(h);
      level.setBlock(pos.below(2), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
      if (face != Direction.UP) level.setBlock(pos.relative(face.getOpposite()), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
      else if (!supportFirst) level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
      var player = player(h);
      if (supportFirst) place(h, player, ModBlocks.TREATED_WOOD_POLE_SUPPORT.get(), pos.below(), Direction.UP);
      place(h, player, ModBlocks.STEEL_CATWALK_TA.get(), pos, face);
      if (!supportFirst) {
         level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
         place(h, player, ModBlocks.TREATED_WOOD_POLE_SUPPORT.get(), pos.below(), Direction.UP);
      }
      var support = level.getBlockState(pos.below());
      h.assertTrue(support.is(ModBlocks.TREATED_WOOD_POLE_SUPPORT.get()), "named support remains present");
      h.assertValueEqual(support.getValue(DirectionalBlock.FACING), Direction.UP, "real floor placement points support upward");
      h.assertTrue(support.canSurvive(level, pos.below()), "floor-mounted support has a genuine sturdy anchor");
      var catwalk = level.getBlockState(pos);
      h.assertTrue(catwalk.is(ModBlocks.STEEL_CATWALK_TA.get()), "named raised catwalk remains present");
      System.out.println("CATWALK_SUPPORT_REPRO order=" + (supportFirst ? "support-first" : "catwalk-first")
         + " click=" + face + " facing=up support_present=true can_survive=true variant=" + catwalk.getValue(RaisedCatwalkBlock.VARIANT));
      h.assertValueEqual(catwalk.getValue(RaisedCatwalkBlock.VARIANT), 5, "upright treated-wood support must select the existing wooden head");
      h.assertFalse(catwalk.getValue(RaisedCatwalkBlock.PAID_LIGHT), "connection cannot create lamp credit");
   }

   private static void clear(GameTestHelper h) {
      var pos = h.absolutePos(CENTER);
      h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).forEach(ItemEntity::discard);
      for (BlockPos clear : BlockPos.betweenClosed(pos.offset(-2,-2,-2), pos.offset(2,1,2)))
         h.getLevel().setBlock(clear, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
   }

   private static void assertVariant(GameTestHelper h, int expected, String message) {
      var state = h.getLevel().getBlockState(h.absolutePos(CENTER));
      h.assertTrue(state.is(ModBlocks.STEEL_CATWALK_TA.get()), "raised catwalk remains present");
      h.assertValueEqual(state.getValue(RaisedCatwalkBlock.VARIANT), expected, message);
   }

   private static void clickLamp(GameTestHelper h, ServerPlayer player, InteractionHand hand) {
      var pos = h.absolutePos(CENTER);
      player.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + 3);
      player.gameMode.useItemOn(player, h.getLevel(), player.getItemInHand(hand), hand,
         new BlockHitResult(Vec3.atCenterOf(pos).add(0, -.5, 0), Direction.DOWN, pos, false));
   }

   private static int ownedLamps(GameTestHelper h, ServerPlayer player) {
      var lamp = ModBlocks.IRON_INSET_LIGHT.get().asItem();
      int count = 0;
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
         var stack = player.getInventory().getItem(i);
         if (stack.is(lamp)) count += stack.getCount();
      }
      return count + h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(CENTER)).inflate(3),
         entity -> entity.getItem().is(lamp)).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
   }

   private static ServerPlayer player(GameTestHelper h) {
      var player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "CatwalkWoodSupport"));
      player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
      var pos = h.absolutePos(CENTER);
      player.setPos(pos.getX() + 4.5, pos.getY(), pos.getZ() + 4.5);
      return player;
   }

   private static void place(GameTestHelper h, ServerPlayer player, Block block, BlockPos pos, Direction face) {
      var clicked = pos.relative(face.getOpposite());
      var stack = new ItemStack(block, 3);
      player.setItemInHand(InteractionHand.MAIN_HAND, stack);
      var context = new BlockPlaceContext(h.getLevel(), player, InteractionHand.MAIN_HAND, stack,
         new BlockHitResult(Vec3.atCenterOf(clicked).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5)), face, clicked, false));
      h.assertValueEqual(context.getClickedPos(), pos, "item targets the intended cell");
      h.assertTrue(((BlockItem) stack.getItem()).place(context).consumesAction(), "real block-item placement succeeds");
      h.assertTrue(h.getLevel().getBlockState(pos).is(block), "correct named block is placed");
      h.assertValueEqual(stack.getCount(), 2, "survival placement consumes exactly one item");
   }
}
