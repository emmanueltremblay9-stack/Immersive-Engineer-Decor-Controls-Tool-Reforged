"""Exact Tree/Freezer/Milking integration layers, never broad preservation exclusions."""
import hashlib
import json
from freezer_v5_reconciliation import LAYER as V5_LAYER, assert_freezer_v5_reconciliation, current_hash


V5_ACCESSOR = '   public boolean freezerReferencePresentation() {\n      return this.freezerReferencePresentation;\n   }\n'
V5_RESERVED_SLOT = '      if (this.kind == MachineKind.SMALL_FREEZER && slot.containerIndex() == 1) {\n         return new MachineMenu.ValidatedMachineSlot(this.container, 1, slot.x(), slot.y(), true) {\n            @Override\n            public boolean isActive() {\n               // Hide the historical reserved opening only in the opt-in presentation.\n               // Its backing contents and ordinary QUICK_MOVE recovery remain intact.\n               return !MachineMenu.this.freezerReferencePresentation;\n            }\n         };\n      }\n'

MENU_TAIL_SHA256 = 'f471ad91c8d8acfabc55d73a19a21807cb7cf1e41052a369bb91a50708772633'
TREE_ACCESSOR = ('   public boolean treeCutterV3ReferencePresentation() '
                 '{ return this.treeCutterV3ReferencePresentation; }\n\n')
TREE_COMMIT = '96ac07aac200e10e9936b0ae4ae99f9053fae11c'
MAIN_COMMIT = '890958ff300e9a88dde2a77fc6c145d19c0f98f9'
UTILITY = 'src/main/java/com/oblixorprime/engineersdecorreforged/utility/'
TREE_FILES = {
    UTILITY + 'MachineBlockEntity.java': '34e971011cfeacd68695fe756fd6c4f0dd67b1a65d82d5af6ef5adb1e45fafa7',
    UTILITY + 'MachineLayout.java': '6e56fb8b448c1b19c65cdb3663aeb34ccbdc7ba88f778dc2e5ee79ba8577642b',
}
BARREL_GUARD = 'tools/validation/check_barrel_reference_wiring.py'
INTEGRATION_FILES = {
    **TREE_FILES,
    BARREL_GUARD: '89128c598d2e698f4f670b0551b8f918661fca3b836100890873853ec76bc7c3',
}
MILKING_FILES = {
    'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java',
    UTILITY + 'MachineBlockEntity.java',
}
MILKING_UPSTREAM_INTEGRATED_FILES = {
    'src/main/java/com/oblixorprime/engineersdecorreforged/client/ClientModEvents.java': '3f32ffeccd725fe8d54b95f5a54ae11acb8a31bfb124172bee60bbf420977723',
    UTILITY + 'MachineLayout.java': TREE_FILES[UTILITY + 'MachineLayout.java'],
}

# PR172 is a separate layer: never rewrite the historical Solar or Tree pins.
FREEZER_MAIN_COMMIT = '509913bd891e7a7bfd7dfa589a3b1fa1008e0397'
FREEZER_COMMIT = '9bfd466766ac66af759600ea74ff5e039daba3d5'
MACHINE_ENTITY = UTILITY + 'MachineBlockEntity.java'
MACHINE_MENU = 'src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java'
FREEZER_FILES = {
    MACHINE_ENTITY: {
        'main_sha256': '34e971011cfeacd68695fe756fd6c4f0dd67b1a65d82d5af6ef5adb1e45fafa7',
        'freezer_sha256': '9ff3858f58c4b3c81aaf01760124a72aea622241a793248fb99846cc4afd692f',
        'integrated_sha256': '9a6678406008a817dd544df65f259eade10cc91c37a52bdca84f6ada18e88c63',
    },
    MACHINE_MENU: {
        'main_sha256': 'f3457bbabf3a0e92874e53cdbf16739d35b4fe50050218750551251d454760ae',
        'freezer_sha256': '720e6b2f6b439f9f29f576965cc9bcad0830dec0d085f06c46dcbd9a45dccc6f',
        'integrated_sha256': '903cc15bbce612d9b5c7f150426fdce52858d63769eca9f7cdc16d6b43fb1ae2',
    },
}
FREEZER_PREVIEW = """   public ItemStack freezerPreviewStack() {
      if (this.kind != MachineKind.SMALL_FREEZER || this.visibleMachineSlots == 0) return ItemStack.EMPTY;
      ItemStack input = ((Slot)this.slots.get(0)).getItem();
      // Field 2 was unused for the Freezer; preserve the existing 16-field/39-slot menu protocol.
      return input.isEmpty() && this.data.get(2) >= 1000
         ? new ItemStack(Items.ICE) : MachineBlockEntity.freezerResultForDisplay(input);
   }

"""
HISTORICAL_FREEZER_PREVIEW = """   public ItemStack freezerPreviewStack() {
      return this.kind == MachineKind.SMALL_FREEZER && this.visibleMachineSlots > 0
         ? MachineBlockEntity.freezerResultForDisplay(((Slot)this.slots.get(0)).getItem())
         : ItemStack.EMPTY;
   }

"""


# PR169 + PR172 is a new layer, not a rewrite of either parent snapshot.
COMBINED_LAYER = 'milking_freezer_integration'
COMBINED_FILES = {
    MACHINE_ENTITY: {
        'main_sha256': '9a6678406008a817dd544df65f259eade10cc91c37a52bdca84f6ada18e88c63',
        'milking_sha256': 'b3733c48d66419efeb818a2992ff67144642ddf36a08652c0394e8d45e78efd9',
        'integrated_sha256': '24266c03ec35e12517d8b8f86d1ef53bf1a3e9d266666d38e645045155999128',
    },
    MACHINE_MENU: {
        'main_sha256': '903cc15bbce612d9b5c7f150426fdce52858d63769eca9f7cdc16d6b43fb1ae2',
        'milking_sha256': '0be659f78a3176707ea292e46a382df180ea220fa2039f6bf4ccbfd839785606',
        'integrated_sha256': 'a8a3d691e191bf4dc5a9725e7c9e4e9b0e19b8647a5a6c1408e2f2a92dcc9fdd',
    },
}
MILKING_CURRENT_HASHES = {
    'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java': '2b0f8cc8bfeac9f421b6948833e4d88b7c993c5c633b6f26feebc6f96f2a0e6f',
    MACHINE_ENTITY: COMBINED_FILES[MACHINE_ENTITY]['integrated_sha256'],
}
COMBINED_RECORD_SHA256 = '0c3b5b944b410bbdf51fc1217c4ee38d8873b8a1e5d197bc7ef9c95622dfa01b'
MANUAL_RECORD_SHA256 = '75042ba4e5dae1de9c8ec5a768918d0ef36e1acb4848ebdcd02858317a5bcf41'
MANUAL_LANGUAGE_PATH = 'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/lang/fr_fr.json'
MANUAL_PREVIOUS_SHA256 = 'f415594130353f155a9fd9a9c26e40cacbf3dc618a07d1dfa8f1d73609881763'
MANUAL_INTEGRATED_SHA256 = '5f737152b4a5e1c372b28c21324fa3287c2896bf7a594146abc1e0541d8e482b'
MILKING_HISTORY_SHA256 = '98738d898787992b47a95c442899641d11b2ac0c8453294c6383c7c01f26c7f1'
FREEZER_HISTORY_SHA256 = 'c2575fe1829e86641cb2b21af54bbbf301cdccab789aaa78ca6f624124bec1c4'


def record_sha256(record):
    return hashlib.sha256(json.dumps(record, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


def assert_combined_integration_preserved(root, manifest):
    """Pin both parent identities and current bytes independently of editable manifests."""
    assert_freezer_v5_reconciliation(root, manifest)
    combined = manifest.get(COMBINED_LAYER)
    assert record_sha256(combined) == COMBINED_RECORD_SHA256, 'Combined Milking/Freezer integration changed'
    for path, record in COMBINED_FILES.items():
        assert record['main_sha256'] == FREEZER_FILES[path]['integrated_sha256'], path
        assert hashlib.sha256((root / path).read_bytes()).hexdigest() == current_hash(path, record['integrated_sha256']), path


def assert_milking_integration_preserved(root, pins):
    assert_combined_integration_preserved(root, pins)
    # Reconstruct the two previous Java pins and the explicitly pinned manual
    # language integration. Baselines, renderer history, Tree provenance and
    # unaffected current pins stay exact.
    history = {key: value for key, value in pins.items() if key not in (COMBINED_LAYER, 'manual_integration', V5_LAYER)}
    history['sha256'] = dict(pins['sha256'])
    if 'manual_integration' in pins:
        assert record_sha256(pins['manual_integration']) == MANUAL_RECORD_SHA256, 'Manual integration provenance changed'
        assert pins['sha256'].get(MANUAL_LANGUAGE_PATH) == MANUAL_INTEGRATED_SHA256, 'Manual integration pin changed'
        assert hashlib.sha256((root / MANUAL_LANGUAGE_PATH).read_bytes()).hexdigest() == current_hash(MANUAL_LANGUAGE_PATH, MANUAL_INTEGRATED_SHA256), 'Manual integration bytes changed'
        history['sha256'][MANUAL_LANGUAGE_PATH] = MANUAL_PREVIOUS_SHA256
    for path, record in COMBINED_FILES.items():
        assert pins['sha256'].get(path) == record['integrated_sha256'], path
        history['sha256'][path] = record['milking_sha256']
    assert record_sha256(history) == MILKING_HISTORY_SHA256, 'Historical Milking preservation changed'


def assert_menu_tail_preserved(source):
    """Normalize only the exact Tree accessor and complete Freezer preview method."""
    marker = '   private static MenuType<?> resolveMenuType'
    assert source.count(marker) == 1, 'Ambiguous menu preservation boundary'
    tail = source.split(marker, 1)[1]
    assert tail.count(V5_ACCESSOR) == 1, 'Missing or changed Freezer V5 accessor'
    assert tail.count(V5_RESERVED_SLOT) == 1, 'Missing or changed Freezer V5 reserved slot'
    tail = tail.replace(V5_ACCESSOR, '', 1).replace(V5_RESERVED_SLOT, '', 1)
    assert tail.count(TREE_ACCESSOR) == 1, 'Missing or changed Tree presentation accessor'
    assert tail.count(FREEZER_PREVIEW) == 1, 'Missing or changed Freezer preview method'
    normalized = tail.replace(TREE_ACCESSOR, '', 1).replace(FREEZER_PREVIEW, HISTORICAL_FREEZER_PREVIEW, 1)
    assert hashlib.sha256(normalized.encode()).hexdigest() == MENU_TAIL_SHA256, 'Menu transfer/data/validation changed'


def assert_solar_baseline_preserved(root, preserved, milking_current_hashes=None):
    """Retain historical pins and admit only exact documented integration identities."""
    milking_current_hashes = milking_current_hashes or {}
    assert set(milking_current_hashes) == MILKING_FILES, 'Unexpected Milking current-hash allowlist'
    assert milking_current_hashes == MILKING_CURRENT_HASHES, 'Milking current hashes changed'
    assert_combined_integration_preserved(root, preserved)
    assert preserved['remote_baseline_commit'] == '15593b28a6853ba36775ebb419b8465a5e2f3240'
    assert preserved['baseline_tree'] == '6dc6f3185624808ce7127331bb2f62c26f102d34'
    files = preserved['files']
    historical = json.dumps(files, sort_keys=True, separators=(',', ':')).encode()
    assert hashlib.sha256(historical).hexdigest() == '8dfad5336a1c39a48481ecfeeb691390e9ea9e3cd80bbfd5811435329c5d3b52', 'Historical preservation pins changed'
    integration = preserved['tree_cutter_integration']
    tree_history = json.dumps(integration, sort_keys=True, separators=(',', ':')).encode()
    assert hashlib.sha256(tree_history).hexdigest() == 'aabd54c402fcda39b953931212bdbcdfadaa538017ca37177727e05790ff51d7', 'Historical Tree integration changed'
    assert integration['main_commit'] == MAIN_COMMIT
    assert integration['tree_commit'] == TREE_COMMIT
    overrides = integration['files']
    assert set(overrides) == set(INTEGRATION_FILES), 'Unexpected integration exception'
    for path, record in overrides.items():
        assert record['baseline_sha256'] == files[path], path
        assert isinstance(record['reason'], str) and record['reason'].strip(), path
        assert record['integrated_sha256'] == INTEGRATION_FILES[path], path
    freezer = preserved['freezer_fluid_automation_integration']
    assert record_sha256(freezer) == FREEZER_HISTORY_SHA256, 'Historical Freezer integration changed'
    assert freezer['main_commit'] == FREEZER_MAIN_COMMIT
    assert freezer['freezer_commit'] == FREEZER_COMMIT
    freezer_overrides = freezer['files']
    assert set(freezer_overrides) == set(FREEZER_FILES), 'Unexpected Freezer integration exception'
    for path, expected_record in FREEZER_FILES.items():
        record = freezer_overrides[path]
        for key, expected in expected_record.items():
            assert record[key] == expected, (path, key)
        assert isinstance(record['reason'], str) and record['reason'].strip(), path
        # These are historical PR172 bytes. Current combined bytes were checked
        # independently above, against the new exact integration identity.
    for path, expected in files.items():
        if path in overrides:
            expected = overrides[path]['integrated_sha256']
        if path in milking_current_hashes:
            expected = milking_current_hashes[path]
        if path in COMBINED_FILES:
            expected = COMBINED_FILES[path]['integrated_sha256']
        assert hashlib.sha256((root / path).read_bytes()).hexdigest() == current_hash(path, expected), path
