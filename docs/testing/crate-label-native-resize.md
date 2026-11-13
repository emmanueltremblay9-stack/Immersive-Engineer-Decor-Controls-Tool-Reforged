# Crate label resize qualification (PR 181)

The production fix retains unsaved values when `LabeledCrateEditScreen.init` is
called again. This opt-in qualification supplements the Java GUI-double
regressions with the real packaged screen and real Minecraft `EditBox` widgets.
It does not change the screen, its artwork or the network handler.

Run on Linux with Java 21, Xvfb and the existing NeoForge build prerequisites:

```sh
python tools/validation/crate_label_native.py --language en_us --height 720
python tools/validation/crate_label_native.py --language fr_fr --height 360
```

A clean committed checkout is required. The runner builds and fingerprints the
normal JAR, checks that the separate development harness is excluded, and starts
only the explicitly enabled `crateLabelPackagedClient` run. It uses a fresh
nonce-owned game directory and a newly generated integrated test world. Existing
saves and stale proofs are rejected; no dedicated server or user world is used.

## Bounded cases

- Both edited lines survive repeated real `Screen.resize` calls.
- An empty first line and Unicode second line survive resize.
- Resize leaves the server label unchanged and the editor open.
- Enter uses the production save route; an independent server-thread read must
  observe the submitted values, not just the optimistic client copy.
- Cancel uses the real screen mouse handler and leaves those server values
  unchanged over repeated observations.
- A new screen takes its own opening values rather than those of the prior screen.

The case uses `EditBox.setValue`, screen resize/key/mouse methods, and the normal
save payload. It is **not physical OS keyboard/mouse input or GLFW window resize**.
The screenshot records the resized screen but is not owner visual acceptance or
proof of font quality at every scale. Block interaction that initially opens the
editor, disk persistence/reconnect, cancellation under arbitrary modded latency,
and a complete modpack remain outside this bounded test.

The runner accepts only a matching source SHA, nonce, exact normal JAR, packaged
screen provenance, case sequence, language/dimensions, decoded screenshot, clean
source and successful client exit. `test_crate_label_native.py` tests these proof
guards synthetically; its success alone is not Minecraft execution evidence.
