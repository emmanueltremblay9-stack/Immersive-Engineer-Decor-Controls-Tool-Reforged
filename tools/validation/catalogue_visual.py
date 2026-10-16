"""Fail-closed gates for dedicated native catalogue screenshots and server/client state."""
import struct
import zlib
from client_mixin_smoke import SmokeFailure
BLOCKS = ('iron_bulb_light','iron_inset_light','iron_ceiling_edge_light','iron_floor_edge_light',
          'industrial_lever','industrial_button','industrial_contact_mat','steel_catwalk_ta')
MODES = ('water','lava','creosote','natural-water')
def validate_catalogue(proof, nonce, directory):
    expected={('immersive_engineer_decor_controls_tool_reforged:'+b,m) for b in BLOCKS for m in MODES}
    if (proof.get('suite')!='native-catalogue-visual-v1' or proof.get('nonce')!=nonce or proof.get('status')!='passed'):
        raise SmokeFailure('Missing current native catalogue proof')
    cases=proof.get('cases');seen=set()
    if not isinstance(cases,list) or len(cases)!=len(expected):raise SmokeFailure('Incomplete catalogue matrix')
    for case in cases:
        identity=(case.get('block'),case.get('mode'))
        if identity not in expected or identity in seen:raise SmokeFailure('Duplicate/unexpected catalogue case')
        seen.add(identity);block,mode=identity;block=block.split(':',1)[1];natural=mode=='natural-water'
        if (case.get('source_before') is not (not natural) or case.get('amount_before')!=(7 if natural else 8)
            or case.get('server_client_agree') is not True or case.get('after_empty') is not True
            or case.get('geometry_and_paid_state_preserved') is not True
            or type(case.get('fluid_vertices')) is not int or case['fluid_vertices']<=0 or case['fluid_vertices']%4):
            raise SmokeFailure('Catalogue fluid, geometry or recession mismatch')
        for phase in ('before','after'):
            name=f'{block}-{mode}-{phase}.png'
            if case.get(phase)!=name:raise SmokeFailure('Unexpected catalogue screenshot path')
            data=(directory/'catalogue-screenshots'/name).read_bytes()
            if not valid_png(data):
                raise SmokeFailure('Missing/full-size catalogue screenshot')
    return proof


def valid_png(data):
    if len(data)>16_000_000 or data[:8]!=b'\x89PNG\r\n\x1a\n':return False
    offset=8;chunks=[];compressed=bytearray();dimensions=None
    while offset+12<=len(data):
        size=struct.unpack('>I',data[offset:offset+4])[0]
        if size>len(data)-offset-12:return False
        kind=data[offset+4:offset+8];payload=data[offset+8:offset+8+size]
        crc=struct.unpack('>I',data[offset+8+size:offset+12+size])[0]
        if zlib.crc32(kind+payload)!=crc:return False
        chunks.append(kind)
        if kind==b'IHDR':
            if size!=13:return False
            width,height,bits,color,compression,filtering,interlace=struct.unpack('>IIBBBBB',payload)
            if (width,height,bits,compression,filtering,interlace)!=(1280,720,8,0,0,0) or color not in (2,6):return False
            dimensions=(width,height,3 if color==2 else 4)
        elif kind==b'IDAT':compressed.extend(payload)
        offset+=12+size
        if kind==b'IEND':break
    if not dimensions or not chunks or chunks[0]!=b'IHDR' or chunks[-1]!=b'IEND' or offset!=len(data):return False
    try:
        width,height,channels=dimensions;expected=height*(1+width*channels)
        decoded=zlib.decompressobj();pixels=decoded.decompress(compressed,expected+1)
        return len(pixels)==expected and decoded.eof and not decoded.unused_data
    except zlib.error:return False
