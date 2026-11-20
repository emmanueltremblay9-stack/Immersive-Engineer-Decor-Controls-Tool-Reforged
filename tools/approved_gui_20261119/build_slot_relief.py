"""Deterministic slot artwork from exact Nov19 refs and V6. No native claim."""
import argparse
import hashlib
import io
from pathlib import Path
import subprocess

import build_assets as base

PARENT = 'b144a647b9c77a6dac88ec6ee0ddda1e8adace2e'
FILTER_PANEL = ((23,81),(27,77),(118,77),(122,81),(122,99),(118,103),(27,103),(23,99))
V6 = {'hopper': '0dbff7d15d60aa333ef0f3d5ddd36009066ccc09ca5221b2d4d749be3768f0ab',
      'dropper': 'a5fce158a629a17a76418c10af28ea1e43ec42c58d739c7951b7f63ae96d8cdc'}


def footprint(kind):
    ox, oy = (8,16) if kind == 'hopper' else (0,0)
    return [(x+ox,y+oy,box) for x,y,box in base.slots(kind)]


def filter_plate(image, source):
    from PIL import Image, ImageDraw, ImageEnhance
    panel = Image.new('L', image.size)
    d = ImageDraw.Draw(panel)
    d.polygon([(x*6,y*6) for x,y in FILTER_PANEL], fill=1)
    d.polygon([(x*6,y*6) for x,y in [(24,82),(28,78),(117,78),(121,82),(121,98),(117,102),(28,102),(24,98)]], fill=2)
    d.polygon([(x*6,y*6) for x,y in [(25,83),(29,79),(116,79),(120,83),(120,97),(116,101),(29,101),(25,97)]], fill=3)
    for x,y,_ in footprint('dropper')[12:15]:
        d.rectangle((x*6,y*6,(x+16)*6-1,(y+16)*6-1),fill=0)
    swatch = ImageEnhance.Color(source.crop((300,440,331,524)).convert('RGB')).enhance(.25)
    dest = image.load(); px=panel.load(); steel=swatch.load()
    for y in range(77*6,103*6+1):
        for x in range(23*6,122*6+1):
            band=px[x,y]
            if band:
                src=steel[x%31,y%84]
                # Three steel levels: dark contact shadow, raised bevel and recessed plate.
                floor,gain= (18,.28) if band==1 else (93,.48) if band==2 and y<90*6 else (33,.40) if band==2 else (37,.57)
                dest[x,y]=tuple(round(floor+v*gain) for v in src)+(dest[x,y][3],)


def trim(image,source,slots,filters=False):
    from PIL import Image
    dest=image.load()
    for x,y,box in slots:
        sample=source.crop(box).resize((108,108),Image.Resampling.LANCZOS).load()
        for j in range(108):
            for i in range(108):
                tx,ty=(x-1)*6+i,(y-1)*6+j
                old=dest[tx,ty]
                if 6<=i<102 and 6<=j<102:
                    # Tone the existing empty steel, never draw trim into the item square.
                    if filters and 12<=i<96 and 12<=j<96:
                        dest[tx,ty]=tuple(min(100,v+5) for v in old[:3])+(old[3],)
                    continue
                # Preserve genuine bronze corners, recover a cool beveled edge at 1×.
                s=sample[i,j][:3];hi=max(s);top_left=j<6 or i<6
                cx,cy=min(i,107-i),min(j,107-j)
                if filters and cx<6 and cy<6:
                    # One logical corner cell per corner, neither a painted screw nor a fill.
                    rgb=(180,140,96) if cx+cy>=4 else (65,64,61)
                elif s[0]-s[2]>=44 and s[0]>=95:
                    rgb=s
                else:
                    target=round((112 if top_left else 61)+min(hi,210)*(.46 if top_left else .37))
                    rgb=(target-3,target,target+4)
                dest[tx,ty]=tuple(round(.25*a+.75*b) for a,b in zip(old[:3],rgb))+(old[3],)


def build(sources,out,filters_only=False):
    from PIL import Image
    assert base.ROOT.resolve() not in (sources.resolve(),*sources.resolve().parents)
    out.mkdir(parents=True,exist_ok=True)
    for kind in ('dropper',) if filters_only else ('dropper','hopper'):
        name,sha,*_=base.SPECS[kind]
        b=(sources/name).read_bytes(); assert hashlib.sha256(b).hexdigest()==sha
        src=Image.open(io.BytesIO(b)).convert('RGBA')
        rel=str((base.OUT/(kind+'.png')).relative_to(base.ROOT))
        old=subprocess.check_output(['git','show',PARENT+':'+rel],cwd=base.ROOT)
        assert hashlib.sha256(old).hexdigest()==V6[kind]
        art=Image.open(io.BytesIO(old)).convert('RGBA')
        if kind=='dropper':filter_plate(art,src)
        slots=footprint(kind)
        if kind=='dropper':
            trim(art,src,slots[12:15],filters=True)
            if not filters_only:trim(art,src,slots[:12]+slots[15:])
        else:trim(art,src,slots)
        p=out/(kind+'.png');art.save(p);print(kind,hashlib.sha256(p.read_bytes()).hexdigest())


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('sources',type=Path);p.add_argument('output',type=Path);p.add_argument('--filters-only',action='store_true')
    a=p.parse_args();build(a.sources,a.output,a.filters_only)
