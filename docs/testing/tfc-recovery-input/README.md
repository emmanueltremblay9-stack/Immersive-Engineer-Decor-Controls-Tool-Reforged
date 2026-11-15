# TFC recovery native pointer transport

Test-harness-only change based on main `97cdb48c394e268bbafaa467c87266b233ea85d7`. No production class, gameplay, art, recipe, or release artifact modification.

Two exact recent candidates exhibited the same failure: PR185 run43623713075 and PR186 run43627649695 attempt1 (`9b779306a4f964454fd5fe0bfd479df6d6c5cbc1`). GLFW reported focused window and cursor(772,258), but Minecraft remained(640,360), settled hover0. Each passed after one retry; those successes do not erase the input-delivery instability.

Replace GLFW warping only with XTest native MotionNotify, as already used by the Mooshroom fixture. Preserve focus, GUI/window coordinate conversion, actual MouseHandler hitbox checks, at least8 settled ticks, full server recovery/second-production observations, capture hashes and nonce completion. No injected callbacks or forced hover state.

Before XOpenDisplay, require existing isolated-directory validation plus a fresh runner-owned marker containing the exact run nonce and local DISPLAY. The marker is created inside xvfb-run, not on the caller's display. The connection closes on completion/failure. Evidence additionally identifies XTest and a positive event count; the validator rejects missing/zero/fabricated-type counts while retaining all previous gates.

Tests execute the exact Java marker guard against missing, foreign and stale identity, mutate the marker check, and retain adversarial real-hover proof checks. These are not native qualification. Exact-commit native CI is required before claiming the intermittent failure fixed; no local Minecraft/profile is installed.
