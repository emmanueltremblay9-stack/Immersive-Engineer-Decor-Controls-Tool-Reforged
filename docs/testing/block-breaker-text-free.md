# Block Breaker text-free nameplate

This isolated follow-up starts from PR 151 head `c86999a03de3a4224abf37f2148c8dfe6d8aa925`.
It removes the only permanent text drawn by the Block Breaker screen: its title.
The localized machine name remains available by hovering the existing nameplate.

No artwork is replaced or covered. Both retained v3 PNGs are hash-pinned by the
regression test. Slot geometry, native stack counts, target selection, work timing,
FE state, packets and every existing target/status/energy tooltip are unchanged.
The central scene is still static artwork and the fluid/power controls remain
unavailable; their existing explanatory tooltips remain intact.

Run `python -m unittest discover -s tools/validation -p 'test_block_breaker_text_free.py' -v`.
The test compiles the exact production label and tooltip methods with recording
Minecraft-facing doubles. It checks absence of permanent title drawing, modeled
one-pixel-padded nameplate edges, three screen origins, cross-machine isolation and preserved
tooltip content. Negative controls restore the title or remove its hover and must
fail. The hover helper is a model, not the exact upstream implementation; real
input bounds still require native qualification. This is not a mapped Minecraft
compile, actual native input or gameplay.

Local result: all 149 Python validation tests passed, including the four new
checks and both negative controls. Java syntax parsing passed for 312 source
files, and `git diff --check` passed. Gradle mapped compilation and JUnit could
not start: the wrapper distribution is absent and its download reports
`Network is unreachable`. No full-build success is inferred from syntax parsing.

Fresh mapped compilation and native EN 720px / FR 260px screenshots are required
from the existing PR Build workflow. Older captures do not qualify this change.
Physical pointer input, custom packs, resize/reload, complex gestures and visual
acceptance are not established by these offline checks. No merge or release.
