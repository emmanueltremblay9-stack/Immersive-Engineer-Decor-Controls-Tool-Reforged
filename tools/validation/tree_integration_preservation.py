"""Exact PR173/PR172 integration exceptions, not broad exclusions from preservation checks."""
import hashlib
import json


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


def assert_menu_tail_preserved(source):
    """Normalize only the exact Tree accessor and complete Freezer preview method."""
    marker = '   private static MenuType<?> resolveMenuType'
    assert source.count(marker) == 1, 'Ambiguous menu preservation boundary'
    tail = source.split(marker, 1)[1]
    assert tail.count(TREE_ACCESSOR) == 1, 'Missing or changed Tree presentation accessor'
    assert tail.count(FREEZER_PREVIEW) == 1, 'Missing or changed Freezer preview method'
    normalized = tail.replace(TREE_ACCESSOR, '', 1).replace(FREEZER_PREVIEW, HISTORICAL_FREEZER_PREVIEW, 1)
    assert hashlib.sha256(normalized.encode()).hexdigest() == MENU_TAIL_SHA256, 'Menu transfer/data/validation changed'


def assert_solar_baseline_preserved(root, preserved):
    """Retain historical pins and admit only exact documented integration identities."""
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
    assert freezer['main_commit'] == FREEZER_MAIN_COMMIT
    assert freezer['freezer_commit'] == FREEZER_COMMIT
    freezer_overrides = freezer['files']
    assert set(freezer_overrides) == set(FREEZER_FILES), 'Unexpected Freezer integration exception'
    for path, expected_record in FREEZER_FILES.items():
        record = freezer_overrides[path]
        for key, expected in expected_record.items():
            assert record[key] == expected, (path, key)
        assert isinstance(record['reason'], str) and record['reason'].strip(), path
        assert hashlib.sha256((root / path).read_bytes()).hexdigest() == expected_record['integrated_sha256'], path
    for path, expected in files.items():
        if path in overrides:
            expected = overrides[path]['integrated_sha256']
        if path in FREEZER_FILES:
            expected = FREEZER_FILES[path]['integrated_sha256']
        assert hashlib.sha256((root / path).read_bytes()).hexdigest() == expected, path
