# Raised catwalk: support plate contact

The thick-steel, thin-steel and treated-wood raised-catwalk models previously stopped their support plates at Y=14 while the central grating underside is at Y=15. Extend only each plate's upper face to Y=15, closing the 1/16-block gap. The plate underside stays at Y=12; the shafts, braces, plate footprints, UVs, deck height and outer rim remain unchanged. Plain, scaffold and inset-lamp models and all block-state mappings are unchanged.

`RaisedCatwalkSupportModelTest` reads the actual packaged JSON resources. It separately checks all three plates against the actual underside plane, preserves the shared deck across all six variants, and verifies the state-to-model mappings. Semantic fingerprints protect all unrelated model data, including each support model with only the permitted plate-top coordinate restored to its pre-fix value. These fingerprints ignore whitespace, object-key order and equivalent numeric spelling; intentional future geometry changes need explicit review and updated expectations.

Run `./gradlew test --tests '*RaisedCatwalkSupportModelTest'` (or the complete `test` task in CI).

Client acceptance still requires the built artifact: place raised catwalks on vertical thick-steel, thin-steel and treated-wood poles, inspect underneath from all four sides, and verify contact between the plate and central grating without a visible slit. Place adjacent plain/scaffold/lit catwalks to verify the deck remains level. Check wood braces, steel shafts, item previews and save/reload. Resource assertions establish geometry and scope, not a rendered-client result.
