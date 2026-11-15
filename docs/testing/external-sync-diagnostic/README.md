# External fluid sync failure diagnostics

PR187 observed `external sync stalled externalCase=193` in the Eastcanal /
Embeddium profile. Case193 is initial synchronization of cut_copper_slab with
flowing water amount7, falling=false. The original message cannot identify
whether the server future, initial client convergence, or final drain stalled.
No renderer-specific cause or production regression has been established.

This fixture-only addition retains the exact400-client-tick failure boundary,
packet order, future join, fluid-tick cleanup and all original validation gates.
Only at timeout it formats the stage, expected state, pending status and client
observation. Immutable volatile server observations are recorded inside the two
existing server tasks. They include case, operation and game time and are labeled
`lastServerObserved`, NOT current server state. No cross-thread server query,
extra server task, retry, recovery or timeout extension is introduced.

Tests reconstruct and hash the original tick method after removing only the
three diagnostic additions. Mutations to timeout, join and drain checks must be
detected. These are structural tests, not mapped compilation or native proof;
exact-commit CI is still required.
