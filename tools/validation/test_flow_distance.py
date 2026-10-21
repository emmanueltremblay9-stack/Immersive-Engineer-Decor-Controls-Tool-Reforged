import copy
import struct
import tempfile
import unittest
from pathlib import Path
from client_mixin_smoke import SmokeFailure
from flow_distance import validate_flow_distance_proof

class FlowDistanceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root/"canal-screenshots").mkdir()
        self.session = {"suite":"native-water-flow-session-v1","nonce":"current","status":"passed"}
        self.proof = {"suite":"native-water-flow-distance-v2","nonce":"current","status":"captured",
                      "actual_integrated_world":True,"orientation":"east","renderer_mods":{},"cases":[]}
        for d in range(10):
            samples=[]
            for i in range(10):
                water = i<8 or (d==9)
                samples.append({"position":f"{13-i}, 80, 5","block_state":"minecraft:water" if water else "minecraft:air",
                                "water":water,"source":i==0 or (d==9 and i==4),"amount":(8 if d==9 and i==4 else max(1,8-i)) if water else 0,
                                "height":max(1,8-i)/9 if water else 0})
            target = copy.deepcopy(samples[4 if d in (0,9) else d])
            if d: target["block_state"]='steel_catwalk_stairs,facing:"east",waterlogged:"true"' if d==9 else 'steel_catwalk_stairs,facing:"east",waterlogged:"false"'
            if d: samples[4 if d==9 else d] = copy.deepcopy(target)
            case={"distance":d,"direct_cell_fill":d==9,"initial_sources_placed":2 if d==9 else 1,
                  "settled_ticks":100,"unchanged_client_ticks":20,"source":samples[0],
                  "target":target,"upstream":copy.deepcopy(samples[(4 if d in (0,9) else d)-1]),
                  "downstream":copy.deepcopy(samples[(4 if d in (0,9) else d)+1]),"channel":samples,
                  "mechanical_flow_in_cell":target["water"],"mechanical_flow_after_cell":samples[(4 if d in (0,9) else d)+1]["water"]}
            case.update(server_elapsed_ticks=100,server_client_agree=True,server_channel=copy.deepcopy(samples))
            for angle in ("overview","close"):
                name=f"flow-distance-{d}-{angle}.png"
                case[angle+"_screenshot"]=name
                case[angle+"_yaw"]=180.0
                case[angle+"_pitch"]=80.0
                (self.root/"canal-screenshots"/name).write_bytes(b"\x89PNG\r\n\x1a\n"+b"\0\0\0\rIHDR"+struct.pack(">II",1280,720)+bytes(1024))
            removed=copy.deepcopy(samples)
            if d!=9:
                for sample in removed: sample.update(water=False,source=False,amount=0,height=0)
            case.update(after_removal_channel=removed,after_removal_server_channel=copy.deepcopy(removed),
                        after_removal_server_ticks=120,after_removal_server_client_agree=True,
                        removal_screenshot=f"flow-distance-{d}-removed.png")
            (self.root/"canal-screenshots"/case["removal_screenshot"]).write_bytes((self.root/"canal-screenshots"/case["close_screenshot"]).read_bytes())
            self.proof["cases"].append(case)
    def validate(self, proof=None):
        return validate_flow_distance_proof(proof or self.proof,self.session,"current","native",
            self.root,"IEDCTR_WATER_FLOW_SESSION PASSED nonce=current",0)
    def test_complete_natural_passage_and_recession(self):
        self.validate()
    def test_rejects_source_promotion_blockage_and_residual_water(self):
        for mutation in ("source","blocked","residual","level","range"):
            p=copy.deepcopy(self.proof);c=p["cases"][1]
            if mutation=="source": c["channel"][1]["source"]=True
            elif mutation=="blocked": c["channel"][1]["water"]=False
            elif mutation=="residual": c["after_removal_channel"][1]["water"]=True
            elif mutation=="range": c["channel"][8]["water"]=True
            else: c["channel"][1]["amount"]=8
            c["server_channel"]=copy.deepcopy(c["channel"])
            c["after_removal_server_channel"]=copy.deepcopy(c["after_removal_channel"])
            with self.subTest(mutation=mutation),self.assertRaises(SmokeFailure): self.validate(p)
    def test_stale_missing_duplicate_wrong_profile_fail(self):
        for mutation in ("stale","missing","duplicate","profile"):
            p=copy.deepcopy(self.proof)
            if mutation=="stale": p["nonce"]="old"
            elif mutation=="missing": p["cases"].pop()
            elif mutation=="duplicate": p["cases"][-1]=p["cases"][1]
            else: p["renderer_mods"]={"embeddium":"other"}
            with self.subTest(mutation=mutation),self.assertRaises(SmokeFailure): self.validate(p)
    def test_rejects_forced_fill_unsettled_motion_and_bad_control(self):
        for mutation in ("fill","settle","camera","target","control","summary"):
            p=copy.deepcopy(self.proof)
            c=p["cases"][1]
            if mutation=="fill": c["direct_cell_fill"]=True
            elif mutation=="settle": c["unchanged_client_ticks"]=0
            elif mutation=="camera": c["close_pitch"]=75
            elif mutation=="target": c["target"]["position"]="9, 80, 5"
            elif mutation=="control": p["cases"][0]["channel"][7]["water"]=False
            else: c["mechanical_flow_in_cell"]=False
            with self.subTest(mutation=mutation),self.assertRaises(SmokeFailure): self.validate(p)
    def test_rejects_screenshot_path_substitution(self):
        self.proof["cases"][1]["close_screenshot"]="../other.png"
        with self.assertRaises(SmokeFailure): self.validate()

    def test_rejects_missing_or_divergent_server_evidence(self):
        for mutation in ("missing","early","disagree"):
            p=copy.deepcopy(self.proof)
            c=p["cases"][1]
            if mutation=="missing": c.pop("server_channel")
            elif mutation=="early": c["server_elapsed_ticks"]=99
            else: c["server_channel"][1]["amount"]=6
            with self.subTest(mutation=mutation),self.assertRaises(SmokeFailure): self.validate(p)

    def test_rejects_summary_channel_disagreement(self):
        self.proof["cases"][1]["target"]["amount"]=1
        with self.assertRaises(SmokeFailure): self.validate()

    def test_rejects_lost_prefilled_source_and_removed_stair(self):
        for mutation in ("prefilled","stair","removal-server"):
            p=copy.deepcopy(self.proof)
            if mutation=="prefilled": p["cases"][9]["after_removal_channel"][4]["source"]=False
            elif mutation=="stair": p["cases"][1]["after_removal_channel"][1]["block_state"]="minecraft:air"
            else: p["cases"][1]["after_removal_server_ticks"]=99
            c=p["cases"][9 if mutation=="prefilled" else 1]
            c["after_removal_server_channel"]=copy.deepcopy(c["after_removal_channel"])
            with self.subTest(mutation=mutation),self.assertRaises(SmokeFailure): self.validate(p)
