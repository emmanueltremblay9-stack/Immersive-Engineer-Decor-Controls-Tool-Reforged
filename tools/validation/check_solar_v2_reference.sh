#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT
root=src/main/java/com/oblixorprime/engineersdecorreforged/utility
java -m jdk.compiler/com.sun.tools.javac.Main -d "$build" "$root/SolarV2ReferenceGeometry.java" "$root/SolarV2ReferenceState.java" src/test/java/com/oblixorprime/engineersdecorreforged/utility/SolarV2ReferenceChecks.java
java -cp "$build" com.oblixorprime.engineersdecorreforged.utility.SolarV2ReferenceChecks
printf '%s  %s\n' d702051b09e09cd7c9ebd7ea874356ba82324db07bcb0657cf8ac66bdf54c796 src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/solar_v2_reference/background.png | sha256sum -c -
