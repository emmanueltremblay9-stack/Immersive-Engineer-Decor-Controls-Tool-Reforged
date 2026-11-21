#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT
root=src/main/java/com/oblixorprime/engineersdecorreforged/utility
java -m jdk.compiler/com.sun.tools.javac.Main -d "$build" "$root/SolarV2ReferenceGeometry.java" "$root/SolarV2ReferenceState.java" src/test/java/com/oblixorprime/engineersdecorreforged/utility/SolarV2ReferenceChecks.java
java -cp "$build" com.oblixorprime.engineersdecorreforged.utility.SolarV2ReferenceChecks
printf '%s  %s\n' c79e63d9761f8c8f3248e05266244bc88e26282d2fc97607a5fe530a54b23daa src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/solar_v2_reference/background.png | sha256sum -c -
