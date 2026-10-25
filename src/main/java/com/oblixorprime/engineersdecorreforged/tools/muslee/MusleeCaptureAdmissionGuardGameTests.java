package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real-engine regression of the deliberate TOOLS-351 capture-admission safety gate. */
@GameTestHolder("immersive_engineer_decor_controls_tool_reforged")
@PrefixGameTestTemplate(false)
public final class MusleeCaptureAdmissionGuardGameTests {
    private MusleeCaptureAdmissionGuardGameTests() {}

    private static final class FixedClock implements MusleeCommandQueue.Clock {
        @Override public long nanoTime() { return 0L; }
    }

    private static MusleeCommandQueue newQueue() {
        return new MusleeCommandQueue(UUID.randomUUID(), new FixedClock());
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void valid_vanilla_captures_never_bypass_unqualified_memory_gate(GameTestHelper helper) {
        var lookup = helper.getLevel().registryAccess();
        UUID session = UUID.randomUUID();
        MusleeCommandQueue queue = newQueue();
        for (ItemStack ingredient : List.of(
                new ItemStack(Items.BREAD, 4),
                new ItemStack(Items.APPLE, 3),
                new ItemStack(Items.WHEAT_SEEDS, 2))) {
            ItemStack before = ingredient.copy();
            MusleeItemStackCapture capture = MusleeItemStackCapture.capture(ingredient, lookup).orElseThrow();
            helper.assertTrue(capture.encodedSize() > 0, "real vanilla ingredient was captured");
            var refused = queue.admitCapture(UUID.randomUUID(), session, true, capture);
            helper.assertTrue(refused.status == MusleeCommandQueue.AdmissionStatus.INVALID_BOUND,
                    "runtime admission must remain blocked without TOOLS-351 memory proof");
            helper.assertTrue(refused.ticket == null, "refusal cannot return an engaged or waiting ticket");
            helper.assertTrue(capture.isClosed(), "rejected unowned capture was cleared");
            helper.assertValueEqual(1, capture.releaseCount(), "clear must happen exactly once");
            capture.close();
            helper.assertValueEqual(1, capture.releaseCount(), "repeated close cannot double release");
            helper.assertTrue(ItemStack.matches(before, ingredient), "refusal did not change the source stack");
        }
        helper.assertValueEqual(0, queue.waitingCount(), "no captured command enqueued");
        helper.assertValueEqual(0L, queue.reservedBytes(), "no memory reservation created");
        helper.assertTrue(queue.active() == null, "no command engaged");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void nested_mutable_component_source_survives_refused_capture_admission(GameTestHelper helper) {
        var lookup = helper.getLevel().registryAccess();
        CompoundTag nested = new CompoundTag();
        nested.putInt("value", 42);
        CompoundTag data = new CompoundTag();
        data.put("nested", nested);
        ItemStack ingredient = new ItemStack(Items.BREAD, 2);
        ingredient.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        ItemStack before = ingredient.copy();
        MusleeItemStackCapture capture = MusleeItemStackCapture.capture(ingredient, lookup).orElseThrow();
        MusleeCommandQueue queue = newQueue();
        var refused = queue.admitCapture(UUID.randomUUID(), UUID.randomUUID(), true, capture);
        helper.assertTrue(refused.status == MusleeCommandQueue.AdmissionStatus.INVALID_BOUND,
                "even representable nested data cannot authorize admission");
        helper.assertTrue(capture.isClosed(), "nested candidate released");
        helper.assertTrue(capture.restore(lookup).isEmpty(), "released bytes cannot reconstruct a stack");
        helper.assertTrue(ItemStack.matches(before, ingredient), "nested source survived refusal exactly");
        helper.assertValueEqual(42,
                ingredient.get(DataComponents.CUSTOM_DATA).copyTag().getCompound("nested").getInt("value"),
                "source's nested data unchanged");
        helper.assertValueEqual(0L, queue.reservedBytes(), "no captured payload retained");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void scalar_fifo_capacity_does_not_enable_capture_admission(GameTestHelper helper) {
        var lookup = helper.getLevel().registryAccess();
        MusleeCommandQueue queue = newQueue();
        UUID session = UUID.randomUUID();
        for (int i = 0; i < MusleeCommandQueue.MAX_WAITING; i++) {
            var admitted = queue.admit(UUID.randomUUID(), session, true, 1L);
            helper.assertTrue(admitted.status == MusleeCommandQueue.AdmissionStatus.ADMITTED,
                    "existing scalar accounting fixture admitted one waiting command");
        }
        helper.assertValueEqual(MusleeCommandQueue.MAX_WAITING, queue.waitingCount(),
                "scalar waiting queue at capacity");
        MusleeItemStackCapture candidate = MusleeItemStackCapture.capture(
                new ItemStack(Items.APPLE, 1), lookup).orElseThrow();
        var blocked = queue.admitCapture(UUID.randomUUID(), session, true, candidate);
        helper.assertTrue(blocked.status == MusleeCommandQueue.AdmissionStatus.INVALID_BOUND,
                "even at full capacity runtime capture gate must remain closed");
        helper.assertTrue(candidate.isClosed(), "candidate rejected and released");
        helper.assertValueEqual(MusleeCommandQueue.MAX_WAITING, queue.waitingCount(),
                "rejected candidate did not change existing FIFO");
        helper.assertValueEqual((long) MusleeCommandQueue.MAX_WAITING, queue.reservedBytes(),
                "rejection did not change existing reservations");
        helper.assertValueEqual(MusleeCommandQueue.MAX_WAITING,
                queue.invalidateSession(session), "explicit cleanup removes only waiting work");
        helper.assertValueEqual(0, queue.waitingCount(), "explicit cleanup leaves queue empty");
        helper.assertValueEqual(0L, queue.reservedBytes(), "explicit cleanup releases reservations");
        MusleeItemStackCapture again = MusleeItemStackCapture.capture(
                new ItemStack(Items.WHEAT_SEEDS, 1), lookup).orElseThrow();
        helper.assertTrue(queue.admitCapture(UUID.randomUUID(), session, true, again).status
                == MusleeCommandQueue.AdmissionStatus.INVALID_BOUND,
                "free capacity does not magically qualify memory");
        helper.assertTrue(again.isClosed(), "fresh unqualified capture still released");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void foreign_queue_cannot_steal_already_owned_real_capture(GameTestHelper helper) {
        var lookup = helper.getLevel().registryAccess();
        MusleeCommandQueue ownerQueue = newQueue();
        MusleeCommandQueue foreignQueue = newQueue();
        UUID ownerSession = UUID.randomUUID();
        MusleeItemStackCapture capture = MusleeItemStackCapture.capture(
                new ItemStack(Items.BREAD, 3), lookup).orElseThrow();
        // Package-private fixture: numeric bytes are NOT a valid runtime memory proof.
        var fixture = ownerQueue.admitCaptureForOwnershipTest(
                UUID.randomUUID(), ownerSession, true, 128L, capture);
        helper.assertTrue(fixture.status == MusleeCommandQueue.AdmissionStatus.ADMITTED,
                "fixture creates an owned waiting capture for lifecycle testing only");
        capture.close();
        helper.assertTrue(!capture.isClosed(), "caller cannot close queue-owned capture");
        helper.assertTrue(foreignQueue.admitCapture(
                UUID.randomUUID(), UUID.randomUUID(), true, capture).status
                == MusleeCommandQueue.AdmissionStatus.INVALID_BOUND,
                "foreign runtime admission always refused");
        helper.assertTrue(!capture.isClosed(), "foreign refusal did not steal or release owned capture");
        helper.assertValueEqual(0, capture.releaseCount(), "owner still retains capture");
        helper.assertValueEqual(1, ownerQueue.invalidateSession(ownerSession),
                "owning queue explicitly invalidates waiting work");
        helper.assertTrue(capture.isClosed(), "actual owner released real capture");
        helper.assertValueEqual(1, capture.releaseCount(), "owned buffer released exactly once");
        helper.assertValueEqual(0L, ownerQueue.reservedBytes(), "owner reservation returned");
        helper.assertValueEqual(0L, foreignQueue.reservedBytes(), "foreign queue unchanged");
        helper.succeed();
    }
}
