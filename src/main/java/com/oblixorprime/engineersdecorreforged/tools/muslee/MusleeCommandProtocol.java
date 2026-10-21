package com.oblixorprime.engineersdecorreforged.tools.muslee;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;
import java.util.UUID;

/** Fixed wire grammar only. Not registered as a network payload. */
public final class MusleeCommandProtocol {
   public static final int VERSION = 1;
   public static final int MAX_PACKET_BYTES = 8192;
   public static final int HEADER_BYTES = 37;

   public enum Type { INSERT, WITHDRAW, PRESS }

   /** INSERT argument: source 0..35; WITHDRAW: 1 bread, 2 apple, 3 seeds; PRESS: 0. */
   public record Command(UUID actionId, Type type, int argument) {
      public Command {
         if (!isV4(actionId) || type == null || !validArgument(type, argument)) {
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
      int parameters = command.type() == Type.PRESS ? 0 : 1;
      ByteBuffer bytes = ByteBuffer.allocate(HEADER_BYTES + parameters).order(ByteOrder.BIG_ENDIAN);
      bytes.putShort((short) VERSION);
      putUuid(bytes, sessionId);
      putUuid(bytes, command.actionId());
      int typeId = switch (command.type()) {
         case INSERT -> 1;
         case WITHDRAW -> 2;
         case PRESS -> 3;
      };
      bytes.put((byte) typeId);
      bytes.putShort((short) parameters);
      if (parameters != 0) {
         bytes.put((byte) command.argument());
      }
      return bytes.array();
   }

   // Called only AFTER authoritative session validation and action-ID recognition.
   static Optional<Command> decodeRecognized(byte[] bytes, UUID actionId) {
      if (bytes.length < HEADER_BYTES) {
         return Optional.empty();
      }
      int typeId = Byte.toUnsignedInt(bytes[34]);
      Type type = switch (typeId) {
         case 1 -> Type.INSERT;
         case 2 -> Type.WITHDRAW;
         case 3 -> Type.PRESS;
         default -> null;
      };
      if (type == null) {
         return Optional.empty();
      }
      int parameterLength = (Byte.toUnsignedInt(bytes[35]) << 8) | Byte.toUnsignedInt(bytes[36]);
      int expected = type == Type.PRESS ? 0 : 1;
      if (parameterLength != expected || bytes.length != HEADER_BYTES + expected) {
         return Optional.empty();
      }
      int argument = expected == 0 ? 0 : Byte.toUnsignedInt(bytes[37]);
      return validArgument(type, argument) ? Optional.of(new Command(actionId, type, argument)) : Optional.empty();
   }

   static UUID uuidAt(byte[] bytes, int offset) {
      ByteBuffer view = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);
      return new UUID(view.getLong(offset), view.getLong(offset + 8));
   }

   private static void putUuid(ByteBuffer buffer, UUID id) {
      buffer.putLong(id.getMostSignificantBits());
      buffer.putLong(id.getLeastSignificantBits());
   }

   private static boolean validArgument(Type type, int argument) {
      return switch (type) {
         case INSERT -> argument >= 0 && argument <= 35;
         case WITHDRAW -> argument >= 1 && argument <= 3;
         case PRESS -> argument == 0;
      };
   }
}
