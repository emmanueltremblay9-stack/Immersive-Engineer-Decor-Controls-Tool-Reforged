"""Adversarial SYNTHETIC evidence tests. Passing these is not native game evidence."""
import copy
import hashlib
import json
from pathlib import Path
import struct
import tempfile
import unittest
import zlib
from milking_qualification_native import (BACKGROUND_SHA,MILK_SHA,SUITE,SCREEN_CLASS,PROFILES,CASES,DEFERRED,
    CAPTURES,SmokeFailure,expected_geometry,slot_position,validate_proof,validate_pixels,decode_png,current_candidate)
from check_milking_qualification import check_preservation


def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data))


def png(w,h,pixels,comment=None):
    data=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))
    if comment is not None:data+=chunk(b'tEXt',comment)
    return data+chunk(b'IDAT',zlib.compress(b''.join(b'\0'+pixels[y*w*4:(y+1)*w*4] for y in range(h))))+chunk(b'IEND',b'')


class MilkingEvidenceTest(unittest.TestCase):
    CACHE={}
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup)
        self.game=Path(self.tmp.name)/'run';self.folder=self.game/'milking-qualification-screenshots';self.folder.mkdir(parents=True)
        self.nonce='a'*32;self.sha='b'*40;self.logs=f'IEDCTR_MILKING_QUALIFICATION PASSED nonce={self.nonce}'
        self.profile('fr-1280x260-scale1')

    def profile(self,name):
        self.width,self.height,self.scale,self.language=PROFILES[name]
        self.g=expected_geometry(self.width,self.height,self.scale)
        self.proof=dict(suite=SUITE,nonce=self.nonce,executed_source_sha=self.sha,status='passed_bounded_smoke',
            source_background_sha256=BACKGROUND_SHA,source_milk_sha256=MILK_SHA,glass_overlay_enabled=False,
            actual_integrated_world=True,native_visual_acceptance=False,requested_language=self.language,
            selected_language=self.language,menu_slot_count=38,screen_class=SCREEN_CLASS,
            cases=sorted(CASES),deferred_checks=sorted(DEFERRED),slot_routes=[],captures=[])
        self.proof.update({k:v for k,v in self.g.items() if not k.startswith(('tank_','presentation_'))})
        for i in range(38):
            dst=2 if i==1 else i;x,y=slot_position(i);dx,dy=slot_position(dst)
            l,t=self.g['presentation_gui_left'],self.g['presentation_gui_top']
            self.proof['slot_routes'].append(dict(source_menu_index=i,destination_menu_index=dst,source_x=l+x+8,
                source_y=t+y+8,destination_x=l+dx+8,destination_y=t+dy+8,
                item_id='minecraft:bucket' if i==0 else 'minecraft:milk_bucket' if i==1 else 'minecraft:dirt',
                count=1,pickup_server_ack=True,return_server_ack=True,conserved_items=1,final_fluid_amount=0,menu_id=1,pickup_to_return_interval_nanos=400_000_000))
        self.proof['bucket_conversion']=dict(native_tick_observed=True,server_confirmed=True,client_confirmed=True,
            seeded_output=False,fluid_id='minecraft:milk',conserved_milk_mb=1000,
            before=dict(amount=1000,input_buckets=1,output_milk_buckets=0,player_items=0,carried_items=0),
            after=dict(amount=0,input_buckets=0,output_milk_buckets=1,player_items=0,carried_items=0))
        if name not in self.CACHE:
            captures=[];files={}
            for label,amount in CAPTURES.items():
                g=self.g;w=self.width;h=self.height;tw=g['tank_width'];th=g['tank_height']
                frame=bytearray(b'\x20\x30\x40\xff'*(w*h));tank=bytearray(b'\x20\x30\x40\xff'*(tw*th))
                fill=(51*amount//4000)*self.scale
                for y in range(th-fill,th):
                    row=b'\xe1\xda\xb8\xff'*tw
                    tank[y*tw*4:(y+1)*tw*4]=row
                    start=((g['tank_y']+y)*w+g['tank_x'])*4;frame[start:start+tw*4]=row
                tn=label[:-4]+'-tank.png';files[label]=png(w,h,frame);files[tn]=png(tw,th,tank)
                captures.append(dict(name=label,tank_name=tn,sha256=hashlib.sha256(files[label]).hexdigest(),
                    tank_sha256=hashlib.sha256(files[tn]).hexdigest(),**g,width=w,height=h,screen_class=SCREEN_CLASS,
                    amount=amount,client_fluid_amount=amount,server_amount=amount,capacity=4000,
                    fluid_id='minecraft:milk' if amount else 'minecraft:empty',empty_slots_and_cursor=True,
                    server_confirmed=True,menu_id=1,server_menu_id=1,settled_client_ticks=12))
            self.CACHE[name]=(captures,files)
        captures,files=self.CACHE[name];self.proof['captures']=copy.deepcopy(captures)
        for name,data in files.items():(self.folder/name).write_bytes(data)

    def validate(self,proof=None,**kwargs):
        opts=dict(logs=self.logs,exit_code=0,timed_out=False,width=self.width,height=self.height,
                  language=self.language,gui_scale=self.scale);opts.update(kwargs)
        return validate_proof(self.proof if proof is None else proof,self.nonce,self.sha,self.game,**opts)

    def reject_mutations(self,path,values):
        for value in values:
            with self.subTest(path=path,value=value):
                p=copy.deepcopy(self.proof);target=p
                for key in path[:-1]:target=target[key]
                target[path[-1]]=value
                with self.assertRaises(SmokeFailure):self.validate(p)

    def alter_image(self,index,fn,tank=False,comment=None):
        c=self.proof['captures'][index];key='tank_name' if tank else 'name';hashkey='tank_sha256' if tank else 'sha256'
        path=self.folder/c[key];w,h,pixels=decode_png(path);data=png(w,h,fn(bytearray(pixels),w,h),comment)
        path.write_bytes(data);c[hashkey]=hashlib.sha256(data).hexdigest()

    def test_all_five_planned_profiles(self):
        for profile in PROFILES:
            with self.subTest(profile=profile):self.profile(profile);self.validate()

    def test_independent_fixed_geometry(self):
        cases=[(1280,720,1,(526,246,630,295,22,51)),(1280,260,1,(526,16,630,65,22,51)),
               (1920,1080,1,(846,426,950,475,22,51)),(1920,1080,2,(366,156,940,410,44,102)),
               (1920,1080,3,(206,66,930,345,66,153)),(1281,261,1,(526,16,630,65,22,51))]
        for w,h,s,expected in cases:
            g=expected_geometry(w,h,s)
            self.assertEqual(tuple(g[k] for k in ('presentation_gui_left','presentation_gui_top','tank_x','tank_y','tank_width','tank_height')),expected)

    def test_slot_mapping_fixed(self):
        self.assertEqual([slot_position(i) for i in (0,1,2,10,11,28,29,37)],
                         [(49,94),(163,94),(26,132),(186,132),(26,152),(186,172),(26,195),(186,195)])

    def test_reject_unsupported_geometry(self):
        for w,h,s in [(1280,260,2),(1921,1081,3),(319,240,1),(320,239,1),(1280,720,True),(1280.,720,1),(1280,720,4)]:
            with self.subTest(w=w,h=h,s=s),self.assertRaises(SmokeFailure):expected_geometry(w,h,s)

    def test_root_identity_and_status(self):
        for key in ('suite','nonce','executed_source_sha','source_background_sha256','source_milk_sha256',
                    'requested_language','selected_language','screen_class','status'):
            self.reject_mutations([key],['','incorrect',None,1])
        for key in ('actual_integrated_world','native_visual_acceptance','glass_overlay_enabled'):
            self.reject_mutations([key],[not self.proof[key],1,None])
        self.reject_mutations(['menu_slot_count'],[37,38.,True,None])

    def test_missing_root_fields(self):
        for key in self.proof:
            p=copy.deepcopy(self.proof);del p[key]
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.validate(p)

    def test_missing_capture_fields(self):
        for key in self.proof['captures'][0]:
            p=copy.deepcopy(self.proof);del p['captures'][0][key]
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.validate(p)

    def test_missing_route_fields(self):
        for key in self.proof['slot_routes'][0]:
            p=copy.deepcopy(self.proof);del p['slot_routes'][0][key]
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.validate(p)

    def test_no_scope_inflation(self):
        for key in ('cases','deferred_checks'):
            self.reject_mutations([key],[[],self.proof[key]+[self.proof[key][0]],None,[False]])
        p=copy.deepcopy(self.proof);p['error']='fixture failed'
        with self.assertRaises(SmokeFailure):self.validate(p)

    def test_clean_exit_and_fatal_logs(self):
        for opts in [dict(exit_code=1),dict(exit_code=False),dict(exit_code=None),dict(timed_out=True),dict(logs=''),dict(logs='Uncaught exception in thread')]:
            with self.subTest(opts=opts),self.assertRaises(SmokeFailure):self.validate(**opts)

    def test_crash_report(self):
        (self.game/'crash-reports').mkdir();(self.game/'crash-reports/crash.txt').write_text('crash')
        with self.assertRaises(SmokeFailure):self.validate()

    def test_viewport_drift(self):
        for key,value in self.g.items():
            paths=[['captures',0,key]]+([] if key.startswith(('tank_','presentation_')) else [[key]])
            for path in paths:self.reject_mutations(path,[value+1,True,None,float('nan')])

    def test_scale_fallback_rejected(self):
        self.profile('en-1920x1080-scale1')
        with self.assertRaises(SmokeFailure):self.validate(gui_scale=3)

    def test_38_ordered_routes_required(self):
        routes=self.proof['slot_routes']
        self.reject_mutations(['slot_routes'],[[],routes[:-1],routes+[routes[0]],list(reversed(routes)),None])

    def test_wrong_route_or_missing_ack(self):
        for i in (0,1,2,28,29,37):
            r=self.proof['slot_routes'][i]
            for key,value in r.items():
                if key=='pickup_to_return_interval_nanos':continue
                bad=not value if type(value)is bool else value+1 if type(value)is int else 'minecraft:air'
                self.reject_mutations(['slot_routes',i,key],[bad,None])

    def test_single_click_spacing_required(self):
        self.reject_mutations(['slot_routes',0,'pickup_to_return_interval_nanos'],[0,249_000_000,299_999_999,True,None,900_000_000_000])

    def test_output_cannot_return_to_output(self):self.reject_mutations(['slot_routes',1,'destination_menu_index'],[1])

    def test_conversion_must_be_generated_and_conserved(self):
        for key in ('native_tick_observed','server_confirmed','client_confirmed','seeded_output'):
            self.reject_mutations(['bucket_conversion',key],[not self.proof['bucket_conversion'][key],1,None])
        for phase in ('before','after'):
            for key,value in self.proof['bucket_conversion'][phase].items():
                self.reject_mutations(['bucket_conversion',phase,key],[value+1,True,None])
        self.reject_mutations(['bucket_conversion','fluid_id'],['minecraft:water'])
        self.reject_mutations(['bucket_conversion','conserved_milk_mb'],[999,True])

    def test_capture_state_server_identity(self):
        for i in range(3):
            for key in ('amount','client_fluid_amount','server_amount','capacity','menu_id','server_menu_id'):
                self.reject_mutations(['captures',i,key],[self.proof['captures'][i][key]+1,True,None])
            for key in ('server_confirmed','empty_slots_and_cursor'):
                self.reject_mutations(['captures',i,key],[False,1,None])
            self.reject_mutations(['captures',i,'fluid_id'],['minecraft:water','neoforge:milk',None])

    def test_capture_requires_settled_client_state(self):
        self.reject_mutations(['captures',0,'settled_client_ticks'],[0,11,1800,True,None,12.0])

    def test_capture_matrix_paths(self):
        self.reject_mutations(['captures'],[[],self.proof['captures'][:-1],self.proof['captures']+[self.proof['captures'][0]]])
        for name in ('../empty.png','/empty.png','half.png','EMPTY.png',None):self.reject_mutations(['captures',0,'name'],[name])
        self.reject_mutations(['captures',0,'tank_name'],['../empty-tank.png','full-tank.png'])

    def test_missing_or_symlink_png(self):
        file=self.folder/'empty.png';data=file.read_bytes();file.unlink()
        with self.assertRaises(SmokeFailure):self.validate()
        elsewhere=Path(self.tmp.name)/'image.png';elsewhere.write_bytes(data);file.symlink_to(elsewhere)
        with self.assertRaises(SmokeFailure):self.validate()

    def test_hash_is_required_but_not_enough(self):self.reject_mutations(['captures',0,'sha256'],['0'*64,'x',None])

    def test_corrupt_crc_with_matching_file_hash(self):
        c=self.proof['captures'][0];p=self.folder/c['name'];data=bytearray(p.read_bytes());data[-1]^=1;p.write_bytes(data);c['sha256']=hashlib.sha256(data).hexdigest()
        with self.assertRaisesRegex(SmokeFailure,'CRC'):self.validate()

    def test_png_trailing_and_truncated_data(self):
        path=self.folder/'empty.png';data=path.read_bytes()
        for changed in (data+b'extra',data[:-12],b'bad',data[:40]):
            path.write_bytes(changed)
            with self.assertRaises(SmokeFailure):decode_png(path)

    def test_crop_pixels_must_match_frame(self):
        self.alter_image(1,lambda p,w,h: bytes([0])*len(p),tank=True)
        with self.assertRaisesRegex(SmokeFailure,'native screenshot region'):self.validate()

    def test_identical_decoded_states_with_different_encodings(self):
        empty=self.proof['captures'][0]
        for index in (1,2):
            for tank,key in ((False,'name'),(True,'tank_name')):
                w,h,pixels=decode_png(self.folder/empty[key]);self.alter_image(index,lambda p,w,h,px=pixels:px,tank=tank,comment=f'distinct {index}'.encode())
        with self.assertRaisesRegex(SmokeFailure,'coverage'):self.validate()

    def test_top_down_half_fill_rejected(self):
        def flip(p,w,h):return b''.join(p[y*w*4:(y+1)*w*4] for y in reversed(range(h)))
        self.alter_image(1,flip,tank=True)
        c=self.proof['captures'][1];_,_,tank=decode_png(self.folder/c['tank_name']);g=self.g
        def replace(p,w,h):
            for y in range(g['tank_height']):
                start=((g['tank_y']+y)*w+g['tank_x'])*4;p[start:start+g['tank_width']*4]=tank[y*g['tank_width']*4:(y+1)*g['tank_width']*4]
            return p
        self.alter_image(1,replace)
        with self.assertRaisesRegex(SmokeFailure,'above bottom-up'):self.validate()

    def test_one_pixel_encoded_distinction_cannot_pass(self):
        # A nonempty hash and even distinct decoded bytes are insufficient.
        empty=self.proof['captures'][0]
        for index in (1,2):
            _,_,base=decode_png(self.folder/empty['tank_name']);changed=bytearray(base)
            changed[-4]=100+index
            self.alter_image(index,lambda p,w,h,px=changed:px,tank=True)
            g=self.g
            def replace(p,w,h,px=changed):
                for y in range(g['tank_height']):
                    start=((g['tank_y']+y)*w+g['tank_x'])*4
                    p[start:start+g['tank_width']*4]=px[y*g['tank_width']*4:(y+1)*g['tank_width']*4]
                return p
            self.alter_image(index,replace)
        with self.assertRaises(SmokeFailure):self.validate()

    def test_server_confirmed_flag_without_conversion_snapshot_fails(self):
        for phase in ('before','after'):
            p=copy.deepcopy(self.proof);del p['bucket_conversion'][phase]
            with self.assertRaises(SmokeFailure):self.validate(p)

    def test_wrong_screen_class_per_capture_fails(self):
        self.reject_mutations(['captures',1,'screen_class'],['com.example.ReplacementScreen',SCREEN_CLASS+'Fake'])

    def test_preservation_guard_rejects_changed_production_bytes(self):
        import shutil
        root=Path(__file__).resolve().parents[2]
        with tempfile.TemporaryDirectory() as tmp:
            dest=Path(tmp)
            manifest=root/'docs/testing/milking-qualification/production-pins.json'
            files=json.loads(manifest.read_text())['sha256']
            for name in [str(manifest.relative_to(root)),*files]:
                out=dest/name;out.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(root/name,out)
            target=dest/next(iter(files));target.write_bytes(target.read_bytes()+b'changed')
            with self.assertRaisesRegex(SmokeFailure,'preservation'):check_preservation(dest)

    def test_transparent_milk_edge_margins_are_valid(self):
        # Edge alpha can round to no changed framebuffer pixels. It must not be
        # required to meet the interior density threshold, at any GUI scale.
        for profile in ('en-1280x720-scale1','en-1920x1080-scale2','en-1920x1080-scale3'):
            self.profile(profile)
            for index in (1,2):
                c=self.proof['captures'][index];w,h,tank=decode_png(self.folder/c['tank_name']);tank=bytearray(tank)
                for y in list(range(2*self.scale))+list(range(h-2*self.scale,h)):
                    tank[y*w*4:(y+1)*w*4]=b'\x20\x30\x40\xff'*w
                self.alter_image(index,lambda p,w,h,px=tank:px,tank=True)
                g=self.g
                def replace(p,w,h,px=tank):
                    for y in range(g['tank_height']):
                        start=((g['tank_y']+y)*w+g['tank_x'])*4
                        p[start:start+g['tank_width']*4]=px[y*g['tank_width']*4:(y+1)*g['tank_width']*4]
                    return p
                self.alter_image(index,replace)
            self.validate()

    def test_pinned_source_translucent_edges_and_interior_coverage(self):
        # Source-derived sampling regression, NOT a native/GPU rasterization claim.
        root=Path(__file__).resolve().parents[2]/'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/milking_redesign_v1'
        _,_,milk=decode_png(root/'milk.png');_,_,background=decode_png(root/'background.png')
        alphas=milk[3::4]
        self.assertTrue(any(0<a<=1 for a in alphas))
        for scale in (1,2,3):
            w,h=22*scale,51*scale;empty=bytearray();full=bytearray()
            for y in range(h):
                for x in range(w):
                    sx=int(479+(x+.5)*296/w);sy=int(228+(y+.5)*802/h)
                    bx=int((104+(x+.5)/scale)*1254/228);by=int((49+(y+.5)/scale)*1254/228)
                    src=milk[(sy*1254+sx)*4:(sy*1254+sx+1)*4]
                    dst=background[(by*1254+bx)*4:(by*1254+bx+1)*4]
                    empty.extend(dst)
                    full.extend(bytes((src[c]*src[3]+dst[c]*(255-src[3])+127)//255 for c in range(3))+b'\xff')
            split=26*scale*w*4;half=empty[:split]+full[split:]
            g=expected_geometry(1920,1080,scale)
            validate_pixels({'empty.png':bytes(empty),'half.png':bytes(half),'full.png':bytes(full)},
                            {name:b'same exterior' for name in CAPTURES},g)

    def test_exterior_change_rejected(self):
        def pixel(p,w,h):
            i=((self.g['presentation_gui_top']+20)*w+self.g['presentation_gui_left']+20)*4;p[i]^=1;return p
        self.alter_image(2,pixel)
        with self.assertRaisesRegex(SmokeFailure,'exterior artwork'):self.validate()

    def test_former_machine_text_regions_are_not_exempt_at_any_scale(self):
        # All former name, UUID, status, amount and capacity bands now remain art.
        for profile in ('en-1280x720-scale1','en-1920x1080-scale2','en-1920x1080-scale3'):
            for x,y in ((114,6),(114,16),(114,115),(150,73),(150,81)):
                with self.subTest(profile=profile,x=x,y=y):
                    self.profile(profile)
                    def pixel(p,w,h):
                        i=(((self.g['presentation_gui_top']+y)*self.scale)*w
                           +(self.g['presentation_gui_left']+x)*self.scale)*4
                        p[i]^=1
                        return p
                    self.alter_image(2,pixel)
                    with self.assertRaisesRegex(SmokeFailure,'exterior artwork'):self.validate()

    def test_outside_label_margin_not_exempt(self):
        def pixel(p,w,h):
            i=((self.g['presentation_gui_top']+89)*w+self.g['presentation_gui_left']+150)*4;p[i]^=1;return p
        self.alter_image(2,pixel)
        with self.assertRaisesRegex(SmokeFailure,'exterior artwork'):self.validate()

    def test_pinned_production_bytes(self):self.assertGreater(check_preservation(),10)

    def test_dirty_candidate_cannot_borrow_base_sha(self):
        root=Path(__file__).resolve().parents[2]
        # Test via a tiny isolated repository so committed future test runs also pass.
        import subprocess
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);subprocess.run(['git','init','-q',str(root)],check=True)
            (root/'uncommitted.py').write_text('uncommitted')
            with self.assertRaisesRegex(SmokeFailure,'clean committed'):current_candidate(root)


if __name__=='__main__':unittest.main()
