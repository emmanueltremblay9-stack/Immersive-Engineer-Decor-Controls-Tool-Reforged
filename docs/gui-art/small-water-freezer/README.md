# Small Water Freezer GUI artwork evidence

## Canonical pixel reproduction

- Canonical PNG: small-water-freezer.reference.png, supplied by the owner, 941 x 1672, file SHA-256 be8066887498aa01d029123da7a33ad32a1e935907ae3a3e706ccdd09bfaa3eb.
- Versioned design record for the original exact crop: [GUI DEV v0.4 JSON at commit 42215126e1c777073644658518af1ef7840b00c9](https://github.com/emmanueltremblay9-stack/Master-Generalist-GUI-Architect-Designer-DEV/blob/42215126e1c777073644658518af1ef7840b00c9/candidates/iedctr/seven-machine-guis/v0.4/01_small_water_freezer.native-exact.json). Its status is a proposed candidate; the design file itself says its gameplay data is illustrative.
- This PR also contains small-water-freezer.reference-exact.toolkit.json, a lossless RGBA pixel encoding of the canonical 941 x 1672 PNG. It was validated and rendered with the official Toolkit at commit 98eecf77190d5716820ec659f8aeb790513c8e93 (Python 3.12.10, Pillow 12.2.0):
  - PYTHONPATH=src python -m pixel_art_skill_toolkit validate JSON_FILE returned ok=true, 941 x 1672, RGBA.
  - PYTHONPATH=src python -m pixel_art_skill_toolkit render JSON_FILE -o RENDER_PNG --scale 1.
- Native-scale RGBA comparison, canonical PNG versus actual CLI render: SHA-256 00ff4c733d0db89efb49b3b1b315ac9578d3aaafb9996e2a51854f5e84df236e for both; 0 differing pixels and 0 differing channels.
- small-water-freezer.reference-crop-422x443.png and small-water-freezer.toolkit-render-crop-422x443.png are exact native crops (259,614)-(681,1057) of the full canonical PNG and actual CLI render, respectively. These are reference evidence, not runtime screenshots.

## Runtime distinction

- The artwork's exact reference crop is 422 x 443. The current logical menu is 209 x 235; the screen maps the crop into that layout and draws live machine values, state, energy and slots over it.
- The zero-difference check above applies only to the native 941 x 1672 source and Toolkit output (and their exact 422 x 443 crops). It does not claim pixel equality after the runtime 209 x 235 mapping.
- No Minecraft client was launched. Runtime layout/click behavior has not been visually validated in-game; this is not simulated as a screenshot. Owner visual approval is pending.