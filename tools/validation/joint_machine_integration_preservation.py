"""Exact PR179 + PR169 reconciliation; immutable parent and current identities."""
import hashlib
import json

JOINT_LAYER = 'breaker_milking_integration'
JOINT_INTEGRATION = {'main_commit': '28a70f534323511e38dcc99453d4019af9576a77',
 'breaker_commit': '0ab0138467fd6146e16cf6a9bbf79d5f18185175',
 'scope': 'Exact automatic Java and language integration of PR179 with merged PR169. Preserve both parent '
          'snapshots and all historical layers. No gameplay changes or native qualification claimed.',
 'files': {'src/main/java/com/oblixorprime/engineersdecorreforged/client/MachineScreen.java': {'main_sha256': '2b0f8cc8bfeac9f421b6948833e4d88b7c993c5c633b6f26feebc6f96f2a0e6f',
                                                                                               'breaker_sha256': 'cb52445a0f4373a7fabf51d12a608c5e8620890c3aceb027895abd0df9c31884',
                                                                                               'integrated_sha256': '2c20b39756c1dc42df8c610d7b2d755fb3d6c52f4e0e4c16a6ddedd91191b3b2'},
           'src/main/java/com/oblixorprime/engineersdecorreforged/client/RedesignedMachineArtwork.java': {'main_sha256': '76436f4e818fbd8bf2d1cc62a69f75746d67ace911c06efdac476cf2e4f8036c',
                                                                                                          'breaker_sha256': 'ad87ca5103883b457a30c3d41ee68f6b234fbaafac57b00386fae0e60f5bf92f',
                                                                                                          'integrated_sha256': '29a3c271ab8edac6e2c032279320266b94db3a28d044576d1771edf8fe530267'},
           'src/main/java/com/oblixorprime/engineersdecorreforged/menu/MachineMenu.java': {'main_sha256': 'a8a3d691e191bf4dc5a9725e7c9e4e9b0e19b8647a5a6c1408e2f2a92dcc9fdd',
                                                                                           'breaker_sha256': '9b78022405e584f41da11e03999cad0b14d5563a9c2636d55441cee874488b14',
                                                                                           'integrated_sha256': '15803c81755ece86eda38308c3fb1bae5dc4a4b21e46eb188fd92a1e781ea3c3'},
           'src/main/java/com/oblixorprime/engineersdecorreforged/utility/MachineBlockEntity.java': {'main_sha256': '24266c03ec35e12517d8b8f86d1ef53bf1a3e9d266666d38e645045155999128',
                                                                                                     'breaker_sha256': '1ab112cfb93c2817c68edae262aed72d9fc91008851cc433a9dc3693f11beb29',
                                                                                                     'integrated_sha256': '3dfb25d8e8d818f6436983b921b2db18d2644be4b28e38ad1d3d0fae4dbe795e'},
           'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/lang/en_us.json': {'main_sha256': 'b6ca6454118504f44a918a2b248fff649ee35ff7f2352389302f0d51a366c500',
                                                                                                         'breaker_sha256': '9028af47cc53509e470db849b290d4580a15401822e523c4356a1843cba02d59',
                                                                                                         'integrated_sha256': '04ef6cd7d2477232a8d3f12cd0a88643919c477231d58f908713301b9f4b5846'},
           'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/lang/fr_fr.json': {'main_sha256': '5f737152b4a5e1c372b28c21324fa3287c2896bf7a594146abc1e0541d8e482b',
                                                                                                         'breaker_sha256': '3611fbedb5d88a1775c2737d2da4a6f96d579c62491f576e74f8b395c9f6e51f',
                                                                                                         'integrated_sha256': 'd9fa4353e1436583d6d3a0786ffae9daf42ee33500c7f7c52fdf7ed2ad6a5a97'}}}
JOINT_FILES = JOINT_INTEGRATION['files']

def assert_joint_integration_preserved(root, manifest):
    assert manifest.get(JOINT_LAYER) == JOINT_INTEGRATION, 'Historical Milking/Breaker joint integration changed'
    for path, record in JOINT_FILES.items():
        assert hashlib.sha256((root / path).read_bytes()).hexdigest() == record['integrated_sha256'], path

def integrated_hash(path, previous):
    return JOINT_FILES[path]['integrated_sha256'] if path in JOINT_FILES else previous
