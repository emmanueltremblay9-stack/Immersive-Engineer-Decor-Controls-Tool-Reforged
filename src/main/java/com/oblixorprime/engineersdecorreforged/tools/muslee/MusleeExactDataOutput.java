package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.io.DataOutput;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Two-pass, exact-capacity binary output. The counting pass allocates no payload
 * buffer; the writing pass owns one exact-length byte array, with no growth or
 * final copy. This bounds only output storage, NOT the writer's allocations or
 * any ItemStack/NBT graph, metadata, validation copies or total JVM footprint.
 */
final class MusleeExactDataOutput implements DataOutput {
    @FunctionalInterface
    interface Writer { void write(DataOutput output) throws IOException; }

    private final int limit;
    private byte[] buffer;
    private int position;
    private boolean finished;

    private MusleeExactDataOutput(int limit, byte[] buffer) {
        this.limit = limit;
        this.buffer = buffer;
    }

    /**
     * Writer must synchronously encode the same immutable logical input in both passes.
     * It and its DataOutput are thread-confined: never publish the output to another
     * thread or perform asynchronous writes. Sealing is not a concurrency barrier.
     * Equal-length content changes are not detected; deterministic input is a caller
     * precondition, not a property established by the two-pass length check.
     */
    static byte[] encode(int maximumBytes, Writer writer) throws IOException {
        if (maximumBytes < 0) throw new IllegalArgumentException("Negative output bound");
        Objects.requireNonNull(writer, "writer");
        MusleeExactDataOutput counter = new MusleeExactDataOutput(maximumBytes, null);
        int size;
        try {
            writer.write(counter);
            size = counter.position;
        } finally {
            counter.finished = true;
        }
        MusleeExactDataOutput output = new MusleeExactDataOutput(size, new byte[size]);
        try {
            writer.write(output);
            if (output.position != size) throw new IOException("Encoding size changed between passes");
            byte[] result = output.buffer;
            output.buffer = null; // exclusive ownership moves to the caller
            return result;
        } finally {
            output.finished = true;
            if (output.buffer != null) {
                Arrays.fill(output.buffer, (byte) 0);
                output.buffer = null;
            }
        }
    }

    private int reserve(int amount) throws IOException {
        if (finished) throw new IOException("Output already finished");
        if (amount < 0 || amount > limit - position) throw new IOException("Encoding exceeds exact bound");
        int start = position;
        position += amount;
        return start;
    }

    private void number(long value, int width) throws IOException {
        int start = reserve(width);
        if (buffer != null) {
            for (int i = width - 1; i >= 0; i--) {
                buffer[start + i] = (byte) value;
                value >>>= 8;
            }
        }
    }

    @Override public void write(int value) throws IOException { number(value, 1); }
    @Override public void write(byte[] value) throws IOException { write(value, 0, value.length); }
    @Override public void write(byte[] value, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, value.length);
        int start = reserve(length);
        if (buffer != null) System.arraycopy(value, offset, buffer, start, length);
    }
    @Override public void writeBoolean(boolean value) throws IOException { number(value ? 1 : 0, 1); }
    @Override public void writeByte(int value) throws IOException { number(value, 1); }
    @Override public void writeShort(int value) throws IOException { number(value, 2); }
    @Override public void writeChar(int value) throws IOException { number(value, 2); }
    @Override public void writeInt(int value) throws IOException { number(value, 4); }
    @Override public void writeLong(long value) throws IOException { number(value, 8); }
    @Override public void writeFloat(float value) throws IOException { number(Float.floatToIntBits(value), 4); }
    @Override public void writeDouble(double value) throws IOException { number(Double.doubleToLongBits(value), 8); }
    @Override public void writeBytes(String value) throws IOException {
        int start = reserve(value.length());
        if (buffer != null) for (int i = 0; i < value.length(); i++) buffer[start + i] = (byte) value.charAt(i);
    }
    @Override public void writeChars(String value) throws IOException {
        long bytes = 2L * value.length();
        if (bytes > Integer.MAX_VALUE) throw new IOException("Character encoding too large");
        int start = reserve((int) bytes);
        if (buffer != null) {
            for (int i = 0; i < value.length(); i++) {
                char character = value.charAt(i);
                buffer[start + 2 * i] = (byte) (character >>> 8);
                buffer[start + 2 * i + 1] = (byte) character;
            }
        }
    }

    /** Standard DataOutput modified UTF-8, directly emitted without a scratch array. */
    @Override public void writeUTF(String value) throws IOException {
        int bytes = 0;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            bytes += character >= 1 && character <= 127 ? 1 : character <= 2047 ? 2 : 3;
            if (bytes > 65535) throw new UTFDataFormatException("Modified UTF-8 exceeds unsigned-short length");
        }
        int start = reserve(bytes + 2);
        if (buffer == null) return;
        buffer[start++] = (byte) (bytes >>> 8);
        buffer[start++] = (byte) bytes;
        for (int i = 0; i < value.length(); i++) {
            int character = value.charAt(i);
            if (character >= 1 && character <= 127) {
                buffer[start++] = (byte) character;
            } else if (character <= 2047) {
                buffer[start++] = (byte) (0xc0 | character >>> 6);
                buffer[start++] = (byte) (0x80 | character & 0x3f);
            } else {
                buffer[start++] = (byte) (0xe0 | character >>> 12);
                buffer[start++] = (byte) (0x80 | character >>> 6 & 0x3f);
                buffer[start++] = (byte) (0x80 | character & 0x3f);
            }
        }
    }
}
