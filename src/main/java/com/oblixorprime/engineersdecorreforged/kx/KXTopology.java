package com.oblixorprime.engineersdecorreforged.kx;

import java.util.*;

/**
 * MK2's K-side / X-center contract. Inputs are genuine, world-verified endpoints.
 * This selector NEVER creates braces or treats nearby blocks as connected by itself.
 */
public final class KXTopology {
    public static final double X_ALIGNMENT_TOLERANCE_VOXELS = .16;
    public static final double K_STAGGER_THRESHOLD_VOXELS = .35;
    public static final double LEFT_PLATE_X = 4.8, RIGHT_PLATE_X = 11.2;
    private static final double CENTER = 8.0;
    private KXTopology() {}

    public enum Slot {
        UL, LL, UR, LR;
        public Side side() { return this==UL||this==LL ? Side.LEFT : Side.RIGHT; }
    }
    public enum Side { LEFT, RIGHT }
    public enum Mode {
        CLEAN, K_LEFT_SIDE, K_RIGHT_SIDE, K_BOTH_SIDES, K_BOTH_STAGGERED,
        X_CENTERED
    }
    public record Port(Slot slot, double x, double y, double z, String plane, boolean reciprocal) {
        public Port {
            Objects.requireNonNull(slot); Objects.requireNonNull(plane);
            if (plane.isBlank() || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
                throw new IllegalArgumentException("Invalid port coordinates / plane");
            if (x<0||x>16||y<0||y>16||z<0||z>16)
                throw new IllegalArgumentException("Port outside a 16-unit cell");
        }
    }
    public record Selection(Mode mode, Set<Slot> arms, Set<Side> sidePlates,
            boolean centerPlate, Map<Side,Double> sidePlateHeights, String assetKey, String reason) {
        public Selection {
            arms=Set.copyOf(arms);sidePlates=Set.copyOf(sidePlates);
            sidePlateHeights=Map.copyOf(sidePlateHeights);
            if(assetKey == null || assetKey.isBlank())throw new IllegalArgumentException("Missing MK2 asset key");
        }
        public String modelKey() { return assetKey; }
        public int armCount() { return arms.size(); }
    }

    public static Selection select(Collection<Port> candidates, String selectedPlane) {
        Objects.requireNonNull(candidates); Objects.requireNonNull(selectedPlane);
        var seen=EnumSet.noneOf(Slot.class);
        var connected=new ArrayList<Port>();
        for (Port p:candidates) {
            if (!seen.add(p.slot())) throw new IllegalArgumentException("Duplicate port slot: "+p.slot());
            if (p.reciprocal() && selectedPlane.equals(p.plane())) connected.add(p);
        }
        if (connected.isEmpty()) return new Selection(Mode.CLEAN,Set.of(),Set.of(),false,Map.of(),"clean","no valid reciprocal port");
        var arms=EnumSet.noneOf(Slot.class);
        for(Port p:connected)arms.add(p.slot());
        boolean centered=connected.stream().allMatch(p-> Math.abs(p.x()-CENTER)<=X_ALIGNMENT_TOLERANCE_VOXELS
            && Math.abs(p.y()-CENTER)<=X_ALIGNMENT_TOLERANCE_VOXELS)
            && spread(connected, Port::z) <= X_ALIGNMENT_TOLERANCE_VOXELS;
        if(arms.size()==4 && centered)
            return new Selection(Mode.X_CENTERED,arms,Set.of(),true,Map.of(),"x_centered","four reciprocal arms share one central face point");

        var heights=new EnumMap<Side,Double>(Side.class);
        for(Side s:Side.values()) {
            var ports=connected.stream().filter(p->p.slot().side()==s).toList();
            if(ports.isEmpty())continue;
            // A K pair may only get one side plate when its two arms really meet there.
            if(ports.size()==2 && (spread(ports,Port::y)>X_ALIGNMENT_TOLERANCE_VOXELS
                || spread(ports,Port::x)>X_ALIGNMENT_TOLERANCE_VOXELS
                || spread(ports,Port::z)>X_ALIGNMENT_TOLERANCE_VOXELS))
                throw new IllegalArgumentException("Noncoincident K pair on "+s+"; cannot draw a floating side plate");
            heights.put(s,ports.stream().mapToDouble(Port::y).average().orElseThrow());
        }
        Set<Side> sides=heights.keySet().isEmpty()?Set.of():Set.copyOf(heights.keySet());
        Mode mode;
        if(sides.size()==2)mode=Math.abs(heights.get(Side.LEFT)-heights.get(Side.RIGHT))>K_STAGGER_THRESHOLD_VOXELS
            ?Mode.K_BOTH_STAGGERED:Mode.K_BOTH_SIDES;
        else mode=sides.contains(Side.LEFT)?Mode.K_LEFT_SIDE:Mode.K_RIGHT_SIDE;
        String visual=(arms.size()==3 && centered)?"x_broken_three":mode.name().toLowerCase(Locale.ROOT);
        return new Selection(mode,arms,sides,false,heights,visual,"non-X endpoints retain their real arms and side plates");
    }

    @FunctionalInterface private interface Coordinate { double get(Port p); }
    private static double spread(Collection<Port> ports, Coordinate axis) {
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
        for(Port p:ports){min=Math.min(min,axis.get(p));max=Math.max(max,axis.get(p));}
        return max-min;
    }

    /**
     * World/model adapter must use these same per-arm endpoints on the pillar AND
     * on the neighboring segment. This is the precise K fallback after a broken X:
     * no fourth phantom arm and no visible gap between two independently rendered cells.
     * In MK2, the front flange connection depth is 2.6 voxel units.
     */
    public static Map<Slot,KXOrientation.Vec3> renderEndpoints(Collection<Port> candidates, String selectedPlane) {
        Selection selection=select(candidates,selectedPlane);
        var mapped=new EnumMap<Slot,KXOrientation.Vec3>(Slot.class);
        double referenceDepth=2.6;
        for(Port p:candidates) {
            if(!p.reciprocal() || !selectedPlane.equals(p.plane())) continue;
            if(Math.abs(p.z()-referenceDepth)>X_ALIGNMENT_TOLERANCE_VOXELS)
                throw new IllegalArgumentException("An endpoint is not on the modeled front flange plane");
            double x=selection.centerPlate()?CENTER:p.slot().side()==Side.LEFT?LEFT_PLATE_X:RIGHT_PLATE_X;
            double y=selection.centerPlate()?CENTER:selection.sidePlateHeights().get(p.slot().side());
            mapped.put(p.slot(),new KXOrientation.Vec3(x,y,referenceDepth));
        }
        return Map.copyOf(mapped);
    }

    /** Each face is independent; a compatible axial joint suppresses its anchor. */
    public static EnumSet<KXOrientation.Face> anchorFaces(Map<KXOrientation.Face,Boolean> verifiedContacts,
            Set<KXOrientation.Face> compatibleAxialFaces) {
        var result=EnumSet.noneOf(KXOrientation.Face.class);
        for(KXOrientation.Face face:KXOrientation.Face.values())
            if(Boolean.TRUE.equals(verifiedContacts.get(face)) && !compatibleAxialFaces.contains(face))result.add(face);
        return result;
    }
}
