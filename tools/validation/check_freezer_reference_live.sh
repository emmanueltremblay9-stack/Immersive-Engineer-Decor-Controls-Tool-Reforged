#!/usr/bin/env bash
# Offline pure Java model / source-pixel proof only. Never invokes Gradle or Minecraft.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
JAVA="${JAVA:-java}"
CLASSES="$ROOT/build/freezer-reference-live-classes"
OUTPUT="${1:-$ROOT/build/reports/freezer-reference-live}"
REFERENCE="$ROOT/src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/freezer_reference_v5/background.png"
mkdir -p "$CLASSES" "$OUTPUT"
"$JAVA" -m jdk.compiler/com.sun.tools.javac.Main -source 21 -target 21 -d "$CLASSES" \
  "$ROOT/src/main/java/com/oblixorprime/engineersdecorreforged/utility/FreezerReferenceGeometry.java" \
  "$ROOT/src/main/java/com/oblixorprime/engineersdecorreforged/utility/FreezerReferenceLiveState.java" \
  "$ROOT/src/test/java/com/oblixorprime/engineersdecorreforged/utility/FreezerReferenceLiveStateChecks.java"
"$JAVA" -Djava.awt.headless=true -cp "$CLASSES" \
  com.oblixorprime.engineersdecorreforged.utility.FreezerReferenceLiveStateChecks "$REFERENCE" "$OUTPUT"
