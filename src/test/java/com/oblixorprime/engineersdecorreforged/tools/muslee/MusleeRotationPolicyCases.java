package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.OptionalInt;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeRotationPolicy.*;

public final class MusleeRotationPolicyCases {
    private static void check(boolean ok) { if(!ok) throw new AssertionError("rotation policy"); }
    public static void run() {
        check(select(null,128).equals(new Selection(SelectionKind.DEFAULT,OptionalInt.of(32))));
        for(int request=-1;request<=140;request++) for(Integer old:new Integer[]{null,-1,15,16,32,128,129}) {
            Selection result=select(request,old);
            if(request>=16 && request<=128) check(result.kind()==SelectionKind.REQUESTED && result.effectiveMiB().getAsInt()==request);
            else if(valid(old)) check(result.kind()==SelectionKind.KEEP_VALIDATED && result.effectiveMiB().getAsInt()==old);
            else check(result.kind()==SelectionKind.SUSPEND && result.effectiveMiB().isEmpty());
        }
        for(int threshold=16;threshold<=128;threshold++) for(long entry:new long[]{1,37,38,262144}) {
            long limit=threshold*MIB;
            check(assess(threshold,limit-entry-1,entry)==AppendDecision.APPEND_CANDIDATE);
            check(assess(threshold,limit-entry,entry)==AppendDecision.ROTATE_FIRST);
            check(assess(threshold,limit-entry+1,entry)==AppendDecision.ROTATE_FIRST);
            check(assess(threshold,limit,entry)==AppendDecision.ROTATE_FIRST);
        }
        check(assess(32,Long.MAX_VALUE,1)==AppendDecision.REFUSE);
        check(assess(32,-1,1)==AppendDecision.REFUSE);
        check(assess(32,0,0)==AppendDecision.REFUSE);
        check(assess(32,0,262145)==AppendDecision.REFUSE);
        check(assess(15,0,1)==AppendDecision.REFUSE && assess(129,0,1)==AppendDecision.REFUSE);
        check(assess(32,0,262144)==AppendDecision.APPEND_CANDIDATE);
        check(select(Integer.MAX_VALUE,Integer.MIN_VALUE).kind()==SelectionKind.SUSPEND);
    }
    public static void main(String[] args) { run(); System.out.println("PASS rotation: 994 configuration pairs, 1808 threshold boundaries and overflow cases"); }
}
