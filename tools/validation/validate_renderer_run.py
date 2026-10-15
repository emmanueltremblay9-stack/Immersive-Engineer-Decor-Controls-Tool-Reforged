"""Validate an isolated Windows matrix run with the fail-closed CI gates."""
import argparse
import json
from pathlib import Path
from client_gameplay import validate_proof, validate_fluidlogging_proof, validate_canal_proof
from renderer_profiles import PROFILES, validate_renderer_profile
if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--game", type=Path, required=True)
    parser.add_argument("--log", type=Path, required=True)
    parser.add_argument("--profile", choices=PROFILES, required=True)
    parser.add_argument("--exit-code", type=int, required=True)
    args = parser.parse_args()
    nonce = (args.game / ".iedctr-client-gameplay-nonce").read_text()
    logs = args.log.read_text(encoding="utf-8", errors="replace")
    logs += (args.game / "logs/latest.log").read_text(encoding="utf-8", errors="replace")
    load = lambda name: json.loads((args.game / name).read_text())
    validate_proof(load("client-gameplay-result.json"), nonce, logs, args.exit_code, False,
                   list(args.game.glob("crash-reports/*")))
    validate_fluidlogging_proof(load("client-fluidlogging-result.json"), nonce)
    proof = load("client-water-canal-result.json")
    validate_canal_proof(proof, nonce, args.game)
    validate_renderer_profile(proof, args.profile)
    print("Renderer profile validated:", args.profile, proof["renderer_mods"], proof["orientation"])
