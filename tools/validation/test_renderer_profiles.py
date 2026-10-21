import copy
import unittest
from renderer_profiles import PROFILES, validate_renderer_profile
from client_mixin_smoke import SmokeFailure

class RendererProfileTests(unittest.TestCase):
    def test_exact_profiles_and_east_water_cell(self):
        for profile, mods in PROFILES.items():
            proof = {"orientation": "east", "renderer_mods": mods,
                     "cases": [{"case": "stairs-wet", "block_state": 'facing:"east",fluid_cell:"empty",waterlogged:"true"'}]}
            validate_renderer_profile(proof, profile)

    def test_missing_wrong_version_extra_mod_and_wrong_orientation_fail(self):
        base = {"orientation": "east", "renderer_mods": PROFILES["both"], "cases": []}
        for change in ({"orientation": "north"}, {"renderer_mods": {}},
                       {"renderer_mods": {"embeddium": "other", "modernfix": "5.27.15+mc1.21.1"}}):
            proof = copy.deepcopy(base)
            proof.update(change)
            with self.assertRaises(SmokeFailure):
                validate_renderer_profile(proof, "both")
        with self.assertRaises(SmokeFailure):
            validate_renderer_profile(base, "native")

    def test_wrong_stair_facing_fails(self):
        with self.assertRaises(SmokeFailure):
            validate_renderer_profile({"orientation": "east", "renderer_mods": {},
                "cases": [{"case": "stairs-wet", "block_state": 'facing:"north"'}]}, "native")
