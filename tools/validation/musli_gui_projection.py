"""Exact additive Musli GUI registration projection; historical GUI pins stay unchanged."""
import hashlib

PATH = "src/main/java/com/oblixorprime/engineersdecorreforged/client/ClientModEvents.java"
BASE_SHA256 = "bf327e4fa3ff6b32afa1ec80c03c23b2c372ff8470a9181e86351cf29feba29c"
CURRENT_SHA256 = "ac9a5df8fcd683e12694cb75853c5250d6d4330244f6d82d400e6e391c06d4e4"
ADDED = b"      event.register((MenuType)ModMenus.MUSLI_BAR_PRESS.get(), MusliBarPressScreen::new);\n"


def project_history(path, source):
    if path != PATH:
        return source
    digest = hashlib.sha256(source).hexdigest()
    if digest == BASE_SHA256:
        return source
    assert digest == CURRENT_SHA256, "Unreviewed Musli GUI client registration change"
    assert source.count(ADDED) == 1, "Missing or duplicate Musli GUI registration"
    historic = source.replace(ADDED, b"", 1)
    assert hashlib.sha256(historic).hexdigest() == BASE_SHA256, "Historical client registration altered"
    return historic


def current_hash(path, baseline):
    if path != PATH:
        return baseline
    assert baseline == BASE_SHA256, "Unexpected Musli GUI registration baseline"
    return CURRENT_SHA256
