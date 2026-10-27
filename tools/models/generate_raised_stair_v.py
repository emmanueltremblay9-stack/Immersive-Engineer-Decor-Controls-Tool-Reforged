"""Split the approved mesh at the block boundary; reuse NeoForge's OBJ loader.

The stem is matched to the real pole section; the approved V above -4 is retained.
The lower pole owns the below-boundary render; the new stair owns the rest.
"""
import hashlib
import json
import math
from raised_stair_sections import PROFILES, surface
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MOD = 'immersive_engineer_decor_controls_tool_reforged'
ASSETS = ROOT / 'src/main/resources/assets' / MOD
SOURCE = Path(__file__).with_name('raised_stair_v_approved.json')
mesh = json.loads(SOURCE.read_text())
assert hashlib.sha256(json.dumps(mesh,separators=(',', ':')).encode()).hexdigest() == '9e4f13ebc34a935100759abc959b84bed273ceb34b2255e73e5a1b22afbf825a'

def clip(poly, axis, bound, greater):
    out=[]
    for a,b in zip(poly,poly[1:]+poly[:1]):
        ia=a[axis]>=bound-1e-10 if greater else a[axis]<=bound+1e-10
        ib=b[axis]>=bound-1e-10 if greater else b[axis]<=bound+1e-10
        if ia:out.append(a)
        if ia != ib:
            t=(bound-a[axis])/(b[axis]-a[axis]);out.append([a[i]+t*(b[i]-a[i]) for i in range(5)])
    return out

def cross(a,b):return [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
def minus(a,b):return [a[i]-b[i] for i in range(3)]
def triangles(part, section='wood', open_bottom=False):
    pole=json.loads((ASSETS/'models/block/pole'/(PROFILES[section]+'.json')).read_text())
    raw=surface(pole['elements'],open_bottom)
    for ids in mesh['triangles']:
        poly=clip([mesh['vertices'][i]+[0,0] for i in ids],1,-4,True)
        raw.extend([[poly[0][:3],poly[i][:3],poly[i+1][:3]] for i in range(1,len(poly)-1)])
    for points in raw:
        top=max(p[1] for p in points)-min(p[1] for p in points)<1e-8
        poly=[p+([p[0],p[2]] if top else [p[0]+p[2],-p[1]]) for p in points]
        if part!='full':poly=clip(poly,1,0,part=='upper')
        if len(poly)<3:continue
        for u in range(math.floor(min(p[3] for p in poly)/16),math.floor(max(p[3] for p in poly)/16)+1):
            q=clip(clip(poly,3,16*u,True),3,16*(u+1),False)
            if len(q)<3:continue
            for v in range(math.floor(min(p[4] for p in q)/16),math.floor(max(p[4] for p in q)/16)+1):
                r=clip(clip(q,4,16*v,True),4,16*(v+1),False)
                for i in range(1,len(r)-1):
                    tri=[r[0],r[i],r[i+1]]
                    normal=cross(minus(tri[1],tri[0]),minus(tri[2],tri[0]))
                    length=math.sqrt(sum(x*x for x in normal))
                    if length<1e-9:continue
                    yield tri,[x/length for x in normal],'end' if top else 'side',u,v

def rotate(p, facing):
    x,y,z=p
    return {'north':(x,y,z),'east':(16-z,y,x),'south':(16-x,y,16-z),'west':(z,y,16-x)}[facing]

def export(part,facing='north',section='wood',open_bottom=False):
    lines=['# Section-matched continuous stem; approved V at y >= -4 unchanged.','mtllib raised_stair_v.mtl','o continuous_v']
    index=0
    for tri,normal,material,u,v in triangles(part,section,open_bottom):
        for p in tri:
            x,y,z=rotate(p[:3],facing);y+=16 if part=='lower' else 0
            lines.append('v %.12g %.12g %.12g'%(x/16,y/16,z/16))
        for p in tri:lines.append('vt %.12g %.12g'%((p[3]-16*u)/16,(p[4]-16*v)/16))
        nx,ny,nz=rotate([normal[0]+8,normal[1],normal[2]+8],facing);nx-=8;nz-=8
        for _ in range(3):lines.append('vn %.12g %.12g %.12g'%(nx,ny,nz))
        lines+=['usemtl '+material,'f '+' '.join(f'{j}/{j}/{j}' for j in range(index+1,index+4))]
        index+=3
    return '\n'.join(lines)+'\n'

def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n' if not isinstance(value,str) else value)

folder=ASSETS/'models/block/raised_stair_v'
write(folder/'raised_stair_v.mtl','newmtl side\nKa 0 0 0\nKd 1 1 1\nmap_Kd #side\n\nnewmtl end\nKa 0 0 0\nKd 1 1 1\nmap_Kd #end\n')
# Keep the original material model names as the wood/thick-steel public resource
# references; thin steel has its own section. All geometry is derived, not scaled
# at bake time. The lower open variant has no coincident cap at a matching pole.
textures={'wood':{'side':'block/pole/treated_wood_pole_side_texture','end':'block/pole/treated_wood_pole_top_texture'},
          'steel':{'side':'block/pole/thick_steel_pole_side_texture','end':'block/pole/thick_steel_pole_top_texture'},
          'thin_steel':{'side':'block/pole/thin_steel_pole_side_texture','end':'block/pole/thin_steel_pole_top_texture'}}
def obj(part,material):
    tex={k:MOD+':'+v for k,v in textures[material].items()};tex['particle']=tex['side']
    return {'loader':'neoforge:obj','model':MOD+':models/block/raised_stair_v/'+part+'.obj',
            'automatic_culling':False,'shade_quads':True,'emissive_ambient':False,
            'render_type':'minecraft:cutout','textures':tex}
write(folder/'full.obj',export('full'))
for material,section in [('wood','wood'),('steel','thick_steel'),('thin_steel','thin_steel')]:
    for part in ['lower','upper']:
        for facing in ['north','east','south','west']:
            filename=part+'_'+facing if material=='wood' else part+'_'+material+'_'+facing
            write(folder/(filename+'.obj'),export(part,facing,section))
            write(folder/f'{part}_{material}_{facing}.json',obj(filename,material))
            if part=='lower':
                filename='lower_open_'+material+'_'+facing
                write(folder/(filename+'.obj'),export('lower',facing,section,True))
                write(folder/(filename+'.json'),obj(filename,material))
write(folder/'item.json',{'parent':'minecraft:block/block','loader':'neoforge:composite',
    'textures':{'particle':MOD+':block/pole/treated_wood_pole_side_texture'},
    'display':{'gui':{'rotation':[30,225,0],'translation':[0,3,0],'scale':[.38,.38,.38]}},
    'children':{'stairs':{'parent':MOD+':block/furniture/steel_catwalk_stairs_model'},'support':obj('full','wood')}})

# All mechanical states reuse the exact existing stair model resolution. The
# connection is derived from render-region neighbors, not extra BlockStates.
write(ASSETS/'blockstates/raised_steel_catwalk_stairs.json',json.loads((ASSETS/'blockstates/steel_catwalk_stairs.json').read_text()))
write(ASSETS/'models/item/raised_steel_catwalk_stairs.json',{'parent':MOD+':block/raised_stair_v/item'})
write(ROOT/'src/main/resources/data'/MOD/'loot_table/blocks/raised_steel_catwalk_stairs.json',
      {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':MOD+':raised_steel_catwalk_stairs'}],
       'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
print('Generated three section-matched stems, open/closed joins, unchanged upper V and stair resources; no recipe.')
