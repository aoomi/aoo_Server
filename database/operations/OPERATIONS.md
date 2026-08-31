# Database operations gates

An empty evidence table, a runbook, or a local fixture is not production evidence.
The following gates must be run on the authorized target topology and retained with
dataset/topology identifiers. Never paste credentials into evidence.

## Online DDL

Populate `aoo_schema_change_plan`, preflight table size and replica health, then
execute the approved online mechanism. Insert `aoo_online_ddl_observation`; its
trigger computes `passed` from measured lock time and replica lag. Abort on
metadata-lock queue growth, lag above plan, disk headroom below 2x the projected
copy, or an unavailable rollback path.

## Core query plans

Run `core-query-plans.sql` on a sanitized production-scale snapshot. Record MySQL
version, dataset hash/rows, JSON plan, access type/index, examined/result rows and
p50/p95/p99 in `aoo_query_plan_observation`. The database trigger applies the
baseline. `ALL`/full-index scans, unapproved filesort/temp tables, index drift, or a
threshold breach fail the gate. Deep pages use the documented keyset cursor; the
offset query exists only for comparison and must not enter an application path.

## Connection and deadlock tests

For each service, stress normal, streaming cancellation, timeout, and exception
paths beyond pool size. Record before/after connections, abandoned connections and
open transactions in `aoo_connection_pool_observation`; the trigger requires no
growth, abandon, or open transaction.

Transactions acquire locks in `aoo_deadlock_retry_policy.lock_order`. Retry only
the listed SQL states, only with an idempotency key, exponential backoff and jitter.
Fault tests must deadlock each policy deliberately and prove one business result,
one ledger effect and one Outbox event. Record every victim and retry result.

## Backup and restore

Each encrypted backup has a manifest with GTID/binlog position, SHA-256, key
version, size and expiry. At the scheduled cadence, restore it into an isolated
environment, roll forward to the selected point, and compare table count, asset
hash, room/history hash and logical relationships. Insert `aoo_restore_drill`; the
trigger derives `passed`. A successful backup command without a successful restore
drill is not recoverability evidence.

## Primary/replica failover

On the authorized replica topology, verify old-primary fencing/read-only state,
GTID equality, connection reroute, distributed-ID ownership, and write rejection on
the old primary. Insert `aoo_replica_failover_drill`. The trigger requires equal
GTID sets, every guard, zero lost rows, and a completed timeline. After promotion,
all primary-only and read-your-write policies remain in force until lag stabilizes.

## Capacity, partitioning and archive

Generate monthly forecasts for room snapshots/events, settlement, replay, ledger,
Outbox, audit and chat data from measured daily rows/bytes. Store 30/90/365-day
values and alert thresholds in `aoo_capacity_forecast`.

Archive is copy -> count/hash verify -> read switch -> legal-hold exclusion ->
purge -> disk reclaim. The idempotency key is source table plus time/partition
boundary. `aoo_archive_run` cannot become `SUCCEEDED` until counts and hashes
match. Reruns resume the same boundary and never double-copy or widen it.

## Repair approval and statistics

Follow the repair workflow in `governance/MIGRATION_RUNBOOK.md`. Production scope
requires distinct data-owner and DBA approvals; security approval is additionally
required for restricted data.

Refresh statistics only within an approved window. Capture statistics hash and a
plan baseline for low/high-selectivity parameter buckets. Alert on plan hash,
selected index, examined rows or p95 drift. Expired baselines fail readiness until
recaptured; hints are not a substitute for fixing schema/query selectivity.

## Zero-row readiness queries

Before traffic or release activation, all must return zero:

```sql
SELECT COUNT(*) FROM v_aoo_active_game_coverage_violation;
SELECT COUNT(*) FROM v_aoo_component_completeness_violation;
SELECT COUNT(*) FROM v_aoo_component_graph_violation;
SELECT COUNT(*) FROM v_aoo_ui_rule_mapping_violation;
SELECT COUNT(*) FROM v_aoo_release_preflight_violation;
SELECT COUNT(*) FROM v_aoo_historical_reference_gap;
SELECT COUNT(*) FROM v_aoo_data_governance_gap;
```

`v_aoo_historical_reference_gap` may be nonzero during the documented Expand and
Migrate phases, but Contract and production-readiness approval remain blocked.
