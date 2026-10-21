package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.ArrayList;
import java.util.List;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeRepairPlan.*;

public final class MusleeRepairPlanCases {
    private static void check(boolean ok) { if(!ok) throw new AssertionError("repair contract"); }
    private static List<Cell> grid() {
        List<Cell> cells=new ArrayList<>();
        for(int i=0;i<9;i++) {
            Kind k=i==4?Kind.PRESS:(i==1||i==3||i==5||i==7)?Kind.IE_IRON_PLATE:Kind.EMPTY;
            cells.add(new Cell(k,k==Kind.EMPTY?0:1));
        }
        return cells;
    }
    public static void run() {
        for(int d=-1;d<=65;d++) {
            List<Cell> g=grid(), unchanged=List.copyOf(g);
            var r=evaluatePickup(true,g,d,true);
            check(r.isPresent()==(d>=0&&d<64) && g.equals(unchanged));
            if(r.isPresent()) check(r.get().equals(new Result(64,64-d,4,0,0)));
        }
        for(int i=0;i<9;i++) for(Kind k:Kind.values()) for(int count=0;count<=4;count++) {
            List<Cell> g=grid(); Cell expected=g.get(i); g.set(i,new Cell(k,count));
            check(evaluatePickup(true,g,0,true).isPresent()==expected.equals(g.get(i)));
        }
        check(evaluatePickup(false,grid(),0,true).isEmpty());
        check(evaluatePickup(true,grid(),0,false).isEmpty());
        check(evaluatePickup(true,null,0,true).isEmpty());
        check(evaluatePickup(true,List.of(),0,true).isEmpty());
        List<Cell> g=grid(); g.set(4,null); check(evaluatePickup(true,g,0,true).isEmpty());
        g=grid(); g.set(4,new Cell(null,1)); check(evaluatePickup(true,g,0,true).isEmpty());
        check(evaluatePickup(true,grid(),Integer.MIN_VALUE,true).isEmpty());
        check(evaluatePickup(true,grid(),Integer.MAX_VALUE,true).isEmpty());
    }
    public static void main(String[] args) { run(); System.out.println("PASS repair: durability domain, 180 grid substitutions and negative cases"); }
}
