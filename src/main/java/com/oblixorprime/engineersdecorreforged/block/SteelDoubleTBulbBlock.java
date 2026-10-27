package com.oblixorprime.engineersdecorreforged.block;

import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** The existing beam can own one separately selectable, purchased Iron Bulb. */
@EventBusSubscriber(modid = EngineersDecorReforged.MOD_ID)
public final class SteelDoubleTBulbBlock extends PortedBlocks.SupportBlock implements DoubleTLightHost {
   public enum Bulb implements StringRepresentable {
      NONE("none"), CREATIVE("creative"), PAID("paid");
      private final String name;
      Bulb(String name) { this.name = name; }
      @Override public String getSerializedName() { return name; }
   }
   public static final EnumProperty<Bulb> BULB_LIGHT = EnumProperty.create("bulb_light", Bulb.class);
   public static boolean hasBulb(BlockState state) { return state.getValue(BULB_LIGHT) != Bulb.NONE; }
   public static boolean paidBulb(BlockState state) { return state.getValue(BULB_LIGHT) == Bulb.PAID; }
   // Complete original bulb/cage bounds after ceiling rotation and five-pixel translation.
   public static final VoxelShape BULB_SHAPE = Block.box(6, 6.75, 6, 10, 11, 10);
   private final VoxelShape[] litOutlines = new VoxelShape[16];

   public SteelDoubleTBulbBlock(Properties properties) {
      super(properties.lightLevel(state -> Math.max(NativeFluidloggedBlock.light(state), hasBulb(state) || SupportLights.attached(state) ? 14 : 0)));
      registerDefaultState(defaultBlockState().setValue(BULB_LIGHT, Bulb.NONE).setValue(SupportLights.ATTACHED,false));
      for (int key = 0; key < litOutlines.length; key++) {
         BlockState state = defaultBlockState().setValue(PortedBlocks.EASTWEST, (key & 1) != 0)
            .setValue(PortedBlocks.LEFTBEAM, (key & 2) != 0).setValue(PortedBlocks.RIGHTBEAM, (key & 4) != 0)
            .setValue(PortedBlocks.DOWNCONNECT, (key & 8) != 0 ? 1 : 0);
         litOutlines[key] = Shapes.or(baseShape(state), BULB_SHAPE).optimize();
      }
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(BULB_LIGHT,SupportLights.ATTACHED);
   }

   @Override public VoxelShape supportShape(BlockState state,BlockGetter level,BlockPos pos){return baseShape(state);}
   @Override public VoxelShape mountingShape(BlockState state,BlockGetter level,BlockPos pos){return baseShape(state.setValue(PortedBlocks.DOWNCONNECT,0).setValue(PortedBlocks.LEFTBEAM,false).setValue(PortedBlocks.RIGHTBEAM,false));}
   private VoxelShape baseShape(BlockState state) {
      return super.getShape(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      if (!hasBulb(state)) return SupportLights.outline(state,level,pos,baseShape(state));
      int key = (state.getValue(PortedBlocks.EASTWEST) ? 1 : 0) | (state.getValue(PortedBlocks.LEFTBEAM) ? 2 : 0)
         | (state.getValue(PortedBlocks.RIGHTBEAM) ? 4 : 0) | (state.getValue(PortedBlocks.DOWNCONNECT) > 0 ? 8 : 0);
      return litOutlines[key];
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      // The old beam collision is unchanged; decorative bulbs remain collision-free.
      return baseShape(state);
   }

   @Nullable
   PortedBlocks.RailingPart selectPart(BlockState state, BlockPos pos, Vec3 start, Vec3 end) {
      BlockHitResult beam = baseShape(state).clip(start, end, pos);
      BlockHitResult bulb = hasBulb(state) ? BULB_SHAPE.clip(start, end, pos) : null;
      if (bulb != null && (beam == null || start.distanceToSqr(bulb.getLocation()) < start.distanceToSqr(beam.getLocation()) - 1.0E-9)) {
         return new PortedBlocks.RailingPart(null, BULB_SHAPE, PortedBlocks.PartKind.BULB_LIGHT);
      }
      return beam == null ? null : new PortedBlocks.RailingPart(null, baseShape(state));
   }

   @Override
   public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                      boolean willHarvest, FluidState fluid) {
      if (player == null) {
         return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
      }
      RailingMining.Completion completion = RailingMining.complete(player, level, pos, state);
      if (completion == null) return false;
      if(completion.part().kind()==PortedBlocks.PartKind.SUPPORT_LIGHT)return SupportLights.remove(state,level,pos,player,willHarvest,completion);
      if (completion.part().kind() != PortedBlocks.PartKind.BULB_LIGHT) {
         return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
      }
      BlockState changed = state.setValue(BULB_LIGHT, Bulb.NONE);
      if (level.setBlock(pos, changed, Block.UPDATE_ALL)) {
         if (level.isClientSide) {
            RailingMining.clientInsetLightRemoved(player, pos);
         } else {
            if (!player.getAbilities().instabuild && willHarvest) {
               player.awardStat(Stats.BLOCK_MINED.get(this));
               player.causeFoodExhaustion(0.005F);
               if (paidBulb(state)) {
                  Block.dropResources(ModBlocks.IRON_BULB_LIGHT.get().defaultBlockState(), level, pos, null, player, completion.tool());
               }
            }
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
         }
      }
      // Only the lamp was removed: never invoke whole-beam playerDestroy/loot.
      return false;
   }

   @Override
   protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
      List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
      // Follow the established paid inset-light contract, including tool/loot/explosion rules.
      if (hasBulb(state) && paidBulb(state)
         && drops.stream().anyMatch(stack -> stack.is(asItem()))) {
         drops.add(new ItemStack(ModBlocks.IRON_BULB_LIGHT.get()));
      }
      return SupportLights.drops(state,params,drops);
   }

   public static boolean isPole(BlockState state) {
      return state.is(ModBlocks.THIN_STEEL_POLE.get()) || state.is(ModBlocks.THICK_STEEL_POLE.get());
   }

   /** Native placement rollback rejects a connector occupying the attached bulb's space. */
   @SubscribeEvent(priority = EventPriority.LOWEST)
   public static void preventOverlappingPole(BlockEvent.EntityPlaceEvent event) {
      if (!isPole(event.getPlacedBlock())) return;
      BlockState above = event.getLevel().getBlockState(event.getPos().above());
      if (above.getBlock() instanceof SteelDoubleTBulbBlock && hasBulb(above)) {
         event.setCanceled(true);
      }
   }
}
