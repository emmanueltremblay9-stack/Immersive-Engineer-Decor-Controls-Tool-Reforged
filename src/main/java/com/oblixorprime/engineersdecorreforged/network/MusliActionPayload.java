package com.oblixorprime.engineersdecorreforged.network;

import com.oblixorprime.engineersdecorreforged.tools.muslee.MusleeCommandProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** UUIDv4 session and command intent only; zero inventory or item fields. */
public record MusliActionPayload(int containerId, byte[] encoded) implements CustomPacketPayload {
   public static final Type<MusliActionPayload> TYPE = new Type<>(
      ResourceLocation.fromNamespaceAndPath("immersive_engineer_decor_controls_tool_reforged", "musli_intent_v2")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, MusliActionPayload> STREAM_CODEC =
      new StreamCodec<RegistryFriendlyByteBuf, MusliActionPayload>() {
         @Override
         public MusliActionPayload decode(RegistryFriendlyByteBuf buffer) {
            int id = buffer.readVarInt();
            // Bound in the network decoder before any copied data or session recognition.
            return new MusliActionPayload(id, buffer.readByteArray(MusleeCommandProtocol.MAX_PACKET_BYTES));
         }
         @Override
         public void encode(RegistryFriendlyByteBuf buffer, MusliActionPayload payload) {
            buffer.writeVarInt(payload.containerId());
            buffer.writeByteArray(payload.encoded());
         }
      };

   public MusliActionPayload {
      if (encoded == null || encoded.length > MusleeCommandProtocol.MAX_PACKET_BYTES) {
         throw new IllegalArgumentException("Musli command exceeds network bound");
      }
      encoded = encoded.clone();
   }

   @Override
   public byte[] encoded() { return encoded.clone(); }

   @Override
   public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
