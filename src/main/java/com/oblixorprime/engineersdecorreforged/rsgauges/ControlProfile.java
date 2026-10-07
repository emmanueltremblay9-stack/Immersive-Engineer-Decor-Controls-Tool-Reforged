package com.oblixorprime.engineersdecorreforged.rsgauges;

import java.util.Set;

/**
 * Semantic configuration flags reconstructed from the selected RSG 1.2.18
 * source. CurseForge 1.2.19 only fixes the version-source location, so these
 * gameplay flags are also the selected 1.2.19 behavior contract.
 */
public final class ControlProfile {
   public static final long DATA_INVERTED = 0x0000000000000100L;
   public static final long DATA_WEAK = 0x0000000000000200L;
   public static final long DATA_NO_OUTPUT = 0x0000000000000400L;
   public static final long DATA_SIDE_BOTTOM = 0x0000000000001000L;
   public static final long DATA_SIDE_TOP = 0x0000000000002000L;
   public static final long DATA_SIDE_FRONT = 0x0000000000004000L;
   public static final long DATA_SIDE_BACK = 0x0000000000008000L;
   public static final long DATA_SIDE_LEFT = 0x0000000000010000L;
   public static final long DATA_SIDE_RIGHT = 0x0000000000020000L;
   public static final long DATA_SIDE_ALL = 0x000000000003F000L;

   public static final long INVERTABLE = 0x0000000000100000L;
   public static final long WEAKABLE = 0x0000000000200000L;
   public static final long PULSE_TIME_CONFIGURABLE = 0x0000000000400000L;
   public static final long TOUCH_CONFIGURABLE = 0x0000000000800000L;
   public static final long PULSE_EXTENDABLE = 0x0000000001000000L;
   public static final long LEFT_CLICK_RESETTABLE = 0x0000000002000000L;
   public static final long BISTABLE = 0x0000000010000000L;
   public static final long PULSE = 0x0000000020000000L;
   public static final long CONTACT = 0x0000000040000000L;
   public static final long TIMER_DAYTIME = 0x0000000100000000L;
   public static final long TIMER_INTERVAL = 0x0000000200000000L;
   public static final long SENSOR_VOLUME = 0x0000000400000000L;
   public static final long SENSOR_LINEAR = 0x0000000800000000L;
   public static final long SENSOR_LIGHT = 0x0000001000000000L;
   public static final long SENSOR_RAIN = 0x0000002000000000L;
   public static final long SENSOR_LIGHTNING = 0x0000004000000000L;
   public static final long SENSOR_BLOCK = 0x0000008000000000L;
   public static final long PROJECTILE_SENSE_ON = 0x0000100000000000L;
   public static final long PROJECTILE_SENSE_OFF = 0x0000200000000000L;
   public static final long PROJECTILE_SENSE = PROJECTILE_SENSE_ON | PROJECTILE_SENSE_OFF;
   public static final long SHOCK_SENSITIVE = 0x0000400000000000L;
   public static final long HIGH_SENSITIVE = 0x0000800000000000L;
   public static final long SIDES_CONFIGURABLE = 0x0040000000000000L;
   public static final long LINK_SOURCE_SUPPORT = 0x0100000000000000L;
   public static final long LINK_TARGET_SUPPORT = 0x0200000000000000L;
   public static final long LINK_SENDER = 0x0400000000000000L;

   private static final long COMMON_LATCHING = BISTABLE | WEAKABLE | INVERTABLE | LINK_TARGET_SUPPORT | LINK_SOURCE_SUPPORT;
   private static final long COMMON_PULSE = PULSE | WEAKABLE | INVERTABLE | PULSE_EXTENDABLE | PULSE_TIME_CONFIGURABLE
      | LEFT_CLICK_RESETTABLE | LINK_TARGET_SUPPORT | LINK_SOURCE_SUPPORT;

   private static final Set<String> LATCHING = Set.of(
      "industrial_small_lever",
      "industrial_lever",
      "industrial_rotary_lever",
      "industrial_rotary_machine_switch",
      "industrial_machine_switch",
      "industrial_knock_switch",
      "rustic_lever",
      "rustic_two_hinge_lever",
      "rustic_angular_lever",
      "rustic_nail_lever",
      "glass_rotary_switch",
      "glass_touch_switch",
      "oldfancy_bistableswitch1",
      "oldfancy_bistableswitch2",
      "light_switch",
      "valve_wheel_switch",
      "industrialswitch"
   );

   private static final Set<String> PROJECTILE_PULSES = Set.of(
      "industrial_button",
      "rustic_button",
      "rustic_small_button",
      "oldfancy_button",
      "oldfancy_small_button",
      "arrow_target",
      "elevator_button"
   );

   private static final Set<String> PULSES = Set.of(
      "industrial_button",
      "industrial_fenced_button",
      "industrial_double_pole_button",
      "industrial_foot_button",
      "industrial_pull_handle",
      "industrial_knock_button",
      "rustic_button",
      "rustic_small_button",
      "rustic_spring_reset_chain",
      "rustic_nail_button",
      "glass_button",
      "glass_small_button",
      "glass_touch_button",
      "oldfancy_button",
      "oldfancy_spring_reset_chain",
      "oldfancy_small_button",
      "arrow_target",
      "elevator_button"
   );

   private static final Set<String> CONTACTS = Set.of(
      "industrial_door_contact_mat",
      "industrial_contact_mat",
      "industrial_shock_sensitive_contact_mat",
      "industrial_shock_sensitive_trapdoor",
      "industrial_high_sensitive_trapdoor",
      "industrial_fallthrough_detector",
      "rustic_door_contact_plate",
      "rustic_contact_plate",
      "rustic_shock_sensitive_plate",
      "rustic_shock_sensitive_trapdoor",
      "rustic_high_sensitive_trapdoor",
      "rustic_fallthrough_detector",
      "glass_door_contact_mat",
      "glass_contact_mat",
      "yellow_power_plant",
      "red_power_plant"
   );

   private ControlProfile() {
   }

   public static long forName(String name) {
      if (LATCHING.contains(name)) {
         long config = COMMON_LATCHING;
         if ("industrial_estop_switch".equals(name)) {
            config |= PROJECTILE_SENSE_OFF;
         }
         if ("industrial_hopper_switch".equals(name)) {
            config |= DATA_WEAK;
         }
         if ("industrial_knock_switch".equals(name)) {
            config |= DATA_SIDE_ALL | SIDES_CONFIGURABLE;
         }
         return config;
      }

      if ("industrial_estop_switch".equals(name)) {
         return COMMON_LATCHING | PROJECTILE_SENSE_OFF;
      }
      if ("industrial_hopper_switch".equals(name)) {
         return COMMON_LATCHING | DATA_WEAK;
      }

      if (PULSES.contains(name)) {
         long config = COMMON_PULSE;
         if (PROJECTILE_PULSES.contains(name)) {
            config |= PROJECTILE_SENSE;
         }
         if ("industrial_knock_button".equals(name)) {
            config |= DATA_SIDE_ALL | SIDES_CONFIGURABLE;
         }
         return config;
      }

      if (CONTACTS.contains(name)) {
         long config = CONTACT | WEAKABLE | INVERTABLE | PULSE_TIME_CONFIGURABLE | LINK_SOURCE_SUPPORT;
         if (name.contains("door_")) {
            config |= TOUCH_CONFIGURABLE;
         }
         if (name.contains("shock_sensitive")) {
            config |= SHOCK_SENSITIVE;
         }
         if (name.contains("high_sensitive")) {
            config |= HIGH_SENSITIVE | SHOCK_SENSITIVE | LINK_TARGET_SUPPORT;
         }
         if (name.contains("trapdoor")) {
            config |= LINK_TARGET_SUPPORT;
         }
         return config;
      }

      if ("door_sensor_switch".equals(name)) {
         return PULSE | WEAKABLE | INVERTABLE | LINK_SOURCE_SUPPORT;
      }

      if (name.contains("day_timer")) {
         long config = TIMER_DAYTIME | WEAKABLE | INVERTABLE | TOUCH_CONFIGURABLE | LINK_SOURCE_SUPPORT;
         if (name.startsWith("glass_")) {
            config |= LINK_TARGET_SUPPORT;
         }
         return config;
      }

      if (name.contains("interval_timer")) {
         return TIMER_INTERVAL | WEAKABLE | INVERTABLE | TOUCH_CONFIGURABLE | LINK_TARGET_SUPPORT;
      }

      if (name.contains("linear_entity_detector")) {
         return SENSOR_LINEAR | PULSE_TIME_CONFIGURABLE | WEAKABLE | INVERTABLE | TOUCH_CONFIGURABLE
            | LINK_SOURCE_SUPPORT | (name.startsWith("glass_") ? LINK_TARGET_SUPPORT : 0L);
      }

      if (name.contains("entity_detector")) {
         return SENSOR_VOLUME | PULSE_TIME_CONFIGURABLE | WEAKABLE | INVERTABLE | TOUCH_CONFIGURABLE
            | LINK_SOURCE_SUPPORT | (name.startsWith("glass_") ? LINK_TARGET_SUPPORT : 0L);
      }

      if (name.contains("light_sensor")) {
         return SENSOR_LIGHT | WEAKABLE | INVERTABLE | TOUCH_CONFIGURABLE | LINK_SOURCE_SUPPORT;
      }
      if (name.contains("rain_sensor")) {
         return SENSOR_RAIN | WEAKABLE | INVERTABLE | TOUCH_CONFIGURABLE | LINK_SOURCE_SUPPORT;
      }
      if (name.contains("lightning_sensor")) {
         return SENSOR_LIGHTNING | WEAKABLE | INVERTABLE | TOUCH_CONFIGURABLE | LINK_SOURCE_SUPPORT;
      }
      if (name.contains("block_detector")) {
         return SENSOR_BLOCK | WEAKABLE | INVERTABLE | TOUCH_CONFIGURABLE | LINK_SOURCE_SUPPORT
            | DATA_SIDE_BOTTOM | DATA_SIDE_TOP | DATA_SIDE_FRONT | DATA_SIDE_LEFT | DATA_SIDE_RIGHT | SIDES_CONFIGURABLE;
      }

      if ("industrial_dimmer".equals(name)) {
         return WEAKABLE | TOUCH_CONFIGURABLE | LINK_SOURCE_SUPPORT;
      }
      if ("industrial_comparator_switch".equals(name)) {
         return WEAKABLE | INVERTABLE | TOUCH_CONFIGURABLE | LINK_SOURCE_SUPPORT;
      }

      if ("industrial_switchlink_receiver".equals(name)) {
         return BISTABLE | WEAKABLE | INVERTABLE | LINK_TARGET_SUPPORT | LINK_SOURCE_SUPPORT;
      }
      if ("industrial_switchlink_receiver_analog".equals(name)) {
         return BISTABLE | WEAKABLE | LINK_TARGET_SUPPORT | LINK_SOURCE_SUPPORT;
      }
      if ("industrial_switchlink_cased_receiver".equals(name)) {
         return BISTABLE | WEAKABLE | INVERTABLE | DATA_SIDE_ALL | SIDES_CONFIGURABLE | LINK_TARGET_SUPPORT | LINK_SOURCE_SUPPORT;
      }
      if ("industrial_switchlink_pulse_receiver".equals(name)) {
         return PULSE | LEFT_CLICK_RESETTABLE | WEAKABLE | INVERTABLE | PULSE_TIME_CONFIGURABLE
            | LINK_TARGET_SUPPORT | LINK_SOURCE_SUPPORT;
      }
      if ("industrial_switchlink_cased_pulse_receiver".equals(name)) {
         return PULSE | WEAKABLE | INVERTABLE | LEFT_CLICK_RESETTABLE | PULSE_TIME_CONFIGURABLE
            | DATA_SIDE_ALL | SIDES_CONFIGURABLE | LINK_TARGET_SUPPORT | LINK_SOURCE_SUPPORT;
      }
      if ("industrial_switchlink_relay".equals(name)) {
         return BISTABLE | LINK_SENDER | INVERTABLE | DATA_WEAK | LINK_TARGET_SUPPORT | LINK_SOURCE_SUPPORT;
      }
      if ("industrial_switchlink_relay_analog".equals(name)) {
         return BISTABLE | LINK_SENDER | INVERTABLE | DATA_WEAK | LINK_TARGET_SUPPORT | LINK_SOURCE_SUPPORT;
      }
      if ("industrial_switchlink_pulse_relay".equals(name)) {
         return PULSE | LINK_SENDER | INVERTABLE | DATA_WEAK | LEFT_CLICK_RESETTABLE | PULSE_TIME_CONFIGURABLE
            | LINK_TARGET_SUPPORT | LINK_SOURCE_SUPPORT;
      }

      return 0L;
   }

   public static boolean has(long config, long flag) {
      return (config & flag) != 0L;
   }

   public static boolean analogSwitchLink(String name) {
      return "industrial_switchlink_receiver_analog".equals(name) || "industrial_switchlink_relay_analog".equals(name);
   }
}
