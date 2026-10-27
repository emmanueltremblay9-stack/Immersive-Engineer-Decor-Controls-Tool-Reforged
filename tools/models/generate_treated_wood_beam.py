"""Rigid pole-derived wood beam resources; --check verifies geometry and per-vertex UVs."""
import argparse
import copy
import itertools
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MOD = 'immersive_engineer_decor_controls_tool_reforged'
ASSETS = ROOT/'src/main/resources/assets'/MOD
SOURCE = ASSETS/'models/block/pole/straight_pole_model.json'
FACES = {
    'down': [(0,0,1),(0,0,0),(1,0,0),(1,0,1)],
    'up': [(0,1,0),(0,1,1),(1,1,1),(1,1,0)],
    'north': [(1,1,0),(1,0,0),(0,0,0),(0,1,0)],
    'south': [(0,1,1),(0,0,1),(1,0,1),(1,1,1)],
    'west': [(0,1,0),(0,0,0),(0,0,1),(0,1,1)],
    'east': [(1,1,1),(1,0,1),(1,0,0),(1,1,0)]}
NORMALS = {'down':(0,-1,0),'up':(0,1,0),'north':(0,0,-1),
           'south':(0,0,1),'west':(-1,0,0),'east':(1,0,0)}
def point(element,bits):
    return tuple(element['to'][i] if bits[i] else element['from'][i] for i in range(3))
def uv_corners(data):
    u,v,U,V=data['uv']; corners=[(u,v),(u,V),(U,V),(U,v)]; shift=data.get('rotation',0)//90
    return [corners[(i+shift)%4] for i in range(4)]
def uv_at(element,side,data,p):
    pts=[point(element,b) for b in FACES[side]]; uv=uv_corners(data)
    def weight(q):
        delta=[q[i]-pts[0][i] for i in range(3)]
        axis=next(i for i in range(3) if delta[i])
        return (p[axis]-pts[0][axis])/delta[axis]
    s,t=weight(pts[1]),weight(pts[3])
    return tuple(round(uv[0][i]+s*(uv[1][i]-uv[0][i])+t*(uv[3][i]-uv[0][i]),10) for i in range(2))
def converted(source,turn,clip=(0,16)):
    out=copy.deepcopy(source);out.pop('display',None);out['elements']=[]
    zero=turn((0,0,0))
    normal_map={}
    for side,normal in NORMALS.items():
        moved=tuple(round(turn(normal)[i]-zero[i]) for i in range(3))
        normal_map[side]=next(s for s,n in NORMALS.items() if n==moved)
    for element in source['elements']:
        assert 'rotation' not in element
        clipped=copy.deepcopy(element);clipped['from'][2]=clip[0];clipped['to'][2]=clip[1]
        corners=[turn(point(clipped,b)) for b in itertools.product((0,1),repeat=3)]
        result={'from':[min(p[i] for p in corners) for i in range(3)],
                'to':[max(p[i] for p in corners) for i in range(3)],'faces':{}}
        for side,data in element['faces'].items():
            target=normal_map[side]
            mapping={turn(point(clipped,b)):uv_at(element,side,data,point(clipped,b)) for b in FACES[side]}
            wanted=[mapping[point(result,b)] for b in FACES[target]]
            face=copy.deepcopy(data);face.pop('rotation',None)
            face['uv']=[min(p[0] for p in wanted),min(p[1] for p in wanted),max(p[0] for p in wanted),max(p[1] for p in wanted)]
            for rotation in (0,90,180,270):
                trial=dict(face,rotation=rotation)
                if uv_corners(trial)==wanted:
                    if rotation:face['rotation']=rotation
                    break
            else:raise AssertionError((side,wanted,face))
            if 'cullface' in face:face['cullface']=normal_map[face['cullface']]
            result['faces'][target]=face
        out['elements'].append(result)
    return out
def products():
    source=json.loads(SOURCE.read_text());assert len(source['elements'])==3
    transforms={
        'ceiling':(lambda p:(p[0],p[1]+5.5,p[2]),lambda p:(p[2],p[1]+5.5,16-p[0])),
        'wall_vertical':(lambda p:(p[0],16-p[2],p[1]+5.5),lambda p:(p[2],p[0],p[1]+5.5)),
        'wall_horizontal':(lambda p:(p[2],p[0],p[1]+5.5),lambda p:(p[0],16-p[2],p[1]+5.5))}
    result={};parts=[]
    prefix=MOD+':block/beam/treated_wood_'
    for mount,(core,cross) in transforms.items():
        models={'core':converted(source,core)}
        for name,clip in [('negative',(0,5.5)),('positive',(10.5,16))]:
            if mount=='wall_horizontal':clip=(16-clip[1],16-clip[0])
            models[name]=converted(source,cross,clip)
        for name,model in models.items():
            result[ASSETS/f'models/block/beam/treated_wood_{mount}_{name}.json']=model
        for facing,y in [('north',0),('east',90),('south',180),('west',270)]:
            condition={'mount':mount,'facing':facing}
            parts.append({'when':condition,'apply':{'model':prefix+mount+'_core','y':y}})
            for name in ('negative','positive'):
                parts.append({'when':dict(condition,**{'join_'+name:'true'}),'apply':{'model':prefix+mount+'_'+name,'y':y}})
    result[ASSETS/'models/block/beam/treated_wood_pole.json']=converted(source,lambda p:(p[0],16-p[2],p[1]),(5,16))
    parts.append({'when':{'mount':'ceiling','pole':'true'},'apply':{'model':prefix+'pole'}})
    result[ASSETS/'blockstates/treated_wood_beam.json']={'multipart':parts}
    result[ASSETS/'models/item/treated_wood_beam.json']={'parent':prefix+'ceiling_core'}
    result[ROOT/'src/main/resources/data'/MOD/'loot_table/blocks/treated_wood_beam.json']={
        'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':MOD+':treated_wood_beam'}],
                                       'conditions':[{'condition':'minecraft:survives_explosion'}]}]}
    return result
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
    outputs=products()
    for path,data in outputs.items():
        text=json.dumps(data,indent=2,ensure_ascii=False)+'\n'
        if args.check:assert path.read_text()==text, str(path.relative_to(ROOT))
        else:path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text)
    print(f'{len(outputs)} wood beam resources verified: exact source cuboids, rigid transforms, vertex UVs retained.')
if __name__=='__main__':main()
