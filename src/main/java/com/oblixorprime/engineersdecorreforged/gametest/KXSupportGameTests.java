package com.oblixorprime.engineersdecorreforged.gametest;

import com.mojang.authlib.GameProfile;
import com.oblixorprime.engineersdecorreforged.EngineersDecorReforged;
import com.oblixorprime.engineersdecorreforged.kx.KXModule;
import com.oblixorprime.engineersdecorreforged.kx.KXOrientation;
import com.oblixorprime.engineersdecorreforged.kx.KXSupportBlock;
import com.oblixorprime.engineersdecorreforged.kx.KXSupportBlockEntity;
import com.oblixorprime.engineersdecorreforged.kx.KXTopology;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(EngineersDecorReforged.MOD_ID)
@PrefixGameTestTemplate(false)
public final class KXSupportGameTests {
    private static final BlockPos P = new BlockPos(3, 4, 3);
    private static final KXOrientation UPRIGHT = new KXOrientation(KXOrientation.Face.UP, 0);

    private KXSupportGameTests() {}

    private static FakePlayer player(GameTestHelper helper, BlockPos pos) {
        FakePlayer player = new FakePlayer(helper.getLevel(),
            new GameProfile(UUID.randomUUID(), "KXSupportGameTests"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + .5, pos.getY() + .5 - player.getEyeHeight(), pos.getZ() - 2);
        return player;
    }

    private static void place(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlock(pos,
            KXSupportBlock.stateForOrientation(KXModule.KX_MK2_SUPPORT.get().defaultBlockState(), UPRIGHT),
            Block.UPDATE_ALL);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof KXSupportBlockEntity,
            "one registered K/X block creates its canonical BlockEntity");
    }

    private static void clearArea(GameTestHelper helper, BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
            helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static BlockPos peerPos(BlockPos source, KXTopology.Slot slot) {
        return switch (slot) {
            case UL -> source.offset(-1, 1, 0);
            case LL -> source.offset(-1, -1, 0);
            case UR -> source.offset(1, 1, 0);
            case LR -> source.offset(1, -1, 0);
        };
    }

    private static KXSupportBlockEntity.PortSpec port(KXTopology.Slot slot, double x, double y) {
        return new KXSupportBlockEntity.PortSpec(slot, x, y, 2.6, "front");
    }

    /**
     * Test fixture supplies BOTH endpoint slots and coordinates explicitly.
     * The chosen fixture mapping is not a production pairing rule.
     */
    private static KXTopology.Selection scenario(GameTestHelper helper,
            KXTopology.Slot[] slots, double[][] sourcePoints) {
        clearArea(helper, helper.absolutePos(P));
        BlockPos sourcePos = helper.absolutePos(P);
        place(helper, sourcePos);
        for (int i = 0; i < slots.length; i++) {
            BlockPos peerPos = peerPos(sourcePos, slots[i]);
            place(helper, peerPos);
            KXSupportBlockEntity.PortSpec sourcePort = port(slots[i], sourcePoints[i][0], sourcePoints[i][1]);
            KXSupportBlockEntity.PortSpec peerPort = port(KXTopology.Slot.LR, 11.2, 8.0);
            var result = KXSupportBlockEntity.recordExplicitConnection(helper.getLevel(),
                sourcePos, sourcePort, peerPos, peerPort);
            helper.assertTrue(result.applied(), "explicit reciprocal edge accepted: " + result.reason());
        }
        return ((KXSupportBlockEntity) helper.getLevel().getBlockEntity(sourcePos)).selection();
    }

    private static double[][] points(double x, double y, int count) {
        double[][] result = new double[count][2];
        for (int i = 0; i < count; i++) result[i] = new double[] { x, y };
        return result;
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void one_registered_block_item_places_and_exposes_all_24_rotations(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos floor = helper.absolutePos(P);
        BlockPos target = floor.above();
        level.setBlock(floor, Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        FakePlayer player = player(helper, target);
        ItemStack stack = new ItemStack(KXModule.KX_MK2_SUPPORT_ITEM.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, .5, 0),
            Direction.UP, floor, false);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));

        helper.assertTrue(level.getBlockState(target).is(KXModule.KX_MK2_SUPPORT.get()),
            "the single K/X MK2 BlockItem places the registered block");
        helper.assertValueEqual(stack.getCount(), 1, "ordinary placement consumes one block item");
        helper.assertValueEqual(KXModule.KX_MK2_SUPPORT.getId().getPath(), "kx_mk2_support",
            "stable block registry path");
        helper.assertValueEqual(KXModule.KX_MK2_SUPPORT_ITEM.getId().getPath(), "kx_mk2_support",
            "one item shares the stable block registry path");
        helper.assertValueEqual(((KXSupportBlockEntity) level.getBlockEntity(target)).selection().assetKey(),
            "anchor_down_only", "a verified solid support below selects the anchor-only R3 form");

        Set<String> states = new HashSet<>();
        for (KXOrientation orientation : KXOrientation.all()) {
            var state = KXSupportBlock.stateForOrientation(KXModule.KX_MK2_SUPPORT.get().defaultBlockState(), orientation);
            helper.assertValueEqual(KXSupportBlock.orientation(state), orientation,
                "BlockState round-trips the six axes and four rolls");
            states.add(state.getValue(KXSupportBlock.FACING).name() + "/" + state.getValue(KXSupportBlock.ROLL));
            var rotated = state;
            for (int turn = 0; turn < 4; turn++) rotated = rotated.rotate(Rotation.CLOCKWISE_90);
            helper.assertValueEqual(rotated, state, "four quarter-turns restore every 24-way orientation");
        }
        helper.assertValueEqual(states.size(), 24, "exactly 24 unique proper orientations are representable");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void eight_r3_forms_are_derived_from_verified_reciprocal_connections(GameTestHelper helper) {
        clearArea(helper, helper.absolutePos(P));
        place(helper, helper.absolutePos(P));
        helper.assertValueEqual(((KXSupportBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(P)))
            .selection().assetKey(), "clean", "no connections means clean, never a fabricated arm");

        clearArea(helper, helper.absolutePos(P));
        place(helper, helper.absolutePos(P));
        helper.getLevel().setBlock(helper.absolutePos(P).below(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertValueEqual(((KXSupportBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(P)))
            .selection().assetKey(), "anchor_down_only", "down anchor requires actual sturdy support");

        KXTopology.Selection selection = scenario(helper,
            new KXTopology.Slot[] { KXTopology.Slot.UL, KXTopology.Slot.LL }, points(4.8, 8, 2));
        helper.assertValueEqual(selection.assetKey(), "k_left_side", "two reciprocal left ports select K-left");

        selection = scenario(helper,
            new KXTopology.Slot[] { KXTopology.Slot.UR, KXTopology.Slot.LR }, points(11.2, 8, 2));
        helper.assertValueEqual(selection.assetKey(), "k_right_side", "two reciprocal right ports select K-right");

        selection = scenario(helper, new KXTopology.Slot[] {
            KXTopology.Slot.UL, KXTopology.Slot.LL, KXTopology.Slot.UR, KXTopology.Slot.LR
        }, new double[][] { {4.8,8}, {4.8,8}, {11.2,8}, {11.2,8} });
        helper.assertValueEqual(selection.assetKey(), "k_both_sides", "same-height K pairs select both side plates");

        selection = scenario(helper, new KXTopology.Slot[] {
            KXTopology.Slot.UL, KXTopology.Slot.LL, KXTopology.Slot.UR, KXTopology.Slot.LR
        }, new double[][] { {4.8,8}, {4.8,8}, {11.2,9}, {11.2,9} });
        helper.assertValueEqual(selection.assetKey(), "k_both_staggered", "different real side heights select staggered K");

        selection = scenario(helper, new KXTopology.Slot[] {
            KXTopology.Slot.UL, KXTopology.Slot.LL, KXTopology.Slot.UR, KXTopology.Slot.LR
        }, points(8, 8, 4));
        helper.assertValueEqual(selection.assetKey(), "x_centered", "four reciprocal center ports select X");

        selection = scenario(helper, new KXTopology.Slot[] {
            KXTopology.Slot.UL, KXTopology.Slot.LL, KXTopology.Slot.UR
        }, points(8, 8, 3));
        helper.assertValueEqual(selection.assetKey(), "x_broken_three", "three real center ports select broken-X without a fourth arm");
        helper.assertValueEqual(selection.armCount(), 3, "broken X carries only its three actual arms");
        helper.assertFalse(selection.centerPlate(), "broken X has no central plate");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void reciprocal_state_survives_save_reload_and_model_data_sync(GameTestHelper helper) {
        clearArea(helper, helper.absolutePos(P));
        BlockPos sourcePos = helper.absolutePos(P);
        place(helper, sourcePos);
        for (KXTopology.Slot slot : KXTopology.Slot.values()) {
            BlockPos peerPos = peerPos(sourcePos, slot);
            place(helper, peerPos);
            var result = KXSupportBlockEntity.recordExplicitConnection(helper.getLevel(),
                sourcePos, port(slot, 8, 8), peerPos, port(KXTopology.Slot.LR, 11.2, 8));
            helper.assertTrue(result.applied(), "explicit test endpoint pair accepted");
        }
        var source = (KXSupportBlockEntity) helper.getLevel().getBlockEntity(sourcePos);
        helper.assertValueEqual(source.selection().assetKey(), "x_centered", "canonical reciprocal graph selects X before save");
        CompoundTag saved = source.saveWithoutMetadata(helper.getLevel().registryAccess());
        source.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertValueEqual(source.connectionCount(), 4, "four reciprocal edges survive disk NBT round trip");
        helper.assertValueEqual(source.selection().assetKey(), "x_centered", "topology is recomputed from restored canonical edges");

        CompoundTag update = source.getUpdateTag(helper.getLevel().registryAccess());
        var clientCopy = new KXSupportBlockEntity(sourcePos, helper.getLevel().getBlockState(sourcePos));
        clientCopy.handleUpdateTag(update, helper.getLevel().registryAccess());
        var synchronizedSelection = clientCopy.getModelData().get(KXSupportBlockEntity.MODEL_SELECTION);
        helper.assertTrue(synchronizedSelection != null, "update tag contains renderer-facing derived state");
        helper.assertValueEqual(synchronizedSelection.assetKey(), "x_centered", "client projection matches server topology");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void one_sided_or_removed_peer_never_creates_a_ghost_connection(GameTestHelper helper) {
        clearArea(helper, helper.absolutePos(P));
        BlockPos sourcePos = helper.absolutePos(P);
        BlockPos peerPos = sourcePos.offset(-1, 1, 0);
        place(helper, sourcePos);
        place(helper, peerPos);
        var result = KXSupportBlockEntity.recordExplicitConnection(helper.getLevel(),
            sourcePos, port(KXTopology.Slot.UL, 4.8, 8),
            peerPos, port(KXTopology.Slot.LR, 11.2, 8));
        helper.assertTrue(result.applied(), "explicit reciprocal test link is established");

        var source = (KXSupportBlockEntity) helper.getLevel().getBlockEntity(sourcePos);
        var peer = (KXSupportBlockEntity) helper.getLevel().getBlockEntity(peerPos);
        helper.assertValueEqual(source.selection().assetKey(), "k_left_side", "reciprocal link creates one real arm");

        // Simulate a stale/one-sided saved edge: the peer's canonical list is absent.
        peer.loadWithComponents(new CompoundTag(), helper.getLevel().registryAccess());
        helper.assertValueEqual(source.selection().assetKey(), "clean",
            "one-sided record is ignored instead of creating a ghost arm");

        helper.getLevel().setBlock(peerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        helper.assertValueEqual(source.selection().assetKey(), "clean",
            "removing a peer cannot leave a phantom K/X connection");
        helper.succeed();
    }
}
