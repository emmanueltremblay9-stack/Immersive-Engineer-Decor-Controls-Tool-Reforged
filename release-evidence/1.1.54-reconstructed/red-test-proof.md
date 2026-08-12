# RED test proof

Canonical 1.1.53 identity:

- JAR: `immersive_engineer_decor_controls_tool_reforged-1.1.53-reconstructed.jar`
- size: 2,670,951 bytes
- SHA-256: `7F357843ACD1E8A9D85D03B979315E3E19058223EC6C10B156375479321BFE98`
- embedded version: `1.1.53-reconstructed`

Final test-only RED command:

```powershell
.\gradlew.bat runGameTestServer --rerun-tasks --console=plain
```

Result: Gradle exit 1 because the GameTest process exited 4. NeoForge ran 182 required tests in 3.207 seconds; exactly four new required tests failed and the other 178 passed:

- `iron_hatch_outline_and_collision_match_all_horizontal_states`: closed north bounds were `AABB[0.0,0.875,0.0] -> [1.0,1.0,1.0]`, proving `Y=14..16` instead of `Y=0..3`.
- `metal_sliding_door_open_collision_allows_player_path_all_facings_and_halves`: open outline mismatched model components for north/left/upper.
- `metal_sliding_door_closed_collision_preserves_panel_all_facings_and_halves`: closed outline mismatched model components for north/left/upper.
- `metal_sliding_door_open_world_collision_allows_real_player_sweep`: a 0.6 x 1.8 standing player AABB was blocked at north/left sample 9.

No production file had been modified when the RED proof was captured.
