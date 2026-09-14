# Data migration and governance runbook

## Authority and phases

Every migration domain must have one row in `aoo_dual_write_control`. The allowed
authority sequence is `LEGACY -> SHADOW -> DUAL_WRITE -> AOO_PRIMARY -> RETIRED`.
The declared mismatch thresholds, stop condition, and rollback manifest are set
before dual write starts. A mismatch over either threshold stops the cutover; it
never silently chooses a side.

Schema changes use three separately deployable records in
`aoo_schema_change_plan`:

1. **Expand** adds nullable/new structures and remains compatible with old and new
   application versions.
2. **Migrate** copies through `aoo_legacy_record_envelope`, checkpoints chunks, and
   records count/hash/balance/reference validation.
3. **Contract** is allowed only after every supported application version writes
   the new shape, the historical-reference gap is zero, and rollback retention has
   elapsed.

For large tables, `COPY` DDL is not approved in the serving schema. The plan must
state algorithm, lock mode, row/byte estimate, maximum lock time, maximum replica
lag, preflight, verification, and rollback. The actual observation is scored
against those limits.

## Lossless extraction and mapping

`legacy_config_converter.py` hashes each immutable source, normalizes gamelist,
gamecreate, roomcost and gamehelp, inventories every column in all original SQL
dumps, and emits a field ledger. Unknown semantics land losslessly at
`aoo_legacy_record_envelope.payload_json.<sourceField>` with `REVIEW_REQUIRED`;
they are not guessed.

The canonical mapping domains are:

| Domain | Canonical authority |
|---|---|
| User/login | `aoo_session` plus the account-domain owner |
| Assets | `aoo_currency_balance` and append-only `aoo_ledger` |
| Club/templates | `aoo_club_member`, `aoo_room_template`, immutable release lock |
| Results/replay | `aoo_settlement`, perspective replay, release/codec identifiers |
| Game configuration | catalog, play version, structured rule/UI/cost/help, release index |
| Unknown/archive-only | lossless envelope until a data owner approves a mapping |

The generated `legacy_field_mapping.csv` is the exhaustive traceability ledger.
Rows marked `REVIEW_REQUIRED` block publication but do not block safe extraction.

## Reconciliation and cutover

For each `aoo_migration_run`:

1. Freeze and hash the source snapshot; record count and business-key hash.
2. Dry-run extraction, quarantine invalid rows, and verify restart from multiple
   checkpoints.
3. Load idempotently using source system/object/key/snapshot as the uniqueness key.
4. Record separate `COUNT`, `HASH`, `BALANCE`, `FOREIGN_KEY`, `UNIQUENESS`, `ENUM`,
   and sample validations.
5. Set the run to `VALIDATING` and call
   `aoo_finalize_migration_run(run_id)`. The procedure marks `PASSED` only when
   counts, hashes and balances agree, orphan/mismatch counts are zero, and no
   validation failed.
6. Run all integrity views. Preserve the source, checkpoint, validation rows and
   rollback manifest through the legal retention period.
7. Switch reads before writes only according to the dual-write record. Roll back
   immediately if thresholds, lag or idempotency conditions fail.

## Data repair

A repair job requires a bounded predicate hash, operation hash, idempotency key,
maximum rows, rate, chunk size, before-image location, and rollback manifest. It
must complete a dry run and enter `AWAITING_APPROVAL`. The requester cannot be an
approver. Distinct `DATA_OWNER` and `DBA` approvals must use the same scope hash;
`aoo_approve_data_repair` rejects any weaker combination. Every chunk, pause,
resume, failure, completion and rollback is appended to `aoo_data_repair_audit`.

## Privacy, legal hold, archive and backup purge

Privacy requests store only a subject hash and verified identity reference. Before
anonymization or deletion, resolve every table policy, archive and backup copy.
`aoo_complete_privacy_request` moves the request to `ON_HOLD` when a matching
legal hold exists. Otherwise its result manifest must include online
anonymization, archive action, backup purge due date, irreversible-token version,
and validation hashes. Physical backup removal follows the table's purge SLA;
until then restored backups must reapply the tombstone/anonymization manifest.
