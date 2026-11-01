# Labeled Storage Crate provenance

The restored GUI and appearance assets come from Stefan Wilhelm's Engineer's Decor 1.1.28 source, pinned to commit 837b8678f4fb5ebd818d14c5cbb476273be8405b. Reused material is under the project's MIT license and is attributed in CREDITS.md and NOTICE.md.

Source files:

- [EdLabeledCrate.java](https://github.com/stfwi/engineers-decor/blob/837b8678f4fb5ebd818d14c5cbb476273be8405b/src/main/java/wile/engineersdecor/blocks/EdLabeledCrate.java)
- [labeled_crate_model.json](https://github.com/stfwi/engineers-decor/blob/837b8678f4fb5ebd818d14c5cbb476273be8405b/src/main/resources/assets/engineersdecor/models/block/device/labeled_crate_model.json)
- [labeled_crate_gui.png](https://github.com/stfwi/engineers-decor/blob/837b8678f4fb5ebd818d14c5cbb476273be8405b/src/main/resources/assets/engineersdecor/textures/gui/labeled_crate_gui.png)
- [labeled_crate_front_texture.png](https://github.com/stfwi/engineers-decor/blob/837b8678f4fb5ebd818d14c5cbb476273be8405b/src/main/resources/assets/engineersdecor/textures/block/misc/labeled_crate_front_texture.png)
- [labeled_crate_side_texture.png](https://github.com/stfwi/engineers-decor/blob/837b8678f4fb5ebd818d14c5cbb476273be8405b/src/main/resources/assets/engineersdecor/textures/block/misc/labeled_crate_side_texture.png)

The GUI retains its original 213x206 dimensions and slot layout. The recipe retains the original RCR / C C / RCR shape and old industrial wood planks, with R adapted to Immersive Engineering's verified immersiveengineering:steel_fence item from target dependency 1.21.1-12.4.2-194.

The in-code GUI screen uses the original PNG directly. No in-game screenshot has been captured for this change; see the PR's texture reference and labeled local layout preview for visual review.