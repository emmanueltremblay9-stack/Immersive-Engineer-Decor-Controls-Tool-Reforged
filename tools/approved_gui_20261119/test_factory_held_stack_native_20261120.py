"""Fail-closed unit tests for the independent Factory held-stack proof contract."""
import copy
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "approved_gui_20261119"))
sys.path.insert(0, str(ROOT / "tools" / "validation"))
from factory_control_tooltip_native import SmokeFailure, fixture_coordinates
from factory_held_stack_native_20261120 import CAPTURES, validate_fields


def sample(machine, scale):
    sha="b"*40
    jar=Path("/tmp/factory-held.jar")
    width,height=1280,720
    nw,nh=(176,166) if machine=="hopper" else (228,250)
    left,top=(width//scale-nw)//2,(height//scale-nh)//2
    px,py=fixture_coordinates(machine)[18 if machine=="hopper" else 15]
    coords=[(left+px+8,top+py+8),
            (left+(-4 if machine=="hopper" else 6),top+(-8 if machine=="hopper" else 8)),
            (left+px+8,top+py+8),
            (left+(-9 if machine=="hopper" else -2),top+83)]
    proof=dict(
        suite="native-factory-held-stack-v1",status="passed",nonce="a"*32,
        executed_source_sha=sha,machine=machine,selected_language="en_us",
        width=width,height=height,gui_scale=scale,opened_via_native_block_interaction=1,
        physical_left_clicks=5,server_client_pickups=2,inert_border_preserved=1,
        server_client_returns=1,outside_item_entity_confirmed=1,
        outside_witness_consumes_stack_by_design=True,native_visual_acceptance=False,
        input_method="XTest physical click witnessed by GLFW",
        packaged_runtime=dict(jar_path=str(jar.resolve()),jar_sha256="c"*64,
                              fixture_free_jar=True,separate_harness_module=True),
        captures=[dict(name=name,file="factory-held-"+name+".png",
                       sha256="d"*64,gui_scale=scale,menu_id=3,
                       pointer_screen_x=x,pointer_screen_y=y)
                  for name,(x,y) in zip(CAPTURES,coords)])
    return proof,("a"*32,sha,jar,"c"*64,width,height,machine,scale)


class FactoryHeldStackProof(unittest.TestCase):
    def test_positive_profiles_both_machines_both_scales(self):
        for machine in ("hopper","dropper"):
            for scale in (1,2):
                with self.subTest(machine=machine,scale=scale):
                    proof,args=sample(machine,scale)
                    captures,pos=validate_fields(proof,*args)
                    self.assertEqual(len(captures),4)
                    self.assertTrue(all(isinstance(n,int) for n in pos))

    def test_reject_missing_or_faked_physical_evidence(self):
        proof,args=sample("hopper",2)
        for name,value in (
            ("physical_left_clicks",4),("server_client_pickups",1),
            ("inert_border_preserved",0),("server_client_returns",0),
            ("outside_item_entity_confirmed",0),("outside_witness_consumes_stack_by_design",False),
            ("native_visual_acceptance",True),("gui_scale",1),
        ):
            mutation=copy.deepcopy(proof)
            mutation[name]=value
            with self.subTest(field=name), self.assertRaises(SmokeFailure):
                validate_fields(mutation,*args)

    def test_reject_swapped_border_and_exterior_coordinates(self):
        for machine in ("hopper","dropper"):
            proof,args=sample(machine,1)
            m=copy.deepcopy(proof)
            a,b=m["captures"][1],m["captures"][3]
            a["pointer_screen_x"],b["pointer_screen_x"]=b["pointer_screen_x"],a["pointer_screen_x"]
            with self.subTest(machine=machine),self.assertRaises(SmokeFailure):
                validate_fields(m,*args)

    def test_reject_stale_jar_source_or_changed_menu(self):
        proof,args=sample("dropper",2)
        tests=[
            ("executed_source_sha","0"*40),
            ("captures",list(reversed(proof["captures"]))),
            ("packaged_runtime",dict(proof["packaged_runtime"],fixture_free_jar=False)),
        ]
        for key,value in tests:
            mutation=copy.deepcopy(proof)
            mutation[key]=value
            with self.subTest(field=key),self.assertRaises(SmokeFailure):
                validate_fields(mutation,*args)
        changed=copy.deepcopy(proof)
        changed["captures"][3]["menu_id"]+=1
        with self.assertRaises(SmokeFailure):validate_fields(changed,*args)


if __name__=="__main__":
    unittest.main()
