package com.oblixorprime.engineersdecorreforged.rsgauges;

import com.oblixorprime.engineersdecorreforged.ModBlocks;
import com.oblixorprime.engineersdecorreforged.ModItems;
import com.oblixorprime.engineersdecorreforged.ReforgedConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public final class ControlsModule {
   private static final Set<String> GAUGES = Set.of(
      "glass_vertical_bar_gauge",
      "industrial_analog_angular_gauge",
      "industrial_analog_horizontal_gauge",
      "industrial_small_digital_gauge",
      "industrial_tube_gauge",
      "industrial_vertical_bar_gauge"
   );
   private static final Set<String> INDICATORS = Set.of(
      "industrial_alarm_lamp",
      "industrial_alarm_siren",
      "industrial_blinking_led",
      "industrial_green_blinking_led",
      "industrial_green_led",
      "industrial_red_blinking_led",
      "industrial_red_led",
      "industrial_white_blinking_led",
      "industrial_white_led",
      "industrial_yellow_blinking_led",
      "industrial_yellow_led",
      "rustic_semaphore"
   );
   private static final Set<String> CONTACT_PLATES = Set.of(
      "glass_contact_mat",
      "glass_door_contact_mat",
      "industrial_contact_mat",
      "industrial_door_contact_mat",
      "industrial_shock_sensitive_contact_mat",
      "rustic_contact_plate",
      "rustic_door_contact_plate",
      "rustic_shock_sensitive_plate"
   );
   private static final Set<String> FALLTHROUGH_FRAMES = Set.of("industrial_fallthrough_detector", "rustic_fallthrough_detector");
   private static final Set<String> CONTACT_TRAPDOORS = Set.of(
      "industrial_high_sensitive_trapdoor", "industrial_shock_sensitive_trapdoor", "rustic_high_sensitive_trapdoor", "rustic_shock_sensitive_trapdoor"
   );
   private static final Set<String> POWER_PLANTS = Set.of("red_power_plant", "yellow_power_plant");
   private static final Set<String> PULSE_SWITCHES = Set.of(
      "arrow_target",
      "door_sensor_switch",
      "glass_button",
      "glass_small_button",
      "glass_touch_button",
      "industrial_button",
      "industrial_double_pole_button",
      "industrial_fenced_button",
      "industrial_knock_button",
      "oldfancy_button",
      "oldfancy_small_button",
      "oldfancy_spring_reset_chain",
      "rustic_button",
      "rustic_nail_button",
      "rustic_small_button",
      "rustic_spring_reset_chain"
   );
   private static final Set<String> SENSITIVE_GLASS = Set.of("orange_sensitiveglass", "sensitive_glass_block", "stained_sensitiveglass");
   private static final Set<String> SWITCHLINK_RECEIVERS = Set.of("industrial_switchlink_receiver", "industrial_switchlink_receiver_analog");
   private static final Set<String> SWITCHLINK_CASED_RECEIVERS = Set.of("industrial_switchlink_cased_receiver");
   private static final Set<String> SWITCHLINK_PULSE_RECEIVERS = Set.of("industrial_switchlink_pulse_receiver");
   private static final Set<String> SWITCHLINK_CASED_PULSE_RECEIVERS = Set.of("industrial_switchlink_cased_pulse_receiver");
   private static final List<String> CONTROL_NAMES = List.of(
      "arrow_target",
      "door_sensor_switch",
      "elevator_button",
      "glass_button",
      "glass_contact_mat",
      "glass_day_timer",
      "glass_door_contact_mat",
      "glass_entity_detector",
      "glass_interval_timer",
      "glass_linear_entity_detector",
      "glass_rotary_switch",
      "glass_small_button",
      "glass_touch_button",
      "glass_touch_switch",
      "glass_vertical_bar_gauge",
      "industrial_alarm_lamp",
      "industrial_alarm_siren",
      "industrial_analog_angular_gauge",
      "industrial_analog_horizontal_gauge",
      "industrial_blinking_led",
      "industrial_block_detector",
      "industrial_button",
      "industrial_comparator_switch",
      "industrial_contact_mat",
      "industrial_day_timer",
      "industrial_dimmer",
      "industrial_door_contact_mat",
      "industrial_double_pole_button",
      "industrial_entity_detector",
      "industrial_estop_switch",
      "industrial_fallthrough_detector",
      "industrial_fenced_button",
      "industrial_foot_button",
      "industrial_green_blinking_led",
      "industrial_green_led",
      "industrial_high_sensitive_trapdoor",
      "industrial_hopper_switch",
      "industrial_interval_timer",
      "industrial_knock_button",
      "industrial_knock_switch",
      "industrial_lever",
      "industrial_light_sensor",
      "industrial_lightning_sensor",
      "industrial_linear_entity_detector",
      "industrial_machine_switch",
      "industrial_pull_handle",
      "industrial_rain_sensor",
      "industrial_red_blinking_led",
      "industrial_red_led",
      "industrial_rotary_lever",
      "industrial_rotary_machine_switch",
      "industrial_shock_sensitive_contact_mat",
      "industrial_shock_sensitive_trapdoor",
      "industrial_small_digital_gauge",
      "industrial_small_lever",
      "industrial_switchlink_cased_pulse_receiver",
      "industrial_switchlink_cased_receiver",
      "industrial_switchlink_pulse_receiver",
      "industrial_switchlink_pulse_relay",
      "industrial_switchlink_receiver",
      "industrial_switchlink_receiver_analog",
      "industrial_switchlink_relay",
      "industrial_switchlink_relay_analog",
      "industrial_tube_gauge",
      "industrial_vertical_bar_gauge",
      "industrial_white_blinking_led",
      "industrial_white_led",
      "industrial_yellow_blinking_led",
      "industrial_yellow_led",
      "industrialswitch",
      "light_switch",
      "oldfancy_bistableswitch1",
      "oldfancy_bistableswitch2",
      "oldfancy_button",
      "oldfancy_small_button",
      "oldfancy_spring_reset_chain",
      "orange_sensitiveglass",
      "red_power_plant",
      "rustic_angular_lever",
      "rustic_button",
      "rustic_contact_plate",
      "rustic_door_contact_plate",
      "rustic_fallthrough_detector",
      "rustic_high_sensitive_trapdoor",
      "rustic_lever",
      "rustic_nail_button",
      "rustic_nail_lever",
      "rustic_semaphore",
      "rustic_shock_sensitive_plate",
      "rustic_shock_sensitive_trapdoor",
      "rustic_small_button",
      "rustic_spring_reset_chain",
      "rustic_two_hinge_lever",
      "sensitive_glass_block",
      "stained_sensitiveglass",
      "valve_wheel_switch",
      "yellow_power_plant"
   );
   private static final Set<String> WIRELESS_CONTROLS = Set.of(
      "switchlink_pearl",
      "industrial_switchlink_receiver",
      "industrial_switchlink_receiver_analog",
      "industrial_switchlink_cased_receiver",
      "industrial_switchlink_pulse_receiver",
      "industrial_switchlink_pulse_relay",
      "industrial_switchlink_relay",
      "industrial_switchlink_relay_analog",
      "industrial_switchlink_cased_pulse_receiver"
   );
   private static final Set<String> RETRO_INDUSTRIAL_CONTROLS = Set.of("industrialswitch", "light_switch", "valve_wheel_switch");
   private static final List<DeferredBlock<? extends Block>> MUTABLE_CONTROLS = new ArrayList<>();
   public static final List<DeferredBlock<? extends Block>> CONTROLS = Collections.unmodifiableList(MUTABLE_CONTROLS);
   public static final DeferredItem<Item> SWITCHLINK_PEARL = ModItems.registerItem("switchlink_pearl", () -> new SwitchLinkPearlItem(new Properties()));
   private static boolean initialized;

   private ControlsModule() {
   }

   public static void init() {
      if (!initialized) {
         initialized = true;
         registerAll();
      }
   }

   private static void registerAll() {
      for (String name : CONTROL_NAMES) {
         register(name, factory(name));
      }
   }

   public static boolean isControlContent(String name) {
      return "switchlink_pearl".equals(name) || CONTROL_NAMES.contains(name);
   }

   public static boolean shouldShowInCreativeTab(String name) {
      if (!isControlContent(name)) {
         return true;
      }

      if (!ReforgedConfig.isEnabled(ReforgedConfig.ENABLE_REDSTONE_CONTROLS)) {
         return false;
      }
      if (!isStyleEnabled(name)) {
         return false;
      }
      if (GAUGES.contains(name) && !ReforgedConfig.isEnabled(ReforgedConfig.ENABLE_GAUGES)) {
         return false;
      }
      if (INDICATORS.contains(name) && !ReforgedConfig.isEnabled(ReforgedConfig.ENABLE_INDICATORS)) {
         return false;
      }
      if (isWirelessControl(name) && !ReforgedConfig.isEnabled(ReforgedConfig.ENABLE_WIRELESS_CONTROLS)) {
         return false;
      }
      if (isSoundIndicator(name) && !ReforgedConfig.isEnabled(ReforgedConfig.ENABLE_SOUND_INDICATORS)) {
         return false;
      }
      if (isSensor(name) && !ReforgedConfig.isEnabled(ReforgedConfig.ENABLE_SENSORS)) {
         return false;
      }
      return true;
   }

   public static int configuredControlCount() {
      return CONTROL_NAMES.size();
   }

   private static boolean isSensor(String name) {
      return name.contains("day_timer")
         || name.contains("rain_sensor")
         || name.contains("lightning_sensor")
         || name.contains("light_sensor")
         || name.contains("player_detector")
         || name.contains("linear_entity_detector")
         || name.contains("entity_detector")
         || name.contains("block_detector")
         || name.contains("interval_timer")
         || "door_sensor_switch".equals(name);
   }

   private static boolean isSoundIndicator(String name) {
      return "industrial_alarm_siren".equals(name);
   }

   private static boolean isWirelessControl(String name) {
      return WIRELESS_CONTROLS.contains(name);
   }

   private static boolean isStyleEnabled(String name) {
      if (name.startsWith("industrial_")) {
         return ReforgedConfig.isEnabled(ReforgedConfig.ENABLE_STYLE_INDUSTRIAL);
      } else if (name.startsWith("oldfancy_")) {
         return ReforgedConfig.isEnabled(ReforgedConfig.ENABLE_STYLE_OLD_FANCY);
      } else if (name.startsWith("rustic_")) {
         return ReforgedConfig.isEnabled(ReforgedConfig.ENABLE_STYLE_RUSTIC);
      } else if (name.startsWith("glass_") || SENSITIVE_GLASS.contains(name)) {
         return ReforgedConfig.isEnabled(ReforgedConfig.ENABLE_STYLE_GLASS);
      } else if (RETRO_INDUSTRIAL_CONTROLS.contains(name)) {
         return ReforgedConfig.isEnabled(ReforgedConfig.ENABLE_STYLE_RETRO_INDUSTRIAL);
      }
      return true;
   }

   private static Supplier<? extends Block> factory(String name) {
      if (GAUGES.contains(name)) {
         return () -> new ControlsBlockTypes.GaugeBlock(controlProperties());
      } else if (INDICATORS.contains(name)) {
         return () -> new ControlsBlockTypes.BooleanIndicatorBlock(indicatorProperties());
      } else {
         ControlsBlockTypes.ContactShape contactShape = contactShape(name);
         if (contactShape != null) {
            return () -> new ControlsBlockTypes.ContactSwitchBlock(controlProperties(), contactShape);
         } else if ("door_sensor_switch".equals(name)) {
            return () -> new ControlsBlockTypes.SensorSwitchBlock(controlProperties(), ControlsBlockTypes.SensorKind.PLAYER);
         } else if (PULSE_SWITCHES.contains(name)) {
            return () -> new ControlsBlockTypes.PulseSwitchBlock(controlProperties(), 25);
         } else if (SENSITIVE_GLASS.contains(name)) {
            return () -> new ControlsBlockTypes.SensitiveGlassBlock(glassProperties());
         } else if (SWITCHLINK_CASED_RECEIVERS.contains(name)) {
            return () -> new ControlsBlockTypes.CasedSwitchLinkReceiverBlock(controlProperties());
         } else if (SWITCHLINK_RECEIVERS.contains(name)) {
            return () -> new ControlsBlockTypes.SwitchLinkReceiverBlock(controlProperties());
         } else if (SWITCHLINK_CASED_PULSE_RECEIVERS.contains(name)) {
            return () -> new ControlsBlockTypes.CasedSwitchLinkPulseReceiverBlock(controlProperties());
         } else if (SWITCHLINK_PULSE_RECEIVERS.contains(name)) {
            return () -> new ControlsBlockTypes.SwitchLinkPulseReceiverBlock(controlProperties());
         } else if ("elevator_button".equals(name)) {
            return () -> new ControlsBlockTypes.ElevatorButtonBlock(controlProperties());
         } else if ("industrial_dimmer".equals(name)) {
            return () -> new ControlsBlockTypes.DimmerBlock(controlProperties());
         } else if ("industrial_comparator_switch".equals(name)) {
            return () -> new ControlsBlockTypes.ComparatorSwitchBlock(controlProperties());
         } else if (name.contains("day_timer")) {
            return () -> new ControlsBlockTypes.SensorSwitchBlock(controlProperties(), ControlsBlockTypes.SensorKind.DAY);
         } else if (name.contains("rain_sensor")) {
            return () -> new ControlsBlockTypes.SensorSwitchBlock(controlProperties(), ControlsBlockTypes.SensorKind.RAIN);
         } else if (name.contains("lightning_sensor")) {
            return () -> new ControlsBlockTypes.SensorSwitchBlock(controlProperties(), ControlsBlockTypes.SensorKind.LIGHTNING);
         } else if (name.contains("light_sensor")) {
            return () -> new ControlsBlockTypes.SensorSwitchBlock(controlProperties(), ControlsBlockTypes.SensorKind.LIGHT);
         } else if (name.contains("player_detector")) {
            return () -> new ControlsBlockTypes.SensorSwitchBlock(controlProperties(), ControlsBlockTypes.SensorKind.PLAYER);
         } else if (name.contains("linear_entity_detector")) {
            return () -> new ControlsBlockTypes.SensorSwitchBlock(controlProperties(), ControlsBlockTypes.SensorKind.LINEAR_ENTITY);
         } else if (name.contains("entity_detector")) {
            return () -> new ControlsBlockTypes.SensorSwitchBlock(controlProperties(), ControlsBlockTypes.SensorKind.ENTITY);
         } else if (name.contains("block_detector")) {
            return () -> new ControlsBlockTypes.SensorSwitchBlock(controlProperties(), ControlsBlockTypes.SensorKind.BLOCK);
         } else {
            return name.contains("interval_timer")
               ? () -> new ControlsBlockTypes.PulseSwitchBlock(controlProperties(), 40)
               : () -> new ControlsBlockTypes.ToggleSwitchBlock(controlProperties());
         }
      }
   }

   private static ControlsBlockTypes.ContactShape contactShape(String name) {
      if (CONTACT_PLATES.contains(name)) {
         return ControlsBlockTypes.ContactShape.CONTACT_PLATE;
      } else if (FALLTHROUGH_FRAMES.contains(name)) {
         return ControlsBlockTypes.ContactShape.FALLTHROUGH_FRAME;
      } else if (CONTACT_TRAPDOORS.contains(name)) {
         return ControlsBlockTypes.ContactShape.TRAPDOOR_PANEL;
      } else if (POWER_PLANTS.contains(name)) {
         return ControlsBlockTypes.ContactShape.POWER_PLANT;
      } else {
         return "industrial_foot_button".equals(name) ? ControlsBlockTypes.ContactShape.ATTACHED_BUTTON : null;
      }
   }

   private static <T extends Block> void register(String name, Supplier<T> supplier) {
      DeferredBlock<? extends Block> block = ModBlocks.registerWithItem(name, supplier);
      MUTABLE_CONTROLS.add(block);
   }

   private static net.minecraft.world.level.block.state.BlockBehaviour.Properties controlProperties() {
      return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
         .mapColor(MapColor.METAL)
         .strength(0.4F, 1.0F)
         .sound(SoundType.METAL)
         .noOcclusion();
   }

   private static net.minecraft.world.level.block.state.BlockBehaviour.Properties indicatorProperties() {
      return controlProperties()
         .lightLevel(state -> state.hasProperty(ControlsBlockTypes.POWER_BOOL) && state.getValue(ControlsBlockTypes.POWER_BOOL) ? 10 : 0);
   }

   private static net.minecraft.world.level.block.state.BlockBehaviour.Properties glassProperties() {
      return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
         .mapColor(MapColor.NONE)
         .strength(0.3F, 1.0F)
         .sound(SoundType.GLASS)
         .noOcclusion()
         .lightLevel(state -> state.hasProperty(ControlsBlockTypes.POWERED) && state.getValue(ControlsBlockTypes.POWERED) ? 8 : 0);
   }
}
