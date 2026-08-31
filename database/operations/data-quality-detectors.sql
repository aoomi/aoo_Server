-- Read-only detectors. Production execution writes each result to
-- aoo_data_quality_issue with detector code, evidence hash and bounded sample.

-- Active games missing an available region, active play version or compiled index.
SELECT * FROM v_aoo_active_game_coverage_violation;

-- Invalid component graph and incomplete release/component registration.
SELECT * FROM v_aoo_component_completeness_violation;
SELECT * FROM v_aoo_component_graph_violation;
SELECT * FROM v_aoo_release_preflight_violation;

-- UI keys that do not map to rules, and required rules with no UI mapping.
SELECT * FROM v_aoo_ui_rule_mapping_violation;

-- Orphan room snapshots: a version/hash exists but the immutable room lock is absent.
SELECT snapshot.room_id,snapshot.release_id,snapshot.play_version
FROM aoo_room_snapshot snapshot
LEFT JOIN aoo_room_rule_lock room_lock ON room_lock.room_id=snapshot.room_id
WHERE snapshot.release_id IS NOT NULL AND room_lock.room_id IS NULL;

-- Duplicate active template names. The unique key prevents new duplicates; this
-- query detects legacy data before the constraint/contract phase.
SELECT club_id,display_name,COUNT(*) AS duplicate_count
FROM aoo_room_template
WHERE status='ACTIVE'
GROUP BY club_id,display_name
HAVING COUNT(*)>1;

-- Ledger/balance mismatch at the latest posted entry for each authority key.
WITH latest AS (
    SELECT ledger.player_id,ledger.currency,ledger.currency_scope_id,MAX(ledger.ledger_id) AS ledger_id
    FROM aoo_ledger ledger
    WHERE ledger.entry_status='POSTED'
    GROUP BY ledger.player_id,ledger.currency,ledger.currency_scope_id
)
SELECT balance.player_id,balance.currency,balance.currency_scope_id,
       balance.balance AS authoritative_balance,ledger.balance_after AS ledger_balance
FROM aoo_currency_balance balance
JOIN latest
  ON latest.player_id=balance.player_id
 AND latest.currency=balance.currency
 AND latest.currency_scope_id=balance.currency_scope_id
JOIN aoo_ledger ledger ON ledger.ledger_id=latest.ledger_id
WHERE balance.balance<>ledger.balance_after;

-- Historical rows that cannot be decoded without guessing current configuration.
SELECT * FROM v_aoo_historical_reference_gap;

-- Tables missing ownership, authority, retention, legal-hold and anonymization policy.
SELECT * FROM v_aoo_data_governance_gap;
