# Fluid Barrel GUI artwork evidence

- Canonical user-supplied PNG: `fluid-barrel.reference.png` (941×1672, SHA-256 `d1141964d190d40ec2b83b57e32af67c3d0637e6f7efc9ed0bc1536f19e8a12f`).
- Toolkit source: `fluid-barrel.reference-exact.toolkit.json`, created as a lossless RGBA encoding of the supplied PNG.
- Toolkit render: `fluid-barrel.toolkit-render.png`, produced at native scale 1.
- CLI: `PYTHONPATH=src python -m pixel_art_skill_toolkit validate <JSON>` and `PYTHONPATH=src python -m pixel_art_skill_toolkit render <JSON> -o <PNG> --scale 1`.
- Toolkit repository commit: `98eecf77190d5716820ec659f8aeb790513c8e93`; Python 3.12.10, Pillow 12.2.0.
- CLI validation returned `ok: true`, width 941, height 1672, mode RGBA. Source and rendered RGBA SHA-256: `61d0a68becbd7c2c5bdb352069ea5a2234ffe0a0db566aa739beb3302d820717`; differing pixels: 0; differing channels: 0.
- Runtime artwork uses the alpha crop `(275, 616, 666, 1055)` resized mechanically to 201×226 for the menu canvas. The screen overlay masks the concept number, example bucket icons, static water level, sample amount, and embedded French labels, then draws the live fluid type/fill, real bucket slots, exact amount/capacity, and localized labels. The output slot has a semantic output flag separate from the role wording, so label translation cannot change slot acceptance or automation. Milk rendering recognizes the verified NeoForge fluid ID `minecraft:milk` with a cream tint; other IDs retain the normal registry-name fallback. This crop is a runtime background texture, not an in-game screenshot.
- No Minecraft client was launched; in-game visual QA and owner approval remain pending.
