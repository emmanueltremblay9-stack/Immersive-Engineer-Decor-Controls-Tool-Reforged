"""Pinned official renderer dependencies for isolated development profiles only."""
from pathlib import Path
import hashlib
import urllib.request
from client_mixin_smoke import SmokeFailure

PROFILES = {
    "native": {},
    "embeddium": {"embeddium": "1.0.15+mc1.21.1"},
    "modernfix": {"modernfix": "5.27.15+mc1.21.1"},
    "both": {"embeddium": "1.0.15+mc1.21.1", "modernfix": "5.27.15+mc1.21.1"},
}
OFFICIAL = {
    "embeddium": ("https://cdn.modrinth.com/data/sk9rgfiA/versions/J7b96IEd/embeddium-1.0.15%2Bmc1.21.1.jar",
        "d073cf52dcf2aeec3ac9f746d4571c7b4b1ad746fa11d4bcc5a21a264ae3119ac6d341d08cc9a70042cf0ddffc3b3ef3329ed30217035d4551c9d6788b6dc1e6", 1048745),
    "modernfix": ("https://cdn.modrinth.com/data/nmDcB62a/versions/ZkPOaQLQ/modernfix-neoforge-5.27.15%2Bmc1.21.1.jar",
        "3fa670a90b59c35fdbd1b06f74783e6bb60aff29ebf222c06cd5355b31fac66a6192ec10a2004030306c1f33f7715ff126fe78874e016cb22d226481ab456870", 597318),
}

def prepare_renderer_dependencies(directory: Path, profile: str) -> list[str]:
    directory.mkdir()
    paths = []
    for mod in PROFILES[profile]:
        url, digest, size = OFFICIAL[mod]
        request = urllib.request.Request(url, headers={"User-Agent": "IEDCTR-isolated-renderer-validation/1"})
        with urllib.request.urlopen(request, timeout=45) as response:
            if not response.url.startswith("https://cdn.modrinth.com/"):
                raise SmokeFailure("Renderer dependency redirected away from the official CDN")
            data = response.read(size + 1)
        if len(data) != size or hashlib.sha512(data).hexdigest() != digest:
            raise SmokeFailure("Official renderer dependency size/checksum mismatch: " + mod)
        path = directory / (mod + ".jar")
        path.write_bytes(data)
        paths.append(str(path.resolve()))
    return paths

def validate_renderer_profile(proof: dict, profile: str) -> None:
    if proof.get("orientation") != "east" or proof.get("renderer_mods") != PROFILES[profile]:
        raise SmokeFailure("Renderer profile orientation or exact loaded-mod versions mismatch")
    for case in proof.get("cases", []):
        if case.get("case", "").startswith("stairs-"):
            facing = "west" if case["case"] == "stairs-railed-reversed-wet" else "east"
            if f'facing:"{facing}"' not in case.get("block_state", ""):
                raise SmokeFailure("Stair orientation does not match east-oriented fixture")
