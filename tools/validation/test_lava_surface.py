import copy
import math
import struct
import tempfile
import unittest
import zlib
from pathlib import Path
from lava_surface import CASES, validate_lava
from client_mixin_smoke import SmokeFailure

class LavaSurfaceProofTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root=Path(self.tmp.name)
        images=self.root/"lava-surface-screenshots"
        images.mkdir()
        def chunk(k,p):
            return struct.pack(">I",len(p))+k+p+struct.pack(">I",zlib.crc32(k+p))
        png=b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">IIBBBBB",1280,720,8,2,0,0,0))+chunk(b"IDAT",zlib.compress(bytes(720*(1+1280*3))))+chunk(b"IEND",b"")
        cases=[]
        for name in CASES:
            meshes=[]
            for x,z in ((0,0),(0,1),(1,0),(1,1)):
                vertices=[[x,.5,z],[x,.5,z+1],[x+1,.5,z+1],[x+1,.5,z]]
                meshes.append(dict(pos=f"{x}, 80, {z}",observed_height=.5,expected_height=.5,position_height_equal=True,
                    actual_vertices=vertices,top_present=True,top_matches_position_heights=True,expected_top_y=[.5]*4))
            screenshot=name+".png"
            (images/screenshot).write_bytes(png)
            cases.append(dict(case=name,server_client_agree=True,native_mesh_mismatches=0,
                position_height_mismatches=0,embeddium_height_mismatches=0,meshes=meshes,
                top_faces_checked=4,attachment_cells=4 if name.endswith("-decor") else 0,
                embeddium_height_probes=0,screenshot=screenshot))
        self.proof=dict(suite="lava-surface-v1",nonce="current",status="passed",cases=cases)

    def validate(self,p=None,profile="native"):
        return validate_lava(p or self.proof,"current",profile,self.root)

    def test_complete_native_and_embeddium(self):
        self.validate()
        p=copy.deepcopy(self.proof)
        for c in p["cases"]: c["embeddium_height_probes"]=16
        self.validate(p,"embeddium")

    def test_stale_partial_or_duplicate(self):
        for field,value in (("nonce","stale"),("status","in-progress"),("suite","other")):
            p=copy.deepcopy(self.proof);p[field]=value
            with self.assertRaises(SmokeFailure):self.validate(p)
        p=copy.deepcopy(self.proof);p["cases"][-1]=p["cases"][0]
        with self.assertRaises(SmokeFailure):self.validate(p)

    def test_missing_real_renderer_probes(self):
        with self.assertRaises(SmokeFailure):self.validate(profile="embeddium")
        p=copy.deepcopy(self.proof);p["cases"][1]["attachment_cells"]=0
        with self.assertRaises(SmokeFailure):self.validate(p)

    def test_raw_heights_and_nonfinite_vertices(self):
        for field,value in (("observed_height",float("nan")),("expected_height",.1)):
            p=copy.deepcopy(self.proof);p["cases"][0]["meshes"][0][field]=value
            with self.assertRaises(SmokeFailure):self.validate(p)
        p=copy.deepcopy(self.proof);p["cases"][0]["meshes"][0]["actual_vertices"][0][1]=float("inf")
        with self.assertRaises(SmokeFailure):self.validate(p)

    def test_nonfinite_expected_corner(self):
        p=copy.deepcopy(self.proof);p["cases"][0]["meshes"][0]["expected_top_y"][0]=float("nan")
        with self.assertRaises(SmokeFailure):self.validate(p)

    def test_shared_corner_gap_even_if_summary_claims_zero(self):
        p=copy.deepcopy(self.proof)
        m=p["cases"][1]["meshes"][0]
        m["actual_vertices"][1][1]=.25
        m["expected_top_y"][1]=.25
        with self.assertRaises(SmokeFailure):self.validate(p)

    def test_vacuous_or_duplicate_mesh(self):
        for meshes in ([],[self.proof["cases"][0]["meshes"][0]]*4):
            p=copy.deepcopy(self.proof);p["cases"][0]["meshes"]=meshes
            with self.assertRaises(SmokeFailure):self.validate(p)

    def test_missing_screenshot(self):
        p=copy.deepcopy(self.proof);p["cases"][0]["screenshot"]="../other.png"
        with self.assertRaises(SmokeFailure):self.validate(p)
