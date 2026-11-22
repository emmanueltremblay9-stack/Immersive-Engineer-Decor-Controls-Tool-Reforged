"""Exact additive PR194 Hopper projection. Historical machine pins stay immutable."""
import hashlib
from heavy_double_t_provenance import project as heavy_project, current_hash as heavy_hash

PATH = 'src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineBlockEntity.java'
BASE_SHA256 = '24266c03ec35e12517d8b8f86d1ef53bf1a3e9d266666d38e645045155999128'
CURRENT_SHA256 = 'd49d082a4b67fbed04b0eb90fcf1f16f2c4b87f9ac93829ef8198fcdf0a7b894'
BLOCKS_PATH = 'src/main/java/com/oblixorprime/engineersdecorreforged/ModBlocks.java'
BLOCKS_BASE_SHA256 = 'a6399703a9f8c27668b0e7f7baa2e3fda4035bb5c1651c696f9d28bf7824a58a'
BLOCKS_CURRENT_SHA256 = '0e39361210dd77bd35714dfdc0029da00a34abb484bd36813731bf47a98d249a'
BLOCKS_BEFORE = '"factory_hopper", () -> new MachineBlocks.DirectionalMachineBlock(machineMetal(), MachineKind.FACTORY_HOPPER)'
BLOCKS_AFTER = '"factory_hopper", () -> new MachineBlocks.DirectionalMachineBlock(machineMetal().strength(2.0F, 15.0F), MachineKind.FACTORY_HOPPER)'

# Exact line ranges in current source, current bytes, and previous bytes.
CHANGES = ((271,
  276,
  '   // Adapted from Stefan Wilhelm, EdHopper (MIT), commit a561e61e35ce0367088356967e1ea4cad5c1f27d.\n'
  '   private boolean hopperPlayerBreakPending;\n'
  '   private boolean hopperDropHandled;\n'
  '   private int hopperTickTimer;\n'
  '   private int hopperCollectionTimer;\n',
  ''),
 (328,
  330,
  '               if (MachineBlockEntity.this.kind() == MachineKind.FACTORY_HOPPER)\n'
  '                  MachineBlockEntity.this.hopperTickTimer = '
  'Math.min(MachineBlockEntity.this.hopperTickTimer, 8);\n',
  ''),
 (355,
  357,
  '               if (MachineBlockEntity.this.kind() == MachineKind.FACTORY_HOPPER)\n'
  '                  MachineBlockEntity.this.hopperTickTimer = '
  'Math.min(MachineBlockEntity.this.hopperTickTimer, 8);\n',
  ''),
 (920,
  921,
  '         if (powerChanged && kind != MachineKind.FACTORY_HOPPER) {\n',
  '         if (powerChanged) {\n'),
 (924,
  925,
  '         if (kind != MachineKind.FACTORY_HOPPER) machine.previousPowered = powered;\n',
  '         machine.previousPowered = powered;\n'),
 (1036,
  1048,
  '      if (kind == MachineKind.FACTORY_HOPPER) {\n'
  '         float fullness = 0;\n'
  '         boolean occupied = false;\n'
  '         for (int slot = 0; slot < 18; slot++) {\n'
  '            ItemStack stack = this.items.get(slot);\n'
  '            if (!stack.isEmpty()) {\n'
  '               fullness += (float)stack.getCount() / Math.min(this.getMaxStackSize(stack), '
  'stack.getMaxStackSize());\n'
  '               occupied = true;\n'
  '            }\n'
  '         }\n'
  '         return Mth.clamp(Mth.floor(fullness / 18 * 14) + (occupied ? 1 : 0), 0, 15);\n'
  '      } else if (kind == MachineKind.SMALL_SOLAR_PANEL) {\n',
  '      if (kind == MachineKind.SMALL_SOLAR_PANEL) {\n'),
 (1078,
  1079,
  '      if (!removed.isEmpty() && this.kind() == MachineKind.FACTORY_HOPPER) this.hopperTickTimer = '
  'Math.min(this.hopperTickTimer, 8);\n',
  ''),
 (1109,
  1110,
  '      if (this.kind() == MachineKind.FACTORY_HOPPER) this.hopperTickTimer = '
  'Math.min(this.hopperTickTimer, 8);\n',
  ''),
 (1127, 1128, '      if (this.kind() == MachineKind.FACTORY_HOPPER) return this.slotsUpTo(18);\n', ''),
 (1330,
  1333,
  '                  this.previousPowered = true;\n'
  '                  this.redstoneUpdated = true;\n'
  '                  this.hopperTickTimer = 1;\n',
  '                  this.manualTrigger = true;\n'),
 (1334, 1334, '', '                  this.cooldown = Math.min(this.cooldown, 1);\n'),
 (2005,
  2013,
  '      if (this.kind() == MachineKind.FACTORY_HOPPER) {\n'
  '         this.hopperTickTimer = 0;\n'
  '         this.hopperCollectionTimer = 0;\n'
  '         this.cooldown = 0;\n'
  '         this.redstoneUpdated = false;\n'
  '         this.manualTrigger = false;\n'
  '         this.machineSlotCursor = Math.floorMod(this.machineSlotCursor, 18);\n'
  '      }\n',
  ''),
 (2096,
  2097,
  '      if (this.kind() != MachineKind.FACTORY_HOPPER) tag.putInt("Cooldown", this.cooldown);\n',
  '      tag.putInt("Cooldown", this.cooldown);\n'),
 (2121,
  2125,
  '      if (this.kind() != MachineKind.FACTORY_HOPPER) {\n'
  '         tag.putBoolean("RedstoneUpdated", this.redstoneUpdated);\n'
  '         tag.putBoolean("ManualTrigger", this.manualTrigger);\n'
  '      }\n',
  '      tag.putBoolean("RedstoneUpdated", this.redstoneUpdated);\n'
  '      tag.putBoolean("ManualTrigger", this.manualTrigger);\n'),
 (2174,
  2175,
  '   /** ED 1.3.29 EdHopper semantics (Stefan Wilhelm, MIT); modern safe item handlers retained. */\n',
  ''),
 (2176,
  2198,
  '      if (this.level == null || --this.hopperTickTimer > 0) return;\n'
  '      this.hopperTickTimer = 10;\n'
  '      boolean allowed = this.logicAllowsTrigger(this.hopperLogic, this.previousPowered);\n'
  '      boolean pulse = (this.hopperLogic & 6) == 0;\n'
  '      boolean trigger = (this.hopperLogic & 4) != 0 || allowed && (this.redstoneUpdated || !pulse);\n'
  '      boolean dirty = this.redstoneUpdated || this.previousPowered != powered;\n'
  '      this.redstoneUpdated = this.previousPowered != powered;\n'
  '      this.previousPowered = powered;\n'
  '      if (dirty) this.setChanged();\n'
  '      Direction output = this.facing(this.getBlockState());\n'
  '      Direction input = output == Direction.UP ? Direction.DOWN : Direction.UP;\n'
  '      if (allowed || pulse) {\n'
  '         BlockPos sourcePos = this.worldPosition.relative(input);\n'
  '         if (this.level.hasChunkAt(sourcePos)) {\n'
  '         IItemHandler source = this.level.getCapability(ItemHandler.BLOCK, sourcePos, '
  'input.getOpposite());\n'
  '         if (source != null) this.pullHopperItems(source, this.hopperTransferCount);\n'
  '         else {\n'
  '            BlockEntity neighbor = this.level.getBlockEntity(sourcePos);\n'
  '            if (neighbor instanceof Container container)\n'
  '               this.pullHopperItems(this.hopperContainerHandler(container, input.getOpposite()), '
  'this.hopperTransferCount);\n'
  '            source = this.hopperEntityInventory(sourcePos);\n'
  '            if (source != null) this.pullHopperItems(source, this.hopperTransferCount);\n',
  '      if (this.level != null && this.tickCounter % 10 == 0) {\n'
  '         boolean redstoneAllowed = this.logicAllowsTrigger(this.hopperLogic, powered);\n'
  '         boolean pulseMode = (this.hopperLogic & 6) == 0;\n'
  '         boolean manual = this.manualTrigger;\n'
  '         boolean trigger = (this.hopperLogic & 4) != 0 || manual || redstoneAllowed && '
  '(this.redstoneUpdated || !pulseMode);\n'
  '         this.manualTrigger = false;\n'
  '         this.redstoneUpdated = false;\n'
  '         Direction direction = this.facing(this.getBlockState());\n'
  '         if (trigger) {\n'
  '            this.collectNearbyItems(direction);\n'),
 (2199,
  2203,
  '         if (source == null && (this.hopperCollectionTimer -= 10) <= 0) {\n'
  '            this.hopperCollectionTimer = 50;\n'
  '            this.collectNearbyItems(input);\n'
  '         }\n',
  '\n'
  '         if (this.cooldown <= 0) {\n'
  '            if (trigger) {\n'
  '               this.cooldown = 10 + this.hopperPeriod * 2;\n'
  '               this.pullItems(direction.getOpposite(), this.hopperTransferCount);\n'
  '               this.pushItems(direction, this.hopperTransferCount);\n'
  '            }\n'),
 (2205,
  2274,
  '      if (trigger && this.cooldown <= 0) {\n'
  '         this.cooldown = 10 + this.hopperPeriod * 2;\n'
  '         this.pushItems(output, this.hopperTransferCount);\n'
  '      }\n'
  '   }\n'
  '\n'
  '   public void hopperNeighborChanged() {\n'
  '      if (this.kind() != MachineKind.FACTORY_HOPPER || this.level == null || this.level.isClientSide) '
  'return;\n'
  '      boolean powered = this.level.hasNeighborSignal(this.worldPosition);\n'
  '      if (this.previousPowered != powered) this.redstoneUpdated = true;\n'
  '      this.previousPowered = powered;\n'
  '      this.hopperTickTimer = 1;\n'
  '      this.setChanged();\n'
  '   }\n'
  '\n'
  '   public void beginHopperPlayerBreak(boolean creative) {\n'
  '      if (this.kind() != MachineKind.FACTORY_HOPPER) return;\n'
  '      this.hopperPlayerBreakPending = !creative;\n'
  '      if (creative) { this.hopperDropHandled = true; this.clearContent(); }\n'
  '   }\n'
  '\n'
  '   public boolean consumeHopperPlayerBreak() {\n'
  '      boolean pending = this.hopperPlayerBreakPending;\n'
  '      this.hopperPlayerBreakPending = false;\n'
  '      return pending;\n'
  '   }\n'
  '\n'
  '   public boolean hopperDropHandled() { return this.hopperDropHandled; }\n'
  '\n'
  '   public void explodeHopperContents() {\n'
  '      if (this.kind() != MachineKind.FACTORY_HOPPER || this.hopperDropHandled || this.level == null) '
  'return;\n'
  '      this.hopperDropHandled = true;\n'
  '      if (this.level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS))\n'
  '         Containers.dropContents(this.level, this.worldPosition, this);\n'
  '      this.clearContent();\n'
  '   }\n'
  '\n'
  '   /** Whitelisted portable state; never carry an arbitrary block entity id or runtime work. */\n'
  '   public CompoundTag portableHopperData() {\n'
  '      CompoundTag tag = new CompoundTag();\n'
  '      if (this.kind() != MachineKind.FACTORY_HOPPER || this.isEmpty() || this.level == null) return tag;\n'
  '      ContainerHelper.saveAllItems(tag, this.items, this.level.registryAccess());\n'
  '      tag.putInt("HopperRange", this.hopperRange);\n'
  '      tag.putInt("HopperTransferCount", this.hopperTransferCount);\n'
  '      tag.putInt("HopperLogic", this.hopperLogic);\n'
  '      tag.putInt("HopperPeriod", this.hopperPeriod);\n'
  '      tag.putInt("MachineSlotCursor", Math.floorMod(this.machineSlotCursor, 18));\n'
  '      return tag;\n'
  '   }\n'
  '\n'
  '   public void restorePortableHopperData(CompoundTag tag) {\n'
  '      if (this.kind() != MachineKind.FACTORY_HOPPER || this.level == null) return;\n'
  '      NonNullList<ItemStack> imported = NonNullList.withSize(this.items.size(), ItemStack.EMPTY);\n'
  '      ContainerHelper.loadAllItems(tag, imported, this.level.registryAccess());\n'
  '      for (int slot = 0; slot < this.items.size(); slot++) this.items.set(slot, imported.get(slot));\n'
  '      this.hopperRange = Mth.clamp(tag.getInt("HopperRange"), 0, 4);\n'
  '      this.hopperTransferCount = Mth.clamp(tag.getInt("HopperTransferCount"), 1, 32);\n'
  '      this.hopperLogic = sanitizeTriggerLogic(savedIntOrDefault(tag, "HopperLogic", 3));\n'
  '      this.hopperPeriod = Mth.clamp(tag.getInt("HopperPeriod"), 0, 100);\n'
  '      this.machineSlotCursor = Math.floorMod(tag.getInt("MachineSlotCursor"), 18);\n'
  '      this.previousPowered = false;\n'
  '      this.redstoneUpdated = false;\n'
  '      this.manualTrigger = false;\n'
  '      this.hopperTickTimer = this.hopperCollectionTimer = this.cooldown = 0;\n'
  '      this.setChanged();\n'
  '   }\n'
  '\n'
  '   public void hopperItemFell() {\n'
  '      if (this.kind() == MachineKind.FACTORY_HOPPER) this.hopperCollectionTimer = 0;\n',
  ''),
 (4283,
  4288,
  '         Vec3 center = Vec3.atCenterOf(facing == Direction.UP ? this.worldPosition.above() : '
  'this.worldPosition.below(2));\n'
  '         int distantCollected = 0;\n'
  '         for (ItemEntity itemEntity : this.level.getEntitiesOfClass(ItemEntity.class, range, item -> '
  'item.isAlive() && item.onGround() && !item.getItem().isEmpty())) {\n'
  '            boolean direct = itemEntity.distanceToSqr(center) < 0.7;\n'
  '            if (!direct && itemEntity.hasPickUpDelay()) continue;\n',
  '         for (ItemEntity itemEntity : this.level.getEntitiesOfClass(ItemEntity.class, range, item -> '
  'item.isAlive() && !item.getItem().isEmpty())) {\n'),
 (4290, 4291, '            int accepted = stack.getCount() - remainder.getCount();\n', ''),
 (4296, 4297, '            if (accepted > 0 && !direct && ++distantCollected >= 3) break;\n', ''),
 (4302,
  4314,
  '      int size = this.accessibleSlots().length;\n'
  '      for (int n = 0; n < size && this.items.get(this.machineSlotCursor % size).isEmpty(); n++)\n'
  '         this.machineSlotCursor = (this.machineSlotCursor + 1) % size;\n'
  '      ItemStack stack = this.items.get(this.machineSlotCursor % size);\n'
  '      if (stack.isEmpty()) { this.machineSlotCursor = 0; return; }\n'
  '      BlockPos targetPos = this.worldPosition.relative(direction);\n'
  '      if (!this.level.hasChunkAt(targetPos)) return;\n'
  '      BlockState targetState = this.level.getBlockState(targetPos);\n'
  '      if (targetState.getBlock() instanceof net.minecraft.world.level.block.HopperBlock\n'
  '            && targetState.getValue(net.minecraft.world.level.block.HopperBlock.FACING) == '
  'direction.getOpposite()) return;\n'
  '      if (targetState.is(ModBlocks.FACTORY_HOPPER.get())\n'
  '            && targetState.getValue(MachineBlocks.FACING) == direction.getOpposite()) return;\n',
  ''),
 (4315,
  4333,
  '      if (target == null) { this.cooldown = 12; return; }\n'
  '      ItemStack remaining = stack.copyWithCount(Math.min(stack.getCount(), Math.max(1, maxCount)));\n'
  '      int initial = remaining.getCount();\n'
  '      // Complete matching stacks before consuming a free slot, as in the original Hopper.\n'
  '      for (int slot = 0; slot < target.getSlots() && !remaining.isEmpty(); slot++)\n'
  '         if (ItemStack.isSameItemSameComponents(target.getStackInSlot(slot), remaining))\n'
  '            remaining = target.insertItem(slot, remaining.copy(), false);\n'
  '      for (int slot = 0; slot < target.getSlots() && !remaining.isEmpty(); slot++)\n'
  '         remaining = target.insertItem(slot, remaining.copy(), false);\n'
  '      int inserted = initial - remaining.getCount();\n'
  '      if (inserted > 0) {\n'
  '         stack.shrink(inserted);\n'
  '         this.hopperTickTimer = Math.min(this.hopperTickTimer, 8);\n'
  '         this.setChanged(); this.updateComparator();\n'
  '      }\n'
  '      if (!remaining.isEmpty()) {\n'
  '         this.machineSlotCursor = (this.machineSlotCursor + 1) % size;\n'
  '         this.setChanged();\n',
  '      if (target != null) {\n'
  '         int var11 = Math.max(1, maxCount);\n'
  '\n'
  '         for (int tries = 0; tries < this.accessibleSlots().length && var11 > 0; tries++) {\n'
  '            int slotIndex = this.accessibleSlots()[this.machineSlotCursor % '
  'this.accessibleSlots().length];\n'
  '            ItemStack stack = (ItemStack)this.items.get(slotIndex);\n'
  '            if (!stack.isEmpty()) {\n'
  '               int count = Math.min(var11, stack.getCount());\n'
  '               ItemStack moving = stack.copyWithCount(count);\n'
  '               ItemStack remainder = ItemHandlerHelper.insertItem(target, moving, false);\n'
  '               int inserted = count - remainder.getCount();\n'
  '               if (inserted > 0) {\n'
  '                  stack.shrink(inserted);\n'
  '                  var11 -= inserted;\n'
  '                  this.setChanged();\n'
  '                  this.updateComparator();\n'
  '               }\n'
  '            }\n'
  '\n'
  '            this.machineSlotCursor = (this.machineSlotCursor + 1) % this.accessibleSlots().length;\n'
  '         }\n'),
 (4337,
  4341,
  '      this.pullHopperItems(this.hopperInventory(direction), maxCount);\n'
  '   }\n'
  '\n'
  '   private void pullHopperItems(IItemHandler source, int maxCount) {\n',
  '      IItemHandler source = this.hopperInventory(direction);\n'),
 (4373,
  4381,
  '      // A present block entity without a capability is authoritative on the output side.\n'
  '      if (this.level.getBlockEntity(pos) != null) return null;\n'
  '      return this.hopperEntityInventory(pos);\n'
  '   }\n'
  '\n'
  '   private IItemHandler hopperContainerHandler(Container container, Direction side) {\n'
  '      if (container instanceof WorldlyContainer sided) {\n'
  '         return new SidedInvWrapper(sided, side) {\n',
  '      // Preserve legacy Container-only inventories, including their sided access rules.\n'
  '      BlockEntity neighbor = this.level.getBlockEntity(pos);\n'
  '      if (neighbor instanceof WorldlyContainer container) {\n'
  '         return new SidedInvWrapper(container, side) {\n'),
 (4382,
  4384,
  '               int actualSlot = SidedInvWrapper.getSlot(sided, slot, side);\n'
  '               return actualSlot >= 0 && sided.canTakeItem(MachineBlockEntity.this, actualSlot, '
  'sided.getItem(actualSlot))\n',
  '               int actualSlot = SidedInvWrapper.getSlot(container, slot, side);\n'
  '               return actualSlot >= 0 && container.canTakeItem(MachineBlockEntity.this, actualSlot, '
  'container.getItem(actualSlot))\n'),
 (4388,
  4404,
  '      return new InvWrapper(container) {\n'
  '         @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {\n'
  '            return container.canTakeItem(MachineBlockEntity.this, slot, container.getItem(slot))\n'
  '               ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;\n'
  '         }\n'
  '      };\n'
  '   }\n'
  '\n'
  '   private IItemHandler hopperEntityInventory(BlockPos pos) {\n'
  '      if (!this.level.hasChunkAt(pos)) return null;\n'
  '      for (net.minecraft.world.entity.Entity entity : '
  'this.level.getEntities((net.minecraft.world.entity.Entity)null,\n'
  '            new AABB(pos), net.minecraft.world.entity.EntitySelector.CONTAINER_ENTITY_SELECTOR)) {\n'
  '         IItemHandler entityHandler = entity.getCapability(ItemHandler.ENTITY, null);\n'
  '         if (entityHandler != null) return entityHandler;\n'
  '         if (entity instanceof Container container) return this.hopperContainerHandler(container, '
  'Direction.UP);\n'
  '         break;\n',
  '      if (neighbor instanceof Container container) {\n'
  '         return new InvWrapper(container) {\n'
  '            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {\n'
  '               return container.canTakeItem(MachineBlockEntity.this, slot, container.getItem(slot))\n'
  '                  ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;\n'
  '            }\n'
  '         };\n'),
 (4983,
  4984,
  '            if (this.kind() == MachineKind.FACTORY_HOPPER) this.hopperTickTimer = '
  'Math.min(this.hopperTickTimer, 8);\n',
  ''))

def current_hash(path, historical_hash):
    if path == BLOCKS_PATH:
        assert historical_hash == BLOCKS_BASE_SHA256, "Unexpected Hopper strength baseline"
        return heavy_hash(path, BLOCKS_CURRENT_SHA256)
    if path != PATH:
        return historical_hash
    assert historical_hash == BASE_SHA256, 'Unexpected Hopper fidelity baseline'
    return CURRENT_SHA256


def project_history(source):
    assert hashlib.sha256(source).hexdigest() == CURRENT_SHA256, 'Hopper integrated bytes changed'
    lines = source.decode('utf-8').splitlines(keepends=True)
    for start, end, current, previous in reversed(CHANGES):
        assert ''.join(lines[start:end]) == current, 'Hopper projection hunk changed'
        lines[start:end] = previous.splitlines(keepends=True)
    result = ''.join(lines).encode('utf-8')
    assert hashlib.sha256(result).hexdigest() == BASE_SHA256, 'Hopper historical bytes changed'
    return result


def project_blocks_history(source):
    source = heavy_project(BLOCKS_PATH, source)
    assert hashlib.sha256(source).hexdigest() == BLOCKS_CURRENT_SHA256, 'Hopper strength integrated bytes changed'
    current, previous = BLOCKS_AFTER.encode(), BLOCKS_BEFORE.encode()
    assert source.count(current) == 1, 'Ambiguous Hopper strength projection'
    result = source.replace(current, previous, 1)
    assert hashlib.sha256(result).hexdigest() == BLOCKS_BASE_SHA256, 'Hopper strength historical bytes changed'
    return result
