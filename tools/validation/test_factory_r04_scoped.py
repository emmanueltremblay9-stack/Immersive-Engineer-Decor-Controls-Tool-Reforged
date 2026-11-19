"""Factory-only R04 static scope regression (not Minecraft QA)."""
import hashlib, json, unittest
from pathlib import Path
from factory_placer_v2_reference_native import decode_png
ROOT=Path(__file__).resolve().parents[2]
ASSET=ROOT/'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2'
DOC=ROOT/'docs/qualification/factory-steel-v2'
CLIENT=ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client'
EXPECTED={'hopper':('14d2a06b9d434a722e67a28c7a3ea1c889d52cbf45236b3678d1248e140622e3',(1056,996),54,6), 'dropper':('9c8517f4a8c6c138b899038de3bb72ed4856676351d6b1dd4972c90b1787baaf',(1368,1500),51,8)}
class FactoryOnlyR04(unittest.TestCase):
 def test_exact_art_and_inventory_geometry(self):
  for kind,(sha,dims,count,controls) in EXPECTED.items():
   with self.subTest(kind=kind):
    data=(ASSET/(kind+'.png')).read_bytes()
    self.assertEqual(hashlib.sha256(data).hexdigest(),sha)
    width,height,pixels=decode_png(ASSET/(kind+'.png'))
    self.assertEqual((width,height),dims)
    d=json.loads((DOC/(kind+'-mapping.json')).read_text())
    slots=d['machine_slots']+d['player_slots']+d['hotbar_slots']
    self.assertEqual(len(slots),count)
    self.assertEqual(len(d['interactive_controls']),controls)
    self.assertEqual([slot['menu_index'] for slot in slots],list(range(count)))
    for slot in slots:
     self.assertEqual((slot['w'],slot['h']),(16,16))
     x=slot['x']*6;y=slot['y']*6
     self.assertTrue(0<=x and x+96<=width and 0<=y and y+96<=height)
     for line in range(y,y+96):
      start=(line*width+x)*4
      self.assertEqual(pixels[start:start+96*4],bytes((12,17,20,255))*96,slot['id'])
 def test_native_controls_not_replaced_by_background(self):
  frame=(CLIENT/'FactorySteelFrame.java').read_text()
  screen=(CLIENT/'MachineScreen.java').read_text()
  hopper=(CLIENT/'FactoryHopperSteelArtwork.java').read_text()
  self.assertNotIn('static void frame(',frame)
  self.assertNotIn('static void divider(',frame)
  self.assertIn('FactorySteelFrame.slots(g,menu,x,y)',hopper)
  self.assertIn('controlSymbol(graphics,x+sx+5,y+90,n)',screen)
  self.assertIn('y0 + 8 + slotIndex / 6 * 18',screen)
  for method in ('handleFactoryHopperClick','handleFactoryDropperClick','drawRetainedDropper','drawFactoryHopperWidgets'):
   self.assertIn(method+'(',screen)
if __name__=='__main__':unittest.main()
