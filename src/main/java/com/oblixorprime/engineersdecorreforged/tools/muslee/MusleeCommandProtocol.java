package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;
import java.util.UUID;

/** Fixed wire grammar only. Not registered as a network payload. */
public final class MusleeCommandProtocol {
   public static final int VERSION = 2;
   public static final int MAX_PACKET_BYTES = 8192;
   public static final int HEADER_BYTES = 37;

   public enum Type { ABSORB, PRESS }

   /** Cursor-only intent. Authoritative state supplies all stacks, counts and slots. */
   public record Command(UUID actionId, Type type) {
      public Command {
         if (!isV4(actionId) || type == null) {
            throw new IllegalArgumentException("Invalid Muslee command");
         }
      }
   }

   private MusleeCommandProtocol() {}

   public static boolean isV4(UUID id) {
      return id != null && id.version() == 4 && id.variant() == 2;
   }

   public static byte[] encode(UUID sessionId, Command command) {
      if (!isV4(sessionId) || command == null) {
         throw new IllegalArgumentException("Invalid Muslee session or command");
      }
      ByteBuffer bytes = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.BIG_ENDIAN);
      bytes.putShort((short) VERSION);
      putUuid(bytes, sessionId);
      putUuid(bytes, command.actionId());
      int typeId = switch (command.type()) {
         case ABSORB -> 1; // Type 2 is permanently reserved for rejected legacy withdrawal.
         case PRESS -> 3;
      };
      bytes.put((byte) typeId);
      bytes.putShort((short) 0);
      return bytes.array();
   }

   // Called only AFTER authoritative session validation and action-ID recognition.
   static Optional<Command> decodeRecognized(byte[] bytes, UUID actionId) {
      if (bytes.length < HEADER_BYTES) {
         return Optional.empty();
      }
      int typeId = Byte.toUnsignedInt(bytes[34]);
      Type type = switch (typeId) {
         case 1 -> Type.ABSORB;
         case 3 -> Type.PRESS;
         default -> null;
      };
      if (type == null) {
         return Optional.empty();
      }
      int parameterLength = (Byte.toUnsignedInt(bytes[35]) << 8) | Byte.toUnsignedInt(bytes[36]);
      if (parameterLength != 0 || bytes.length != HEADER_BYTES) {
         return Optional.empty();
      }
      return Optional.of(new Command(actionId, type));
   }

   static UUID uuidAt(byte[] bytes, int offset) {
      ByteBuffer view = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
      return new UUID(view.getLong(offset), view.getLong(offset + 8));
   }

   private static void putUuid(ByteBuffer buffer, UUID id) {
      buffer.putLong(id.getMostSignificantBits());
      buffer.putLong(id.getLeastSignificantBits());
   }
}
