"""Fail-closed qualification of generic fluid packets and underwater support variants."""
import json
from catalogue_visual import valid_png
from client_mixin_smoke import SmokeFailure
FLUIDS=('minecraft:water','minecraft:lava','immersiveengineering:creosote','immersiveengineering:ethanol','immersiveengineering:biodiesel','immersiveengineering:plantoil')
MODES=('dry','source-submerged','flow-submerged')
EXTERNAL_BLOCKS = tuple('minecraft:'+name for name in (
    'oak_stairs','stone_slab','oak_fence','cobblestone_wall','glass_pane','red_stained_glass_pane',
    'iron_bars','ladder','oak_trapdoor','cut_copper_stairs','cut_copper_slab','copper_trapdoor')) + tuple(
    'immersiveengineering:'+name for name in ('stairs_treated_wood_horizontal','stairs_treated_wood_vertical',
    'stairs_treated_wood_packaged','slab_treated_wood_horizontal','slab_treated_wood_vertical','slab_treated_wood_packaged',
    'wall_clinker_brick','treated_trapdoor','treated_trapdoor_framed','steel_trapdoor'))
def rows(proof,nonce,suite,expected,key):
    if proof.get('suite')!=suite or proof.get('nonce')!=nonce or proof.get('status')!='passed':
        raise SmokeFailure('Missing current '+suite+' proof')
    cases=proof.get('cases')
    if not isinstance(cases,list) or len(cases)!=len(expected):raise SmokeFailure('Incomplete '+suite+' matrix')
    seen=set()
    for case in cases:
        if not isinstance(case,dict):raise SmokeFailure('Invalid '+suite+' row')
        identity=key(case)
        if identity not in expected or identity in seen:raise SmokeFailure('Unexpected/duplicate '+suite+' row')
        seen.add(identity)
    return cases

def validate_generic(proof,nonce):
    expected={(fluid,kind,sync) for fluid in FLUIDS for kind in range(5) for sync in ('update','initial')}
    for c in rows(proof,nonce,'generic-fluid-client-v1',expected,lambda c:(c.get('fluid'),c.get('state'),c.get('sync'))):
        if type(c.get('state')) is not int or c.get('server_client_region_agree') is not True or c.get('drain_cleared') is not True or c.get('local_prediction_restore') is not True:
            raise SmokeFailure('Generic fluid state/snapshot/drain mismatch')
    external=proof.get('external')
    if not isinstance(external,dict):raise SmokeFailure('Missing external fluid qualification')
    expected_external={(block,kind,sync) for block in EXTERNAL_BLOCKS for kind in range(6) for sync in ('update','initial')}
    for c in rows(external,nonce,'external-fluid-client-v1',expected_external,lambda c:(c.get('block'),c.get('state'),c.get('sync'))):
        if type(c.get('state')) is not int or any(c.get(field) is not True for field in (
                'server_client_region_agree','drain_cleared','local_prediction_restore','textured_mesh','light_cleared')):
            raise SmokeFailure('External fluid state/mesh/light mismatch')
    return proof

def validate_submerged(proof,nonce,directory):
    expected={(variant,mode) for variant in range(6) for mode in MODES}
    for c in rows(proof,nonce,'submerged-catwalk-v1',expected,lambda c:(c.get('variant'),c.get('mode'))):
        variant,mode=c['variant'],c['mode'];wet=mode!='dry';source=mode=='source-submerged'
        if (type(variant) is not int or c.get('server_client_agree') is not True or c.get('paid_state_preserved') is not True
            or c.get('immersed_camera') is not wet or c.get('source') is not source
            or type(c.get('amount')) is not int or (c['amount']!=0 if not wet else not 1<=c['amount']<=8)):
            raise SmokeFailure('Submerged camera/fluid/support state mismatch')
        name=f'variant-{variant}-{mode}.png'
        if c.get('screenshot')!=name or not valid_png((directory/'submerged-catwalk-screenshots'/name).read_bytes()):
            raise SmokeFailure('Missing current full-size submerged screenshot')
    return proof

def validate_extensions(directory,nonce):
    generic=json.loads((directory/'client-generic-fluid-result.json').read_text())
    submerged=json.loads((directory/'client-submerged-catwalk-result.json').read_text())
    validate_generic(generic,nonce);validate_submerged(submerged,nonce,directory)
    return generic,submerged
