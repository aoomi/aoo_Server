# V06｜建房、入房与房间通用能力验证

验证时间：2026-08-24（Asia/Shanghai）

## 结论

**当前快照未通过 V06，禁止标记完成。** 本轮先执行源码链路核查和现有自动测试，再在独占边界内补齐 `Client/assets/Games/Common` 公共房间控制器及其机器测试，并修复房间生产故障自测因 `AuthoritativeGameSession.stateVersion()` 契约演进造成的编译中断。旧版 `/Users/aoo/Code/Game/BCG/Test` 未修改，玩法规则未修改。

仍有两个已机器取证的生产硬阻塞位于本任务明示修改边界之外，而且相关文件在本轮检查时存在其它 AI 的同期修改，故未覆盖：公共房间 WSS 信封与服务端 Bridge 契约不一致；建房 Saga 未由生产 Bootstrap 装配。人工和真实设备验收按要求暂缓。

## 本轮整改

### Client Games/Common 房间控制器

`RoomController` 现统一覆盖：入房、状态读取、离房、准备/取消准备、房间设置、快捷文字、快捷语音资产、魔法表情、邀请、解散申请/投票、托管、心跳、洗牌、踢人及重连入口。

- 所有请求只经过注入的 `ProtocolClient`，没有旧消息 handler、HTTP fallback 或本地假成功旁路。
- 每次写请求都携带 `roomId/seatId/playVersion/stateVersion/expectedStateVersion`；写成功必须返回 `accepted=true` 和严格递增的安全整数 `stateVersion`，否则拒绝推进本地权威上下文。
- 对 reconnect cursor/token、快捷语音 assetId、魔法表情与目标座位、解散 voteId、设置数量和踢人座位执行客户端前置校验；非法输入不会触达网络。
- 新增 `room-common-controller.test.mjs`，动态执行控制器，验证完整命令集合、权威上下文透传、版本单调与非法输入 fail-fast。

### Server 房间通用编排自测

`ProductionRoomEdgeFaultSelfTest` 的真实生产 committer/recovery 故障矩阵已适配新增的服务端权威 `stateVersion()` 契约，恢复 gameServer reactor 编译，并继续覆盖原子提交、提交确认丢失、身份/序列/fencing 不匹配、非 JDBC 生产持久化拒绝、恢复抢占与 fencing race。

## 能力矩阵

| 能力 | 当前源码证据 | 本轮结果 |
|---|---|---|
| 支付 Saga / 建房 | `RoomCreateSaga`、`JdbcRoomCreateSagaStore` 与 3 项 Saga 测试存在；支付预占→Hall 注册→玩法 authority→确认及逆序补偿已建模 | **阻塞**：`BootstrapAPP` 仍调用四参数 `HallHttpRoutes.mount(...)`，没有注入 Saga；生产 `/api/v2/hall/rooms` fail closed |
| 入房 | Hall JDBC join/ticket 与公共控制器 `room.join` 存在 | **阻塞**：公共 WSS 信封契约断链，见下文 |
| 设置 | 公共控制器已补 `room.settings`；服务端校验 owner 与最多 32 项 | **阻塞**：WSS 断链；且当前服务端只回显 settings，未找到对权威 snapshot/event 的持久提交证据 |
| 文字 | `room.quick_text` 控制器与真实 `room.opChat` | **阻塞**：WSS 断链 |
| 快捷语音 | 控制器只发送 READY assetId；服务端调用 Media 内部鉴权并执行 `opRoomVoice` | **阻塞**：WSS 断链；真实 Media 环境验收暂缓 |
| 魔法表情 | 控制器校验 expression/target seat；服务端校验目标座位并执行 room chat | **阻塞**：WSS 断链 |
| 邀请 | 控制器 `room.invite`；服务端强制签名密钥并生成有期限签名 payload | **阻塞**：WSS 断链；深链消费属于 V77 协调项 |
| 解散 | 控制器申请/投票；服务端调用真实 dissolve apply/agree/refuse | **阻塞**：WSS 断链 |
| 踢人 | 控制器要求 authorityCommitted 与递增版本；服务端校验座位/版本/Init 状态并调用真实 `kickOut` | **阻塞**：Bridge command 白名单未包含 kick，WSS 不可达 |
| 洗牌 | 控制器严格推进版本；服务端要求 provider `ROOM_SHUFFLE` 能力并调用真实 `opXiPai` | **阻塞**：Bridge command 白名单未包含 shuffle，WSS 不可达 |
| 观战 | 既有 Spectator Gateway/Controller 覆盖申请、授权、snapshot/events、leave；Reconnect service 对 observer 仅输出 PUBLIC 视角 | 专项静态测试通过；真实多设备验收暂缓 |
| 离房 | 公共控制器 `room.leave`；服务端调用真实 `exitRoom`；另有客户端退出清理器 | **阻塞**：WSS 断链；既有 `room-exit-cleanup.test.mjs` 本轮因其测试自身无法剥离 TS `export type` 而失败 |
| 重连 | `RoomReconnectController` 分页恢复、序列/版本校验、single-flight；JDBC snapshot/event 与玩家/观察者裁剪 | 自动测试 3/3 通过；真实弱网/设备验收暂缓 |

## 生产硬阻塞证据（未越界覆盖）

### 1. 公共房间 WSS 契约不可达

`ProtocolClient` 当前只把 `room.reconnect` 视为 authoritative。其它 `room.*` 会 canonicalize 成 `common.room.dispatch`，其 envelope：

- 不把原请求的 `roomId/roundNo/playVersion` 提升到顶层；
- body 生成 `{ action: legacyEvent, payload: originalBody }`。

而 `ProtocolV2Bridge` 在进入 authority runtime 前：

- 强制 envelope 顶层同时存在 `roomId/roundNo/playVersion`；
- 对 `*.dispatch` 强制 body 存在 `command`；
- common command 白名单不含 `shuffle/kick`。

因此当前 `room.join/leave/settings/quick_text/voice/magic_expression/invite/dissolve/trustee/heartbeat/shuffle/kick` 均无法到达 `ProductionRoomRealtimeService`。这是可确定的生产断链，不得以控制器单测或 service 编译替代端到端通过。

此外 `ProductionRoomRealtimeService` 多数通用命令把客户端 transport `command.sequence()` 当作 `stateVersion`；这不等价于 `AuthoritativeGameSession.stateVersion()` 的服务端权威修订，且 read-only 请求也会推进 transport sequence。统一契约 owner 必须将通用房间写接入服务端权威版本和 JDBC event/snapshot 原子提交，不能用客户端序列充当状态版本。

### 2. 建房 Saga 生产未装配

`RoomCreateSaga` 及 JDBC store 的定向测试通过，但 `BootstrapAPP.mountHall()` 仍调用四参数 `HallHttpRoutes.mount`，等价于向五参数 mount 传入 `null` Saga。生产建房路由由此返回 `HALL_ROOM_CREATE_SAGA_UNAVAILABLE`，没有任何旁路成功；所以支付预占、建房 authority 与失败补偿尚未形成生产闭环。

## 自动验证记录

通过：

1. `node --test Client/tests/unit/room-common-controller.test.mjs Client/tests/unit/room-reconnect-state.test.mjs Client/tests/unit/spectator-flow.test.mjs`：7/7 通过（公共控制器 2、重连 3、观战 2）。
2. `node Admin/node_modules/typescript/bin/tsc -p Client/tsconfig.json --pretty false --ignoreDeprecations 6.0`：通过。
3. `./mvnw -Dexec.skip=true -pl server/Hall -am -Dtest=RoomCreateSagaTest -Dsurefire.failIfNoSpecifiedTests=false test`：BUILD SUCCESS，Saga 3/3 通过。
4. `./mvnw -Dexec.skip=true -pl server/gameServer -am -DskipTests compile`：修复自测契约后 6/6 reactor BUILD SUCCESS，gameServer 2285 源文件编译通过。
5. `java -ea ... core.server.RoomEdgeRuntimePolicySelfTest`：PASS。
6. `java -ea ... core.server.ProductionRoomEdgeFaultSelfTest`：PASS。

未通过但未覆盖的既有测试问题：

- 把 `Client/tests/unit/room-exit-cleanup.test.mjs` 加入集合时，该测试生成的 data URL 仍保留 TypeScript `export type`，Node 20 抛出 `SyntaxError: Unexpected token 'export'`。失败发生在测试装载阶段，不是本轮 `RoomController` 行为失败；修复文件位于 `Client/assets/Common`/其测试 owner 边界外。

## V06 关闭前必须完成

1. 由统一 Protocol owner 对齐 `ProtocolClient`、生成契约与 `ProtocolV2Bridge`，补真实 Bridge/authority 集成测试，证明所有 common room 命令可达；禁止恢复旧 handler。
2. 通用房间服务改用服务端权威 `stateVersion` 并将成功写原子提交到 JDBC room event/snapshot/outbox；重放、乱序与 acknowledgement-lost 必须有机器测试。
3. 由 Bootstrap/Billing/Gateway owner 装配 `JdbcRoomCreateSagaStore` 和真实 Billing/Hall/authority ports，执行逐步故障补偿与进程重启重放测试。
4. 修复并复跑 room exit cleanup 测试装载问题，再执行同一快照的全 Client unit 与 Server reactor。
5. 人工、真实数据库、真实 Media、弱网、多设备玩家/观察者及真实设备验收继续保持暂缓，不得据此宣称 V06 完成。

## 2026-08-24 V09 authority 联合复验

本文件原“通用房间使用 transport seq 且未 JDBC 原子提交”的阻塞已经整改：通用房间读写先读取 `aoo_room_snapshot.state_version`，严格校验客户端仅作为前置条件的 `expectedStateVersion`；写成功后以数据库 CAS 生成下一版本，并在同一事务写 room event、snapshot、outbox。shuffle/kick 同样接收数据库派生的 expected/next 版本，不再读取客户端 `stateVersion` 或 `command.sequence()` 作为权威修订。

复验结果：公共房间、重连、观战 Node 测试 7/7 通过；GameCommon/gameServer reactor 测试退出码 0；`ProductionRoomEdgeFaultSelfTest` 与 `RoomEdgeRuntimePolicySelfTest` 均 PASS。Client 严格 TypeScript 复验被边界外 `ByzpSwitchCoordinator.ts` 引用不存在的 `LegacyNJPdkSwitchCoordinator` 阻断，因此 V06 全项仍不得标记完成；建房 Saga、真实数据库及人工设备项继续按原清单处理。
