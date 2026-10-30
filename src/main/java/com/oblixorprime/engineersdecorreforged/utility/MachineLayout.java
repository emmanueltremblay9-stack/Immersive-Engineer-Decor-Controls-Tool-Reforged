package com.oblixorprime.engineersdecorreforged.utility;

import java.util.ArrayList;
import java.util.List;

public record MachineLayout(
   int imageWidth,
   int imageHeight,
   int playerInventoryX,
   int playerInventoryY,
   int hotbarX,
   int hotbarY,
   List<MachineLayout.MachineSlot> machineSlots,
   List<MachineLayout.Panel> panels,
   MachineLayout.LayoutStyle style,
   String textureName
) {
   public static final int BACKING_SLOT_COUNT = 27;

   public MachineLayout(
      int imageWidth,
      int imageHeight,
      int playerInventoryX,
      int playerInventoryY,
      int hotbarX,
      int hotbarY,
      List<MachineLayout.MachineSlot> machineSlots,
      List<MachineLayout.Panel> panels,
      MachineLayout.LayoutStyle style,
      String textureName
   ) {
      machineSlots = List.copyOf(machineSlots);
      panels = List.copyOf(panels);
      if (machineSlots.isEmpty()) {
         throw new IllegalArgumentException("Machine layouts must expose at least one machine slot");
      }

      for (MachineLayout.MachineSlot slot : machineSlots) {
         if (slot.containerIndex() < 0 || slot.containerIndex() >= 27) {
            throw new IllegalArgumentException("Machine slot index out of range: " + slot.containerIndex());
         }
      }

      this.imageWidth = imageWidth;
      this.imageHeight = imageHeight;
      this.playerInventoryX = playerInventoryX;
      this.playerInventoryY = playerInventoryY;
      this.hotbarX = hotbarX;
      this.hotbarY = hotbarY;
      this.machineSlots = machineSlots;
      this.panels = panels;
      this.style = style;
      this.textureName = textureName;
   }

   public static MachineLayout forKind(MachineKind kind) {
      return switch (kind) {
         case METAL_CRAFTING_TABLE -> metalCraftingTable();
         case LABELED_CRATE -> crate();
         case SMALL_LAB_FURNACE -> labFurnace();
         case SMALL_ELECTRICAL_FURNACE -> electricalFurnace();
         case SMALL_WASTE_INCINERATOR -> wasteIncinerator();
         case FACTORY_HOPPER -> hopper();
         case FACTORY_DROPPER -> dropper();
         case FACTORY_PLACER -> placer();
         case SMALL_MINERAL_SMELTER, SMALL_FREEZER -> processor(kind);
         case FLUID_BARREL, SMALL_FLUID_FUNNEL, PASSIVE_FLUID_ACCUMULATOR, SMALL_MILKING_MACHINE -> fluid(kind);
         case SMALL_SOLAR_PANEL -> energy();
         case SMALL_BLOCK_BREAKER -> blockBreaker();
         case SMALL_TREE_CUTTER -> automation(kind);
      };
   }

   private static MachineLayout blockBreaker() {
      return new MachineLayout(
         209,
         235,
         23,
         144,
         23,
         202,
         List.of(new MachineLayout.MachineSlot(0, 118, 38, "Collected output")),
         List.of(),
         MachineLayout.LayoutStyle.ORIGINAL_TEXTURE,
         "block_breaker_v08/background"
      );
   }

   private static MachineLayout crate() {
      return base(grid(0, 8, 18, 9, 3, "Storage"), List.of(new MachineLayout.Panel("Storage", 6, 15, 164, 59)), MachineLayout.LayoutStyle.STORAGE, null);
   }

   private static MachineLayout metalCraftingTable() {
      return base(
         concat(
            List.of(new MachineLayout.MachineSlot(0, 22, 35, "Engineer's Hammer"), new MachineLayout.MachineSlot(10, 142, 35, "Crafting output")),
            grid(1, 62, 17, 3, 3, "Crafting grid")
         ),
         List.of(
            new MachineLayout.Panel("Hammer", 16, 30, 30, 30),
            new MachineLayout.Panel("Craft", 56, 12, 66, 62),
            new MachineLayout.Panel("Output", 136, 30, 30, 30)
         ),
         MachineLayout.LayoutStyle.CRAFTING,
         null
      );
   }

   private static MachineLayout hopper() {
      return original(grid(0, 11, 9, 6, 3, "Storage"), 8, 71, 8, 129, "factory_hopper_gui");
   }

   private static MachineLayout placer() {
      return original(grid(0, 11, 9, 6, 3, "Blocks"), 9, 71, 9, 129, "factory_placer_gui");
   }

   private static MachineLayout dropper() {
      return original(
         concat(
            grid(0, 10, 6, 6, 2, "Input"),
            List.of(
               new MachineLayout.MachineSlot(12, 19, 48, "Filter"),
               new MachineLayout.MachineSlot(13, 55, 48, "Filter"),
               new MachineLayout.MachineSlot(14, 91, 48, "Filter")
            )
         ),
         8,
         86,
         8,
         144,
         "factory_dropper_gui"
      );
   }

   private static MachineLayout labFurnace() {
      return original(
         List.of(
            new MachineLayout.MachineSlot(0, 59, 17, "Input"),
            new MachineLayout.MachineSlot(1, 59, 53, "Fuel"),
            new MachineLayout.MachineSlot(2, 101, 35, "Output"),
            new MachineLayout.MachineSlot(3, 34, 17, "Input FIFO"),
            new MachineLayout.MachineSlot(4, 16, 17, "Input FIFO"),
            new MachineLayout.MachineSlot(5, 34, 53, "Fuel FIFO"),
            new MachineLayout.MachineSlot(6, 16, 53, "Fuel FIFO"),
            new MachineLayout.MachineSlot(7, 126, 35, "Output FIFO"),
            new MachineLayout.MachineSlot(8, 144, 35, "Output FIFO"),
            new MachineLayout.MachineSlot(9, 126, 61, "Aux"),
            new MachineLayout.MachineSlot(10, 144, 61, "Aux")
         ),
         8,
         86,
         8,
         144,
         "small_lab_furnace_gui"
      );
   }

   private static MachineLayout electricalFurnace() {
      return original(
         List.of(
            new MachineLayout.MachineSlot(0, 59, 28, "Input"),
            new MachineLayout.MachineSlot(1, 16, 52, "Aux"),
            new MachineLayout.MachineSlot(2, 101, 28, "Output"),
            new MachineLayout.MachineSlot(3, 34, 28, "Input FIFO"),
            new MachineLayout.MachineSlot(4, 16, 28, "Input FIFO"),
            new MachineLayout.MachineSlot(5, 126, 28, "Output FIFO"),
            new MachineLayout.MachineSlot(6, 144, 28, "Output FIFO")
         ),
         8,
         86,
         8,
         144,
         "small_electrical_furnace_gui"
      );
   }

   private static MachineLayout wasteIncinerator() {
      return original(
         List.of(
            new MachineLayout.MachineSlot(0, 13, 9, "Queue"),
            new MachineLayout.MachineSlot(1, 37, 12, "Queue"),
            new MachineLayout.MachineSlot(2, 54, 13, "Queue"),
            new MachineLayout.MachineSlot(3, 71, 14, "Queue"),
            new MachineLayout.MachineSlot(4, 88, 15, "Queue"),
            new MachineLayout.MachineSlot(5, 105, 16, "Queue"),
            new MachineLayout.MachineSlot(6, 122, 17, "Queue"),
            new MachineLayout.MachineSlot(7, 139, 18, "Queue"),
            new MachineLayout.MachineSlot(8, 144, 38, "Queue"),
            new MachineLayout.MachineSlot(9, 127, 39, "Queue"),
            new MachineLayout.MachineSlot(10, 110, 40, "Queue"),
            new MachineLayout.MachineSlot(11, 93, 41, "Queue"),
            new MachineLayout.MachineSlot(12, 76, 42, "Queue"),
            new MachineLayout.MachineSlot(13, 59, 43, "Queue"),
            new MachineLayout.MachineSlot(14, 42, 44, "Queue"),
            new MachineLayout.MachineSlot(15, 17, 58, "Queue")
         ),
         8,
         86,
         8,
         144,
         "small_waste_incinerator_gui"
      );
   }

   private static MachineLayout processor(MachineKind kind) {
      String middle = kind == MachineKind.SMALL_LAB_FURNACE
         ? "Fuel"
         : (kind == MachineKind.SMALL_ELECTRICAL_FURNACE ? "Power" : (kind == MachineKind.SMALL_MINERAL_SMELTER ? "Heat" : "Cold"));
      return base(
         List.of(
            new MachineLayout.MachineSlot(0, 44, 20, "Input"),
            new MachineLayout.MachineSlot(1, 44, 54, middle),
            new MachineLayout.MachineSlot(2, 116, 37, "Output")
         ),
         List.of(
            new MachineLayout.Panel("Input", 38, 15, 30, 25),
            new MachineLayout.Panel(middle, 38, 49, 30, 25),
            new MachineLayout.Panel("Output", 110, 32, 30, 25)
         ),
         MachineLayout.LayoutStyle.PROCESSOR,
         null
      );
   }

   private static MachineLayout fluid(MachineKind kind) {
      String source = kind == MachineKind.SMALL_MILKING_MACHINE ? "Bucket" : "Empty";
      String result = kind == MachineKind.SMALL_MILKING_MACHINE ? "Milk" : "Filled";
      return base(
         List.of(new MachineLayout.MachineSlot(0, 35, 37, source), new MachineLayout.MachineSlot(1, 124, 37, result)),
         List.of(
            new MachineLayout.Panel(source, 29, 32, 30, 25), new MachineLayout.Panel("Tank", 74, 16, 28, 58), new MachineLayout.Panel(result, 118, 32, 30, 25)
         ),
         MachineLayout.LayoutStyle.FLUID,
         null
      );
   }

   private static MachineLayout energy() {
      return base(
         List.of(new MachineLayout.MachineSlot(0, 80, 53, "Cell")),
         List.of(new MachineLayout.Panel("Array", 36, 18, 104, 24), new MachineLayout.Panel("Cell", 74, 48, 30, 25)),
         MachineLayout.LayoutStyle.ENERGY,
         null
      );
   }

   private static MachineLayout automation(MachineKind kind) {
      String input = kind == MachineKind.FACTORY_PLACER
         ? "Blocks"
         : (
            kind != MachineKind.SMALL_BLOCK_BREAKER && kind != MachineKind.SMALL_TREE_CUTTER
               ? (kind == MachineKind.SMALL_WASTE_INCINERATOR ? "Waste" : "Buffer")
               : "Drops"
         );
      return base(
         concat(grid(0, 17, 24, 3, 3, input), List.of(new MachineLayout.MachineSlot(9, 134, 42, "I/O"))),
         List.of(
            new MachineLayout.Panel(input, 15, 19, 58, 58), new MachineLayout.Panel("Action", 82, 26, 36, 44), new MachineLayout.Panel("I/O", 128, 37, 30, 25)
         ),
         MachineLayout.LayoutStyle.AUTOMATION,
         null
      );
   }

   private static MachineLayout original(List<MachineLayout.MachineSlot> slots, int playerX, int playerY, int hotbarX, int hotbarY, String textureName) {
      return new MachineLayout(176, 166, playerX, playerY, hotbarX, hotbarY, slots, List.of(), MachineLayout.LayoutStyle.ORIGINAL_TEXTURE, textureName);
   }

   private static MachineLayout base(
      List<MachineLayout.MachineSlot> slots, List<MachineLayout.Panel> panels, MachineLayout.LayoutStyle style, String textureName
   ) {
      return new MachineLayout(176, 166, 8, 84, 8, 142, slots, panels, style, textureName);
   }

   private static List<MachineLayout.MachineSlot> grid(int firstIndex, int startX, int startY, int columns, int rows, String role) {
      List<MachineLayout.MachineSlot> slots = new ArrayList<>(columns * rows);

      for (int row = 0; row < rows; row++) {
         for (int column = 0; column < columns; column++) {
            slots.add(new MachineLayout.MachineSlot(firstIndex + column + row * columns, startX + column * 18, startY + row * 18, role));
         }
      }

      return slots;
   }

   @SafeVarargs
   private static List<MachineLayout.MachineSlot> concat(List<MachineLayout.MachineSlot>... groups) {
      List<MachineLayout.MachineSlot> slots = new ArrayList<>();

      for (List<MachineLayout.MachineSlot> group : groups) {
         slots.addAll(group);
      }

      return slots;
   }

   public enum LayoutStyle {
      STORAGE,
      CRAFTING,
      ORIGINAL_TEXTURE,
      PROCESSOR,
      FLUID,
      ENERGY,
      AUTOMATION;
   }

   public record MachineSlot(int containerIndex, int x, int y, String role) {
   }

   public record Panel(String label, int x, int y, int width, int height) {
   }
}
