"""Validate exact V6 alpha and narrowly bounded RGB against actual repo PNGs."""
import functools,io,subprocess,sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'validation'))
from factory_placer_v2_reference_native import decode_png
import factory_owner_20261119 as gate
import build_slot_relief as art

class Bytes(io.BytesIO):
    def read_bytes(self):return self.getvalue()

@functools.lru_cache(maxsize=None)
def pictures(kind):
    gate.ensure_base_object(anchor=art.PARENT)
    path=art.base.OUT/(kind+'.png'); rel=str(path.relative_to(gate.ROOT))
    old=subprocess.check_output(['git','show',art.PARENT+':'+rel],cwd=gate.ROOT)
    assert gate.digest(old)==art.V6[kind]==gate.historical_v6_recipe()['candidate'][rel]
    return decode_png(Bytes(old)),decode_png(path)


def mask(kind,w,h):
    allowed=bytearray(w*h)
    def mark(x0,y0,x1,y1,value):
        assert 0<=x0<=x1<=w and 0<=y0<=y1<=h
        for y in range(y0,y1):allowed[y*w+x0:y*w+x1]=bytes([value])*(x1-x0)
    if kind=='dropper':
        # Closed polygon in logical coordinates, exact integer half-planes.
        corners=art.FILTER_PANEL[1:]+art.FILTER_PANEL[:1]
        for y in range(77*6,103*6+1):
            for x in range(23*6,122*6+1):
                if all((c-a)*(y-b*6)>=(d-b)*(x-a*6) for (a,b),(c,d) in zip(art.FILTER_PANEL,corners)):
                    allowed[y*w+x]=1
    for i,(x,y,_) in enumerate(art.footprint(kind)):
        mark((x-1)*6,(y-1)*6,(x+17)*6,(y+17)*6,1)
        mark(x*6,y*6,(x+16)*6,(y+16)*6,0)
        if kind=='dropper' and 12<=i<15:
            mark((x+1)*6,(y+1)*6,(x+15)*6,(y+15)*6,2)
    return allowed


def verify(kind,old,new):
    assert old[:2]==new[:2] and old[:2]==({'hopper':(1152,1092),'dropper':(1368,1500)}[kind])
    w,h,a=old;b=new[2]
    assert a[3::4]==b[3::4],'V6 alpha changed'
    allowed=mask(kind,w,h)
    count=0
    for j,permit in enumerate(allowed):
        i=j*4
        if not permit:assert a[i:i+3]==b[i:i+3],'Protected RGB changed'
        elif permit==2:
            assert b[i:i+3]==bytes(min(100,v+5) for v in a[i:i+3]),'Filter well tone differs'
        count+=a[i:i+3]!=b[i:i+3]
    assert count>0,'Slot relief absent'
    return count


class SlotRelief(unittest.TestCase):
    def test_exact_alpha_and_bounded_current_rgb(self):
        for kind in ('hopper','dropper'):
            with self.subTest(kind=kind):verify(kind,*pictures(kind))

    def test_reject_old_art_and_alpha_or_foreign_rgb_mutation(self):
        old,new=pictures('dropper')
        with self.assertRaises(AssertionError):verify('dropper',old,old)
        for off in (3,0,(105*6*1368+38*6)*4):
            with self.subTest(offset=off),self.assertRaises(AssertionError):
                b=bytearray(new[2]);b[off]^=1;verify('dropper',old,(*new[:2],bytes(b)))

    def test_filter_four_corners_survive_actual_scale1_sampling(self):
        _,(w,_,p)=pictures('dropper')
        for x,y,_ in art.footprint('dropper')[12:15]:
            for cx,cy in [(x-1,y-1),(x+16,y-1),(x-1,y+16),(x+16,y+16)]:
                for dx,dy in [(2,2),(2,3),(3,2),(3,3)]:
                    i=((cy*6+dy)*w+cx*6+dx)*4
                    self.assertGreaterEqual(p[i]-p[i+2], 38)

    def test_v6_history_pin_cannot_be_rewritten(self):
        import tempfile
        from unittest.mock import patch
        with tempfile.TemporaryDirectory() as td:
            p=Path(td)/'v6.json';p.write_bytes(gate.V6_RECIPE.read_bytes()+b' ')
            with patch.object(gate,'V6_RECIPE',p),self.assertRaisesRegex(AssertionError,'Historical v6 recipe changed'):gate.recipe()
