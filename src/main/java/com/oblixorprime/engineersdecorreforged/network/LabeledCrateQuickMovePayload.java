package com.oblixorprime.engineersdecorreforged.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record LabeledCrateQuickMovePayload(int containerId, int slotId) implements CustomPacketPayload {
   public static final Type<LabeledCrateQuickMovePayload> TYPE = new Type(
      ResourceLocation.fromNamespaceAndPath("immersive_engineer_decor_controls_tool_reforged", "labeled_crate_quick_move")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, LabeledCrateQuickMovePayload> STREAM_CODEC =
      new StreamCodec<RegistryFriendlyByteBuf, LabeledCrateQuickMovePayload>() {
         @Override
         public LabeledCrateQuickMovePayload decode(RegistryFriendlyByteBuf buffer) {
            return new LabeledCrateQuickMovePayload(buffer.readVarInt(), buffer.readVarInt());
         }

         @Override
         public void encode(RegistryFriendlyByteBuf buffer, LabeledCrateQuickMovePayload payload) {
            buffer.writeVarInt(payload.containerId());
            buffer.writeVarInt(payload.slotId());
         }
      };

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}