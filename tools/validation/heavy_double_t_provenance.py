"""Exact Heavy additive provenance; not runtime adoption or visual proof."""
import hashlib

# Historical pins and the exact reviewed Heavy additions are both immutable.
ROWS = {
    'src/main/java/com/oblixorprime/engineersdecorreforged/ModBlocks.java': (
        '0e39361210dd77bd35714dfdc0029da00a34abb484bd36813731bf47a98d249a',
        '170e70ace96d069673a0b2b5c019f28043c6ba924d6649a1c5c25eb25dd99f9c',
        (
            b'   // MK3/MK3.1 proposed Heavy Double-T item on the draft implementation branch.\n'
            b'   // Does not replace either legacy Double-T registry identity.\n'
            b'   public static final DeferredBlock<com.oblixorprime.engineersdecorreforged.block.HeavyDoubleTSupportBlock> HEAVY_INDUSTRIAL_DOUBLE_T_SUPPORT = registerWithItem(\n'
            b'      "heavy_industrial_double_t_support", () -> new com.oblixorprime.engineersdecorreforged.block.HeavyDoubleTSupportBlock(metal().noOcclusion())\n'
            b'   );\n'
        ),
        (
            b''
        ),
    ),
    'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/lang/en_us.json': (
        '39b48736b027d915123882143a649d1da6394eecc894bf4867d1e7d049a644db',
        '1995df8f38ce44609bcce187f3127b1d4a9fdc7a775ce71c80d0af544954519a',
        (
            b'  "gui.immersive_engineer_decor_controls_tool_reforged.small_solar_panel.cell_tooltip": "Energy item slot: accepts items with FE energy storage. Charging depends on the item accepting energy.",\n'
            b'  "block.immersive_engineer_decor_controls_tool_reforged.heavy_industrial_double_t_support": "Heavy Industrial Double-T Support"\n'
        ),
        (
            b'  "gui.immersive_engineer_decor_controls_tool_reforged.small_solar_panel.cell_tooltip": "Energy item slot: accepts items with FE energy storage. Charging depends on the item accepting energy."\n'
        ),
    ),
    'src/main/resources/assets/immersive_engineer_decor_controls_tool_reforged/lang/fr_fr.json': (
        '550f1ffbe5ad7dd1d2f070222a0d6f66c253c3848f7c523fa27ba390911c7ef9',
        '638690089b551a2af37653c7fab7987a1d0f19a3ca82de72f1ecb6c1e2a0df86',
        (
            b'  "gui.immersive_engineer_decor_controls_tool_reforged.small_solar_panel.cell_tooltip": "Emplacement pour objet \xc3\xa0 r\xc3\xa9serve d\xe2\x80\x99\xc3\xa9nergie FE. La recharge d\xc3\xa9pend de la capacit\xc3\xa9 de l\xe2\x80\x99objet \xc3\xa0 recevoir de l\xe2\x80\x99\xc3\xa9nergie.",\n'
            b'  "block.immersive_engineer_decor_controls_tool_reforged.heavy_industrial_double_t_support": "Support industriel lourd \xc3\xa0 double T"\n'
        ),
        (
            b'  "gui.immersive_engineer_decor_controls_tool_reforged.small_solar_panel.cell_tooltip": "Emplacement pour objet \xc3\xa0 r\xc3\xa9serve d\xe2\x80\x99\xc3\xa9nergie FE. La recharge d\xc3\xa9pend de la capacit\xc3\xa9 de l\xe2\x80\x99objet \xc3\xa0 recevoir de l\xe2\x80\x99\xc3\xa9nergie."\n'
        ),
    ),
}

def project(path, data):
    if path not in ROWS:
        return data
    old_hash, new_hash, inserted, previous = ROWS[path]
    digest = hashlib.sha256(data).hexdigest()
    if digest == old_hash:
        return data
    assert digest == new_hash, 'Heavy additive bytes changed: '+path
    assert data.count(inserted) == 1, 'Heavy additive fragment ambiguous: '+path
    restored = data.replace(inserted, previous, 1)
    assert hashlib.sha256(restored).hexdigest() == old_hash, 'Heavy historical bytes changed: '+path
    return restored

def current_hash(path, historical_hash):
    if path not in ROWS:
        return historical_hash
    old_hash, new_hash, _, _ = ROWS[path]
    assert historical_hash == old_hash, 'Heavy prior pin changed: '+path
    return new_hash
