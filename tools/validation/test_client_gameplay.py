"""Evidence-gate regressions, separate from the actual native gameplay run."""

import copy
import hashlib
from pathlib import Path
import struct
import unittest
import zlib

from client_gameplay import (EXPECTED_CASES, SUITE, validate_corrected_machine_gui_proof,
                             validate_proof, validate_smelter_gui_proof)
from client_mixin_smoke import SmokeFailure


class NativeGameplayProofTests(unittest.TestCase):
    def setUp(self):
        self.nonce = "a" * 32
        self.proof = {"suite": SUITE, "nonce": self.nonce, "status": "passed", "expected_cases": 10,
                      "actual_integrated_world": True, "fixture_attack_context_verified": True, "cases": []}
        for index, name in enumerate(sorted(EXPECTED_CASES)):
            mode, approach = name.split("-")
            expected = 1 if mode == "survival" else 0
            self.proof["cases"].append({"case": name, "mode": mode, "approach": approach,
                "immediate_client_prediction_lamp_only": True, "authoritative_lamp_only": True,
                "release_allows_native_deck_mining": True, "immediate_client_prediction_deck_removed": True,
                "authoritative_deck_removed": True, "active_attack_binding": True,
                "synchronous_lamp_completion_event": True, "client_lamp_removal_notifications": 1,
                "equal_id_cross_side_players_verified": True, "survival_lamp_hold_checks": 20 * expected,
                "lamp_completion_action": "STOP_DESTROY_BLOCK" if expected else "START_DESTROY_BLOCK",
                "deck_completion_action": "STOP_DESTROY_BLOCK" if expected else "START_DESTROY_BLOCK",
                "lamp_sequence": 2 * index + 1,
                "lamp_ack_applied": 2 * index + 1, "deck_sequence": 2 * index + 2,
                "deck_ack_applied": 2 * index + 2, "held_ticks": 25, "held_attack_cancellations": 27,
                "held_extra_mining_packets": 0, "lamp_drops_or_inventory": expected,
                "lamp_tool_damage": expected, "native_mining_ticks": 20 * expected})
        self.logs = f"IEDCTR_CLIENT_GAMEPLAY PASSED nonce={self.nonce} cases=10"

    def check(self, proof=None, **overrides):
        args = dict(proof=self.proof if proof is None else proof, nonce=self.nonce,
                    logs=self.logs, exit_code=0, timed_out=False, crash_files=[])
        args.update(overrides)
        return validate_proof(**args)

    def test_complete_proof(self):
        self.assertEqual(self.check(), self.proof)

    def test_rejects_timeout_even_with_success_marker(self):
        with self.assertRaises(SmokeFailure): self.check(timed_out=True)

    def test_rejects_failed_process(self):
        with self.assertRaises(SmokeFailure): self.check(exit_code=1)

    def test_rejects_load_only_stale_failed_and_wrong_scope(self):
        for patch in ({}, {"status": "running"}, {"status": "failed"}, {"nonce": "b" * 32},
                      {"suite": "client-mixin-load"}, {"actual_integrated_world": False},
                      {"fixture_attack_context_verified": False}):
            proof = copy.deepcopy(self.proof) if patch else {}
            proof.update(patch)
            with self.subTest(patch=patch), self.assertRaises(SmokeFailure): self.check(proof)

    def test_rejects_incomplete_duplicate_and_mislabeled_cases(self):
        for mutate in (lambda p: p["cases"].pop(),
                       lambda p: p["cases"].__setitem__(1, p["cases"][0]),
                       lambda p: p["cases"][0].update(mode="survival")):
            proof = copy.deepcopy(self.proof)
            mutate(proof)
            with self.assertRaises(SmokeFailure): self.check(proof)

    def test_requires_each_prediction_authority_and_input_assertion(self):
        for key, value in (("immediate_client_prediction_lamp_only", False), ("authoritative_lamp_only", False),
                           ("release_allows_native_deck_mining", False), ("immediate_client_prediction_deck_removed", False),
                           ("authoritative_deck_removed", False), ("lamp_ack_applied", 0), ("deck_ack_applied", 0),
                           ("deck_sequence", 1), ("held_ticks", 0), ("held_attack_cancellations", 0),
                           ("held_extra_mining_packets", 1), ("lamp_drops_or_inventory", 1), ("lamp_tool_damage", 1),
                           ("native_mining_ticks", -1), ("active_attack_binding", False),
                           ("synchronous_lamp_completion_event", False), ("client_lamp_removal_notifications", 0),
                           ("client_lamp_removal_notifications", 2), ("client_lamp_removal_notifications", True),
                           ("equal_id_cross_side_players_verified", False), ("survival_lamp_hold_checks", 1)):
            proof = copy.deepcopy(self.proof)
            proof["cases"][0][key] = value
            with self.subTest(key=key), self.assertRaises(SmokeFailure): self.check(proof)

    def test_rejects_server_only_or_missing_input_context_evidence(self):
        for key in ("active_attack_binding", "synchronous_lamp_completion_event", "client_lamp_removal_notifications"):
            proof = copy.deepcopy(self.proof)
            del proof["cases"][0][key]
            with self.subTest(key=key), self.assertRaises(SmokeFailure): self.check(proof)
        proof = copy.deepcopy(self.proof)
        del proof["fixture_attack_context_verified"]
        with self.assertRaises(SmokeFailure): self.check(proof)

    def test_requires_every_survival_hold_to_keep_its_side_intent(self):
        for checks in (0, 1, True, 21):
            proof = copy.deepcopy(self.proof)
            case = next(case for case in proof["cases"] if case["mode"] == "survival")
            case["survival_lamp_hold_checks"] = checks
            with self.subTest(checks=checks), self.assertRaises(SmokeFailure): self.check(proof)

    def test_survival_start_ack_cannot_qualify_completion(self):
        for stage in ("lamp", "deck"):
            proof = copy.deepcopy(self.proof)
            case = next(case for case in proof["cases"] if case["mode"] == "survival")
            case[f"{stage}_completion_action"] = "START_DESTROY_BLOCK"
            with self.subTest(stage=stage), self.assertRaises(SmokeFailure): self.check(proof)

    def test_requires_real_sequences_not_boolean_json_values(self):
        proof = copy.deepcopy(self.proof)
        proof["cases"][0]["lamp_sequence"] = True
        with self.assertRaises(SmokeFailure): self.check(proof)

    def test_rejects_crashes_fatals_and_missing_completion_marker(self):
        for args in ({"crash_files": [Path("crash.txt")]}, {"logs": self.logs + "\nNoSuchMethodError"},
                     {"logs": "Loaded Engineer's Decor"}, {"logs": self.logs.replace(self.nonce, "b" * 32)}):
            with self.subTest(args=args), self.assertRaises(SmokeFailure): self.check(**args)


class NativeSmelterGuiProofTests(unittest.TestCase):
    def setUp(self):
        import hashlib
        import struct
        import tempfile

        self.hashlib = hashlib
        self.struct = struct
        self.temp = tempfile.TemporaryDirectory()
        self.directory = Path(self.temp.name)
        self.image_dir = self.directory / "smelter-gui-screenshots"
        self.image_dir.mkdir()
        self.nonce = "a" * 32
        self.source_sha = "1" * 40
        self.pr_head_sha = "2" * 40
        self.window_width = 1280
        self.window_height = 260
        gui_width, gui_height = 1280, 260
        scale = max(0.1, min(1.0, (gui_width - 12.0) / 234.0, (gui_height - 12.0) / 270.0))
        self.proof = {
            "suite": "native-smelter-gui-v1", "nonce": self.nonce, "status": "passed",
            "actual_integrated_world": True, "executed_source_sha": self.source_sha,
            "pull_request_head_sha": self.pr_head_sha, "language": "fr_fr", "translated_lava_mode": "LAVE",
            "screen_identity": "com.oblixorprime.engineersdecorreforged.client.SmallMineralSmelterHybridScreen",
            "client_menu_identity": "com.oblixorprime.engineersdecorreforged.menu.MachineMenu",
            "server_menu_identity": "com.oblixorprime.engineersdecorreforged.menu.MachineMenu",
            "machine_kind": "SMALL_MINERAL_SMELTER", "client_menu_id": 4, "server_menu_id": 4,
            "server_menu_context_valid": True, "visible_machine_slots": 2, "total_slots": 38,
            "machine_slot_backing_indices": [0, 2], "data_fields": 17,
            "live_state": {
                "selected_mode": 0, "batch_mode": 0, "batch_progress": 12, "thermal": 12,
                "input_item": "minecraft:cobblestone", "output_item": "minecraft:magma_block",
                "output_count": 2, "tank_amount": 2500, "tank_capacity": 4000,
                "energy": 10000, "energy_capacity": 16000, "fe_rate": 16, "status": 1,
            },
            "window_width": self.window_width, "window_height": self.window_height,
            "screen": {
                "gui_width": gui_width, "gui_height": gui_height, "panel_width": 234, "panel_height": 270,
                "view_scale": scale, "drawer_closed_capture": True, "drawer_open_capture": True,
                "output_slot_hidden_when_closed": True, "output_slot_visible_when_open": True,
            },
            "screenshots": [],
        }
        for name, progress, energy, status in (
            ("progress-empty.png", 0, 0, 3),
            ("progress-mid.png", 50, 0, 3),
            ("progress-full.png", 90, 0, 3),
            ("screen-closed.png", 52, 11984, 1),
            ("drawer-open.png", 52, 11984, 1),
        ):
            data = (bytes((137, 80, 78, 71, 13, 10, 26, 10)) + bytes((0, 0, 0, 13)) + b"IHDR"
                    + struct.pack(">II", self.window_width, self.window_height)
                    + bytes((8, 6, 0, 0, 0)) + bytes(4) + bytes(1100))
            (self.image_dir / name).write_bytes(data)
            self.proof["screenshots"].append({
                "name": name, "sha256": hashlib.sha256(data).hexdigest(),
                "width": self.window_width, "height": self.window_height,
                "batch_progress": progress, "energy": energy, "status": status,
            })
        self.logs = f"IEDCTR_SMELTER_GUI PASSED nonce={self.nonce}"

    def tearDown(self):
        self.temp.cleanup()

    def check(self, proof=None, **overrides):
        args = dict(proof=self.proof if proof is None else proof, nonce=self.nonce,
                    executed_sha=self.source_sha, pr_head_sha=self.pr_head_sha,
                    directory=self.directory, window_width=self.window_width,
                    window_height=self.window_height, language="fr_fr", logs=self.logs)
        args.update(overrides)
        return validate_smelter_gui_proof(**args)

    def test_accepts_native_progress_states_and_two_fresh_drawer_views(self):
        self.assertEqual(self.check(), self.proof)

    def test_rejects_wrong_nonce_or_source_or_pr_head(self):
        for patch in ({"nonce": "b" * 32}, {"executed_source_sha": "3" * 40},
                      {"pull_request_head_sha": "4" * 40}):
            proof = copy.deepcopy(self.proof)
            proof.update(patch)
            with self.subTest(patch=patch), self.assertRaises(SmokeFailure):
                self.check(proof)

    def test_rejects_wrong_screen_menu_or_slots(self):
        for patch in ({"screen_identity": "FixtureScreen"}, {"client_menu_identity": "FixtureMenu"},
                      {"server_menu_context_valid": False}, {"machine_slot_backing_indices": [0, 1]},
                      {"data_fields": 16}):
            proof = copy.deepcopy(self.proof)
            proof.update(patch)
            with self.subTest(patch=patch), self.assertRaises(SmokeFailure):
                self.check(proof)

    def test_rejects_empty_live_state(self):
        proof = copy.deepcopy(self.proof)
        proof["live_state"]["batch_progress"] = 0
        with self.assertRaises(SmokeFailure):
            self.check(proof)

    def test_rejects_progress_frame_with_wrong_server_state(self):
        proof = copy.deepcopy(self.proof)
        full = next(item for item in proof["screenshots"] if item["name"] == "progress-full.png")
        full["batch_progress"] = 0
        with self.assertRaises(SmokeFailure):
            self.check(proof)

    def test_rejects_bad_locale_or_translation(self):
        proof = copy.deepcopy(self.proof)
        proof["translated_lava_mode"] = "LAVA"
        with self.assertRaises(SmokeFailure):
            self.check(proof)
        with self.assertRaises(SmokeFailure):
            self.check(language="en_us")

    def test_rejects_wrong_window_scale_or_clipping(self):
        proof = copy.deepcopy(self.proof)
        proof["screen"]["view_scale"] = 1.0
        with self.assertRaises(SmokeFailure):
            self.check(proof)
        with self.assertRaises(SmokeFailure):
            self.check(window_height=720)

    def test_rejects_stale_missing_bad_hash_and_extra_screenshots(self):
        (self.image_dir / "stale.png").write_bytes(b"stale")
        with self.assertRaises(SmokeFailure):
            self.check()
        (self.image_dir / "stale.png").unlink()
        proof = copy.deepcopy(self.proof)
        proof["screenshots"][0]["sha256"] = "0" * 64
        with self.assertRaises(SmokeFailure):
            self.check(proof)
        (self.image_dir / "drawer-open.png").unlink()
        with self.assertRaises(SmokeFailure):
            self.check()

    def test_rejects_wrong_capture_size_and_missing_marker(self):
        proof = copy.deepcopy(self.proof)
        proof["window_height"] = 720
        with self.assertRaises(SmokeFailure):
            self.check(proof)
        with self.assertRaises(SmokeFailure):
            self.check(logs="")


class CorrectedMachineGuiProofTests(unittest.TestCase):
    MACHINES = {
        "fluid_barrel": ("FLUID_BARREL", 2, 201, 226, 32000, 0),
        "small_milking_machine": ("SMALL_MILKING_MACHINE", 2, 228, 228, 4000, 0),
        "small_block_breaker": ("SMALL_BLOCK_BREAKER", 1, 256, 256, 0, 32000),
        "small_solar_panel": ("SMALL_SOLAR_PANEL", 1, 228, 212, 0, 64000),
        "small_freezer": ("SMALL_FREEZER", 3, 209, 235, 0, 16000),
        "small_fluid_funnel": ("SMALL_FLUID_FUNNEL", 2, 256, 256, 3000, 0),
        "passive_fluid_accumulator": ("PASSIVE_FLUID_ACCUMULATOR", 2, 256, 256, 4000, 0),
    }

    def setUp(self):
        import tempfile
        self.temp = tempfile.TemporaryDirectory()
        self.directory = Path(self.temp.name)
        self.image_dir = self.directory / "corrected-machine-gui-screenshots"
        self.image_dir.mkdir()
        self.nonce = "a" * 32
        self.source_sha = "b" * 40
        self.pr_head_sha = "c" * 40
        self.window_width = 1280
        self.window_height = 260
        self.proof = {
            "barrel_legacy_selector": "iedctr.dev.barrelV5Reference=false", "barrel_qualification": "legacy-diagnostic-only",
            "suite": "native-corrected-machine-gui-v1", "barrel_fixture_fluid": "minecraft:water", "nonce": self.nonce, "status": "passed",
            "actual_integrated_world": True, "executed_source_sha": self.source_sha,
            "pull_request_head_sha": self.pr_head_sha, "language": "fr_fr", "selected_language": "fr_fr",
            "window_width": self.window_width, "window_height": self.window_height,
            "screenshot_method": "Native Minecraft panel crops with cursor outside, separate full-window hover captures, and a Tree Cutter badge-only crop",
            "cases": [], "screenshots": [],
        }
        for machine, (kind, slot_count, panel_width, panel_height, fluid_capacity, energy_capacity) in self.MACHINES.items():
            screen_identity = f"com.oblixorprime.engineersdecorreforged.client.MachineScreen${machine.title().replace('_', '')}Screen"
            menu_identity = "com.oblixorprime.engineersdecorreforged.menu.MachineMenu"
            slots = []
            for index in range(slot_count):
                insertion_allowed = not (machine == "small_freezer" and index == 1)
                slots.append({"container_index": index, "x": 20 + index * 18, "y": 30,
                              "layout_x": 20 + index * 18, "layout_y": 30,
                              "active": True, "center_hit_test": True,
                              "insertion_allowed": insertion_allowed})
            self.proof["cases"].append({
                "machine_id": machine, "machine_kind": kind, "screen_identity": screen_identity,
                "client_menu_identity": menu_identity, "server_menu_identity": menu_identity,
                "client_menu_id": 2, "server_menu_id": 2, "menu_context_matches": True,
                "visible_machine_slots": slot_count, "total_slots": slot_count + 36,
                "panel_width": panel_width, "panel_height": panel_height, "gui_width": 1280,
                "gui_height": self.window_height, "geometry_matches_layout": True,
                "fluid_amount": fluid_capacity, "fluid_capacity": fluid_capacity,
                "energy": energy_capacity, "energy_capacity": energy_capacity,
                "progress": 25 if machine == "small_freezer" else 0,
                "freezer_progress_ticks": 45 if machine == "small_freezer" else 0,
                "language": "fr_fr", "captured_states": (["empty", "mid", "full-hover", "input-occupied", "output-occupied"]
                    if machine == "small_milking_machine" else ["empty", "mid", "full-hover"]),
                "machine_slots": slots,
            })
            states = ("empty", "mid", "full-hover")
            for state in states:
                name = f"{machine}-{state}.png"
                if fluid_capacity:
                    fluid_amount = 0 if state == "empty" else fluid_capacity // 2 if state == "mid" else fluid_capacity
                    energy = 0
                else:
                    energy = 0 if state == "empty" else energy_capacity // 2 if state == "mid" else energy_capacity
                    fluid_amount = 0
                capture = {"name": name, "state": state, "kind": kind,
                           "screen_identity": screen_identity, "menu_id": 2, "server_menu_id": 2,
                           "fluid_amount": fluid_amount, "energy": energy,
                           "progress": 25 if machine == "small_freezer" and state == "mid" else 0}
                capture["capture_mode"] = "gui_panel"
                capture["panel_cursor_outside"] = True
                capture["panel_tooltips_suppressed"] = True
                if machine == "fluid_barrel":
                    top = 73 if state == "mid" else 38
                    capture["barrel_fluid_pixels"] = {
                        "x": 99, "y": top, "width": 16, "height": 107 - top,
                        # Synthetic gate metadata, not a claim of native rendered pixels.
                        "distinct_rgba_colors": 1 if state == "empty" else 8,
                        "fluid_id": "minecraft:empty" if state == "empty" else "minecraft:water",
                    }
                    if state != "empty":
                        capture["barrel_fluid_pixels"].update(
                            still_texture="minecraft:block/water_still", tint_argb="ff3f76e4")
                if machine == "small_milking_machine":
                    capture["tank_pixels"] = {
                        "x": 104, "y": 49, "width": 22, "height": 51,
                        "encoding": "rgba8-row-major",
                        # Synthetic metadata exercises the gate; not a native render claim.
                        "sha256": hashlib.sha256(state.encode()).hexdigest(),
                    }
                    capture["machine_slot_items"] = [
                        {"container_index": 0, "item_id": "minecraft:air", "count": 0, "empty": True},
                        {"container_index": 1, "item_id": "minecraft:air", "count": 0, "empty": True},
                    ]
                capture_width, capture_height = panel_width, panel_height
                if state == "full-hover":
                    capture["hover"] = {"slot_index": 0, "slot_hitbox_center_verified": True}
                image = self.image_dir / name
                image.write_bytes(self._png(capture_width, capture_height))
                capture["width"] = capture_width
                capture["height"] = capture_height
                capture["sha256"] = hashlib.sha256(image.read_bytes()).hexdigest()
                if state == "full-hover":
                    tooltip_name = f"{machine}-full-hover-window.png"
                    tooltip = self.image_dir / tooltip_name
                    tooltip.write_bytes(self._png(self.window_width, self.window_height))
                    capture["tooltip_name"] = tooltip_name
                    capture["tooltip_capture_mode"] = "full_window_tooltip"
                    capture["tooltip_width"] = self.window_width
                    capture["tooltip_height"] = self.window_height
                    capture["tooltip_sha256"] = hashlib.sha256(tooltip.read_bytes()).hexdigest()
                self.proof["screenshots"].append(capture)
            if machine == "small_milking_machine":
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
                for state, machine_slot_items in occupied_states.items():
                    name = f"small_milking_machine-{state}.png"
                    capture = {"name": name, "state": state, "kind": kind,
                               "screen_identity": screen_identity, "menu_id": 2, "server_menu_id": 2,
                               "fluid_amount": 0, "energy": 0, "progress": 0,
                               "machine_slot_items": machine_slot_items,
                               "capture_mode": "gui_panel", "panel_cursor_outside": True,
                               "panel_tooltips_suppressed": True,
                               "width": panel_width, "height": panel_height}
                    image = self.image_dir / name
                    image.write_bytes(self._png(panel_width, panel_height))
                    capture["sha256"] = hashlib.sha256(image.read_bytes()).hexdigest()
                    self.proof["screenshots"].append(capture)
                self.proof["cases"][-1]["slot_item_transfers"] = {
                    "player_to_machine_input": {
                        "source_menu_index": 29, "source_player_inventory_index": 0,
                        "destination_container_index": 0, "item_id": "minecraft:bucket",
                        "source_count_before": 3, "moved_count": 3, "source_count_after": 0,
                        "destination_item_id": "minecraft:bucket", "destination_count_after": 3,
                        "quick_move_succeeded": True,
                    },
                    "machine_output_to_player": {
                        "source_menu_index": 1, "source_container_index": 1,
                        "destination_player_inventory_index": 8, "destination_menu_index": 37,
                        "item_id": "minecraft:milk_bucket", "source_count_before": 1,
                        "moved_count": 1, "source_count_after": 0,
                        "destination_inventory_count_after": 1, "quick_move_succeeded": True,
                    },
                }
            if machine == "small_freezer":
                name = "small_freezer-near-end.png"
                capture = {"name": name, "state": "near-end", "kind": kind,
                           "screen_identity": screen_identity, "menu_id": 2, "server_menu_id": 2,
                           "fluid_amount": 0, "energy": 8, "progress": 0,
                           "freezer_progress_ticks": 178,
                           "capture_mode": "gui_panel", "panel_cursor_outside": True,
                           "panel_tooltips_suppressed": True,
                           "width": panel_width, "height": panel_height}
                image = self.image_dir / name
                image.write_bytes(self._png(panel_width, panel_height))
                capture["sha256"] = hashlib.sha256(image.read_bytes()).hexdigest()
                self.proof["screenshots"].append(capture)
        tree_name = "small_tree_cutter-badge-only.png"
        tree_capture = {
            "machine_id": "small_tree_cutter", "name": tree_name, "state": "badge-only",
            "kind": "SMALL_TREE_CUTTER",
            "screen_identity": "com.oblixorprime.engineersdecorreforged.client.MachineScreen$SmallTreeCutterScreen",
            "menu_id": 3, "server_menu_id": 3, "fluid_amount": 0, "energy": 0, "progress": 0,
            "capture_mode": "gui_panel", "panel_cursor_outside": True,
            "panel_tooltips_suppressed": True, "width": 228, "height": 212,
        }
        tree_image = self.image_dir / tree_name
        tree_image.write_bytes(self._png(228, 212))
        tree_capture["sha256"] = hashlib.sha256(tree_image.read_bytes()).hexdigest()
        self.proof["screenshots"].append(tree_capture)
        self.proof["tree_cutter_badge_only_capture"] = copy.deepcopy(tree_capture)
        self.logs = f"IEDCTR_CORRECTED_MACHINE_GUI PASSED nonce={self.nonce}"

    def tearDown(self):
        self.temp.cleanup()

    @staticmethod
    def _png(width, height):
        def chunk(kind, payload):
            return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF)
        header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
        pixels = (b"\x00" + b"\x20\x30\x40\xff" * width) * height
        image_data = zlib.compress(pixels, 1)
        return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header)
                + chunk(b"tEXt", b"fixture\x00" + b"x" * 1200)
                + chunk(b"IDAT", image_data) + chunk(b"IEND", b""))

    def check(self, proof=None, **overrides):
        args = dict(proof=self.proof if proof is None else proof, nonce=self.nonce,
                    executed_sha=self.source_sha, pr_head_sha=self.pr_head_sha,
                    directory=self.directory, window_width=self.window_width,
                    window_height=self.window_height, language="fr_fr", logs=self.logs)
        args.update(overrides)
        return validate_corrected_machine_gui_proof(**args)

    def test_requires_explicit_legacy_diagnostic_not_default_qualification(self):
        for field, values in {
            "barrel_legacy_selector": [None, "iedctr.dev.barrelV5Reference=true", "false"],
            "barrel_qualification": [None, "default", "packaged-default"],
        }.items():
            for value in values:
                bad = copy.deepcopy(self.proof)
                bad[field] = value
                with self.subTest(field=field, value=value), self.assertRaises(SmokeFailure):
                    self.check(bad)

    def test_lava_and_milk_require_matching_native_identity_and_texture(self):
        for fluid, texture in (("lava", "minecraft:block/lava_still"), ("milk", "neoforge:block/milk_still")):
            proof = copy.deepcopy(self.proof)
            proof["barrel_fixture_fluid"] = "minecraft:" + fluid
            for capture in proof["screenshots"]:
                if capture["name"] in ("fluid_barrel-mid.png", "fluid_barrel-full-hover.png"):
                    capture["barrel_fluid_pixels"].update(fluid_id="minecraft:" + fluid, still_texture=texture)
            self.check(proof, barrel_fluid=fluid)
            with self.assertRaises(SmokeFailure): self.check(proof)
            for state in ("mid", "full-hover"):
                for field, value in (("fluid_id", "minecraft:water"), ("still_texture", "minecraft:block/water_still")):
                    bad = copy.deepcopy(proof)
                    next(c for c in bad["screenshots"] if c["name"] == f"fluid_barrel-{state}.png")["barrel_fluid_pixels"][field] = value
                    with self.assertRaises(SmokeFailure): self.check(bad, barrel_fluid=fluid)

    def test_missing_or_unsupported_barrel_fixture_fluid_fails(self):
        proof = copy.deepcopy(self.proof)
        del proof["barrel_fixture_fluid"]
        with self.assertRaises(SmokeFailure): self.check(proof)
        with self.assertRaises(SmokeFailure): self.check(barrel_fluid="oil")

    def test_accepts_six_live_menus_and_tree_cutter_badge_only_capture(self):
        self.assertEqual(self.check(), self.proof)

    def test_rejects_missing_capture_bad_hash_or_wrong_live_slot_geometry(self):
        missing = copy.deepcopy(self.proof)
        missing["screenshots"].pop()
        with self.assertRaises(SmokeFailure): self.check(missing)
        bad_hash = copy.deepcopy(self.proof)
        bad_hash["screenshots"][0]["sha256"] = "0" * 64
        with self.assertRaises(SmokeFailure): self.check(bad_hash)
        bad_slot = copy.deepcopy(self.proof)
        bad_slot["cases"][1]["machine_slots"][0]["x"] += 1
        with self.assertRaises(SmokeFailure): self.check(bad_slot)

    def test_rejects_panel_capture_with_cursor_over_the_gui(self):
        proof = copy.deepcopy(self.proof)
        proof["screenshots"][0]["panel_cursor_outside"] = False
        with self.assertRaises(SmokeFailure): self.check(proof)

    def test_rejects_panel_capture_with_tooltips_visible(self):
        proof = copy.deepcopy(self.proof)
        proof["screenshots"][0]["panel_tooltips_suppressed"] = False
        with self.assertRaises(SmokeFailure): self.check(proof)

    def test_rejects_missing_tree_cutter_badge_capture(self):
        proof = copy.deepcopy(self.proof)
        proof["screenshots"] = [capture for capture in proof["screenshots"]
                                if capture["name"] != "small_tree_cutter-badge-only.png"]
        with self.assertRaises(SmokeFailure): self.check(proof)

    def test_rejects_freezer_frame_that_did_not_capture_near_completion(self):
        proof = copy.deepcopy(self.proof)
        near_end = next(item for item in proof["screenshots"] if item["name"] == "small_freezer-near-end.png")
        near_end["freezer_progress_ticks"] = 0
        with self.assertRaises(SmokeFailure): self.check(proof)

    def test_rejects_missing_malformed_or_duplicate_milking_tank_pixels(self):
        name = "small_milking_machine-mid.png"
        for mutation in ("missing", "hash", "roi", "encoding", "duplicate"):
            with self.subTest(mutation=mutation):
                proof = copy.deepcopy(self.proof)
                capture = next(item for item in proof["screenshots"] if item["name"] == name)
                if mutation == "missing":
                    del capture["tank_pixels"]
                elif mutation == "hash":
                    capture["tank_pixels"]["sha256"] = "not-a-hash"
                elif mutation == "roi":
                    capture["tank_pixels"]["x"] = 146  # Amount label is not tank evidence.
                elif mutation == "encoding":
                    capture["tank_pixels"]["encoding"] = "png-file"
                else:
                    full = next(item for item in proof["screenshots"]
                                if item["name"] == "small_milking_machine-full-hover.png")
                    capture["tank_pixels"]["sha256"] = full["tank_pixels"]["sha256"]
                with self.assertRaises(SmokeFailure):
                    self.check(proof)

    def test_rejects_incorrect_milking_items_or_quick_move_counts(self):
        proof = copy.deepcopy(self.proof)
        empty = next(item for item in proof["screenshots"]
                     if item["name"] == "small_milking_machine-empty.png")
        empty["machine_slot_items"][0].update(item_id="minecraft:bucket", count=1, empty=False)
        with self.assertRaises(SmokeFailure): self.check(proof)

        proof = copy.deepcopy(self.proof)
        occupied = next(item for item in proof["screenshots"]
                        if item["name"] == "small_milking_machine-input-occupied.png")
        occupied["machine_slot_items"][0]["count"] = 2
        with self.assertRaises(SmokeFailure): self.check(proof)

        proof = copy.deepcopy(self.proof)
        occupied = next(item for item in proof["screenshots"]
                        if item["name"] == "small_milking_machine-output-occupied.png")
        occupied["machine_slot_items"][1]["item_id"] = "minecraft:bucket"
        with self.assertRaises(SmokeFailure): self.check(proof)

        proof = copy.deepcopy(self.proof)
        case = next(item for item in proof["cases"] if item["machine_id"] == "small_milking_machine")
        case["slot_item_transfers"]["player_to_machine_input"]["moved_count"] = 2
        with self.assertRaises(SmokeFailure): self.check(proof)

        proof = copy.deepcopy(self.proof)
        case = next(item for item in proof["cases"] if item["machine_id"] == "small_milking_machine")
        case["slot_item_transfers"]["machine_output_to_player"]["destination_menu_index"] = 36
        with self.assertRaises(SmokeFailure): self.check(proof)

    def test_rejects_flat_or_missing_barrel_sprite_evidence(self):
        for state in ("empty", "mid", "full-hover"):
            for mutation in ("missing", "roi", "color_count", "fluid_id", "texture", "tint"):
                if state == "empty" and mutation in ("texture", "tint"):
                    continue
                with self.subTest(state=state, mutation=mutation):
                    proof = copy.deepcopy(self.proof)
                    capture = next(item for item in proof["screenshots"]
                                   if item["name"] == f"fluid_barrel-{state}.png")
                    sample = capture["barrel_fluid_pixels"]
                    if mutation == "missing": del capture["barrel_fluid_pixels"]
                    elif mutation == "roi": sample["x"] = 95  # Glass highlight cannot prove textured fluid.
                    elif mutation == "color_count": sample["distinct_rgba_colors"] = 2 if state == "empty" else 1
                    elif mutation == "fluid_id": sample["fluid_id"] = "minecraft:lava"
                    elif mutation == "texture": sample["still_texture"] = "minecraft:missingno"
                    elif mutation == "tint": sample["tint_argb"] = "3f76e4"  # Missing alpha channel.
                    with self.assertRaises(SmokeFailure): self.check(proof)

    def test_rejects_wrong_locale_or_missing_freezer_progress(self):
        bad_locale = copy.deepcopy(self.proof)
        bad_locale["selected_language"] = "en_us"
        with self.assertRaises(SmokeFailure): self.check(bad_locale)
        freezer = next(case for case in self.proof["cases"] if case["machine_id"] == "small_freezer")
        freezer_mid = next(image for image in self.proof["screenshots"] if image["name"] == "small_freezer-mid.png")
        freezer_mid["progress"] = 0
        with self.assertRaises(SmokeFailure): self.check()

    def test_rejects_missing_or_mismatched_full_window_tooltip_capture(self):
        proof = copy.deepcopy(self.proof)
        hovered = next(item for item in proof["screenshots"] if item["name"] == "fluid_barrel-full-hover.png")
        tooltip = self.image_dir / hovered["tooltip_name"]
        tooltip.unlink()
        with self.assertRaises(SmokeFailure): self.check(proof)
        tooltip.write_bytes(self._png(self.window_width, self.window_height))
        hovered["tooltip_sha256"] = "0" * 64
        with self.assertRaises(SmokeFailure): self.check(proof)


if __name__ == "__main__":
    unittest.main()

