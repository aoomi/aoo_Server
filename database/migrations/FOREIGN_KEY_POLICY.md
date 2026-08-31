# Foreign-key policy

- Production and all normal migrations require `FOREIGN_KEY_CHECKS=1`.
- Foreign keys may only be disabled inside isolated legacy dump import staging schemas under `database/original`; staging data is never served.
- Before traffic opens, `ForeignKeyIntegrityReadiness` enumerates every relationship from `information_schema`, checks all composite columns, and rejects any orphan row.
- Promoting staged data requires migrations, the full integrity scan, and normal constraints to succeed. There is no production bypass flag.
