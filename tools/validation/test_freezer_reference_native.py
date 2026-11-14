"""Synthetic validator unit tests only. These do NOT execute Minecraft or prove GUI behavior."""
import copy
import hashlib
import json
import math
from pathlib import Path
import struct
import tempfile
import unittest
import zlib

from freezer_reference_native import ART_SHA, CAPTURES, CASES, SCREEN_CLASS, SUITE, validate_proof
from client_mixin_smoke import SmokeFailure


class NativeFreezerEvidenceChecks(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.game = Path(self.temp.name) / "run"
        folder = self.game / "freezer-reference-screenshots"
        folder.mkdir(parents=True)
        self.nonce, self.sha = "a" * 32, "b" * 40
        self.logs = f"IEDCTR_FREEZER_REFERENCE PASSED nonce={self.nonce}"
        self.main = {"suite": SUITE, "nonce": self.nonce, "status": "passed", "actual_integrated_world": True}
        self.proof = {"suite": SUITE, "nonce": self.nonce, "status": "passed_bounded_smoke",
                      "executed_source_sha": self.sha, "source_art_sha256": ART_SHA,
                      "actual_integrated_world": True, "presentation_mode": "normal-dynamic", "dev_selectors_absent": True, "frost_art_static": False, "decorative_rim_frost_static": True, "phase_source": "server_menu_phase_natural_recipe", "phase4_native_observed": False,
                      "native_visual_acceptance": False, "menu_slot_count": 39,
                      "selected_language": "en_us", "cases": sorted(CASES), "captures": []}
        for i, (name, (energy, cells)) in enumerate(CAPTURES.items()):
            # Explicit synthetic colored PNG fixtures for parser tests, never native evidence.
            def chunk(kind, payload):
                return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload))
            data = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 1280, 720, 8, 2, 0, 0, 0))
                    + chunk(b"IDAT", zlib.compress((b"\0" + bytes((i, 0, 0)) * 1280) * 720)) + chunk(b"IEND", b""))
            (folder / name).write_bytes(data)
            self.proof["captures"].append({"name": name, "sha256": hashlib.sha256(data).hexdigest(),
                "width": 1280, "height": 720, "energy": energy, "lit_cells": cells,
                "menu_id": 1 if i < 3 else 2, "screen_class": SCREEN_CLASS, "view_scale": 1.0})

        self.write_meters(720)

    def write_meters(self, height):
        for i, capture in enumerate(self.proof["captures"]):
            scale = capture["view_scale"]
            ox, oy = (1280 - 314 * scale) / 2, (height - 314 * scale) / 2
            width = math.ceil(ox + 790 * 314 / 1254 * scale) - math.floor(ox + 500 * 314 / 1254 * scale)
            mh = math.ceil(oy + 698 * 314 / 1254 * scale) - math.floor(oy + 670 * 314 / 1254 * scale)
            def chunk(kind, payload):
                return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload))
            data = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, mh, 8, 2, 0, 0, 0))
                    + chunk(b"IDAT", zlib.compress((b"\0" + bytes((i, 0, 0)) * width) * mh)) + chunk(b"IEND", b""))
            name = capture["name"][:-4] + "-meter.png"
            (self.game / "freezer-reference-screenshots" / name).write_bytes(data)
            capture.update(meter_name=name, meter_width=width, meter_height=mh, meter_sha256=hashlib.sha256(data).hexdigest())

        self.write_frost(height)

    def write_frost(self, height):
        def png(w,h,n):
            def chunk(k,p): return struct.pack(">I",len(p))+k+p+struct.pack(">I",zlib.crc32(k+p))
            return b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",w,h,8,2,0,0,0))+chunk(b"IDAT",zlib.compress((b"\0"+bytes((n,0,0))*w)*h))+chunk(b"IEND",b"")
        self.proof["frost_captures"]=[]
        for phase in range(4):
            c=copy.deepcopy(self.proof["captures"][0]);c.update(name=f"frost-phase-{phase}.png",phase=phase,progress_ticks=phase*45,menu_id=1 if phase==0 else 2,energy=0 if phase==0 else 1440-phase*45*8)
            data=png(1280,height,phase);(self.game/'freezer-reference-screenshots'/c['name']).write_bytes(data);c['sha256']=hashlib.sha256(data).hexdigest()
            self.proof["frost_captures"].append(c)
        for c in self.proof["captures"]: c.update(phase=0,progress_ticks=0)
        for c in self.proof["captures"]+self.proof["frost_captures"]:
            scale=c['view_scale'];ox,oy=(1280-314*scale)/2,(height-314*scale)/2
            fw=math.ceil(ox+795*314/1254*scale)-math.floor(ox+460*314/1254*scale)
            fh=math.ceil(oy+555*314/1254*scale)-math.floor(oy+390*314/1254*scale)
            data=png(fw,fh,c['phase']);name=c['name'][:-4]+'-frost.png';(self.game/'freezer-reference-screenshots'/name).write_bytes(data)
            c.update(frost_name=name,frost_width=fw,frost_height=fh,frost_sha256=hashlib.sha256(data).hexdigest())

    def test_missing_or_fabricated_natural_phase_fails(self):
        for change in ('missing','phase4','progress','static','reset'):
            p=copy.deepcopy(self.proof)
            if change=='missing':p['frost_captures'].pop()
            elif change=='phase4':p['frost_captures'][3]['phase']=4
            elif change=='progress':p['frost_captures'][1]['progress_ticks']=0
            elif change=='static':p['frost_art_static']=True
            else:p['captures'][-1]['phase']=3
            with self.subTest(change=change),self.assertRaises(SmokeFailure):self.validate(proof=p)

    def test_frost_crop_tampering_or_duplicate_phase_fails(self):
        for field,value in (("phase",0),("frost_name","../outside.png"),("frost_sha256","0"*64),("energy",999),("view_scale",0.5)):
            p=copy.deepcopy(self.proof);p["frost_captures"][1][field]=value
            with self.subTest(field=field),self.assertRaises(SmokeFailure):self.validate(proof=p)

    def validate(self, **kwargs):
        values = dict(proof=self.proof, main=self.main, nonce=self.nonce, source_sha=self.sha,
                      game=self.game, logs=self.logs, exit_code=0, timed_out=False, width=1280, height=720, language="en_us")
        values.update(kwargs)
        return validate_proof(**values)

    def test_complete_synthetic_schema_is_accepted(self):
        self.assertIs(self.proof, self.validate())

    def test_missing_wrong_or_unrequested_language_fails(self):
        for selected in (None, "fr_fr", "de_de", True):
            proof = copy.deepcopy(self.proof)
            if selected is None:
                proof.pop("selected_language")
            else:
                proof["selected_language"] = selected
            with self.subTest(selected=selected), self.assertRaises(SmokeFailure):
                self.validate(proof=proof)
        with self.assertRaises(SmokeFailure):
            self.validate(language="fr_fr")
        with self.assertRaises(SmokeFailure):
            self.validate(language="de_de")

    def test_requested_french_language_passes(self):
        self.proof["selected_language"] = "fr_fr"
        self.assertIs(self.proof, self.validate(language="fr_fr"))

    def test_wrong_identity_or_scope_fails(self):
        for field, wrong in {"suite": "cpu-preview", "nonce": "stale", "status": "passed",
            "executed_source_sha": "c" * 40, "source_art_sha256": "d" * 64, "actual_integrated_world": False,
            "frost_art_static": True, "native_visual_acceptance": True, "menu_slot_count": 38}.items():
            with self.subTest(field=field):
                proof = copy.deepcopy(self.proof)
                proof[field] = wrong
                with self.assertRaises(SmokeFailure): self.validate(proof=proof)

    def test_missing_duplicate_or_invalid_cases_fail(self):
        for cases in ([], self.proof["cases"][:-1], self.proof["cases"][:-1] + [self.proof["cases"][0]], [True] * 10):
            with self.subTest(cases=cases):
                proof = copy.deepcopy(self.proof)
                proof["cases"] = cases
                with self.assertRaises(SmokeFailure): self.validate(proof=proof)

    def test_failed_process_or_missing_marker_fails(self):
        for values in ({"exit_code": 1}, {"exit_code": None}, {"timed_out": True}, {"logs": "title loaded"},
                       {"logs": self.logs + "\njava.lang.NoSuchMethodError"}):
            with self.subTest(values=values), self.assertRaises(SmokeFailure): self.validate(**values)

    def test_world_harness_cannot_be_missing_or_stale(self):
        for main in ({}, dict(self.main, status="running"), dict(self.main, nonce="old"), dict(self.main, actual_integrated_world=False)):
            with self.subTest(main=main), self.assertRaises(SmokeFailure): self.validate(main=main)

    def test_bad_capture_metadata_fails(self):
        for key, value in {"energy": 1, "lit_cells": True, "screen_class": "MachineScreen",
                           "menu_id": -1, "view_scale": 0.8, "width": 314, "height": 314,
                           "sha256": "e" * 64, "name": "../outside.png"}.items():
            proof = copy.deepcopy(self.proof)
            proof["captures"][0][key] = value
            with self.subTest(field=key), self.assertRaises(SmokeFailure): self.validate(proof=proof)

    def test_duplicate_capture_names_fail(self):
        proof = copy.deepcopy(self.proof)
        proof["captures"][1] = proof["captures"][0]
        with self.assertRaises(SmokeFailure): self.validate(proof=proof)

    def test_no_reopen_identity_fails(self):
        self.proof["captures"][-1]["menu_id"] = 1
        with self.assertRaises(SmokeFailure): self.validate()

    def test_energy_identity_changes_fail(self):
        self.proof["captures"][1]["menu_id"] = 3
        with self.assertRaises(SmokeFailure): self.validate()

    def test_static_meter_with_different_world_backdrops_fails(self):
        folder = self.game / "freezer-reference-screenshots"
        (folder / "energy-8000-meter.png").write_bytes((folder / "energy-0-meter.png").read_bytes())
        self.proof["captures"][1]["meter_sha256"] = self.proof["captures"][0]["meter_sha256"]
        with self.assertRaises(SmokeFailure): self.validate()

    def test_missing_tampered_or_linked_png_fails(self):
        path = self.game / "freezer-reference-screenshots" / "energy-0.png"
        path.unlink()
        with self.assertRaises(SmokeFailure): self.validate()
        path.write_bytes(b"not a native png")
        with self.assertRaises(SmokeFailure): self.validate()
        path.unlink()
        path.symlink_to(path.parent / "energy-8000.png")
        with self.assertRaises(SmokeFailure): self.validate()

    def test_crash_report_fails(self):
        crash = self.game / "crash-reports"
        crash.mkdir()
        (crash / "crash.txt").write_text("synthetic failure")
        with self.assertRaises(SmokeFailure): self.validate()

    def test_complete_small_window_synthetic_schema_is_accepted(self):
        for i, capture in enumerate(self.proof["captures"]):
            def chunk(kind, payload):
                return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload))
            data = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 1280, 260, 8, 2, 0, 0, 0))
                    + chunk(b"IDAT", zlib.compress((b"\0" + bytes((i, 0, 0)) * 1280) * 260)) + chunk(b"IEND", b""))
            (self.game / "freezer-reference-screenshots" / capture["name"]).write_bytes(data)
            capture.update(height=260, view_scale=248 / 314, sha256=hashlib.sha256(data).hexdigest())
        self.write_meters(260)
        self.assertIs(self.proof, self.validate(height=260))

    def test_small_window_requires_fitted_panel(self):
        with self.assertRaises(SmokeFailure): self.validate(height=260)


if __name__ == "__main__":
    unittest.main()
