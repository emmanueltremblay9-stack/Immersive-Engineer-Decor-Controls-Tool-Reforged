package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.OptionalInt;

/** Pure configuration/rotation decisions; no configuration, file or admission is changed. */
public final class MusleeRotationPolicy {
    public static final int MIN_MIB=16, DEFAULT_MIB=32, MAX_MIB=128;
    public static final long MIB=1048576L, MAX_ENTRY_BYTES=262144L;
    public enum SelectionKind { DEFAULT, REQUESTED, KEEP_VALIDATED, SUSPEND }
    public enum AppendDecision { APPEND_CANDIDATE, ROTATE_FIRST, REFUSE }
    public record Selection(SelectionKind kind,OptionalInt effectiveMiB) {}
    private MusleeRotationPolicy() {}
    public static boolean valid(Integer mib) { return mib!=null && mib>=MIN_MIB && mib<=MAX_MIB; }

    /** Null request means omitted. A parser must reject non-integral explicit input upstream. */
    public static Selection select(Integer explicitMiB,Integer lastValidatedMiB) {
        if(explicitMiB==null) return new Selection(SelectionKind.DEFAULT,OptionalInt.of(DEFAULT_MIB));
        if(valid(explicitMiB)) return new Selection(SelectionKind.REQUESTED,OptionalInt.of(explicitMiB));
        if(valid(lastValidatedMiB)) return new Selection(SelectionKind.KEEP_VALIDATED,OptionalInt.of(lastValidatedMiB));
        return new Selection(SelectionKind.SUSPEND,OptionalInt.empty());
    }
    /** This assesses one complete new entry. It never splits or engages that entry. */
    public static AppendDecision assess(int thresholdMiB,long currentBytes,long completeEntryBytes) {
        if(!valid(thresholdMiB) || currentBytes<0 || completeEntryBytes<=0
                || completeEntryBytes>MAX_ENTRY_BYTES) return AppendDecision.REFUSE;
        final long prospective;
        try { prospective=Math.addExact(currentBytes,completeEntryBytes); }
        catch(ArithmeticException overflow) { return AppendDecision.REFUSE; }
        return prospective>=thresholdMiB*MIB?AppendDecision.ROTATE_FIRST:AppendDecision.APPEND_CANDIDATE;
    }
}
