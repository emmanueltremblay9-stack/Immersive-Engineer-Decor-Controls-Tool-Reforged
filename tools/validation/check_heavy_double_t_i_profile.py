"""Check the runtime I profile against the immutable MK3 review assets."""
from pathlib import Path
import json
import hashlib

ROOT = Path(__file__).resolve().parents[2]
REVIEW = ROOT / "docs/candidates/heavy-double-t-mk3-mk31/review/src/main/resources"
RUNTIME = ROOT / "src/main/resources"
MOD = "immersive_engineer_decor_controls_tool_reforged"
MODEL_DIR = Path(f"assets/{MOD}/models/block/heavy_double_t")
STEEL_TILE = Path(f"assets/{MOD}/textures/block/heavy_double_t/steel_side_continuous.png")
STEEL_SIDE = f"{MOD}:block/heavy_double_t/steel_side_continuous"
STEEL_TILE_SHA256 = "4f6d6224a4f179b351583574d2afa37ce38f773b11160594b0c4d474c71ea2bc"
OPPOSITE = dict(up="down", down="up", north="south", south="north", east="west", west="east")


def load(path):
    return json.loads(path.read_bytes())


def expected_end_face(element, side):
    x, y, z = element["from"]
    x1, y1, z1 = element["to"]
    uv = {
        "down": [x, 16-z1, x1, 16-z], "up": [x, z, x1, z1],
        "north": [16-x1, 16-y1, 16-x, 16-y], "south": [x, 16-y1, x1, 16-y],
        "west": [z, 16-y1, z1, 16-y], "east": [16-z1, 16-y1, 16-z, 16-y],
    }[side]
    return {"uv": uv, "texture": "#end", "cullface": side}


def main():
    baseline_paths = {p.relative_to(REVIEW) for p in REVIEW.rglob("*.json")}
    expected_models = {MODEL_DIR / f"core_{facing}_{roll}.json" for facing in OPPOSITE for roll in range(4)}
    assert len(baseline_paths) == 87 and expected_models <= baseline_paths
    assert {p.relative_to(RUNTIME) for p in (RUNTIME / MODEL_DIR).glob("*.json")} == expected_models

    assert hashlib.sha256((RUNTIME / STEEL_TILE).read_bytes()).hexdigest() == STEEL_TILE_SHA256

    for path in baseline_paths:
        if path.parent != MODEL_DIR:
            assert (RUNTIME / path).read_bytes() == (REVIEW / path).read_bytes(), path
        elif path in expected_models:
            _, facing, _ = path.stem.split("_")
            expected = load(REVIEW / path)
            expected["textures"].update(side=STEEL_SIDE, particle=STEEL_SIDE)
            expected["elements"] = expected["elements"][:3]
            for element in expected["elements"]:
                for side in (facing, OPPOSITE[facing]):
                    element["faces"][side] = expected_end_face(element, side)
            assert load(RUNTIME / path) == expected, path

    print("24 I profiles, 60 retired overlays and 3 byte-identical blockstate/item/loot assets verified")


if __name__ == "__main__":
    main()
