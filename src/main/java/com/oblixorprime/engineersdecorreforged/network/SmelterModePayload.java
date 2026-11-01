package com.oblixorprime.engineersdecorreforged.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

/** v0.5 client intent only; the server binds it to the current validated Smelter menu. */
public record SmelterModePayload(int menuId, int expectedRevision, int modeId) implements CustomPacketPayload {
   public static final Type<SmelterModePayload> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("immersive_engineer_decor_controls_tool_reforged", "smelter_exact_v05/select_mode"));
   public static final StreamCodec<RegistryFriendlyByteBuf, SmelterModePayload> STREAM_CODEC =
      new StreamCodec<RegistryFriendlyByteBuf, SmelterModePayload>() {
         public SmelterModePayload decode(RegistryFriendlyByteBuf buffer) {
            return new SmelterModePayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
         }

         public void encode(RegistryFriendlyByteBuf buffer, SmelterModePayload payload) {
            buffer.writeVarInt(payload.menuId());
            buffer.writeVarInt(payload.expectedRevision());
            buffer.writeVarInt(payload.modeId());
         }
      };

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}