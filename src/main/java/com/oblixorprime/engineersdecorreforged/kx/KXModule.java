package com.oblixorprime.engineersdecorreforged.kx;

import com.oblixorprime.engineersdecorreforged.ModBlockEntities;
import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** One registry block, one BlockItem and one BlockEntity type for all eight K/X MK2 forms. */
public final class KXModule {
    public static final DeferredBlock<KXSupportBlock> KX_MK2_SUPPORT = ModBlocks.register(
        "kx_mk2_support",
        () -> new KXSupportBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL).strength(2.0F, 10.0F).sound(SoundType.METAL)
            .noOcclusion().requiresCorrectToolForDrops())
    );
    public static final DeferredItem<BlockItem> KX_MK2_SUPPORT_ITEM =
        ModItems.registerBlockItem("kx_mk2_support", KX_MK2_SUPPORT);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KXSupportBlockEntity>> KX_MK2_SUPPORT_ENTITY =
        ModBlockEntities.BLOCK_ENTITY_TYPES.register("kx_mk2_support",
            () -> BlockEntityType.Builder.of(KXSupportBlockEntity::new, KX_MK2_SUPPORT.get()).build(null));

    private KXModule() {}

    public static void initialize() {
        NeoForge.EVENT_BUS.addListener(KXSupportBlockEntity::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(KXSupportBlockEntity::onChunkUnload);
    }
}
