"""Synthetic images/proofs exercise rejection only, never native gameplay evidence."""
import copy
import hashlib
from pathlib import Path
import tempfile
import unittest
from tree_cutter_v3_native import *
from test_factory_placer_v2_reference_native import png_rows


class TreeEvidenceTests(unittest.TestCase):
    CACHE={}
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup)
        self.game=Path(self.tmp.name);self.jar=self.game/'normal.jar'
        self.nonce='a'*32;self.sha='b'*40;self.jar_sha='c'*64
        classes=[SCREEN,'com.oblixorprime.engineersdecorreforged.EngineersDecorReforged',
            'com.oblixorprime.engineersdecorreforged.utility.TreeCutterV3ReferenceGeometry',
            'com.oblixorprime.engineersdecorreforged.menu.MachineMenu','com.oblixorprime.engineersdecorreforged.utility.MachineBlockEntity']
        self.proof=dict(suite='native-tree-cutter-v3-v2',status='passed_bounded_smoke',nonce=self.nonce,
            executed_source_sha=self.sha,selected_language='en_us',menu_slot_count=37,native_visual_acceptance=False,
            opened_via_native_block_interaction=1,pickup_server_acks=37,return_server_acks=37,rejected_output_attempts_state_preserved=37,
            final_item_total=1,resource_reload_confirmed=True,output_packet_processing_observed=False,physical_slot_clicks=111,slot_routes=list(range(37)),
            input_method='XTest physical block opening and slot clicks; observed GLFW cursor and server inventory transitions',
            packaged_runtime=dict(jar_path=str(self.jar),jar_sha256=self.jar_sha,fixture_free_jar=True,separate_harness_module=True,
                active_texture_sha256=ART_SHA,active_texture_pack='mod/immersive_engineer_decor_controls_tool_reforged',
                class_origins=[dict(**{'class':c},resource_uri='union:/synthetic/'+c,code_source_uri='union:/synthetic/',sha256='d'*64) for c in classes]))
        self.profile(1280,720,1)

    def profile(self,w,h,s):
        self.width,self.height,self.scale=w,h,s
        self.proof.update(width=w,height=h,gui_scale=float(s));self.proof['captures']=[]
        x,y,cw,ch,ox,oy,v=geometry(w,h,s)
        key=w,h,s
        if key not in self.CACHE:
            art=source_pixels(ART_PATH);factor=s*v*f32(1/3)
            rows=[]
            for cy in range(ch):
                sy=min(1253,max(0,math.floor((cy+y+.5-oy*s)/factor)))
                row=bytearray()
                for cx in range(cw):
                    sx=min(1253,max(0,math.floor((cx+x+.5-ox*s)/factor)))
                    row+=art[(sy*1254+sx)*4:(sy*1254+sx+1)*4]
                rows.append(row)
            self.CACHE[key]=png_rows(cw,ch,rows)
        data=self.CACHE[key]
        for i,name in enumerate(NAMES):
            file='tree-'+name+'.png';(self.game/file).write_bytes(data)
            self.proof['captures'].append(dict(name=name,file=file,sha256=hashlib.sha256(data).hexdigest(),
                x=x,y=y,width=cw,height=ch,menu_id=1 if i<2 else 2,screen_class=SCREEN))

    def validate(self,proof=None,**kwargs):
        validate(self.proof if proof is None else proof,self.nonce,self.sha,self.jar,self.jar_sha,self.game,
                 self.width,self.height,self.proof['selected_language'],self.scale,**kwargs)

    def test_profiles(self):
        for w,h,s,lang in [(1280,720,1,'en_us'),(1280,260,1,'fr_fr'),(1920,1080,1,'en_us'),(1920,1080,2,'en_us'),(1920,1080,3,'fr_fr')]:
            with self.subTest(w=w,h=h,s=s):
                self.profile(w,h,s);self.proof['selected_language']=lang;self.validate()

    def test_identity_types_and_ack_rejection(self):
        for key,value in [('nonce','stale'),('executed_source_sha','e'*40),('status','passed'),('width',True),
            ('gui_scale',True),('menu_slot_count',True),('native_visual_acceptance',True),('final_item_total',0),
            ('physical_slot_clicks',110),('physical_slot_clicks',True),('suite','native-tree-cutter-v3-v1'),('pickup_server_acks',36),('return_server_acks',True),('rejected_output_attempts_state_preserved',36),
            ('output_packet_processing_observed',True),('resource_reload_confirmed',1),('input_method','physical clicks'),('opened_via_native_block_interaction',0)]:
            p=copy.deepcopy(self.proof);p[key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.validate(p)

    def test_old_packet_ack_claim_rejected(self):
        p=copy.deepcopy(self.proof);p['rejected_insertion_server_acks']=37
        with self.assertRaisesRegex(SmokeFailure,'Unsupported output packet acknowledgement'):
            self.validate(p)
        del p['rejected_output_attempts_state_preserved']
        with self.assertRaises(SmokeFailure):self.validate(p)

    def test_routes(self):
        for routes in [list(range(36)),[False]+list(range(1,37)),list(range(36))+[35]]:
            p=copy.deepcopy(self.proof);p['slot_routes']=routes
            with self.assertRaises(SmokeFailure):self.validate(p)

    def test_provenance(self):
        for key,value in [('jar_path','other.jar'),('jar_sha256','e'*64),('fixture_free_jar',1),('separate_harness_module',False),
            ('active_texture_sha256','e'*64),('active_texture_pack','resourcepack'),('class_origins',[])]:
            p=copy.deepcopy(self.proof);p['packaged_runtime'][key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.validate(p)

    def test_capture_bounds_and_paths(self):
        for key,value in [('file','../outside.png'),('width',1280),('x',0),('menu_id',True),('screen_class','other')]:
            p=copy.deepcopy(self.proof);p['captures'][0][key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.validate(p)
        p=copy.deepcopy(self.proof);p['captures']=p['captures'][:-1]
        with self.assertRaises(SmokeFailure):self.validate(p)

    def test_reopen_identity(self):
        p=copy.deepcopy(self.proof);p['captures'][2]['menu_id']=1
        with self.assertRaises(SmokeFailure):self.validate(p)

    def test_forged_pixels_with_updated_hash(self):
        c=self.proof['captures'][0];w,h=c['width'],c['height']
        data=png_rows(w,h,[bytes((20,20,20,255))*w]*h)
        (self.game/c['file']).write_bytes(data);c['sha256']=hashlib.sha256(data).hexdigest()
        with self.assertRaisesRegex(SmokeFailure,'pixels differ'):self.validate()

    def test_full_frame_rejected_even_with_updated_hash(self):
        c=self.proof['captures'][0]
        data=png_rows(self.width,self.height,[bytes((20,20,20,255))*self.width]*self.height)
        (self.game/c['file']).write_bytes(data);c['sha256']=hashlib.sha256(data).hexdigest()
        with self.assertRaisesRegex(SmokeFailure,'GUI-only crop'):self.validate()

    def test_missing_symlink_crc_and_source(self):
        c=self.proof['captures'][0];path=self.game/c['file'];data=path.read_bytes();path.unlink()
        with self.assertRaises(SmokeFailure):self.validate()
        target=self.game/'target.png';target.write_bytes(data);path.symlink_to(target)
        with self.assertRaises(SmokeFailure):self.validate()
        path.unlink();bad=bytearray(data);bad[-1]^=1;path.write_bytes(bad);c['sha256']=hashlib.sha256(bad).hexdigest()
        with self.assertRaises(SmokeFailure):self.validate()
        path.write_bytes(data);c['sha256']=hashlib.sha256(data).hexdigest()
        with self.assertRaises(SmokeFailure):self.validate(source_path=target)

    def test_failure_and_crash(self):
        for p in [[],False,dict(self.proof,error='failed')]:
            with self.assertRaises(SmokeFailure):self.validate(p)
        (self.game/'crash-reports').mkdir()
        with self.assertRaises(SmokeFailure):self.validate()

    def test_malformed_nested_records(self):
        for key,value in [('captures',[None]),('packaged_runtime',[]),('captures',True)]:
            p=copy.deepcopy(self.proof);p[key]=value
            with self.assertRaises(SmokeFailure):self.validate(p)
        p=copy.deepcopy(self.proof);p['packaged_runtime']['class_origins']=[None]*5
        with self.assertRaises(SmokeFailure):self.validate(p)

    def test_invalid_viewports(self):
        for w,h,s in [(1280,260,2),(1280,720,True),(0,720,1),(1920,1080,4)]:
            with self.assertRaises(SmokeFailure):geometry(w,h,s)


if __name__=='__main__':unittest.main()
