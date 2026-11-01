# Labeled Storage Crate provenance

The restored GUI and appearance assets come from Stefan Wilhelm's Engineer's Decor 1.1.28 source, pinned to commit 837b8678f4fb5ebd818d14c5cbb476273be8405b. Reused material is under the project's MIT license and is attributed in CREDITS.md and NOTICE.md.

Source files:

- [EdLabeledCrate.java](https://github.com/stfwi/engineers-decor/blob/837b8678f4fb5ebd818d14c5cbb476273be8405b/src/main/java/wile/engineersdecor/blocks/EdLabeledCrate.java)
- [labeled_crate_model.json](https://github.com/stfwi/engineers-decor/blob/837b8678f4fb5ebd818d14c5cbb476273be8405b/src/main/resources/assets/engineersdecor/models/block/device/labeled_crate_model.json)
- [labeled_crate_gui.png](https://github.com/stfwi/engineers-decor/blob/837b8678f4fb5ebd818d14c5cbb476273be8405b/src/main/resources/assets/engineersdecor/textures/gui/labeled_crate_gui.png)
- [labeled_crate_front_texture.png](https://github.com/stfwi/engineers-decor/blob/837b8678f4fb5ebd818d14c5cbb476273be8405b/src/main/resources/assets/engineersdecor/textures/block/misc/labeled_crate_front_texture.png)
- [labeled_crate_side_texture.png](https://github.com/stfwi/engineers-decor/blob/837b8678f4fb5ebd818d14c5cbb476273be8405b/src/main/resources/assets/engineersdecor/textures/block/misc/labeled_crate_side_texture.png)

The GUI retains its original 213x206 dimensions and slot layout. The original inventory was 54 storage slots plus a one-item frame hint slot. The new crate therefore exposes 54 slots to automation and keeps the hint slot outside the item capability.

## Immersive Engineering upgrade recipe

The standard IE treated-wood crate is `immersiveengineering:crate` and has 27 storage slots. In the pinned target dependency (Immersive Engineering 1.21.1-12.4.2-194), its recipe is a ring of eight `immersiveengineering:treated_wood`. The distinct reinforced crate is `immersiveengineering:reinforced_crate`; it also has 27 storage slots. Its IE upgrade recipe copies components from the center crate and adds four treated-wood blocks, two steel rods, and two steel plates to the standard crate. This Labeled Crate recipe intentionally takes the standard crate rather than the reinforced output.

The Labeled Crate upgrade pattern is `RPR / PCP / RPR`. It keeps the standard crate cost (eight treated wood), adds four steel rods and four steel plates, and uses no additional treated wood. Compared with IE reinforced-crate upgrading, this trades four treated wood for two extra rods and two extra plates:

- `R` = four steel rods, tag `c:rods/steel`, target item `immersiveengineering:stick_steel`
- `P` = four steel plates, tag `c:plates/steel`, target item `immersiveengineering:plate_steel`
- `C` = one standard `immersiveengineering:crate`

The custom upgrade recipe copies the crate's components to its output and rejects contents the Labeled Crate cannot safely store, including nested containers. On placement, the source crate's `DataComponents.CONTAINER` slots are transferred in order into storage slots 0–26; the hint slot stays empty. The custom name is applied to the placed block, and enchantments are retained through a crate-specific block-entity field and restored when the block drops. Stored ItemStack components, including custom data on individual items, are preserved through the inventory transfer and save/reload. Normal breaking returns one crate item with its inventory; explosion handling remains a separate contents-release path. This avoids consuming a filled input into a blank result and avoids inventing a second inventory copy.

## Visual review status

No in-game screenshot has been captured for this change. The PR includes the original GUI PNG reference, integrated GUI texture, and a Toolkit preview labeled as a generated preview rather than a game capture. Owner visual approval remains pending.
