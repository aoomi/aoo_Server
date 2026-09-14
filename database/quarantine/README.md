# Migration quarantine

Files in this directory are not runtime resources and are excluded from every build.

`legacy-roomcost-region-variants-delete-after-2027-08-25.json` preserves the
pre-migration regional room-cost variants only for migration audit. Runtime room
pricing must come from an explicitly published play-version billing policy.

- Owner: `data-governance`
- Runtime access: forbidden
- Delete after: `2027-08-25`
- Restore path: none; conflicts must be resolved through a new published billing policy
