package com.oblixorprime.engineersdecorreforged.stool;

import com.mojang.serialization.MapCodec;
import com.oblixorprime.engineersdecorreforged.ModEntities;
import com.oblixorprime.engineersdecorreforged.block.PortedBlocks;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Keeps the original stool's block ID and shape; seating is transient server state. */
public final class TreatedWoodStoolBlock extends PortedBlocks.StoolBlock {
   public static final MapCodec<TreatedWoodStoolBlock> CODEC = simpleCodec(TreatedWoodStoolBlock::new);

   public TreatedWoodStoolBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends Block> codec() {
      return CODEC;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      // Passing while already seated lets the normal held-food interaction run.
      if (player.isShiftKeyDown() || player.isPassenger()) {
         return InteractionResult.PASS;
      }
      if (player.isSpectator() || !player.isAlive() || player.getPose() != Pose.STANDING || !player.getAbilities().mayBuild
         || !level.mayInteract(player, pos) || !player.canInteractWithBlock(pos, 0.0)) {
         return InteractionResult.FAIL;
      }
      if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      }
      for (StoolSeatEntity existing : level.getEntitiesOfClass(StoolSeatEntity.class, new AABB(pos))) {
         if (existing.isVehicle() && !existing.isRemoved()) {
            return InteractionResult.FAIL;
         }
         existing.discard();
      }
      StoolSeatEntity seat = ModEntities.STOOL_SEAT.get().create(level);
      if (seat == null) {
         return InteractionResult.FAIL;
      }
      seat.setPos(pos.getX() + 0.5, pos.getY() + StoolSeatEntity.SEAT_HEIGHT, pos.getZ() + 0.5);
      if (!seat.hasRoomFor(player) || !level.addFreshEntity(seat)) {
         seat.discard();
         return InteractionResult.FAIL;
      }
      // Never force riding: NeoForge's mount event and other mods' vetoes stay effective.
      if (!player.startRiding(seat)) {
         seat.discard();
         return InteractionResult.FAIL;
      }
      seat.positionRider(player);
      return InteractionResult.CONSUME;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      if (context instanceof EntityCollisionContext entityContext
         && entityContext.getEntity() instanceof Player player
         && player.getVehicle() instanceof StoolSeatEntity seat && seat.blockPosition().equals(pos)) {
         return Shapes.empty();
      }
      return super.getCollisionShape(state, level, pos, context);
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean movedByPiston) {
      if (!level.isClientSide && !state.is(replacement.getBlock())) {
         level.getEntitiesOfClass(StoolSeatEntity.class, new AABB(pos)).forEach(StoolSeatEntity::discard);
      }
      super.onRemove(state, level, pos, replacement, movedByPiston);
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      super.appendHoverText(stack, context, tooltip, flag);
      tooltip.add(Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.treated_wood_stool.sitting")
         .withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.treated_wood_stool.meal_bonus")
         .withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("block.immersive_engineer_decor_controls_tool_reforged.treated_wood_stool.cake_bonus")
         .withStyle(ChatFormatting.GRAY));
   }
}
