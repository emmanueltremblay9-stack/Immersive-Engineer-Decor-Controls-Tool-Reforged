# 1.1.56-reconstructed preparation evidence scaffold

Status: FIRST_QUALIFICATION_PASS; EXACT_COMMIT_REBUILD_REQUIRED_BEFORE_REVIEW. This directory describes the candidate contract; it is not a publication receipt.

The final external preparation evidence package is required to contain artifact-manifest.json (schema_version 4), SHA256SUMS, exact source commit/tree, build command receipts and exit codes, Java/Gradle versions, localization/manual/metadata/sign validators, real GameTest totals, two clean-build byte hashes, published-1.1.55 comparison, independent review, and original-checkout preservation evidence. Full logs and binary artifacts remain outside the source commit.

Required gates: clean build --no-build-cache; compileJava; processResources; validateManualResources; validateProjectMetadata; validateSignItemModels; validate-localization.ps1; 189 required GameTests; exact JAR identity and resource inventory; byte reproducibility; clean scoped diff and original-checkout preservation.

Build provenance distinguishes the merged source baseline (8e8794624db0cf6ccc0be5397682b8c9ce6080f2), the preparation commit/tree, and the historical published 1.1.55 asset. The external manifest binds the exact preparation commit after qualification, avoiding self-referential committed hashes.

New client runtime, new dedicated packaged-server qualification, installation, tag, release, release-asset upload, CurseForge upload and Modrinth upload: NOT_PERFORMED. Higher dependency-version qualification lanes: NOT_PERFORMED. Native fluency: UNAVAILABLE. Earlier sampled QA confirmed shared Hindi/Arabic/Thai rendering limitations; exact component attribution and exhaustive visual coverage remain unverified.

See PUBLICATION_CHECKLIST.md for later publication prerequisites. A preparation PR is not release authorization.

First qualification: clean build and all named validators exited 0; 189/189 required GameTests passed. PowerShell 7 is required by the localization validator (System.Text.Json); Windows PowerShell 5 is unsupported. The exact preparation commit is rebuilt and byte-compared externally before the PR is created. Final source binding and review status are read back in the external manifest and PR checks.
