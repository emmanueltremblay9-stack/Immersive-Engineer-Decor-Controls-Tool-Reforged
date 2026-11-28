#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT
root=src/main/java/com/oblixorprime/engineersdecorreforged/utility
java -m jdk.compiler/com.sun.tools.javac.Main -d "$build" "$root/MachineKind.java" "$root/Gui11V5CRuntimeGeometry.java" "$root/BarrelReferenceGeometry.java" "$root/BarrelReferenceState.java" src/test/java/com/oblixorprime/engineersdecorreforged/utility/BarrelReferenceChecks.java
java -cp "$build" com.oblixorprime.engineersdecorreforged.utility.BarrelReferenceChecks
printf '%s  %s\n' db926e05f68a75e5fd84a6b67e2d7d6ad875d90de52122a3a584759d55c47c6c src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/barrel_reference_v5/background.png | sha256sum -c -
printf '%s  %s\n' eb22fd2eba33a68ebb462ca8dbe6bc2fab41720c26a1ca49436af06b2a490388 src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/gui11_v5c/15.png | sha256sum -c -
