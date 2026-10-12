package com.oblixorprime.engineersdecorreforged.block;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Click-only pairing of native-compatible accessways. No OPEN setter, saved pairing,
 * redstone forwarding, item-placement fallback, or recursive neighbor traversal.
 */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID)
public final class SynchronizedAccessways {
   // Accessed only on the logical server. References last at most the current tick.
   private static final Map<Player, Click> CLICKS = new IdentityHashMap<>();
   private static final ThreadLocal<Forward> FORWARD = new ThreadLocal<>();

   private static final ClassValue<Boolean> NATIVE_BLOCK_INTERACTION = new ClassValue<>() {
      @Override
      protected Boolean computeValue(Class<?> type) {
         try {
            Class<?> nativeUse = DoorBlock.class.isAssignableFrom(type) ? DoorBlock.class
               : FenceGateBlock.class.isAssignableFrom(type) ? FenceGateBlock.class
               : type == PortedBlocks.FenceGateSegmentBlock.class ? type : null;
            return nativeUse != null
               && methodOwner(type, "useWithoutItem", BlockState.class, Level.class, BlockPos.class, Player.class, BlockHitResult.class) == nativeUse
               && methodOwner(type, "useItemOn", ItemStack.class, BlockState.class, Level.class, BlockPos.class,
                  Player.class, InteractionHand.class, BlockHitResult.class) == BlockBehaviour.class;
         } catch (ReflectiveOperationException | SecurityException | LinkageError ignored) {
            return false;
         }
      }
   };

   private static final ClassValue<Boolean> NATIVE_ITEM_FIRST = new ClassValue<>() {
      @Override
      protected Boolean computeValue(Class<?> type) {
         try {
            return type.getMethod("onItemUseFirst", ItemStack.class, UseOnContext.class).getDeclaringClass() == IItemExtension.class;
         } catch (ReflectiveOperationException | SecurityException | LinkageError ignored) {
            return false;
         }
      }
   };

   private SynchronizedAccessways() {
   }

   private static Class<?> methodOwner(Class<?> type, String name, Class<?>... arguments) throws NoSuchMethodException {
      for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
         try {
            owner.getDeclaredMethod(name, arguments);
            return owner;
         } catch (NoSuchMethodException ignored) {
            // Protected methods are not exposed by Class.getMethod.
         }
      }
      throw new NoSuchMethodException(name);
   }

   @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
   public static void beginClick(PlayerInteractEvent.RightClickBlock event) {
      if (event.getLevel().isClientSide || FORWARD.get() != null) {
         return;
      }
      Player player = event.getEntity();
      CLICKS.remove(player); // Even an offhand or cancelled attempt invalidates the previous context.
      if (event.getHand() == InteractionHand.MAIN_HAND && !player.isSpectator()
         && accessway(event.getLevel(), event.getPos()) != null) {
         CLICKS.put(player, new Click(event));
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
   public static void recordBlockPhase(UseItemOnBlockEvent event) {
      if (event.getLevel().isClientSide) {
         return;
      }
      Forward forward = FORWARD.get();
      if (forward != null) {
         if (event.getPlayer() == forward.player && event.getPos().equals(forward.hit.getBlockPos())
            && event.getHand() == InteractionHand.MAIN_HAND) {
            if (event.getUsePhase() == UseItemOnBlockEvent.UsePhase.ITEM_BEFORE_BLOCK) {
               forward.ambiguous |= forward.itemEvent != null;
               if (forward.itemEvent == null) forward.itemEvent = event;
            } else if (event.getUsePhase() == UseItemOnBlockEvent.UsePhase.BLOCK) {
               forward.ambiguous |= forward.blockEvent != null;
               if (forward.blockEvent == null) forward.blockEvent = event;
            }
         }
         return;
      }
      Click click = CLICKS.get(event.getPlayer());
      if (click == null || event.getHand() != InteractionHand.MAIN_HAND || !event.getPos().equals(click.event.getPos())) {
         return;
      }
      if (event.getUsePhase() == UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK) {
         CLICKS.remove(event.getPlayer());
      } else if (event.getUsePhase() == UseItemOnBlockEvent.UsePhase.ITEM_BEFORE_BLOCK) {
         click.ambiguous |= click.itemEvent != null;
         if (click.itemEvent == null) click.itemEvent = event;
      } else if (event.getUsePhase() == UseItemOnBlockEvent.UsePhase.BLOCK) {
         click.ambiguous |= click.blockEvent != null;
         if (click.blockEvent == null) {
            click.blockEvent = event;
            click.before = accessway(event.getLevel(), event.getPos());
            click.tick = event.getLevel().getGameTime();
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
   public static void afterNativeOpenOrClose(VanillaGameEvent event) {
      if (FORWARD.get() != null || !(event.getCause() instanceof Player player)) {
         return;
      }
      boolean open = event.getVanillaEvent() == GameEvent.BLOCK_OPEN;
      if (!open && event.getVanillaEvent() != GameEvent.BLOCK_CLOSE) {
         return;
      }
      Click click = CLICKS.remove(player);
      Level level = event.getLevel();
      if (click == null || click.ambiguous || click.before == null || click.blockEvent == null || click.tick != level.getGameTime()
         || click.event.getLevel() != level || click.event.isCanceled() || click.event.getUseBlock() == TriState.FALSE
         || click.blockEvent.isCanceled() || click.itemEvent != null && click.itemEvent.isCanceled()
         || click.before.open() == open || player.isSpectator() || !safeItem(player.getMainHandItem())) {
         return;
      }
      Accessway source = accessway(level, BlockPos.containing(event.getEventPosition()));
      if (!sameIdentity(click.before, source) || source.open() != open || powered(level, source)) {
         return;
      }
      Accessway target = partner(level, source);
      if (target == null || target.open() == open || powered(level, target)) {
         return;
      }
      BlockPos offset = target.pos.subtract(source.pos);
      BlockHitResult originalHit = click.event.getHitVec();
      BlockPos targetHitPos = originalHit.getBlockPos().offset(offset);
      BlockHitResult targetHit = new BlockHitResult(originalHit.getLocation().add(Vec3.atLowerCornerOf(offset)),
         originalHit.getDirection(), targetHitPos, originalHit.isInside());
      if (!canUse(player, level, target, targetHit)) {
         return;
      }
      Forward forward = new Forward(player, targetHit);
      FORWARD.set(forward);
      try {
         forwardNativeInteraction(level, player, source, target, open, forward);
      } finally {
         FORWARD.remove();
      }
   }

   private static void forwardNativeInteraction(Level level, Player player, Accessway source, Accessway target, boolean open, Forward forward) {
      PlayerInteractEvent.RightClickBlock permission = CommonHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND,
         forward.hit.getBlockPos(), forward.hit);
      if (permission.isCanceled() || permission.getUseBlock() == TriState.FALSE
         || !stillValid(level, source, target, open) || !canUse(player, level, target, forward.hit)) {
         return;
      }
      ItemStack stack = player.getMainHandItem();
      if (!safeItem(stack)) {
         return;
      }
      if (permission.getUseItem() != TriState.FALSE) {
         InteractionResult first = stack.onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND, forward.hit));
         // The native wrapper posts ITEM_BEFORE_BLOCK even for an empty hand. A cancelled
         // event may return PASS, so inspecting only the returned enum would bypass a veto.
         if (forward.ambiguous || first != InteractionResult.PASS || forward.itemEvent == null || forward.itemEvent.isCanceled()) {
            return;
         }
      }
      if (!stillValid(level, source, target, open) || !canUse(player, level, target, forward.hit) || !safeItem(player.getMainHandItem())) {
         return;
      }
      boolean hasItem = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
      boolean sneakingBypassesBlock = player.isSecondaryUseActive() && hasItem
         && !(player.getMainHandItem().doesSneakBypassUse(level, forward.hit.getBlockPos(), player)
            && player.getOffhandItem().doesSneakBypassUse(level, forward.hit.getBlockPos(), player));
      if (permission.getUseBlock() != TriState.TRUE && sneakingBypassesBlock) {
         return;
      }
      BlockState targetState = level.getBlockState(forward.hit.getBlockPos());
      ItemInteractionResult itemResult = targetState.useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND, forward.hit);
      if (forward.ambiguous || forward.blockEvent == null || forward.blockEvent.isCanceled()
         || itemResult != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
         || !stillValid(level, source, target, open) || !canUse(player, level, target, forward.hit)) {
         return;
      }
      // Re-read after third-party event handlers. They may already have opened or replaced
      // a leaf. Never toggle it twice, recreate it, or reuse an obsolete BlockState.
      targetState = level.getBlockState(forward.hit.getBlockPos());
      targetState.useWithoutItem(level, player, forward.hit);
   }

   private static boolean safeItem(ItemStack stack) {
      return NATIVE_ITEM_FIRST.get(stack.getItem().getClass());
   }

   private static boolean canUse(Player player, Level level, Accessway target, BlockHitResult hit) {
      if (player.level() != level || player.isSpectator() || !player.canInteractWithBlock(hit.getBlockPos(), 1.0)
         || !level.getWorldBorder().isWithinBounds(hit.getBlockPos())) {
         return false;
      }
      for (int y = 0; y < target.height; y++) {
         BlockPos part = target.pos.above(y);
         if (!level.hasChunkAt(part) || !level.mayInteract(player, part)) {
            return false;
         }
      }
      return level.getBlockState(hit.getBlockPos()).getBlock().isEnabled(level.enabledFeatures());
   }

   private static boolean stillValid(Level level, Accessway originalSource, Accessway originalTarget, boolean open) {
      Accessway source = accessway(level, originalSource.pos);
      Accessway target = accessway(level, originalTarget.pos);
      return sameIdentity(originalSource, source) && sameIdentity(originalTarget, target)
         && source.open() == open && target.open() != open && !powered(level, source) && !powered(level, target)
         && sameIdentity(target, partner(level, source));
   }

   private static boolean powered(Level level, Accessway way) {
      for (int y = 0; y < way.height; y++) {
         BlockPos pos = way.pos.above(y);
         BlockState state = level.getBlockState(pos);
         if (state.hasProperty(BlockStateProperties.POWERED)
            && (state.getValue(BlockStateProperties.POWERED) || level.hasNeighborSignal(pos))) {
            return true;
         }
      }
      return false;
   }

   @Nullable
   private static Accessway partner(Level level, Accessway source) {
      if (source.door) {
         Direction side = source.state.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT
            ? source.facing().getClockWise() : source.facing().getCounterClockWise();
         Accessway candidate = accessway(level, source.pos.relative(side));
         return compatible(source, candidate) && source.state.getValue(DoorBlock.HINGE) != candidate.state.getValue(DoorBlock.HINGE)
            ? candidate : null;
      }
      Accessway candidate = uniqueGateNeighbor(level, source);
      return candidate != null && sameIdentity(source, uniqueGateNeighbor(level, candidate)) ? candidate : null;
   }

   @Nullable
   private static Accessway uniqueGateNeighbor(Level level, Accessway source) {
      Accessway first = accessway(level, source.pos.relative(source.facing().getClockWise()));
      Accessway second = accessway(level, source.pos.relative(source.facing().getCounterClockWise()));
      boolean firstMatches = compatible(source, first);
      boolean secondMatches = compatible(source, second);
      return firstMatches == secondMatches ? null : firstMatches ? first : second;
   }

   private static boolean compatible(Accessway source, @Nullable Accessway candidate) {
      return candidate != null && candidate.state.is(source.state.getBlock()) && candidate.height == source.height
         && candidate.pos.getY() == source.pos.getY() && candidate.door == source.door
         && (source.door ? candidate.facing() == source.facing() : candidate.facing().getAxis() == source.facing().getAxis());
   }

   private static boolean sameIdentity(@Nullable Accessway first, @Nullable Accessway second) {
      return first != null && second != null && first.pos.equals(second.pos) && compatible(first, second)
         && (!first.door || first.state.getValue(DoorBlock.HINGE) == second.state.getValue(DoorBlock.HINGE));
   }

   @Nullable
   private static Accessway accessway(Level level, BlockPos clicked) {
      if (!level.hasChunkAt(clicked)) {
         return null;
      }
      BlockState state = level.getBlockState(clicked);
      if (!NATIVE_BLOCK_INTERACTION.get(state.getBlock().getClass())) {
         return null;
      }
      if (state.getBlock() instanceof DoorBlock door) {
         if (!door.type().canOpenByHand()) {
            return null;
         }
         BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? clicked : clicked.below();
         BlockState bottom = level.getBlockState(lower);
         BlockState top = level.getBlockState(lower.above());
         if (!bottom.is(state.getBlock()) || !top.is(state.getBlock()) || bottom.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER
            || top.getValue(DoorBlock.HALF) != DoubleBlockHalf.UPPER || !bottom.equals(top.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER))) {
            return null;
         }
         return new Accessway(lower.immutable(), bottom, 2, true);
      }
      if (state.getBlock() instanceof PortedBlocks.FenceGateSegmentBlock) {
         BlockPos lower = state.getValue(PortedBlocks.SEGMENT) == 0 ? clicked : clicked.below();
         BlockState bottom = level.getBlockState(lower);
         BlockState top = level.getBlockState(lower.above());
         if (!bottom.is(state.getBlock()) || bottom.getValue(PortedBlocks.SEGMENT) != 0) {
            return null;
         }
         int height = 1;
         if (top.is(bottom.getBlock())) {
            if (top.getValue(PortedBlocks.SEGMENT) != 1 || !bottom.equals(top.setValue(PortedBlocks.SEGMENT, 0))) {
               return null;
            }
            height = 2;
         }
         return new Accessway(lower.immutable(), bottom, height, false);
      }
      return new Accessway(clicked.immutable(), state, 1, false);
   }

   @SubscribeEvent
   public static void expireClicks(ServerTickEvent.Post event) {
      CLICKS.clear();
   }

   private record Accessway(BlockPos pos, BlockState state, int height, boolean door) {
      boolean open() {
         return state.getValue(BlockStateProperties.OPEN);
      }

      Direction facing() {
         return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
      }
   }

   private static final class Click {
      private final PlayerInteractEvent.RightClickBlock event;
      private UseItemOnBlockEvent itemEvent;
      private UseItemOnBlockEvent blockEvent;
      private Accessway before;
      private long tick;
      private boolean ambiguous;

      private Click(PlayerInteractEvent.RightClickBlock event) {
         this.event = event;
      }
   }

   private static final class Forward {
      private final Player player;
      private final BlockHitResult hit;
      private UseItemOnBlockEvent itemEvent;
      private UseItemOnBlockEvent blockEvent;

      private boolean ambiguous;

      private Forward(Player player, BlockHitResult hit) {
         this.player = player;
         this.hit = hit;
      }
   }
}
