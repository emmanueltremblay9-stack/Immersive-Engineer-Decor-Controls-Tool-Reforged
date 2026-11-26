package com.oblixorprime.engineersdecorreforged.kx;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Discrete, proper (non-mirrored) rotations matching the MK2 24-frame design. */
public record KXOrientation(KXOrientation.Face facing, int roll) {
    public enum Face {
        DOWN(0,-1,0), UP(0,1,0), NORTH(0,0,-1), SOUTH(0,0,1), WEST(-1,0,0), EAST(1,0,0);
        final Int3 vector;
        Face(int x, int y, int z) { vector = new Int3(x,y,z); }
        public Int3 vector() { return vector; }
        public Face opposite() { return from(vector.negate()); }
        public static Face from(Int3 value) {
            for (Face face : values()) if (face.vector.equals(value)) return face;
            throw new IllegalArgumentException("Not a six-way face: " + value);
        }
    }
    public record Int3(int x, int y, int z) {
        public Int3 negate() { return new Int3(-x,-y,-z); }
        public int dot(Int3 b) { return x*b.x + y*b.y + z*b.z; }
        public Int3 cross(Int3 b) { return new Int3(y*b.z-z*b.y,z*b.x-x*b.z,x*b.y-y*b.x); }
        public Int3 add(Int3 b) { return new Int3(x+b.x,y+b.y,z+b.z); }
        public Int3 multiply(int k) { return new Int3(x*k,y*k,z*k); }
    }
    public record Vec3(double x, double y, double z) {
        public Vec3 add(Vec3 b) { return new Vec3(x+b.x,y+b.y,z+b.z); }
    }

    public KXOrientation {
        Objects.requireNonNull(facing);
        if (roll < 0 || roll > 3) throw new IllegalArgumentException("roll must be 0..3");
    }
    public Int3 axis() { return facing.vector(); }
    public Int3 right() {
        Int3 base = switch (facing) {
            case UP, DOWN, NORTH, SOUTH -> Face.EAST.vector();
            case EAST -> Face.SOUTH.vector();
            case WEST -> Face.NORTH.vector();
        };
        Int3 depth = base.cross(axis());
        return switch (roll) { case 0 -> base; case 1 -> depth; case 2 -> base.negate(); default -> depth.negate(); };
    }
    public Int3 depth() { return right().cross(axis()); }
    public Int3 front() { return depth().negate(); }
    public Int3 worldDirection(int lx, int ly, int lz) {
        return right().multiply(lx).add(axis().multiply(ly)).add(depth().multiply(lz));
    }
    /** Rotate local coordinates (in voxel units 0..16) about the 8,8,8 center. */
    public Vec3 toWorld(Vec3 local) {
        var rotated = worldVector(local.x-8,local.y-8,local.z-8);
        return new Vec3(rotated.x+8,rotated.y+8,rotated.z+8);
    }
    /** Inverse of toWorld, useful for identifying the clicked local port. */
    public Vec3 toLocal(Vec3 world) {
        double x=world.x-8,y=world.y-8,z=world.z-8;
        return new Vec3(8+dot(right(),x,y,z),8+dot(axis(),x,y,z),8+dot(depth(),x,y,z));
    }
    private Vec3 worldVector(double x, double y, double z) {
        Int3 r=right(),a=axis(),d=depth();
        return new Vec3(r.x*x+a.x*y+d.x*z,r.y*x+a.y*y+d.y*z,r.z*x+a.z*y+d.z*z);
    }
    private static double dot(Int3 p,double x,double y,double z) { return p.x*x+p.y*y+p.z*z; }
    public static List<KXOrientation> all() {
        var result = new ArrayList<KXOrientation>();
        for (Face face : Face.values()) for (int roll=0;roll<4;roll++) result.add(new KXOrientation(face,roll));
        return List.copyOf(result);
    }
}
