#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT
root=src/main/java/com/oblixorprime/engineersdecorreforged/utility
java -m jdk.compiler/com.sun.tools.javac.Main -d "$build" "$root/BarrelReferenceGeometry.java" "$root/BarrelReferenceState.java" src/test/java/com/oblixorprime/engineersdecorreforged/utility/BarrelReferenceChecks.java
java -cp "$build" com.oblixorprime.engineersdecorreforged.utility.BarrelReferenceChecks
printf '%s  %s\n' df89575afd98ccee396b38cee34f230ef03f94528d5a4ad5bc3425658ef54ca9 src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/barrel_reference_v5/background.png | sha256sum -c -
