# R03｜房间通用链路整改与自动验证

验证时间：2026-08-24（Asia/Shanghai）

## 结论

R03 独占边界内的自动化验收通过。公共房间写操作只走 Protocol V2，服务端按已落库 `stateVersion` 做 CAS 前置校验，并将事件、快照和 outbox 同事务提交；重复 `requestId` 返回既有结果，执行中结果未知时 fail closed。玩家重连按已授权身份返回裁剪后的快照和增量事件。

本轮补齐此前公共控制器遗漏的“开局”入口，并把客户端并发写串行化，避免多个同时点击复用同一个 `expectedStateVersion`。服务端 `room.start` 调用真实 `AbsBaseRoom.startGame(pid)`；房主身份、房间阶段和全员准备条件仍由服务端权威校验，不存在客户端假成功或旧 handler fallback。

## 能力证据

| 能力 | 生产链路与约束 |
|---|---|
| 创建 | Hall 的 `RoomCreateSaga` 以 requestId 持久化步骤，覆盖费用预占、房间登记、玩法 authority 创建、确认和逆序补偿；重复请求读取 Saga 状态。 |
| 加入/退出 | Hall JDBC join/ticket/leave 与 WSS `room.join`/`room.leave` 分层；房间命令校验真实座位，退出调用 `AbsBaseRoom.exitRoom`。 |
| 准备/取消/开局 | `room.ready`、`room.unready`、`room.start` 均进入 `ProductionRoomRealtimeService`；开局调用真实 `startGame` 并由房主、Init 状态、全员准备门禁决定结果。 |
| 解散投票 | 申请和同意/拒绝分别调用真实 `dissolveRoom`、`dissolveRoomAgree`、`dissolveRoomRefuse`；voteId 和布尔票值服务端校验。 |
| 重连/异常恢复 | `ProductionRoomReconnectService` 从 JDBC snapshot/event 恢复；客户端分页、single-flight、游标/版本/玩法版本校验，失败不覆盖最后正确视图。 |
| 托管/设置 | 托管调用真实 `opRoomTrusteeship`；设置仅房主可写，数量受限，并进入统一权威提交链。 |
| 聊天/快捷语音/魔法表情 | 快捷文字调用真实 room chat；语音必须经 Media 内部鉴权且资产为 READY；表情验证目标座位并由服务端广播。 |
| 邀请 | 服务端生成限时签名邀请；生产缺少签名密钥时 fail closed。 |
| 幂等/并发 | 服务端以玩家、动作、房间、局号、requestId 建幂等键；客户端公共写队列保证并发 UI 手势依次取得新版本；服务端仍以数据库 CAS 为最终裁决。 |
| 视角裁剪 | 普通状态快照递归移除密码、定位、经纬度；重连服务按玩家/观察者授权视角读取 snapshot/event。 |

## 本轮修改

- `Client/assets/Games/Common/Code/RoomController.ts`
  - 新增 `start()` 的 V2 公共房间命令。
  - 所有 mutation 通过单一 Promise 队列串行，失败不会堵塞后续操作。
  - 每个写响应继续要求 `accepted=true` 且服务端版本严格递增。
- `Server/server/gameServer/src/core/server/ProductionRoomRealtimeService.java`
  - 将 `start` 加入公共命令白名单。
  - 调用真实房间 `startGame(playerId)`，只有服务端接受后才生成成功响应并提交权威版本。
- `Client/tests/unit/room-common-controller.test.mjs`
  - 覆盖开局可达性和完整权威上下文。
  - 新增三个并发写按版本 6、7、8 串行提交的机器测试。

未修改登录、大厅/亲友圈公共 UI owner、任何具体玩法规则或算分、公共协议定义、Admin、scene profile。

## 自动验证

1. Client 房间专项：`node --test ...room-common... ...room-reconnect... ...room-exit... ...room-safety... ...poker-social...`，13/13 通过。
2. Client 严格 TypeScript：`node Admin/node_modules/typescript/bin/tsc -p Client/tsconfig.json --pretty false --ignoreDeprecations 6.0`，通过。
3. Server gameServer reactor：`./mvnw -Dexec.skip=true -pl server/gameServer -am -DskipTests compile`，6/6 模块 BUILD SUCCESS，2285 个 gameServer 源文件编译通过。
4. `RoomEdgeRuntimePolicySelfTest`：PASS。
5. `ProductionRoomEdgeFaultSelfTest`：PASS（生产依赖、提交确认丢失、恢复与 fencing race）。

真实设备、真实第三方 Media、正式数据库与弱网多设备验收仍属于项目统一暂缓项，不以本次自动验证冒充完成。
