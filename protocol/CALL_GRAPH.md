# V2 唯一调用图

```text
HTTPS /api/v1 -> HttpRoutePolicy -> 统一鉴权/HttpResult -> ProtocolRegistry -> service
WSS handshake -> SecureWsTicketService(30s/origin/device/once) -> ConnectionSession
WSS frame -> GatewayFrameCodec -> WebSocketRequestGuard -> RoomOwnershipRegistry
          -> RoomOrderedExecutor -> GameWebSocketRouter -> Game SPI -> stable response/push
```

业务入口只能使用机器源中的 `msgId`。`*.dispatch` 是登记过的迁移桥，禁止新增 action；旧数字消息号、动态事件名、直接 transport 调用不属于新调用图。客户端只按 `code` 分支，`message` 仅用于展示。房间写请求在解析后必须通过 routeVersion、protocolVersion 与 fencingToken，再进入单房间有序执行器。

连接、迁移和节点异常返回 `ROUTE_NOT_FOUND`、`ROUTE_MOVED`、`ROUTE_STALE`、`NODE_DRAINING`、`VERSION_INCOMPATIBLE` 或 `FENCING_REJECTED`，并携带 expectedVersion、redirectNode 与 retryAfter；不得根据错误文本推断行为。
