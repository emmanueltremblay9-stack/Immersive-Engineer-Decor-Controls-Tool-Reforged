"""Synthetic negative controls for packaged evidence; no native game claims."""
import copy
import hashlib
from pathlib import Path
import tempfile
import unittest
import zipfile

import factory_placer_delivery_evidence as delivery
from client_mixin_smoke import SmokeFailure
from factory_placer_v2_reference_native import DEFAULT_SOURCE, ART_SHA


def snapshot(counts, carried=0, dropped=0):
    return dict(menu_id=2, logic=0, selected_slot=0, signal=0, carried_count=carried,
                world_drop_items=dropped, item_total=69, hidden_sentinels_intact=True,
                slot_counts=copy.deepcopy(counts))


def gestures():
    result = []
    before = delivery.INITIAL_SLOTS
    for index, name in enumerate(delivery.GESTURES):
        steps = []
        for number, (counts, carried, dropped) in enumerate(delivery.expected_steps(index)):
            step = snapshot(counts, carried, dropped)
            step.update(step=number, server_ack=True)
            control = number if index in (4, 5) else number - 1 if index == 6 and 1 <= number <= 6 else None
            if control is not None:
                step['control'] = delivery.CONTROLS[control]
            steps.append(step)
        record = dict(case=name, before=snapshot(before), after=copy.deepcopy(steps[-1]),
                      steps=steps, server_confirmed=True, server_ack=True,
                      item_total_before=69, item_total_after=69, carried_count=0,
                      world_drop_items=24 if index == 7 else 0, slot_counts=copy.deepcopy(delivery.AFTER_SLOTS[index]),
                      control_observations=[dict(step, server_observed_unchanged=True, causal_packet_receipt_proven=False)
                                            for step in steps if 'control' in step],
                      input_method='Screen.mouseClicked/mouseReleased; physical XTest Shift observed through GLFW',
                      ack_scope='independent authoritative unchanged-state observation; does not prove receipt of a rejected packet',
                      native_shift_observed=True)
        result.append(record)
        before = delivery.AFTER_SLOTS[index]
    return {'advanced_gestures': result}


class AdvancedEvidenceTest(unittest.TestCase):
    def test_complete_authoritative_sequence(self):
        delivery.validate_gestures(gestures())

    def test_missing_duplicate_reordered_case(self):
        for change in ('missing', 'duplicate', 'reorder'):
            p = gestures()
            rows = p['advanced_gestures']
            if change == 'missing': rows.pop()
            elif change == 'duplicate': rows[-1] = copy.deepcopy(rows[0])
            else: rows[0], rows[1] = rows[1], rows[0]
            with self.subTest(change=change), self.assertRaises(SmokeFailure):
                delivery.validate_gestures(p)

    def test_each_snapshot_must_conserve_slots_carried_drops_and_hidden(self):
        changes = dict(item_total=70, world_drop_items=0, hidden_sentinels_intact=1,
                       menu_id=3, logic=4, selected_slot=1, signal=1, carried_count=1)
        for key, value in changes.items():
            p = gestures(); p['advanced_gestures'][-1]['after'][key] = value
            with self.subTest(key=key), self.assertRaises(SmokeFailure):
                delivery.validate_gestures(p)

    def test_each_intermediate_ack_required(self):
        for field in ('server_ack', 'step', 'slot_counts', 'carried_count'):
            p = gestures(); p['advanced_gestures'][0]['steps'][0].pop(field)
            with self.subTest(field=field), self.assertRaises(SmokeFailure):
                delivery.validate_gestures(p)

    def test_invalid_input_all_six_controls_required(self):
        for index in (4, 5, 6):
            p = gestures(); step = p['advanced_gestures'][index]['steps'][1]
            step['control'] = 'invented'
            with self.subTest(case=index), self.assertRaises(SmokeFailure):
                delivery.validate_gestures(p)

    def test_invalid_input_cannot_claim_causal_receipt(self):
        p = gestures(); p['advanced_gestures'][4]['ack_scope'] = 'packet received'
        with self.assertRaises(SmokeFailure): delivery.validate_gestures(p)

    def test_independent_control_observation_cannot_claim_packet_receipt(self):
        p = gestures(); p['advanced_gestures'][4]['control_observations'][0]['causal_packet_receipt_proven'] = True
        with self.assertRaises(SmokeFailure): delivery.validate_gestures(p)

    def test_shift_needs_observed_physical_modifier(self):
        p = gestures(); p['advanced_gestures'][2]['native_shift_observed'] = 1
        with self.assertRaises(SmokeFailure): delivery.validate_gestures(p)

    def test_numeric_booleans_rejected(self):
        p = gestures(); p['advanced_gestures'][0]['before']['slot_counts']['2'] = False
        with self.assertRaises(SmokeFailure): delivery.validate_gestures(p)

    def test_complete_long_viewport_outline_sample_regions(self):
        for selected in range(18):
            edges = delivery.selection_edges(1280, 720, 1, selected)
            self.assertEqual(set(edges), {'top', 'right', 'bottom', 'left'})
            self.assertTrue(all(edges.values()))

    def test_short_viewport_missing_edges_not_invented(self):
        missing = [(slot, name) for slot in range(18)
                   for name, values in delivery.selection_edges(1280, 260, 1, slot).items() if not values]
        self.assertTrue(missing, 'Current one-logical-pixel geometry has documented subpixel gaps')

    def test_translated_tooltip_expectations(self):
        project = Path(__file__).resolve().parents[2]
        for language in ('en_us', 'fr_fr', 'fr_ca'):
            texts = delivery.expected_tooltip_texts(project, language)
            self.assertEqual(tuple(texts), delivery.TARGETS)
            self.assertTrue(all('%s' not in text for text in texts.values()))
            self.assertIn('18', texts['ROW_BOTTOM'])
        self.assertNotEqual(delivery.expected_tooltip_texts(project, 'en_us')['HEADER'],
                            delivery.expected_tooltip_texts(project, 'fr_ca')['HEADER'])


class PackagedIdentityTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(); self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.jar = self.root / 'runtime.jar'
        classes = ('EngineersDecorReforged', 'client.FactoryPlacerV2ReferenceScreen',
                   'client.MachineScreen$FactoryPlacerScreen', 'menu.MachineMenu',
                   'utility.FactoryPlacerV2ReferenceGeometry', 'utility.MachineBlockEntity')
        classes = ['com.oblixorprime.engineersdecorreforged.' + name for name in classes]
        # Explicitly fake class bytes test provenance matching, not Java execution.
        self.entries = {name.replace('.', '/') + '.class': ('fake class ' + name).encode() for name in classes}
        self.entries[delivery.ART_ENTRY] = DEFAULT_SOURCE.read_bytes()
        with zipfile.ZipFile(self.jar, 'w') as archive:
            for name, data in self.entries.items(): archive.writestr(name, data)
        sha = hashlib.sha256(self.jar.read_bytes()).hexdigest()
        uri = 'union:/isolated/runtime.jar%230!/'
        self.identity = dict(jar_path=str(self.jar), jar_sha256=sha, mod_root_uri=uri,
            harness_mod_id=delivery.HARNESS_ID, harness_mod_path=str(self.root / 'harness'),
            fixture_free_jar=True, separate_harness_module=True,
            active_texture_sha256=ART_SHA, active_texture_pack='mod/' + delivery.MOD_ID,
            class_origins=[dict(**{'class': name}, resource_uri=uri + name.replace('.', '/') + '.class',
                 code_source_uri=uri, sha256=hashlib.sha256(self.entries[name.replace('.', '/') + '.class']).hexdigest()) for name in classes])
        self.classpath = dict(jar_path=str(self.jar), jar_sha256=sha, fixture_free_jar=True, split_packages=False,
            loaded_development_mods=[delivery.HARNESS_ID], runtime_classpath=[str(self.jar), str(self.root / 'harness')],
            forbidden_main_outputs=[str(self.root / 'main')], harness_outputs=[str(self.root / 'harness')])

    def validate(self):
        return delivery.validate_packaged_runtime(self.identity, self.jar, self.classpath)

    def test_matching_packaged_identity(self):
        self.assertEqual(self.validate(), self.identity['jar_sha256'])

    def test_different_jar_hash_rejected(self):
        self.identity['jar_sha256'] = '0' * 64
        with self.assertRaises(SmokeFailure): self.validate()

    def test_main_output_cannot_shadow_jar(self):
        self.classpath['runtime_classpath'].extend(self.classpath['forbidden_main_outputs'])
        with self.assertRaises(SmokeFailure): self.validate()

    def test_wrong_development_mod_rejected(self):
        self.classpath['loaded_development_mods'].append(delivery.MOD_ID)
        with self.assertRaises(SmokeFailure): self.validate()

    def test_runtime_class_origin_is_not_a_filename_substring(self):
        self.identity['class_origins'][0]['resource_uri'] = 'file:/other/runtime.jar/classes/EngineersDecorReforged.class'
        with self.assertRaises(SmokeFailure): self.validate()

    def test_semantically_equal_file_code_source_uri(self):
        self.identity['class_origins'][0]['code_source_uri'] = self.jar.as_uri().replace('file:///', 'file:/', 1)
        self.validate()

    def test_origin_query_fragment_and_different_root_rejected(self):
        original = self.identity['class_origins'][0]['code_source_uri']
        for suffix in ('?other=1', '#other', '../other/'):
            self.identity['class_origins'][0]['code_source_uri'] = original + suffix
            with self.subTest(suffix=suffix), self.assertRaises(SmokeFailure): self.validate()

    def test_loaded_class_hash_must_match_jar(self):
        self.identity['class_origins'][0]['sha256'] = '0' * 64
        with self.assertRaises(SmokeFailure): self.validate()

    def test_texture_override_rejected(self):
        self.identity['active_texture_pack'] = 'file/resourcepacks/replacement.zip'
        with self.assertRaises(SmokeFailure): self.validate()

    def test_class_origin_matrix_must_be_complete(self):
        self.identity['class_origins'].pop()
        with self.assertRaises(SmokeFailure): self.validate()

    def test_fixture_in_normal_jar_rejected_even_when_hash_matches(self):
        with zipfile.ZipFile(self.jar, 'a') as archive:
            archive.writestr('com/oblixorprime/iedctrtest/Fake.class', b'fake')
        sha = hashlib.sha256(self.jar.read_bytes()).hexdigest()
        self.identity['jar_sha256'] = self.classpath['jar_sha256'] = sha
        with self.assertRaises(SmokeFailure): self.validate()


if __name__ == '__main__':
    unittest.main()
