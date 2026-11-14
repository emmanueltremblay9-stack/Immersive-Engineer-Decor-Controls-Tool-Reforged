import copy
import hashlib
import struct
import tempfile
import unittest
from pathlib import Path
from freezer_full_inventory_proof import validate_full_inventory
from client_mixin_smoke import SmokeFailure

class FullInventoryProofTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(); self.addCleanup(self.tmp.cleanup)
        self.game = Path(self.tmp.name); folder = self.game / 'freezer-full-inventory-screenshots'; folder.mkdir()
        def capture(name):
            data = b'\x89PNG\r\n\x1a\n' + b'\0'*8 + struct.pack('>II', 1280, 260)
            (folder / name).write_bytes(data)
            return dict(name=name, width=1280, height=260, sha256=hashlib.sha256(data).hexdigest())
        self.proof = dict(durability_verified=False, full_inventory_input='GLFW cursor positioning and native tooltip/render events; not physical OS input', full_inventory_overview=capture('inventory-full.png'), full_inventory_hovers=[])
        for i in range(3, 39):
            self.proof['full_inventory_hovers'].append(dict(capture(f'inventory-slot-{i}.png'), menu_index=i, container_index=i+6 if i<30 else i-30, count=64, item='minecraft:stone', rendered_frames=3, pointer_x=i*2, pointer_y=100))
    def test_complete(self):
        self.assertTrue(validate_full_inventory(self.proof, self.game, 1280, 260))
    def test_reject_mutations(self):
        for key, value in [('menu_index', 9), ('container_index', 8), ('count', 63), ('rendered_frames', 0), ('pointer_x', float('nan')), ('item', 'minecraft:dirt'), ('sha256','bad')]:
            with self.subTest(key=key):
                p=copy.deepcopy(self.proof); p['full_inventory_hovers'][0][key]=value
                with self.assertRaises(SmokeFailure): validate_full_inventory(p,self.game,1280,260)
    def test_missing_reordered_overstated(self):
        for mutation in ('missing','reorder','audio'):
            p=copy.deepcopy(self.proof)
            if mutation=='missing': p['full_inventory_hovers'].pop()
            elif mutation=='reorder': p['full_inventory_hovers'].reverse()
            else: p['durability_verified']=True
            with self.assertRaises(SmokeFailure): validate_full_inventory(p,self.game,1280,260)

if __name__=='__main__': unittest.main()
