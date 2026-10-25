"""Pinned official Lithium dependency for explicit, isolated compatibility tests."""
import argparse
import hashlib
import json
from pathlib import Path
import urllib.request

from client_mixin_smoke import SmokeFailure

VERSION = "0.15.4+mc1.21.1"
URL = "https://cdn.modrinth.com/data/gvQqBUqZ/versions/DDUrRVCA/lithium-neoforge-0.15.4%2Bmc1.21.1.jar"
SHA512 = "2735da2088b88a8bdcd4ad02a2b6fffbfd3925557cefd4fa54b5477dfb9e582ea7f521300c060f57dff1325dd21bff276b7333363ad3371ed6893e3de9eca9cd"
SHA256 = "488f33216030c1cb0baa33e59de3d657d08d5795a6c2080c8c8bbbc133fc8757"
SIZE = 774220


def prepare_lithium_dependency(directory: Path) -> str:
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / "lithium.jar"
    if path.exists():
        data = path.read_bytes()
    else:
        request = urllib.request.Request(URL, headers={"User-Agent": "IEDCTR-isolated-Lithium-validation/1"})
        with urllib.request.urlopen(request, timeout=45) as response:
            if not response.url.startswith("https://cdn.modrinth.com/"):
                raise SmokeFailure("Lithium redirected away from the official CDN")
            data = response.read(SIZE + 1)
    if len(data) != SIZE or hashlib.sha512(data).hexdigest() != SHA512 or hashlib.sha256(data).hexdigest() != SHA256:
        raise SmokeFailure("Official Lithium dependency size/checksum mismatch")
    if not path.exists():
        path.write_bytes(data)
    (directory / "identity.json").write_text(json.dumps({"version": VERSION, "url": URL,
        "sha256": SHA256, "sha512": SHA512, "size": SIZE}, indent=2) + "\n", encoding="utf-8")
    return str(path.resolve())


def validate_lithium_runtime(logs: str, nonce: str) -> None:
    if f"IEDCTR_LITHIUM_COMPAT version={VERSION} bfs=true nonce={nonce}" not in logs:
        raise SmokeFailure("No matching proof of the exact Lithium version with its fluid BFS enabled")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", type=Path)
    args = parser.parse_args()
    jar = prepare_lithium_dependency(args.directory)
    game = (args.directory / "gametest-run").resolve()
    if game.exists():
        raise SystemExit("Refusing to reuse an existing GameTest world")
    def quote(path):
        return "'" + str(path).replace("\\", "\\\\").replace("'", "\\'") + "'"
    init = """// Explicit test-only dependency; never part of production build.gradle.
gradle.projectsEvaluated {
    rootProject.dependencies.add('runtimeOnly', rootProject.files(%s))
    rootProject.extensions.getByName('neoForge').runs.named('gameTestServer') {
        gameDirectory.set(rootProject.file(%s))
        systemProperty 'iedctr.requireLithiumFluidBfs', 'true'
    }
}
""" % (quote(jar), quote(game))
    (args.directory / "gametest.init.gradle").write_text(init, encoding="utf-8")
    print(json.dumps({"jar": jar, "sha256": SHA256, "init_script": str((args.directory / "gametest.init.gradle").resolve())}))


if __name__ == "__main__":
    main()
