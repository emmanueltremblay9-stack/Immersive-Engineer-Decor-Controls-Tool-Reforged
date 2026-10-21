package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import static com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeJournalIntegrity.*;

public final class MusleeJournalIntegrityCases {
    private static void check(boolean ok) { if(!ok) throw new AssertionError("digest contract"); }
    public static void run() {
        byte[] known=new byte[35];
        System.arraycopy("abc".getBytes(StandardCharsets.US_ASCII),0,known,0,3);
        byte[] expected=HexFormat.of().parseHex("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        check(Arrays.equals(checksum(known,3).orElseThrow(),expected));
        System.arraycopy(expected,0,known,3,32); check(verifyIntegrityOnly(known,3));
        byte[] original=known.clone(); checksum(known,3); verifyIntegrityOnly(known,3); check(Arrays.equals(original,known));
        known[3]^=1; check(!verifyIntegrityOnly(known,3));
        check(Arrays.equals(checksum(known,3).orElseThrow(),expected));
        for(int offset=0;offset<=48;offset++) {
            byte[] entry=new byte[80];
            for(int i=0;i<entry.length;i++) entry[i]=(byte)(i*17+3);
            byte[] sum=checksum(entry,offset).orElseThrow();
            System.arraycopy(sum,0,entry,offset,32); check(verifyIntegrityOnly(entry,offset));
            for(int i=0;i<entry.length;i++) {
                byte[] changed=entry.clone(); changed[i]^=1;
                check(!verifyIntegrityOnly(changed,offset));
            }
        }
        byte[] max=new byte[MAX_ENTRY_BYTES];
        System.arraycopy(checksum(max,max.length-32).orElseThrow(),0,max,max.length-32,32);
        check(verifyIntegrityOnly(max,max.length-32));
        for(int offset:new int[]{-1,Integer.MIN_VALUE,Integer.MAX_VALUE,4}) check(checksum(new byte[35],offset).isEmpty());
        check(checksum(null,0).isEmpty() && checksum(new byte[31],0).isEmpty());
        check(checksum(new byte[MAX_ENTRY_BYTES+1],0).isEmpty());
        check(!verifyIntegrityOnly(new byte[MAX_ENTRY_BYTES+1],0));
        // Integrity of a synthetic buffer says nothing about a real journal's schema.
    }
    public static void main(String[] args) { run(); System.out.println("PASS integrity: SHA-256 known vector, 3920 tamper cases and bounds"); }
}
