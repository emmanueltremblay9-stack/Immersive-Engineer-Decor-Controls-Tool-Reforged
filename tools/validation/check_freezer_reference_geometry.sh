#!/usr/bin/env bash
# Offline geometry/source identity checks only. Does not invoke Gradle or Minecraft.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
JAVA="${JAVA:-java}"
CLASSES="$ROOT/build/freezer-reference-geometry-classes"
REFERENCE="${1:-$ROOT/src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/freezer_reference_v5/background.png}"
mkdir -p "$CLASSES"

# Invoking the compiler module also supports JDK installations without a javac launcher.
# source/target avoids relying on an optional stripped ct.sym release archive.
"$JAVA" -m jdk.compiler/com.sun.tools.javac.Main -source 21 -target 21 -d "$CLASSES" \
   "$ROOT/src/main/java/com/oblixorprime/engineersdecorreforged/utility/FreezerReferenceGeometry.java" \
   "$ROOT/src/test/java/com/oblixorprime/engineersdecorreforged/utility/FreezerReferenceGeometryChecks.java"
"$JAVA" -Djava.awt.headless=true -cp "$CLASSES" \
   com.oblixorprime.engineersdecorreforged.utility.FreezerReferenceGeometryChecks "$REFERENCE"
