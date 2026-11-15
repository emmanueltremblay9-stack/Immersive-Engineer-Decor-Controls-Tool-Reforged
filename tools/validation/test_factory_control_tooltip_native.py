"""Reject malformed native proof records; fixtures are NOT runtime qualification."""
import copy
from pathlib import Path
import unittest
from unittest.mock import patch
from factory_control_tooltip_native import validate, NAMES, SmokeFailure

class ProofTests(unittest.TestCase):
    def make(self, machine):
        p=dict(suite='native-factory-control-tooltip-v2',status='passed',nonce='a'*32,
            executed_source_sha='b'*40,width=320,height=260,selected_language='fr_fr',machine=machine,
            presentation_selector_absent=True,opened_via_native_block_interaction=1,native_visual_acceptance=False,
            state_change_server_acks=3 if machine=='hopper' else 0,
            trigger_setup_before=4,trigger_setup_after=0,
            packaged_runtime=dict(jar_path=str(Path('artifact.jar').resolve()),jar_sha256='c'*64,
                fixture_free_jar=True,separate_harness_module=True),captures=[])
        states={'inversion':1,'inversion_changed':4,'triggermode':0,'triggermode_changed':2}
        for name in NAMES[machine]:
            text='Complete translated tooltip\nwith dynamic state '+name
            p['captures'].append(dict(name=name,file='factory-control-'+name+'.png',sha256='d'*64,
                render_frames=3,tooltip=dict(in_bounds=True,width=230,height=40,gray_characters=10,
                    text=text,expected_text=text,synced_logic=states.get(name,0))))
        return p
    def check(self,p):
        with patch('factory_control_tooltip_native.png'):
            validate(p,'a'*32,'b'*40,Path('artifact.jar'),'c'*64,Path('game'),320,260,'fr_fr',p['machine'])
    def test_valid_records(self):
        for machine in NAMES: self.check(self.make(machine))
    def test_reject_identity_isolation_and_coverage(self):
        p=self.make('hopper')
        for key,value in [('nonce','x'),('executed_source_sha','x'),('state_change_server_acks',0),('captures',[]),('native_visual_acceptance',True)]:
            q=copy.deepcopy(p);q[key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.check(q)
        for key,value in [('jar_sha256','x'),('fixture_free_jar',False),('separate_harness_module',False)]:
            q=copy.deepcopy(p);q['packaged_runtime'][key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.check(q)
    def test_reject_unchanged_server_state(self):
        p=self.make('hopper')
        p['captures'][3]['tooltip']['synced_logic']=p['captures'][2]['tooltip']['synced_logic']
        with self.assertRaises(SmokeFailure):self.check(p)
    def test_reject_invisible_trigger_transition(self):
        p=self.make('hopper')
        for key in ('text','expected_text'):
            p['captures'][5]['tooltip'][key]=p['captures'][4]['tooltip'][key]
        with self.assertRaises(SmokeFailure):self.check(p)
    def test_reject_ignored_trigger_mode(self):
        p=self.make('hopper');p['captures'][4]['tooltip']['synced_logic']=4
        p['trigger_setup_after']=4
        with self.assertRaises(SmokeFailure):self.check(p)
    def test_reject_render_semantic_mismatch(self):
        p=self.make('dropper');p['captures'][1]['tooltip']['expected_text']='Different complete text'
        with self.assertRaises(SmokeFailure):self.check(p)
    def test_reject_tooltip_damage(self):
        for key,value in [('in_bounds',False),('width',241),('height',10),('gray_characters',0),('text','short'),('synced_logic',None)]:
            q=self.make('hopper');q['captures'][2]['tooltip'][key]=value
            with self.subTest(key=key),self.assertRaises(SmokeFailure):self.check(q)
if __name__=='__main__':unittest.main()
