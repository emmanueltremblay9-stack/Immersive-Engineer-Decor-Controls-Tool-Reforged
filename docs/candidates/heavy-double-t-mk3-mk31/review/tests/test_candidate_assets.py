"""Static generator/geometry tests; NOT a NeoForge build or in-game QA."""
import sys
import unittest
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools/models"))
import generate_heavy_double_t as gen

class CheckCandidate(unittest.TestCase):
    def test_number_of_assets(self):
        assets=list(gen.data_files())
        self.assertEqual(87,len(assets))
        self.assertEqual(87,len({name for name,_ in assets}))

    def test_24_unique_proper_rotations(self):
        seen=set()
        for direction in gen.FACES:
            for roll in range(4):
                ex,ey,ez=gen.basis(direction,roll)
                self.assertEqual(gen.cross(ex,ey),ez)
                self.assertEqual(sum(x*x for x in ex),1)
                self.assertEqual(sum(x*x for x in ey),1)
                self.assertEqual(sum(x*x for x in ez),1)
                seen.add((ex,ey,ez))
        self.assertEqual(len(seen),24)

    def test_all_asset_models_are_valid_and_inside_cell(self):
        for path,source in gen.data_files():
            raw=json.dumps(source)
            self.assertEqual(source,json.loads(raw),path)
            if 'elements' not in source:continue
            for element in source['elements']:
                self.assertEqual(3,len(element['from']))
                self.assertEqual(3,len(element['to']))
                for a,b in zip(element['from'],element['to']):
                    self.assertGreaterEqual(a,0,path)
                    self.assertLessEqual(b,16,path)
                    self.assertLess(a,b,path)
                if 'rotation' in element:
                    self.assertIn(element['rotation']['angle'],(-45,45))
                    self.assertIn(element['rotation']['axis'],('x','y','z'))
                self.assertTrue(element['faces'])

    def test_axial_core_caps_are_conditional_not_duplicated(self):
        for facing in gen.FACES:
            for roll in range(4):
                core=gen.core(facing,roll)
                for elem in core['elements'][:3]:
                    self.assertNotIn(facing,elem['faces'])
                    self.assertNotIn(gen.OPPOSITE[facing],elem['faces'])
                paths=[p for p,_ in gen.data_files()]
                for sign in ('positive','negative'):
                    self.assertIn(f'assets/{gen.MOD}/models/block/heavy_double_t/end_{facing}_{roll}_{sign}.json',paths)

    def test_all_faces_get_anchor_and_junction(self):
        paths={p for p,_ in gen.data_files()}
        for face in gen.FACES:
            self.assertIn(f'assets/{gen.MOD}/models/block/heavy_double_t/anchor_{face}.json',paths)
            self.assertIn(f'assets/{gen.MOD}/models/block/heavy_double_t/junction_{face}.json',paths)

    def test_preserves_uploaded_source(self):
        source=json.loads((ROOT/'sources/Heavy industrial Double T support.bbs.json').read_text('utf-8'))
        self.assertEqual(source['version'],'0.7.2')
        cubes=source['model']['groups']['Poteau en I 6,8']['cubes']
        self.assertEqual(len(cubes),7)
        self.assertEqual(source['model']['texture'],[16,16])

if __name__=='__main__':unittest.main()
