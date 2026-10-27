package com.oblixorprime.engineersdecorreforged.utility;

import com.oblixorprime.engineersdecorreforged.block.NativeFluidloggedBlock;
import com.oblixorprime.engineersdecorreforged.block.NativeTransientFluidBlock;
import com.oblixorprime.engineersdecorreforged.block.NativeDecorativeBlocks;
import java.util.Set;
import com.oblixorprime.engineersdecorreforged.fluid.NativeFluidCells;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

/** Loaded-cell search only. A result is a snapshot, never a reservation of world fluid. */
public final class FlowingFluidIntake {
   /** Approved connected-fluid path limit, measured from the moving inlet cell above the Funnel. */
   public static final int MAX_PATH_DISTANCE = 16;
   private FlowingFluidIntake() {}
   public record Source(BlockPos pos, BlockState block, FluidState fluid, int distance) {
      public FluidStack volume() { return new FluidStack(fluid.getType(), 1000); }
   }
   public record Search(Source source, boolean incomplete, int connectedCellsVisited) {}
   private static final Comparator<BlockPos> ORDER = Comparator.<BlockPos>comparingInt(pos -> pos.getX())
      .thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ);

   public static Search search(ServerLevel level, BlockPos opening, int maxDistance) {
      if (maxDistance < 0 || maxDistance > MAX_PATH_DISTANCE || !level.hasChunkAt(opening)) return new Search(null, true, 0);
      FluidState intake=level.getFluidState(opening);
      if (intake.isEmpty() || intake.isSource()) return new Search(null, false, 0);
      BlockGetter view=loadedView(level);
      if(Block.isFaceFull(level.getBlockState(opening).getCollisionShape(view,opening,CollisionContext.empty()),Direction.DOWN))
         return new Search(null,false,0);
      var visited=new HashSet<BlockPos>();visited.add(opening);
      List<BlockPos> layer=List.of(opening);
      for(int distance=0;distance<=maxDistance;distance++) {
         var sources=new ArrayList<BlockPos>();
         for(var pos:layer) if(level.getFluidState(pos).isSource()) sources.add(pos);
         if(!sources.isEmpty()) {
            sources.sort(ORDER);var pos=sources.getFirst();
            return new Search(new Source(pos,level.getBlockState(pos),level.getFluidState(pos),distance),false,visited.size());
         }
         if(distance==maxDistance) break;
         var next=new ArrayList<BlockPos>();boolean incomplete=false;
         for(var pos:layer) for(Direction face:Direction.values()) {
            var neighbor=pos.relative(face);
            if(level.isOutsideBuildHeight(neighbor)||visited.contains(neighbor)) continue;
            if(!level.hasChunkAt(neighbor)){incomplete=true;continue;}
            var fluid=level.getFluidState(neighbor);
            if(fluid.isEmpty()||!fluid.getType().isSame(intake.getType())) continue;
            var first=level.getBlockState(pos);var second=level.getBlockState(neighbor);
            if(!Shapes.mergedFaceOccludes(first.getCollisionShape(view,pos,CollisionContext.empty()),
                  second.getCollisionShape(view,neighbor,CollisionContext.empty()),face)) {
               visited.add(neighbor); next.add(neighbor);
            }
         }
         // An unavailable cell at the next distance could contain a nearer/equal source.
         if(incomplete) return new Search(null,true,visited.size());
         if(next.isEmpty()) break;
         layer=next;
      }
      return new Search(null,false,visited.size());
   }

   /** Integer lattice L1 ball: 833 cells at 8 edges, 6017 at 16; each has at most six neighbours. */
   public static int maximumVisitedCells(int distance) {
      if(distance<0||distance>MAX_PATH_DISTANCE)throw new IllegalArgumentException("Unsupported search bound");
      return (4*distance*distance*distance+6*distance*distance+8*distance+3)/3;
   }

   /** Verified source-pickup implementations; arbitrary subclass overrides are not admitted. */
   private static final Set<Class<?>> SOURCE_PICKUPS=Set.of(LiquidBlock.class,SimpleWaterloggedBlock.class,
      NativeFluidloggedBlock.class,NativeTransientFluidBlock.class,
      NativeDecorativeBlocks.NativeSolidBlock.class,NativeDecorativeBlocks.NativeOpenBlock.class,
      NativeDecorativeBlocks.NativeStairBlock.class,NativeDecorativeBlocks.NativeWallBlock.class,
      NativeDecorativeBlocks.NativeLadderBlock.class,NativeDecorativeBlocks.NativeSlabBlock.class,
      NativeDecorativeBlocks.NativeDoorBlock.class);

   /** Conservative adapters: never replace an arbitrary fluidlogged block with air. */
   public static boolean canDrain(ServerLevel level, Source source) {
      if(!current(level,source)||!removalNeighborhoodLoaded(level,source.pos())) return false;
      var block=source.block().getBlock();
      if(NativeFluidCells.supported(source.block()) && NativeFluidCells.active(level,source.pos())) return bucketMatches(source);
      if(block instanceof LiquidBlock && level.getBlockEntity(source.pos())!=null) return false;
      return block instanceof BucketPickup && SOURCE_PICKUPS.contains(pickupOwner(block)) && bucketMatches(source);
   }

   public static boolean drain(ServerLevel level, Source source) {
      if(!canDrain(level,source)||!current(level,source)) return false;
      var block=source.block().getBlock();
      if(NativeFluidCells.supported(source.block()) && NativeFluidCells.active(level,source.pos())) {
         NativeFluidCells.set(level,source.pos(),Fluids.EMPTY.defaultFluidState());
         return removed(level,source);
      }
      ItemStack result=((BucketPickup)block).pickupBlock(null,level,source.pos(),source.block());
      FluidStack removed=FluidUtil.getFluidContained(result).orElse(FluidStack.EMPTY);
      return removed.getAmount()==1000 && FluidStack.isSameFluidSameComponents(removed,source.volume()) && removed(level,source);
   }

   private static boolean removed(ServerLevel level,Source source) {
      if(!level.hasChunkAt(source.pos()))return false;
      var remaining=level.getFluidState(source.pos());
      return !remaining.isSource()||!remaining.getType().isSame(source.fluid().getType());
   }

   /** Keep standard neighbour/shape reads around a source removal away from absent chunks. */
   private static boolean removalNeighborhoodLoaded(ServerLevel level,BlockPos pos) {
      for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)
         if(Math.abs(dx)+Math.abs(dz)<=2&&!level.hasChunkAt(pos.offset(dx,0,dz)))return false;
      return true;
   }

   private static boolean current(ServerLevel level,Source source) {
      return source!=null&&level.hasChunkAt(source.pos())&&level.getBlockState(source.pos()).equals(source.block())
         && level.getFluidState(source.pos()).equals(source.fluid())&&source.fluid().isSource();
   }
   private static boolean bucketMatches(Source source) {
      var stack=FluidUtil.getFilledBucket(source.volume());
      var contained=FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
      return source.fluid().getType().getBucket()==stack.getItem() && contained.getAmount()==1000
         && FluidStack.isSameFluidSameComponents(contained,source.volume());
   }
   private static Class<?> pickupOwner(Block block) {
      try { return block.getClass().getMethod("pickupBlock",net.minecraft.world.entity.player.Player.class,
         LevelAccessor.class,BlockPos.class,BlockState.class).getDeclaringClass(); }
      catch(NoSuchMethodException absent) { return null; }
   }
   private static BlockGetter loadedView(ServerLevel level) {
      return new BlockGetter() {
         @Override public BlockState getBlockState(BlockPos pos) {
            return level.hasChunkAt(pos)?level.getBlockState(pos):Blocks.BARRIER.defaultBlockState();
         }
         @Override public FluidState getFluidState(BlockPos pos) {
            return level.hasChunkAt(pos)?level.getFluidState(pos):Fluids.EMPTY.defaultFluidState();
         }
         @Override public BlockEntity getBlockEntity(BlockPos pos) { return level.hasChunkAt(pos)?level.getBlockEntity(pos):null; }
         @Override public int getHeight(){return level.getHeight();}
         @Override public int getMinBuildHeight(){return level.getMinBuildHeight();}
      };
   }
}
