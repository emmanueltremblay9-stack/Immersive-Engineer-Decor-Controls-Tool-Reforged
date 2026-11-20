"""Source geometry regression only, not native Minecraft or physical input QA."""
import re
import hashlib
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parent
SOURCE=ROOT/'MachineScreen.java'
LAYOUT=ROOT/'MachineLayout.java'
if not SOURCE.exists():
    ROOT=Path(__file__).resolve().parents[2]
    SOURCE=ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java'
    LAYOUT=ROOT/'src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineLayout.java'

def layout_geometry():
    layout=LAYOUT.read_text()
    hopper=layout.split('private static MachineLayout hopper()',1)[1].split('\n   private ',1)[0]
    grid=re.search(r'grid\(0,\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*"Storage"\)',hopper)
    assert grid, 'Hopper layout changed; review selection alignment'
    sx,sy,cols,rows=map(int,grid.groups())
    grid_body=layout.split('private static List<MachineLayout.MachineSlot> grid(',1)[1].split('\n   @',1)[0]
    px=int(re.search(r'startX\s*\+\s*column\s*\*\s*(\d+)',grid_body).group(1))
    py=int(re.search(r'startY\s*\+\s*row\s*\*\s*(\d+)',grid_body).group(1))
    return sx,sy,cols,rows,px,py

def selection_xy(source,index,x0=0,y0=0):
    body=source.split('private void drawFactoryHopperWidgets(',1)[1].split('\n   private ',1)[0]
    match=re.search(r'graphics\.blit\(texture, (x0[^,]+), (y0[^,]+), 200, 8, 18, 18\);',body)
    assert match, 'Selection expression missing or changed; re-review actual geometry'
    # Strict grammar accepts only this integer arithmetic expression, no arbitrary code.
    exprs=match.groups()
    for expr in exprs:
        assert re.fullmatch(r'[a-zA-Z0-9_ +*/%()-]+',expr)
        assert set(re.findall(r'[A-Za-z_][A-Za-z_0-9]*',expr)) <= {'x0','y0','slotIndex'}
    return tuple(eval(e.replace('/','//'),{'__builtins__':{}},{'x0':x0,'y0':y0,'slotIndex':index}) for e in exprs)

def assert_layout(source):
    sx,sy,cols,rows,px,py=layout_geometry()
    for x0,y0 in [(0,0),(31,47)]:
        for i in range(cols*rows):
            expected=(x0+sx+(i%cols)*px-1,y0+sy+(i//cols)*py-1)
            assert selection_xy(source,i,x0,y0)==expected,(i,expected,selection_xy(source,i,x0,y0))

class HopperSelectionAlignment(unittest.TestCase):
    def test_all_six_columns_all_three_rows_and_offset(self):
        assert_layout(SOURCE.read_text())
    def test_old_seventeen_pitch_is_rejected(self):
        source=SOURCE.read_text();start=source.index('private void drawFactoryHopperWidgets(')
        mutated=source[:start]+source[start:].replace('y0 + 8 + slotIndex / 6 * 18','y0 + 8 + slotIndex / 6 * 17',1)
        with self.assertRaises(AssertionError): assert_layout(mutated)
    def test_each_row_top(self):
        s=SOURCE.read_text()
        self.assertEqual([selection_xy(s,i)[1] for i in (0,6,12)],[8,26,44])
    def test_placer_method_unchanged_by_hopper_fix(self):
        part=SOURCE.read_text().split('   private void drawFactoryPlacerWidgets(',1)[1].split('\n   private ',1)[0]
        self.assertEqual(hashlib.sha256(part.encode()).hexdigest(),'2bcef1a03a54fd16966dcf87dbe1a075356d66b7189269b8f54d617b3be70271')

if __name__=='__main__':unittest.main()
