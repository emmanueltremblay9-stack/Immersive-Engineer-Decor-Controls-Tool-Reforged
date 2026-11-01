# Small Solar Panel GUI

![User-provided reference PNG](reference.png)

![Pixel Art Skill Toolkit render](toolkit-render.png)

The first image is a byte-for-byte copy of the user-provided reference. The second is the native scale-1 output rendered from `pixel-art.json`. These are design assets and a Toolkit render, not a Minecraft in-game capture.

## Runtime data and behavior

The runtime background is a native-size crop of the canonical PNG at `src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/small_solar_panel_art.png`. All 455x424 crop pixels match the canonical alpha-bounds crop; `MachineLayout` maps that source region explicitly to the 228x212 GUI canvas. Runtime crop SHA-256: `A7F6BA30D930F58033D2D39EA70B4450A3EC6D8432A601CF72CA5482179ADD6E`.

The existing menu keeps the Cell at index 0 and the 64,000 FE capacity. The cell item, energy bar, stored/capacity readout, and comparator value use the real menu/server data. The reference's battery, orange fill, and `32 000 / 64 000 FE` example are covered by the live slot and meter. The synchronized energy value has 100 FE granularity; the hover tooltip states that precision. Charging, extraction, inventory placement, and comparator behavior are unchanged.

The lower inventory panel is adapted to Minecraft's live 3x9 inventory and 9-slot hotbar geometry. The reference and Toolkit render remain untouched and are still the exact-art deliverables.

Regenerate the runtime crop without external Python packages:

```powershell
python scripts/generate_gui_art_texture.py --reference docs/gui/small-solar-panel/reference.png --output src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/textures/gui/small_solar_panel_art.png
```

## Exact pixel reproduction

The source PNG was kept unchanged. The Toolkit document has a 941×1672 transparent canvas. It encodes the source’s nontransparent pixels using the Toolkit `pixels` operation and a color palette.

Toolkit: `emmanueltremblay9-stack/pixel-art-skill-toolkit` at commit `98eecf77190d5716820ec659f8aeb790513c8e93`. The local CLI modules used for this run were fetched from that exact commit; their Git blob IDs were verified:

- `__init__.py`: `0c61e69d67f09922a3280afcbe5edd24d6949965`
- `__main__.py`: `2efe692355a2fab5977172d38f236f8106ba9b3f`
- `cli.py`: `91e98b62603b164f616ebeaff59204f521c04e3b`
- `format.py`: `5c1642a83bf7a918384f6c004587f00e18668e43`
- `renderer.py`: `a022505fab700c014456e157318f398c05970dad`

From the repository root, with `PYTHONPATH` pointed at the Toolkit checkout’s `src` directory:

```powershell
python scripts/generate_exact_pixel_art_json.py docs/gui/small-solar-panel/reference.png docs/gui/small-solar-panel/pixel-art.json
python -m pixel_art_skill_toolkit validate docs/gui/small-solar-panel/pixel-art.json
python -m pixel_art_skill_toolkit render docs/gui/small-solar-panel/pixel-art.json --scale 1 --output docs/gui/small-solar-panel/toolkit-render.png
python scripts/verify_exact_pixel_art.py docs/gui/small-solar-panel/reference.png docs/gui/small-solar-panel/toolkit-render.png --report docs/gui/small-solar-panel/pixel-match.txt
```

The Toolkit CLI reported `941×1672 RGBA`. Native RGBA comparison reports **0 differing pixels**.

| File | SHA-256 |
|---|---|
| Reference PNG | `6C84213F58FC30E13EB2AC9A9F51853AD65EAB09001D886C7850DE3B9BBB297E` |
| Toolkit JSON | `AA78E9EE887FA46C412091BCB2321B1B9FD4FF7C747EE60BC9A5E96FDC94C6D0` |
| Toolkit render PNG | `549EE41995F32F1811389F11718358372294F67C1F5886ED9ADD0E3DC0F4308C` |

The reference and Toolkit render above are visual evidence of the exact design reproduction. They are not an in-game capture; please inspect the runtime GUI visually before merging this draft.
