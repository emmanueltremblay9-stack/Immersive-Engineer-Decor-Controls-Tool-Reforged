package com.oblixorprime.engineersdecorreforged.utility;

/** Pure projection of the existing Placer fields and action protocol, never predicted state. */
public final class FactoryPlacerV2ReferenceState {
   public static final int SELECTED_COLOR = 0xFFD9AC55;
   public static final int ACTIVE_COLOR = 0xFF8ED6AA;
   public static final int INACTIVE_COLOR = 0xFF41464B;
   public static final int SIGNAL_ON_COLOR = 0xFFFFC17A;
   public static final int SIGNAL_OFF_COLOR = 0xFF681915;

   private FactoryPlacerV2ReferenceState() { }

   public static int selectedSlot(int synchronizedCursor) { return Math.max(0, Math.min(17, synchronizedCursor)); }
   public static int selectedRow(int synchronizedCursor) { return selectedSlot(synchronizedCursor) / 6; }
   public static boolean signalActive(int synchronizedSignal) { return synchronizedSignal != 0; }
   public static boolean continuous(int synchronizedLogic) { return (synchronizedLogic & 2) != 0; }

   /** 0 = requires power, 1 = requires no power, 2 = ignores power. */
   public static int redstoneMode(int synchronizedLogic) {
      return (synchronizedLogic & 4) != 0 ? 2 : (synchronizedLogic & 1);
   }

   public static int nextInversionLogic(int synchronizedLogic) {
      int next = switch (synchronizedLogic & 5) {
         case 0 -> 1;
         case 1 -> 4;
         case 4 -> 0;
         default -> 4;
      };
      return synchronizedLogic & ~5 | next;
   }

   public static Action actionFor(FactoryPlacerV2ReferenceGeometry.Control control, int synchronizedLogic) {
      return switch (control) {
         case ROW_TOP -> new Action(22, 0);
         case ROW_MIDDLE -> new Action(22, 1);
         case ROW_BOTTOM -> new Action(22, 2);
         case MANUAL_TRIGGER -> new Action(21, 1);
         case INVERSION -> new Action(20, nextInversionLogic(synchronizedLogic));
         case TRIGGER_MODE -> new Action(20, synchronizedLogic ^ 2);
      };
   }

   /** Left presses with an empty cursor only. Drag/release/right-click remain vanilla. */
   public static Action actionAt(double localX, double localY, int button, boolean carrying, int synchronizedLogic) {
      var control = FactoryPlacerV2ReferenceGeometry.controlAt(localX, localY);
      return button == 0 && !carrying && control != null ? actionFor(control, synchronizedLogic) : null;
   }

   public record Action(int action, int valueA) { }
}
