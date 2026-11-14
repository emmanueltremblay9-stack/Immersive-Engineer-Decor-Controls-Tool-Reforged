"""Exact dual-parent byte projections for PR179/current-main integration."""
import hashlib
import json
LAYER = 'breaker_current_main_integration'
RECORD = json.loads(r'''
{
  "main_commit": "362bf1389edd165b1df33fe4150a209aac2cc8d8",
  "breaker_commit": "ed5d25cdb90153d5fd325e078dbc3d3f036768bd",
  "scope": "Exact automatic integration with normal Freezer, crate resize and smelter bounds; no new gameplay or protection claim. Both historical parent layers remain immutable.",
  "files": {
    "src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java": {
      "integrated_sha256": "916b05d77a586089d7ecf814f1f9ca4afeb6da2b2fadc0a15423eed67664e78f",
      "breaker_sha256": "916b05d77a586089d7ecf814f1f9ca4afeb6da2b2fadc0a15423eed67664e78f",
      "breaker_projection": [],
      "main_sha256": "2b0f8cc8bfeac9f421b6948833e4d88b7c993c5c633b6f26feebc6f96f2a0e6f",
      "main_projection": [
        {
          "start": 7,
          "end": 8,
          "original": "import com.oblixorprime.engineersdecorreforged.utility.BlockBreakerRedesignLayout;\n",
          "replacement": ""
        },
        {
          "start": 532,
          "end": 547,
          "original": "      var fluidRegion = BlockBreakerRedesignLayout.FLUID;\n      if (this.isHovering(fluidRegion.x(), fluidRegion.y(), fluidRegion.width(), fluidRegion.height(), mouseX, mouseY)) {\n         boolean accelerated = menu.blockBreakerCycleActive()\n            ? menu.blockBreakerAccelerated() : menu.fluidAmount() >= MachineBlockEntity.BLOCK_BREAKER_LUBRICANT_PER_SUCCESS;\n         Component mode = blockBreakerText(accelerated ? \"mode.accelerated\" : \"mode.normal\");\n         Component state = blockBreakerText(menu.blockBreakerCycleActive() ? \"cycle.active\" : \"cycle.next\");\n         int duration = menu.blockBreakerDurationTicks();\n         Component costText = cost >= 0 ? blockBreakerText(\"tooltip.lubricant_fe\", cost)\n            : blockBreakerText(\"tooltip.lubricant_fe_unavailable\");\n         Component tooltip = blockBreakerText(\"tooltip.lubricant\", menu.blockBreakerFluidName(), menu.fluidAmount(), menu.fluidCapacity()).copy()\n            .append(\"\\n\").append(blockBreakerText(\"tooltip.lubricant_mode\", mode, state, duration))\n            .append(\"\\n\").append(blockBreakerText(\"tooltip.lubricant_use\", MachineBlockEntity.BLOCK_BREAKER_LUBRICANT_PER_SUCCESS))\n            .append(\"\\n\").append(costText);\n         return tooltip;\n      }\n",
          "replacement": "      if (this.isHovering(191, 71, 46, 12, mouseX, mouseY))\n         return blockBreakerText(\"tooltip.fluid_unavailable\");\n"
        },
        {
          "start": 1458,
          "end": 1460,
          "original": "         graphics.renderTooltip(this.font,\n            this.font.split(blockBreakerTooltip, milkingTooltipWidth(this.width)), mouseX, mouseY);\n",
          "replacement": "         graphics.renderTooltip(this.font, blockBreakerTooltip, mouseX, mouseY);\n"
        }
      ]
    },
    "src/main/java/com/oblixorprime/engineersdecorreforged/client/RedesignedMachineArtwork.java": {
      "integrated_sha256": "29a3c271ab8edac6e2c032279320266b94db3a28d044576d1771edf8fe530267",
      "breaker_sha256": "29a3c271ab8edac6e2c032279320266b94db3a28d044576d1771edf8fe530267",
      "breaker_projection": [],
      "main_sha256": "76436f4e818fbd8bf2d1cc62a69f75746d67ace911c06efdac476cf2e4f8036c",
      "main_projection": [
        {
          "start": 46,
          "end": 60,
          "original": "      var fluid = BlockBreakerRedesignLayout.FLUID;\n      graphics.fill(x + fluid.x(), y + fluid.y(), x + fluid.right(), y + fluid.bottom(), 0xFF181A19);\n      int fluidFill = fillPixels(menu.fluidAmount(), menu.fluidCapacity(), fluid.height());\n      if (fluidFill > 0 && !menu.blockBreakerFluidStack().isEmpty()) {\n         FluidBarrelSpriteRenderer.draw(graphics, menu.blockBreakerFluidStack(),\n            x + fluid.x(), y + fluid.y(), fluid.width(), fluid.height(),\n            x + fluid.x(), y + fluid.bottom() - fluidFill, x + fluid.right(), y + fluid.bottom());\n      }\n      var progress = BlockBreakerRedesignLayout.PROGRESS;\n      int duration = menu.blockBreakerDurationTicks();\n      int progressFill = duration > 0 ? fillPixels(menu.field(1), duration, progress.width()) : 0;\n      if (progressFill > 0) {\n         graphics.fill(x + progress.x(), y + progress.y(), x + progress.x() + progressFill, y + progress.bottom(), 0xFF7FA77A);\n      }\n",
          "replacement": ""
        },
        {
          "start": 61,
          "end": 62,
          "original": "      // The power icon is a non-interactive neutral presentation, not an invented on/off command.\n",
          "replacement": "      // No fluid reservoir exists yet; the fluid gauge remains empty. The power icon\n      // is a non-interactive neutral presentation, not an invented on/off command.\n"
        }
      ]
    },
    "src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java": {
      "integrated_sha256": "1d7869986d663e01d0d3257a140597c1dd18e334e6f94aa15433c595de7f9223",
      "breaker_sha256": "15803c81755ece86eda38308c3fb1bae5dc4a4b21e46eb188fd92a1e781ea3c3",
      "breaker_projection": [
        {
          "start": 11,
          "end": 12,
          "original": "import com.oblixorprime.engineersdecorreforged.utility.FreezerReferenceGeometry;\n",
          "replacement": ""
        },
        {
          "start": 48,
          "end": 51,
          "original": "   /** DEVELOPMENT/VALIDATION only. Consulted by the client factory, never server gameplay. */\n   public static final String DEV_FREEZER_REFERENCE_PROPERTY = \"iedctr.dev.freezerV5ExactReference\";\n   public static final String DEV_FREEZER_LIVE_ENERGY_PROPERTY = \"iedctr.dev.freezerV5LiveEnergy\";\n",
          "replacement": ""
        },
        {
          "start": 120,
          "end": 121,
          "original": "   private final boolean freezerReferencePresentation;\n",
          "replacement": ""
        },
        {
          "start": 140,
          "end": 142,
          "original": "         kind == MachineKind.SMALL_FREEZER && (Boolean.getBoolean(DEV_FREEZER_REFERENCE_PROPERTY)\n            || Boolean.parseBoolean(System.getProperty(DEV_FREEZER_LIVE_ENERGY_PROPERTY, \"true\"))),\n",
          "replacement": ""
        },
        {
          "start": 148,
          "end": 149,
          "original": "      this(kind, id, inventory, container, data, false, false, false, false);\n",
          "replacement": "      this(kind, id, inventory, container, data, false, false, false);\n"
        },
        {
          "start": 152,
          "end": 153,
          "original": "                       boolean freezerReferenceRequested, boolean barrelReferenceRequested, boolean factoryPlacerV2ReferenceRequested,\n",
          "replacement": "                       boolean barrelReferenceRequested, boolean factoryPlacerV2ReferenceRequested,\n"
        },
        {
          "start": 166,
          "end": 171,
          "original": "      // Geometry is client presentation only. Both sides retain the same per-machine slot\n      // count/order, container indices, transfer ranges and data fields. A server JVM flag does\n      // not change authoritative menus, and no client-only Minecraft classes are loaded here.\n      this.freezerReferencePresentation = freezerReferenceRequested\n         && kind == MachineKind.SMALL_FREEZER && inventory.player.level().isClientSide;\n",
          "replacement": ""
        },
        {
          "start": 191,
          "end": 196,
          "original": "         if (this.freezerReferencePresentation && slot.containerIndex() != 1) {\n            var anchor = FreezerReferenceGeometry.anchor(slot.containerIndex());\n            presentationSlot = new MachineLayout.MachineSlot(\n               slot.containerIndex(), anchor.x(), anchor.y(), slot.role(), slot.output());\n         } else if (barrelReferencePresentation) {\n",
          "replacement": "         if (barrelReferencePresentation) {\n"
        },
        {
          "start": 218,
          "end": 223,
          "original": "            if (this.freezerReferencePresentation) {\n               var anchor = FreezerReferenceGeometry.anchor(3 + row * 9 + column);\n               x = anchor.x();\n               y = anchor.y();\n            } else if (barrelReferencePresentation) {\n",
          "replacement": "            if (barrelReferencePresentation) {\n"
        },
        {
          "start": 245,
          "end": 250,
          "original": "         if (this.freezerReferencePresentation) {\n            var anchor = FreezerReferenceGeometry.anchor(30 + column);\n            x = anchor.x();\n            y = anchor.y();\n         } else if (barrelReferencePresentation) {\n",
          "replacement": "         if (barrelReferencePresentation) {\n"
        },
        {
          "start": 288,
          "end": 291,
          "original": "   public boolean freezerReferencePresentation() {\n      return this.freezerReferencePresentation;\n   }\n",
          "replacement": ""
        },
        {
          "start": 620,
          "end": 630,
          "original": "      if (this.kind == MachineKind.SMALL_FREEZER && slot.containerIndex() == 1) {\n         return new MachineMenu.ValidatedMachineSlot(this.container, 1, slot.x(), slot.y(), true) {\n            @Override\n            public boolean isActive() {\n               // Hide the historical reserved opening only in the opt-in presentation.\n               // Its backing contents and ordinary QUICK_MOVE recovery remain intact.\n               return !MachineMenu.this.freezerReferencePresentation;\n            }\n         };\n      }\n",
          "replacement": ""
        }
      ],
      "main_sha256": "74f95f7e8ab5af051ae6ce9ff435070a95cc12ea199f11c775e35fd14dfeb3de",
      "main_projection": [
        {
          "start": 45,
          "end": 46,
          "original": "import net.neoforged.neoforge.fluids.FluidStack;\n",
          "replacement": ""
        },
        {
          "start": 94,
          "end": 98,
          "original": "   private static final int BLOCK_BREAKER_FLUID_PRESENT = 1;\n   private static final int BLOCK_BREAKER_ACCELERATED = 2;\n   private static final int BLOCK_BREAKER_CYCLE_ACTIVE = 4;\n   private static final ResourceLocation BLOCK_BREAKER_LUBRICANT_ID = ResourceLocation.fromNamespaceAndPath(\"immersivepetroleum\", \"lubricant\");\n",
          "replacement": ""
        },
        {
          "start": 333,
          "end": 334,
          "original": "         case SMALL_BLOCK_BREAKER -> MachineBlockEntity.BLOCK_BREAKER_LUBRICANT_CAPACITY;\n",
          "replacement": ""
        },
        {
          "start": 347,
          "end": 351,
          "original": "      if (this.kind == MachineKind.SMALL_BLOCK_BREAKER) {\n         return (this.data.get(4) & BLOCK_BREAKER_FLUID_PRESENT) != 0 && this.data.get(2) > 0\n            ? BuiltInRegistries.FLUID.get(BLOCK_BREAKER_LUBRICANT_ID) : Fluids.EMPTY;\n      }\n",
          "replacement": ""
        },
        {
          "start": 352,
          "end": 384,
          "original": "   }\n\n   public boolean blockBreakerAccelerated() {\n      return this.kind == MachineKind.SMALL_BLOCK_BREAKER && (this.data.get(4) & BLOCK_BREAKER_ACCELERATED) != 0;\n   }\n\n   public boolean blockBreakerCycleActive() {\n      return this.kind == MachineKind.SMALL_BLOCK_BREAKER && (this.data.get(4) & BLOCK_BREAKER_CYCLE_ACTIVE) != 0;\n   }\n\n   public FluidStack blockBreakerFluidStack() {\n      Fluid fluid = this.storedFluid();\n      return fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, this.fluidAmount());\n   }\n\n   public Component blockBreakerFluidName() {\n      FluidStack stack = this.blockBreakerFluidStack();\n      return stack.isEmpty()\n         ? Component.translatable(\"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.fluid_name\")\n         : stack.getHoverName();\n   }\n\n   public int blockBreakerDurationTicks() {\n      if (this.kind != MachineKind.SMALL_BLOCK_BREAKER) return 0;\n      int synced = this.data.get(3);\n      if (synced > 0) return synced;\n      float hardness = this.blockBreakerHardness();\n      if (!Float.isFinite(hardness) || hardness < 0.0F || this.blockBreakerTargetStack().isEmpty()) return 0;\n      long normalTicks = 20L + Math.round(hardness * 20.0F);\n      int normal = (int)Math.max(20L, Math.min(200L, normalTicks));\n      return this.fluidAmount() >= MachineBlockEntity.BLOCK_BREAKER_LUBRICANT_PER_SUCCESS\n         ? Math.max(20, (normal + 1) / 2) : normal;\n",
          "replacement": ""
        }
      ]
    },
    "src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineBlockEntity.java": {
      "integrated_sha256": "3dfb25d8e8d818f6436983b921b2db18d2644be4b28e38ad1d3d0fae4dbe795e",
      "breaker_sha256": "3dfb25d8e8d818f6436983b921b2db18d2644be4b28e38ad1d3d0fae4dbe795e",
      "breaker_projection": [],
      "main_sha256": "24266c03ec35e12517d8b8f86d1ef53bf1a3e9d266666d38e645045155999128",
      "main_projection": [
        {
          "start": 162,
          "end": 165,
          "original": "   public static final int BLOCK_BREAKER_LUBRICANT_CAPACITY = 4000;\n   public static final int BLOCK_BREAKER_LUBRICANT_PER_SUCCESS = 10;\n   private static final ResourceLocation BLOCK_BREAKER_LUBRICANT_ID = ResourceLocation.fromNamespaceAndPath(\"immersivepetroleum\", \"lubricant\");\n",
          "replacement": ""
        },
        {
          "start": 225,
          "end": 230,
          "original": "   private BlockState blockBreakerCycleTarget;\n   private BlockPos blockBreakerCyclePosition;\n   private boolean blockBreakerCycleAccelerated;\n   private int blockBreakerCycleEnergyCost;\n   private Tag unreadableBlockBreakerFluid;\n",
          "replacement": ""
        },
        {
          "start": 254,
          "end": 305,
          "original": "\n   private final FluidTank blockBreakerLubricantTank = new FluidTank(BLOCK_BREAKER_LUBRICANT_CAPACITY) {\n      @Override public boolean isFluidValid(int tank, FluidStack stack) {\n         return tank == 0 && MachineBlockEntity.isBlockBreakerLubricant(stack);\n      }\n      @Override public int fill(FluidStack stack, FluidAction action) {\n         return MachineBlockEntity.this.kind() == MachineKind.SMALL_BLOCK_BREAKER\n            && MachineBlockEntity.this.unreadableBlockBreakerFluid == null\n            && MachineBlockEntity.isBlockBreakerLubricant(stack) ? super.fill(stack, action) : 0;\n      }\n      @Override public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }\n      @Override public FluidStack drain(FluidStack stack, FluidAction action) { return FluidStack.EMPTY; }\n      @Override protected void onContentsChanged() { MachineBlockEntity.this.setChanged(); }\n   };\n\n   private static boolean isBlockBreakerLubricant(FluidStack stack) {\n      return !stack.isEmpty() && BLOCK_BREAKER_LUBRICANT_ID.equals(BuiltInRegistries.FLUID.getKey(stack.getFluid()));\n   }\n\n   private boolean isBlockBreakerLubricantSide(Direction side) {\n      if (side == null || this.kind() != MachineKind.SMALL_BLOCK_BREAKER) return false;\n      Direction right = switch (this.facing(this.getBlockState())) {\n         case NORTH -> Direction.WEST;\n         case EAST -> Direction.NORTH;\n         case SOUTH -> Direction.EAST;\n         case WEST -> Direction.SOUTH;\n         default -> null;\n      };\n      return side == right;\n   }\n\n   private IFluidHandler blockBreakerLubricantHandler(Direction side) {\n      return new IFluidHandler() {\n         private boolean active() { return MachineBlockEntity.this.isBlockBreakerLubricantSide(side); }\n         public int getTanks() { return active() && MachineBlockEntity.this.unreadableBlockBreakerFluid == null ? 1 : 0; }\n         public FluidStack getFluidInTank(int tank) {\n            return active() && MachineBlockEntity.this.unreadableBlockBreakerFluid == null && tank == 0\n               ? MachineBlockEntity.this.blockBreakerLubricantTank.getFluid().copy() : FluidStack.EMPTY;\n         }\n         public int getTankCapacity(int tank) { return active() && tank == 0 ? BLOCK_BREAKER_LUBRICANT_CAPACITY : 0; }\n         public boolean isFluidValid(int tank, FluidStack stack) {\n            return active() && MachineBlockEntity.this.unreadableBlockBreakerFluid == null\n               && tank == 0 && MachineBlockEntity.isBlockBreakerLubricant(stack);\n         }\n         public int fill(FluidStack stack, FluidAction action) {\n            return active() ? MachineBlockEntity.this.blockBreakerLubricantTank.fill(stack, action) : 0;\n         }\n         public FluidStack drain(FluidStack stack, FluidAction action) { return FluidStack.EMPTY; }\n         public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }\n      };\n   }\n",
          "replacement": ""
        },
        {
          "start": 889,
          "end": 892,
          "original": "      if (this.kind() == MachineKind.SMALL_BLOCK_BREAKER) {\n         return this.isBlockBreakerLubricantSide(side) ? this.blockBreakerLubricantHandler(side) : null;\n      }\n",
          "replacement": ""
        },
        {
          "start": 1626,
          "end": 1628,
          "original": "               case 2:\n                  yield this.blockBreakerLubricantTank.getFluidAmount();\n",
          "replacement": ""
        },
        {
          "start": 1631,
          "end": 1634,
          "original": "                  yield (this.blockBreakerLubricantTank.getFluidAmount() <= 0 ? 0 : 1)\n                     | (this.blockBreakerCycleAccelerated ? 2 : 0)\n                     | (this.blockBreakerCycleTarget != null ? 4 : 0);\n",
          "replacement": "                  yield this.comparatorOutput();\n"
        },
        {
          "start": 1876,
          "end": 1879,
          "original": "                  case 2:\n                  case 4:\n                     return;\n",
          "replacement": ""
        },
        {
          "start": 2006,
          "end": 2054,
          "original": "      if (this.kind() == MachineKind.SMALL_BLOCK_BREAKER) {\n         this.unreadableBlockBreakerFluid = null;\n         this.blockBreakerLubricantTank.setFluid(FluidStack.EMPTY);\n         if (tag.contains(\"BlockBreakerLubricant\")) {\n            Tag raw = tag.get(\"BlockBreakerLubricant\");\n            if (!(raw instanceof CompoundTag savedFluid)\n                  || !savedFluid.contains(\"id\", Tag.TAG_STRING)\n                  || !savedFluid.contains(\"amount\", Tag.TAG_INT)\n                  || !BLOCK_BREAKER_LUBRICANT_ID.toString().equals(savedFluid.getString(\"id\"))\n                  || savedFluid.getInt(\"amount\") <= 0\n                  || savedFluid.getInt(\"amount\") > BLOCK_BREAKER_LUBRICANT_CAPACITY) {\n               this.unreadableBlockBreakerFluid = raw.copy();\n            } else {\n               FluidStack parsed = FluidStack.parseOptional(registries, savedFluid);\n               if (parsed.isEmpty() || !isBlockBreakerLubricant(parsed)) this.unreadableBlockBreakerFluid = savedFluid.copy();\n               else this.blockBreakerLubricantTank.setFluid(parsed);\n            }\n         }\n\n         this.blockBreakerCycleTarget = null;\n         this.blockBreakerCyclePosition = null;\n         this.blockBreakerCycleAccelerated = false;\n         this.blockBreakerCycleEnergyCost = 0;\n         if (tag.contains(\"BlockBreakerCycleTarget\", Tag.TAG_COMPOUND)\n               && tag.contains(\"BlockBreakerCyclePosition\", Tag.TAG_LONG)\n               && tag.contains(\"BlockBreakerCycleAccelerated\", Tag.TAG_BYTE)\n               && tag.contains(\"BlockBreakerCycleEnergyCost\", Tag.TAG_INT)) {\n            BlockState savedTarget = NbtUtils.readBlockState(\n               registries.lookupOrThrow(Registries.BLOCK), tag.getCompound(\"BlockBreakerCycleTarget\")\n            );\n            int savedCost = tag.getInt(\"BlockBreakerCycleEnergyCost\");\n            boolean savedAccelerated = tag.getBoolean(\"BlockBreakerCycleAccelerated\");\n            if (!savedTarget.isAir() && this.progress > 0 && this.progress < this.processTimeNeeded\n                  && this.processTimeNeeded >= 20 && this.processTimeNeeded <= 200 && savedCost > 0\n                  && (!savedAccelerated || this.blockBreakerLubricantTank.getFluidAmount() >= BLOCK_BREAKER_LUBRICANT_PER_SUCCESS)) {\n               this.blockBreakerCycleTarget = savedTarget;\n               this.blockBreakerCyclePosition = BlockPos.of(tag.getLong(\"BlockBreakerCyclePosition\"));\n               this.blockBreakerCycleAccelerated = savedAccelerated;\n               this.blockBreakerCycleEnergyCost = savedCost;\n            } else {\n               this.progress = 0;\n               this.processTimeNeeded = 0;\n            }\n         } else {\n            this.progress = 0;\n            this.processTimeNeeded = 0;\n         }\n      }\n",
          "replacement": ""
        },
        {
          "start": 2203,
          "end": 2215,
          "original": "         }\n      }\n      if (this.kind() == MachineKind.SMALL_BLOCK_BREAKER) {\n         if (this.unreadableBlockBreakerFluid != null) tag.put(\"BlockBreakerLubricant\", this.unreadableBlockBreakerFluid.copy());\n         else if (this.blockBreakerLubricantTank.getFluidAmount() > 0) {\n            tag.put(\"BlockBreakerLubricant\", this.blockBreakerLubricantTank.getFluid().save(registries));\n         }\n         if (this.blockBreakerCycleTarget != null) {\n            tag.put(\"BlockBreakerCycleTarget\", NbtUtils.writeBlockState(this.blockBreakerCycleTarget));\n            tag.putLong(\"BlockBreakerCyclePosition\", this.blockBreakerCyclePosition.asLong());\n            tag.putBoolean(\"BlockBreakerCycleAccelerated\", this.blockBreakerCycleAccelerated);\n            tag.putInt(\"BlockBreakerCycleEnergyCost\", this.blockBreakerCycleEnergyCost);\n",
          "replacement": ""
        },
        {
          "start": 2444,
          "end": 2450,
          "original": "         if (this.blockBreakerCycleTarget != null && (!this.blockBreakerCycleTarget.equals(targetState)\n               || !target.equals(this.blockBreakerCyclePosition))) {\n            this.clearBlockBreakerCycle();\n            this.progress = 0;\n            this.processTimeNeeded = 0;\n         }\n",
          "replacement": ""
        },
        {
          "start": 2454,
          "end": 2455,
          "original": "            this.clearBlockBreakerCycle();\n",
          "replacement": ""
        },
        {
          "start": 2463,
          "end": 2464,
          "original": "            this.clearBlockBreakerCycle();\n",
          "replacement": ""
        },
        {
          "start": 2469,
          "end": 2470,
          "original": "            this.clearBlockBreakerCycle();\n",
          "replacement": ""
        },
        {
          "start": 2472,
          "end": 2479,
          "original": "            int requiredTicks = this.processTimeNeeded;\n            int energyCost;\n            if (this.blockBreakerCycleTarget == null) {\n               int normalTicks = this.blockBreakerProcessTicks(targetState, target);\n               boolean accelerated = this.blockBreakerLubricantTank.getFluidAmount() >= BLOCK_BREAKER_LUBRICANT_PER_SUCCESS;\n               requiredTicks = accelerated ? Math.max(20, (normalTicks + 1) / 2) : normalTicks;\n               int cycleCost = blockBreakerEnergyCostPerBlock(hardness);\n",
          "replacement": "            int requiredTicks = this.blockBreakerProcessTicks(targetState, target);\n            if (this.processTimeNeeded != requiredTicks) {\n"
        },
        {
          "start": 2480,
          "end": 2495,
          "original": "               if (!this.canBlockBreakerStoreDrops(target, targetState)) {\n                  this.blockBreakerStatus = BREAKER_STATUS_OUTPUT_BLOCKED;\n                  this.setBooleanProperty(MachineBlocks.ACTIVE, false);\n                  return;\n               }\n               energyCost = this.blockBreakerEnergyCostPerTick(cycleCost, 0, requiredTicks);\n               if (energyCost <= 0 || this.energy < energyCost) {\n                  this.blockBreakerStatus = BREAKER_STATUS_WAITING_FOR_ENERGY;\n                  this.setBooleanProperty(MachineBlocks.ACTIVE, false);\n                  return;\n               }\n               this.blockBreakerCycleTarget = targetState;\n               this.blockBreakerCyclePosition = target;\n               this.blockBreakerCycleAccelerated = accelerated;\n               this.blockBreakerCycleEnergyCost = cycleCost;\n",
          "replacement": ""
        },
        {
          "start": 2496,
          "end": 2515,
          "original": "            } else {\n               if (requiredTicks < 20 || requiredTicks > 200 || this.blockBreakerCycleEnergyCost <= 0) {\n                  this.clearBlockBreakerCycle();\n                  this.progress = 0;\n                  this.processTimeNeeded = 0;\n                  this.setBooleanProperty(MachineBlocks.ACTIVE, false);\n                  return;\n               }\n               if (!this.canBlockBreakerStoreDrops(target, targetState)) {\n                  this.blockBreakerStatus = BREAKER_STATUS_OUTPUT_BLOCKED;\n                  this.setBooleanProperty(MachineBlocks.ACTIVE, false);\n                  return;\n               }\n               energyCost = this.blockBreakerEnergyCostPerTick(this.blockBreakerCycleEnergyCost, this.progress, requiredTicks);\n               if (energyCost <= 0 || this.energy < energyCost) {\n                  this.blockBreakerStatus = BREAKER_STATUS_WAITING_FOR_ENERGY;\n                  this.setBooleanProperty(MachineBlocks.ACTIVE, false);\n                  return;\n               }\n",
          "replacement": "            }\n\n            if (!this.canBlockBreakerStoreDrops(target, targetState)) {\n               this.blockBreakerStatus = BREAKER_STATUS_OUTPUT_BLOCKED;\n               this.setBooleanProperty(MachineBlocks.ACTIVE, false);\n               return;\n            }\n\n            int energyCost = this.blockBreakerEnergyCostPerTick(hardness, this.progress, requiredTicks);\n            if (energyCost <= 0 || this.energy < energyCost) {\n               this.blockBreakerStatus = BREAKER_STATUS_WAITING_FOR_ENERGY;\n               this.setBooleanProperty(MachineBlocks.ACTIVE, false);\n               return;\n"
        },
        {
          "start": 2525,
          "end": 2532,
          "original": "                  if (this.blockBreakerCycleAccelerated) {\n                     FluidStack stored = this.blockBreakerLubricantTank.getFluid();\n                     if (stored.getAmount() >= BLOCK_BREAKER_LUBRICANT_PER_SUCCESS) {\n                        this.blockBreakerLubricantTank.setFluid(stored.getAmount() == BLOCK_BREAKER_LUBRICANT_PER_SUCCESS\n                           ? FluidStack.EMPTY : stored.copyWithAmount(stored.getAmount() - BLOCK_BREAKER_LUBRICANT_PER_SUCCESS));\n                     }\n                  }\n",
          "replacement": ""
        },
        {
          "start": 2534,
          "end": 2535,
          "original": "                  this.clearBlockBreakerCycle();\n",
          "replacement": ""
        },
        {
          "start": 2553,
          "end": 2560,
          "original": "   private void clearBlockBreakerCycle() {\n      this.blockBreakerCycleTarget = null;\n      this.blockBreakerCyclePosition = null;\n      this.blockBreakerCycleAccelerated = false;\n      this.blockBreakerCycleEnergyCost = 0;\n   }\n\n",
          "replacement": ""
        },
        {
          "start": 2573,
          "end": 2577,
          "original": "      return this.blockBreakerEnergyCostPerTick(blockBreakerEnergyCostPerBlock(hardness), elapsedTicks, totalTicks);\n   }\n\n   private int blockBreakerEnergyCostPerTick(int totalCost, int elapsedTicks, int totalTicks) {\n",
          "replacement": ""
        },
        {
          "start": 2582,
          "end": 2583,
          "original": "      long costBefore = (long)totalCost * elapsed / totalTicks;\n",
          "replacement": "      long totalCost = blockBreakerEnergyCostPerBlock(hardness);\n      long costBefore = totalCost * elapsed / totalTicks;\n"
        }
      ]
    },
    "src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/lang/en_us.json": {
      "integrated_sha256": "59bef5693d7fdc70cd7efad7cfdef577e98039ceb2cdf13ccee853333e30daea",
      "breaker_sha256": "04ef6cd7d2477232a8d3f12cd0a88643919c477231d58f908713301b9f4b5846",
      "breaker_projection": [
        {
          "start": 1103,
          "end": 1108,
          "original": "  \"gui.immersive_engineer_decor_controls_tool_reforged.freezer_reference.recover\": \"Recover reserved items\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.freezer_reference.recover_help\": \"Recover legacy slot 1 to your inventory. Ctrl+Shift+R. Full inventory: items stay stored.\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.freezer_reference.live_energy_cells\": \"Live energy: %s / %s segments (rounded down).\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.freezer_reference.static_frost\": \"STATIC FROST ART: no clean chamber layer is available.\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.freezer_reference.static_art\": \"STATIC REFERENCE ART: frost and energy are not live.\",\n",
          "replacement": ""
        }
      ],
      "main_sha256": "efdd1e32fa134196f629ca2c06bd2bf74a86e94a4be4f9bb2944b86d605b2c30",
      "main_projection": [
        {
          "start": 1090,
          "end": 1100,
          "original": "  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.fluid_name\": \"Immersive Petroleum Lubricant\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.mode.normal\": \"Normal\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.mode.accelerated\": \"Accelerated\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.cycle.active\": \"active cycle\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.cycle.next\": \"next cycle\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.tooltip.lubricant\": \"%1$s: %2$s / %3$s mB\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.tooltip.lubricant_mode\": \"Mode: %1$s (%2$s); duration: %3$s ticks.\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.tooltip.lubricant_use\": \"Accelerated cycles use %1$s mB only after a successful break.\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.tooltip.lubricant_fe\": \"FE cost: %1$s per block.\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.tooltip.lubricant_fe_unavailable\": \"FE cost is unavailable for the current target.\",\n",
          "replacement": "  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.tooltip.fluid_unavailable\": \"Fluid storage is not implemented. No level or bonus is simulated.\",\n"
        }
      ]
    },
    "src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/lang/fr_fr.json": {
      "integrated_sha256": "222c8e97aab9fe9748bef791c7fd1dc7d7d0985f612945d17d68fb055bceb385",
      "breaker_sha256": "d9fa4353e1436583d6d3a0786ffae9daf42ee33500c7f7c52fdf7ed2ad6a5a97",
      "breaker_projection": [
        {
          "start": 1103,
          "end": 1108,
          "original": "  \"gui.immersive_engineer_decor_controls_tool_reforged.freezer_reference.recover\": \"Récupérer les objets réservés\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.freezer_reference.recover_help\": \"Récupérer l’ancien emplacement 1 dans votre inventaire. Ctrl+Maj+R. Inventaire plein : les objets restent stockés.\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.freezer_reference.live_energy_cells\": \"Énergie réelle : %s / %s segments (arrondi inférieur).\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.freezer_reference.static_frost\": \"GIVRE STATIQUE : aucune couche de chambre dégivrée n’est disponible.\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.freezer_reference.static_art\": \"IMAGE DE RÉFÉRENCE STATIQUE : le givre et l’énergie ne sont pas actualisés.\",\n",
          "replacement": ""
        }
      ],
      "main_sha256": "4fb0a94655848377b3fe95d2ab173762e9feadc57a7da692b63f54cc47ae8734",
      "main_projection": [
        {
          "start": 1090,
          "end": 1100,
          "original": "  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.fluid_name\": \"Lubrifiant d’Immersive Petroleum\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.mode.normal\": \"Normal\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.mode.accelerated\": \"Accéléré\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.cycle.active\": \"cycle actif\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.cycle.next\": \"prochain cycle\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.tooltip.lubricant\": \"%1$s : %2$s / %3$s mB\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.tooltip.lubricant_mode\": \"Mode : %1$s (%2$s) ; durée : %3$s ticks.\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.tooltip.lubricant_use\": \"Un cycle accéléré consomme %1$s mB uniquement après une destruction réussie.\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.tooltip.lubricant_fe\": \"Coût en FE : %1$s par bloc.\",\n  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.tooltip.lubricant_fe_unavailable\": \"Le coût en FE est indisponible pour la cible actuelle.\",\n",
          "replacement": "  \"gui.immersive_engineer_decor_controls_tool_reforged.block_breaker.tooltip.fluid_unavailable\": \"Réservoir de fluide non implémenté. Aucun niveau ni bonus simulé.\",\n"
        }
      ]
    }
  }
}
''')

def assert_record(manifest):
    assert manifest.get(LAYER) == RECORD, 'Current-main integration record changed'

def project(path, data, parent):
    if path not in RECORD['files']: return data
    record = RECORD['files'][path]
    assert hashlib.sha256(data).hexdigest() == record['integrated_sha256'], 'Integrated bytes changed: '+path
    lines = data.decode().splitlines(keepends=True)
    for patch in reversed(record[parent+'_projection']):
        start,end = patch['start'],patch['end']
        assert ''.join(lines[start:end]) == patch['original'], 'Projection source changed: '+path
        lines[start:end] = patch['replacement'].splitlines(keepends=True)
    result = ''.join(lines).encode()
    assert hashlib.sha256(result).hexdigest() == record[parent+'_sha256'], 'Parent reconstruction changed: '+path
    return result

def integrated_current_hash(path, expected):
    if path not in RECORD['files']: return expected
    record = RECORD['files'][path]
    assert expected in (record['main_sha256'],record['breaker_sha256']), 'Unexpected parent identity: '+path
    return record['integrated_sha256']
