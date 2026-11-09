#!/usr/bin/env python3
"""Run isolated, bounded native-client lamp mining and prediction/ACK regressions.

Unlike client_mixin_smoke.py, a title screen or a timed-out client cannot pass.
No external world, user options or installed Minecraft directory is used.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import struct
import secrets
import shutil
import subprocess
import sys
import tempfile

from client_mixin_smoke import FATAL_PATTERN, SmokeFailure, run_bounded
from renderer_profiles import PROFILES, prepare_renderer_dependencies, validate_renderer_profile
from flow_distance import validate_flow_distance_proof
from lithium_profile import prepare_lithium_dependency, validate_lithium_runtime


SUITE = "native-client-lamp-mining-v1"
APPROACHES = ("down", "north", "east", "south", "west")
EXPECTED_CASES = {f"{mode}-{side}" for mode in ("creative", "survival") for side in APPROACHES}


def validate_proof(proof: dict, nonce: str, logs: str, exit_code: int | None,
                   timed_out: bool, crash_files: list[Path]) -> dict:
    """Fail closed on missing/stale/incomplete/replayed or loading-only evidence."""
    if timed_out or exit_code != 0:
        raise SmokeFailure(f"Native gameplay client did not finish cleanly (exit={exit_code}, deadline={timed_out})")
    if crash_files:
        raise SmokeFailure("Native gameplay client wrote a crash report")
    fatal = FATAL_PATTERN.search(logs)
    if fatal:
        raise SmokeFailure(f"Fatal client diagnostic: {fatal.group(0)}")
    if proof.get("suite") != SUITE or proof.get("nonce") != nonce or proof.get("status") != "passed":
        raise SmokeFailure("Missing matching successful gameplay proof")
    if proof.get("actual_integrated_world") is not True or proof.get("expected_cases") != len(EXPECTED_CASES):
        raise SmokeFailure("Gameplay proof lacks its real integrated-world scope")
    if proof.get("fixture_attack_context_verified") is not True:
        raise SmokeFailure("Gameplay fixture did not verify an active attack binding under its isolation screen")
    cases = proof.get("cases")
    if not isinstance(cases, list) or len(cases) != len(EXPECTED_CASES):
        raise SmokeFailure("Incomplete native gameplay scenario matrix")
    if any(not isinstance(case, dict) for case in cases):
        raise SmokeFailure("Invalid native gameplay case")
    names = [case.get("case") for case in cases]
    if len(set(names)) != len(names) or set(names) != EXPECTED_CASES:
        raise SmokeFailure("Duplicate or missing native gameplay scenario")
    for case in cases:
        label = case["case"]
        if label != f"{case.get('mode')}-{case.get('approach')}":
            raise SmokeFailure(f"Inconsistent scenario identity: {label}")
        for flag in ("immediate_client_prediction_lamp_only", "authoritative_lamp_only",
                     "release_allows_native_deck_mining", "immediate_client_prediction_deck_removed",
                     "authoritative_deck_removed", "active_attack_binding", "synchronous_lamp_completion_event",
                     "equal_id_cross_side_players_verified"):
            if case.get(flag) is not True:
                raise SmokeFailure(f"{label} lacks {flag}")
        notifications = case.get("client_lamp_removal_notifications")
        if type(notifications) is not int or notifications != 1:
            raise SmokeFailure(f"{label} lacks exactly one synchronous native client lamp completion")
        for stage in ("lamp", "deck"):
            action = "START_DESTROY_BLOCK" if case["mode"] == "creative" else "STOP_DESTROY_BLOCK"
            if case.get(f"{stage}_completion_action") != action:
                raise SmokeFailure(f"{label} lacks the exact native {stage} completion action")
            sequence, ack = case.get(f"{stage}_sequence"), case.get(f"{stage}_ack_applied")
            if type(sequence) is not int or type(ack) is not int or sequence < 0 or ack < sequence:
                raise SmokeFailure(f"{label} lacks processed native {stage} ACK evidence")
        if case["deck_sequence"] <= case["lamp_sequence"]:
            raise SmokeFailure(f"{label} reused an old mining sequence")
        if type(case.get("held_ticks")) is not int or case["held_ticks"] < 12:
            raise SmokeFailure(f"{label} lacks a sustained held-input observation")
        if type(case.get("held_attack_cancellations")) is not int or case["held_attack_cancellations"] < case["held_ticks"]:
            raise SmokeFailure(f"{label} did not suppress every continued attack")
        if type(case.get("held_extra_mining_packets")) is not int or case["held_extra_mining_packets"] != 0:
            raise SmokeFailure(f"{label} leaked native mining packets after lamp removal")
        expected = 1 if case["mode"] == "survival" else 0
        if case.get("lamp_drops_or_inventory") != expected or case.get("lamp_tool_damage") != expected:
            raise SmokeFailure(f"{label} has incorrect authoritative lamp loot/tool wear")
        if type(case.get("native_mining_ticks")) is not int or case["native_mining_ticks"] < expected:
            raise SmokeFailure(f"{label} did not exercise native mining duration")
        checks = case.get("survival_lamp_hold_checks")
        if type(checks) is not int or (checks != case["native_mining_ticks"] if expected else checks != 0):
            raise SmokeFailure(f"{label} did not preserve its own intent on every survival lamp HOLD")
    marker = f"IEDCTR_CLIENT_GAMEPLAY PASSED nonce={nonce} cases={len(EXPECTED_CASES)}"
    if marker not in logs:
        raise SmokeFailure("Missing current-run successful gameplay completion marker")
    return proof


def validate_fluidlogging_proof(proof: dict, nonce: str) -> dict:
    blocks = {f"immersive_engineer_decor_controls_tool_reforged:{name}"
              for name in ("steel_catwalk", "steel_catwalk_stairs", "steel_catwalk_ta", "steel_railing")}
    fluids = {"minecraft:water", "minecraft:lava", "immersiveengineering:creosote"}
    expected = {(block, fluid, sync) for block in blocks for fluid in fluids for sync in ("update", "chunk")}
    if (proof.get("suite") != "native-fluidlogging-client-v1" or proof.get("nonce") != nonce
            or proof.get("status") != "passed" or proof.get("actual_integrated_world") is not True):
        raise SmokeFailure("Missing matching native fluidlogging client proof")
    cases = proof.get("cases")
    if not isinstance(cases, list) or len(cases) != len(expected):
        raise SmokeFailure("Incomplete native fluidlogging scenario matrix")
    seen = set()
    for case in cases:
        if not isinstance(case, dict):
            raise SmokeFailure("Invalid fluidlogging case")
        identity = (case.get("block"), case.get("fluid"), case.get("sync"))
        if identity not in expected or identity in seen:
            raise SmokeFailure("Duplicate or unexpected fluidlogging case")
        seen.add(identity)
        vertices = case.get("native_textured_vertices")
        if (case.get("client_world_and_chunk_source") is not True or case.get("client_drain_cleared") is not True
                or type(vertices) is not int or vertices <= 0 or vertices % 4 != 0):
            raise SmokeFailure(f"Fluidlogging case lacks synchronization or textured geometry: {identity}")
    return proof



def validate_canal_proof(proof: dict, nonce: str, directory: Path) -> dict:
    import struct
    expected = {"stairs-wet", "stairs-dry-control", "catwalk-wet", "raised-wet",
                "stairs-railed-wet", "stairs-railed-reversed-wet",
                "stairs-flow-adjacent-natural", "stairs-flow-adjacent-stored"}
    if (proof.get("suite") != "native-water-canal-diagnostic-v2"
            or proof.get("nonce") != nonce or proof.get("status") != "captured"):
        raise SmokeFailure("Missing current canal scene proof")
    cases = proof.get("cases")
    if not isinstance(cases, list) or len(cases) != len(expected):
        raise SmokeFailure("Incomplete canal scene matrix")
    seen = set()
    for case in cases:
        name = case.get("case")
        if name not in expected or name in seen:
            raise SmokeFailure("Duplicate or unexpected canal scene")
        seen.add(name)
        wet = name not in {"stairs-dry-control", "stairs-flow-adjacent-natural"}
        if case.get("source") is not wet:
            raise SmokeFailure(f"Canal source state mismatch: {name}")
        quads = case.get("upper_surface_quads")
        if type(quads) is not int or (quads != 0 if name=="stairs-dry-control" else quads < 1):
            raise SmokeFailure(f"Canal lacks the expected exposed upper fluid surface: {name}")
        if name=="stairs-flow-adjacent-natural":
            height=case.get("cell_fluid_height")
            if "minecraft:flowing_water" not in case.get("fluid_state", "") or type(height) not in (int,float) or not 0 < height < case.get("adjacent_fluid_height",0):
                raise SmokeFailure("Natural stair cell lacks its lower non-source water level")
        if name.startswith("stairs-flow-adjacent-"):
            height = case.get("adjacent_fluid_height")
            if (case.get("adjacent_water") is not True or case.get("adjacent_source") is not False
                    or type(height) not in (int, float) or not 0 < height < 8/9):
                raise SmokeFailure(f"Canal lacks actual lower flowing water beside stairs: {name}")
        for angle in ("oblique", "overhead"):
            filename = f"{name}-{angle}.png"
            if case.get(f"{angle}_screenshot") != filename:
                raise SmokeFailure("Unexpected canal screenshot identity")
            image = directory / "canal-screenshots" / filename
            data = image.read_bytes()
            if len(data) < 1000 or data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
                raise SmokeFailure("Canal screenshot is absent or invalid")
            width, height = struct.unpack(">II", data[16:24])
            if width < 600 or height < 400:
                raise SmokeFailure("Canal screenshot is too small to inspect")
    return proof




def validate_smelter_gui_proof(proof: dict, nonce: str, executed_sha: str, pr_head_sha: str,
                               directory: Path, window_width: int, window_height: int,
                               language: str, logs: str) -> dict:
    """Validate live-menu identity/state and exact fresh Minecraft framebuffer captures."""
    if (proof.get("suite") != "native-smelter-gui-v1" or proof.get("nonce") != nonce
            or proof.get("status") != "passed" or proof.get("actual_integrated_world") is not True):
        raise SmokeFailure("Missing matching successful native Smelter GUI proof")
    if (not re.fullmatch(r"[0-9a-f]{40}", executed_sha)
            or proof.get("executed_source_sha") != executed_sha):
        raise SmokeFailure("Smelter GUI proof does not identify the exact executed source revision")
    if (not re.fullmatch(r"[0-9a-f]{40}", pr_head_sha)
            or proof.get("pull_request_head_sha") != pr_head_sha):
        raise SmokeFailure("Smelter GUI proof does not identify the checked PR head")

    if proof.get("language") != language or language not in {"en_us", "fr_fr"}:
        raise SmokeFailure("Smelter GUI proof locale does not match the requested locale")
    expected_label = {"en_us": "LAVA", "fr_fr": "LAVE"}[language]
    if proof.get("translated_lava_mode") != expected_label:
        raise SmokeFailure("Smelter GUI did not load the expected localized mode label")

    expected_identity = "com.oblixorprime.engineersdecorreforged.client.SmallMineralSmelterHybridScreen"
    expected_menu = "com.oblixorprime.engineersdecorreforged.menu.MachineMenu"
    if proof.get("screen_identity") != expected_identity:
        raise SmokeFailure("The real Small Mineral Smelter screen is absent")
    if proof.get("client_menu_identity") != expected_menu or proof.get("server_menu_identity") != expected_menu:
        raise SmokeFailure("The real registered Smelter menu is absent on client or server")
    if proof.get("machine_kind") != "SMALL_MINERAL_SMELTER":
        raise SmokeFailure("The menu is not bound to the Small Mineral Smelter kind")
    client_id, server_id = proof.get("client_menu_id"), proof.get("server_menu_id")
    if (type(client_id) is not int or type(server_id) is not int or client_id < 0 or client_id != server_id
            or proof.get("server_menu_context_valid") is not True):
        raise SmokeFailure("Client and server do not share a valid live Smelter menu context")
    if (proof.get("visible_machine_slots") != 2 or proof.get("total_slots") != 38
            or proof.get("machine_slot_backing_indices") != [0, 2]
            or proof.get("data_fields") != 17):
        raise SmokeFailure("Smelter GUI slot backing or v0.5 data projection is incorrect")

    state = proof.get("live_state")
    if not isinstance(state, dict):
        raise SmokeFailure("Smelter GUI lacks live server-projected state")
    if (state.get("selected_mode") != 0 or state.get("batch_mode") != 0
            or type(state.get("batch_progress")) is not int or state["batch_progress"] <= 0
            or type(state.get("thermal")) is not int or state["thermal"] <= 0
            or state.get("input_item") != "minecraft:cobblestone"
            or state.get("output_item") != "minecraft:magma_block" or state.get("output_count") != 2
            or type(state.get("tank_amount")) is not int or state["tank_amount"] <= 0
            or type(state.get("tank_capacity")) is not int or state["tank_capacity"] < state["tank_amount"]
            or type(state.get("energy")) is not int or state["energy"] <= 0
            or type(state.get("energy_capacity")) is not int or state["energy_capacity"] < state["energy"]
            or state.get("fe_rate") != 16 or state.get("status") != 1):
        raise SmokeFailure("Smelter menu did not synchronize the fixture's non-empty, actively heating state")

    screen = proof.get("screen")
    if not isinstance(screen, dict):
        raise SmokeFailure("Smelter GUI lacks native screen geometry")
    if (proof.get("window_width") != window_width or proof.get("window_height") != window_height
            or screen.get("panel_width") != 234 or screen.get("panel_height") != 270):
        raise SmokeFailure("Smelter GUI window or logical panel dimensions differ from the requested fixture")
    gui_width, gui_height, scale = screen.get("gui_width"), screen.get("gui_height"), screen.get("view_scale")
    if (type(gui_width) is not int or type(gui_height) is not int
            or gui_width <= 0 or gui_height <= 0
            or type(scale) not in (int, float) or not 0.1 <= scale <= 1.0):
        raise SmokeFailure("Smelter GUI scale measurements are invalid")
    expected_scale = max(0.1, min(1.0, (gui_width - 12.0) / 234.0, (gui_height - 12.0) / 270.0))
    if abs(scale - expected_scale) > 0.002:
        raise SmokeFailure("Recorded viewScale differs from the live screen's scale rule")
    origin_x, origin_y = (gui_width - 234.0 * scale) / 2.0, (gui_height - 270.0 * scale) / 2.0
    if (origin_x < -0.01 or origin_y < -0.01
            or origin_x + 234.0 * scale > gui_width + 0.01
            or origin_y + 270.0 * scale > gui_height + 0.01):
        raise SmokeFailure("Smelter faceplate does not fit inside the active GUI viewport")
    if window_height < 300 and scale >= 1.0:
        raise SmokeFailure("The reduced-height capture did not exercise viewScale")

    if (screen.get("drawer_closed_capture") is not True
            or screen.get("drawer_open_capture") is not True
            or screen.get("output_slot_hidden_when_closed") is not True
            or screen.get("output_slot_visible_when_open") is not True):
        raise SmokeFailure("The native screen did not verify the real output-drawer interaction")

    captures = proof.get("screenshots")
    expected_names = {
        "progress-empty.png", "progress-mid.png", "progress-full.png",
        "screen-closed.png", "drawer-open.png",
    }
    if not isinstance(captures, list) or len(captures) != len(expected_names):
        raise SmokeFailure("Native Smelter GUI must provide three progress states plus both drawer views")
    if {item.get("name") for item in captures if isinstance(item, dict)} != expected_names:
        raise SmokeFailure("Native Smelter GUI capture names are missing, duplicated, or unexpected")
    image_dir = directory / "smelter-gui-screenshots"
    if not image_dir.is_dir() or {path.name for path in image_dir.glob("*.png")} != expected_names:
        raise SmokeFailure("Smelter screenshots are stale or contain unexpected files")
    png_signature = bytes((137, 80, 78, 71, 13, 10, 26, 10))
    captures_by_name = {item["name"]: item for item in captures}
    for capture in captures:
        name = capture["name"]
        image = image_dir / name
        data = image.read_bytes()
        if len(data) < 1000 or data[:8] != png_signature or data[12:16] != b"IHDR":
            raise SmokeFailure(f"Native Smelter screenshot is absent or invalid: {name}")
        width, height = struct.unpack(">II", data[16:24])
        if (width, height) != (window_width, window_height):
            raise SmokeFailure(f"Native Smelter screenshot has stale or unexpected dimensions: {name}")
        digest = hashlib.sha256(data).hexdigest()
        if capture.get("sha256") != digest:
            raise SmokeFailure(f"Native Smelter screenshot hash does not match its proof: {name}")
        if (capture.get("width"), capture.get("height")) != (window_width, window_height):
            raise SmokeFailure(f"Native Smelter screenshot dimensions do not match its proof: {name}")
    for name, progress in (("progress-empty.png", 0), ("progress-mid.png", 50), ("progress-full.png", 90)):
        capture = captures_by_name[name]
        if (capture.get("batch_progress") != progress or capture.get("energy") != 0
                or capture.get("status") != 3):
            raise SmokeFailure(f"Native Smelter progress frame does not show its frozen live state: {name}")
    for name in ("screen-closed.png", "drawer-open.png"):
        capture = captures_by_name[name]
        if (type(capture.get("batch_progress")) is not int or capture["batch_progress"] <= 0
                or type(capture.get("energy")) is not int or capture["energy"] <= 0
                or capture.get("status") != 1):
            raise SmokeFailure(f"Native Smelter drawer view did not resume active heating: {name}")
    if f"IEDCTR_SMELTER_GUI PASSED nonce={nonce}" not in logs:
        raise SmokeFailure("Missing current-run native Smelter GUI completion marker")
    return proof


def validate_corrected_machine_gui_proof(proof: dict, nonce: str, executed_sha: str, pr_head_sha: str,
                                         directory: Path, window_width: int, window_height: int,
                                         language: str, logs: str, barrel_fluid: str = "water") -> dict:
    """Fail closed for seven live MachineScreens; Tree Cutter V3 requires its independent suite."""
    fluids = {"water": ("minecraft:water", "minecraft:block/water_still"),
              "lava": ("minecraft:lava", "minecraft:block/lava_still"),
              "milk": ("minecraft:milk", "neoforge:block/milk_still")}
    if barrel_fluid not in fluids or proof.get("barrel_fixture_fluid") != fluids[barrel_fluid][0]:
        raise SmokeFailure("Barrel proof does not identify the requested fixture fluid")
    expected_fluid, expected_still = fluids[barrel_fluid]
    expected = {
        "fluid_barrel": ("FLUID_BARREL", 2, 201, 226),
        "small_milking_machine": ("SMALL_MILKING_MACHINE", 2, 228, 228),
        "small_block_breaker": ("SMALL_BLOCK_BREAKER", 1, 256, 256),
        "small_solar_panel": ("SMALL_SOLAR_PANEL", 1, 228, 212),
        "small_freezer": ("SMALL_FREEZER", 3, 209, 235),
        "small_fluid_funnel": ("SMALL_FLUID_FUNNEL", 2, 256, 256),
        "passive_fluid_accumulator": ("PASSIVE_FLUID_ACCUMULATOR", 2, 256, 256),
    }
    if (proof.get("suite") != "native-corrected-machine-gui-v1" or proof.get("nonce") != nonce
            or proof.get("status") != "passed" or proof.get("actual_integrated_world") is not True):
        raise SmokeFailure("Missing matching successful native corrected-machine GUI proof")
    if (type(proof.get("schema_version")) is not int or proof["schema_version"] != 2
            or proof.get("coverage_scope") != "seven-machine-screens-excluding-tree-cutter"
            or proof.get("tree_cutter_qualification") != "independent:tree-cutter-v3-native.yml"
            or "tree_cutter_badge_only_capture" in proof):
        raise SmokeFailure("Corrected-machine GUI proof must explicitly exclude independent Tree Cutter V3 qualification")
    if not re.fullmatch(r"[0-9a-f]{40}", executed_sha) or proof.get("executed_source_sha") != executed_sha:
        raise SmokeFailure("Corrected-machine GUI proof does not identify the exact executed source revision")
    if not re.fullmatch(r"[0-9a-f]{40}", pr_head_sha) or proof.get("pull_request_head_sha") != pr_head_sha:
        raise SmokeFailure("Corrected-machine GUI proof does not identify the checked PR head")
    if proof.get("language") != language or language not in {"en_us", "fr_fr"}:
        raise SmokeFailure("Corrected-machine GUI proof locale does not match the requested locale")
    if proof.get("selected_language") != language:
        raise SmokeFailure("Minecraft did not load the requested corrected-machine GUI locale")
    if (proof.get("window_width") != window_width or proof.get("window_height") != window_height
            or proof.get("screenshot_method") != "Native Minecraft panel crops with cursor outside and separate full-window hover captures"):
        raise SmokeFailure("Corrected-machine GUI proof lacks panel crops and tooltip-preserving captures")

    cases = proof.get("cases")
    if not isinstance(cases, list) or len(cases) != len(expected):
        raise SmokeFailure("Corrected-machine GUI proof does not cover all seven registered screens")
    by_id = {}
    for case in cases:
        if not isinstance(case, dict) or case.get("machine_id") not in expected or case["machine_id"] in by_id:
            raise SmokeFailure("Corrected-machine GUI proof has a duplicate or unexpected machine")
        by_id[case["machine_id"]] = case
    if set(by_id) != set(expected):
        raise SmokeFailure("Corrected-machine GUI proof is missing a corrected screen")

    capture_names = {f"{machine}-{state}.png" for machine in expected
                     for state in ("empty", "mid", "full-hover")}
    capture_names.update({
        "small_milking_machine-input-occupied.png",
        "small_milking_machine-output-occupied.png",
    })
    capture_names.add("small_freezer-near-end.png")
    tooltip_names = {f"{machine}-full-hover-window.png" for machine in expected}
    captures = proof.get("screenshots")
    if not isinstance(captures, list) or len(captures) != len(capture_names):
        raise SmokeFailure("Corrected-machine GUI proof requires 24 panel captures and seven separate hover captures")
    if {capture.get("name") for capture in captures if isinstance(capture, dict)} != capture_names:
        raise SmokeFailure("Corrected-machine GUI screenshot names are missing, duplicated, or unexpected")
    image_dir = directory / "corrected-machine-gui-screenshots"
    if not image_dir.is_dir() or {path.name for path in image_dir.glob("*.png")} != (capture_names | tooltip_names):
        raise SmokeFailure("Corrected-machine GUI screenshots are stale or contain unexpected files")
    png_signature = bytes((137, 80, 78, 71, 13, 10, 26, 10))
    captures_by_name = {capture["name"]: capture for capture in captures}
    for machine, (kind, slots, panel_width, panel_height) in expected.items():
        case = by_id[machine]
        if (case.get("machine_kind") != kind or case.get("client_menu_identity") != "com.oblixorprime.engineersdecorreforged.menu.MachineMenu"
                or case.get("server_menu_identity") != case.get("client_menu_identity")
                or case.get("menu_context_matches") is not True
                or type(case.get("client_menu_id")) is not int
                or case.get("client_menu_id") != case.get("server_menu_id")
                or not isinstance(case.get("screen_identity"), str)
                or "MachineScreen$" not in case["screen_identity"]
                or case.get("visible_machine_slots") != slots or case.get("total_slots") != slots + 36
                or case.get("panel_width") != panel_width or case.get("panel_height") != panel_height
                or case.get("gui_height") != window_height or case.get("geometry_matches_layout") is not True
                or case.get("captured_states") != (["empty", "mid", "full-hover", "input-occupied", "output-occupied"]
                    if machine == "small_milking_machine" else ["empty", "mid", "full-hover"])):
            raise SmokeFailure(f"Live menu, panel or layout geometry is incorrect for {machine}")
        slot_proofs = case.get("machine_slots")
        if not isinstance(slot_proofs, list) or len(slot_proofs) != slots:
            raise SmokeFailure(f"Live machine-slot hitboxes are missing for {machine}")
        for slot in slot_proofs:
            if not isinstance(slot, dict) or type(slot.get("container_index")) is not int:
                raise SmokeFailure(f"Invalid machine-slot hitbox proof for {machine}")
            reserved_cold_slot = machine == "small_freezer" and slot["container_index"] == 1
            if (slot.get("container_index") < 0 or slot.get("x") != slot.get("layout_x")
                    or slot.get("y") != slot.get("layout_y") or slot.get("active") is not True
                    or slot.get("center_hit_test") is not True
                    or (reserved_cold_slot and slot.get("insertion_allowed") is not False)):
                raise SmokeFailure(f"Live slot click/hover geometry differs from its layout for {machine}")
        expected_meter_capacity = (32000 if machine == "fluid_barrel" else 4000 if machine in {
            "small_milking_machine", "passive_fluid_accumulator"} else 3000 if machine == "small_fluid_funnel" else 0)
        expected_energy_capacity = 64000 if machine == "small_solar_panel" else 16000 if machine == "small_freezer" else 32000 if machine == "small_block_breaker" else 0
        for state in ("empty", "mid", "full-hover"):
            name = f"{machine}-{state}.png"
            capture = captures_by_name[name]
            image = image_dir / name
            data = image.read_bytes()
            if len(data) < 1000 or data[:8] != png_signature or data[12:16] != b"IHDR":
                raise SmokeFailure(f"Native corrected-machine screenshot is absent or invalid: {name}")
            width, height = struct.unpack(">II", data[16:24])
            expected_capture_size = (panel_width, panel_height)
            if ((width, height) != expected_capture_size or capture.get("capture_mode") != "gui_panel"):
                raise SmokeFailure(f"Native screenshot is not a clean panel crop: {name}")
            if capture.get("panel_cursor_outside") is not True:
                raise SmokeFailure(f"Native panel crop was not captured with its cursor outside: {name}")
            if capture.get("panel_tooltips_suppressed") is not True:
                raise SmokeFailure(f"Native panel crop may contain a clipped pointer tooltip: {name}")
            if capture.get("sha256") != hashlib.sha256(data).hexdigest():
                raise SmokeFailure(f"Native corrected-machine screenshot hash does not match proof: {name}")
            if (capture.get("width"), capture.get("height"), capture.get("kind"),
                    capture.get("screen_identity"), capture.get("menu_id")) != (
                    expected_capture_size[0], expected_capture_size[1], kind,
                    case.get("screen_identity"), case.get("client_menu_id")):
                raise SmokeFailure(f"Native corrected-machine screenshot identity differs from proof: {name}")
            if state == "full-hover":
                tooltip_name = f"{machine}-full-hover-window.png"
                tooltip = image_dir / tooltip_name
                tooltip_data = tooltip.read_bytes()
                if (len(tooltip_data) < 1000 or tooltip_data[:8] != png_signature
                        or tooltip_data[12:16] != b"IHDR"):
                    raise SmokeFailure(f"Native full-window tooltip screenshot is absent or invalid: {tooltip_name}")
                tooltip_size = struct.unpack(">II", tooltip_data[16:24])
                if (tooltip_size != (window_width, window_height)
                        or capture.get("tooltip_name") != tooltip_name
                        or capture.get("tooltip_capture_mode") != "full_window_tooltip"
                        or capture.get("tooltip_sha256") != hashlib.sha256(tooltip_data).hexdigest()
                        or (capture.get("tooltip_width"), capture.get("tooltip_height")) != tooltip_size):
                    raise SmokeFailure(f"Native tooltip screenshot bounds or hash do not match proof: {tooltip_name}")
            elif any(key in capture for key in ("tooltip_name", "tooltip_capture_mode", "tooltip_sha256")):
                raise SmokeFailure(f"Unexpected tooltip screenshot metadata for panel-only state: {name}")
            stage = "full" if state == "full-hover" else state
            if machine == "small_milking_machine" and capture.get("machine_slot_items") != [
                    {"container_index": 0, "item_id": "minecraft:air", "count": 0, "empty": True},
                    {"container_index": 1, "item_id": "minecraft:air", "count": 0, "empty": True}]:
                raise SmokeFailure(f"Native Milking {stage} frame does not prove both machine slots are actually empty")
            if expected_meter_capacity:
                expected_amount = 0 if stage == "empty" else expected_meter_capacity // 2 if stage == "mid" else expected_meter_capacity
                if capture.get("fluid_amount") != expected_amount:
                    raise SmokeFailure(f"Native fluid gauge did not show {stage} state: {machine}")
                if machine == "fluid_barrel":
                    sample = capture.get("barrel_fluid_pixels")
                    top = 73 if stage == "mid" else 38
                    if (not isinstance(sample, dict)
                            or any(type(sample.get(key)) is not int or sample[key] != value for key, value in (
                                ("x", 99), ("y", top), ("width", 16), ("height", 107 - top)))
                            or type(sample.get("distinct_rgba_colors")) is not int
                            or sample.get("fluid_id") != ("minecraft:empty" if stage == "empty" else expected_fluid)):
                        raise SmokeFailure(f"Native Barrel {stage} frame lacks the exact fluid-only pixel sample")
                    if stage == "empty":
                        if sample["distinct_rgba_colors"] != 1:
                            raise SmokeFailure("Native empty Barrel must show only its dark backing")
                    elif (sample["distinct_rgba_colors"] <= 1
                            or sample.get("still_texture") != expected_still
                            or not isinstance(sample.get("tint_argb"), str)
                            or re.fullmatch(r"[0-9a-f]{8}", sample["tint_argb"]) is None):
                        raise SmokeFailure("Native Barrel fluid lacks textured fluid pixels or block-atlas/tint evidence")
            elif expected_energy_capacity:
                energy = capture.get("energy")
                if (stage == "empty" and energy != 0) or (stage == "full" and energy != expected_energy_capacity):
                    raise SmokeFailure(f"Native energy gauge did not show {stage} state: {machine}")
                if stage == "mid" and (type(energy) is not int or not 0 < energy < expected_energy_capacity):
                    raise SmokeFailure(f"Native energy gauge did not show a live intermediate state: {machine}")
                if machine == "small_freezer" and stage == "mid" and capture.get("progress", 0) <= 0:
                    raise SmokeFailure("Freezer native middle frame lacks active progress")
        hover = captures_by_name[f"{machine}-full-hover.png"].get("hover")
        if (not isinstance(hover, dict) or hover.get("slot_hitbox_center_verified") is not True
                or type(hover.get("slot_index")) is not int):
            raise SmokeFailure(f"Native corrected-machine hover capture lacks hitbox evidence: {machine}")

        if machine == "small_milking_machine":
            tank_hashes = set()
            for state in ("empty", "mid", "full-hover"):
                tank = captures_by_name[f"{machine}-{state}.png"].get("tank_pixels")
                if (not isinstance(tank, dict)
                        or any(type(tank.get(key)) is not int or tank[key] != value for key, value in (
                            ("x", 104), ("y", 49), ("width", 22), ("height", 51)))
                        or tank.get("encoding") != "rgba8-row-major"
                        or not isinstance(tank.get("sha256"), str)
                        or re.fullmatch(r"[0-9a-f]{64}", tank["sha256"]) is None):
                    raise SmokeFailure(f"Native Milking {state} capture lacks the exact tank pixel-region hash")
                tank_hashes.add(tank["sha256"])
            if len(tank_hashes) != 3:
                raise SmokeFailure("Native Milking empty/mid/full tank pixels must be three distinct renderings")
            occupied_states = {
                "input-occupied": [
                    {"container_index": 0, "item_id": "minecraft:bucket", "count": 3, "empty": False},
                    {"container_index": 1, "item_id": "minecraft:air", "count": 0, "empty": True},
                ],
                "output-occupied": [
                    {"container_index": 0, "item_id": "minecraft:bucket", "count": 3, "empty": False},
                    {"container_index": 1, "item_id": "minecraft:milk_bucket", "count": 1, "empty": False},
                ],
            }
            for state, expected_items in occupied_states.items():
                name = f"small_milking_machine-{state}.png"
                capture = captures_by_name[name]
                image = image_dir / name
                data = image.read_bytes()
                if (len(data) < 1000 or data[:8] != png_signature or data[12:16] != b"IHDR"):
                    raise SmokeFailure(f"Native Milking occupied-slot screenshot is absent or invalid: {name}")
                dimensions = struct.unpack(">II", data[16:24])
                if (dimensions != (panel_width, panel_height) or capture.get("state") != state
                        or capture.get("capture_mode") != "gui_panel"
                        or capture.get("panel_cursor_outside") is not True
                        or capture.get("panel_tooltips_suppressed") is not True
                        or capture.get("sha256") != hashlib.sha256(data).hexdigest()
                        or (capture.get("width"), capture.get("height"), capture.get("kind"),
                            capture.get("screen_identity"), capture.get("menu_id")) != (
                            panel_width, panel_height, kind, case.get("screen_identity"), case.get("client_menu_id"))
                        or capture.get("fluid_amount") != 0
                        or capture.get("machine_slot_items") != expected_items
                        or any(key in capture for key in ("tooltip_name", "tooltip_capture_mode", "tooltip_sha256", "hover"))):
                    raise SmokeFailure(f"Native Milking occupied-slot frame is not a clean, live, item-backed panel: {name}")

            transfers = case.get("slot_item_transfers")
            input_transfer = transfers.get("player_to_machine_input") if isinstance(transfers, dict) else None
            if (not isinstance(input_transfer, dict)
                    or input_transfer.get("quick_move_succeeded") is not True
                    or input_transfer.get("source_menu_index") != slots + 27
                    or input_transfer.get("source_player_inventory_index") != 0
                    or input_transfer.get("destination_container_index") != 0
                    or input_transfer.get("item_id") != "minecraft:bucket"
                    or input_transfer.get("destination_item_id") != "minecraft:bucket"
                    or any(type(input_transfer.get(key)) is not int or input_transfer[key] != value for key, value in (
                        ("source_count_before", 3), ("moved_count", 3), ("source_count_after", 0),
                        ("destination_count_after", 3)))):
                raise SmokeFailure("Native Milking proof lacks a real three-bucket player-to-input quick-move")
            output_transfer = transfers.get("machine_output_to_player") if isinstance(transfers, dict) else None
            if (not isinstance(output_transfer, dict)
                    or output_transfer.get("quick_move_succeeded") is not True
                    or output_transfer.get("source_menu_index") != 1
                    or output_transfer.get("source_container_index") != 1
                    or output_transfer.get("item_id") != "minecraft:milk_bucket"
                    or any(type(output_transfer.get(key)) is not int or output_transfer[key] != value for key, value in (
                        ("source_count_before", 1), ("moved_count", 1), ("source_count_after", 0),
                        ("destination_inventory_count_after", 1)))
                    or type(output_transfer.get("destination_player_inventory_index")) is not int
                    or not 0 <= output_transfer["destination_player_inventory_index"] < 36
                    or type(output_transfer.get("destination_menu_index")) is not int):
                raise SmokeFailure("Native Milking proof lacks a real milk-bucket output quick-move to player inventory")
            inventory_index = output_transfer["destination_player_inventory_index"]
            expected_menu_index = slots + 27 + inventory_index if inventory_index < 9 else slots + inventory_index - 9
            if output_transfer["destination_menu_index"] != expected_menu_index:
                raise SmokeFailure("Native Milking output transfer destination does not match the live player slot")
    near_end = captures_by_name["small_freezer-near-end.png"]
    near_end_image = image_dir / "small_freezer-near-end.png"
    near_end_data = near_end_image.read_bytes()
    if (len(near_end_data) < 1000 or near_end_data[:8] != png_signature
            or near_end_data[12:16] != b"IHDR"):
        raise SmokeFailure("Native Freezer near-end screenshot is absent or invalid")
    near_end_size = struct.unpack(">II", near_end_data[16:24])
    if (near_end_size != (209, 235) or near_end.get("capture_mode") != "gui_panel"
            or near_end.get("panel_cursor_outside") is not True
            or near_end.get("panel_tooltips_suppressed") is not True
            or near_end.get("kind") != "SMALL_FREEZER"
            or near_end.get("sha256") != hashlib.sha256(near_end_data).hexdigest()
            or (near_end.get("width"), near_end.get("height")) != near_end_size
            or type(near_end.get("freezer_progress_ticks")) is not int
            or not 178 <= near_end["freezer_progress_ticks"] < 180):
        raise SmokeFailure("Native Freezer near-end frame does not prove progress immediately before completion")

    if f"IEDCTR_CORRECTED_MACHINE_GUI PASSED nonce={nonce}" not in logs:
        raise SmokeFailure("Missing current-run corrected-machine GUI completion marker")
    return proof


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--prepare-timeout", type=int, default=600)
    parser.add_argument("--timeout", type=int, default=1080)
    parser.add_argument("--renderer-profile", choices=PROFILES)
    parser.add_argument("--flow-distance-only", action="store_true")
    parser.add_argument("--lava-surface-only", action="store_true")
    parser.add_argument("--lithium", action="store_true", help="Test the pinned official Lithium build with its fluid BFS enabled")
    parser.add_argument("--smelter-gui-only", action="store_true", help="Capture the real Smelter menu in an isolated native client")
    parser.add_argument("--smelter-language", choices=("en_us", "fr_fr"), default="en_us")
    parser.add_argument("--smelter-window-height", type=int, choices=(260, 720), default=720)
    parser.add_argument("--smelter-pr-head-sha", default=os.environ.get("IEDCTR_PR_HEAD_SHA", ""))
    parser.add_argument("--corrected-machine-gui-only", action="store_true",
                        help="Capture all seven corrected MachineScreen menus in an isolated native client")
    parser.add_argument("--machine-gui-language", choices=("en_us", "fr_fr"), default="en_us")
    parser.add_argument("--machine-gui-window-height", type=int, choices=(260, 720), default=720)
    parser.add_argument("--machine-gui-pr-head-sha", default=os.environ.get("IEDCTR_PR_HEAD_SHA", ""))
    parser.add_argument("--barrel-fluid", choices=("water", "lava", "milk"), default="water",
                        help="Fluid for the isolated corrected-machine Barrel fixture, never gameplay")
    args = parser.parse_args()
    if args.barrel_fluid != "water" and not args.corrected_machine_gui_only:
        parser.error("--barrel-fluid requires --corrected-machine-gui-only")
    if args.lava_surface_only and (not args.renderer_profile or args.flow_distance_only):
        parser.error("Lava surface requires one renderer profile and its own session")
    if args.flow_distance_only and not args.renderer_profile:
        parser.error("A renderer profile is required for the east flow-distance sweep")
    if args.smelter_gui_only and (args.renderer_profile or args.flow_distance_only or args.lava_surface_only or args.lithium):
        parser.error("Smelter GUI capture requires its own isolated native-client session")
    if args.smelter_gui_only and args.corrected_machine_gui_only:
        parser.error("Each native GUI fixture requires its own isolated client session")
    if args.smelter_gui_only and not re.fullmatch(r"[0-9a-fA-F]{40}", args.smelter_pr_head_sha):
        parser.error("Smelter GUI capture requires the exact pull-request head SHA")
    if args.corrected_machine_gui_only and (args.renderer_profile or args.flow_distance_only or args.lava_surface_only or args.lithium):
        parser.error("Corrected-machine GUI capture requires its own isolated native-client session")
    if args.corrected_machine_gui_only and not re.fullmatch(r"[0-9a-fA-F]{40}", args.machine_gui_pr_head_sha):
        parser.error("Corrected-machine GUI capture requires the exact pull-request head SHA")
    if not args.smelter_gui_only and (args.smelter_language != "en_us" or args.smelter_window_height != 720):
        parser.error("Smelter language and window-height options require --smelter-gui-only")
    if not args.corrected_machine_gui_only and (args.machine_gui_language != "en_us" or args.machine_gui_window_height != 720):
        parser.error("Corrected-machine language and window-height options require --corrected-machine-gui-only")
    if sys.platform != "linux":
        parser.error("The native gameplay runner supports isolated Linux CI only")
    if min(args.prepare_timeout, args.timeout) <= 0:
        parser.error("Timeouts must be positive")
    missing = [name for name in ("java", "Xvfb", "xvfb-run", "xauth") if not shutil.which(name)]
    if missing:
        print("Native gameplay prerequisites missing: " + ", ".join(missing), file=sys.stderr)
        return 1
    project = args.project.resolve()
    executed_source_sha = ""
    if args.smelter_gui_only or args.corrected_machine_gui_only:
        revision = subprocess.run(["git", "rev-parse", "HEAD"], cwd=project, check=True, capture_output=True, text=True)
        executed_source_sha = revision.stdout.strip().lower()
        if not re.fullmatch(r"[0-9a-f]{40}", executed_source_sha):
            parser.error("Unable to identify the exact source revision executed by this fixture")
        args.smelter_pr_head_sha = args.smelter_pr_head_sha.lower()
        args.machine_gui_pr_head_sha = args.machine_gui_pr_head_sha.lower()
    if not (project / "gradlew").is_file():
        parser.error("Project must contain the checked-in Gradle wrapper")
    (project / "build").mkdir(exist_ok=True)
    evidence = Path(tempfile.mkdtemp(prefix="client-gameplay-", dir=project / "build"))
    game = evidence / "run"
    game.mkdir()
    nonce = secrets.token_hex(16)
    (game / ".iedctr-client-gameplay-nonce").write_text(nonce, encoding="utf-8")
    # These options apply only to this empty fixture directory. No user data or
    # resource packs are imported; onboarding cannot hide the title screen.
    options_text = (
        "onboardAccessibility:false\ntutorialStep:none\nrenderDistance:2\nsimulationDistance:2\n"
        "maxFps:60\npauseOnLostFocus:false\nautoJump:true\nsoundCategory_master:0.0\n")
    if args.smelter_gui_only or args.corrected_machine_gui_only:
        selected_language = args.smelter_language if args.smelter_gui_only else args.machine_gui_language
        options_text += f"guiScale:1\nlang:{selected_language}\n"
    (game / "options.txt").write_text(options_text, encoding="utf-8")
    renderer_jars = prepare_renderer_dependencies(evidence / "renderer-dependencies", args.renderer_profile) if args.renderer_profile else []
    if args.lithium:
        renderer_jars.append(prepare_lithium_dependency(evidence / "lithium-dependency"))
    window_width = 1280
    window_height = (args.smelter_window_height if args.smelter_gui_only
                     else args.machine_gui_window_height if args.corrected_machine_gui_only else 720)
    init = evidence / "gameplay.init.gradle"
    init.write_text("""// Isolated, explicit, opt-in native gameplay fixture.
gradle.projectsEvaluated {
    def jars = new groovy.json.JsonSlurper().parseText(System.getenv('IEDCTR_RENDERER_JARS'))
    if (jars) rootProject.dependencies.add('runtimeOnly', rootProject.files(jars))
    rootProject.extensions.getByName('neoForge').runs.named('client') {
        gameDirectory.set(rootProject.file(System.getenv('IEDCTR_GAMEPLAY_DIR')))
        systemProperty 'iedctr.clientGameplay', 'true'
        systemProperty 'iedctr.requireLithiumFluidBfs', System.getenv('IEDCTR_EXPECT_LITHIUM')
        systemProperty 'iedctr.canalEast', System.getenv('IEDCTR_CANAL_EAST')
        systemProperty 'iedctr.flowDistanceOnly', System.getenv('IEDCTR_FLOW_DISTANCE_ONLY')
        systemProperty 'iedctr.lavaSurfaceOnly', System.getenv('IEDCTR_LAVA_SURFACE_ONLY')
        systemProperty 'iedctr.clientGameplayNonce', System.getenv('IEDCTR_GAMEPLAY_NONCE')
        systemProperty 'iedctr.clientGameplayDirectory', System.getenv('IEDCTR_GAMEPLAY_DIR')
        systemProperty 'iedctr.smelterGuiOnly', System.getenv('IEDCTR_SMELTER_GUI_ONLY')
        systemProperty 'iedctr.smelterLanguage', System.getenv('IEDCTR_SMELTER_LANGUAGE')
        systemProperty 'iedctr.correctedMachineGuiOnly', System.getenv('IEDCTR_CORRECTED_MACHINE_GUI_ONLY')
        systemProperty 'iedctr.machineGuiLanguage', System.getenv('IEDCTR_MACHINE_GUI_LANGUAGE')
        systemProperty 'iedctr.barrelFixtureFluid', System.getenv('IEDCTR_BARREL_FIXTURE_FLUID')
        systemProperty 'iedctr.executedSourceSha', System.getenv('IEDCTR_EXECUTED_SOURCE_SHA')
        systemProperty 'iedctr.pullRequestHeadSha', System.getenv('IEDCTR_PR_HEAD_SHA')
        programArgument '--width'
        programArgument System.getenv('IEDCTR_SCREEN_WIDTH')
        programArgument '--height'
        programArgument System.getenv('IEDCTR_SCREEN_HEIGHT')
    }
}
""", encoding="utf-8")
    env = dict(os.environ, IEDCTR_GAMEPLAY_DIR=str(game), IEDCTR_GAMEPLAY_NONCE=nonce,
               LIBGL_ALWAYS_SOFTWARE="1", ALSOFT_DRIVERS="null",
               IEDCTR_RENDERER_JARS=json.dumps(renderer_jars), IEDCTR_CANAL_EAST=str(bool(args.renderer_profile)).lower(),
               IEDCTR_FLOW_DISTANCE_ONLY=str(args.flow_distance_only).lower(),
               IEDCTR_LAVA_SURFACE_ONLY=str(args.lava_surface_only).lower(),
               IEDCTR_EXPECT_LITHIUM=str(args.lithium).lower(),
               IEDCTR_SMELTER_GUI_ONLY=str(args.smelter_gui_only).lower(),
               IEDCTR_SMELTER_LANGUAGE=args.smelter_language if args.smelter_gui_only else '',
               IEDCTR_CORRECTED_MACHINE_GUI_ONLY=str(args.corrected_machine_gui_only).lower(),
               IEDCTR_MACHINE_GUI_LANGUAGE=args.machine_gui_language if args.corrected_machine_gui_only else '',
               IEDCTR_BARREL_FIXTURE_FLUID=args.barrel_fluid,
               IEDCTR_EXECUTED_SOURCE_SHA=executed_source_sha,
               IEDCTR_PR_HEAD_SHA=args.smelter_pr_head_sha if args.smelter_gui_only else args.machine_gui_pr_head_sha if args.corrected_machine_gui_only else '',
               IEDCTR_SCREEN_WIDTH=str(window_width), IEDCTR_SCREEN_HEIGHT=str(window_height))
    gradle = [str(project / "gradlew"), "--no-daemon", "--init-script", str(init), "--stacktrace"]
    prepare_log, client_log = evidence / "prepare.log", evidence / "client.log"
    print(f"Native client gameplay evidence: {evidence}", flush=True)
    try:
        code, expired = run_bounded(gradle + ["prepareClientRun"], project, env, prepare_log, args.prepare_timeout)
        if expired or code != 0:
            raise SmokeFailure(f"Native client preparation failed (exit={code}, deadline={expired})")
        code, expired = run_bounded(["xvfb-run", "-a", "-s", f"-screen 0 {window_width}x{window_height}x24"] + gradle + ["runClient"],
                                    project, env, client_log, args.timeout)
        result_name = ("client-smelter-gui-result.json" if args.smelter_gui_only else
                       "client-corrected-machine-gui-result.json" if args.corrected_machine_gui_only else
                       "client-gameplay-result.json")
        path = game / result_name
        if not path.is_file():
            raise SmokeFailure("No native gameplay proof was produced; loading alone cannot pass")
        proof = json.loads(path.read_text(encoding="utf-8"))
        logs = client_log.with_suffix(".observed.log").read_text(encoding="utf-8", errors="replace")
        for name in ("latest.log", "debug.log"):
            runtime = game / "logs" / name
            if runtime.is_file():
                logs += "\n" + runtime.read_text(encoding="utf-8", errors="replace")
        crashes = list(game.glob("crash-reports/*")) + list(evidence.rglob("hs_err_pid*.log"))
        if args.smelter_gui_only:
            if expired or code != 0 or crashes or FATAL_PATTERN.search(logs):
                raise SmokeFailure("Native Smelter GUI client did not finish cleanly")
            main_proof_path = game / "client-gameplay-result.json"
            if not main_proof_path.is_file():
                raise SmokeFailure("Native Smelter GUI fixture did not write its isolated-world result")
            main_proof = json.loads(main_proof_path.read_text(encoding="utf-8"))
            if (main_proof.get("suite") != "native-smelter-gui-v1" or main_proof.get("nonce") != nonce
                    or main_proof.get("status") != "passed" or main_proof.get("actual_integrated_world") is not True):
                raise SmokeFailure("The isolated native-client fixture did not complete successfully")
            validate_smelter_gui_proof(proof, nonce, executed_source_sha, args.smelter_pr_head_sha,
                                       game, window_width, window_height, args.smelter_language, logs)
            print(json.dumps(proof, indent=2), flush=True)
            return 0
        if args.corrected_machine_gui_only:
            if expired or code != 0 or crashes or FATAL_PATTERN.search(logs):
                raise SmokeFailure("Native corrected-machine GUI client did not finish cleanly")
            main_proof_path = game / "client-gameplay-result.json"
            if not main_proof_path.is_file():
                raise SmokeFailure("Corrected-machine fixture did not write its isolated-world result")
            main_proof = json.loads(main_proof_path.read_text(encoding="utf-8"))
            if (main_proof.get("suite") != "native-corrected-machine-gui-v1" or main_proof.get("nonce") != nonce
                    or main_proof.get("status") != "passed" or main_proof.get("actual_integrated_world") is not True):
                raise SmokeFailure("The isolated corrected-machine client did not complete successfully")
            validate_corrected_machine_gui_proof(proof, nonce, executed_source_sha, args.machine_gui_pr_head_sha,
                                                  game, window_width, window_height, args.machine_gui_language, logs, args.barrel_fluid)
            print(json.dumps(proof, indent=2), flush=True)
            return 0
        from fluid_extensions import validate_extensions
        if expired or code != 0 or crashes or FATAL_PATTERN.search(logs):
            raise SmokeFailure('Fluid extension client did not finish cleanly')
        from tracker_render import validate_tracker
        validate_tracker(game, nonce)
        if args.lithium:
            validate_lithium_runtime(logs, nonce)
        if args.lava_surface_only:
            if (proof.get("suite")!="lava-surface-session-v1" or proof.get("status")!="passed"
                    or proof.get("nonce")!=nonce or proof.get("actual_integrated_world") is not True
                    or f"IEDCTR_LAVA_SURFACE PASSED nonce={nonce}" not in logs):
                raise SmokeFailure("Missing complete isolated lava session")
            from lava_surface import validate_lava
            lava=json.loads((game/"client-lava-surface-result.json").read_text())
            validate_lava(lava,nonce,args.renderer_profile,game)
            print(json.dumps(lava,indent=2),flush=True)
            return 0
        validate_extensions(game, nonce)
        if args.flow_distance_only:
            if expired:
                raise SmokeFailure("Natural-flow client exceeded its deadline")
            flow = json.loads((game / "client-water-flow-distance-result.json").read_text())
            validate_flow_distance_proof(flow, proof, nonce, args.renderer_profile, game, logs, code)
            print(json.dumps(flow, indent=2), flush=True)
            return 0
        validate_proof(proof, nonce, logs, code, expired, crashes)
        fluid_proof = json.loads((game / "client-fluidlogging-result.json").read_text(encoding="utf-8"))
        validate_fluidlogging_proof(fluid_proof, nonce)
        from catalogue_visual import validate_catalogue
        catalogue_proof = json.loads((game / "client-catalogue-result.json").read_text(encoding="utf-8"))
        validate_catalogue(catalogue_proof, nonce, game)
        canal_proof = json.loads((game / "client-water-canal-result.json").read_text(encoding="utf-8"))
        validate_canal_proof(canal_proof, nonce, game)
        if args.renderer_profile:
            validate_renderer_profile(canal_proof, args.renderer_profile)
        print(json.dumps(canal_proof, indent=2), flush=True)
        print(json.dumps(fluid_proof, indent=2), flush=True)
        print(json.dumps(proof, indent=2), flush=True)
        return 0
    except (SmokeFailure, OSError, ValueError, TypeError, subprocess.SubprocessError) as error:
        print(f"Native client gameplay FAILED: {error}\nEvidence: {evidence}", file=sys.stderr)
        for diagnostic in (prepare_log, client_log.with_suffix(".observed.log"),
                           game / "logs/latest.log", game / "client-gameplay-result.json",
                           game / "client-smelter-gui-result.json", game / "client-corrected-machine-gui-result.json"):
            if diagnostic.is_file():
                print(f"--- {diagnostic.name}: last 100 lines ---", file=sys.stderr)
                print("\n".join(diagnostic.read_text(encoding="utf-8", errors="replace").splitlines()[-100:]), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())

