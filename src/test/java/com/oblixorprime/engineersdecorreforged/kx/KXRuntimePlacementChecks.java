package com.oblixorprime.engineersdecorreforged.kx;
import java.util.*;
import static com.oblixorprime.engineersdecorreforged.kx.KXPlacement.*;
public final class KXRuntimePlacementChecks {
    static int count;
    static void check(boolean condition) { count++; if (!condition) throw new AssertionError("check " + count); }
    static final class World implements KXPlacement.World, KXRuntimePlacement.SyncPort {
        Map<Cell,Snapshot> cells = new HashMap<>(); int items=20,writes,charges,refreshes,notifications;
        boolean failWrite,failRestore,failSync,failNotify,denyCharge,server=true;
        public boolean isServerThread(){return server;}
        public boolean isLoaded(Cell c){return true;}
        public boolean isCompatiblePillar(Cell c,KXOrientation o){return true;}
        public boolean isReciprocal(Span s,KXOrientation o){return true;}
        public boolean entitySpaceClear(Cell c){return true;}
        public Snapshot snapshot(Cell c){return cells.getOrDefault(c,new Snapshot(CellKind.AIR,null,null,Set.of(),null));}
        public boolean placeOrUpdate(Cell c,KXOrientation o,String p,Set<KXTopology.Slot> s){writes++;cells.put(c,new Snapshot(CellKind.OWN_SEGMENT,o,p,s,null));return !failWrite;}
        public boolean restore(Cell c,Snapshot old){if(failRestore)return false;if(old.kind()==CellKind.AIR)cells.remove(c);else cells.put(c,old);return true;}
        public int itemsAvailable(){return items;}
        public boolean consumeItems(int n){charges++;if(denyCharge||items<n)return false;items-=n;return true;}
        public boolean creativeMode(){return false;}
        public void refreshTopology(Set<Cell> a){refreshes++;if(failSync)throw new IllegalStateException();}
        public void notifyClients(Set<Cell> a){notifications++;if(failNotify)throw new IllegalStateException();}
    }
    static Plan plan(KXOrientation o){Cell from=new Cell(10,20,30);return KXPlacement.plan(o,"front",List.of(new Span(from,from.plus(o.worldDirection(4,4,0)),KXTopology.Slot.UL,"front")));}
    public static void main(String[] args){
        count=0;
        for(KXOrientation o:KXOrientation.all()) {
            Plan p=plan(o); World w=new World(); var r=KXRuntimePlacement.apply(w,p,w);
            check(r.placement().status()==Status.APPLIED);check(r.placement().chargedItems()==3);check(w.items==17 && w.charges==1);
            check(r.sync()==KXRuntimePlacement.SyncState.COMPLETED);check(w.refreshes==1 && w.notifications==1);
            check(r.affected().contains(p.spans().getFirst().fromPillar()));check(r.affected().contains(p.spans().getFirst().toPillar()));
            check(r.affected().containsAll(p.segments().keySet()));
        }
        Plan p=plan(KXOrientation.all().getFirst());
        World w=new World();w.failSync=true;var r=KXRuntimePlacement.apply(w,p,w);
        check(r.placement().status()==Status.APPLIED && r.sync()==KXRuntimePlacement.SyncState.PENDING);check(w.charges==1&&w.notifications==1);
        w=new World();w.failWrite=true;r=KXRuntimePlacement.apply(w,p,w);
        check(r.placement().status()==Status.ROLLED_BACK);check(w.cells.isEmpty()&&w.items==20&&w.charges==0);check(w.notifications==1);
        w=new World();w.failWrite=true;w.failRestore=true;r=KXRuntimePlacement.apply(w,p,w);
        check(r.placement().status()==Status.RECOVERY_REQUIRED);check(w.notifications==1&&w.charges==0);
        w=new World();w.items=0;r=KXRuntimePlacement.apply(w,p,w);
        check(r.placement().status()==Status.REJECTED&&w.writes==0&&w.refreshes==0);
        w=new World();var forged=new Plan(p.orientation(),p.plane(),p.spans(),Map.of(new Cell(999,999,999),Set.of(KXTopology.Slot.LL)));
        r=KXRuntimePlacement.apply(w,forged,w);check(r.placement().status()==Status.REJECTED&&w.writes==0&&w.notifications==0);
        w=new World();w.server=false;r=KXRuntimePlacement.apply(w,p,w);check(r.placement().status()==Status.REJECTED&&w.writes==0);
        w=new World();w.failNotify=true;r=KXRuntimePlacement.apply(w,p,w);
        check(r.placement().status()==Status.APPLIED&&r.sync()==KXRuntimePlacement.SyncState.PENDING&&w.charges==1);
        w=new World();w.denyCharge=true;r=KXRuntimePlacement.apply(w,p,w);
        check(r.placement().status()==Status.ROLLED_BACK&&w.items==20&&w.cells.isEmpty());
        w=new World();r=KXRuntimePlacement.apply(w,new Plan(null,p.plane(),p.spans(),p.segments()),w);
        check(r.placement().status()==Status.REJECTED&&w.writes==0);
        System.out.println("PASS KX runtime seam: "+count+" assertions; no Minecraft runtime");
    }
}
