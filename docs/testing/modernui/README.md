# ModernUI additional-blur diagnostic, not a fix

Refs issue #70 / closed empty PR #126. Reporter screen/version/GPU configuration remains unknown.
This bounded matrix investigates current ordinary IEDCTR JAR SHA-256
`53ff03f30cb0c8921f4a8fdb205788cba567aab515804c7d2c7a0ab2daf6f98f`
on main630cf7, with official ModernUI NeoForge1.21.1 3.13.0.1 pinned by published SHA-512.

The existing isolated packaged client launcher is reused. A generated init script adds
only `src/modernuiDiagnostic/java` to the separate test-module source set and the
pinned ModernUI JAR to that harness runtime. No main output fallback, normal JAR
mutation, game/server installation or user config is allowed. Config lives only in
the nonce-owned test directory (`config/ModernUI/client.toml`, case-sensitive official path).

OFF and ON are separate processes using identical versions, window size and settings,
changing only `screen.additionalBlurEffect`. Reflection observes the actual config
value, effective BlurHandler switch and shader readiness, plus loaded artifact hash
and class provenance. It does not force those fields or disable global user effects.
We do **not** instrument calls to the shader; readiness is not a count of blur passes.

Three real screens are observed: label editor, Small Electrical Furnace (the active MachineScreen subclass) and
vanilla chest. Original framebuffer PNGs are captured after settling. Actual screen
handlers and normal packets must yield server acknowledgements for text saving or
slot pickup. Inputs are method-driven, not physical OS input. World creation/menu
opening is fixture setup. This is not all-machine, all-GPU, shader-pack or reporter
modpack acceptance. Software-rendered cloud observations cannot establish universal
compatibility.

A green diagnostic means evidence collected and interactions acknowledged, **not**
that issue70 is reproduced or fixed. Independently inspect the OFF/ON images and logs
before proposing any minimal IEDCTR rendering correction. No new correction, closure,
merge, external reporter message or release is included.
