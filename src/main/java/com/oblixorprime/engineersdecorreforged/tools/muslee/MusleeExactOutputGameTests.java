package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class MusleeExactOutputGameTests {
    private MusleeExactOutputGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void exact_output_matches_actual_nbt_writer_and_parser(GameTestHelper helper) throws IOException {
        CompoundTag root = new CompoundTag();
        root.putByte("byte", (byte) -128); root.putShort("short", (short) -32768);
        root.putInt("int", Integer.MIN_VALUE); root.putLong("long", Long.MIN_VALUE);
        root.putFloat("float", -0.0f); root.putDouble("double", -0.0);
        root.putString("string\u0000\ud800", "text\u0000\u07ff\u0800\ud83d\ude80\udfff");
        root.putByteArray("bytes", new byte[]{-1, 0, 127});
        root.putIntArray("ints", new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE});
        root.putLongArray("longs", new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE});
        ListTag list = new ListTag(); list.add(StringTag.valueOf("one")); list.add(StringTag.valueOf("two"));
        CompoundTag nested = new CompoundTag(); nested.put("list", list); root.put("nested", nested);
        ByteArrayOutputStream reference = new ByteArrayOutputStream();
        NbtIo.write(root, new DataOutputStream(reference));
        byte[] exact = MusleeExactDataOutput.encode(4096, output -> NbtIo.write(root, output));
        helper.assertTrue(Arrays.equals(reference.toByteArray(), exact), "actual NBT bytes match standard DataOutputStream");
        CompoundTag parsed = NbtIo.read(new DataInputStream(new ByteArrayInputStream(exact)), NbtAccounter.create(65536));
        helper.assertTrue(root.equals(parsed), "actual NBT parser preserves values, types and modified UTF");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void exact_output_enforces_nbt_length_before_payload_allocation(GameTestHelper helper) throws IOException {
        CompoundTag root = new CompoundTag(); root.putByteArray("bytes", new byte[64]);
        byte[] exact = MusleeExactDataOutput.encode(1024, output -> NbtIo.write(root, output));
        byte[] atLimit = MusleeExactDataOutput.encode(exact.length, output -> NbtIo.write(root, output));
        helper.assertTrue(Arrays.equals(exact, atLimit), "exact serialized limit accepted");
        boolean rejected = false;
        try { MusleeExactDataOutput.encode(exact.length - 1, output -> NbtIo.write(root, output)); }
        catch (IOException expected) { rejected = true; }
        helper.assertTrue(rejected, "one byte below exact NBT length refused");
        helper.succeed();
    }
}
