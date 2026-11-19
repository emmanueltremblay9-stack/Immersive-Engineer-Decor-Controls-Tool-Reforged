"""Supplementary R04 boundary proof gates; no gameplay/artistic acceptance."""
from factory_control_tooltip_native import require
from factory_placer_v2_reference_native import png
POINTS = {
 'hopper': [(2,2),(175,2),(2,164),(174,164),(2,83),(174,83),(88,64),(88,153)],
 'dropper': [(2,2),(226,2),(2,248),(226,248),(2,125),(226,125),(114,116),(114,243)],
}

def validate_interaction(proof, game, width, height, machine):
    p=proof.get('interaction',{})
    require(p.get('input_method')=='XTest','Interaction must use native XTest')
    require(p.get('fixture_item')=='minecraft:stone' and type(p.get('initial_total')) is int and p['initial_total']==64,'Wrong fixture')
    require(p.get('gui_scale')==1,'Unqualified interaction scale')
    panel=(176,166) if machine=='hopper' else (228,250)
    require(p.get('panel_width')==panel[0] and p.get('panel_height')==panel[1]
            and p.get('panel_left')==(width-panel[0])//2 and p.get('panel_top')==(height-panel[1])//2,'Wrong panel geometry')
    require(p.get('entity_mutation') is False,'Witness entity was modified')
    pickup=p.get('pickup',{})
    require(pickup.get('server_ack') is True and pickup.get('pointer_ack') is True and pickup.get('xtest_press_release_ack') is True and pickup.get('button')==1,'Missing real pickup')
    pre,post=pickup.get('before',{}),pickup.get('after',{})
    require(pre.get('player_inventory')==[64 if i==9 else 0 for i in range(41)],'Wrong fixture inventory')
    for index,record in enumerate([pickup]+p.get('cases',[])):
        require(record.get('mc_input_action_index')==index-1 and record.get('mc_button')==(0 if record.get('button')==1 else 1),'Mouse callback action/button differs')
        require(record.get('consumed_press_count')==1 and record.get('consumed_release_count')==1,'Unobserved or duplicate Minecraft mouse event')
        require(all(type(record.get(k)) is int and record[k]>=3 for k in ('render_frames_before','render_frames_after')),'Capture before synchronized render')
    require([pre.get(k) for k in ('carried','slots','drops','total')]==[0,64,0,64]
            and [post.get(k) for k in ('carried','slots','drops','total')]==[64,0,0,64],'Wrong pickup transition')
    require(pickup.get('file')=='factory-interaction-pickup.png' and pickup.get('before_file')=='factory-interaction-before-pickup.png','Wrong pickup capture')
    for key,digest in [('file','sha256'),('before_file','before_sha256')]:png(game/pickup[key],width,height,pickup.get(digest))
    cases=p.get('cases',[])
    require(len(cases)==17,'Incomplete boundary coverage')
    # Exact expected sequence is independent of the supplied evidence.
    expected=[(x,y,b) for x,y in POINTS[machine] for b in (1,3)]
    expected.append((-8,83 if machine=='hopper' else 125,3))
    slots_count=54 if machine=='hopper' else 51
    config_count=4 if machine=='hopper' else 7
    previous=post
    for i,(c,(x,y,button)) in enumerate(zip(cases,expected)):
        require(all(type(c.get(k)) is int for k in ('x','y','button')) and (c['x'],c['y'],c['button'])==(x,y,button),'Click coordinate/button differs')
        require(c.get('server_ack') is True and c.get('pointer_ack') is True and c.get('xtest_press_release_ack') is True,'Missing input/server observation')
        require(type(c.get('settle_ticks')) is int and c['settle_ticks']>=12,'Insufficient post-click observation')
        require(c.get('pointer_x')==p['panel_left']+x and c.get('pointer_y')==p['panel_top']+y,'Pointer not at target')
        before,after,client=c.get('before',{}),c.get('after',{}),c.get('client_after',{})
        for snap in (before,after):
            require(all(type(snap.get(k)) is int and snap[k]>=0 for k in ('carried','slots','drops','total')),'Invalid observed conservation counts')
            require(snap['total']==64==snap['carried']+snap['slots']+snap['drops'],'Lost/duplicated fixture items')
            require(type(snap.get('fields')) is list and len(snap['fields'])==16 and all(type(v) is int for v in snap['fields']),'Missing fields')
        for snap in (before,after,client):
            require(type(snap.get('slot_counts')) is list and len(snap['slot_counts'])==slots_count
                    and all(type(v) is int and v>=0 for v in snap['slot_counts']) and sum(snap['slot_counts'])==snap.get('slots'),'Missing per-slot counts')
            require(type(snap.get('player_inventory')) is list and len(snap['player_inventory'])==41
                    and all(type(v) is int and v==0 for v in snap['player_inventory']),'Player inventory not empty after pickup')
            require(type(snap.get('container_id')) is int and snap['container_id']==post.get('container_id'),'Menu identity changed')
        require(before.get('entity_count')==0 and after.get('entity_count')==(1 if i==16 else 0),'Wrong dropped entity count')
        require(before['carried']==64 and before['slots']==0 and before['drops']==0,'Invalid pre-click state')
        require(after['carried']==(63 if i==16 else 64) and after['slots']==0 and after['drops']==(1 if i==16 else 0),'Wrong interior/outside behavior')
        require(before['fields'][:config_count]==after['fields'][:config_count],'Unexpected configuration action')
        require(client.get('carried')==after['carried'] and client.get('slots')==after['slots'] and client.get('fields',[])[:config_count]==after['fields'][:config_count],'Client/server mismatch')
        if previous is not None:
            require(all(previous[k]==before[k] for k in ('carried','slots','drops','total')) and previous['fields'][:config_count]==before['fields'][:config_count],'Discontinuous sequence')
        previous=after
        require(c.get('file')=='factory-interaction-'+str(i)+'.png','Unexpected capture path')
        require(c.get('before_file')=='factory-interaction-before-'+str(i)+'.png','Wrong before capture')
        png(game/c['before_file'],width,height,c.get('before_sha256'))
        png(game/c['file'],width,height,c.get('sha256'))
