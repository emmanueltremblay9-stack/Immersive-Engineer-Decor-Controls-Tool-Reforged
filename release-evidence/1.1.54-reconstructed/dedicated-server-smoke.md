# Packaged dedicated-server smoke

Validated stack:

- Eclipse Adoptium Java 21.0.11+10-LTS
- Minecraft 1.21.1
- NeoForge 21.1.230 installed into a disposable directory
- Immersive Engineering 12.4.2-194
- canonical target JAR `1.1.54-reconstructed`, SHA-256 `80F6FF3364A7ECFF96D8EA4C4F7ECBF165793A35D145ED7B3ACBB6863E660E29`

Observed sequence:

1. Production `forgeserver` discovered the exact canonical target JAR and required Immersive Engineering JAR.
2. The target mod registered 200 blocks and 214 items.
3. Recipes and advancements loaded, and the server reached `Done (1.370s)!` on loopback port 25566.
4. An authenticated RCON command on loopback port 25576 returned `Stopping the server`.
5. The server stopped, saved all dimensions, and stopped RCON with process exit code 0.

No client-only class-loading, collision/VoxelShape, registry, datapack/tag, capability, or mandatory-dependency failure was found. No mission-owned Java process remained after shutdown. The server log SHA-256 is `77171DD82BABDE2BDF53E52968A989533E619A4C74F7A27F713F67F20E7E3873`.
