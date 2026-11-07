"""Synthetic pixels test validator rejection, never claim native execution."""
import copy
import hashlib
from pathlib import Path
import math
import struct
import tempfile
import unittest
import zlib
from factory_placer_v2_reference_native import *


def png_rows(w,h,rows):
    def chunk(name,data):return struct.pack('>I',len(data))+name+data+struct.pack('>I',zlib.crc32(name+data))
    return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(b'\0'+r for r in rows)))+chunk(b'IEND',b'')


class NativePlacerProofTest(unittest.TestCase):
    CACHE={}
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup)
        self.game=Path(self.tmp.name)/'run';self.folder=self.game/'factory-placer-v2-reference-screenshots';self.folder.mkdir(parents=True)
        self.nonce='a'*32;self.sha='b'*40;self.logs=f'IEDCTR_FACTORY_PLACER_V2_REFERENCE PASSED nonce={self.nonce}'
        self.proof=dict(suite=SUITE,nonce=self.nonce,executed_source_sha=self.sha,status='passed_bounded_smoke',source_art_sha256=ART_SHA,
            actual_integrated_world=True,native_visual_acceptance=False,selected_language='en_us',menu_slot_count=54,
            machine_saved_slot_count=27,machine_visible_slot_count=18,slot_routes=list(range(54)),cases=sorted(CASES),
            route_pickup_server_acks=54,route_return_server_acks=54,initial_item_total=46,final_item_total=46,
            hidden_slot_sentinels_before=copy.deepcopy(SENTINELS),hidden_slot_sentinels_after=copy.deepcopy(SENTINELS),
            placement=copy.deepcopy(PLACEMENT),control_routes=copy.deepcopy(CONTROL_ROUTES),manual_trigger_server_acks=3)
        self.set_captures(1280,720,1)

    def set_captures(self,width,height,scale):
        self.width,self.height,self.scale=width,height,scale
        key=width,height,scale
        if key in self.CACHE:
            metadata,files=self.CACHE[key];self.proof.update(copy.deepcopy(metadata))
            for name,data in files.items():(self.folder/name).write_bytes(data)
            return
        viewport,view_scale,(tx,ty,tw,th)=expected_geometry(width,height,scale)
        self.proof.update(viewport);self.proof['captures']=[];sx,sy=transform(width,height,scale)
        # Synthetic nearest-neighbor rendering of the verified retained artwork.
        # This is validator test data, not evidence of a native game run.
        sw,sh,art=load_source()
        background=bytearray(bytes((30,30,30,255))*width*height)
        factor=view_scale*f32(1/3)*scale
        x0,x1=math.ceil(sx(0)),math.floor(sx(f32(sw*f32(1/3))))
        source_x=[min(sw-1,max(0,math.floor((x+.5-sx(0))/factor))) for x in range(x0,x1)]
        for y in range(math.ceil(sy(0)),math.floor(sy(f32(sh*f32(1/3))))):
            yy=min(sh-1,max(0,math.floor((y+.5-sy(0))/factor)))
            background[(y*width+x0)*4:(y*width+x1)*4]=b''.join(art[(yy*sw+x)*4:(yy*sw+x+1)*4] for x in source_x)
        for name,(logic,selected) in CAPTURES.items():
            pixels=bytearray(background)
            def paint(rect,color):
                l,t,r,b=rect
                for y in range(math.floor(sy(t)),math.ceil(sy(b))):
                    for x in range(math.floor(sx(l)),math.ceil(sx(r))):pixels[(y*width+x)*4:(y*width+x+1)*4]=color
            x,y=SLOT_ORIGINS[selected]
            # Rasterize exact border coverage at pixel centers, not expanded edges.
            for l,t,r,b in selection_rects(width,height,scale,selected):
                for py in range(math.ceil(t-.5),math.ceil(b-.5)):
                    for px in range(math.ceil(l-.5),math.ceil(r-.5)):
                        pixels[(py*width+px)*4:(py*width+px+1)*4]=ROW_ACTIVE
            paint(INDICATORS[3],bytes((104,25,21,255)))
            paint(INDICATORS[selected//6],ROW_ACTIVE)
            for i,x in enumerate((282,293,304)):paint((x,171,x+6,173),ACTIVE if i==(2 if logic&4 else logic&1) else INACTIVE)
            for i,x in enumerate((334,350)):paint((x,171,x+10,173),ACTIVE if i==bool(logic&2) else INACTIVE)
            data=png_rows(width,height,[pixels[y*width*4:(y+1)*width*4] for y in range(height)])
            cn=name[:-4]+'-controls.png';crop=png_rows(tw,th,[pixels[(y*width+tx)*4:(y*width+tx+tw)*4] for y in range(ty,ty+th)])
            (self.folder/name).write_bytes(data);(self.folder/cn).write_bytes(crop)
            self.proof['captures'].append(dict(name=name,sha256=hashlib.sha256(data).hexdigest(),**viewport,
                **expected_presentation_bounds(width,height,scale),width=width,height=height,view_scale=view_scale,
                menu_id=2 if name in ('reopened.png','reloaded.png') else 1,screen_class=SCREEN_CLASS,logic=logic,selected_slot=selected,signal=0,
                controls_name=cn,controls_x=tx,controls_y=ty,controls_width=tw,controls_height=th,controls_sha256=hashlib.sha256(crop).hexdigest(),
                frame_exterior_sha256=image_evidence(self.folder/name,self.folder/cn,width,height,scale)))
        world=png_rows(width,height,[bytes((10,10,10,255))*width]*height);(self.folder/'placement-world.png').write_bytes(world)
        self.proof['world_capture']=dict(name='placement-world.png',width=width,height=height,sha256=hashlib.sha256(world).hexdigest(),
            screen_class='none',target_block='minecraft:stone',target=[0,80,1],server_confirmed=True,**viewport)
        self.CACHE[key]=({k:copy.deepcopy(self.proof[k]) for k in (*viewport,'captures','world_capture')},{p.name:p.read_bytes() for p in self.folder.glob('*.png')})

    def validate(self,p=None,**kwargs):
        opts=dict(logs=self.logs,exit_code=0,timed_out=False,width=self.width,height=self.height,language=self.proof['selected_language'],gui_scale=self.scale);opts.update(kwargs)
        return validate_proof(self.proof if p is None else p,self.nonce,self.sha,self.game,**opts)

    def test_five_profiles(self):
        for w,h,s,lang in [(1280,720,1,'en_us'),(1280,260,1,'fr_fr'),(1920,1080,1,'en_us'),(1920,1080,2,'en_us'),(1920,1080,3,'en_us')]:
            with self.subTest(w=w,h=h,s=s):self.set_captures(w,h,s);self.proof['selected_language']=lang;self.validate()

    def test_identity_and_scope(self):
        for k,v in [('suite','wrong'),('nonce','stale'),('executed_source_sha','0'*40),('source_art_sha256','0'*64),('actual_integrated_world',1),('native_visual_acceptance',True),('selected_language','de_de'),('status','passed'),('menu_slot_count',True),('machine_visible_slot_count',27),('machine_saved_slot_count',18)]:
            p=copy.deepcopy(self.proof);p[k]=v
            with self.subTest(key=k),self.assertRaises(SmokeFailure):self.validate(p)

    def test_conservation_and_ack_fields(self):
        for k in ['route_pickup_server_acks','route_return_server_acks','initial_item_total','final_item_total','manual_trigger_server_acks']:
            for value in [0,True,46.0,None]:
                p=copy.deepcopy(self.proof);p[k]=value
                with self.subTest(key=k,value=value),self.assertRaises(SmokeFailure):self.validate(p)
        for k in ['hidden_slot_sentinels_before','hidden_slot_sentinels_after','placement','control_routes']:
            p=copy.deepcopy(self.proof);p[k]=[]
            with self.subTest(key=k),self.assertRaises(SmokeFailure):self.validate(p)
        for k in PLACEMENT:
            p=copy.deepcopy(self.proof);p['placement'][k]=None
            with self.subTest(key=k),self.assertRaises(SmokeFailure):self.validate(p)

    def test_routes_cases_and_missing_capture(self):
        for k,v in [('slot_routes',list(range(53))),('slot_routes',[False]+list(range(1,54))),('slot_routes',list(range(53))+[52]),('cases',[]),('cases',sorted(CASES)+[sorted(CASES)[0]]),('captures',[])]:
            p=copy.deepcopy(self.proof);p[k]=v
            with self.subTest(key=k),self.assertRaises(SmokeFailure):self.validate(p)

    def test_capture_metadata(self):
        for k,v in [('name','../escape.png'),('logic',True),('signal',True),('signal',1),('selected_slot',6),('menu_id',0),('view_scale',float('nan')),('screen_class','other'),('controls_name','other.png'),('controls_x',0),('frame_exterior_sha256','0'*64)]:
            p=copy.deepcopy(self.proof);p['captures'][0][k]=v
            with self.subTest(key=k),self.assertRaises(SmokeFailure):self.validate(p)

    def test_all_viewport_fields_fail_closed(self):
        viewport,_,_=expected_geometry(self.width,self.height,self.scale)
        for target in ['root','capture','world']:
            for k in viewport:
                p=copy.deepcopy(self.proof);d=p if target=='root' else p['captures'][0] if target=='capture' else p['world_capture'];d[k]=True
                with self.subTest(target=target,key=k),self.assertRaises(SmokeFailure):self.validate(p)

    def test_public_presentation_bounds(self):
        for k in expected_presentation_bounds(self.width,self.height,self.scale):
            p=copy.deepcopy(self.proof);p['captures'][0][k]+=1
            with self.subTest(key=k),self.assertRaises(SmokeFailure):self.validate(p)

    def test_same_menu_reopen_rejected(self):
        p=copy.deepcopy(self.proof);p['captures'][-2]['menu_id']=1
        with self.assertRaises(SmokeFailure):self.validate(p)

    def test_png_crc_and_hash(self):
        p=copy.deepcopy(self.proof);c=p['captures'][0];path=self.folder/c['name'];data=bytearray(path.read_bytes());data[-1]^=1;path.write_bytes(data);c['sha256']=hashlib.sha256(data).hexdigest()
        with self.assertRaises(SmokeFailure):self.validate(p)

    def test_crop_detached_from_full_image(self):
        p=copy.deepcopy(self.proof);c=p['captures'][0];w,h=c['controls_width'],c['controls_height'];data=png_rows(w,h,[bytes((99,0,0,255))*w]*h)
        (self.folder/c['controls_name']).write_bytes(data);c['controls_sha256']=hashlib.sha256(data).hexdigest()
        with self.assertRaisesRegex(SmokeFailure,'not the native screenshot'):self.validate(p)

    def test_self_consistent_wrong_control_pixels(self):
        p=copy.deepcopy(self.proof);base=p['captures'][0];c=p['captures'][3]
        for key in ['sha256','controls_sha256','frame_exterior_sha256']:c[key]=base[key]
        (self.folder/c['name']).write_bytes((self.folder/base['name']).read_bytes());(self.folder/c['controls_name']).write_bytes((self.folder/base['controls_name']).read_bytes())
        with self.assertRaisesRegex(SmokeFailure,'Inversion pixels'):self.validate(p)

    def test_exterior_mutation_with_recomputed_hash(self):
        p=copy.deepcopy(self.proof);c=p['captures'][0];path=self.folder/c['name'];w,h,px=decode_png(path);pixels=bytearray(px);sx,sy=transform(w,h,self.scale);x,y=math.ceil(sx(10)),math.ceil(sy(10));pixels[(y*w+x)*4]^=1
        data=png_rows(w,h,[pixels[y*w*4:(y+1)*w*4] for y in range(h)]);path.write_bytes(data);c['sha256']=hashlib.sha256(data).hexdigest();c['frame_exterior_sha256']=image_evidence(path,self.folder/c['controls_name'],w,h,self.scale)
        with self.assertRaises(SmokeFailure):self.validate(p)

    def test_missing_and_symlink_image(self):
        path=self.folder/'normal.png';target=self.folder/'actual.png';path.rename(target);path.symlink_to(target)
        with self.assertRaises(SmokeFailure):self.validate()
        path.unlink()
        with self.assertRaises(SmokeFailure):self.validate()

    def test_invalid_requested_viewports(self):
        for w,h,s in [(319,240,1),(1280,260,2),(1920,1080,True),(1920,1080,4),(1280.0,720,1)]:
            with self.assertRaises(SmokeFailure):expected_geometry(w,h,s)

    def test_unclean_finish_and_fixture_diagnostic(self):
        for kwargs in [dict(exit_code=1),dict(timed_out=True),dict(logs='no marker'),dict(logs='FATAL java.lang.IllegalStateException')]:
            with self.assertRaises(SmokeFailure):self.validate(**kwargs)
        p=copy.deepcopy(self.proof);p.update(status='failed',error='Specific failure\n details')
        with self.assertRaisesRegex(SmokeFailure,'reported failure: Specific failure details'):self.validate(p)
        p['status']='passed_bounded_smoke'
        with self.assertRaises(SmokeFailure):self.validate(p)

    def test_crash_report(self):
        d=self.game/'crash-reports';d.mkdir();(d/'crash.txt').write_text('crash')
        with self.assertRaises(SmokeFailure):self.validate()

    def test_world_capture_required(self):
        for key in ['world_capture']:
            p=copy.deepcopy(self.proof);del p[key]
            with self.assertRaises(SmokeFailure):self.validate(p)

    def test_non_object_and_passing_error(self):
        for value in ([],False,'passed'):
            with self.assertRaises(SmokeFailure):self.validate(value)
        p=copy.deepcopy(self.proof);p['error']=None
        with self.assertRaises(SmokeFailure):self.validate(p)

    def test_complete_but_forged_control_route(self):
        for key,value in [('server_confirmed',1),('logic',3),('selected_slot',1),('control','MANUAL_TRIGGER')]:
            p=copy.deepcopy(self.proof);p['control_routes'][0][key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.validate(p)

    def test_world_metadata_cannot_substitute_for_png(self):
        (self.folder/'placement-world.png').unlink()
        with self.assertRaises(SmokeFailure):self.validate()

    def test_invalid_png_structure(self):
        path=self.folder/'bad.png'
        for data in (b'not png',b'\x89PNG\r\n\x1a\n',png_rows(1,1,[bytes((0,0,0,255))])+b'extra'):
            path.write_bytes(data)
            with self.assertRaises(SmokeFailure):decode_png(path)

    def test_consistently_wrong_artwork_rejected_with_recomputed_evidence(self):
        p=copy.deepcopy(self.proof)
        for c in p['captures']:
            path=self.folder/c['name'];w,h,pixels=decode_png(path);pixels=bytearray(pixels)
            for x,y,_,_ in source_samples(w,h,self.scale):pixels[(y*w+x)*4:(y*w+x+1)*4]=bytes((30,30,30,255))
            data=png_rows(w,h,[pixels[y*w*4:(y+1)*w*4] for y in range(h)]);path.write_bytes(data);c['sha256']=hashlib.sha256(data).hexdigest()
            tx,ty,tw,th=c['controls_x'],c['controls_y'],c['controls_width'],c['controls_height']
            crop=png_rows(tw,th,[pixels[(y*w+tx)*4:(y*w+tx+tw)*4] for y in range(ty,ty+th)])
            (self.folder/c['controls_name']).write_bytes(crop);c['controls_sha256']=hashlib.sha256(crop).hexdigest()
            c['frame_exterior_sha256']=image_evidence(path,self.folder/c['controls_name'],w,h,self.scale)
        with self.assertRaisesRegex(SmokeFailure,'differs from retained source'):self.validate(p)

    def test_source_hash_verified_even_after_cache_populated(self):
        source=self.game/'wrong-source.png';source.write_bytes(b'wrong source')
        with self.assertRaisesRegex(SmokeFailure,'artwork hash differs'):self.validate(source_path=source)

    def test_forged_settled_signal_pixels_rejected(self):
        p=copy.deepcopy(self.proof);c=p['captures'][0];path=self.folder/c['name'];w,h,pixels=decode_png(path);pixels=bytearray(pixels)
        sx,sy=transform(w,h,self.scale);l,t,r,b=INDICATORS[3];x,y=math.floor(sx((l+r)/2)),math.floor(sy((t+b)/2))
        pixels[(y*w+x)*4:(y*w+x+1)*4]=bytes((255,193,122,255))
        data=png_rows(w,h,[pixels[y*w*4:(y+1)*w*4] for y in range(h)]);path.write_bytes(data);c['sha256']=hashlib.sha256(data).hexdigest()
        tx,ty,tw,th=c['controls_x'],c['controls_y'],c['controls_width'],c['controls_height']
        crop=png_rows(tw,th,[pixels[(y*w+tx)*4:(y*w+tx+tw)*4] for y in range(ty,ty+th)])
        (self.folder/c['controls_name']).write_bytes(crop);c['controls_sha256']=hashlib.sha256(crop).hexdigest()
        c['frame_exterior_sha256']=image_evidence(path,self.folder/c['controls_name'],w,h,self.scale)
        with self.assertRaisesRegex(SmokeFailure,'Settled signal indicator pixels'):self.validate(p)

    def test_stable_gray_unselected_rows_rejected(self):
        p=copy.deepcopy(self.proof)
        for c in p['captures']:
            path=self.folder/c['name'];w,h,pixels=decode_png(path);pixels=bytearray(pixels);sx,sy=transform(w,h,self.scale)
            for i,(l,t,r,b) in enumerate(INDICATORS[:3]):
                if i==c['selected_slot']//6:continue
                for y in range(math.floor(sy(t)),math.ceil(sy(b))):
                    for x in range(math.floor(sx(l)),math.ceil(sx(r))):pixels[(y*w+x)*4:(y*w+x+1)*4]=INACTIVE
            data=png_rows(w,h,[pixels[y*w*4:(y+1)*w*4] for y in range(h)]);path.write_bytes(data);c['sha256']=hashlib.sha256(data).hexdigest()
            tx,ty,tw,th=c['controls_x'],c['controls_y'],c['controls_width'],c['controls_height']
            crop=png_rows(tw,th,[pixels[(y*w+tx)*4:(y*w+tx+tw)*4] for y in range(ty,ty+th)])
            (self.folder/c['controls_name']).write_bytes(crop);c['controls_sha256']=hashlib.sha256(crop).hexdigest()
            c['frame_exterior_sha256']=image_evidence(path,self.folder/c['controls_name'],w,h,self.scale)
        with self.assertRaisesRegex(SmokeFailure,'Unselected row indicator differs from retained source'):self.validate(p)

    def test_outline_samples_exist_for_all_captured_slots_in_five_profiles(self):
        for w,h,s in [(1280,720,1),(1280,260,1),(1920,1080,1),(1920,1080,2),(1920,1080,3)]:
            for slot in range(18):
                with self.subTest(w=w,h=h,s=s,slot=slot):self.assertEqual(len(outline_samples(w,h,s,slot)),4)

    def test_permanent_slot_zero_outline_rejected(self):
        p=copy.deepcopy(self.proof)
        # Replace every slot1 capture's outline samples with retained source,
        # retaining a slot0 outline. Crop/hash/ring metadata remain consistent.
        sw,sh,art=load_source();sx,sy=transform(self.width,self.height,self.scale)
        _,vs,_=expected_geometry(self.width,self.height,self.scale);factor=vs*f32(1/3)*self.scale
        for c in p['captures']:
            if c['selected_slot']!=1:continue
            path=self.folder/c['name'];w,h,pixels=decode_png(path);pixels=bytearray(pixels)
            for x,y in outline_samples(w,h,self.scale,1):
                tx,ty=math.floor((x+.5-sx(0))/factor),math.floor((y+.5-sy(0))/factor)
                pixels[(y*w+x)*4:(y*w+x+1)*4]=art[(ty*sw+tx)*4:(ty*sw+tx+1)*4]
            for x,y in outline_samples(w,h,self.scale,0):pixels[(y*w+x)*4:(y*w+x+1)*4]=ROW_ACTIVE
            data=png_rows(w,h,[pixels[y*w*4:(y+1)*w*4] for y in range(h)]);path.write_bytes(data);c['sha256']=hashlib.sha256(data).hexdigest()
            c['frame_exterior_sha256']=image_evidence(path,self.folder/c['controls_name'],w,h,self.scale)
        with self.assertRaisesRegex(SmokeFailure,'Selected-slot outline pixels'):self.validate(p)

if __name__=='__main__':unittest.main()
