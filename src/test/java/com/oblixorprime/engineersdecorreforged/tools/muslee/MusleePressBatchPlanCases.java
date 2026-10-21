package com.oblixorprime.engineersdecorreforged.tools.muslee;

import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleePressBatchPlan.*;

public final class MusleePressBatchPlanCases {
    private static void check(boolean ok) { if(!ok) throw new AssertionError("press batch contract"); }
    public static void run() {
        check(evaluate(null).isEmpty());
        for(int b=0;b<=9;b++) for(int a=0;a<=9;a++) for(int s=0;s<=9;s++)
            for(int d=0;d<=65;d++) {
                Input input=new Input(b,a,s,true,d,4);
                var result=evaluate(input);
                boolean expected=b>=1&&b<=8&&a>=1&&a<=8&&s>=1&&s<=8&&d>=1&&d<=64;
                check(result.isPresent()==expected);
                if(result.isPresent()) {
                    Result p=result.get();
                    check(p.breadAfter()==b-1&&p.applesAfter()==a-1&&p.seedsAfter()==s-1);
                    check(p.durabilityAfter()==d-1&&p.barsProduced()==4&&p.clearSeedType()==(s==1));
                    check(input.bread()==b&&input.remainingDurability()==d);
                }
            }
        for(int room=-1;room<=8;room++) check(evaluate(new Input(8,8,8,true,64,room)).isPresent()==(room>=4));
        check(evaluate(new Input(1,1,1,false,64,64)).isEmpty());
        check(evaluate(new Input(1,1,1,true,128,64)).isEmpty());
        check(evaluate(new Input(-1,1,1,true,64,64)).isEmpty());
        check(evaluate(new Input(1,1,Integer.MAX_VALUE,true,64,64)).isEmpty());
        Result last=evaluate(new Input(1,1,1,true,1,4)).orElseThrow();
        check(last.durabilityAfter()==0&&last.clearSeedType()&&last.barsProduced()==4);
    }
    public static void main(String[] args) { run(); System.out.println("PASS press: 66000 state combinations + boundaries"); }
}
