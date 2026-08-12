# Release evidence summary — 1.1.53-reconstructed

Qualification target: `v1.1.53-reconstructed` on the final release commit descended from corrective commit `886010f4b07b13b68ba01f82b6f362de8ca40801`.

## Outcome before publication

- Version, mod ID, Minecraft, NeoForge, Java, Immersive Engineering, support URL, MIT license, notices, and reconstruction provenance were read directly from project files and the runtime JAR.
- Workflow actions were upgraded and pinned to immutable SHAs. The qualifying workflow run completed every expected step with no annotations.
- Two independent clean local builds and the downloaded CI artifact produced the same 2,670,951-byte JAR and SHA-256.
- The JAR was installed in the NeoForge 1.21.1 Prism LAB with exact source/target hash equality and exactly one matching mod JAR.
- Manual, solar output/conservation, door/hatch state transitions, Creative inventory, optional JEI, and conventional tool mining were exercised in the client.
- A dedicated server discovered the mod, completed registries, loaded a world, reached `Done`, and shut down cleanly.
- CurseForge and Modrinth publication are `BLOCKED_BY_MISSING_CONFIGURATION`; no project IDs, configured publishing task/workflow, or authenticated repository secrets were found. A manual-upload bundle is provided.

Raw logs and screenshots are retained outside Git. Only compact, non-secret evidence and hashes are versioned here.
