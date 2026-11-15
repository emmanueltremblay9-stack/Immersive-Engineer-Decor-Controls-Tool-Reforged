import hashlib
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/LabeledCrateEditScreen.java'
class LabelEditorReferenceTest(unittest.TestCase):
 def test_art_exact(self):
  p=ROOT/'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/label_editor_reference_v1/background.png'
  self.assertEqual(hashlib.sha256(p.read_bytes()).hexdigest(),'8329a12b0fd82a767044cec03651bd4672ecca2d4cd6b3b5edbe49ea3f75e7ac')
 def test_preserves_native_widgets_and_unsaved_values(self):
  s=SOURCE.read_text()
  self.assertEqual(s.count('new EditBox('),2)
  self.assertIn('class PlaqueButton extends Button',s)
  self.assertIn('this.firstLine == null ? this.initialLines[0] : this.firstLine.getValue()',s)
  self.assertIn('this.secondLine == null ? this.initialLines[1] : this.secondLine.getValue()',s)
  self.assertIn('this.getFocused() instanceof Button',s)
  self.assertIn('button -> this.onClose()',s)
  self.assertIn('MachineBlockEntity.LABEL_MAX_UTF16_UNITS',s)
 def test_profiles_geometry(self):
  for w,h in [(1280,720),(640,360),(640,180),(1280,260),(426,240)]:
   scale=min(.25,max(1,w-12)/1536,max(1,h-12)/1024)
   pw,ph=int(1536*scale),int(1024*scale)
   self.assertLessEqual(pw,w);self.assertLessEqual(ph,h)
   # Text baseline +/-4 fits the retained source apertures on supported profiles.
   for center,lo,hi in [(438,392,487),(607,557,658)]:
    self.assertGreaterEqual(center*ph/1024-4,lo*ph/1024)
    self.assertLessEqual(center*ph/1024+5,hi*ph/1024)
if __name__=='__main__':unittest.main()
