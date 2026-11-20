"""Current-candidate alpha tests. No native-rendering claim."""
import hashlib,subprocess,sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'validation'))
from factory_placer_v2_reference_native import decode_png
import factory_owner_20261119 as gate
import dropper_alpha as a
class Bytes:
    def __init__(self,b):self.b=b
    def read_bytes(self):return self.b
class DropperAlpha(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        gate.ensure_base_object(anchor=a.PARENT)
        raw=subprocess.check_output(['git','show',a.PARENT+':'+a.ASSET],cwd=a.ROOT)
        assert hashlib.sha256(raw).hexdigest()=='59b0d6029aa2440df05c59e4cc92da220450322ecf5d57b9f4c4d92981d0e8cf'
        cls.old=decode_png(Bytes(raw));cls.new=decode_png(a.ROOT/a.ASSET)
    def test_real_candidate(self):a.verify(self.old,self.new)
    def test_reject_old_matte(self):
        with self.assertRaises(AssertionError):a.verify(self.old,self.old)
    def test_reject_interior_hole(self):
        b=bytearray(self.new[2]);b[(600*1368+600)*4+3]=0
        with self.assertRaises(AssertionError):a.verify(self.old,(*self.new[:2],bytes(b)))
    def test_reject_rgb_change(self):
        b=bytearray(self.new[2]);b[0]^=1
        with self.assertRaises(AssertionError):a.verify(self.old,(*self.new[:2],bytes(b)))
    def test_v5_recipe_frozen(self):
        self.assertEqual(gate.digest(gate.V5_RECIPE.read_bytes()),gate.V5_RECIPE_SHA256)
    def test_reject_v5_recipe_mutation(self):
        import tempfile
        from unittest.mock import patch
        with tempfile.TemporaryDirectory() as td:
            p=Path(td)/'v5.json';p.write_bytes(gate.V5_RECIPE.read_bytes()+b' ')
            with patch.object(gate,'V5_RECIPE',p),self.assertRaises(AssertionError):gate.recipe()
