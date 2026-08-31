# Account identity protocol v2

All 64-bit `accountId` values cross JSON as decimal strings. They are authorization
subjects only and must never be rendered as a player-facing ID. `displayId` is a
decimal string of at least six digits, without a leading zero.

`POST /api/v2/account/login` accepts `identity` plus the ordinary password. Identity
may be a current display ID, alias, verified E.164 phone, `wechat-union:<unionId>`, or
`wechat-open:<appId>:<openId>`. Every failure returns the same `invalid credentials`
class; callers must not infer whether an identity exists.

High-risk administration requires `account.identity.mutate` or
`account.identity.migrate`, a verified second factor, non-empty reason, request ID,
trace ID, operator ID, and source IP. Display-ID changes and identity migrations
atomically revoke sessions, write account audit and Outbox events, and are idempotent.

Device PIN authentication is not password authentication. The client first obtains a
two-minute one-use challenge, then signs
`challengeId:challenge:sequence:sourceIp` with the bound EC private key and submits
the one-digit PIN. Replay, device mismatch, expired challenge, sequence reuse, five
failures, unknown device, or missing secure key all require strong verification.
