# XQP 对照 → Aoo 登录恢复与活动房间状态机报告

## XQP 只读对照

| XQP 位置 | 权威语义 | Aoo 框架层落点 |
| --- | --- | --- |
| Client/assets/Script/Loading/LoadingWindow.ts | Inspector isClearLocalData=true 时先清理本地数据，再加载登录模块。 | Client/assets/Login/Code/Bootstrap/LoadingEnvironmentPolicy.ts 与 LoginScreenBootstrap.ts：只有启动场景的显式开关可清理并阻断自动登录。 |
| Client/assets/Script/Remote/Login/LoginWindow.ts | 无本地登录参数时显示登录页；有参数时自动登录。 | AuthSession.restoreSession()：未勾选时恢复认证，勾选时进入登录页。 |
| Client/assets/Script/Remote/Message/Handler/HomeHandler.ts | 登录成功广播中的 roomId 是当前仍在房间的服务端权威结论；无房间时清掉本地房间上下文。 | HallRoomGateway.activeRoom() 和 SceneRouter.recoverAuthenticatedTarget()：所有登录后恢复均查询 /api/v2/hall/rooms/active；无活动房间即清除本地 room intent 并进入大厅。 |
| Client/assets/Script/Remote/Game/Chess/Common/Data/ChessRoomData.ts | GAMEOVER、DISSOLVE 与大结算均不可作为后续恢复目标。 | ActiveRoomRecoveryPolicy：终态、结算态、roomTerminal、dissolved、大结算快照均不返回为活动房间。 |

## Aoo 三个状态转换

1. **勾选清理本地数据**：启动先删除认证和恢复上下文 → 登录页 → 用户重新登录 → Hall 查询权威活动房间 → 仅等待开局/进行中房间预加载资源并进入；其余进入大厅。
2. **未勾选**：保留认证与最近界面上下文 → 自动登录 → Hall 查询权威活动房间 → 活动房间恢复；无活动房间清掉陈旧 room intent 后进入大厅，绝不重放开房。
3. **房间终止**：首局等待超过 300 秒由 Gateway 自动解散 → Hall membership、票据和 Gateway route/snapshot 一并失效 → /rooms/active 不再返回该房间。

## 修改文件

- Server/server/Hall/src/main/java/com/aoo/bcg/hall/room/ActiveRoomRecoveryPolicy.java
- Server/server/Hall/src/main/java/com/aoo/bcg/hall/room/JdbcHallRepository.java
- Server/server/Hall/src/test/java/com/aoo/bcg/hall/room/ActiveRoomRecoveryPolicyTest.java
- Client/assets/Login/Code/Navigation/SceneRouter.ts
- Client/assets/Lobby/Code/Runtime/HallRoomGateway.ts
- Client/assets/Login/Code/Bootstrap/LoginScreenBootstrap.ts

未运行全量测试或构建。
