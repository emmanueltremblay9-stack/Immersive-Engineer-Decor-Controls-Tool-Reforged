# GitHub Actions readback

Qualifying run: https://github.com/emmanueltremblay9-stack/Immersive-Engineer-Decor-Controls-Tool-Reforged/actions/runs/31547104501

- Head: `0bda63097e61dbcfa1ffba24641a6ae87a6882b1`
- Branch: `codex/release-1.1.53-closure`
- Conclusion: `success`
- Job: `Gradle validation` (`93961850797`)
- Expected essential steps: all `success`; none skipped
- Annotations: zero
- GameTests: `All 179 required tests passed :)`
- Artifact ID: `9122933427`
- Artifact name: `immersive_engineer_decor_controls_tool_reforged-1.1.53-reconstructed`
- Transport ZIP: 1,820,737 bytes, SHA-256 `0549143DFCF0C69798B6F54E07A94B249C86B8988ECDF8852AB63DD3ADB65D1C`
- Inner runtime JAR: 2,670,951 bytes, SHA-256 `7F357843ACD1E8A9D85D03B979315E3E19058223EC6C10B156375479321BFE98`

The workflow uses minimal `contents: read` permissions, immutable full-SHA pins for checkout/setup-java/setup-gradle/upload-artifact, `if-no-files-found: error`, a versioned artifact name, 30-day retention, and a recorded artifact digest. The archived run log contains only the compiler's generic deprecated-API note; no correctable Actions Node-runtime deprecation remains.

Because a Git commit cannot include the ID of the workflow run it triggers, the final documentation-only commit's successful run and artifact are bound to that commit through GitHub checks and the published release readback. The source-bearing CI artifact above establishes the runtime JAR hash used throughout the evidence pack.
