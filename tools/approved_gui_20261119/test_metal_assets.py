"""Local asset regression, standard library only; not Minecraft runtime evidence."""
import functools
import subprocess
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'validation'))
from factory_placer_v2_reference_native import decode_png
import build_metal_assets as m

ROOT = m.ROOT
BASE = '8f909beebf96b0197830813aaa415a0e72d44881'

class PngBytes:
    """Read-only byte source for the existing strict PNG decoder."""
    def __init__(self, data): self.data = data
    def read_bytes(self): return self.data

@functools.lru_cache(maxsize=None)
def picture(kind, previous=False):
    path = m.OUT / (kind + '.png')
    if previous:
        raw = subprocess.check_output(['git', 'show', BASE + ':' + str(path.relative_to(ROOT))], cwd=ROOT)
        return decode_png(PngBytes(raw))
    return decode_png(path)

def crop(image, box):
    w, h, data = image
    x0, y0, x1, y1 = box
    assert 0 <= x0 <= x1 <= w and 0 <= y0 <= y1 <= h, box
    return b''.join(data[(y*w+x0)*4:(y*w+x1)*4] for y in range(y0, y1))

def pixels(data):
    return (data[i:i+4] for i in range(0, len(data), 4))

def dark(pixel):
    return max(pixel[:3]) < 115 and max(pixel[:3]) - min(pixel[:3]) < 42

class MetalAssets(unittest.TestCase):
    def test_all_105_plates_are_matte_opaque_and_textured(self):
        count = 0
        for kind in ('hopper', 'dropper'):
            im = picture(kind)
            ox, oy = (8,16) if kind == 'hopper' else (0,0)
            for x, y, _ in m.base.slots(kind):
                count += 1
                px = list(pixels(crop(im, ((x+ox)*6,(y+oy)*6,(x+ox+16)*6,(y+oy+16)*6))))
                v = [max(p[:3]) for p in px]
                self.assertTrue(all(p[3] == 255 for p in px))
                self.assertGreater(min(v), 15)
                self.assertLessEqual(max(v), 100)
                self.assertGreater(max(v)-min(v), 10)
        self.assertEqual(count, 105)

    def test_controls_frames_and_external_pixels_unchanged(self):
        for kind in ('hopper', 'dropper'):
            old, new = picture(kind, True), picture(kind)
            self.assertEqual(old[:2], new[:2])
            w, h, oa = old
            na = new[2]
            mask = bytearray(w*h)
            def mark(x0,y0,x1,y1):
                self.assertTrue(0 <= x0 <= x1 <= w and 0 <= y0 <= y1 <= h)
                for y in range(y0,y1): mask[y*w+x0:y*w+x1] = b'\1'*(x1-x0)
            for x,y,z,t in m.PANELS[kind]+[m.BARS[kind]]: mark(x*6,y*6,z*6,t*6)
            for x,y,cw,ch in m.CONTROL_RECTS[kind]: mark(x*6,y*6,(x+cw)*6,(y+ch)*6)
            ox,oy = (8,16) if kind == 'hopper' else (0,0)
            for x,y,_ in m.base.slots(kind): mark((x+ox)*6,(y+oy)*6,(x+ox+16)*6,(y+oy+16)*6)
            self.assertTrue(all(oa[i*4:i*4+4] == na[i*4:i*4+4] for i,allowed in enumerate(mask) if not allowed))
            self.assertEqual(oa[3::4], na[3::4])
            for x,y,cw,ch in m.CONTROL_RECTS[kind]:
                box=(x*6,y*6,(x+cw)*6,(y+ch)*6)
                self.assertTrue(all(a == b for a,b in zip(pixels(crop(old,box)),pixels(crop(new,box))) if not dark(a)))
            for x,y,_ in m.base.slots(kind):
                x+=ox; y+=oy
                for a,b,c,d in [(x-1,y-1,x+17,y),(x-1,y+16,x+17,y+17),(x-1,y,x,y+16),(x+16,y,x+17,y+16)]:
                    self.assertEqual(crop(old,(a*6,b*6,c*6,d*6)),crop(new,(a*6,b*6,c*6,d*6)))

    def test_no_central_hazard_ribbon(self):
        for kind in ('hopper','dropper'):
            x,y,z,t=m.BARS[kind]
            px=pixels(crop(picture(kind),(x*6,y*6+6,z*6,t*6-6)))
            self.assertFalse(any(r>140 and g>95 and b<60 for r,g,b,a in px))

    def test_runtime_fixed_well_is_metal_but_rim_and_alpha_retained(self):
        old,new=picture('owner_control_well',True),picture('owner_control_well')
        self.assertEqual(old[:2],new[:2])
        pairs=list(zip(pixels(old[2]),pixels(new[2])))
        self.assertTrue(all(a==b for a,b in pairs if not dark(a)))
        self.assertEqual(old[2][3::4],new[2][3::4])
        self.assertGreater(min(v for a,b in pairs if dark(a) for v in b[:3]),15)

    def test_no_production_or_control_asset_change(self):
        for scope in ['src/main/java','src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2/owner_control_thumb.png']:
            self.assertEqual(subprocess.check_output(['git','diff',BASE,'--',scope],cwd=ROOT),b'')

if __name__ == '__main__': unittest.main()
