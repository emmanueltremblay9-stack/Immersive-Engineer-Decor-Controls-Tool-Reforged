package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;

/** Digest primitive only: never structural validity, authenticity or execution proof. */
public final class MusleeJournalIntegrity {
    public static final int MAX_ENTRY_BYTES=262144;
    public static final int DIGEST_BYTES=32;
    private MusleeJournalIntegrity() {}

    /** Offset must be supplied by an independently validated envelope schema. */
    public static Optional<byte[]> checksum(byte[] entry, int digestOffset) {
        if(!bounded(entry,digestOffset)) return Optional.empty();
        return digest(entry.clone(),digestOffset);
    }
    /** Caller must also validate version, boundaries, identities and canonical payload. */
    public static boolean verifyIntegrityOnly(byte[] entry,int digestOffset) {
        if(!bounded(entry,digestOffset)) return false;
        byte[] snapshot=entry.clone();
        Optional<byte[]> calculated=digest(snapshot,digestOffset);
        return calculated.isPresent() && MessageDigest.isEqual(calculated.get(),
                Arrays.copyOfRange(snapshot,digestOffset,digestOffset+DIGEST_BYTES));
    }
    private static boolean bounded(byte[] entry,int offset) {
        return entry!=null && entry.length>=DIGEST_BYTES && entry.length<=MAX_ENTRY_BYTES
                && offset>=0 && offset<=entry.length-DIGEST_BYTES;
    }
    private static Optional<byte[]> digest(byte[] bytes,int offset) {
        try {
            MessageDigest hash=MessageDigest.getInstance("SHA-256");
            hash.update(bytes,0,offset);
            hash.update(bytes,offset+DIGEST_BYTES,bytes.length-offset-DIGEST_BYTES);
            return Optional.of(hash.digest());
        } catch(NoSuchAlgorithmException unavailable) { return Optional.empty(); }
    }
}
