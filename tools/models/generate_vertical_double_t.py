"""Rigidly rotate the original Double-T model; preserve UVs at every vertex."""
import copy,json,itertools
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
MOD='immersive_engineer_decor_controls_tool_reforged'
ASSETS=ROOT/'src/main/resources/assets'/MOD
SOURCE=ASSETS/'models/block/hsupport/steel_double_t_support_model.json'
FACES={
 'down':[(0,0,1),(0,0,0),(1,0,0),(1,0,1)],
 'up':[(0,1,0),(0,1,1),(1,1,1),(1,1,0)],
 'north':[(1,1,0),(1,0,0),(0,0,0),(0,1,0)],
 'south':[(0,1,1),(0,0,1),(1,0,1),(1,1,1)],
 'west':[(0,1,0),(0,0,0),(0,0,1),(0,1,1)],
 'east':[(1,1,1),(1,0,1),(1,0,0),(1,1,0)]}
NORMALS={'north':'up','south':'down','up':'south','down':'north','east':'east','west':'west'}
def point(e,bits):return tuple(e['to'][i] if bits[i] else e['from'][i] for i in range(3))
def turn(p):return (p[0],16-p[2],p[1])
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2,ensure_ascii=False)+'\n')
old=json.loads(SOURCE.read_text());new=copy.deepcopy(old);new.pop('display',None)
new['elements']=[]
for element in old['elements']:
 assert 'rotation' not in element
 corners=[turn(point(element,b)) for b in itertools.product((0,1),repeat=3)]
 out={'from':[min(p[i] for p in corners) for i in range(3)],'to':[max(p[i] for p in corners) for i in range(3)],'faces':{}}
 for side,data in element['faces'].items():
  target=NORMALS[side];old_points=[turn(point(element,b)) for b in FACES[side]];new_points=[point(out,b) for b in FACES[target]]
  shifts={(old_points.index(p)-j+data.get('rotation',0)//90)%4 for j,p in enumerate(new_points)}
  assert len(shifts)==1
  face=copy.deepcopy(data);r=shifts.pop()*90
  if r:face['rotation']=r
  else:face.pop('rotation',None)
  if 'cullface' in face:face['cullface']=NORMALS[face['cullface']]
  out['faces'][target]=face
 new['elements'].append(out)
write(ASSETS/'models/block/hsupport/vertical_steel_double_t_support_model.json',new)
write(ASSETS/'blockstates/vertical_steel_double_t_support.json',{'variants':{f'facing={f}':{'model':MOD+':block/hsupport/vertical_steel_double_t_support_model','y':y} for f,y in [('north',0),('east',90),('south',180),('west',270)]}})
write(ASSETS/'models/item/vertical_steel_double_t_support.json',{'parent':MOD+':block/hsupport/vertical_steel_double_t_support_model'})
write(ROOT/'src/main/resources/data'/MOD/'loot_table/blocks/vertical_steel_double_t_support.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':MOD+':vertical_steel_double_t_support'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
print('Four source cuboids rotated rigidly; textures and per-vertex UV retained; bounds [5,0,11]..[11,16,16].')
