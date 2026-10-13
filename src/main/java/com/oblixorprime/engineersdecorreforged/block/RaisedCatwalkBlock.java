package com.oblixorprime.engineersdecorreforged.block;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Top-aligned catwalk with contextual pole supports and an optional inset light. */
public final class RaisedCatwalkBlock extends Block implements SimpleWaterloggedBlock {
   // Keep the recovered numeric states and model selectors compatible with existing saves.
   public static final IntegerProperty VARIANT = PortedBlocks.VARIANT;
   public static final BooleanProperty WATERLOGGED = PortedBlocks.WATERLOGGED;
   public static final BooleanProperty PAID_LIGHT = BooleanProperty.create("paid_light");
   private static final VoxelShape DECK = box(0, 14, 0, 16, 16, 16);
   // Original Engineer's Decor shapes: Stefan Wilhelm / wile, MIT (see NOTICE.md).
   private static final VoxelShape[] SHAPES = {
      DECK,
      Shapes.or(DECK, box(5, 0, 5, 11, 15, 11)).optimize(),
      Shapes.or(DECK, box(6, 0, 6, 10, 15, 10)).optimize(),
      Shapes.or(DECK, box(0, 0, 0, 16, 2, 16), box(0, 0, 0, 1, 16, 1),
         box(15, 0, 0, 16, 16, 1), box(15, 0, 15, 16, 16, 16), box(0, 0, 15, 1, 16, 16)).optimize(),
      DECK
   };

   public RaisedCatwalkBlock(Properties properties) {
      // Match the standalone iron inset light's level, including vanilla's cached state emission.
      super(properties.noOcclusion().lightLevel(state -> state.getValue(VARIANT) == 4 ? 14 : 0));
      registerDefaultState(stateDefinition.any().setValue(VARIANT, 0)
         .setValue(WATERLOGGED, false).setValue(PAID_LIGHT, false));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(VARIANT, WATERLOGGED, PAID_LIGHT);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState placed = withPoleSupport(defaultBlockState()
         .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER), level, pos);
      if (placed.getValue(VARIANT) == 0 && context.getClickedFace() == Direction.UP
         && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
         return placed.setValue(VARIANT, 3);
      }
      return placed;
   }

   private BlockState withPoleSupport(BlockState state, BlockGetter level, BlockPos pos) {
      // Installing or changing a support must never destroy a paid light.
      if (state.getValue(VARIANT) == 4) {
         return state;
      }
      BlockState below = level.getBlockState(pos.below());
      int variant = state.getValue(VARIANT) == 3 ? 3 : 0;
      if (below.is(ModBlocks.THICK_STEEL_POLE.get()) || below.is(ModBlocks.THICK_STEEL_POLE_HEAD.get())) {
         variant = 1;
      } else if (below.is(ModBlocks.THIN_STEEL_POLE.get()) || below.is(ModBlocks.THIN_STEEL_POLE_HEAD.get())) {
         variant = 2;
      }
      return state.setValue(VARIANT, variant);
   }

   @Override
   protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                    LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      if (state.getValue(WATERLOGGED)) {
         level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }
      // Side/top updates must not silently reinterpret legacy saved variants.
      return direction == Direction.DOWN ? withPoleSupport(state, level, pos) : state;
   }

   @Override
   protected FluidState getFluidState(BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   @Override
   protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
      return true;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPES[state.getValue(VARIANT)];
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return getShape(state, level, pos, context);
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                            Player player, InteractionHand hand, BlockHitResult hit) {
      boolean extending = stack.is(asItem()) && hit.getDirection().getAxis().isVertical();
      boolean lighting = stack.is(ModBlocks.IRON_INSET_LIGHT.get().asItem()) && hit.getDirection() == Direction.DOWN
         && (state.getValue(VARIANT) == 0 || state.getValue(VARIANT) == 4);
      if (!extending && !lighting) {
         return super.useItemOn(stack, state, level, pos, player, hand, hit);
      }
      if (player.isSpectator() || !player.getAbilities().mayBuild || !level.mayInteract(player, pos)
         || !player.mayUseItemAt(pos, hit.getDirection(), stack) || !level.getWorldBorder().isWithinBounds(pos)) {
         return ItemInteractionResult.sidedSuccess(level.isClientSide);
      }
      if (level.isClientSide) {
         return ItemInteractionResult.sidedSuccess(true);
      }
      // Honor the current target and hand, even if an interaction hook changed either one.
      state = level.getBlockState(pos);
      stack = player.getItemInHand(hand);
      if (!state.is(this)) {
         return ItemInteractionResult.CONSUME;
      }
      if (extending) {
         if (!stack.is(asItem())) {
            return ItemInteractionResult.CONSUME;
         }
         Direction forward = player.getDirection();
         BlockPos target = pos.relative(forward);
         if (!level.mayInteract(player, target) || !player.mayUseItemAt(target, forward, stack)
            || !level.getWorldBorder().isWithinBounds(target)) {
            return ItemInteractionResult.CONSUME;
         }
         // Use the normal item path for obstruction/replacement, fluid state, placement hooks,
         // sound, item cost and creative handling. The virtual horizontal hit keeps the same Y.
         UseOnContext context = new UseOnContext(player, hand,
            new BlockHitResult(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(forward.getNormal()).scale(0.5)),
               forward, pos, false));
         stack.useOn(context);
         return ItemInteractionResult.CONSUME;
      }
      if (!stack.is(ModBlocks.IRON_INSET_LIGHT.get().asItem())
         || (state.getValue(VARIANT) != 0 && state.getValue(VARIANT) != 4)) {
         return ItemInteractionResult.CONSUME;
      }
      boolean removing = state.getValue(VARIANT) == 4;
      boolean creative = player.getAbilities().instabuild;
      boolean refund = removing && state.getValue(PAID_LIGHT);
      BlockState changed = withPoleSupport(state.setValue(VARIANT, removing ? 0 : 4)
         .setValue(PAID_LIGHT, !removing && !creative), level, pos);
      if (!level.setBlock(pos, changed, Block.UPDATE_ALL)) {
         return ItemInteractionResult.CONSUME;
      }
      if (!creative) {
         if (!removing) {
            stack.shrink(1);
         } else if (refund) {
            ItemStack returned = new ItemStack(ModBlocks.IRON_INSET_LIGHT.get());
            if (!player.getInventory().add(returned)) {
               player.drop(returned, false);
            }
         }
      }
      level.playSound(null, pos, removing ? SoundEvents.METAL_BREAK : SoundEvents.METAL_PLACE,
         SoundSource.BLOCKS, 1.0F, 1.0F);
      level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
      return ItemInteractionResult.CONSUME;
   }

   @Override
   protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
      List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
      // Legacy coordinate-selected and creative lights own no paid item. Respect the base
      // table's explosion survival/custom loot result, and return each purchased lamp once.
      if (state.getValue(VARIANT) == 4 && state.getValue(PAID_LIGHT)
         && drops.stream().anyMatch(stack -> stack.is(asItem()))) {
         drops.add(new ItemStack(ModBlocks.IRON_INSET_LIGHT.get()));
      }
      return drops;
   }
}
