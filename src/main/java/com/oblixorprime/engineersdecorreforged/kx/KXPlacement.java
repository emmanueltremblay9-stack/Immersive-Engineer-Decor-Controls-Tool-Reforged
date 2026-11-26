package com.oblixorprime.engineersdecorreforged.kx;

import java.util.*;

/**
 * Bounded placement transaction for 45-degree bracing segments.
 * The Minecraft adapter is responsible for authoritative server-thread execution,
 * real BlockState/BlockEntity snapshots, permissions and exact restoration.
 */
public final class KXPlacement {
    private KXPlacement() {}
    public static final int MAX_SPAN_CELLS = 16;

    public record Cell(int x,int y,int z) {
        public Cell plus(KXOrientation.Int3 delta) { return new Cell(x+delta.x(),y+delta.y(),z+delta.z()); }
        public KXOrientation.Int3 minus(Cell other) { return new KXOrientation.Int3(x-other.x,y-other.y,z-other.z); }
    }
    public record Span(Cell fromPillar, Cell toPillar, KXTopology.Slot slot, String plane) {
        public Span {
            Objects.requireNonNull(fromPillar);Objects.requireNonNull(toPillar);
            Objects.requireNonNull(slot);Objects.requireNonNull(plane);
            if(plane.isBlank()) throw new IllegalArgumentException("Missing brace plane");
        }
    }
    public record Plan(KXOrientation orientation, String plane, List<Span> spans,
            Map<Cell,Set<KXTopology.Slot>> segments) {
        public Plan {
            spans=List.copyOf(spans);
            var copy=new LinkedHashMap<Cell,Set<KXTopology.Slot>>();
            segments.forEach((cell,slots)->copy.put(cell,Set.copyOf(slots)));
            segments=Collections.unmodifiableMap(copy);
        }
        public int uniqueCells() { return segments.size(); }
    }

    /**
     * Input endpoints are world cell centers; two axes must differ equally (45 degrees),
     * the third must remain zero in the selected pillar plane.
     * Crossing diagonals merge into a single block-cell payload rather than overlapping world blocks.
     */
    public static Plan plan(KXOrientation orientation, String plane, Collection<Span> requestedSpans) {
        Objects.requireNonNull(orientation);Objects.requireNonNull(plane);Objects.requireNonNull(requestedSpans);
        if(plane.isBlank() || requestedSpans.isEmpty() || requestedSpans.size()>4)
            throw new IllegalArgumentException("Expected 1 to 4 spans in one nonblank plane");
        var spans=List.copyOf(requestedSpans);
        var grouped=new LinkedHashMap<Cell, EnumSet<KXTopology.Slot>>();
        var seen=EnumSet.noneOf(KXTopology.Slot.class);
        for(Span span:spans) {
            if(!plane.equals(span.plane())) throw new IllegalArgumentException("Different brace planes cannot be joined");
            if(!seen.add(span.slot())) throw new IllegalArgumentException("Duplicate span slot: "+span.slot());
            KXOrientation.Int3 delta=span.toPillar.minus(span.fromPillar);
            int across=delta.dot(orientation.right());
            int up=delta.dot(orientation.axis());
            int depth=delta.dot(orientation.depth());
            if(depth!=0 || across==0 || up==0 || Math.abs(across)!=Math.abs(up)
                    || Math.abs(across)>MAX_SPAN_CELLS || Math.abs(across)<2)
                throw new IllegalArgumentException("Only bounded 45-degree in-plane spans of 2..16 cells are supported");
            int rightSign=Integer.signum(across), upSign=Integer.signum(up);
            for(int step=1;step<Math.abs(across);step++) {
                Cell cell=span.fromPillar.plus(orientation.worldDirection(step*rightSign,step*upSign,0));
                grouped.computeIfAbsent(cell,k->EnumSet.noneOf(KXTopology.Slot.class)).add(span.slot());
            }
        }
        if(grouped.isEmpty()) throw new IllegalArgumentException("No owned brace cells to place");
        return new Plan(orientation,plane,spans,new LinkedHashMap<>(grouped));
    }

    public enum CellKind { AIR, OWN_SEGMENT, OCCUPIED }
    /** opaqueSave must contain any necessary state and block-entity data for exact restoration. */
    public record Snapshot(CellKind kind, KXOrientation orientation,
            String plane, Set<KXTopology.Slot> slots, Object opaqueSave) {
        public Snapshot {
            Objects.requireNonNull(kind); slots=slots==null?Set.of():Set.copyOf(slots);
            if(kind==CellKind.OWN_SEGMENT && (orientation==null || plane==null))
                throw new IllegalArgumentException("Existing segment requires orientation and plane");
        }
    }
    /** Bridge MUST be implemented on the server using actual game permission/collision checks. */
    public interface World {
        boolean isServerThread();
        boolean isLoaded(Cell cell); // must not load a chunk
        boolean isCompatiblePillar(Cell cell,KXOrientation orientation);
        boolean isReciprocal(Span span, KXOrientation orientation);
        boolean entitySpaceClear(Cell cell);
        Snapshot snapshot(Cell cell);
        boolean placeOrUpdate(Cell cell,KXOrientation orientation,String plane,Set<KXTopology.Slot> slots);
        boolean restore(Cell cell,Snapshot previous); // MUST restore block entity as well
        int itemsAvailable();
        /**
         * Atomic all-or-nothing debit: false or an exception MUST leave inventory
         * unchanged. The cell rollback cannot restore an inventory hidden by this
         * interface. A game adapter must qualify this contract before runtime use.
         */
        boolean consumeItems(int count);
        boolean creativeMode();
    }
    public enum Status { APPLIED, REJECTED, ROLLED_BACK, RECOVERY_REQUIRED }
    public record Result(Status status, int newCells, int chargedItems, String message) {}

    /**
     * Preflight all cells/endpoints before any writes, then commit with compensating
     * rollback on any failure. Actual Minecraft event side effects are NOT guaranteed
     * atomic by this Java method; server integration must be GameTest qualified.
     */
    public static Result apply(World world,Plan plan) {
        Objects.requireNonNull(world);Objects.requireNonNull(plan);
        if(!world.isServerThread())return new Result(Status.REJECTED,0,0,"not on authoritative server thread");
        for(Span span:plan.spans()) {
            if(!world.isLoaded(span.fromPillar()) || !world.isLoaded(span.toPillar()))
                return new Result(Status.REJECTED,0,0,"endpoint chunk not loaded");
            if(!world.isCompatiblePillar(span.fromPillar(),plan.orientation())
              ||!world.isCompatiblePillar(span.toPillar(),plan.orientation())
              ||!world.isReciprocal(span,plan.orientation()))
                return new Result(Status.REJECTED,0,0,"unpaired or incompatible endpoint");
        }
        var snapshots=new LinkedHashMap<Cell,Snapshot>();
        for(var entry:plan.segments().entrySet()) {
            Cell cell=entry.getKey();
            if(!world.isLoaded(cell) || !world.entitySpaceClear(cell))
                return new Result(Status.REJECTED,0,0,"blocked, entity-occupied or unloaded cell");
            Snapshot snapshot=world.snapshot(cell);
            if(snapshot==null || snapshot.kind()==CellKind.OCCUPIED)
                return new Result(Status.REJECTED,0,0,"foreign or unrecognized occupant");
            if(snapshot.kind()==CellKind.OWN_SEGMENT && (!plan.orientation().equals(snapshot.orientation())
                    || !plan.plane().equals(snapshot.plane())))
                return new Result(Status.REJECTED,0,0,"existing segment has an incompatible orientation/plane");
            snapshots.put(cell,snapshot);
        }
        int newCells=(int)snapshots.values().stream().filter(s->s.kind()==CellKind.AIR).count();
        if(!world.creativeMode() && world.itemsAvailable()<newCells)
            return new Result(Status.REJECTED,0,0,"insufficient items; exactly one per new segment cell");
        var changed=new ArrayList<Cell>();
        try {
            for(var entry:plan.segments().entrySet()) {
                Cell cell=entry.getKey();Snapshot previous=snapshots.get(cell);
                Set<KXTopology.Slot> combined=EnumSet.noneOf(KXTopology.Slot.class);
                combined.addAll(previous.slots());combined.addAll(entry.getValue());
                changed.add(cell); // a failed write may have partially changed a cell
                if(!world.placeOrUpdate(cell,plan.orientation(),plan.plane(),combined))
                    return rollback(world,snapshots,changed,"segment write refused");
            }
            if(!world.creativeMode() && !world.consumeItems(newCells))
                return rollback(world,snapshots,changed,"inventory changed before final charge");
            return new Result(Status.APPLIED,newCells,world.creativeMode()?0:newCells,"segments placed; no phantom-cell charge");
        } catch(RuntimeException failure) {
            return rollback(world,snapshots,changed,"placement exception: "+failure.getClass().getSimpleName());
        }
    }
    private static Result rollback(World world,Map<Cell,Snapshot> snapshots,List<Cell> changed,String message) {
        boolean allRestored=true;
        for(int i=changed.size()-1;i>=0;i--) {
            Cell c=changed.get(i);
            try { if(!world.restore(c,snapshots.get(c))) allRestored=false; }
            catch(RuntimeException ignored) { allRestored=false; }
        }
        return new Result(allRestored?Status.ROLLED_BACK:Status.RECOVERY_REQUIRED,0,0,
                allRestored?message+"; state rollback completed":message+"; state restoration incomplete");
    }
}
