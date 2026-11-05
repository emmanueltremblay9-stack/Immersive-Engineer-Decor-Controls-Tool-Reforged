# Retired Muslee withdrawal planner

The generic-reserve design irreversibly converts ingredients, so withdrawal is no
longer an operation. The unused destination planner and its positive withdrawal
tests have been removed. Repository caller audit found only those tests referenced
it; no live menu, packet or item path depended on it. The public helper API is
removed, so any external source consumer must stop issuing ingredient withdrawals.

The reserve core still rejects every withdrawal attempt, and protocol v2 rejects
legacy withdrawal type 2 with both zero and nonzero payloads. These negative tests
replace the old positive withdrawal behavior. No legacy item data is deleted,
and current held-right-click gameplay remains unchanged until replacement safe
activation is qualified.
