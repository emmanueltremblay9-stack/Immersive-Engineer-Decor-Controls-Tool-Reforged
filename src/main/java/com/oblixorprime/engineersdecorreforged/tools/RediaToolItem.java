package com.oblixorprime.engineersdecorreforged.tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public class RediaToolItem extends TooltipItem {
   public static final int MAX_DAMAGE = 3000;
   private static final float MINING_SPEED = Tiers.DIAMOND.getSpeed();
   private static final List<BlockPos> HORIZONTAL_OFFSETS = List.of(
      new BlockPos(1, 0, 0),
      new BlockPos(1, 0, 1),
      new BlockPos(0, 0, 1),
      new BlockPos(-1, 0, 1),
      new BlockPos(-1, 0, 0),
      new BlockPos(-1, 0, -1),
      new BlockPos(0, 0, -1),
      new BlockPos(1, 0, -1)
   );

   public RediaToolItem(Item.Properties properties) {
      super("redia_tool", applyRediaProperties(properties));
   }

   private static Item.Properties applyRediaProperties(Item.Properties properties) {
      return properties
         .component(DataComponents.TOOL, createToolProperties())
         .attributes(DiggerItem.createAttributes(Tiers.DIAMOND, 5.0F, -3.0F));
   }

   private static Tool createToolProperties() {
      return new Tool(
         List.of(
            Tool.Rule.deniesDrops(Tiers.DIAMOND.getIncorrectBlocksForDrops()),
            Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, MINING_SPEED),
            Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_AXE, MINING_SPEED),
            Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, MINING_SPEED),
            Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_HOE, MINING_SPEED),
            Tool.Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0F),
            Tool.Rule.overrideSpeed(BlockTags.LEAVES, 15.0F),
            Tool.Rule.overrideSpeed(BlockTags.WOOL, 5.0F),
            Tool.Rule.overrideSpeed(List.of(Blocks.VINE, Blocks.GLOW_LICHEN), 2.0F)
         ),
         MINING_SPEED,
         1
      );
   }

   @Override
   public boolean isFoil(ItemStack stack) {
      return false;
   }

   @Override
   public int getEnchantmentValue() {
      return Tiers.DIAMOND.getEnchantmentValue();
   }

   @Override
   public int getMaxDamage(ItemStack stack) {
      return EngineerToolsConfig.rediaToolDurability();
   }

   @Override
   public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
      return true;
   }

   @Override
   public float getDestroySpeed(ItemStack stack, BlockState state) {
      return MINING_SPEED;
   }

   @Override
   public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
      return true;
   }

   @Override
   public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
      return true;
   }

   @Override
   public boolean isValidRepairItem(ItemStack toRepair, ItemStack repairCandidate) {
      return repairCandidate.is(Items.DIAMOND) || super.isValidRepairItem(toRepair, repairCandidate);
   }

   @Override
   public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
      return isRediaSupportedEnchantment(enchantment) || super.supportsEnchantment(stack, enchantment);
   }

   @Override
   public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
      if (enchantment.is(Enchantments.EFFICIENCY) || enchantment.is(Enchantments.FORTUNE)) {
         return false;
      }

      return isRediaSupportedEnchantment(enchantment) || super.isPrimaryItemFor(stack, enchantment);
   }

   private static boolean isRediaSupportedEnchantment(Holder<Enchantment> enchantment) {
      return enchantment.is(Enchantments.SHARPNESS)
         || enchantment.is(Enchantments.KNOCKBACK)
         || enchantment.is(Enchantments.FIRE_ASPECT)
         || enchantment.is(Enchantments.LOOTING)
         || enchantment.is(Enchantments.UNBREAKING)
         || enchantment.is(Enchantments.MENDING)
         || enchantment.is(Enchantments.EFFICIENCY)
         || enchantment.is(Enchantments.FORTUNE)
         || enchantment.is(Enchantments.SILK_TOUCH);
   }

   @Override
   public void onCraftedBy(ItemStack stack, Level level, Player player) {
      super.onCraftedBy(stack, level, player);
      applyInitialCraftingDamage(stack, EngineerToolsConfig.rediaInitialDurabilityPercent());
   }

   private static void applyInitialCraftingDamage(ItemStack stack, int durabilityPercent) {
      boolean hasNonDamageCraftingData = stack.getComponentsPatch()
         .entrySet()
         .stream()
         .anyMatch(entry -> entry.getKey() != DataComponents.DAMAGE);
      if (stack.getDamageValue() == 0 && !hasNonDamageCraftingData) {
         stack.setDamageValue(absoluteDamage(stack, durabilityPercent));
      }
   }

   @Override
   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      return true;
   }

   @Override
   public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      stack.hurtAndBreak(2, attacker, EquipmentSlot.MAINHAND);
   }

   @Override
   public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
      if (!EngineerToolsConfig.withSafeAttacking()) {
         return false;
      }
      if (entity instanceof Villager) {
         return true;
      }

      if (entity instanceof TamableAnimal tameable && tameable.isTame() && tameable.isOwnedBy(player)) {
         return true;
      }

      return entity instanceof ZombifiedPiglin piglin && piglin.getTarget() == null;
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      }

      if (player.isShiftKeyDown()) {
         InteractionResult result = tryPlantSnipping(context, player);
         if (result != InteractionResult.PASS) {
            return result;
         }

         if (context.getClickedFace() == Direction.UP) {
            result = tryDigOver(context, player);
            if (result != InteractionResult.PASS) {
               return result;
            }
            return InteractionResult.PASS;
         } else if (context.getClickedFace().getAxis().isHorizontal()) {
            result = tryTorchPlacing(context, player);
            if (result != InteractionResult.PASS) {
               return result;
            }
            return InteractionResult.PASS;
         }

         return tryAxeUse(context, player);
      }

      return tryTorchPlacing(context, player);
   }

   @Override
   public boolean mineBlock(ItemStack tool, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
      if (!level.isClientSide) {
         if (state.getDestroySpeed(level, pos) > 0.5F || level.getRandom().nextDouble() > 0.67) {
            tool.hurtAndBreak(1, miningEntity, EquipmentSlot.MAINHAND);
         }

         if (miningEntity instanceof Player player && player.isShiftKeyDown()) {
            tryTreeFelling(level, state, pos, miningEntity);
         }

         decayEnchantments(tool, level);
      }

      return true;
   }

   @Override
   public InteractionResult interactLivingEntity(ItemStack tool, Player player, LivingEntity entity, InteractionHand hand) {
      if (!(entity instanceof IShearable target)) {
         return InteractionResult.PASS;
      }

      if (entity.level().isClientSide()) {
         return InteractionResult.PASS;
      }

      BlockPos pos = entity.blockPosition();
      if (target.isShearable(player, tool, entity.level(), pos)) {
         List<ItemStack> drops = target.onSheared(player, tool, entity.level(), pos);
         for (ItemStack drop : drops) {
            target.spawnShearedDrop(entity.level(), pos, drop);
         }

         tool.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
         entity.gameEvent(GameEvent.SHEAR, player);
      }

      return InteractionResult.SUCCESS;
   }

   @Override
   public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
      return ItemAbilities.DEFAULT_AXE_ACTIONS.contains(itemAbility)
         || ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(itemAbility)
         || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(itemAbility)
         || ItemAbilities.DEFAULT_HOE_ACTIONS.contains(itemAbility)
         || ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(itemAbility);
   }

   public static ItemStack onShapelessRecipeRepaired(ItemStack stack, int previousDamage, int repairedDamage, HolderLookup.Provider registries) {
      int enchantmentIncrease = repairedDamage == 0 ? 2 : 0;
      HolderLookup.RegistryLookup<Enchantment> enchantments = registries.lookupOrThrow(Registries.ENCHANTMENT);
      Holder<Enchantment> efficiency = enchantments.getOrThrow(Enchantments.EFFICIENCY);
      Holder<Enchantment> fortune = enchantments.getOrThrow(Enchantments.FORTUNE);
      int maxEfficiency = durabilityDependentEfficiency(stack);
      int maxFortune = durabilityDependentFortune(stack);

      EnchantmentHelper.updateEnchantments(
         stack,
         mutable -> {
            int currentEfficiency = mutable.getLevel(efficiency);
            int currentFortune = mutable.getLevel(fortune);
            int newEfficiency = Math.min(currentEfficiency + enchantmentIncrease, maxEfficiency);
            int newFortune = Math.min(currentFortune + enchantmentIncrease, maxFortune);

            if (newFortune > 0) {
               if (currentFortune > 0 || currentEfficiency >= maxEfficiency) {
                  mutable.set(fortune, newFortune);
               }
            } else {
               mutable.set(fortune, 0);
            }

            if (newEfficiency > 0) {
               if (currentEfficiency < maxEfficiency) {
                  mutable.set(efficiency, newEfficiency);
               }
            } else {
               mutable.set(efficiency, 0);
            }
         }
      );

      return stack;
   }

   private static int absoluteDamage(ItemStack stack, int durabilityPercent) {
      int clamped = Math.max(1, Math.min(100, durabilityPercent));
      return stack.getMaxDamage() * (100 - clamped) / 100;
   }

   private static double relativeDurability(ItemStack stack) {
      if (stack.getMaxDamage() <= 0) {
         return 1.0;
      }

      return Math.max(0.0, Math.min(1.0, (double)(stack.getMaxDamage() - stack.getDamageValue()) / (double)stack.getMaxDamage()));
   }

   private static int durabilityDependentEfficiency(ItemStack stack) {
      int[] curve = EngineerToolsConfig.rediaEfficiencyCurve();
      int index = Math.max(0, Math.min(curve.length - 1, (int)(relativeDurability(stack) * curve.length)));
      return curve[index];
   }

   private static int durabilityDependentFortune(ItemStack stack) {
      int[] curve = EngineerToolsConfig.rediaFortuneCurve();
      int index = Math.max(0, Math.min(curve.length - 1, (int)(relativeDurability(stack) * curve.length)));
      return curve[index];
   }

   private static void decayEnchantments(ItemStack stack, Level level) {
      if (level.getRandom().nextDouble() > 0.17) {
         return;
      }

      HolderLookup.RegistryLookup<Enchantment> enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
      Holder<Enchantment> efficiency = enchantments.getOrThrow(Enchantments.EFFICIENCY);
      Holder<Enchantment> fortune = enchantments.getOrThrow(Enchantments.FORTUNE);
      int maxEfficiency = durabilityDependentEfficiency(stack);
      int maxFortune = durabilityDependentFortune(stack);

      EnchantmentHelper.updateEnchantments(
         stack,
         mutable -> {
            int currentFortune = mutable.getLevel(fortune);
            if (currentFortune > maxFortune) {
               mutable.set(fortune, maxFortune);
            }

            int currentEfficiency = mutable.getLevel(efficiency);
            if (currentEfficiency > maxEfficiency) {
               mutable.set(efficiency, maxEfficiency);
            }
         }
      );
   }

   private static InteractionResult tryPlantSnipping(UseOnContext context, Player player) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState state = level.getBlockState(pos);
      Block block = state.getBlock();
      boolean shearableBlock = state.is(BlockTags.LEAVES)
         || state.is(BlockTags.WOOL)
         || state.is(Blocks.COBWEB)
         || state.is(Blocks.SHORT_GRASS)
         || state.is(Blocks.TALL_GRASS)
         || state.is(Blocks.FERN)
         || state.is(Blocks.LARGE_FERN)
         || state.is(Blocks.DEAD_BUSH)
         || state.is(Blocks.HANGING_ROOTS)
         || state.is(Blocks.VINE)
         || state.is(Blocks.GLOW_LICHEN)
         || state.is(Blocks.TRIPWIRE);
      if (!shearableBlock) {
         return InteractionResult.PASS;
      }

      if (!level.isClientSide) {
         Item item = block.asItem();
         if (item != Items.AIR) {
            ItemEntity dropped = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(item));
            dropped.setDefaultPickUpDelay();
            level.addFreshEntity(dropped);
         }

         level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
         context.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
         level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.8F, 1.1F);
         level.gameEvent(GameEvent.SHEAR, pos, GameEvent.Context.of(player));
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   private static InteractionResult tryTorchPlacing(UseOnContext context, Player player) {
      ItemStack torch = findTorch(player.getInventory());
      if (torch.isEmpty()) {
         return InteractionResult.PASS;
      }

      ItemStack tool = player.getItemInHand(context.getHand());
      player.setItemInHand(context.getHand(), torch);
      try {
         BlockHitResult hit = new BlockHitResult(
            context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside()
         );
         return torch.getItem().useOn(new UseOnContext(player, context.getHand(), hit));
      } finally {
         player.setItemInHand(context.getHand(), tool);
      }
   }

   private static InteractionResult tryDigOver(UseOnContext context, Player player) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      if (level.getBlockEntity(pos) != null) {
         return InteractionResult.PASS;
      }

      BlockState state = level.getBlockState(pos);
      BlockState replacement = null;
      if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)) {
         replacement = Blocks.FARMLAND.defaultBlockState();
      } else if (state.is(Blocks.FARMLAND)) {
         replacement = Blocks.COARSE_DIRT.defaultBlockState();
      } else if (state.is(Blocks.COARSE_DIRT)) {
         replacement = Blocks.DIRT_PATH.defaultBlockState();
      } else if (state.is(Blocks.DIRT_PATH)) {
         replacement = Blocks.DIRT.defaultBlockState();
      }

      if (replacement == null) {
         return InteractionResult.PASS;
      }
      level.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 0.8F, 1.1F);
      if (!level.isClientSide) {
         level.setBlock(pos, replacement, 3);
         context.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
         level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, replacement));
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   private static InteractionResult tryAxeUse(UseOnContext context, Player player) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState state = level.getBlockState(pos);
      Optional<BlockState> modified = Optional.ofNullable(state.getToolModifiedState(context, ItemAbilities.AXE_STRIP, false));
      SoundEvent sound = SoundEvents.AXE_STRIP;
      int levelEvent = 0;

      if (modified.isEmpty()) {
         modified = Optional.ofNullable(state.getToolModifiedState(context, ItemAbilities.AXE_SCRAPE, false));
         sound = SoundEvents.AXE_SCRAPE;
         levelEvent = 3005;
      }

      if (modified.isEmpty()) {
         modified = Optional.ofNullable(state.getToolModifiedState(context, ItemAbilities.AXE_WAX_OFF, false));
         sound = SoundEvents.AXE_WAX_OFF;
         levelEvent = 3004;
      }

      if (modified.isEmpty()) {
         return InteractionResult.PASS;
      }

      ItemStack itemstack = context.getItemInHand();
      if (player instanceof ServerPlayer serverPlayer) {
         CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, itemstack);
      }

      level.playSound(player, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
      if (levelEvent != 0) {
         level.levelEvent(player, levelEvent, pos, 0);
      }

      if (!level.isClientSide) {
         BlockState replacement = modified.get();
         level.setBlock(pos, copyAxis(state, replacement), 11);
         level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, replacement));
         itemstack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   private static BlockState copyAxis(BlockState original, BlockState replacement) {
      return original.hasProperty(RotatedPillarBlock.AXIS) && replacement.hasProperty(RotatedPillarBlock.AXIS)
         ? replacement.setValue(RotatedPillarBlock.AXIS, original.getValue(RotatedPillarBlock.AXIS))
         : replacement;
   }

   private static ItemStack findTorch(Inventory inventory) {
      for (int i = 0; i < inventory.getContainerSize(); i++) {
         ItemStack stack = inventory.getItem(i);
         if (stack.is(Items.TORCH)) {
            return stack;
         }
      }

      return ItemStack.EMPTY;
   }

   private boolean tryTreeFelling(Level level, BlockState state, BlockPos pos, LivingEntity player) {
      if (!state.is(BlockTags.LOGS)) {
         return false;
      }

      chopTree(level, state, pos, player);
      return true;
   }

   private List<BlockPos> findBlocksAround(Level level, BlockPos centerPos, BlockState matchingState, Set<BlockPos> checked, int recursionLeft) {
      ArrayList<BlockPos> found = new ArrayList<>();
      for (int y = -1; y <= 1; y++) {
         BlockPos layer = centerPos.offset(0, y, 0);
         for (BlockPos offset : HORIZONTAL_OFFSETS) {
            BlockPos candidate = layer.offset(offset);
            if (!checked.contains(candidate) && level.getBlockState(candidate).getBlock() == matchingState.getBlock()) {
               checked.add(candidate);
               found.add(candidate);
               if (recursionLeft > 0) {
                  found.addAll(findBlocksAround(level, candidate, matchingState, checked, recursionLeft - 1));
               }
            }
         }
      }

      return found;
   }

   private static boolean isSameLog(BlockState first, BlockState second) {
      return first.getBlock() == second.getBlock();
   }

   private static boolean isLeaves(BlockState state) {
      return state.is(BlockTags.LEAVES);
   }

   private void breakBlock(Level level, BlockPos pos, LivingEntity entity) {
      BlockState state = level.getBlockState(pos);
      if (state.isAir()) {
         return;
      }

      if (entity instanceof Player player) {
         player.causeFoodExhaustion(0.005F);
      }

      Block.dropResources(state, level, pos, null, entity, new ItemStack(this));
      level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(), 11);
   }

   private void chopTree(Level level, BlockState brokenState, BlockPos startPos, LivingEntity player) {
      ItemStack tool = player.getMainHandItem().is(this) ? player.getMainHandItem() : player.getOffhandItem();
      if (!tool.is(this)) {
         return;
      }

      Set<BlockPos> checked = new HashSet<>();
      ArrayList<BlockPos> toBreak = new ArrayList<>();
      ArrayList<BlockPos> toDecay = new ArrayList<>();
      checked.add(startPos);

      ArrayList<BlockPos> queue = new ArrayList<>();
      ArrayList<BlockPos> upqueue = new ArrayList<>();
      queue.add(startPos);
      int cutLevel = 0;
      int stepsLeft = 64;
      while (!queue.isEmpty() && --stepsLeft >= 0) {
         BlockPos current = queue.remove(0);
         BlockPos up = current.above();
         BlockState upState = level.getBlockState(up);
         if (!checked.contains(up)) {
            checked.add(up);
            if (isSameLog(upState, brokenState)) {
               upqueue.add(up);
               toBreak.add(up);
               stepsLeft = 64;
            } else if (isLeaves(upState) || level.isEmptyBlock(up) || upState.is(Blocks.VINE)) {
               if (isLeaves(upState)) {
                  toDecay.add(up);
               }

               for (BlockPos offset : HORIZONTAL_OFFSETS) {
                  BlockPos candidate = up.offset(offset);
                  if (checked.contains(candidate)) {
                     continue;
                  }

                  checked.add(candidate);
                  BlockState candidateState = level.getBlockState(candidate);
                  if (isSameLog(candidateState, brokenState)) {
                     queue.add(candidate);
                     toBreak.add(candidate);
                  } else if (isLeaves(candidateState)) {
                     toDecay.add(candidate);
                  }
               }
            }
         }

         for (BlockPos offset : HORIZONTAL_OFFSETS) {
            BlockPos candidate = current.offset(offset);
            if (checked.contains(candidate)) {
               continue;
            }

            checked.add(candidate);
            if (candidate.distSqr(new BlockPos(startPos.getX(), candidate.getY(), startPos.getZ())) > 3 + cutLevel * cutLevel) {
               continue;
            }

            BlockState candidateState = level.getBlockState(candidate);
            if (isSameLog(candidateState, brokenState)) {
               queue.add(candidate);
               toBreak.add(candidate);
            } else if (isLeaves(candidateState)) {
               toDecay.add(candidate);
            }
         }

         if (queue.isEmpty() && !upqueue.isEmpty()) {
            queue = upqueue;
            upqueue = new ArrayList<>();
            cutLevel++;
         }
      }

      for (BlockPos leafPos : List.copyOf(toDecay)) {
         toBreak.addAll(findBlocksAround(level, leafPos, brokenState, checked, 1));
      }

      if (!toDecay.isEmpty()) {
         BlockState leafType = level.getBlockState(toDecay.get(0));
         ArrayList<BlockPos> leaves = toDecay;
         toDecay = new ArrayList<>();
         for (BlockPos leafPos : leaves) {
            toDecay.add(leafPos);
            toDecay.addAll(findBlocksAround(level, leafPos, leafType, checked, 2));
         }
      }

      checked.remove(startPos);
      for (BlockPos breakPos : toBreak) {
         breakBlock(level, breakPos, player);
      }

      for (BlockPos decayPos : toDecay) {
         breakBlock(level, decayPos, player);
      }

      int damage = toBreak.size() * 6 / 5 + toDecay.size() / 10 - 1;
      if (damage < 1) {
         damage = 1;
      }

      if (player instanceof Player user) {
         user.causeFoodExhaustion(Math.clamp(damage / 8.0F, 0.5F, 20.0F));
      }
      tool.hurtAndBreak(damage, player, EquipmentSlot.MAINHAND);
   }
}
