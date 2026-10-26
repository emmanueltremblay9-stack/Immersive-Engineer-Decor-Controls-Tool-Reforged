package com.oblixorprime.engineersdecorreforged.block;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Uses the ordinary NeoForge placement transaction for an attachment in the clicked beam. */
public final class IronBulbItem extends BlockItem {
   public IronBulbItem(Block block, Properties properties) {
      super(block, properties);
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      BlockState state = context.getLevel().getBlockState(context.getClickedPos());
      if (!(state.getBlock() instanceof SteelDoubleTBulbBlock) || context.getClickedFace() != Direction.DOWN) {
         return super.useOn(context);
      }
      Level level = context.getLevel();
      Player player = context.getPlayer();
      if (level.isOutsideBuildHeight(context.getClickedPos()) || !level.getWorldBorder().isWithinBounds(context.getClickedPos())
         || player != null && (player.isSpectator() || !level.mayInteract(player, context.getClickedPos())
            || !player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), context.getItemInHand()))) {
         return InteractionResult.FAIL;
      }
      // No mutation/shrink here: ItemStack.useOn owns snapshot rollback and EntityPlaceEvent.
      return super.place(new AttachedContext(context, state));
   }

   @Override
   protected BlockState getPlacementState(BlockPlaceContext context) {
      if (!(context instanceof AttachedContext attached)) return super.getPlacementState(context);
      if (!attached.canPlace()) return null;
      Player player = context.getPlayer();
      BlockState changed = attached.original.setValue(SteelDoubleTBulbBlock.BULB_LIGHT,
         player != null && player.getAbilities().instabuild ? SteelDoubleTBulbBlock.Bulb.CREATIVE : SteelDoubleTBulbBlock.Bulb.PAID);
      return canPlace(context, changed) ? changed : null;
   }

   private static final class AttachedContext extends BlockPlaceContext {
      private final BlockState original;

      private AttachedContext(UseOnContext context, BlockState state) {
         super(context);
         original = state;
         replaceClicked = true;
      }

      @Override
      public boolean canPlace() {
         return getLevel().getBlockState(getClickedPos()) == original
            && !SteelDoubleTBulbBlock.hasBulb(original)
            && original.getValue(PortedBlocks.DOWNCONNECT) == 0
            && !SteelDoubleTBulbBlock.isPole(getLevel().getBlockState(getClickedPos().below()));
      }
   }
}
