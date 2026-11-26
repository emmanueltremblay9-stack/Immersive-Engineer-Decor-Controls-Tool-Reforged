package com.oblixorprime.engineersdecorreforged.utility;

import java.util.function.IntUnaryOperator;

/** Pure projection of the existing three-machine protocol. It never predicts server state. */
public final class FactoryStudyState {
   public static final int SELECTED = 0xFFD9AC55;
   public static final int ACTIVE = 0xFF8ED6AA;
   public static final int INACTIVE = 0xFF41464B;
   public static final int SIGNAL = 0xFFFFC17A;
   public static final int FOCUS = 0xFFF5E9C8;
   public static final int PENDING = 0xFFE5B95A;

   private FactoryStudyState() { }

   public record Action(int action, int valueA, int valueB) { }

   public static boolean adjustable(FactoryStudyGeometry.Control control) {
      return switch (control) {
         case RANGE, PERIOD, COUNT, VELOCITY, DIRECTION -> true;
         default -> false;
      };
   }

   public static int fieldIndex(MachineKind kind, FactoryStudyGeometry.Control control) {
      return switch (control) {
         case RANGE, VELOCITY -> 0;
         case COUNT -> kind == MachineKind.FACTORY_HOPPER ? 1 : 4;
         case PERIOD -> kind == MachineKind.FACTORY_HOPPER ? 3 : 6;
         case DIRECTION -> 1;
         default -> -1;
      };
   }

   public static Action pointAction(MachineKind kind, FactoryStudyGeometry.Control control,
                                    double fractionX, double fractionY, int logic) {
      if (kind == MachineKind.FACTORY_HOPPER) {
         return switch (control) {
            case RANGE -> new Action(1, unit(fractionX, 0, 4), 0);
            case COUNT -> new Action(2, unit(fractionX, 1, 32), 0);
            case PERIOD -> new Action(4, unit(fractionX, 0, 100), 0);
            case MANUAL -> new Action(5, 1, 0);
            case REDSTONE -> new Action(3, nextHopperRedstone(logic), 0);
            case TRIGGER -> new Action(3, (logic ^ 2) & 7, 0);
            default -> null;
         };
      }
      if (kind == MachineKind.FACTORY_DROPPER) {
         return switch (control) {
            case VELOCITY -> new Action(10, unit(1.0 - fractionY, 0, 100), 0);
            case DIRECTION -> new Action(11, deadZone(unit(fractionX, -100, 100)), deadZone(unit(1.0 - fractionY, -100, 100)));
            case COUNT -> new Action(12, unit(fractionX, 1, 32), 0);
            case PERIOD -> new Action(13, unit(fractionX, 0, 100), 0);
            case MANUAL -> new Action(15, 1, 0);
            case REDSTONE -> new Action(14, nextDropperExternal(logic), 0);
            case FILTER_GATE -> new Action(14, (logic ^ 1) & 51, 0);
            case TRIGGER -> new Action(14, (logic ^ 16) & 51, 0);
            default -> null;
         };
      }
      return null;
   }

   public static Action incrementAction(MachineKind kind, FactoryStudyGeometry.Control control,
                                        int deltaX, int deltaY, int currentA, int currentB, int logic) {
      if (kind != MachineKind.FACTORY_HOPPER && kind != MachineKind.FACTORY_DROPPER) return null;
      if (control == FactoryStudyGeometry.Control.DIRECTION) {
         return kind == MachineKind.FACTORY_DROPPER
            ? new Action(11, clamp(currentA + deltaX, -100, 100), clamp(currentB + deltaY, -100, 100)) : null;
      }
      if (!adjustable(control)) return pointAction(kind, control, 0, 0, logic);
      int delta = control == FactoryStudyGeometry.Control.VELOCITY ? deltaY : deltaX;
      int min = control == FactoryStudyGeometry.Control.COUNT ? 1 : 0;
      int max = control == FactoryStudyGeometry.Control.RANGE ? 4 : control == FactoryStudyGeometry.Control.COUNT ? 32 : 100;
      int value = clamp(currentA + delta, min, max);
      double fraction = (double)(value - min) / (max - min);
      return pointAction(kind, control, fraction, 1.0 - fraction, logic);
   }

   public static int nextHopperRedstone(int logic) {
      int next = switch (logic & 5) { case 0 -> 1; case 1 -> 4; case 4 -> 0; default -> 4; };
      return ((logic & ~5) | next) & 7;
   }

   public static int nextDropperExternal(int logic) {
      int next = switch (logic & 34) { case 0 -> 32; case 32 -> 2; default -> 0; };
      return ((logic & ~34) | next) & 51;
   }

   /** Manual requests have only a synchronized signal observation, never a work-success receipt. */
   public static boolean actionMatches(MachineKind kind, Action action, IntUnaryOperator fields) {
      if (action == null) return false;
      if (kind == MachineKind.FACTORY_HOPPER) {
         return switch (action.action()) {
            case 1 -> fields.applyAsInt(0) == action.valueA();
            case 2 -> fields.applyAsInt(1) == action.valueA();
            case 3 -> fields.applyAsInt(2) == action.valueA();
            case 4 -> fields.applyAsInt(3) == action.valueA();
            case 5 -> fields.applyAsInt(5) != 0;
            default -> false;
         };
      }
      if (kind == MachineKind.FACTORY_DROPPER) {
         return switch (action.action()) {
            case 10 -> fields.applyAsInt(0) == action.valueA();
            case 11 -> fields.applyAsInt(1) == action.valueA() && fields.applyAsInt(2) == action.valueB();
            case 12 -> fields.applyAsInt(4) == action.valueA();
            case 13 -> fields.applyAsInt(6) == action.valueA();
            case 14 -> fields.applyAsInt(5) == action.valueA();
            case 15 -> fields.applyAsInt(11) != 0;
            default -> false;
         };
      }
      return false;
   }

   public static int periodTicks(int setting) { return 10 + 2 * clamp(setting, 0, 100); }
   public static int fractionPixels(int value, int maximum, int extent) {
      return maximum <= 0 || extent <= 0 ? 0 : (int)Math.min(extent, Math.max(0L, (long)value * extent / maximum));
   }
   public static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
   private static int unit(double fraction, int min, int max) {
      if (!Double.isFinite(fraction)) fraction = 0;
      return clamp((int)Math.round(min + (max - min) * Math.max(0, Math.min(1, fraction))), min, max);
   }
   private static int deadZone(int value) { return Math.abs(value) < 9 ? 0 : value; }
}
