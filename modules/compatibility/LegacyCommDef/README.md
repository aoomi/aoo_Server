# LegacyCommDef boundary

`LegacyCommDef` is a source-only compatibility boundary for 2.22 wire DTOs,
enums and constants. It is not a public framework API or a Maven artifact.

- `GameSPI` owns modern gameplay contracts and authoritative domain types.
- Modern framework, category and gameplay modules must not import `jsproto.*`
  or `cenum.*`.
- Only the legacy `gameServer` adapter compiles these sources while historical
  clients are supported.
- New protocol fields and business rules belong in typed `GameSPI` contracts;
  this directory receives compatibility fixes only.
