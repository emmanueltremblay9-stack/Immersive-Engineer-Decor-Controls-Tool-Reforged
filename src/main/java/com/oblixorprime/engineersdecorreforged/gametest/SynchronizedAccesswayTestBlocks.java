package com.oblixorprime.engineersdecorreforged.gametest;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Registered only in the dedicated GameTest run, never in an ordinary client/server. */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class SynchronizedAccesswayTestBlocks {
   public static final BooleanProperty LOCKED = BooleanProperty.create("test_locked");
   public static final BooleanProperty REFUSE_ITEM = BooleanProperty.create("test_refuse_item");
   public static DoorBlock LOCKABLE_DOOR;
   public static DoorBlock INHERITED_DOOR;

   private SynchronizedAccesswayTestBlocks() {
   }

   @SubscribeEvent
   public static void registerFixtures(RegisterEvent event) {
      if (!System.getProperty("neoforge.enabledGameTestNamespaces", "").contains(EngineersDecorReforged.MOD_ID)) {
         return;
      }
      event.register(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("iedctr_pair_test", "inherited_door"), () -> {
         INHERITED_DOOR = new InheritedDoor();
         return INHERITED_DOOR;
      });
      event.register(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("iedctr_pair_test", "lockable_door"), () -> {
         LOCKABLE_DOOR = new LockableDoor();
         return LOCKABLE_DOOR;
      });
   }

   private static final class InheritedDoor extends DoorBlock {
      private InheritedDoor() {
         super(BlockSetType.OAK, BlockBehaviour.Properties.of().noOcclusion());
      }
   }

   private static final class LockableDoor extends DoorBlock {
      private LockableDoor() {
         super(BlockSetType.OAK, BlockBehaviour.Properties.of().noOcclusion());
         registerDefaultState(defaultBlockState().setValue(LOCKED, false).setValue(REFUSE_ITEM, false));
      }

      @Override
      protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
         super.createBlockStateDefinition(builder);
         builder.add(LOCKED, REFUSE_ITEM);
      }

      @Override
      protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
         Player player, InteractionHand hand, BlockHitResult hit) {
         return state.getValue(REFUSE_ITEM) ? ItemInteractionResult.FAIL : super.useItemOn(stack, state, level, pos, player, hand, hit);
      }

      @Override
      protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
         return state.getValue(LOCKED) ? InteractionResult.FAIL : super.useWithoutItem(state, level, pos, player, hit);
      }
   }
}
