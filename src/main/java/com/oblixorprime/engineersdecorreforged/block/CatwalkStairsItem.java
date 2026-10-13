package com.oblixorprime.engineersdecorreforged.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Redirects attachment geometry while retaining vanilla/NeoForge placement transactions. */
public final class CatwalkStairsItem extends BlockItem {
   public CatwalkStairsItem(Block block, Properties properties) {
      super(block, properties);
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      CatwalkStairsPlacement.Attachment attachment = CatwalkStairsPlacement.attachment(context);
      if (attachment == null) {
         return super.useOn(context);
      }
      Level level = context.getLevel();
      BlockPos target = attachment.target();
      Player player = context.getPlayer();
      if (level.isOutsideBuildHeight(target) || !level.getWorldBorder().isWithinBounds(target)
         || player != null && (player.isSpectator()
            || !level.mayInteract(player, context.getClickedPos()) || !level.mayInteract(player, target)
            || !player.mayUseItemAt(target, context.getClickedFace(), context.getItemInHand()))) {
         return InteractionResult.FAIL;
      }
      // Do not set blocks or shrink stacks here. ItemStack.useOn surrounds this native
      // placement with snapshot rollback and EntityPlaceEvent, including the redirected cell.
      return super.place(CatwalkStairsPlacement.context(context, attachment));
   }
}
