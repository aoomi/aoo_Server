# R05 HTTP/WSS 协议终验

执行日期：2026-08-24

## 结论

状态：**通过（阻塞项 0）**。

本轮已修复可在协议边界内确认的漂移：Gateway 错误码重复、`ws_ticket` DTO 与机器协议不一致、微信客户端读取旧字段、客户端响应关联校验不完整、push envelope 校验不完整、权威请求默认伪造 `playVersion`、票据在升级后继续进入业务帧、HTTP 非 2xx 丢失机器错误码。

P0 已关闭：Gateway 在鉴权 handshake 成功回调中安装真实 V2 frame handler；handler 解析文本 JSON、建立票据到账户/玩家会话的显式绑定、调用 `GameWebSocketRouter`，并生成严格关联的 response/push。生产启动要求恰好一个 `GatewayRuntimeProvider`，缺失或重复均 fail-fast，不退回空实现。

## 逐项证据

| 项目 | 结果 | 证据 |
|---|---|---|
| 唯一 HTTPS/WSS 入口 | 通过 | `GatewayApplication.TICKET_PATH/WS_PATH` 与 `HttpRoutePolicy` 只允许 `/api/v2/gateway/ws_ticket`、`/api/v2/gateway/ws`；旧入口返回 410 |
| TLS 与 Origin | 通过（边缘代理前提） | `X-Forwarded-Proto` 必须为 HTTPS/WSS；Origin 按完整值精确白名单匹配 |
| 鉴权与 wsTicket | 通过 | access token、device、Origin、幂等键签发；JDBC 事务内单次消费；30 秒 TTL；消费时复验会话、设备、封禁与 auth generation |
| requestId 幂等 | 通过（游戏 Router） | `GameWebSocketRouter` 先 acquire，完成结果持久化；处理中/未知结果不重复变更；客户端重连队列保留原 envelope/requestId |
| seq/timestamp、防重放 | 通过（Router 单元边界） | `WebSocketRequestGuard` 校验时间窗、room/playVersion，并要求连接序列严格连续 |
| playVersion | 通过 | 服务端比较连接、房间、请求三方版本；客户端权威请求缺失版本直接失败，不再回填 `1.0.0` |
| 错误码 | 通过 | 修复 `RISK_ADMISSION_REJECTED` 与 `API_VERSION_REQUIRED` 共用 1010；新增唯一性测试；客户端保留非 2xx 机器码 |
| DTO 字段 | 通过 | wsTicket 签发响应统一为 `wsTicket/expiresInSeconds/wsUrl`；客户端同步读取 `wsTicket` |
| 请求/响应匹配 | 通过 | Gateway response 与客户端同时核对 protocolVersion、kind、requestId、traceId、msgId、seq |
| 广播匹配与去重 | 通过 | Gateway 只在首次执行后发布 V2 push，幂等重放不重复广播；push 带原 requestId/traceId；客户端以 requestId 因果去重 |
| 心跳重连 | 通过（客户端单元边界） | 23 秒无活动触发重连；后台恢复超过 15 秒重连；重连有次数界限、重新鉴权并刷新 ticket |
| 旧入口关闭 | 通过 | V1、PHP、`WEBSOCKET`、`ClientPack`、`JavaServerPack` 均由退休入口策略拒绝 |
| 真实 WSS 请求处理 | 通过 | handshake 后安装 `gateway-v2-business`；文本帧调用真实 `GameWebSocketRouter`；binary/非法/越权帧结构化失败并关闭；ping/pong 与 close 受控处理 |
| Client 实际线格式 | 通过 | `ProtocolClient` 覆盖继承 transport hook，WSS 实际发送/接收 V2 文本 JSON；legacy 二进制 codec 不再承载 V2 业务帧 |
| 协议 CI | 通过 | GitHub Actions 使用 `Server/.node-version` 的 Node 24.19.0；Java 生成物路径改为 GameSPI 当前路径；三份生成物做确定性与 diff 校验 |

## 自动化结果

- Gateway：`GatewayHandshakeIntegrationTest`、`GatewayWebSocketFrameHandlerTest`、`GameWebSocketRouterTest`、`WebSocketRequestGuardTest`、`JdbcWsTicketServiceTest`、`HttpGatewayContractTest` 共 17 项通过。测试使用 Netty 实际 upgrade/frame pipeline、H2 真实票据事务和真实 Router，不使用 mock。
- Client：Node 24.19.0 执行 `Client/tests/protocol/r05-protocol-contract.test.mjs` 通过。
- 协议工具：Node 24.19.0 执行 `check.mjs`、`check-generated.mjs`、`verify-reproducible.mjs` 全通过；Java/TypeScript/参考文档连续两次生成摘要一致。
- 全程未写入 `Client/assets/Login/Scenes/LoginScene.scene`；登录场景节点、组件、层级及布局保持用户当前版本。

## 关闭结论

真实 WSS 可达、身份绑定、幂等/顺序/时间/版本校验、response/push 关联、异常关闭、Client 文本线格式及协议 CI 均已有执行证据。R05 关闭。
