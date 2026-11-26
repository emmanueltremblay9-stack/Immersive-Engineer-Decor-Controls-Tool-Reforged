package com.oblixorprime.engineersdecorreforged.kx;

import java.util.*;
import static com.oblixorprime.engineersdecorreforged.kx.KXTopology.*;
import static com.oblixorprime.engineersdecorreforged.kx.KXOrientation.*;
import static com.oblixorprime.engineersdecorreforged.kx.KXPlacement.*;

/** Standalone Java 21 tests without Minecraft dependencies. Exits nonzero on first failure. */
public final class KXMechanicsChecks {
    private static int checks=0;
    private static void ok(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void eq(Object actual,Object expected,String message){checks++;if(!Objects.equals(actual,expected))throw new AssertionError(message+" got="+actual+" wanted="+expected);}
    private static void close(double actual,double expected,String message){checks++;if(Math.abs(actual-expected)>1e-7)throw new AssertionError(message+" got="+actual+" wanted="+expected);}
    private static void fail(Runnable action,String message){checks++;try{action.run();throw new AssertionError("Expected rejection: "+message);}catch(IllegalArgumentException expected){}}
    private static Port p(Slot s,double x,double y){return new Port(s,x,y,2.6,"front",true);}
    private static List<Port> pairL(double y){return List.of(p(Slot.UL,4.8,y),p(Slot.LL,4.8,y));}
    private static List<Port> pairR(double y){return List.of(p(Slot.UR,11.2,y),p(Slot.LR,11.2,y));}
    private static List<Port> both(double ly,double ry){var l=new ArrayList<>(pairL(ly));l.addAll(pairR(ry));return l;}
    private static List<Port> center(){return List.of(p(Slot.UL,8,8),p(Slot.LL,8,8),p(Slot.UR,8,8),p(Slot.LR,8,8));}
    private static KXOrientation o(){return new KXOrientation(Face.UP,0);}

    private static void topology() {
        eq(select(List.of(),"front").mode(),Mode.CLEAN,"no supports: clean pillar");
        eq(select(pairL(8),"front").mode(),Mode.K_LEFT_SIDE,"left K uses side plate");
        eq(select(pairR(8),"front").mode(),Mode.K_RIGHT_SIDE,"right K uses side plate");
        Selection same=select(both(8,8),"front");
        eq(same.mode(),Mode.K_BOTH_SIDES,"two same-height K pairs do not make X");
        eq(same.sidePlates(),Set.of(Side.LEFT,Side.RIGHT),"both K plates visible");
        ok(!same.centerPlate(),"no central plate for lateral contacts");
        eq(select(both(8,9),"front").mode(),Mode.K_BOTH_STAGGERED,"staggered plates");
        Selection x=select(center(),"front");
        eq(x.mode(),Mode.X_CENTERED,"all four central ports select X");
        ok(x.centerPlate(),"one central plate");
        eq(x.sidePlates(),Set.of(),"X suppresses side plates");
        eq(x.armCount(),4,"four actual arms only");
        var broken=center().subList(0,3);
        Selection bx=select(broken,"front");
        eq(bx.mode(),Mode.K_BOTH_SIDES,"broken X correctly returns to K topology");
        eq(bx.modelKey(),"x_broken_three","broken X chooses the three-real-arm asset");
        eq(bx.armCount(),3,"three real arms only");
        ok(!bx.centerPlate(),"broken X center plate removed");
        eq(bx.sidePlates(),Set.of(Side.LEFT,Side.RIGHT),"broken X falls back to side plates");
        close(renderEndpoints(broken,"front").get(Slot.UR).x(),11.2,"right broken endpoint reprojected");
        close(renderEndpoints(broken,"front").get(Slot.UL).x(),4.8,"left broken endpoint reprojected");
        close(renderEndpoints(center(),"front").get(Slot.UR).x(),8,"aligned X retains center endpoint");
        eq(renderEndpoints(center(),"front").size(),4,"no missing X endpoint");
        eq(select(List.of(new Port(Slot.UL,4.8,8,2.6,"back",true)),"front").mode(),Mode.CLEAN,"wrong face ignored");
        eq(select(List.of(new Port(Slot.UL,4.8,8,2.6,"front",false)),"front").mode(),Mode.CLEAN,"nonreciprocal ignored");
        eq(select(List.of(p(Slot.UL,4.8,8)),"front").armCount(),1,"single arm never creates phantom second arm");
        fail(()->select(List.of(p(Slot.UL,4.8,8),p(Slot.UL,4.8,8)),"front"),"duplicate port");
        fail(()->select(List.of(p(Slot.UL,4.8,8),p(Slot.LL,4.8,9)),"front"),"K pair must share one real connection height");
        fail(()->new Port(Slot.UL,17,8,2.6,"front",true),"out of bounds");
        fail(()->renderEndpoints(List.of(new Port(Slot.UL,4.8,8,3.4,"front",true)),"front"),"off-plane flange end");
        eq(select(List.of(p(Slot.UR,11.2,9),p(Slot.LR,11.2,9)),"front").sidePlateHeights().get(Side.RIGHT),9.0,"actual right attachment Y");
    }
    private static void orientations() {
        var unique=new HashSet<String>();
        for(KXOrientation ori:KXOrientation.all()) {
            var right=ori.right();var axis=ori.axis();var depth=ori.depth();
            eq(right.cross(axis),depth,"right-handed 3D basis");
            eq(right.dot(axis),0,"orthogonal axes");
            var local=new Vec3(4.8,9,2.6);
            var restored=ori.toLocal(ori.toWorld(local));
            close(restored.x(),local.x(),"rotation inverse x");
            close(restored.y(),local.y(),"rotation inverse y");
            close(restored.z(),local.z(),"rotation inverse z");
            unique.add(ori.facing()+"/"+right+"/"+depth);
        }
        eq(unique.size(),24,"exactly 24 proper unique orientations");
        fail(()->new KXOrientation(Face.UP,4),"roll 4 invalid");
        eq(new KXOrientation(Face.UP,0).front(),Face.NORTH.vector(),"face of first visible flange faces north");
    }
    private static void anchors() {
        var valid=new EnumMap<Face,Boolean>(Face.class);
        for(Face f:Face.values())valid.put(f,true);
        eq(KXTopology.anchorFaces(valid,Set.of()).size(),6,"all six qualified faces may anchor");
        eq(KXTopology.anchorFaces(valid,Set.of(Face.UP,Face.NORTH)),EnumSet.of(Face.DOWN,Face.SOUTH,Face.WEST,Face.EAST),"axial structural joints suppress plates");
        valid.put(Face.SOUTH,false);
        ok(!KXTopology.anchorFaces(valid,Set.of()).contains(Face.SOUTH),"invalid face stays bare");
    }
    private static Plan plan() {
        Plan p=KXPlacement.plan(o(),"front",List.of(new Span(new Cell(0,0,0),new Cell(4,4,0),Slot.UL,"front")));
        eq(p.uniqueCells(),3,"four-cell diagonal owns exactly 3 intermediate cells");
        eq(p.segments().keySet(),Set.of(new Cell(1,1,0),new Cell(2,2,0),new Cell(3,3,0)),"actual 45-degree cells");
        for(KXOrientation f:KXOrientation.all()) {
            Cell start=new Cell(2,3,4);
            Cell end=start.plus(f.worldDirection(4,4,0));
            eq(KXPlacement.plan(f,"front",List.of(new Span(start,end,Slot.UL,"front"))).uniqueCells(),3,"all orientations rasterize");
        }
        fail(()->KXPlacement.plan(o(),"front",List.of(new Span(new Cell(0,0,0),new Cell(3,4,0),Slot.UL,"front"))),"non-45-degree slope rejected");
        fail(()->KXPlacement.plan(o(),"front",List.of(new Span(new Cell(0,0,0),new Cell(3,3,1),Slot.UL,"front"))),"wrong plane rejected");
        fail(()->KXPlacement.plan(o(),"front",List.of(new Span(new Cell(0,0,0),new Cell(20,20,0),Slot.UL,"front"))),"unbounded span rejected");
        fail(()->KXPlacement.plan(o(),"front",List.of(new Span(new Cell(0,0,0),new Cell(1,1,0),Slot.UL,"front"))),"no owned intermediate cell rejected");
        fail(()->KXPlacement.plan(o(),"front",List.of(new Span(new Cell(0,0,0),new Cell(3,3,0),Slot.UL,"back"))),"different planes rejected");
        fail(()->KXPlacement.plan(o(),"front",List.of(new Span(new Cell(0,0,0),new Cell(3,3,0),Slot.UL,"front"),new Span(new Cell(0,0,0),new Cell(4,4,0),Slot.UL,"front"))),"duplicate arm rejected");
        Plan crossing=KXPlacement.plan(o(),"front",List.of(
            new Span(new Cell(-2,-2,0),new Cell(2,2,0),Slot.UL,"front"),
            new Span(new Cell(-2,2,0),new Cell(2,-2,0),Slot.UR,"front")));
        eq(crossing.uniqueCells(),5,"crossing segment cells deduplicated");
        eq(crossing.segments().get(new Cell(0,0,0)),Set.of(Slot.UL,Slot.UR),"crossing cell owns both real bars");
        return p;
    }
    private static final class MockWorld implements World, KXRuntimePlacement.SyncPort {
        final Map<Cell,Snapshot> cells=new HashMap<>();
        final Set<Cell> unloaded=new HashSet<>(),occupied=new HashSet<>();
        int items=10,writes=0,restores=0,failOn=0,denyAt=-1,refreshes=0,notifications=0;
        boolean creative=false,server=true,reciprocal=true,compat=true,restoreSuccess=true,failRefresh=false,failNotify=false;
        public boolean isServerThread(){return server;}
        public boolean isLoaded(Cell c){return !unloaded.contains(c);}
        public boolean isCompatiblePillar(Cell c,KXOrientation o){return compat;}
        public boolean isReciprocal(Span s,KXOrientation o){return reciprocal;}
        public boolean entitySpaceClear(Cell c){return !occupied.contains(c);}
        public Snapshot snapshot(Cell c){return cells.getOrDefault(c,new Snapshot(CellKind.AIR,null,null,Set.of(),null));}
        public boolean placeOrUpdate(Cell c,KXOrientation o,String plane,Set<Slot> slots){
            writes++;
            if(failOn==writes) return false;
            cells.put(c,new Snapshot(CellKind.OWN_SEGMENT,o,plane,slots,"snapshot"));return true;
        }
        public boolean restore(Cell c,Snapshot original){restores++;if(!restoreSuccess)return false;cells.put(c,original);return true;}
        public int itemsAvailable(){return items;}
        public boolean consumeItems(int count){if(denyAt==count)return false;items-=count;return true;}
        public boolean creativeMode(){return creative;}
        public void refreshTopology(Set<Cell> affected){refreshes++;if(failRefresh)throw new IllegalStateException("refresh failed");}
        public void notifyClients(Set<Cell> affected){notifications++;if(failNotify)throw new IllegalStateException("notify failed");}
    }
    private static void transaction(Plan plan) {
        MockWorld w=new MockWorld();
        Result r=apply(w,plan);
        eq(r.status(),Status.APPLIED,"happy path commits");eq(r.newCells(),3,"three new cells owned");eq(r.chargedItems(),3,"exact charge");
        eq(w.items,7,"inventory debited once after successful commit");
        eq(w.writes,3,"three world writes");
        eq(apply(w,plan).chargedItems(),0,"reapplying same segment does not double-charge");
        MockWorld creative=new MockWorld();creative.creative=true;eq(apply(creative,plan).chargedItems(),0,"creative costs zero");
        MockWorld insufficient=new MockWorld();insufficient.items=2;
        eq(apply(insufficient,plan).status(),Status.REJECTED,"insufficient items fail preflight");eq(insufficient.writes,0,"no partial write if inventory insufficient");
        MockWorld unloaded=new MockWorld();unloaded.unloaded.add(new Cell(2,2,0));
        eq(apply(unloaded,plan).status(),Status.REJECTED,"unloaded cell rejected");eq(unloaded.writes,0,"do not force-load chunks");
        MockWorld entity=new MockWorld();entity.occupied.add(new Cell(3,3,0));eq(apply(entity,plan).status(),Status.REJECTED,"entity collision rejected");
        MockWorld wrong=new MockWorld();wrong.reciprocal=false;eq(apply(wrong,plan).status(),Status.REJECTED,"missing reciprocal endpoint rejected");
        MockWorld notServer=new MockWorld();notServer.server=false;eq(apply(notServer,plan).status(),Status.REJECTED,"client forbidden to mutate");
        MockWorld foreign=new MockWorld();foreign.cells.put(new Cell(1,1,0),new Snapshot(CellKind.OCCUPIED,null,null,Set.of(),"stone"));
        eq(apply(foreign,plan).status(),Status.REJECTED,"foreign cell is not erased");
        MockWorld mismatch=new MockWorld();mismatch.cells.put(new Cell(1,1,0),new Snapshot(CellKind.OWN_SEGMENT,new KXOrientation(Face.EAST,2),"front",Set.of(Slot.UR),null));
        eq(apply(mismatch,plan).status(),Status.REJECTED,"misoriented segment not repurposed");
        MockWorld fail=new MockWorld();fail.failOn=2;
        eq(apply(fail,plan).status(),Status.ROLLED_BACK,"failed second write triggers rollback");
        eq(fail.items,10,"rollback consumes no items");eq(fail.restores,2,"rollback includes possibly changed failed cell");
        eq(fail.snapshot(new Cell(1,1,0)).kind(),CellKind.AIR,"first written cell restored");
        MockWorld failRestore=new MockWorld();failRestore.failOn=2;failRestore.restoreSuccess=false;
        eq(apply(failRestore,plan).status(),Status.RECOVERY_REQUIRED,"failed rollback declared unsafe");
        MockWorld failCharge=new MockWorld();failCharge.denyAt=3;
        eq(apply(failCharge,plan).status(),Status.ROLLED_BACK,"inventory failure triggers restoration");
        eq(failCharge.items,10,"no phantom charge from failed inventory");
    }
    private static void interaction() {
        KXInteraction service = new KXInteraction();
        UUID player=UUID.fromString("ae34b7c8-e021-426b-8bf1-74c90b5c6012");
        var a=new KXInteraction.Endpoint(new Cell(0,0,0),o(),Slot.UL,"front","test:overworld");
        var b=new KXInteraction.Endpoint(new Cell(4,4,0),o(),Slot.UL,"front","test:overworld");
        ok(!service.hasPending(player),"session is empty before first click");
        service.begin(player,a);
        ok(service.hasPending(player),"first port selected");
        eq(service.preview(player,b).uniqueCells(),3,"an explicitly matching source/target slot is representable");
        var ambiguous=new KXInteraction.Endpoint(new Cell(4,4,0),o(),Slot.LL,"front","test:overworld");
        fail(()->service.preview(player,ambiguous),"different source/target slots are rejected instead of guessed");

        MockWorld fail=new MockWorld();fail.items=0;
        var rejected=service.confirm(player,b,fail,fail);
        eq(rejected.placement().status(),Status.REJECTED,"failed confirmation is not silently applied");
        eq(rejected.sync(),KXRuntimePlacement.SyncState.NOT_NEEDED,"rejection does not request sync");
        eq(fail.refreshes,0,"rejected placement does not refresh topology");
        ok(service.hasPending(player),"failed preflight retains the selected source");

        MockWorld success=new MockWorld();
        var applied=service.confirm(player,b,success,success);
        eq(applied.placement().status(),Status.APPLIED,"confirmed placement succeeds");
        eq(applied.sync(),KXRuntimePlacement.SyncState.COMPLETED,"interaction uses runtime sync path");
        eq(success.refreshes,1,"successful interaction refreshes topology");
        eq(success.notifications,1,"successful interaction notifies clients");
        ok(!service.hasPending(player),"committed placement clears source selection");

        service.begin(player,a);
        MockWorld syncFailure=new MockWorld();syncFailure.failRefresh=true;
        var pending=service.confirm(player,b,syncFailure,syncFailure);
        eq(pending.placement().status(),Status.APPLIED,"sync failure does not undo committed placement");
        eq(pending.sync(),KXRuntimePlacement.SyncState.PENDING,"sync failure is visible to caller");
        eq(pending.placement().chargedItems(),3,"sync failure does not repeat or alter item charge");
        eq(syncFailure.notifications,1,"client notification is still attempted after topology refresh fails");
        ok(!service.hasPending(player),"sync-pending commit is not offered for duplicate retry");

        service.begin(player,a);
        MockWorld recovery=new MockWorld();recovery.failOn=1;recovery.restoreSuccess=false;
        var unsafe=service.confirm(player,b,recovery,recovery);
        eq(unsafe.placement().status(),Status.RECOVERY_REQUIRED,"incomplete rollback is reported");
        ok(!service.hasPending(player),"unsafe world state clears pending selection");

        service.begin(player,a);
        var wrongDim=new KXInteraction.Endpoint(new Cell(4,4,0),o(),Slot.UL,"front","test:nether");
        fail(()->service.preview(player,wrongDim),"cannot connect across dimensions");
        var wrongRoll=new KXInteraction.Endpoint(new Cell(4,4,0),new KXOrientation(Face.UP,1),Slot.UL,"front","test:overworld");
        fail(()->service.preview(player,wrongRoll),"cannot connect misrotated planes");
        service.cancel(player);ok(!service.hasPending(player),"cancel releases selection");
        service.begin(player,a);service.clear(player);
        ok(!service.hasPending(player),"logout/world transfer clears selection");
    }
    public static void main(String[] args) {
        topology();orientations();anchors();Plan plan=plan();transaction(plan);interaction();
        System.out.println("PASS KX Mechanics Java21: "+checks+" assertions — MK2 topology, rotations, face anchors, 45-degree plans, preflight and rollback");
    }
}
