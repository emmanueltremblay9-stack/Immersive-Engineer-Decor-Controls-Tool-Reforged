package com.oblixorprime.engineersdecorreforged.rsgauges;

import com.oblixorprime.engineersdecorreforged.ModBlockEntities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Persistent per-block state for the reconstructed RSG switch contract.
 *
 * The original mod kept output configuration, pulse timing, timer/sensor
 * parameters and SwitchLink pearls in its switch block entity. The initial
 * Reforged port flattened most of that into block classes; restoring a
 * persistent state object lets the modern port preserve those semantics
 * without reintroducing the old Forge architecture.
 */
public class ControlStateBlockEntity extends BlockEntity {
   public static final int MAX_PULSE_TICKS = 200;
   public static final int MAX_TIMER_TICKS = 20 * 60 * 10;
   public static final int MIN_TIMER_TICKS = 5;
   public static final int MAX_TIMER_RAMP = 5;
   public static final int MAX_ENTITY_SENSOR_RANGE = 16;
   public static final int MAX_BLOCK_SENSOR_RANGE = 8;
   public static final int MAX_SENSOR_DEBOUNCE = 10;
   public static final int MAX_SWITCHLINK_DISTANCE = 48;

   private int outputPower = 15;
   private boolean inverted;
   private boolean weak;
   private boolean noOutput;
   private long enabledSides;
   private int configuredPulseTicks;
   private long pulseOffDeadline;

   private int timerOnTicks = 20;
   private int timerOffTicks = 20;
   private int timerRamp;
   private int timerRawPower;
   private int timerCountdown;
   private boolean timerHigh;

   private int sensorRange = 5;
   private int sensorThreshold = 1;
   private int sensorFilter;
   private int sensorDebounce;
   private int sensorDebounceCounter;
   private int sensorLightOn = 8;
   private int sensorLightOff = 7;

   private long lastLinkGameTime = Long.MIN_VALUE;
   private final List<LinkTarget> links = new ArrayList<>();

   public ControlStateBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.CONTROL_STATE.get(), pos, state);
      this.applyBlockDefaults(state);
   }

   private void applyBlockDefaults(BlockState state) {
      if (state.getBlock() instanceof ControlsBlockTypes.ToggleSwitchBlock control) {
         long config = control.config();
         int configuredPower = (int)(config & 15L);
         this.outputPower = configuredPower == 0 ? 15 : configuredPower;
         this.inverted = ControlProfile.has(config, ControlProfile.DATA_INVERTED);
         this.weak = ControlProfile.has(config, ControlProfile.DATA_WEAK);
         this.noOutput = ControlProfile.has(config, ControlProfile.DATA_NO_OUTPUT);
         this.enabledSides = config & ControlProfile.DATA_SIDE_ALL;
         if (ControlProfile.has(config, ControlProfile.SENSOR_BLOCK)) {
            this.sensorRange = 0;
            this.sensorThreshold = 1;
         }
         if (ControlProfile.has(config, ControlProfile.SENSOR_LIGHT)) {
            this.sensorLightOn = 7;
            this.sensorLightOff = 6;
         }
         if (ControlProfile.has(config, ControlProfile.SENSOR_RAIN) || ControlProfile.has(config, ControlProfile.SENSOR_LIGHTNING)) {
            this.sensorDebounce = 4;
         }
      }
   }

   public void resetToBlockDefaults() {
      this.outputPower = 15;
      this.inverted = false;
      this.weak = false;
      this.noOutput = false;
      this.enabledSides = 0L;
      this.configuredPulseTicks = 0;
      this.pulseOffDeadline = 0L;
      this.timerOnTicks = 20;
      this.timerOffTicks = 20;
      this.timerRamp = 0;
      this.timerRawPower = 0;
      this.timerCountdown = 0;
      this.timerHigh = false;
      this.sensorRange = 5;
      this.sensorThreshold = 1;
      this.sensorFilter = 0;
      this.sensorDebounce = 0;
      this.sensorDebounceCounter = 0;
      this.sensorLightOn = 7;
      this.sensorLightOff = 6;
      this.gaugeComparatorMode = false;
      this.linkedInputPower = 0;
      this.links.clear();
      this.applyBlockDefaults(this.getBlockState());
      this.setChanged();
   }

   public int outputPower() {
      return this.outputPower;
   }

   public void outputPower(int value) {
      this.outputPower = Mth.clamp(value, 0, 15);
      this.setChanged();
   }

   public boolean inverted() {
      return this.inverted;
   }

   public void inverted(boolean value) {
      this.inverted = value;
      this.setChanged();
   }

   public boolean weak() {
      return this.weak;
   }

   public void weak(boolean value) {
      this.weak = value;
      this.setChanged();
   }

   public boolean noOutput() {
      return this.noOutput;
   }

   public void noOutput(boolean value) {
      this.noOutput = value;
      this.setChanged();
   }

   public long enabledSides() {
      return this.enabledSides;
   }

   public void enabledSides(long value) {
      this.enabledSides = value & ControlProfile.DATA_SIDE_ALL;
      this.setChanged();
   }

   public int configuredPulseTicks() {
      return this.configuredPulseTicks;
   }

   public void configuredPulseTicks(int value) {
      this.configuredPulseTicks = Mth.clamp(value, 0, MAX_PULSE_TICKS);
      this.setChanged();
   }

   public int pulseTicksOr(int fallback) {
      return this.configuredPulseTicks >= 2 ? this.configuredPulseTicks : fallback;
   }

   public void resetPulseTimer() {
      this.pulseOffDeadline = 0L;
      this.setChanged();
   }

   public int startOrExtendPulse(Level level, int fallback, boolean extendable) {
      int configured = this.pulseTicksOr(fallback);
      int remaining = this.pulseRemaining(level);
      int duration = configured;
      if (extendable && this.configuredPulseTicks < 2) {
         duration = remaining > 90 ? MAX_PULSE_TICKS : remaining > 45 ? 100 : remaining > 15 ? 50 : remaining > 1 ? 30 : fallback;
      }
      this.pulseOffDeadline = level.getGameTime() + Mth.clamp(duration, 1, MAX_PULSE_TICKS);
      this.setChanged();
      return duration;
   }

   public int pulseRemaining(Level level) {
      long remaining = Math.max(0L, this.pulseOffDeadline - level.getGameTime());
      return remaining > MAX_PULSE_TICKS ? 0 : (int)remaining;
   }

   public int timerOnTicks() {
      return this.timerOnTicks;
   }

   public void timerOnTicks(int value) {
      this.timerOnTicks = Mth.clamp(value, 0, MAX_TIMER_TICKS);
      this.setChanged();
   }

   public int timerOffTicks() {
      return this.timerOffTicks;
   }

   public void timerOffTicks(int value) {
      this.timerOffTicks = Mth.clamp(value, 0, MAX_TIMER_TICKS);
      this.setChanged();
   }

   public int timerRamp() {
      return this.timerRamp;
   }

   public void timerRamp(int value) {
      this.timerRamp = Mth.clamp(value, 0, MAX_TIMER_RAMP);
      this.setChanged();
   }

   public int timerRawPower() {
      return this.timerRawPower;
   }

   public void restartTimer() {
      this.timerRawPower = 0;
      this.timerCountdown = 0;
      this.timerHigh = false;
      this.setChanged();
   }

   public int timerEffectivePower(BlockState state) {
      if (!state.hasProperty(ControlsBlockTypes.POWERED) || !state.getValue(ControlsBlockTypes.POWERED) || this.noOutput) {
         return 0;
      }
      return this.inverted ? 15 - this.timerRawPower : this.timerRawPower;
   }

   public int nextHigherInterval(int ticks) {
      int next;
      if (ticks < 100) {
         next = ticks + 5;
      } else if (ticks < 200) {
         next = ticks + 10;
      } else if (ticks < 400) {
         next = ticks + 20;
      } else if (ticks < 600) {
         next = ticks + 40;
      } else if (ticks < 800) {
         next = ticks + 100;
      } else if (ticks < 2400) {
         next = ticks + 200;
      } else {
         next = ticks + 600;
      }
      return Math.min(next, MAX_TIMER_TICKS);
   }

   public int nextLowerInterval(int ticks) {
      int next;
      if (ticks < 100) {
         next = ticks - 5;
      } else if (ticks < 200) {
         next = ticks - 10;
      } else if (ticks < 400) {
         next = ticks - 20;
      } else if (ticks < 600) {
         next = ticks - 40;
      } else if (ticks < 800) {
         next = ticks - 100;
      } else if (ticks < 2400) {
         next = ticks - 200;
      } else {
         next = ticks - 600;
      }
      return Math.max(next, MIN_TIMER_TICKS);
   }

   public int sensorRange() {
      return this.sensorRange;
   }

   public void sensorRange(int value, boolean blockSensor) {
      int max = blockSensor ? MAX_BLOCK_SENSOR_RANGE : MAX_ENTITY_SENSOR_RANGE;
      this.sensorRange = Mth.clamp(value, blockSensor ? 0 : 1, max);
      if (this.sensorThreshold > Math.max(1, this.sensorRange)) {
         this.sensorThreshold = Math.max(1, this.sensorRange);
      }
      this.setChanged();
   }

   public int sensorThreshold() {
      return this.sensorThreshold;
   }

   public void sensorThreshold(int value) {
      this.sensorThreshold = Mth.clamp(value, 1, Math.max(1, this.sensorRange));
      this.setChanged();
   }

   public int sensorFilter() {
      return this.sensorFilter;
   }

   public void sensorFilter(int value, int maxExclusive) {
      this.sensorFilter = Mth.clamp(value, 0, Math.max(0, maxExclusive - 1));
      this.setChanged();
   }

   public int sensorDebounce() {
      return this.sensorDebounce;
   }

   public void sensorDebounce(int value) {
      this.sensorDebounce = Mth.clamp(value, 0, MAX_SENSOR_DEBOUNCE);
      this.sensorDebounceCounter = Mth.clamp(this.sensorDebounceCounter, 0, this.sensorDebounce);
      this.setChanged();
   }

   public boolean debounced(boolean active) {
      if (this.sensorDebounce <= 0) {
         this.sensorDebounceCounter = active ? 1 : 0;
         return active;
      }

      if (active) {
         this.sensorDebounceCounter = Math.min(this.sensorDebounce, this.sensorDebounceCounter + 1);
         return this.sensorDebounceCounter >= this.sensorDebounce;
      }

      this.sensorDebounceCounter = Math.max(0, this.sensorDebounceCounter - 1);
      return this.sensorDebounceCounter > 0;
   }

   public int sensorLightOn() {
      return this.sensorLightOn;
   }

   public void sensorLightOn(int value) {
      this.sensorLightOn = Mth.clamp(value, 0, 15);
      this.setChanged();
   }

   public int sensorLightOff() {
      return this.sensorLightOff;
   }

   public void sensorLightOff(int value) {
      this.sensorLightOff = Mth.clamp(value, 0, 15);
      this.setChanged();
   }

   public boolean gaugeComparatorMode() {
      return this.gaugeComparatorMode;
   }

   public void gaugeComparatorMode(boolean value) {
      this.gaugeComparatorMode = value;
      this.setChanged();
   }

   public int linkedInputPower() {
      return this.linkedInputPower;
   }

   public void linkedInputPower(int value) {
      this.linkedInputPower = Mth.clamp(value, 0, 15);
      this.setChanged();
   }

   public List<LinkTarget> links() {
      return Collections.unmodifiableList(this.links);
   }

   public boolean addLink(Level level, BlockPos sourcePos, BlockPos targetPos, LinkMode mode) {
      if (sourcePos.equals(targetPos) || sourcePos.distSqr(targetPos) > (double)(MAX_SWITCHLINK_DISTANCE * MAX_SWITCHLINK_DISTANCE)) {
         return false;
      }
      BlockState targetState = level.getBlockState(targetPos);
      if (!ControlsBlockTypes.supportsSwitchLinkTarget(targetState)) {
         return false;
      }
      String blockId = BuiltInRegistries.BLOCK.getKey(targetState.getBlock()).toString();
      for (LinkTarget existing : this.links) {
         if (existing.targetPos() == targetPos.asLong()) {
            return false;
         }
      }
      this.links.add(new LinkTarget(targetPos.asLong(), blockId, mode));
      this.setChanged();
      return true;
   }

   public void clearLinks() {
      this.links.clear();
      this.setChanged();
   }

   public boolean activateLinks(Level level, BlockPos sourcePos, int analogPower, int digitalPower, boolean stateChanged) {
      if (this.links.isEmpty()) {
         return true;
      }
      long now = level.getGameTime();
      if (this.lastLinkGameTime == now) {
         return false;
      }
      this.lastLinkGameTime = now;
      boolean ok = true;
      for (LinkTarget link : List.copyOf(this.links)) {
         BlockPos targetPos = BlockPos.of(link.targetPos());
         if (sourcePos.distSqr(targetPos) > (double)(MAX_SWITCHLINK_DISTANCE * MAX_SWITCHLINK_DISTANCE)
            || !level.isLoaded(targetPos)
            || !BuiltInRegistries.BLOCK.getKey(level.getBlockState(targetPos).getBlock()).toString().equals(link.blockId())) {
            ok = false;
            continue;
         }
         if (!ControlsBlockTypes.applySwitchLink(level, targetPos, link.mode(), analogPower, digitalPower, stateChanged)) {
            ok = false;
         }
      }
      return ok;
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, ControlStateBlockEntity control) {
      if (state.getBlock() instanceof ControlsBlockTypes.IntervalTimerBlock timer) {
         timer.serverTick(level, pos, state, control);
      }
   }

   @Override
   protected void loadAdditional(CompoundTag tag, Provider registries) {
      super.loadAdditional(tag, registries);
      if (tag.contains("output_power", Tag.TAG_INT)) this.outputPower = Mth.clamp(tag.getInt("output_power"), 0, 15);
      this.inverted = tag.getBoolean("inverted");
      this.weak = tag.getBoolean("weak");
      this.noOutput = tag.getBoolean("no_output");
      if (tag.contains("enabled_sides", Tag.TAG_LONG)) this.enabledSides = tag.getLong("enabled_sides") & ControlProfile.DATA_SIDE_ALL;
      if (tag.contains("pulse_ticks", Tag.TAG_INT)) this.configuredPulseTicks = Mth.clamp(tag.getInt("pulse_ticks"), 0, MAX_PULSE_TICKS);
      if (tag.contains("pulse_deadline", Tag.TAG_LONG)) this.pulseOffDeadline = tag.getLong("pulse_deadline");

      if (tag.contains("timer_on", Tag.TAG_INT)) this.timerOnTicks = Mth.clamp(tag.getInt("timer_on"), 0, MAX_TIMER_TICKS);
      if (tag.contains("timer_off", Tag.TAG_INT)) this.timerOffTicks = Mth.clamp(tag.getInt("timer_off"), 0, MAX_TIMER_TICKS);
      if (tag.contains("timer_ramp", Tag.TAG_INT)) this.timerRamp = Mth.clamp(tag.getInt("timer_ramp"), 0, MAX_TIMER_RAMP);
      if (tag.contains("timer_power", Tag.TAG_INT)) this.timerRawPower = Mth.clamp(tag.getInt("timer_power"), 0, 15);
      if (tag.contains("timer_countdown", Tag.TAG_INT)) this.timerCountdown = Math.max(0, tag.getInt("timer_countdown"));
      this.timerHigh = tag.getBoolean("timer_high");

      if (tag.contains("sensor_range", Tag.TAG_INT)) this.sensorRange = Mth.clamp(tag.getInt("sensor_range"), 0, MAX_ENTITY_SENSOR_RANGE);
      if (tag.contains("sensor_threshold", Tag.TAG_INT)) this.sensorThreshold = Math.max(1, tag.getInt("sensor_threshold"));
      if (tag.contains("sensor_filter", Tag.TAG_INT)) this.sensorFilter = Math.max(0, tag.getInt("sensor_filter"));
      if (tag.contains("sensor_debounce", Tag.TAG_INT)) this.sensorDebounce = Mth.clamp(tag.getInt("sensor_debounce"), 0, MAX_SENSOR_DEBOUNCE);
      if (tag.contains("sensor_light_on", Tag.TAG_INT)) this.sensorLightOn = Mth.clamp(tag.getInt("sensor_light_on"), 0, 15);
      if (tag.contains("sensor_light_off", Tag.TAG_INT)) this.sensorLightOff = Mth.clamp(tag.getInt("sensor_light_off"), 0, 15);
      this.gaugeComparatorMode = tag.getBoolean("gauge_comparator");
      if (tag.contains("linked_input", Tag.TAG_INT)) this.linkedInputPower = Mth.clamp(tag.getInt("linked_input"), 0, 15);

      this.links.clear();
      if (tag.contains("links", Tag.TAG_LIST)) {
         ListTag list = tag.getList("links", Tag.TAG_COMPOUND);
         for (Tag entry : list) {
            CompoundTag link = (CompoundTag)entry;
            String blockId = link.getString("block");
            if (ResourceLocation.tryParse(blockId) == null) {
               continue;
            }
            this.links.add(new LinkTarget(link.getLong("pos"), blockId, LinkMode.byId(link.getInt("mode"))));
         }
      }
   }

   @Override
   protected void saveAdditional(CompoundTag tag, Provider registries) {
      super.saveAdditional(tag, registries);
      tag.putInt("output_power", this.outputPower);
      tag.putBoolean("inverted", this.inverted);
      tag.putBoolean("weak", this.weak);
      tag.putBoolean("no_output", this.noOutput);
      tag.putLong("enabled_sides", this.enabledSides);
      tag.putInt("pulse_ticks", this.configuredPulseTicks);
      tag.putLong("pulse_deadline", this.pulseOffDeadline);

      tag.putInt("timer_on", this.timerOnTicks);
      tag.putInt("timer_off", this.timerOffTicks);
      tag.putInt("timer_ramp", this.timerRamp);
      tag.putInt("timer_power", this.timerRawPower);
      tag.putInt("timer_countdown", this.timerCountdown);
      tag.putBoolean("timer_high", this.timerHigh);

      tag.putInt("sensor_range", this.sensorRange);
      tag.putInt("sensor_threshold", this.sensorThreshold);
      tag.putInt("sensor_filter", this.sensorFilter);
      tag.putInt("sensor_debounce", this.sensorDebounce);
      tag.putInt("sensor_light_on", this.sensorLightOn);
      tag.putInt("sensor_light_off", this.sensorLightOff);
      tag.putBoolean("gauge_comparator", this.gaugeComparatorMode);
      tag.putInt("linked_input", this.linkedInputPower);

      ListTag linksTag = new ListTag();
      for (LinkTarget link : this.links) {
         CompoundTag entry = new CompoundTag();
         entry.putLong("pos", link.targetPos());
         entry.putString("block", link.blockId());
         entry.putInt("mode", link.mode().id());
         linksTag.add(entry);
      }
      if (!linksTag.isEmpty()) {
         tag.put("links", linksTag);
      }
   }

   public void intervalStep(Level level, BlockPos pos, BlockState state) {
      int before = this.timerEffectivePower(state);
      if (!state.getValue(ControlsBlockTypes.POWERED) || this.timerOnTicks <= 0 || this.timerOffTicks <= 0 || this.outputPower <= 0) {
         this.timerRawPower = 0;
         this.timerCountdown = 20;
      } else if (--this.timerCountdown <= 0) {
         if (!this.timerHigh) {
            this.timerCountdown = this.timerOnTicks;
            if (this.timerRamp <= 0 || (this.timerRawPower += this.timerRamp) >= this.outputPower) {
               this.timerRawPower = this.outputPower;
               this.timerHigh = true;
            } else {
               this.timerCountdown = 5;
            }
         } else {
            this.timerCountdown = this.timerOffTicks;
            if (this.timerRamp <= 0 || (this.timerRawPower -= this.timerRamp) <= 0) {
               this.timerRawPower = 0;
               this.timerHigh = false;
            } else {
               this.timerCountdown = 5;
            }
         }
      }

      int after = this.timerEffectivePower(state);
      if (before != after) {
         this.setChanged();
         level.updateNeighborsAt(pos, state.getBlock());
         if (state.hasProperty(ControlsBlockTypes.FACING)) {
            level.updateNeighborsAt(pos.relative(state.getValue(ControlsBlockTypes.FACING).getOpposite()), state.getBlock());
         }
         this.activateLinks(level, pos, after, after > 0 ? 15 : 0, (before > 0) != (after > 0));
      }
   }

   public enum LinkMode {
      AS_STATE(0),
      INV_STATE(1),
      ACTIVATE(2),
      DEACTIVATE(3),
      TOGGLE(4);

      private final int id;

      LinkMode(int id) {
         this.id = id;
      }

      public int id() {
         return this.id;
      }

      public LinkMode next() {
         LinkMode[] values = values();
         return values[(this.ordinal() + 1) % values.length];
      }

      public static LinkMode byId(int id) {
         for (LinkMode value : values()) {
            if (value.id == id) return value;
         }
         return TOGGLE;
      }
   }

   public record LinkTarget(long targetPos, String blockId, LinkMode mode) {
   }
}
