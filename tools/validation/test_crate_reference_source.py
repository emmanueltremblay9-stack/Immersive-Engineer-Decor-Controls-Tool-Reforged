import hashlib
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]
JAVA=ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged'
class CrateReferenceSourceTest(unittest.TestCase):
 def test_art_exact(self):
  p=ROOT/'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/crate_reference_v1/background.png'
  self.assertEqual(hashlib.sha256(p.read_bytes()).hexdigest(),'fbd91d759dcc40a4733b7a5749fd356577c6d92bb2e42dbf0fc60d73e14cc8a3')
 def test_client_only_mapping(self):
  s=(JAVA/'menu/LabeledCrateMenu.java').read_text()
  self.assertIn('new SimpleContainer(CRATE_SLOT_COUNT), true)',s)
  self.assertIn('this(id, inventory, container, false)',s)
  self.assertIn('MENU_SLOT_COUNT = 91',s)
  self.assertIn('STORAGE_SLOT_COUNT = 54',s)
 def test_title_tooltip_preserves_custom_menu_title(self):
  s=(JAVA/'client/LabeledCrateScreen.java').read_text()
  self.assertIn('containsTitlePlaque(view.localX(mouseX), view.localY(mouseY))',s)
  self.assertIn('font.split(title, Math.max(1, Math.min(240, width - 16)))',s)
  self.assertNotIn('graphics.drawString',s)
 def test_screen_input_and_quick_transfer(self):
  s=(JAVA/'client/LabeledCrateScreen.java').read_text()
  for n in ['mouseClicked','mouseReleased','mouseDragged','mouseScrolled','mouseMoved','renderTooltip','getGuiLeft','getGuiTop','getXSize','getYSize']:
   self.assertIn(n,s)
  self.assertIn('view.localDelta(dragX)',s)
  self.assertIn('Screen.hasShiftDown() && Screen.hasControlDown()',s)
  self.assertIn('new LabeledCrateQuickMovePayload(this.menu.containerId, slotId)',s)
if __name__=='__main__': unittest.main()
