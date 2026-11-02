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
        for name in ("screen-closed.png", "drawer-open.png"):
            data = (bytes((137, 80, 78, 71, 13, 10, 26, 10)) + bytes((0, 0, 0, 13)) + b"IHDR"
                    + struct.pack(">II", self.window_width, self.window_height)
                    + bytes((8, 6, 0, 0, 0)) + bytes(4) + bytes(1100))
            (self.image_dir / name).write_bytes(data)
            self.proof["screenshots"].append({
                "name": name, "sha256": hashlib.sha256(data).hexdigest(),
                "width": self.window_width, "height": self.window_height,
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

    def test_accepts_current_native_proof_and_two_fresh_captures(self):
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
        "small_milking_machine": ("SMALL_MILKING_MACHINE", 2, 216, 210, 4000, 0),
        "small_solar_panel": ("SMALL_SOLAR_PANEL", 1, 228, 212, 0, 64000),
        "small_freezer": ("SMALL_FREEZER", 3, 209, 235, 0, 16000),
        "small_fluid_funnel": ("SMALL_FLUID_FUNNEL", 2, 209, 238, 3000, 0),
        "passive_fluid_accumulator": ("PASSIVE_FLUID_ACCUMULATOR", 2, 209, 238, 4000, 0),
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
            "suite": "native-corrected-machine-gui-v1", "nonce": self.nonce, "status": "passed",
            "actual_integrated_world": True, "executed_source_sha": self.source_sha,
            "pull_request_head_sha": self.pr_head_sha, "language": "fr_fr", "selected_language": "fr_fr",
            "window_width": self.window_width, "window_height": self.window_height,
            "screenshot_method": "Native Minecraft framebuffer cropped to the live GUI panel",
            "cases": [], "screenshots": [],
        }
        for machine, (kind, slot_count, panel_width, panel_height, fluid_capacity, energy_capacity) in self.MACHINES.items():
            screen_identity = f"com.oblixorprime.engineersdecorreforged.client.MachineScreen${machine.title().replace('_', '')}Screen"
            menu_identity = "com.oblixorprime.engineersdecorreforged.menu.MachineMenu"
            slots = []
            for index in range(slot_count):
                active = not (machine == "small_freezer" and index == 1)
                slots.append({"container_index": index, "x": 20 + index * 18, "y": 30,
                              "layout_x": 20 + index * 18, "layout_y": 30,
                              "active": active, "center_hit_test": active})
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
                "language": "fr_fr", "captured_states": ["empty", "mid", "full-hover"],
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
                if state == "full-hover":
                    capture["hover"] = {"slot_index": 0, "slot_hitbox_center_verified": True}
                image = self.image_dir / name
                image.write_bytes(self._png(panel_width, panel_height))
                capture["width"] = panel_width
                capture["height"] = panel_height
                capture["sha256"] = hashlib.sha256(image.read_bytes()).hexdigest()
                self.proof["screenshots"].append(capture)
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

    def test_accepts_six_live_menus_and_eighteen_cropped_state_captures(self):
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

    def test_rejects_wrong_locale_or_missing_freezer_progress(self):
        bad_locale = copy.deepcopy(self.proof)
        bad_locale["selected_language"] = "en_us"
        with self.assertRaises(SmokeFailure): self.check(bad_locale)
        freezer = next(case for case in self.proof["cases"] if case["machine_id"] == "small_freezer")
        freezer_mid = next(image for image in self.proof["screenshots"] if image["name"] == "small_freezer-mid.png")
        freezer_mid["progress"] = 0
        with self.assertRaises(SmokeFailure): self.check()


if __name__ == "__main__":
    unittest.main()
