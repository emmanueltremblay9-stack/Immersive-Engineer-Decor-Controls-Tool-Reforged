package com.oblixorprime.engineersdecorreforged.tools;

import com.oblixorprime.engineersdecorreforged.client.TrackerItemRenderer;
import com.oblixorprime.engineersdecorreforged.client.TrackerTooltipClientState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public class TrackerItem extends NonEnchantableTooltipItem {
   private static final String TAG_DIMENSION = "target_dimension";
   private static final String TAG_X = "target_x";
   private static final String TAG_Y = "target_y";
   private static final String TAG_Z = "target_z";
   private static final String TAG_ENTITY = "target_entity";
   private static final String TAG_ENTITY_NAME = "target_entity_name";
   private static final String TAG_PLAYER_POSITION = "client_player_position";

   public TrackerItem(Properties properties) {
      super("tracker", properties);
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      ItemStack stack = context.getItemInHand();
      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      }
      if (context.getLevel().isClientSide) {
         return InteractionResult.SUCCESS;
      }
      if (!checkOverwrite(stack, player)) {
         return InteractionResult.FAIL;
      }

      setTarget(stack, context.getLevel(), context.getClickedPos(), null, null);
      player.displayClientMessage(Component.translatable("item.immersive_engineer_decor_controls_tool_reforged.tracker.msg.locationset"), true);
      return InteractionResult.SUCCESS;
   }

   @Override
   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
      if (player.level().isClientSide) {
         return InteractionResult.SUCCESS;
      }
      if (!checkOverwrite(stack, player)) {
         return InteractionResult.FAIL;
      }

      setTarget(stack, target.level(), target.blockPosition(), target.getUUID(), target.getDisplayName().getString());
      player.displayClientMessage(
         Component.translatable("item.immersive_engineer_decor_controls_tool_reforged.tracker.msg.entityset", target.getDisplayName()), true
      );
      return InteractionResult.SUCCESS;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      return InteractionResultHolder.pass(player.getItemInHand(hand));
   }

   @Override
   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      if (!(entity instanceof Player player)) {
         return;
      }
      if (level.isClientSide) {
         updateClientPlayerPosition(stack, level, player);
         return;
      }
      if (!(level instanceof ServerLevel serverLevel) || (serverLevel.getGameTime() & 7L) != 0L) {
         return;
      }

      CompoundTag data = data(stack);
      if (!data.hasUUID(TAG_ENTITY)) {
         return;
      }
      Entity target = serverLevel.getEntity(data.getUUID(TAG_ENTITY));
      if (target == null || !target.isAlive()) {
         data.remove(TAG_X);
         data.remove(TAG_Y);
         data.remove(TAG_Z);
      } else {
         BlockPos position = target.blockPosition();
         data.putString(TAG_DIMENSION, target.level().dimension().location().toString());
         data.putInt(TAG_X, position.getX());
         data.putInt(TAG_Y, position.getY());
         data.putInt(TAG_Z, position.getZ());
      }
      setData(stack, data);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      if (helpRequested()) {
         super.appendHoverText(stack, context, tooltip, flag);
         return;
      }
      CompoundTag data = data(stack);
      MutableComponent target = null;
      if (data.contains(TAG_ENTITY_NAME, Tag.TAG_STRING)) {
         String entityName = data.getString(TAG_ENTITY_NAME);
         if (!entityName.isEmpty()) {
            target = Component.translatable(
               "item.immersive_engineer_decor_controls_tool_reforged.tracker.tip.target.entity",
               entityName
            );
         }
      } else if (hasCompleteTarget(data)) {
         target = Component.translatable(
            "item.immersive_engineer_decor_controls_tool_reforged.tracker.tip.target.location",
            "[" + data.getInt(TAG_X) + "," + data.getInt(TAG_Y) + "," + data.getInt(TAG_Z) + "]"
         );
      }
      if (target == null) {
         return;
      }
      Optional<BlockPos> targetPosition = targetPosition(stack);
      if (targetPosition.isPresent() && data.contains(TAG_PLAYER_POSITION, Tag.TAG_LONG)) {
         BlockPos playerPosition = BlockPos.of(data.getLong(TAG_PLAYER_POSITION));
         BlockPos destination = targetPosition.get();
         long dx = (long)playerPosition.getX() - destination.getX();
         long dy = (long)playerPosition.getY() - destination.getY();
         long dz = (long)playerPosition.getZ() - destination.getZ();
         int distance = (int)Math.sqrt((double)(dx * dx + dy * dy + dz * dz));
         if (distance > 0) {
            target.append(
               Component.translatable(
                  "item.immersive_engineer_decor_controls_tool_reforged.tracker.tip.target.distance",
                  distance
               )
            );
         }
      } else {
         Optional<ResourceLocation> dimension = targetDimension(stack);
         if (dimension.isPresent()) {
            target.append(
               Component.translatable(
                  "item.immersive_engineer_decor_controls_tool_reforged.tracker.tip.dimension." + dimensionName(dimension.get())
               )
            );
         }
      }
      tooltip.add(target);
   }

   @Override
   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(new IClientItemExtensions() {
         private TrackerItemRenderer renderer;

         @Override
         public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (this.renderer == null) {
               this.renderer = new TrackerItemRenderer();
            }
            return this.renderer;
         }
      });
   }

   public static Optional<BlockPos> targetPosition(ItemStack stack) {
      CompoundTag data = data(stack);
      if (!hasCompleteTarget(data)) {
         return Optional.empty();
      }
      return Optional.of(new BlockPos(data.getInt(TAG_X), data.getInt(TAG_Y), data.getInt(TAG_Z)));
   }

   public static Optional<ResourceLocation> targetDimension(ItemStack stack) {
      CompoundTag data = data(stack);
      return data.contains(TAG_DIMENSION, Tag.TAG_STRING)
         ? Optional.ofNullable(ResourceLocation.tryParse(data.getString(TAG_DIMENSION)))
         : Optional.empty();
   }

   private static boolean checkOverwrite(ItemStack stack, Player player) {
      CompoundTag current = data(stack);
      if (current.isEmpty()) {
         return true;
      }
      if (player.getLookAngle().y < -0.98) {
         stack.remove(DataComponents.CUSTOM_DATA);
         player.displayClientMessage(Component.translatable("item.immersive_engineer_decor_controls_tool_reforged.tracker.hint.cleared"), true);
      } else {
         player.displayClientMessage(Component.translatable("item.immersive_engineer_decor_controls_tool_reforged.tracker.hint.clearfirst"), true);
      }
      return false;
   }

   private static void setTarget(ItemStack stack, Level level, BlockPos position, UUID entityId, String entityName) {
      CompoundTag data = new CompoundTag();
      data.putString(TAG_DIMENSION, level.dimension().location().toString());
      data.putInt(TAG_X, position.getX());
      data.putInt(TAG_Y, position.getY());
      data.putInt(TAG_Z, position.getZ());
      if (entityId != null) {
         data.putUUID(TAG_ENTITY, entityId);
         data.putString(TAG_ENTITY_NAME, entityName == null ? "" : entityName);
      }
      setData(stack, data);
   }

   private static boolean hasCompleteTarget(CompoundTag data) {
      return data.contains(TAG_X, Tag.TAG_INT) && data.contains(TAG_Y, Tag.TAG_INT) && data.contains(TAG_Z, Tag.TAG_INT);
   }

   private static CompoundTag data(ItemStack stack) {
      return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
   }

   private static void setData(ItemStack stack, CompoundTag data) {
      if (data.isEmpty()) {
         stack.remove(DataComponents.CUSTOM_DATA);
      } else {
         stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
      }
   }

   private static void updateClientPlayerPosition(ItemStack stack, Level level, Player player) {
      CompoundTag data = data(stack);
      boolean sameDimension = hasCompleteTarget(data)
         && data.contains(TAG_DIMENSION, Tag.TAG_STRING)
         && level.dimension().location().toString().equals(data.getString(TAG_DIMENSION));
      if (!sameDimension) {
         if (data.contains(TAG_PLAYER_POSITION, Tag.TAG_LONG)) {
            data.remove(TAG_PLAYER_POSITION);
            setData(stack, data);
         }
         return;
      }
      long packedPosition = player.blockPosition().asLong();
      if (!data.contains(TAG_PLAYER_POSITION, Tag.TAG_LONG) || data.getLong(TAG_PLAYER_POSITION) != packedPosition) {
         data.putLong(TAG_PLAYER_POSITION, packedPosition);
         setData(stack, data);
      }
   }

   private static String dimensionName(ResourceLocation dimension) {
      if (dimension.equals(Level.OVERWORLD.location())) {
         return "overworld";
      }
      if (dimension.equals(Level.NETHER.location())) {
         return "nether";
      }
      return dimension.equals(Level.END.location()) ? "end" : "other";
   }

   private static boolean helpRequested() {
      return FMLEnvironment.dist == Dist.CLIENT && TrackerTooltipClientState.helpRequested();
   }
}
