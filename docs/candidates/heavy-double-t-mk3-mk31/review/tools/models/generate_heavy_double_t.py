#!/usr/bin/env python3
"""Generate the single-block, 24-orientation MK3 + MK3.1 visual prototype.

Uses the measured 12-wide I profile from the user's 123-unit .bbs.json design,
normalized to one 16-unit Minecraft cell. Generated models are candidate assets:
no UV-artist approval or Minecraft render validation is implied.
"""
from __future__ import annotations
import json
from pathlib import Path
import argparse

MOD = "immersive_engineer_decor_controls_tool_reforged"
FACES = {
    "down": (0, -1, 0), "up": (0, 1, 0),
    "north": (0, 0, -1), "south": (0, 0, 1),
    "west": (-1, 0, 0), "east": (1, 0, 0),
}
REVERSE = {v:k for k,v in FACES.items()}
OPPOSITE = {"down":"up","up":"down","north":"south","south":"north","west":"east","east":"west"}
STEEL = f"{MOD}:block/hsupport/steel_double_t_support_side_texture"
END = f"{MOD}:block/hsupport/steel_double_t_support_end_texture"
TOP = f"{MOD}:block/hsupport/steel_double_t_support_top_texture"


def cross(a, b):
    return (a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0])


def basis(facing, roll):
    axis = FACES[facing]
    base = FACES["east"] if facing in ("up","down","north","south") else FACES["south" if facing=="east" else "north"]
    depth = cross(base, axis)
    right = (base,depth,tuple(-n for n in base),tuple(-n for n in depth))[roll]
    return right,axis,cross(right,axis)


def xf(v, facing, roll):
    bs=basis(facing,roll)
    offset=[v[i]-8 for i in range(3)]
    return [round(8+sum(bs[j][i]*offset[j] for j in range(3)),6) for i in range(3)]


def turn_dir(name, facing, roll):
    b=basis(facing,roll)
    n=FACES[name]
    return REVERSE[tuple(sum(b[j][i]*n[j] for j in range(3)) for i in range(3))]


def transform_box(box, facing, roll):
    mn,mx=box
    a,b=xf(mn,facing,roll),xf(mx,facing,roll)
    return [min(a[i],b[i]) for i in range(3)],[max(a[i],b[i]) for i in range(3)]


def element(box, facing, roll, *, kind="side", continuous=False, incline=None):
    a,b=transform_box(box,facing,roll)
    obj={"from":a,"to":b,"faces":{}}
    for face in FACES:
        if continuous and face in ("up","down"):
            continue
        obj["faces"][turn_dir(face,facing,roll)]={"uv":[0,0,16,16],"texture":"#"+("end" if kind=="end" else "side")}
    if incline is not None:
        local_origin, local_axis, angle = incline
        axis_world=turn_dir(local_axis,facing,roll)
        axis_vector=FACES[axis_world]
        sign=next(x for x in axis_vector if x)
        obj["rotation"]={"origin":xf(local_origin,facing,roll),"axis":"xyz"[[i for i,x in enumerate(axis_vector) if x][0]],"angle":angle*sign,"rescale":False}
    return obj


def model(elements):
    return {"parent":"block/block","textures":{"side":STEEL,"end":END,"top":TOP,"particle":STEEL},"elements":elements}


def core(facing,roll):
    boxes=[
        ((2,0,2),(14,16,4)),
        ((7,0,4),(9,16,12)),
        ((2,0,12),(14,16,14)),
    ]
    items=[element(box,facing,roll,continuous=True) for box in boxes]
    # Steel seat/cantilever and a welded diagonal gusset. The diagonal is a
    # 45-degree element rotation, not a texture pretending to be structural.
    items.append(element(((0,11.5,5.5),(8.2,14,10.5)),facing,roll))
    items.append(element(((2,6,6.5),(11,8.25,9.5)),facing,roll,
                         incline=((7,8,8),"south",-45)))
    items.append(element(((7,3,6),(10,6,10)),facing,roll))
    return model(items)


def anchor(face):
    facing=OPPOSITE[face]
    boxes=[((1,0,1),(15,1.15,15))]
    for x in (2,12.2):
        for z in (2,12.2):
            boxes.append(((x,1.15,z),(x+1.8,2.05,z+1.8)))
    # Three inset ribs mimic welded gusset plate contact, without projecting
    # beyond the contacted face or creating a separate lootable fitting.
    boxes.extend([((6.75,1.15,3),(9.25,2.1,6)),((6.75,1.15,10),(9.25,2.1,13))])
    return model([element(b,facing,0,kind="end") for b in boxes])


def junction(face):
    facing=OPPOSITE[face]
    boxes=[((3,0,3),(13,1,13)),((6.5,1,3),(9.5,2.5,13)),((3,1,6.5),(13,2.5,9.5))]
    return model([element(b,facing,0) for b in boxes])


def endcap(facing,roll,positive):
    y0,y1=(15.65,16) if positive else (0,.35)
    boxes=[((2,y0,2),(14,y1,4)),((7,y0,4),(9,y1,12)),((2,y0,12),(14,y1,14))]
    return model([element(b,facing,roll,kind="end") for b in boxes])


def data_files():
    variants={}
    for facing in FACES:
        for roll in range(4):
            variants[f"facing={facing},roll={roll}"]={"model":f"{MOD}:block/heavy_double_t/core_{facing}_{roll}"}
    yield f"assets/{MOD}/blockstates/heavy_industrial_double_t_support.json",{"variants":variants}
    for facing in FACES:
        for roll in range(4):
            yield f"assets/{MOD}/models/block/heavy_double_t/core_{facing}_{roll}.json",core(facing,roll)
            for part,is_positive in (("positive",True),("negative",False)):
                yield f"assets/{MOD}/models/block/heavy_double_t/end_{facing}_{roll}_{part}.json",endcap(facing,roll,is_positive)
        yield f"assets/{MOD}/models/block/heavy_double_t/anchor_{facing}.json",anchor(facing)
        yield f"assets/{MOD}/models/block/heavy_double_t/junction_{facing}.json",junction(facing)
    yield f"assets/{MOD}/models/item/heavy_industrial_double_t_support.json",{
        "parent":f"{MOD}:block/heavy_double_t/core_up_0"
    }
    yield f"data/{MOD}/loot_table/blocks/heavy_industrial_double_t_support.json",{
        "type":"minecraft:block",
        "pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":f"{MOD}:heavy_industrial_double_t_support"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]
    }


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args=parser.parse_args()
    root=Path(__file__).resolve().parents[2] / "src/main/resources"
    n=0
    for name,content in data_files():
        dest=root/name
        text=json.dumps(content,indent=2,ensure_ascii=False)+"\n"
        if args.check:
            if not dest.exists() or dest.read_text("utf-8")!=text:
                raise SystemExit("DIFF/ABSENT: "+str(dest))
        else:
            dest.parent.mkdir(parents=True,exist_ok=True)
            dest.write_text(text,encoding="utf-8",newline="\n")
        n+=1
    print(f"{'CHECKED' if args.check else 'GENERATED'} {n} JSON assets; 24 facings/rolls, 6 anchors, 6 junctions, 48 endcaps")

if __name__=="__main__":
    main()
