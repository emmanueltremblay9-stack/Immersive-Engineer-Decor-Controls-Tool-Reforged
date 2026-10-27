"""Original steel profiles rigidly transformed into the wall plane, retaining vertex UVs."""
import argparse
import copy
import itertools
import json
from pathlib import Path
FACES={
 'down':[(0,0,1),(0,0,0),(1,0,0),(1,0,1)],
 'up':[(0,1,0),(0,1,1),(1,1,1),(1,1,0)],
 'north':[(1,1,0),(1,0,0),(0,0,0),(0,1,0)],
 'south':[(0,1,1),(0,0,1),(1,0,1),(1,1,1)],
 'west':[(0,1,0),(0,0,0),(0,0,1),(0,1,1)],
 'east':[(1,1,1),(1,0,1),(1,0,0),(1,1,0)]}
NORMALS={'north':(0,0,-1),'south':(0,0,1),'east':(1,0,0),'west':(-1,0,0),'up':(0,1,0),'down':(0,-1,0)}
def point(element,bits):return tuple(element['to'][i] if bits[i] else element['from'][i] for i in range(3))
def converted(source,turn):
    result=copy.deepcopy(source);result.pop('display',None);result['elements']=[]
    origin=turn((0,0,0));normal_map={}
    for side,normal in NORMALS.items():
        changed=tuple(turn(normal)[i]-origin[i] for i in range(3))
        normal_map[side]=next(name for name,vector in NORMALS.items() if vector==changed)
    for element in source['elements']:
        assert 'rotation' not in element
        corners=[turn(point(element,b)) for b in itertools.product((0,1),repeat=3)]
        out={'from':[min(p[i] for p in corners) for i in range(3)],'to':[max(p[i] for p in corners) for i in range(3)],'faces':{}}
        for side,data in element['faces'].items():
            target=normal_map[side];old_points=[turn(point(element,b)) for b in FACES[side]];new_points=[point(out,b) for b in FACES[target]]
            shifts={(old_points.index(p)-j+data.get('rotation',0)//90)%4 for j,p in enumerate(new_points)}
            assert len(shifts)==1
            face=copy.deepcopy(data);rotation=shifts.pop()*90
            if rotation:face['rotation']=rotation
            else:face.pop('rotation',None)
            if 'cullface' in face:face['cullface']=normal_map[face['cullface']]
            out['faces'][target]=face
        result['elements'].append(out)
    return result

ROOT=Path(__file__).resolve().parents[2]
MOD='immersive_engineer_decor_controls_tool_reforged'
ASSETS=ROOT/'src/main/resources/assets'/MOD
PREFIX=MOD+':block/hsupport/'

def products():
    base=json.loads((ASSETS/'models/block/hsupport/steel_double_t_support_model.json').read_text())
    arm=json.loads((ASSETS/'models/block/hsupport/steel_double_t_support_xconnect_model.json').read_text())
    # Both transforms preserve the original 6x5 section and 16-pixel length.
    upright=lambda p:(p[0],16-p[2],p[1])
    across=lambda p:(16-p[2],16-p[0],p[1])
    negative=lambda p:(16-p[0],p[2],p[1])
    across_negative=lambda p:(p[2],p[0],p[1])
    models={
        'vertical_steel_double_t_support_model':converted(base,upright),
        'wall_horizontal_steel_double_t_model':converted(base,across),
        'wall_vertical_double_t_negative':converted(arm,negative),
        'wall_vertical_double_t_positive':converted(arm,upright),
        'wall_horizontal_double_t_negative':converted(arm,across_negative),
        'wall_horizontal_double_t_positive':converted(arm,across)}
    result={ASSETS/f'models/block/hsupport/{name}.json':data for name,data in models.items()}
    parts=[]
    for horizontal in (False,True):
        mount='wall_horizontal' if horizontal else 'wall_vertical'
        core='wall_horizontal_steel_double_t_model' if horizontal else 'vertical_steel_double_t_support_model'
        for facing,y in [('north',0),('east',90),('south',180),('west',270)]:
            when={'wall_horizontal':str(horizontal).lower(),'facing':facing}
            parts.append({'when':when,'apply':{'model':PREFIX+core,'y':y}})
            for name in ('negative','positive'):
                parts.append({'when':dict(when,**{'join_'+name:'true'}),
                              'apply':{'model':PREFIX+mount+'_double_t_'+name,'y':y}})
    result[ASSETS/'blockstates/vertical_steel_double_t_support.json']={'multipart':parts}
    return result

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
    for path,data in products().items():
        text=json.dumps(data,indent=2,ensure_ascii=False)+'\n'
        if args.check:assert path.read_text()==text,str(path.relative_to(ROOT))
        else:path.write_text(text)
    print('Six original-profile wall models and all mount/facing/connector combinations verified.')
if __name__=='__main__':main()
