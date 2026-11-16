"""Exact candidate art guards. Not a substitute for native GUI qualification."""
import hashlib
import json
from pathlib import Path
import unittest
from factory_placer_v2_reference_native import decode_png

ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2'
DOC=ROOT/'docs/qualification/factory-steel-v2'

class FactoryCandidateV2Tests(unittest.TestCase):
    def test_entire_item_footprints_are_clear(self):
        for name,count,size in [('hopper',54,(1056,996)),('dropper',51,(1368,1500))]:
            with self.subTest(name=name):
                mapping=json.loads((DOC/f'{name}-mapping.json').read_text())
                width,height,pixels=decode_png(ASSETS/f'{name}.png')
                self.assertEqual((width,height),size)
                slots=sum((mapping[k] for k in ['machine_slots','player_slots','hotbar_slots']),[])
                self.assertEqual(len(slots),count)
                self.assertEqual([x['menu_index'] for x in slots],list(range(count)))
                for slot in slots:
                    x,y=slot['x']*6,slot['y']*6
                    self.assertEqual(slot['w'],16);self.assertEqual(slot['h'],16)
                    for row in range(y,y+96):
                        start=(row*width+x)*4
                        self.assertEqual(pixels[start:start+96*4],bytes([12,17,20,255])*96,slot['id'])

    def test_exact_bytes_match_specialist_provenance(self):
        source=json.loads((DOC/'provenance.json').read_text())
        self.assertEqual(source['source_zip_sha256'],'fc0f24b3f1523004be384a2e7ea776ea8b3e4f7a372959d400bfef1eb7382754')
        for name,digest in source['assets'].items():
            self.assertEqual(hashlib.sha256((ASSETS/f'{name}.png').read_bytes()).hexdigest(),digest)

    def test_live_slot_rims_and_symbols_are_outside_content(self):
        folder=ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client'
        hopper=(folder/'FactoryHopperSteelArtwork.java').read_text()
        dropper=(folder/'MachineScreen.java').read_text().split('private void drawRetainedDropper',1)[1].split('private void drawFactoryDropperWidgets',1)[0]
        self.assertIn('FactorySteelFrame.slots(g,menu,x,y);',hopper)
        self.assertIn('FactorySteelFrame.slots(graphics,m,x,y);',dropper)
        self.assertIn('controlSymbol(graphics,x+sx+5,y+90,n)',dropper)
        self.assertIn('inset(graphics,x+sx,y+100,18,8)',dropper)
        self.assertLess(90+7,100)
        frame=(folder/'FactorySteelFrame.java').read_text()
        for fragment in ['sx-1,sy-1,18,1','sx-1,sy+16,18,1','sx-1,sy,1,16','sx+16,sy,1,16']:
            self.assertIn(fragment,frame)

    def test_capture_uses_full_stacks_for_frame_review(self):
        source=(ROOT/'src/guiAlbumThree/java/com/oblixorprime/iedctrtest/factoryplacer/GuiAlbumThree.java').read_text()
        for index in [0,5]:
            self.assertIn(f'machines[{index}].setItem(0,new ItemStack(Items.COBBLESTONE,64));',source)

if __name__=='__main__':unittest.main()
