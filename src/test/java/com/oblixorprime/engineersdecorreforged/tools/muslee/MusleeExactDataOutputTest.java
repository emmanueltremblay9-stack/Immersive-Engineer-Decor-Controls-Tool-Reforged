package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class MusleeExactDataOutputTest {
    private static byte[] reference(MusleeExactDataOutput.Writer writer) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        writer.write(new DataOutputStream(bytes));
        return bytes.toByteArray();
    }
    private static byte[] buffer(DataOutput output) {
        try {
            Field field = MusleeExactDataOutput.class.getDeclaredField("buffer");
            field.setAccessible(true);
            return (byte[]) field.get(output);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }
    private static void assertZero(byte[] bytes) {
        assertNotNull(bytes);
        for (byte value : bytes) assertEquals(0, value);
    }

    @Test public void everyPrimitiveAndStringMethodMatchesDataOutputStream() throws Exception {
        MusleeExactDataOutput.Writer writer = output -> {
            output.write(0x123); output.writeBoolean(true); output.writeBoolean(false);
            output.writeByte(-128); output.writeShort(0x89ab); output.writeChar(0xdef0);
            output.writeInt(Integer.MIN_VALUE); output.writeInt(Integer.MAX_VALUE);
            output.writeLong(Long.MIN_VALUE); output.writeLong(Long.MAX_VALUE);
            output.writeFloat(-0.0f); output.writeFloat(Float.intBitsToFloat(0x7fc01234));
            output.writeDouble(-0.0); output.writeDouble(Double.longBitsToDouble(0x7ff8000000001234L));
            output.writeBytes("A\u0000\u00ff\u1234"); output.writeChars("A\u0000\ud83d\ude80");
            output.write(new byte[]{1, 2, 3}); output.write(new byte[]{4, 5, 6, 7}, 1, 2);
            output.writeUTF("plain\u0000\u007f\u0080\u07ff\u0800\uffff\ud83d\ude80\ud800\udfff");
        };
        assertArrayEquals(reference(writer), MusleeExactDataOutput.encode(1024, writer));
    }

    @Test public void modifiedUtfMatchesEveryUtf16CodeUnit() throws Exception {
        for (int block = 0; block < 64; block++) {
            char[] chars = new char[1024];
            for (int i = 0; i < chars.length; i++) chars[i] = (char) (block * chars.length + i);
            String text = new String(chars);
            MusleeExactDataOutput.Writer writer = output -> output.writeUTF(text);
            assertArrayEquals(reference(writer), MusleeExactDataOutput.encode(4096, writer));
        }
    }

    @Test public void randomPrimitiveStreamsAreBitExact() throws Exception {
        Random random = new Random(351);
        for (int sample = 0; sample < 64; sample++) {
            long value = random.nextLong();
            int bits = random.nextInt();
            MusleeExactDataOutput.Writer writer = output -> {
                output.writeLong(value); output.writeInt(bits);
                output.writeFloat(Float.intBitsToFloat(bits));
                output.writeDouble(Double.longBitsToDouble(value));
            };
            assertArrayEquals(reference(writer), MusleeExactDataOutput.encode(24, writer));
        }
    }

    @Test public void exactCapacityUsesOneArrayTransferredWithoutFinalCopy() throws Exception {
        byte[][] observed = new byte[1][];
        AtomicInteger passes = new AtomicInteger();
        byte[] result = MusleeExactDataOutput.encode(4, output -> {
            if (passes.incrementAndGet() == 1) assertNull(buffer(output));
            else observed[0] = buffer(output);
            output.writeInt(123);
        });
        assertEquals(2, passes.get());
        assertEquals(4, result.length);
        assertSame(observed[0], result);
    }

    @Test public void countingPassRejectsBeforeAnyPayloadArrayOrSecondPass() throws Exception {
        AtomicInteger passes = new AtomicInteger();
        try {
            MusleeExactDataOutput.encode(3, output -> {
                passes.incrementAndGet(); assertNull(buffer(output)); output.writeInt(1);
            });
            fail("Oversize must fail");
        } catch (IOException expected) { assertEquals(1, passes.get()); }
    }

    @Test public void negativeBoundDoesNotCallWriter() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        try { MusleeExactDataOutput.encode(-1, output -> calls.incrementAndGet()); fail(); }
        catch (IllegalArgumentException expected) { assertEquals(0, calls.get()); }
    }

    @Test public void zeroLengthEncodingWorksWithoutSpareCapacity() throws Exception {
        assertEquals(0, MusleeExactDataOutput.encode(0, output -> {}).length);
        try { MusleeExactDataOutput.encode(0, output -> output.writeByte(1)); fail(); }
        catch (IOException expected) { }
    }

    @Test public void utfUnsignedShortBoundaryMatchesReference() throws Exception {
        String maximum = "x".repeat(65535);
        MusleeExactDataOutput.Writer writer = output -> output.writeUTF(maximum);
        assertArrayEquals(reference(writer), MusleeExactDataOutput.encode(65537, writer));
        try { MusleeExactDataOutput.encode(100000, output -> output.writeUTF(maximum + "x")); fail(); }
        catch (UTFDataFormatException expected) { }
        try { MusleeExactDataOutput.encode(100000, output -> output.writeUTF("\u0000".repeat(32768))); fail(); }
        catch (UTFDataFormatException expected) { }
    }

    @Test public void shorterSecondPassFailsAndZerosWrittenPayload() throws Exception {
        AtomicInteger passes = new AtomicInteger(); byte[][] observed = new byte[1][];
        try {
            MusleeExactDataOutput.encode(4, output -> {
                if (passes.incrementAndGet() == 1) output.writeInt(0);
                else { observed[0] = buffer(output); output.writeByte(99); }
            });
            fail("Size mismatch must fail");
        } catch (IOException expected) { assertZero(observed[0]); }
    }

    @Test public void largerSecondPassFailsAndZerosWrittenPayload() throws Exception {
        AtomicInteger passes = new AtomicInteger(); byte[][] observed = new byte[1][];
        try {
            MusleeExactDataOutput.encode(8, output -> {
                if (passes.incrementAndGet() == 1) output.writeByte(0);
                else { observed[0] = buffer(output); output.writeByte(99); output.writeByte(100); }
            });
            fail("Exact allocation must not grow");
        } catch (IOException expected) { assertZero(observed[0]); }
    }

    @Test public void exceptionalSecondPassClearsBufferAndSealsEscapedWriter() throws Exception {
        AtomicInteger passes = new AtomicInteger(); byte[][] observed = new byte[1][];
        DataOutput[] escaped = new DataOutput[1];
        try {
            MusleeExactDataOutput.encode(4, output -> {
                if (passes.incrementAndGet() == 1) output.writeInt(0);
                else {
                    observed[0] = buffer(output); escaped[0] = output;
                    output.writeInt(-1); throw new IllegalStateException("test");
                }
            });
            fail();
        } catch (IllegalStateException expected) { assertZero(observed[0]); }
        assertNull(buffer(escaped[0]));
        try { escaped[0].writeByte(1); fail(); } catch (IOException expected) { }
    }

    @Test public void successfulOwnershipTransferSealsBothEscapedOutputs() throws Exception {
        DataOutput[] escaped = new DataOutput[2]; AtomicInteger passes = new AtomicInteger();
        byte[] result = MusleeExactDataOutput.encode(1, output -> {
            escaped[passes.getAndIncrement()] = output; output.writeByte(7);
        });
        for (DataOutput output : escaped) {
            assertNull(buffer(output));
            try { output.writeByte(9); fail(); } catch (IOException expected) { }
        }
        assertArrayEquals(new byte[]{7}, result);
    }

    @Test public void invalidArrayRangesCannotAdvanceEncoding() throws Exception {
        for (int[] range : new int[][]{{-1, 1}, {0, -1}, {1, Integer.MAX_VALUE}, {3, 1}}) {
            try {
                MusleeExactDataOutput.encode(8, output -> output.write(new byte[3], range[0], range[1]));
                fail();
            } catch (IndexOutOfBoundsException expected) { }
        }
    }

    @Test public void canonicalNbtStylePayloadIsBitwiseDataOutputCompatible() throws Exception {
        MusleeExactDataOutput.Writer writer = output -> {
            output.writeByte(10); output.writeUTF("");
            output.writeByte(3); output.writeUTF("count"); output.writeInt(8);
            output.writeByte(8); output.writeUTF("id"); output.writeUTF("minecraft:bread");
            output.writeByte(9); output.writeUTF("values"); output.writeByte(1); output.writeInt(3);
            output.write(new byte[]{4, 5, 6}); output.writeByte(0);
        };
        assertArrayEquals(reference(writer), MusleeExactDataOutput.encode(1024, writer));
    }
}
