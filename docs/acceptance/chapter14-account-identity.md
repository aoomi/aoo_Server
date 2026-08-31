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
