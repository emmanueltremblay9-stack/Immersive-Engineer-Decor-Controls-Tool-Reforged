package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeWithdrawalPlanner.*;

public final class MusleeWithdrawalPlannerCases {
    private static void check(boolean ok) { if (!ok) throw new AssertionError("withdrawal contract"); }
    private static List<Slot> full() {
        List<Slot> s = new ArrayList<>();
        for (int i = 0; i < 36; i++) s.add(new Slot(i, 64, 64, false, true));
        return s;
    }
    public static void run() {
        List<Slot> s = full();
        s.set(0, new Slot(0, 0, 64, false, true));
        s.set(35, new Slot(35, 62, 64, true, true));
        Plan p = simulate(5, s).orElseThrow();
        check(p.moves().equals(List.of(new Move(35,2), new Move(0,3))));
        check(p.transferred() == 5 && s.get(35).count() == 62);
        s.set(35, new Slot(35, 64, 64, false, true));
        check(p.moves().get(0).count() == 2);
        try { p.moves().clear(); throw new AssertionError("mutable plan"); }
        catch (UnsupportedOperationException expected) { }
        s = full();
        s.set(1,new Slot(1,0,2,false,false));
        s.set(3,new Slot(3,0,2,false,true));
        s.set(7,new Slot(7,1,3,true,true));
        p = simulate(8,s).orElseThrow();
        check(p.transferred()==4 && p.moves().equals(List.of(new Move(7,2),new Move(3,2))));
        check(simulate(1,full()).isEmpty());
        check(simulate(0,s).isEmpty() && simulate(9,s).isEmpty() && simulate(1,null).isEmpty());
        check(simulate(1,List.of()).isEmpty());
        s.set(0,new Slot(1,0,1,true,true)); check(simulate(1,s).isEmpty());
        s.set(0,new Slot(0,2,1,true,true)); check(simulate(1,s).isEmpty());
        s.set(0,null); check(simulate(1,s).isEmpty());
        s=full(); s.set(0,new Slot(0,Integer.MAX_VALUE-1,Integer.MAX_VALUE,true,true));
        check(simulate(8,s).orElseThrow().transferred()==1);
        Random random=new Random(327328L);
        for(int round=0;round<4000;round++) {
            s=new ArrayList<>(); int free=0, available=1+random.nextInt(8);
            for(int i=0;i<36;i++) {
                int cap=random.nextInt(65), count=cap==0?0:random.nextInt(cap+1);
                boolean compatible=random.nextBoolean(), admits=random.nextBoolean();
                s.add(new Slot(i,count,cap,compatible,admits));
                if(admits && (count==0 || compatible)) free+=cap-count;
            }
            var result=simulate(available,s);
            check(result.isPresent()==(free>0));
            if(result.isEmpty()) continue;
            p=result.get(); int sum=0, phase=0, previous=-1; boolean[] seen=new boolean[36];
            for(Move m:p.moves()) {
                Slot slot=s.get(m.index()); int currentPhase=slot.count()==0?1:0;
                check(currentPhase>=phase);
                if(currentPhase!=phase) previous=-1;
                check(m.index()>previous && !seen[m.index()] && slot.admissible());
                check(slot.count()==0 || slot.compatible());
                check(m.count()>0 && m.count()<=slot.capacity()-slot.count());
                phase=currentPhase; previous=m.index(); seen[m.index()]=true; sum+=m.count();
            }
            check(sum==Math.min(available,free) && sum==p.transferred());
        }
    }
    public static void main(String[] args) { run(); System.out.println("PASS withdrawal: fixed regressions + 4000 deterministic layouts"); }
}
