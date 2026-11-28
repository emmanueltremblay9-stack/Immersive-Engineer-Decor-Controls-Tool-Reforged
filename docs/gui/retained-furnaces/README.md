# Retained Electrical Furnace and Waste Incinerator artwork

Source: GUI Designer DEV `candidates/iedctr/exact-gui-json/2026-11-06/MANIFEST.json`, inspected at `8344355fecae8a5ee476c5ddd74471d2a16d2337`. The existing official renders were copied unchanged, not regenerated.

- Electrical PNG SHA256: `a04ff87a54ae7b52299cb3334219f714e508bb675231333c951db31ffd1e1e12`, 1199×1312.
- Incinerator PNG SHA256: `7b5da44b8e7c405a29c69c022e99e3e14b9cd27500e15d56a1d04b38dc783cb4`, 1254×1254.

Seven electrical slots and sixteen recoverable incinerator FIFO slots preserve their container indices, roles and server semantics. Player inventory remains 27+9. Source coordinates are individually rounded and 16×16 hitboxes remain non-overlapping. The electrical heat, FE and progress indicators use existing synchronized fields. Its four-position speed control sends the existing action 30. The painted upright pointer is covered before drawing the current position. No mechanics or stored data change.

Lab Furnace remains on its previous GUI because the archive explicitly classifies its new artwork as `PROVISIONAL_DIRECTION_ONLY`. It must not be represented as finally approved.

Native captures and exact-commit CI remain required. Static layout checks do not establish visual acceptance, native compatibility, redistribution rights or release readiness.
