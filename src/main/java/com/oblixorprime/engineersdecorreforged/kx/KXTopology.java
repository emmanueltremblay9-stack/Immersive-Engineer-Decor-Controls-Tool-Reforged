package com.oblixorprime.engineersdecorreforged.kx;

import java.util.*;

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
        CLEAN, ANCHOR_DOWN_ONLY, K_LEFT_SIDE, K_RIGHT_SIDE, K_BOTH_SIDES,
        K_BOTH_STAGGERED, X_CENTERED
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
            Objects.requireNonNull(mode);
            arms=Set.copyOf(arms);sidePlates=Set.copyOf(sidePlates);
            sidePlateHeights=Map.copyOf(sidePlateHeights);
            if(assetKey == null || assetKey.isBlank())throw new IllegalArgumentException("Missing MK2 asset key");
        }
        public String modelKey() { return assetKey; }
        public int armCount() { return arms.size(); }
    }

    public static Selection select(Collection<Port> candidates, String selectedPlane) {
        return select(candidates, selectedPlane, false);
    }

    public static Selection select(Collection<Port> candidates, String selectedPlane, boolean anchorDown) {
        Objects.requireNonNull(candidates); Objects.requireNonNull(selectedPlane);
        var seen=EnumSet.noneOf(Slot.class);
        var connected=new ArrayList<Port>();
        for (Port p:candidates) {
            if (!seen.add(p.slot())) throw new IllegalArgumentException("Duplicate port slot: "+p.slot());
            if (p.reciprocal() && selectedPlane.equals(p.plane())) connected.add(p);
        }
        if (connected.isEmpty()) {
            return anchorDown
                ? new Selection(Mode.ANCHOR_DOWN_ONLY,Set.of(),Set.of(),false,Map.of(),
                    "anchor_down_only","verified down-face support; no diagonal connection")
                : new Selection(Mode.CLEAN,Set.of(),Set.of(),false,Map.of(),"clean","no valid reciprocal port");
        }
        var arms=EnumSet.noneOf(Slot.class);
        for(Port p:connected)arms.add(p.slot());
        boolean centered=connected.stream().allMatch(p-> Math.abs(p.x()-CENTER)<=X_ALIGNMENT_TOLERANCE_VOXELS
            && Math.abs(p.y()-CENTER)<=X_ALIGNMENT_TOLERANCE_VOXELS)
            && spread(connected, Port::z) <= X_ALIGNMENT_TOLERANCE_VOXELS;
        if(arms.size()==4 && centered)
            return new Selection(Mode.X_CENTERED,arms,Set.of(),true,Map.of(),"x_centered",
                "four reciprocal arms share one central face point");

        var heights=new EnumMap<Side,Double>(Side.class);
        for(Side s:Side.values()) {
            var ports=connected.stream().filter(p->p.slot().side()==s).toList();
            if(ports.isEmpty())continue;
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
        return new Selection(mode,arms,sides,false,heights,visual,
            "non-X endpoints retain their real arms and side plates");
    }

    /** Validate existing R3 attachment planes; the caller still supplies both slots explicitly. */
    public static void validatePortGeometry(Port port) {
        Objects.requireNonNull(port);
        double expectedDepth=switch(port.plane()) {
            case "front" -> 2.6;
            case "rear" -> 13.4;
            default -> throw new IllegalArgumentException("Unsupported MK2 flange plane: "+port.plane());
        };
        if(Math.abs(port.z()-expectedDepth)>X_ALIGNMENT_TOLERANCE_VOXELS)
            throw new IllegalArgumentException("Port is not on the modeled "+port.plane()+" flange plane");
        double sideX=port.slot().side()==Side.LEFT?LEFT_PLATE_X:RIGHT_PLATE_X;
        if(Math.abs(port.x()-CENTER)>X_ALIGNMENT_TOLERANCE_VOXELS
            && Math.abs(port.x()-sideX)>X_ALIGNMENT_TOLERANCE_VOXELS)
            throw new IllegalArgumentException("Port X does not match its side plate or shared center");
    }

    @FunctionalInterface private interface Coordinate { double get(Port p); }
    private static double spread(Collection<Port> ports, Coordinate axis) {
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
        for(Port p:ports){min=Math.min(min,axis.get(p));max=Math.max(max,axis.get(p));}
        return max-min;
    }

    public static Map<Slot,KXOrientation.Vec3> renderEndpoints(Collection<Port> candidates, String selectedPlane) {
        Selection selection=select(candidates,selectedPlane);
        double referenceDepth=switch(selectedPlane) {
            case "front" -> 2.6;
            case "rear" -> 13.4;
            default -> throw new IllegalArgumentException("Unsupported MK2 flange plane: "+selectedPlane);
        };
        var mapped=new EnumMap<Slot,KXOrientation.Vec3>(Slot.class);
        for(Port p:candidates) {
            if(!p.reciprocal() || !selectedPlane.equals(p.plane())) continue;
            validatePortGeometry(p);
            double x=selection.centerPlate()?CENTER:p.slot().side()==Side.LEFT?LEFT_PLATE_X:RIGHT_PLATE_X;
            double y=selection.centerPlate()?CENTER:selection.sidePlateHeights().get(p.slot().side());
            mapped.put(p.slot(),new KXOrientation.Vec3(x,y,referenceDepth));
        }
        return Map.copyOf(mapped);
    }

    public static EnumSet<KXOrientation.Face> anchorFaces(Map<KXOrientation.Face,Boolean> verifiedContacts,
            Set<KXOrientation.Face> compatibleAxialFaces) {
        var result=EnumSet.noneOf(KXOrientation.Face.class);
        for(KXOrientation.Face face:KXOrientation.Face.values())
            if(Boolean.TRUE.equals(verifiedContacts.get(face)) && !compatibleAxialFaces.contains(face))result.add(face);
        return result;
    }
}
