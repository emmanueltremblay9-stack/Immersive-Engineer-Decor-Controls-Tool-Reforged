package com.oblixorprime.engineersdecorreforged.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 24 proper, quarter-turn rotations: local +Y becomes facing; local +X selects roll. */
public final class HeavyDoubleTOrientation {
   private HeavyDoubleTOrientation() {}

   public static Direction right(Direction facing, int roll) {
      if (roll < 0 || roll > 3) throw new IllegalArgumentException("roll 0..3");
      Direction base = switch (facing) {
         case UP, DOWN, NORTH, SOUTH -> Direction.EAST;
         case EAST -> Direction.SOUTH;
         case WEST -> Direction.NORTH;
      };
      Direction depth = cross(base, facing);
      return switch (roll) {
         case 0 -> base;
         case 1 -> depth;
         case 2 -> base.getOpposite();
         default -> depth.getOpposite();
      };
   }

   public static Direction depth(Direction facing, int roll) {
      return cross(right(facing, roll), facing);
   }

   public static int rollFor(Direction facing, Direction right) {
      for (int roll = 0; roll < 4; roll++) if (right(facing, roll) == right) return roll;
      throw new IllegalArgumentException("right axis must be perpendicular to facing");
   }

   public static Direction cross(Direction a, Direction b) {
      int x = a.getStepY() * b.getStepZ() - a.getStepZ() * b.getStepY();
      int y = a.getStepZ() * b.getStepX() - a.getStepX() * b.getStepZ();
      int z = a.getStepX() * b.getStepY() - a.getStepY() * b.getStepX();
      for (Direction d : Direction.values()) {
         if (d.getStepX() == x && d.getStepY() == y && d.getStepZ() == z) return d;
      }
      throw new IllegalArgumentException("expected perpendicular axes");
   }

   private static double[] point(double x, double y, double z, Direction facing, int roll) {
      Direction right = right(facing, roll);
      Direction depth = depth(facing, roll);
      x -= 0.5; y -= 0.5; z -= 0.5;
      return new double[] {
         0.5 + x * right.getStepX() + y * facing.getStepX() + z * depth.getStepX(),
         0.5 + x * right.getStepY() + y * facing.getStepY() + z * depth.getStepY(),
         0.5 + x * right.getStepZ() + y * facing.getStepZ() + z * depth.getStepZ()
      };
   }

   /** Rotates axis-aligned collision boxes about the block centre. Units are block units [0,1]. */
   public static VoxelShape shape(VoxelShape local, Direction facing, int roll) {
      VoxelShape result = Shapes.empty();
      for (var box : local.toAabbs()) {
         double[] a = point(box.minX, box.minY, box.minZ, facing, roll);
         double[] b = point(box.maxX, box.maxY, box.maxZ, facing, roll);
         result = Shapes.or(result, Shapes.box(
            Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
            Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])
         ));
      }
      return result.optimize();
   }
}
