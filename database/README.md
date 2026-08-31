# Aoo database responsibility package

This directory is the executable authority for Aoo's MySQL 8 schema, immutable
game publication index, legacy-data staging, governance contracts, and database
operational evidence. `database/original` is read-only input and is never an
application initialization source.

## What is implemented

- `V20260824_01`: canonical game/category/family/region catalog; structured room
  rules, create UI, cost/help, component registry, dependency/conflict graph.
- `V20260824_02`: immutable release bundles and component snapshots; precompiled
  `gameId + regionCode + playVersion` index; atomic active pointer, cache epoch,
  audit and Outbox; room/template/replay version locks.
- `V20260824_03`: complete ownership/retention dictionary model; old/new field
  mapping, reconciliation, dual-write authority, Expand/Migrate/Contract, repair,
  data-quality, privacy, legal-hold, shard/pagination/replica contracts.
- `V20260824_04`: online-DDL, query-plan, connection-pool, deadlock, backup/restore,
  failover, capacity, archive, repair-approval and statistics evidence tables.
- `V20260824_05`: zero-row integrity views and immutable-history triggers.
- `V20260824_06`: fail-closed activation, migration-finalization, repair approval,
  privacy completion and evidence-scoring procedures/triggers.
- `V20260824_07`: scoped exact-unit ledger dimensions and bounded exact settlement
  score/multiplier storage; no floating-point financial or scoring columns.
- `V20260824_08`: active-only soft-delete uniqueness, concurrency/update-time
  contracts, versioned JSON schemas and bounded shard routing policies.

`aoo_activate_compiled_game_index` serializes on the catalog row and commits the
active pointer, cache epoch, release audit, and Outbox invalidation event in one
transaction. It rejects missing required components, unresolved dependencies,
component conflicts, and non-`READY` indexes. Rollback uses the same procedure
with `p_action='ROLLBACK'` and an older validated generation.

## Local verification

Run only the responsibility package gates:

```bash
python3 database/tools/legacy_config_converter.py
python3 database/tools/mysql_integration_gate.py
python3 database/tools/database_package_audit.py
```

The MySQL gate refuses to reuse an existing Docker container, applies every
migration to disposable MySQL 8.4, executes activation and negative-path tests,
loads 50,000 room events, captures `EXPLAIN ANALYZE`, generates the complete live
data dictionary, then removes only the container it created.

Evidence:

- `.work/audit/mysql-integration-gate.json`
- `.work/audit/database-package-static.json`
- `target/data-dictionary.json`
- `target/legacy-2.22-structured/manifest.json`

Generated `target` and `.work` assets prove conversion and local execution. They
are not permission to publish legacy games or evidence of a production topology
exercise. Production gates and pass criteria are in `operations/OPERATIONS.md`.

## Release and deletion rules

1. A catalog row may become `ACTIVE` only after provider and every required
   component type have been registered and behavior-tested.
2. Legacy conversion always emits `DRAFT`; ambiguous aliases, duplicate UI keys,
   missing catalog games, and count/default violations remain quarantined.
3. New rooms and templates must persist the immutable release sidecar. Existing
   nullable history columns are an Expand phase and cannot contract to `NOT NULL`
   until `v_aoo_historical_reference_gap` is empty in the deployment database.
4. A referenced release, room rule lock, or template release lock cannot be
   deleted or rewritten. Retention uses verified archive/anonymization workflows.
5. A plan, baseline, or empty table is not a successful operational test. Only an
   observation/drill row scored by the database trigger and linked evidence counts.
