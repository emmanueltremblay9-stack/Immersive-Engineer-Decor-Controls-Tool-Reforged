# Installer wrapper readback

The repository-owned `install-mod.ps1` checks `$LASTEXITCODE` only immediately after the external Gradle wrapper invocation, where that variable is defined and appropriate. It does not reproduce the reported post-install wrapper failure.

The failing wrapper that returned 1 after a successful install was external to this repository and was not identified in the authorized source. No source or installation-logic change was made. The exact Prism installation and the 50 unrelated JAR hashes were independently reverified.
