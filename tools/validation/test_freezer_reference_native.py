"""Synthetic schema/rejection tests only; these do not execute Minecraft."""
import copy
import hashlib
import json
from pathlib import Path
import struct
import tempfile
import unittest
import zlib

from freezer_reference_native import (ART_SHA, CANVAS_HEIGHT, CANVAS_WIDTH, CAPTURES, CASES,
    EFFECT_CROP, ENERGY_CROP, SCREEN_CLASS, SUITE, _crop_dimensions, _expected_scale,
    load_run_proofs, validate_proof)
from client_mixin_smoke import SmokeFailure


def png(width, height, token):
    def chunk(kind, payload):
        return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload))
    row = b"\0" + bytes((token & 255, (token * 3) & 255, (token * 7) & 255)) * width
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(row * height)) + chunk(b"IEND", b""))


class NativeFreezerEvidenceChecks(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.game = Path(self.temp.name) / "run"
        self.folder = self.game / "freezer-reference-screenshots"
        self.folder.mkdir(parents=True)
        self.nonce, self.sha = "a" * 32, "b" * 40
        self.logs = f"IEDCTR_FREEZER_REFERENCE PASSED nonce={self.nonce}"
        self.main = {"suite": SUITE, "nonce": self.nonce, "status": "passed", "actual_integrated_world": True}
        self.proof = {"suite": SUITE, "nonce": self.nonce, "status": "passed_bounded_smoke",
            "executed_source_sha": self.sha, "screen_class": SCREEN_CLASS,
            "source_art_sha256": ART_SHA, "canvas_width": CANVAS_WIDTH,
            "canvas_height": CANVAS_HEIGHT, "visible_machine_slots": 2, "visible_slot_count": 38,
            "reserved_menu_index": 1, "menu_slot_count": 39, "actual_integrated_world": True,
            "presentation_mode": "normal-dynamic", "dev_selectors_absent": True, "frost_art_static": False,
            "effect_visibility_rule": "server_phase_greater_than_zero_v5c_chamber_pulse",
            "phase_source": "server_menu_phase_natural_recipe", "phase4_native_observed": False,
            "native_visual_acceptance": False, "selected_language": "en_us", "cases": sorted(CASES),
            "captures": [], "frost_captures": []}
        self.rebuild(1280, 720)

    def test_gameplay_failure_stage_precedes_missing_success_proof(self):
        game = Path(self.temp.name) / "failed-run"
        game.mkdir()
        (game / "client-gameplay-result.json").write_text(json.dumps({
            "status": "failed", "phase": "FREEZER_REFERENCE",
            "error": "Reserved-item recovery control is not visible"
        }))
        with self.assertRaisesRegex(SmokeFailure,
                "FREEZER_REFERENCE: Reserved-item recovery control is not visible"):
            load_run_proofs(game)

    def _write_crop(self, capture, kind, rect, width, height, scale, token):
        name = capture["name"][:-4] + f"-{kind}.png"
        cw, ch = _crop_dimensions(width, height, scale, rect)
        data = png(cw, ch, token)
        (self.folder / name).write_bytes(data)
        capture.update({f"{kind}_crop_name": name, f"{kind}_crop_width": cw,
            f"{kind}_crop_height": ch, f"{kind}_crop_sha256": hashlib.sha256(data).hexdigest()})

    def _write_frame(self, capture, width, height, token, *, energy_token, effect_token):
        data = png(width, height, token)
        (self.folder / capture["name"]).write_bytes(data)
        capture.update(width=width, height=height, sha256=hashlib.sha256(data).hexdigest())
        scale = capture["view_scale"]
        self._write_crop(capture, "energy", ENERGY_CROP, width, height, scale, energy_token)
        self._write_crop(capture, "effect", EFFECT_CROP, width, height, scale, effect_token)

    def rebuild(self, width, height):
        scale = _expected_scale(width, height)
        self.proof["captures"] = []
        for i, (name, energy) in enumerate(CAPTURES.items()):
            clip = 136 - (44 * energy) // 16000
            capture = {"name": name, "energy": energy, "energy_capacity": 16000,
                "energy_clip_top": clip, "menu_id": 1 if i < 3 else 2, "screen_class": SCREEN_CLASS,
                "view_scale": scale, "phase": 0, "progress_ticks": 0}
            self._write_frame(capture, width, height, i, energy_token=energy // 1000,
                effect_token=0)
            self.proof["captures"].append(capture)
        self.proof["frost_captures"] = []
        for phase in range(4):
            ticks = phase * 45
            energy = 0 if phase == 0 else 1440 - 8 * ticks
            capture = {"name": f"frost-phase-{phase}.png", "phase": phase, "progress_ticks": ticks,
                "energy": energy, "energy_capacity": 16000,
                "energy_clip_top": 136 - (44 * energy) // 16000,
                "menu_id": 1 if phase == 0 else 2, "screen_class": SCREEN_CLASS,
                "view_scale": scale, "rendered_frames": 3, "capture_event": "ScreenEvent.Render.Post"}
            self._write_frame(capture, width, height, 10 + phase, energy_token=energy // 100,
                effect_token=0 if phase == 0 else 1)
            self.proof["frost_captures"].append(capture)

    def validate(self, **kwargs):
        values = dict(proof=self.proof, main=self.main, nonce=self.nonce, source_sha=self.sha,
            game=self.game, logs=self.logs, exit_code=0, timed_out=False, width=1280, height=720, language="en_us")
        values.update(kwargs)
        return validate_proof(**values)

    def test_complete_synthetic_schema_is_accepted(self):
        self.assertIs(self.proof, self.validate())

    def test_screen_canvas_slots_and_active_art_are_strict(self):
        for field, wrong in {"screen_class": "FreezerReferenceScreen", "canvas_width": 314,
            "canvas_height": 314, "visible_machine_slots": 3, "visible_slot_count": 39,
            "reserved_menu_index": 0, "menu_slot_count": 38, "energy_art_sha256": "0" * 64,
            "effect_visibility_rule": "static"}.items():
            proof = copy.deepcopy(self.proof); proof[field] = wrong
            with self.subTest(field=field), self.assertRaises(SmokeFailure): self.validate(proof=proof)

    def test_wrong_source_language_or_scope_fails(self):
        for field, wrong in {"suite": "cpu-preview", "nonce": "stale", "status": "passed",
            "executed_source_sha": "c" * 40, "source_art_sha256": "d" * 64,
            "actual_integrated_world": False, "frost_art_static": True,
            "native_visual_acceptance": True}.items():
            proof = copy.deepcopy(self.proof); proof[field] = wrong
            with self.subTest(field=field), self.assertRaises(SmokeFailure): self.validate(proof=proof)
        with self.assertRaises(SmokeFailure): self.validate(language="de_de")
        proof = copy.deepcopy(self.proof); proof["selected_language"] = "fr_fr"
        self.assertIs(proof, self.validate(proof=proof, language="fr_fr"))

    def test_missing_duplicate_or_invalid_cases_fail(self):
        for cases in ([], self.proof["cases"][:-1], self.proof["cases"][:-1] + [self.proof["cases"][0]], [True] * 10):
            proof = copy.deepcopy(self.proof); proof["cases"] = cases
            with self.subTest(cases=cases), self.assertRaises(SmokeFailure): self.validate(proof=proof)

    def test_energy_states_are_real_capacity_and_crop_distinct(self):
        proof = copy.deepcopy(self.proof)
        for key, value in (("energy", 1), ("energy_capacity", 32000), ("energy_clip_top", 191),
                           ("screen_class", "MachineScreen"), ("menu_id", -1), ("view_scale", 0.5)):
            proof["captures"][0][key] = value
            with self.subTest(key=key), self.assertRaises(SmokeFailure): self.validate(proof=proof)
            proof = copy.deepcopy(self.proof)
        proof["captures"][1]["energy_crop_sha256"] = proof["captures"][0]["energy_crop_sha256"]
        with self.assertRaises(SmokeFailure): self.validate(proof=proof)

    def test_frost_phases_require_live_frame_evidence_and_reset(self):
        for key, value in (("rendered_frames", 2), ("capture_event", "ClientTick"),
                           ("phase", 4), ("progress_ticks", 0), ("energy", 999)):
            proof = copy.deepcopy(self.proof); proof["frost_captures"][1][key] = value
            with self.subTest(key=key), self.assertRaises(SmokeFailure): self.validate(proof=proof)
        proof = copy.deepcopy(self.proof)
        proof["frost_captures"][0]["effect_crop_sha256"] = proof["frost_captures"][1]["effect_crop_sha256"]
        with self.assertRaises(SmokeFailure): self.validate(proof=proof)
        proof = copy.deepcopy(self.proof)
        proof["frost_captures"].pop()
        with self.assertRaises(SmokeFailure): self.validate(proof=proof)

    def test_recipe_menu_identity_close_reopen_and_completion_reset(self):
        proof = copy.deepcopy(self.proof)
        proof["captures"][-1]["menu_id"] = 1
        with self.assertRaises(SmokeFailure): self.validate(proof=proof)
        proof = copy.deepcopy(self.proof); proof["captures"][1]["menu_id"] = 3
        with self.assertRaises(SmokeFailure): self.validate(proof=proof)
        proof = copy.deepcopy(self.proof)
        proof["captures"][-1]["energy_crop_sha256"] = "f" * 64
        with self.assertRaises(SmokeFailure): self.validate(proof=proof)

    def test_missing_tampered_linked_images_and_crashes_fail(self):
        proof = copy.deepcopy(self.proof)
        path = self.folder / "energy-0.png"
        path.unlink()
        with self.assertRaises(SmokeFailure): self.validate(proof=proof)
        path.write_bytes(b"not a PNG")
        with self.assertRaises(SmokeFailure): self.validate(proof=proof)
        path.unlink(); path.symlink_to(self.folder / "energy-8000.png")
        with self.assertRaises(SmokeFailure): self.validate(proof=proof)
        path.unlink(); self._write_frame(self.proof["captures"][0], 1280, 720, 0, energy_token=0, effect_token=0)
        crash = self.game / "crash-reports"; crash.mkdir(); (crash / "crash.txt").write_text("failure")
        with self.assertRaises(SmokeFailure): self.validate()

    def test_process_failure_and_missing_marker_fail(self):
        for values in ({"exit_code": 1}, {"exit_code": None}, {"timed_out": True}, {"logs": "title loaded"},
                       {"logs": self.logs + "\njava.lang.NoSuchMethodError"}):
            with self.subTest(values=values), self.assertRaises(SmokeFailure): self.validate(**values)

    def test_small_window_uses_281_by_313_fit_and_exact_crop_dimensions(self):
        self.rebuild(1280, 260)
        self.assertAlmostEqual(_expected_scale(1280, 260), 0.78, places=6)
        self.assertIs(self.proof, self.validate(height=260))

    def test_small_window_rejects_old_thermal_fit(self):
        self.rebuild(1280, 260)
        proof = copy.deepcopy(self.proof)
        proof["captures"][0]["view_scale"] = 248 / 381
        with self.assertRaises(SmokeFailure): self.validate(proof=proof, height=260)


class HiddenRecoveryClickContractTests(unittest.TestCase):
    def test_hidden_recovery_button_reuses_native_hit_test_without_visible_guard(self):
        source_path = Path(__file__).resolve().parents[2] / (
            "src/main/java/com/oblixorprime/engineersdecorreforged/client/validation/"
            "NativeFreezerReferenceValidation.java")
        source = source_path.read_text()
        repeat = source.split("case REPEAT_RECOVERY -> {", 1)[1].split("case CLOSE -> {", 1)[0]
        self.assertIn("require(!screen.validationRecoveryVisible()", repeat)
        self.assertIn("clickRecoveryButtonCenter(screen)", repeat)
        self.assertNotIn("clickRecovery(screen)", repeat)
        guarded = source.split("private static void clickRecovery(ThermalVerticalScreen screen)", 1)[1].split(
            "private static void clickRecoveryButtonCenter", 1)[0]
        self.assertIn("require(screen.validationRecoveryVisible()", guarded)


if __name__ == "__main__":
    unittest.main()
