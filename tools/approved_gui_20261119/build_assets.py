"""Deterministic integration assets, not a native render or owner acceptance.
Only the two SHA-pinned owner references supply pixels. No generated artwork.
"""
import hashlib,json,os
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SOURCES=Path(os.environ['IEDCTR_OWNER_SOURCE_DIR']) if os.environ.get('IEDCTR_OWNER_SOURCE_DIR') else None
OUT=ROOT/'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2'
SPECS={
'hopper':('ChatGPT Image 19 Nov 2026, 05_52_17-1.png','566a03669af7e5299b4ec3c795f8cb4937fa47b8cb1770ca16d6b00676196282',(192,182),[(0,0),(129,18),(821,127),(891,137),(1195,178),(1292,192)],[(0,0),(163,24),(511,78),(637,86),(977,141),(1004,144),(1110,161),(1218,182)]),
'dropper':('ChatGPT Image 19 Nov 2026, 05_52_20-2.png','844ded3692701c998790b2d61b946355a6f6a44f72e4b389f6f558ce8b8d5fa2',(228,250),[(0,0),(1198,228)],[(0,0),(1313,250)])}
def axis(value,anchors):
 for (s0,d0),(s1,d1) in zip(anchors,anchors[1:]):
  if d0<=value<=d1:return s0+(value-d0)*(s1-s0)/(d1-d0)
 raise AssertionError(value)
def patch(image,src,box):
 from PIL import Image
 image.paste(src.resize((box[2]-box[0],box[3]-box[1]),Image.Resampling.LANCZOS),box)
def tile_source(image,src,box):
 # Preserve source texture frequency rather than magnifying a tiny sample.
 for y in range(box[1],box[3],src.height):
  for x in range(box[0],box[2],src.width):
   image.paste(src.crop((0,0,min(src.width,box[2]-x),min(src.height,box[3]-y))),(x,y))

def slots(kind):
 if kind=='hopper':
  return [(11+18*c,9+18*r, (129+114*c,163+116*r,243+114*c,279+116*r)) for r in range(3) for c in range(6)]+[(8+18*c,71+18*r,(140+112*c,652+108*r,252+112*c,760+108*r)) for r in range(3) for c in range(9)]+[(8+18*c,129,(171+106*c,1003,277+106*c,1110)) for c in range(9)]
 rnd=lambda a:int(a+.5)
 return [(rnd((154+99*c)*228/1199)-8,rnd((239+108*r)*250/1312)-8,(141+93*c,220+90*r,234+93*c,310+90*r)) for r in range(2) for c in range(6)]+[(rnd((202+174*c)*228/1199)-8,rnd(474*250/1312)-8,(186+161*c,436,282+161*c,535)) for c in range(3)]+[(rnd((199+100*c)*228/1199)-8,rnd((740+96*r)*250/1312)-8,(153+99*c,701+97*r,252+99*c,798+97*r)) for r in range(3) for c in range(9)]+[(rnd((207+98*c)*228/1199)-8,rnd(1075*250/1312)-8,(178+94*c,1028,272+94*c,1126)) for c in range(9)]
def build(output=OUT, manifest_path=None):
 if SOURCES is None:raise RuntimeError("Private reproduction requires IEDCTR_OWNER_SOURCE_DIR outside the repository")
 if ROOT.resolve() in SOURCES.resolve().parents or SOURCES.resolve()==ROOT.resolve():raise RuntimeError("Private source directory must remain outside the public repository")
 from PIL import Image
 output.mkdir(parents=True,exist_ok=True);manifest={'recipe':'owner-20261119-v2-wells-stripe','baseline':'87f8b8e03cac46a6154b54522d1497baa52c67b2','status':'INTEGRATION_CANDIDATE_NOT_NATIVE_TESTED','sources':{},'slots':{}}
 for kind,(name,sha,size,xs,ys) in SPECS.items():
  path=SOURCES/name;assert hashlib.sha256(path.read_bytes()).hexdigest()==sha
  source=Image.open(path).convert('RGBA');W,H=size[0]*6,size[1]*6
  # Piecewise registration of owner architecture to unchanged logical menu.
  mesh=[]
  for (_,y0),(_,y1) in zip(ys,ys[1:]):
   for (_,x0),(_,x1) in zip(xs,xs[1:]):
    box=(x0*6,y0*6,x1*6,y1*6);sx0,sx1=axis(x0,xs),axis(x1,xs);sy0,sy1=axis(y0,ys),axis(y1,ys)
    mesh.append((box,(sx0,sy0,sx0,sy1,sx1,sy1,sx1,sy0)))
  # Remove the old registered supports BEFORE warping: one frame per new slot,
  # never a new well pasted over a displaced original well/shadow.
  clean=source.copy()
  regions=([(129,163,821,511),(140,652,1156,977),(171,1003,1125,1110)] if kind=='hopper' else
           [(136,215,706,406),(179,429,616,542),(153,701,1044,992),(178,1028,1024,1126),(795,168,1090,578)])
  backing=source.crop((301,440,331,524) if kind=='dropper' else (390,88,430,98))
  for region in regions:tile_source(clean,backing,region)
  result=clean.transform((W,H),Image.Transform.MESH,mesh,Image.Resampling.BICUBIC)
  rects=[]
  ox,oy=(8,16) if kind=='hopper' else (0,0)
  for x,y,box in slots(kind):
   # Register the approved slot border outside the 16x16 item footprint.
   x+=ox;y+=oy
   tile=source.crop(box);patch(result,tile,((x-1)*6,(y-1)*6,(x+17)*6,(y+17)*6))
   # Preserve the complete dark well and its vignette, not a flat centre crop.
   # A fixed 16-source-pixel inset excludes metal on every approved source slot.
   well=tile.crop((16,16,tile.width-16,tile.height-16))
   # Local rejected-runtime follow-up: lift only the empty cavity, not metal,
   # controls, items or the panel. Preserve source vignette; RGB curve may shift chroma slightly.
   # Shadow floor + concave tonal curve retain a dark edge and graded matte
   # cavity. Only RGB of the empty well changes; alpha stays opaque.
   channels=well.split()
   lut=[min(255,round(5+50*(v/32)**0.7)) for v in range(256)]
   well=Image.merge('RGBA',tuple(c.point(lut) for c in channels[:3])+(channels[3],))
   patch(result,well,(x*6,y*6,(x+16)*6,(y+16)*6));rects.append([x,y,16,16])
  controls=([(131,13,38,7,(911,146,1193,218)),(130,26,39,7,(911,241,1193,314)),(132,39,37,7,(911,336,1193,409)),(132,48,11,11,(909,432,988,512)),(144,48,11,11,(1009,432,1088,512)),(158,48,11,11,(1111,432,1190,512))] if kind=='hopper' else [(152,31,9,31,(806,174,858,343)),(169,32,30,29,(900,180,1062,336)),(155,65,45,9,(808,366,1072,412)),(155,77,45,9,(808,432,1072,480)),(142,89,20,20,(796,508,879,567)),(165,89,20,20,(899,508,982,567)),(188,89,20,20,(1000,508,1083,567))])
  # Erase transformed control frames before registration, using only a blank
  # interior from this same approved source. Otherwise ghost rails survive.
  if kind=='hopper':
   patch(result,source.crop((970,170,1010,190)),((129+ox)*6,(5+oy)*6,(170+ox)*6,(62+oy)*6))
  else:
   tile_source(result,source.crop((301,440,331,524)),(141*6,64*6,211*6,110*6))
  for x,y,w,h,box in controls:
   x+=ox;y+=oy
   patch(result,source.crop(box),(x*6,y*6,(x+w)*6,(y+h)*6))
  if kind=='hopper':
   # Native gap is only 8 logical pixels: preserve stripe direction isotropically.
   # Repetition uses one 86px source period, not a flattened whole crossmember.
   left,right,top,bottom=18*6,177*6,78*6,86*6
   # Pure painted ribbon sampled between fixings. Isotropic sampling preserves
   # the original diagonal; phase is tied to source x=140, period 86 texels.
   ribbon=source.crop((439,575,525,613))
   scale=(right-left)/1033
   tile_w=round(86*scale);tile_h=round(38*scale)
   ribbon=ribbon.resize((tile_w,tile_h),Image.Resampling.LANCZOS)
   phase=round(((140-439)%86)*scale)
   for xx in range(left-phase,right,tile_w):
    lo=max(left,xx);hi=min(right,xx+tile_w)
    result.paste(ribbon.crop((lo-xx,0,hi-xx,tile_h)),(lo,top+(48-tile_h)//2))
   # Close the integer-resampling seam explicitly, never retain old motif.
   seam=top+(48-tile_h)//2+tile_h
   if seam<bottom-6:
    result.paste(result.crop((left,seam-1,right,seam)).resize((right-left,bottom-6-seam)),(left,seam))
   patch(result,source.crop((140,554,1173,563)),(left,top,right,top+6))
   patch(result,source.crop((140,615,1173,624)),(left,bottom-6,right,bottom))
   # Small sampled heads preserve source cadence, with 1.5px guard to rails.
   for sx in (377,647,917):
    cx=round((18+(sx-140)*159/1033)*6)
    patch(result,source.crop((sx-23,567,sx+23,613)),(cx-15,top+9,cx+15,top+39))
   # Large octagonal corner bolts cannot fit that gap. Keep them circular in
   # the margins, fully outside all slot/control footprints (14px wide).
   patch(result,source.crop((0,516,130,646)),(0,75*6,14*6,89*6))
   patch(result,source.crop((1162,516,1292,646)),(178*6,75*6,192*6,89*6))
  result.save(output/(kind+'.png'))
  manifest['sources'][kind]={'file':name,'sha256':sha,'size':source.size,'output_sha256':hashlib.sha256((output/(kind+'.png')).read_bytes()).hexdigest(),'draw_offset':[-ox,-oy],'x_registration':xs,'y_registration':ys}
  manifest['slots'][kind]=rects
 # Runtime rim replaces historical Placer texture with an owner-source rim.
 source=Image.open(SOURCES/SPECS['hopper'][0]).convert('RGBA');source.crop((129,163,243,279)).resize((108,108),Image.Resampling.LANCZOS).save(output/'owner_slot_rim.png')
 source.crop((909,432,988,512)).resize((108,108),Image.Resampling.LANCZOS).save(output/'owner_control_well.png')
 source.crop((37,29,96,91)).resize((60,60),Image.Resampling.LANCZOS).save(output/'owner_control_thumb.png')
 manifest['runtime_assets']={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(output.glob('*.png')) if p.name in ['hopper.png','dropper.png','owner_slot_rim.png','owner_control_well.png','owner_control_thumb.png']}
 manifest['input_bounds']={'hopper':[-8,-16,184,166],'others':'super unchanged','carried_stack_native_test':'NOT_RUN'}
 if manifest_path is not None: manifest_path.write_text(json.dumps(manifest,indent=2)+'\n')
 return manifest
if __name__=='__main__':build()
