"""Exact V5 exterior-alpha transition; standard-library verification only."""
from collections import deque
import subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
PARENT='cec601e71369e87fa39b1ed7498ad628480a0a80'
ASSET='src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/factory_steel_v2/dropper.png'
def silhouette(image):
    w,h,p=image
    assert (w,h)==(1368,1500)
    support=[]
    for y in range(h):
        xs=[x for x in range(w) if max(p[(y*w+x)*4:(y*w+x)*4+3])>16]
        support.append((min(xs),max(xs)) if xs else None)
    rows=[]
    for y in range(h):
        near=[r for r in support[max(0,y-8):min(h,y+9)] if r]
        rows.append((max(0,min(r[0] for r in near)-8),min(w-1,max(r[1] for r in near)+8)) if near else None)
    return rows
def verify(old,new):
    assert old[:2]==new[:2]==(1368,1500),'Dimensions changed'
    w,h,a=old;b=new[2]
    assert len(a)==len(b)==w*h*4
    assert all(a[k::4]==b[k::4] for k in (0,1,2)),'RGB changed'
    changed=0
    for y,row in enumerate(silhouette(old)):
        for x in range(w):
            i=(y*w+x)*4
            inside=row is not None and row[0]<=x<=row[1]
            assert b[i+3]==(a[i+3] if inside else 0),'Alpha outside exact contract'
            if not inside:
                assert max(a[i:i+3])<=16,'Visible rim removed'
                changed+=a[i+3]!=0
    assert changed==133796,'Silhouette extent changed'

