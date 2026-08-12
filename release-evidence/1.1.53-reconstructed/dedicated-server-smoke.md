# Dedicated-server smoke

Validated stack:

- Minecraft 1.21.1
- Java 21.0.11
- NeoForge 21.1.230
- Immersive Engineering 12.4.2-194
- Target mod 1.1.53-reconstructed

Observed sequence:

1. NeoForge launched the `forgeserverdev` target.
2. The target mod and required dependency were discovered.
3. The mod registered 200 blocks and 214 items.
4. Recipes and advancements loaded; a new world was created.
5. The server reached `Done (7.067s)!`.
6. RCON accepted the controlled stop request.
7. The server stopped, saved worlds, and stopped the RCON listener.

No `ERROR`, `FATAL`, exception, client-only screen/renderer class-loading failure, mixin failure, missing mandatory dependency, capability failure, or datapack/tag failure was found.

Log SHA-256: `4807D9A202A68010A914FDAB1D375BB39EB5DE2BA4F5C8AD0AB5F5987890BD0E`.
