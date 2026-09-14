# Hall / Game 重连唯一所有权审计（2026-09-05）

## 结论

- Hall 与 Game 连接分别由 `ReconnectCoordinator` 的独立 slot 和独立取消域管理；一个连接重试不再取消另一个连接。
- 只有 `NetworkRuntime` 可以创建 Hall `ProtocolClient`，只有 `ConnectionOwnership` 可以创建 Game `ProtocolClient`。业务模块不再直接关闭 Hall，也不再配置传输层内部重连。
- Game 新代连接在完成 Hall 房间查询、一次性 Game ticket 刷新、Authority 连接和权威状态恢复前保持 `RECOVERING`；恢复监听全部完成后才进入 `READY`。
- 非幂等动作在断线时立即失败；可重放查询只在新 generation 上重放一次，保留原幂等键。
- 旧 `C1110UUID`、`C1111RoleReLogin`、传输层重连 setter 和业务层 `hallClient.close()` 已从 TypeScript 生产资产中清零。

## 验证证据

- TypeScript：`tsc --noEmit -p tsconfig.json` 通过。
- 定向网络/PDK 契约：17/17 通过，包括 Hall/Game 并行重试、取消后不得落入 TERMINAL、ticket single-flight、恢复完成前不得 READY、默认重试预算覆盖受管重启窗口、旧 Game socket 回放入口退出、CommonPdk 权威事件恢复。
- 结算资产校验：8 个正式 bundle、1127 个注册资产、8 个 Poker family default 全部通过。PDK 使用现有正式 `Games/Poker/PDK/Common/Prefab/SmallSettlement`，未创建占位资源。
- 正式服务已重新打包并通过 `tools/local-dev-services.sh restart all` 部署：Gateway 8080 / Authority 18080、Hall 8093、Account 8096、Version 8095、Social 8097 全部健康。
- Creator 3.8.8 实际界面检查：当前错误/警告计数均为 0；`LoginScreenBootstrap` 可被编辑器编译识别。未修改场景布局、`uiGame-001` 或 `NewMain.prefab`。
- 真实浏览器故障注入：账号 11 登录后由 Hall 活跃房间查询恢复至房间 535014、1/8 局小结算；随后仅重启 Gateway/Authority（Gateway PID 45787 → 46767），浏览器不刷新，5 秒后仍停留在同一权威结算画面，座位、剩余牌、分页和“继续”状态未被清空或重复叠加。

## 双账号故障注入补充

- 两个隔离 Chrome context 登录身份 11/12（权威玩家 391/392），共同恢复房间 837990。基线为第 2/8 局非最终小结算；两席均 `JOINED`，route `ACTIVE`，stateVersion 73，fencing token 32，两端无 `SEVERE`。
- 双端各点击一次“继续”后进入第 3/8 局 `PLAYING`；各端看到自己的完整手牌，对端仅显示 16 张计数。
- 首次 Gateway 重启发现默认 4 次重试无法覆盖正式受管重启窗口。默认预算已提高为 8 次。Creator 正式重编译后重复测试：两端各关闭旧 Game WS 1 条、各创建新 Game WS 1 条并恢复；membership 保持一人一席，stateVersion 73→74，fencing 32→35。
- Gateway 停机窗口点击出牌，当前行动端明确得到 `REQUEST_INTERRUPTED_NOT_REPLAYABLE`，没有进入重放队列；恢复后可重新操作。
- Hall 单服务停机期间既有 Game 画面和双玩家席位保持；Hall 恢复后健康检查通过。页面刷新时两个身份均能通过活动房查询回到权威画面。
- 同步 Gateway 测试的 pageInstanceId/connectionId 新契约后，完整定向批次曾有 23/23 通过；最终复跑的五个核心 Gateway 测试类为 17/17，客户端网络/PDK 契约为 17/17。

## 最终双账号逐态矩阵

正式服务矩阵使用身份 11/12（玩家 391/392）创建并完成房间 256153 的 8 局对局。每格均关闭两端 Game transport、各签发并消费 1 张新 Game ticket、各发 1 次权威 `state_req`，然后核对持久化 generation、Gateway 分配的 connectionId、stateVersion、Hall membership 和 Authority route。

| 验收格 | 服务端 phase / 判定 | stateVersion（双端） | 新 generation（11/12） | ticket / request 增量（每端） | 最终场景与权威关系 |
|---|---|---:|---:|---:|---|
| WAITING | `WAITING` | 1 | 1 / 1 | 1 / 1 | `GameRoom2D`；两席 `JOINED`；route `ACTIVE` |
| READY | `WAITING` + seat 0 ready | 2 | 2 / 2 | 1 / 1 | `GameRoom2D`；两席和 route 不变 |
| PLAYING | `PLAYING` | 3 | 4 / 4 | 1 / 1 | `GameRoom2D`；两席和 route 不变 |
| ROUND_SETTLEMENT | `FINISHED` + `matchFinished=false` | 58 | 6 / 5 | 1 / 1 | 小结算；两席 `JOINED`；route `ACTIVE` |
| FINAL | `FINISHED` + `matchFinished=true` | 444 | 7 / 6 | 1 / 1 | 8/8 大结算；两席 `JOINED`；route `ACTIVE` |
| 正式离房 | Hall active room 均为 false | 444 | 不再建 Game 连接 | 0 / 0 | Lobby；两席 `LEFT`；随后 close/remove 为 `REMOVED` |

- 独立 Game-only 注入：仅关闭身份 12 的 Game socket，Hall `/health/hall` 全程 `UP`；connectionId 从 `4037fbca-3c5e-4901-a9a8-b5a42f8d6a5a` 变为 `98468cc8-50b7-41b8-bb81-e003bb44cd6e`，generation 2→3，ticket=1、state request=1，stateVersion 保持 2。
- 真实 token 过期：将身份 11 当前持久化 access credential 的 `access_expires_at` 推到当前时间之前；Hall ticket 返回 HTTP 401。随后使用真实 refresh token 旋转 session，只签发 1 张新 ticket 并以 connectionId `29a9c57e-e054-4e8e-9cd1-e8b1ef3558f1`、generation 3 恢复，stateVersion 保持 2。
- 非幂等动作：强制断线前生成但未发送的动作 requestId 在新连接自动重放次数为 0；恢复前后 stateVersion 均为 3。浏览器 Gateway 停机点击测试同时得到 `REQUEST_INTERRUPTED_NOT_REPLAYABLE`。
- 原生应用级前后台：两个非 headless Chrome 独立 context 进入房间 415166 后，通过 macOS `System Events` 真正隐藏整个 Google Chrome 应用 3 秒，再由 Launch Services 激活；不是派发 DOM 合成事件。前台恢复后两端均 `wsCreated=0`、`wsClosed=0`、心跳各 1 发/1 收、HTTP=0、SEVERE=0；connectionId/generation 保持 `668ddb0e-de78-4026-b2f6-b3dfa4700226`/2 与 `a88ff310-eb5f-408e-b42a-cb29cb83388d`/2，stateVersion 保持 1，membership/route 保持 `JOINED`/`ACTIVE`。

矩阵执行过程中还发现并修复两处生产路径漂移：正式 Gateway 原先绕过 `JdbcConnectionGenerationStore`，导致 generation 固定为默认值；现由 `ConnectionSessionFactory` 分配持久化 generation 和唯一 connectionId。干净构建还暴露正式 runtime catalog 缺少已发布的 NJPDK 629 映射；已补入与 Hall 发布一致的 `poker:pao-de-kuai` / `legacy-equivalent-1` 条目。干净产物的新房权威快照确认 `cardsPerPlayer=16`。

## 清理证据

- 535014、837990 均已通过正式 Hall leave/close 与 Authority remove 清理为 `DISSOLVED` / `REMOVED`，成员均 `LEFT`，Game ticket 为 0。
- 进一步枚举发现玩家 391/392 历史遗留 39 个 `JOINED/ACTIVE` 房间。全部逐房调用正式 Hall close，并携带 fencing token 调用 Authority remove；最终 `joined_remaining=0`、`active_routes_remaining=0`、`tickets_remaining=0`。未直接修改业务表。
- 最终矩阵房间 256153 与原生前后台房间 415166 均已通过正式 Hall leave/close、Authority remove 清理；两席 `LEFT`、未消费 ticket=0、route `REMOVED`。

## 全库门禁中已存在的非重连失败

完整 `Client/scripts/verify-build.sh` 已越过空资产、字体、结算资产和 TypeScript 门禁，但仍被仓库既有的 PDK 展示职责、已迁移资源路径及大厅清理断言阻断（例如旧 `CompatibilityApp/role`、旧 `Common/Prefab/Poker_Card.prefab`、旧 `Store.prefab` 路径）。这些断言与本次重连唯一所有权变更无关，未用放宽生产约束的方式掩盖。

基础设施附注：Aoo 核心正式服务健康；扩展健康脚本报告本机 Redis 16379、MongoDB 27017 未运行，它们不在 `local-dev-services.sh` 管理的本次核心拓扑内。
