"""Exact functional-pixel/alpha guards. No native or owner-approval claim."""
import hashlib,json,unittest
from pathlib import Path
from factory_placer_v2_reference_native import decode_png
ROOT=Path(__file__).resolve().parents[2]
DOC=ROOT/'docs/qualification/factory-steel-v2'
ASSETS=ROOT/'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2'
FIXTURE_SHA='cd1bac05da8d8055cb9d6c2ccaf570a12d1226fa240122559f3b05890bbf17b1'
h=lambda b:hashlib.sha256(b).hexdigest()

class RegisteredArtworkTests(unittest.TestCase):
    def test_exact_original_functional_pixels_and_alpha(self):
        data=(DOC/'protected-regions-r04.json').read_bytes()
        self.assertEqual(h(data),FIXTURE_SHA)
        for name,record in json.loads(data)['machines'].items():
            w,hh,pixels=decode_png(ASSETS/f'{name}.png')
            self.assertEqual([w,hh],record['size'])
            self.assertEqual(h(bytes(pixels[3::4])),record['alpha_sha256'],name)
            for q in record['regions']:
                x,y,rw,rh=[v*6 for v in q['rect']]
                crop=b''.join(bytes(pixels[(row*w+x)*4:(row*w+x+rw)*4]) for row in range(y,y+rh))
                with self.subTest(machine=name,region=q['id']):self.assertEqual(h(crop),q['rgba_sha256'])
    def test_fixture_not_blank_or_partial(self):
        record=json.loads((DOC/'protected-regions-r04.json').read_text())
        self.assertEqual(record['source_head'],'c3a2fd55e290a1102e885b3b30972cf41f6cf0d4')
        self.assertEqual(len(record['machines']['hopper']['regions']),54+6+1+18)
        self.assertEqual(len(record['machines']['dropper']['regions']),51+8+6)

if __name__=='__main__':unittest.main()
