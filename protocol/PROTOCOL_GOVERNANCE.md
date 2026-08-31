# 协议治理与运行闭环

1. 业务代码只允许依赖客户端 `ProtocolClient` 或服务端 `ProtocolRegistry`。
2. `ProtocolClient` 将全部历史事件归入 M1-M7 的规范领域，并由统一开关决定 V1 兼容传输或 V2。
3. 灰度开关只存在于 `ProtocolMigrationRouter`，禁止玩法代码自行判断协议版本。
4. 写操作必须接入 `ProtocolIdempotencyStore`；房间创建时保存 `ProtocolVersionLock`。
5. 所有房间下行消息必须经过 `PlayerViewProjector`，不得广播完整权威状态。
6. 运维平台读取 `migration-state.json` 展示迁移状态；切换必须记录操作人、范围、时间、旧值和新值。
7. M8 不是人工强制按钮。只有 V1 连续 30 天零流量、没有 V1 活跃房间且回滚演练通过后才能执行物理删除。
8. 账号服和所有游戏服必须配置相同的 `-DWsTicketSecret`，长度至少 32 字符；不得提交到代码库。
9. 修改机器协议源后执行 `node Server/tools/protocol/generate.mjs`，生成的 TypeScript 和 Java 文件禁止手工编辑。
10. CI 必须运行 `node Server/tools/protocol/check.mjs`，破坏性变更必须提升协议主版本。
11. `ProtocolMigrationTelemetry`按UTC日期保存V1/V2流量45天，禁止手工清除迁移统计键。
12. 只有`ProtocolRetirementGate.evaluate().allowed=true`时才允许执行M8；任何脚本不得提供跳过门禁参数。
13. HTTP 与 WSS 统一使用 `protocolVersion/msgId/kind/requestId/seq/timestamp/traceId/body`；禁止新增平行信封字段。
14. 兼容事件必须封装为机器源登记的聚合 `msgId + action + payload`，禁止动态生成未登记 `msgId`。
15. 主动推送必须使用机器源登记的 `state_push`，原事件仅作为 `body.action`，客户端按动作分发。
16. 写请求必须 Redis 原子占位后执行；账务、创建房间和结算还必须有数据库唯一约束。
17. 所有 JSON 业务 ID 使用字符串；Java 内部可使用 long，但跨端 DTO 和网关契约不得暴露 long ID。
18. 断线重连和切换游戏服必须先通过 HTTPS 自动换取新的一次性 `wsTicket`。
19. CI 自动禁止业务代码直接使用 `fetch/new WebSocket`，并禁止生产 `http://` 地址；仅统一网络层和本地回环调试例外。
20. 通信日志只记录 `traceId/msgId/action/code/bytes`，禁止记录报文正文、Token、票据、手牌和聊天内容。
21. 原生房间快照使用 `common.room.state_push`；旧处理器迁移推送使用 `common.room.compat_state_push`，两者不得伪装混用。
22. 全局消息号只可在 `aoo-protocol-v2.json` 注册；删除消息必须永久加入 `reservedMessageIds`，禁止复用，注册表结构变更递增 `messageRegistryVersion`。
23. 协议生成固定使用仓库 `.node-version`，生成器只依赖 Node 标准库；提交前必须执行 `node tools/protocol/verify-reproducible.mjs`，连续两次输出哈希必须一致。
24. Java/TypeScript 协议生成物随源码提交并登记生成物清单，禁止手改；Maven validate 执行 `check-generated.mjs`，缺失或与机器源不一致立即失败。
25. 业务模块禁止直接依赖生成类；生成定义只允许由协议 Registry/Bridge 适配层消费。遗留 commdef 保持参考隔离，不得加入 active reactor。
