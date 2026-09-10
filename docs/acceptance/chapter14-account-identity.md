# Chapter 14 acceptance

Authority: `docs/后端架构开发文档.md`, chapter 14.

The implementation lives in `Account/UnifiedIdentityService`,
`Account/TrustedDevicePinService`, migration `V20260825_90`, and the client
`UnifiedIdentityAuthGateway`. The login scene and all scene/prefab files remain
byte-for-byte untouched.

Bootstrap integration is intentionally not edited because another owner owns that
file. Required merge: construct `UnifiedIdentityService(accountDataSource, clock)`
and `TrustedDevicePinService(accountDataSource, clock)` beside
`JdbcAccountSessionService`; mount the identity admin routes after the existing
account routes. Production configuration must reject any short-account/test-credential
switch. No JJMJ/2.2.2 code or client MD5 is a runtime dependency.

## `user_type` identity acceptance

- The server is the only authority for `user_type`; the accepted mapping is exactly `0 = normal player`, `1 = companion player`, and `2 = robot`.
- Authenticated profile/session payloads and identity-dependent authoritative state expose the server value unchanged; clients and business modules only consume it.
- Fixtures whose `ppk`, display ID, nickname, login alias, or number range appears to imply another category still follow `user_type`. No digit, bit, length, prefix, suffix, or display-format inference is accepted.
- Missing, out-of-range, forged, or server-conflicting `user_type` values fail the identity-dependent branch with an auditable error instead of falling back to a guessed identity.
- Static boundary checks find no alternate identity classifier based on `ppk`, display ID, nickname, login alias, or account number range in client, server, or admin business code.
