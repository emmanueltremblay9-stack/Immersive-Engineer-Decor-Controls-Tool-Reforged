package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.block.IronBulbItem;
import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import com.oblixorprime.engineersdecorreforged.block.RailingMining;
import com.oblixorprime.engineersdecorreforged.block.StairWaterFlow;
import com.oblixorprime.engineersdecorreforged.block.SteelDoubleTBulbBlock;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real registered item, placement rollback and native mining hooks; no direct attachment mutator. */
@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IronBulbAttachmentGameTests {
   private static final BlockPos P = new BlockPos(3, 4, 3);
   private IronBulbAttachmentGameTests() {}

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void actual_bulb_item_attaches_in_clicked_beam_and_preserves_every_cell_kind(GameTestHelper h) {
      ServerPlayer p = player(h, false);
      h.assertTrue(bulb() instanceof IronBulbItem, "the registered existing item owns placement");
      for (boolean ew : new boolean[]{false, true}) for (int kind = 0; kind < 5; kind++) {
         clear(h); BlockState before = cell(beam().defaultBlockState().setValue(PortedBlocks.EASTWEST, ew), kind);
         put(h, P, before); give(p, bulb(), 4);
         use(h, p, beamHit(h, p, true));
         h.assertValueEqual(h.getBlockState(P), before.setValue(SteelDoubleTBulbBlock.BULB_LIGHT, SteelDoubleTBulbBlock.Bulb.PAID), "attachment changes only its ownership flags");
         h.assertTrue(h.getBlockState(P.below()).isAir(), "never reserves or places in the adjacent lower cell");
         h.assertValueEqual(p.getMainHandItem().getCount(), 3, "one survival bulb paid once");
         h.assertValueEqual(h.getBlockState(P).getFluidState(), before.getFluidState(), "source/current fluid identity retained");
      }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void cancelled_native_attachment_restores_beam_fluid_payment_and_empty_lower_cell(GameTestHelper h) {
      ServerPlayer p = player(h, false);
      for (int kind : new int[]{0, 1, 2, 3, 4}) {
         clear(h); BlockState before = cell(beam().defaultBlockState(), kind); put(h, P, before); give(p, bulb(), 4);
         boolean[] fired = {false};
         Consumer<BlockEvent.EntityPlaceEvent> listener = e -> {
            if (e.getEntity() == p && e.getPos().equals(h.absolutePos(P))) { fired[0] = true; e.setCanceled(true); }
         };
         NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, BlockEvent.EntityPlaceEvent.class, listener);
         try {
            use(h, p, beamHit(h, p, true));
            h.assertTrue(fired[0], "native placement event covers the original clicked beam");
            h.assertValueEqual(h.getBlockState(P), before, "rollback restores full fluid/beam state");
            h.assertValueEqual(p.getMainHandItem().getCount(), 4, "rollback restores item count");
            h.assertTrue(h.getBlockState(P.below()).isAir(), "cancellation does not place elsewhere");
         } finally { NeoForge.EVENT_BUS.unregister(listener); }
      }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void occupied_attachment_and_existing_pole_reject_without_fallback_or_item_cost(GameTestHelper h) {
      ServerPlayer p = player(h, false);
      for (Block pole : List.of(ModBlocks.THIN_STEEL_POLE.get(), ModBlocks.THICK_STEEL_POLE.get())) {
         clear(h); put(h, P.below(), pole.defaultBlockState());
         BlockState before = beam().defaultBlockState().setValue(PortedBlocks.DOWNCONNECT, pole == ModBlocks.THIN_STEEL_POLE.get() ? 1 : 2);
         put(h, P, before); give(p, bulb(), 4);
         // A forged/occluded packet must also be rejected; a thick pole hides this surface.
         use(h, p, new BlockHitResult(Vec3.atLowerCornerOf(h.absolutePos(P)).add(.34, 11.0/16, .34), Direction.DOWN, h.absolutePos(P), false));
         h.assertValueEqual(h.getBlockState(P), before, "existing connector remains unchanged");
         h.assertTrue(h.getBlockState(P.below()).is(pole), "pole preserved");
         h.assertValueEqual(p.getMainHandItem().getCount(), 4, "failed mount costs nothing");
      }
      clear(h); put(h, P, beam().defaultBlockState()); give(p, bulb(), 4);
      use(h, p, beamHit(h, p, true)); BlockState mounted = h.getBlockState(P);
      use(h, p, beamHit(h, p, true));
      h.assertValueEqual(h.getBlockState(P), mounted, "second mount never toggles or pays a second time");
      h.assertValueEqual(p.getMainHandItem().getCount(), 3, "one mounted bulb owns one paid item");
      h.assertTrue(h.getBlockState(P.below()).isAir(), "occupied attachment never falls back below");
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void pole_after_bulb_is_cancelled_by_native_placement_without_overlap_or_refund(GameTestHelper h) {
      ServerPlayer p = player(h, false);
      for (Block pole : List.of(ModBlocks.THIN_STEEL_POLE.get(), ModBlocks.THICK_STEEL_POLE.get())) {
         clear(h); put(h, P, beam().defaultBlockState()); give(p, bulb(), 4); use(h, p, beamHit(h, p, true));
         BlockState before = h.getBlockState(P); give(p, pole.asItem(), 4);
         use(h, p, beamHit(h, p, true));
         h.assertValueEqual(h.getBlockState(P), before, "reverse order preserves purchased lamp and beam");
         h.assertTrue(h.getBlockState(P.below()).isAir(), "overlapping connector placement rolls back");
         h.assertValueEqual(p.getMainHandItem().getCount(), 4, "rejected pole costs nothing");
         h.assertValueEqual(drops(h, bulb()), 0, "no automatic refund/duplication during rollback");
      }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void lamp_mining_retains_beam_and_fluid_returns_once_and_requires_retarget_for_beam(GameTestHelper h) {
      ServerPlayer p = player(h, false);
      for (boolean ew : new boolean[]{false, true}) for (int kind : new int[]{0, 1, 2, 3, 4}) {
         clear(h); BlockState before = cell(beam().defaultBlockState().setValue(PortedBlocks.EASTWEST, ew), kind);
         put(h, P, before); give(p, bulb(), 4); use(h, p, beamHit(h, p, true)); give(p, Items.IRON_PICKAXE, 1);
         BlockHitResult hit = aim(h, p, false);
         h.assertValueEqual(selected(h, p).kind(), PortedBlocks.PartKind.BULB_LIGHT, "ray selects visible bulb rather than metal");
         start(h, p, hit); p.gameMode.destroyBlock(hit.getBlockPos());
         h.assertValueEqual(h.getBlockState(P), before, "partial mining restores original beam/cell state");
         h.assertValueEqual(drops(h, bulb()), 1, "purchased lamp returned exactly once");
         h.assertValueEqual(drops(h, beam().asItem()), 0, "no beam drop from lamp mining");
         p.gameMode.destroyBlock(hit.getBlockPos());
         h.assertValueEqual(h.getBlockState(P), before, "a stale completion cannot remove the exposed beam");
         h.assertValueEqual(drops(h, bulb()), 1, "stale completion cannot refund twice");
         hit = aim(h, p, true); start(h, p, hit); p.gameMode.destroyBlock(hit.getBlockPos());
         h.assertTrue(!h.getBlockState(P).is(beam()), "explicit new beam START permits ordinary beam mining");
         h.assertValueEqual(drops(h, beam().asItem()), 1, "one beam mined normally");
         h.assertValueEqual(drops(h, bulb()), 1, "whole beam after dismantling cannot duplicate lamp");
      }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void whole_beam_mining_refunds_one_purchased_bulb_without_separate_removal(GameTestHelper h) {
      ServerPlayer p = player(h, false);
      for (boolean ew : new boolean[]{false, true}) {
         clear(h); put(h, P, beam().defaultBlockState().setValue(PortedBlocks.EASTWEST, ew));
         give(p, bulb(), 4); use(h, p, beamHit(h, p, true)); give(p, Items.IRON_PICKAXE, 1);
         BlockHitResult hit = aim(h, p, true);
         h.assertValueEqual(selected(h, p).kind(), PortedBlocks.PartKind.BASE, "exposed metal selects beam");
         start(h, p, hit); p.gameMode.destroyBlock(hit.getBlockPos());
         h.assertTrue(h.getBlockState(P).isAir(), "whole beam removed");
         h.assertValueEqual(drops(h, beam().asItem()), 1, "one original beam drop");
         h.assertValueEqual(drops(h, bulb()), 1, "one purchased lamp returned with beam");
      }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void creative_attachment_has_no_paid_refund_when_mined_in_survival(GameTestHelper h) {
      ServerPlayer p = player(h, true); clear(h); put(h, P, beam().defaultBlockState()); give(p, bulb(), 4);
      use(h, p, beamHit(h, p, true));
      h.assertValueEqual(p.getMainHandItem().getCount(), 4, "creative placement spends nothing");
      h.assertTrue(!SteelDoubleTBulbBlock.paidBulb(h.getBlockState(P)), "creative does not manufacture paid ownership");
      p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL); give(p, Items.IRON_PICKAXE, 1);
      BlockHitResult hit = aim(h, p, true); start(h, p, hit); p.gameMode.destroyBlock(hit.getBlockPos());
      h.assertValueEqual(drops(h, bulb()), 0, "creative-to-survival cannot generate free lamps");
      h.assertValueEqual(drops(h, beam().asItem()), 1, "beam's normal loot remains"); h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void cancelled_break_retains_attachment_and_has_no_partial_or_whole_drops(GameTestHelper h) {
      ServerPlayer p = player(h, false); clear(h); put(h, P, beam().defaultBlockState()); give(p, bulb(), 4);
      use(h, p, beamHit(h, p, true)); BlockState before = h.getBlockState(P); give(p, Items.IRON_PICKAXE, 1);
      Consumer<BlockEvent.BreakEvent> listener = e -> { if (e.getPlayer() == p) e.setCanceled(true); };
      NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, BlockEvent.BreakEvent.class, listener);
      try {
         for (boolean base : new boolean[]{false, true}) {
            BlockHitResult hit = aim(h, p, base); start(h, p, hit); p.gameMode.destroyBlock(hit.getBlockPos());
            h.assertValueEqual(h.getBlockState(P), before, "cancelled native break retains both components");
            h.assertValueEqual(drops(h, bulb()) + drops(h, beam().asItem()), 0, "cancelled break cannot pay any drop");
         }
      } finally { NeoForge.EVENT_BUS.unregister(listener); }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void changing_target_or_state_invalidates_old_lamp_completion(GameTestHelper h) {
      ServerPlayer p = player(h, false); clear(h); put(h, P, beam().defaultBlockState()); give(p, bulb(), 4);
      use(h, p, beamHit(h, p, true)); give(p, Items.IRON_PICKAXE, 1); BlockState before = h.getBlockState(P);
      BlockHitResult lamp = aim(h, p, false); start(h, p, lamp); aim(h, p, true); p.gameMode.destroyBlock(lamp.getBlockPos());
      h.assertValueEqual(h.getBlockState(P), before, "lamp START cannot finish as beam break after retarget");
      lamp = aim(h, p, false); start(h, p, lamp);
      BlockState wet = before.setValue(PortedBlocks.WATERLOGGED, true); put(h, P, wet); p.gameMode.destroyBlock(lamp.getBlockPos());
      h.assertValueEqual(h.getBlockState(P), wet, "changed fluid state invalidates old snapshot");
      h.assertValueEqual(drops(h, bulb()) + drops(h, beam().asItem()), 0, "stale targets do not pay drops"); h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void state_codec_loads_old_beams_unlit_and_retains_new_paid_attachment(GameTestHelper h) {
      for (BlockState state : beam().getStateDefinition().getPossibleStates()) {
         if (SteelDoubleTBulbBlock.hasBulb(state)) continue;
         CompoundTag old = NbtUtils.writeBlockState(state);
         old.getCompound("Properties").remove("bulb_light"); old.getCompound("Properties").remove("paid_bulb_light");
         h.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), old), state, "old palettes acquire none default without changing existing fields");
         BlockState attached = state.setValue(SteelDoubleTBulbBlock.BULB_LIGHT, SteelDoubleTBulbBlock.Bulb.PAID);
         h.assertValueEqual(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), NbtUtils.writeBlockState(attached)), attached, "new saved ownership and fluid/connection state roundtrip");
      }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void attached_bulb_touches_flange_preserves_cached_collision_and_light(GameTestHelper h) {
      for (BlockState unlit : beam().getStateDefinition().getPossibleStates()) {
         if (SteelDoubleTBulbBlock.hasBulb(unlit)) continue;
         BlockState lit = unlit.setValue(SteelDoubleTBulbBlock.BULB_LIGHT, SteelDoubleTBulbBlock.Bulb.PAID);
         var level = h.getLevel(); BlockPos pos = h.absolutePos(P);
         h.assertTrue(!Shapes.joinIsNotEmpty(unlit.getCollisionShape(level, pos), lit.getCollisionShape(level, pos), BooleanOp.NOT_SAME), "lamp must not add collision");
         h.assertTrue(lit.getShape(level, pos) == lit.getShape(level, pos), "outline is cached, independent of fluid/payment");
         h.assertTrue(SteelDoubleTBulbBlock.BULB_SHAPE.bounds().maxY == 11.0/16, "plate touches existing metal underside at11/16");
         h.assertValueEqual(lit.getLightEmission(level, pos), Math.max(14, NativeFluidloggedBlock.light(unlit)), "bulb14 never dims intrinsic lava15");
      }
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 80)
   public static void ordinary_bulb_item_six_faces_and_support_removal_keep_existing_behavior(GameTestHelper h) {
      ServerPlayer p = player(h, false);
      for (Direction face : Direction.values()) {
         clear(h); put(h, P, Blocks.STONE.defaultBlockState()); give(p, bulb(), 4);
         Vec3 hit = Vec3.atCenterOf(h.absolutePos(P)).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5));
         use(h, p, new BlockHitResult(hit, face, h.absolutePos(P), false));
         BlockPos target = P.relative(face); BlockState placed = h.getBlockState(target);
         h.assertTrue(placed.is(ModBlocks.IRON_BULB_LIGHT.get()), "ordinary solid support still places original lamp block");
         h.assertValueEqual(placed.getValue(PortedBlocks.FACING), face.getOpposite(), "ordinary six-face orientation retained");
         put(h, P, Blocks.AIR.defaultBlockState());
         h.assertValueEqual(h.getBlockState(target), placed, "standalone decorative light still survives removed support");
         h.assertValueEqual(p.getMainHandItem().getCount(), 3, "ordinary survival lamp still costs one");
      }
      h.succeed();
   }

   private static BlockState cell(BlockState state, int kind) {
      return switch (kind) {
         case 1 -> state.setValue(PortedBlocks.WATERLOGGED, true);
         case 2 -> state.setValue(NativeFluidloggedBlock.FLUID_CELL, NativeFluidloggedBlock.Cell.LAVA);
         case 3 -> state.setValue(NativeFluidloggedBlock.FLUID_CELL, NativeFluidloggedBlock.Cell.CREOSOTE);
         case 4 -> state.setValue(StairWaterFlow.WATER_FLOW, 5);
         default -> state;
      };
   }
   private static Block beam() { return ModBlocks.STEEL_DOUBLE_T_SUPPORT.get(); }
   private static Item bulb() { return ModBlocks.IRON_BULB_LIGHT.get().asItem(); }
   private static ServerPlayer player(GameTestHelper h, boolean creative) {
      ServerPlayer p = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "BulbTest"));
      p.gameMode.changeGameModeForPlayer(creative ? GameType.CREATIVE : GameType.SURVIVAL); return p;
   }
   private static void give(ServerPlayer p, Item item, int count) { p.getInventory().clearContent(); p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, count)); }
   private static void use(GameTestHelper h, ServerPlayer p, BlockHitResult hit) {
      BlockPos pos = hit.getBlockPos(); p.setPos(pos.getX()+2, pos.getY()-2, pos.getZ()+2);
      p.gameMode.useItemOn(p, h.getLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
   }
   private static BlockHitResult beamHit(GameTestHelper h, ServerPlayer p, boolean edge) {
      return aim(h, p, edge);
   }
   private static BlockHitResult aim(GameTestHelper h, ServerPlayer p, boolean base) {
      BlockPos pos = h.absolutePos(P); double d = base ? 5.5/16 : .5;
      p.setPos(pos.getX()+d, pos.getY()-.75-p.getEyeHeight(), pos.getZ()+d); p.setYRot(0); p.setXRot(-90);
      Vec3 from = p.getEyePosition();
      BlockHitResult hit = h.getLevel().clip(new ClipContext(from, from.add(0, 4, 0), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
      h.assertValueEqual(hit.getType(), HitResult.Type.BLOCK, "real eye ray must hit fixture");
      h.assertValueEqual(hit.getBlockPos(), pos, "ray must hit beam owner cell");
      h.assertValueEqual(hit.getDirection(), Direction.DOWN, "ray must contact underside"); return hit;
   }
   private static PortedBlocks.RailingPart selected(GameTestHelper h, ServerPlayer p) {
      Vec3 from = p.getEyePosition(); return PortedBlocks.selectRailingPart(h.getLevel(), h.absolutePos(P), from, from.add(0, 4, 0), p);
   }
   private static void start(GameTestHelper h, ServerPlayer p, BlockHitResult hit) {
      p.gameMode.handleBlockBreakAction(hit.getBlockPos(), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
         hit.getDirection(), h.getLevel().getMaxBuildHeight(), 0);
   }
   private static int drops(GameTestHelper h, Item item) {
      return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(P)).inflate(2))
         .stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
   }
   private static void put(GameTestHelper h, BlockPos pos, BlockState state) { h.getLevel().setBlock(h.absolutePos(pos), state, Block.UPDATE_CLIENTS); }
   private static void clear(GameTestHelper h) {
      for (BlockPos pos : BlockPos.betweenClosed(P.offset(-2,-2,-2), P.offset(2,2,2))) put(h, pos, Blocks.AIR.defaultBlockState());
      h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(P)).inflate(3)).forEach(ItemEntity::discard);
   }
}
