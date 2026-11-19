"""Local metal-assembly integration revision. No runtime or acceptance claim.
Originals remain external. Written owner directive supersedes black wells and
central hazard ribbon, but not geometry, native controls or outside silhouette.
"""
import hashlib,json,os
from pathlib import Path
import build_assets as base
ROOT=Path(__file__).resolve().parents[2]
SOURCES=Path(os.environ['IEDCTR_OWNER_SOURCE_DIR']) if os.environ.get('IEDCTR_OWNER_SOURCE_DIR') else None
OUT=base.OUT
CONTROL_RECTS={
 'hopper':[(139,29,38,7),(138,42,39,7),(140,55,37,7),(140,64,11,11),(152,64,11,11),(166,64,11,11)],
 'dropper':[(152,31,9,31),(169,32,30,29),(155,65,45,9),(155,77,45,9),(142,89,20,20),(165,89,20,20),(188,89,20,20)]}
PANELS={'hopper':[(18,24,127,78),(137,21,179,78),(16,86,179,141),(22,144,174,162)],
        'dropper':[(20,32,139,110),(148,28,210,111),(23,131,206,190),(29,195,201,216)]}
BARS={'hopper':(14,78,178,86),'dropper':(25,118,203,127)}
def protect(kind,x,y):return any(a<=x<b+a and c<=y<d+c for a,c,b,d in CONTROL_RECTS[kind])
def build(output=OUT):
 from PIL import Image,ImageEnhance
 assert SOURCES is not None, 'Reproduction requires external approved source directory'
 # Existing registered artwork is rebuilt first, never copied from a stale JAR.
 base.build(output)
 sources={}
 for kind,(name,sha,*_) in base.SPECS.items():
  raw=(SOURCES/name).read_bytes();assert hashlib.sha256(raw).hexdigest()==sha
  sources[kind]=Image.open(SOURCES/name).convert('RGBA')
 # Clean, unlabelled original steel regions. No generated or external art.
 steel=sources['dropper'].crop((300,440,331,524)).convert('RGB')
 # Matte relief is bounded, avoiding near-black wells and bright item competition.
 steel=ImageEnhance.Color(steel).enhance(.35)
 steel=steel.point([round(32+min(v,150)*.42) for v in range(256)]*3)
 pixels=steel.load();sw,sh=steel.size;report={}
 for kind in ('hopper','dropper'):
  path=output/(kind+'.png');im=Image.open(path).convert('RGBA');before=im.copy();p=im.load()
  def metal(x,y):
   # Stable global phase gives one coherent material across every adjacent plate.
   rgb=pixels[x%sw,y%sh];return (*rgb,255)
  ox,oy=(8,16) if kind=='hopper' else (0,0)
  protected_slots=[(x+ox-1,y+oy-1,x+ox+17,y+oy+17) for x,y,_ in base.slots(kind)]
  for panel_index,(x0,y0,x1,y1) in enumerate(PANELS[kind]):
   for y in range(y0*6,y1*6):
    for x in range(x0*6,x1*6):
     if any(a*6<=x<c*6 and b*6<=y<d*6 for a,b,c,d in protected_slots):continue
     r,g,b,a=p[x,y]
     # Keep existing bright bevels and copper fixings; replace interior backing.
     if a==255 and max(r,g,b)<115 and max(r,g,b)-min(r,g,b)<42:
      gain=.72 if protect(kind,x/6,y/6) else (.92,1.00,1.04,.97)[panel_index]
      p[x,y]=(*(round(c*gain) for c in metal(x,y)[:3]),255)
  ox,oy=(8,16) if kind=='hopper' else (0,0)
  for sx,sy,_ in base.slots(kind):
   x0=(sx+ox)*6;y0=(sy+oy)*6
   for y in range(y0,y0+96):
    for x in range(x0,x0+96):
     rgb=metal(x,y)[:3]
     # A matte individual inset plate: preserved perimeter, no hardware on item.
     edge=min(x-x0,y-y0,x0+95-x,y0+95-y)
     gain=.62 if edge<6 else (1.15 if kind=='dropper' and 70<sy<95 else .88)
     p[x,y]=(*(round(c*gain) for c in rgb),255)
  x0,y0,x1,y1=BARS[kind]
  # The central painted band becomes steel. Existing rails outside it remain.
  # No central yellow/black pixel or old compressed screw is retained.
  for y in range(y0*6,y1*6):
   for x in range(x0*6,x1*6):p[x,y]=(*(min(120,round(c*1.35)) for c in metal(x,y)[:3]),255)
  # Joint/bevel strips are sampled from the source steel frame, not drawn glyphs.
  rail=sources['dropper'].crop((300,606,530,612))
  for y in (y0*6,y1*6-3):
   im.paste(rail.resize(((x1-x0)*6,3),Image.Resampling.LANCZOS),(x0*6,y))
  # Source joints divide the structural bar into three joined plate lengths.
  joint=ImageEnhance.Brightness(ImageEnhance.Color(sources['dropper'].crop((759,174,768,335))).enhance(0)).enhance(.75)
  for xx in (round(x0+(x1-x0)/3),round(x0+2*(x1-x0)/3)):
   im.paste(joint.resize((6,(y1-y0)*6-6),Image.Resampling.LANCZOS),(xx*6,y0*6+3))
  # Fixed control backgrounds can sit outside the broad panel masks.
  for cx,cy,cw,ch in CONTROL_RECTS[kind]:
   for y in range(cy*6,(cy+ch)*6):
    for x in range(cx*6,(cx+cw)*6):
     r,g,b,a=p[x,y]
     if a==255 and max(r,g,b)<115 and max(r,g,b)-min(r,g,b)<42:
      p[x,y]=(*(round(c*.72) for c in metal(x,y)[:3]),255)
  im.save(path)
  report[kind]={'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'size':im.size,'panels':PANELS[kind],'central_bar':BARS[kind],'native':'NOT_RUN'}
 # Runtime fixed well overlays must use the same darker steel. Do not change
 # owner_control_thumb or any native glyph/UV/state texture.
 well_path=output/'owner_control_well.png';well=Image.open(well_path).convert('RGBA');wp=well.load()
 for y in range(well.height):
  for x in range(well.width):
   r,g,b,a=wp[x,y]
   if a==255 and max(r,g,b)<115 and max(r,g,b)-min(r,g,b)<42:
    wp[x,y]=(*(round(c*.72) for c in pixels[x%sw,y%sh]),255)
 well.save(well_path)
 report['owner_control_well']={'sha256':hashlib.sha256(well_path.read_bytes()).hexdigest(),'runtime':'NOT_RUN','scope':'fixed darker metal background only'}
 return report
if __name__=='__main__':print(json.dumps(build(),indent=2))
