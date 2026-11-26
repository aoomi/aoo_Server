# 房卡按 play_code 归属与零价开房架构重构

> 任务卡格式经主协调批准，只读参考 `/Users/aoo/Code/Game/BCG/Aoo/docs/01-AI员工/任务模版.md` 的结构与字段；本卡及全部交付仅写入 `/Users/aoo/Code/Game/aoo`。目标项目缺少现行知识路由词表，故不伪造 `knowledge_tags`。

## 任务合同

- Task ID：`DIRECT-20261126T055133Z-7335c0ef`
- 需求版本：`R2`
- 标题：房卡按 play_code 归属、每玩法独立配置表、默认零价合法确认与开房链路闭环
- 用户原话摘要：`play_code := 当前权威 aoo_game_catalog.game_code / manifest gameCode`；每个玩法生成独立房卡配置表；房卡业务不以 region_code 或数字 gameId 命名/定价；默认扣卡金额为 0；重构查询、发布、预留、确认、释放/退款、API/UI 展示；备份、scratch、目标实施、多玩法真实开房与独立 QA。
- 当前授权：用户已明确授权跨 Billing/Hall/API/UI/DB 重构、必要迁移、目标库可恢复写入与仅目标服务重启；禁止 Git 提交/上传。
- 唯一协调/卡维护/实施 LEAD：`/root/aoo_runtime_lead`
- 独立测试：实现完成后新建非实施者 QA；写代码者不得验收自己的实现。
- 当前状态：`R2_DEPENDENCY_AUDIT_AND_BACKUP`
- 合同版本：`R2-20261126T170000Z`

## 唯一目标

以当前权威 `aoo_game_catalog.game_code` / manifest `gameCode` 为房卡业务 `play_code`，为全部当前玩法建立受校验的独立房卡配置表；默认零价策略可合法确认，使全部玩法可创建并进入等待房，同时保持 reserve → create → confirm、失败 release/refund 与幂等语义。

## 非目标与禁改

- 禁止读取任务无关的账号、隐私或密钥，日志和 Evidence 不记录密钥。
- 禁止修改或启动 `/Users/aoo/Code/Game/BCG/Aoo/**`；其参考模板仅作已批准的只读格式来源。
- 禁止修改大厅地区分类本身；只从房卡域移除 region 匹配与展示。
- 禁止修改 `Client/assets/Login/Scenes/LoginScene.scene`。
- 禁止 Git commit/push/merge/rebase 或上传。
- 禁止猜造 play_code、价格、玩法规则或跨玩法复制非零价格。
- 禁止在未验证完整备份与 scratch 恢复前 DROP 旧对象；若无法证明安全，不删除旧表。
- 既存指向 BCG 的 `*.direct-lock*` 为卡外污染，只登记，不复用、不修改。

## 权威与基线

- play_code 唯一权威：当前目标库 `aoo_game_catalog.game_code` 与当前 manifest `gameCode`，二者必须一致。
- 当前已核实 manifest 共 32 个唯一、安全代号，包含 `quanguo2998`、`chengdu2001`、`chengdu2999`；物理表名仅由同一正则验证后的 play_code 派生。
- 当前房卡旧链路以 `game_id + play_version + region_code + 维度` 查询中央策略表；本任务移除房卡域的 region 匹配，不移除大厅 region 数据。
- 目标项目 `SWARM.md` / `project.md` 关键字段仍为 UNKNOWN，且目标知识路由词表缺失；这是治理缺口，不伪装闭合。当前任务依用户直接授权与本卡精确范围推进。

## 允许读取

- `/Users/aoo/Code/Game/aoo/Server/**` 中与 catalog、房卡定价、开房 saga、账本、迁移和测试直接相关文件。
- `/Users/aoo/Code/Game/aoo/Client/assets/Modules/CreateRoom/**`、`Client/assets/Lobby/**` 中与当前开房/规则展示直接相关文件。
- `/Users/aoo/Code/Game/aoo/Admin/src/**` 中与房卡配置展示直接相关文件。
- 当前目标本地数据库的 catalog、pricing、room、member、rule_lock、billing、reservation、ledger、provenance 相关对象，只限本任务核对。
- `/Users/aoo/Code/Game/aoo/work/local-runtime-aoo.py` 仅用于目标服务状态与启动，不输出其中的凭据。

## 写入白名单与 Owner/锁

> Ready 前先完成静态/运行依赖证明；下列清单只能收紧或通过卡内新版本显式扩展。卡 Writer 仍仅 `/root/aoo_runtime_lead`；实现文件按下列互斥 Owner 锁分配，独立 QA 只写本卡 Evidence 区。

- 本卡及其 `.direct-lock/owner.txt`、`.direct-lock/lock.json`。
- DB/bootstrap Owner：`/root/aoo_runtime_lead/pricing_registry_impl`，独占以下路径：
- `Server/database/bootstrap/room-card-pricing/manifest.json`
- `Server/database/bootstrap/room-card-pricing/schema/room-card-policy.schema.json`
- `Server/database/bootstrap/room-card-pricing/plays/*.json`：仅当前 32 个权威 play_code；默认 `amountMinor=0`。
- `Server/database/bootstrap/room-card-pricing/generate_room_card_tables.py`
- `Server/database/bootstrap/room-card-pricing/test_room_card_pricing_bootstrap.py`
- `Server/database/migrations/V20261126_01__room_card_pricing_by_play_code.sql`：仅新增 registry/逐玩法表；不 DROP 旧对象、不硬编码可变金额。
- `Server/database/migrations/V20261126_02__publish_room_resource_routes.sql`：仅以不可变新 release/index 为 32 个当前 ACTIVE 玩法发布客户端已存在的 bundle/scene 路由，并原子移动 active pointer；不改历史发布内容。
- `Server/database/migrations/V20261126_03__repair_unique_compiled_index_generations.sql`：仅修正本卡新发布 index generation 的全局唯一性，不改 release 内容。
- `Server/database/migrations/V20261126_04__rebind_play_pricing_to_active_authority.sql`：仅将 32 个已确认 0 价策略重绑到当前 ACTIVE release/index/sourceHash，不改金额或匹配维度。
- `Server/engineering/release/generate_ui_hash_forward_publication.rb`：仅将当前 32 个 ACTIVE 投影转换为带 CAS 保护的前向发布 SQL；使用 Hall 相同的 canonical JSON 算法。
- `Server/database/migrations/V20261126_05__repair_active_ui_hashes_forward.sql`：仅新建 immutable release/index、重算 `ui_hash`/`bundle_hash` 并原子切换 32 个 ACTIVE 指针；不原地改历史 release。
- Billing/Bootstrap Owner：`/root/aoo_runtime_lead/billing_playcode_impl`，独占以下路径：
- `Server/modules/user-domain/Billing/src/main/java/com/aoo/bcg/billing/RoomChargeOperation.java`
- `Server/modules/user-domain/Billing/src/main/java/com/aoo/bcg/billing/JdbcRoomCostPolicyRepository.java`
- `Server/modules/user-domain/Billing/src/main/java/com/aoo/bcg/billing/RoomCardPricingProvenance.java`
- `Server/modules/user-domain/Billing/src/main/java/com/aoo/bcg/billing/RoomCardPricingImportValidator.java`
- `Server/modules/user-domain/Billing/src/main/java/com/aoo/bcg/billing/JdbcRoomBillingIntentJournal.java`
- `Server/modules/user-domain/Billing/src/test/java/com/aoo/bcg/billing/`：上述类对应测试。
- `Server/modules/infrastructure/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/JdbcRoomBillingLifecyclePort.java`
- `Server/modules/infrastructure/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/CanonicalPricingPolicy.java`
- `Server/modules/infrastructure/Bootstrap/src/test/java/com/aoo/bcg/bootstrap/`：上述类对应测试。
- `Server/modules/social-ops/Lobby/src/main/java/com/aoo/bcg/lobby/` 与对应测试：仅当影响证明显示 Hall 请求/响应必须增加 play_code 或去除房卡 region 匹配。
- `Server/modules/infrastructure/Bootstrap/src/main/resources/canonical-pricing-policy.json`：仅当运行依赖证明仍把它作为当前零价发布输入；不得编造非零价格。
- Admin/Client：仅当影响证明显示当前 UI 实际展示房卡归属字段时，回填精确文件后才可写；不得改 LoginScene。
- `work/backups/room-card-play-code-architecture-*/`、`work/scratch/room-card-play-code-architecture-*/`：仅存本任务可恢复备份、scratch 脚本和去敏 Evidence。
- R2 用户明确要求在无损迁移后删除旧房卡表，只保留按 `play_code` 的玩法价格表。允许新增 forward-only 迁移 `Server/database/migrations/V20261126_08__retire_legacy_room_cost_tables.sql` 及其生成/验证脚本；历史 migration 禁改。
- R2 允许写入 `Server/modules/user-domain/Billing/**`、`Server/modules/infrastructure/Bootstrap/**`、`Server/modules/social-ops/Lobby/**` 中经依赖盘点证明直接需要迁移的房卡查询、publication/provenance、reservation snapshot、AA、退款、幂等实现与对应测试；共享文件 Writer 仅 `/root/aoo_runtime_lead`。
- R2 允许修改 `Server/database/local/seed_local_runtime.sql`、`Server/database/tools/legacy_config_converter.py` 和 `Server/database/bootstrap/room-card-pricing/**`，仅用于移除旧表生产/seed/converter 依赖并生成新结构；不得改历史 migration。
- R2 DB 写入对象：既有 `aoo_room_card_play_registry`、32 个 `aoo_room_card_price_<play_code>`，以及仅在必要时新增、名称不含 `room_cost` 的共享 publication/provenance/authority 元数据表；旧 `aoo_room_cost_policy`、`aoo_room_cost_publication`、`aoo_room_cost_active`、`aoo_room_cost_policy_provenance` 仅在完整历史数据、FK 和运行语义均无损迁移且 scratch 恢复/回滚通过后允许 DROP。
- R2 备份与 scratch Evidence 路径：`work/backups/room-card-play-code-architecture-r2-*`、`work/scratch/room-card-play-code-architecture-r2-*`，Owner 仅 `/root/aoo_runtime_lead`。
- UI 兼容纠正轮次 Client Owner：`/root/aoo_runtime_lead/room_right_seat_ui`，独占且仅允许修改以下文件；`CommonRoom.prefab` 明确退出白名单，禁止以公共统一坐标覆盖玩法布局：
  - `Client/assets/Games/Common/Code/Runtime/GameRuntimeEntries.ts`
  - `Client/assets/Games/Poker/PDK/Common/Code/Runtime/PdkSwitchCoordinator.ts`
  - `Client/tests/unit/common-room-seat-lifecycle.test.mjs`
  - PDK 专项测试文件仅在 Owner 只读确认现有精确路径后回填，未回填前不得新建或修改。
  该轮次仅修复公共房间座位从旧 1600x900 左下原点坐标误用到当前 1280x720 中心原点坐标系的根因；不改 Canvas/design resolution、不改已提供 `seatLayout` 的玩法覆盖、不做截图特判。验收须包含用户实际视口、固定与动态座位边界、Creator 构建和非实施者 QA。
- 准备命令纠正轮次 Client Owner：`/root/aoo_runtime_lead/room_right_seat_ui`，独占且仅允许修改：
  - `Client/assets/Games/Poker/PDK/Common/Code/Runtime/PdkRuntime.ts`
  - `Client/tests/unit/pdk-canonical-command-routing.test.mjs`
  该轮次只允许将生命周期权威 canonical commands 通过显式 allowlist 原样发给 Gateway；真实区域动作继续使用 `poker.<play_code>.dispatch`。禁止宽泛放行全部 `common.room.*`，禁止服务端注册假命令、吞错或兜底。覆盖 `liangshan2003` 与 `chengdu2001` 的 ready 直通及合法区域动作不回归。
- Ready 幂等 lease 公共框架纠正轮次 Owner：`/root/aoo_runtime_lead/ready_lease_framework_impl`，独占且仅允许修改：
  - `Server/modules/infrastructure/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java`
  - `Server/modules/infrastructure/Bootstrap/src/test/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthorityReadyIdempotencyTest.java`
  该轮次只补齐 canonical ready 与同类 execute 一致的 idempotency acquire/release/markUnknown 生命周期；不得绕过 lease、吞错、按玩法特判或改 Router/Committer/Store。回归首次提交、同 requestId 重放、变更前失败 release、变更后失败 unknown/recovery，以及跨用户/房间隔离。
- Ready 成功响应状态落模纠正轮次 Client Owner：`/root/aoo_runtime_lead/room_right_seat_ui`，独占且仅允许修改：
  - `Client/assets/Games/Poker/PDK/Common/Code/Runtime/PdkRuntime.ts`
  - `Client/tests/unit/pdk-canonical-command-routing.test.mjs`
  该轮次只允许让已获准的 canonical lifecycle mutation 成功响应统一进入既有 `applyAuthoritativePacket`；不得忽略/固定/猜测 stateVersion，不得盲重试、吞错或放宽服务端版本门禁。必须回归首次 READY 响应后 UI/模型立即变为 READY，按钮不再重复提交，并保留区域动作 dispatch 语义。

## Ready 核验

- [x] 用户直接授权、跨模块范围与 `play_code := game_code` 已闭合。
- [x] 禁改、禁止 Git、BCG 禁触碰边界已记录。
- [x] 卡维护者/唯一 Writer 已确定，卡锁只指向目标项目。
- [x] 验收终点、独立 QA 与失败停止条件已定义。
- [x] 精确到文件和 DB 对象的最终影响白名单已回填。
- [x] 全量 DB 备份已生成并校验：`/Users/aoo/Code/Game/aoo/work/backups/room-card-play-code-architecture-20261126T0630Z/aoo_login_local.sql`，152161209 bytes，SHA256 `e59ea5a1da1f756c1c961d0252dbc952b288391359cce5b614111652cfbf88c8`；恢复方式为新建空库后导入该 SQL，禁止覆盖式回滚。
- [x] scratch 恢复已核实：`aoo_room_card_arch_scratch_20261126`，恢复后 341 tables、`aoo_game_catalog` 68 rows；后续所有 DDL/生成器先在此库演练。
- Ready 结论：`READY_FOR_IMPLEMENTATION`。当前 ACTIVE tuple 是运行发布绑定权威；manifest 只提供 gameCode/displayName 与配置源，三项版本差异不得静默采用 manifest 版本。所有玩法使用显式 `PLAY_DEFAULT` 零价策略，不猜造维度组合。

## 验收标准

1. 当前所有权威 play_code 均有唯一安全物理表与 registry 映射，默认有效策略 amount=0、CONFIRMED；无 region 参与房卡匹配。
2. API/管理展示仅以 play_code 和中文玩法名表达房卡业务归属，不显示 CN-51-01/GLOBAL/数字 gameId 为业务名称。
3. reserve → room create → confirm；失败 release/refund；幂等重复请求均有自动测试和 DB Evidence。零价不写负扣款，账本语义明确。
4. 目标服务重启后遍历全部玩法验证配置表存在且默认 0。
5. 至少对此前 game5 对应的权威 play_code、`chengdu2001`、`chengdu2999` 真实 POST 开房、取得 roomId、进入等待房，并核对 room/member/rule_lock/authority/receipt/reservation/ledger。
6. 全新非实施者 QA 复验浏览器用户路径、console/network/server logs；实施者自测不能代替独立验收。

## Checkpoint、重试与停止条件

- Checkpoint A：卡、白名单、锁、Ready；B：备份/恢复校验；C：scratch 全流程；D：目标实施和单元/集成测试；E：目标运行 E2E；F：独立 QA。
- 同一失败指纹自动尝试最多 2 次；第二次仍失败立即停止并升级。
- 权威 play_code 缺失/冲突、动态 SQL 不能由 registry+正则双重限制、备份不可恢复、DDL 无可靠回滚、现有房间数据影响不明：写入前 fail-closed。
- SAFE_CHECKPOINT：每阶段记录时间、Owner、实际变更、命令/日志/DB 去敏证据、回滚点和下一步。
- R2 DROP 额外门禁：旧表任一历史行、FK、退款/AA/reservation snapshot、publication/provenance identity 或幂等语义无法映射时，必须停在 DROP 前并报告精确对象；不得以清空历史账务换取通过。

## 执行与 Evidence

- `2026-11-26T17:00:00Z` `/root/aoo_runtime_lead`：用户追加 R2，要求删除旧房卡表并只保留按 play_code 的玩法表。卡 Writer/共享代码/DB migration Writer 锁归 `/root/aoo_runtime_lead`；独立 QA 保持只读验收。当前仅进入依赖审计与新完整备份阶段，DROP 尚未授权执行到门禁通过。
- `2026-11-26T18:05:00Z` `/root/aoo_runtime_lead`：只读证明右侧座位出界唯一根因：`CommonRoom.prefab` 与动态座位仍使用 1600x900 左下原点坐标，而当前公共房间是 1280x720 中心原点；非 Canvas/Widget/浏览器缩放故障。已将上述 3 文件精确白名单和唯一 Client Writer 锁追加到本卡，不打断 R2 DB 迁移。
- `2026-11-26T18:35:00Z` `/root/aoo_runtime_lead`：用户纠正 UI 业务权威：上述“公共统一坐标”诊断与验收标准已作废，不得用它宣称通过。座位必须从当前分类玩法 Prefab 的 `Players/Seat_n/Head` 序列化坐标加载；房间号/局数必须来自真实 room snapshot。原 Client Owner 已被指令先精确撤销其本轮错误公共坐标改动，再提交唯一根因与新精确白名单；新修复暂未开始。
- `2026-11-26T18:48:00Z` `/root/aoo_runtime_lead`：原 Client Owner 已精确撤回其三处错误通用坐标 diff，未写新修复。唯一根因闭合为生命周期时序：common prepare 阶段过早解析 `commonRoomPorts()`，而 PDK runtime/playController 仅在 gameplay commit 后存在，故返回 null 并退回公共 `CommonRoom.prefab`；这同时绕过 PDK mounted hook（真实 room snapshot 未刷新，残留占位房号/局数）和 PDK Prefab 的 `Players/Seat_n/Head` 布局（右座沿用公共 x=1450 被裁切）。纠正白名单限定为延迟解析 ports、从已实例化的 PDK 权威 Prefab 提取布局及对应测试；不得改公共 Prefab 坐标。
- `2026-11-26T18:32:00Z` `/root/aoo_runtime_lead`：R2 目标库写入前重新校验完整备份 `/Users/aoo/Code/Game/aoo/work/backups/room-card-play-code-architecture-r2-20261126T1730Z/aoo_login_local.sql`，SHA256 `f7721ca1a4cc45ee143040df0ba9e151ecb1518fe1c93b628324f92659499396`。已应用 forward-only `V20261126_08__retire_legacy_room_cost_tables.sql`：旧四表数量 0；新 publication/policy/authority/provenance 历史行数分别 1/427/1/6；32 个 ACTIVE registry、32 张按 play_code 的定价表完整；reservation/refund 为 260/9，refund orphan=0，19 条已引用策略的 reservation 保留 publicationId+policy snapshot。Billing 定向 compile/install 成功；根 reactor 仍被卡外治理脚本缺失阻塞，Bootstrap 单模块 package 又被本地 poker-cx/nn/父 POM 解析阻塞，未越界修复。已将新 Billing jar 更新到现有 runtime lib 并仅重启目标栈：avatar/version/account/social/hall/gateway PID `11654/11655/11658/11659/11661/11669`；相关健康端点 200，尚未完成真实开房与独立 QA。

- `2026-11-26T05:51:33Z` `/root/aoo_runtime_lead`：建立本卡；当前仅完成只读影响初筛，未修改代码/数据库。
- `2026-11-26T06:30:00Z` `/root/aoo_runtime_lead`：完成全库备份及 SHA256 校验，并恢复到独立 scratch；旧房卡表保持不变。
- `2026-11-26T06:40:00Z` `/root/aoo_runtime_lead`：完成精确白名单与依赖核对；模板落点经用户确认仅为后端 `Server/database/bootstrap/room-card-pricing/`，卡转为 Ready。
- `2026-11-26T15:15:00Z` `/root/aoo_runtime_lead`：新增写入前全库备份 `/Users/aoo/Code/Game/aoo/work/backups/room-card-play-code-architecture-20261126T1515Z/aoo_login_local-before-resource-route.sql`，124121885 bytes，SHA256 `3dbf8dfd6435a7374c1e7d32a66e585c9ca85cf2b0302a7492c7c13fbb569c8d`。恢复到 scratch `aoo_room_route_scratch_20261126` 后演练 `V20261126_02` 通过：32/32 ACTIVE schema 含 bundle/scene、release/index schema 相等、bundle hash 相等。
- `2026-11-26T15:18:00Z` `/root/aoo_runtime_lead`：目标库应用 `V20261126_02` 并验证 32 个 ACTIVE 指针全部切到不可变新 release/index；五种受当前客户端常量和当前 DB 权威记录证明的 bundle 路由，scene 均为 `GameRoom2D`。目标服务重启 PID avatar 95440、version 95441、account 95442、social 95445、hall 95446、gateway 95449；端口 8765/8095/8096/8097/8093/8080+18080，Creator 7457。
- `2026-11-26T15:55:00Z` `/root/aoo_runtime_lead`：独立 QA 首错 trace `fb9839da-06b3-4fda-a90a-8b906eda334e` 为 `room-card pricing authority mismatch`；读只对比证明 32/32 registry/policy 的 sourceHash 与当前 ACTIVE 相同，release/index 仍为资源路由发布前标识。新建完整备份 `work/backups/room-card-play-code-architecture-20261126T1555Z/aoo_login_local-before-pricing-authority-rebind.sql`，153230224 bytes，SHA256 `e073eed519f85c9ceeb865cc0bc43c30547126c1be6931d78a82f533ac1c38ca`；恢复到 scratch `aoo_pricing_rebind_scratch_20261126` 可读。
- `2026-11-26T15:58:00Z` `/root/aoo_runtime_lead`：`V20261126_04` 先在 scratch 演练通过后应用目标库；仅重绑 32 个已确认 0 价策略的 ACTIVE release/index/sourceHash，未改金额、payer、measure 或 context。验证：32 张表/32 条策略，amount 总和 0，未确认 0，权威异常表 0。目标栈重启 PID avatar 97204、version 97205、account 97207、social 97209、hall 97212、gateway 97216；Creator 67393，MySQL 63126。已交同一非实施 QA 用全新隔离 profile 重测三个代表玩法。
- `2026-11-26T15:19:00Z` `/root/aoo_runtime_lead`：bootstrap 三项测试通过，32 份配置 validate 通过。旧 `aoo_room_cost_*` 未 DROP：`JdbcRoomCardPricingProvenanceRepository`、AA/carry/退款路径与既有测试仍有直接运行依赖，不满足安全删除门禁。
- 卡外发现：目标仓库存在多个 `*.direct-lock*/lock.json` 指向 `/Users/aoo/Code/Game/BCG/Aoo`；本任务不复用、不修改。
- `2026-11-26T19:30:00Z` `/root/aoo_runtime_lead`：用户真实房间 `284790` 已证明 UI 纠正生效（真实 `0/8`、右侧头像在画布内、另一扑克分类未串布局），但两用户准备均失败。唯一根因闭合：`PdkRoomLifecycleController` 已生成权威外层 `common.room.ready_req`，`PdkRuntime.request()` 却无条件重包为 `poker.liangshan2003.dispatch`；Gateway 因此外层命令不匹配 ready 路由而进入区域 execute，authority 对 leaf=`dispatch` 正确拒绝。已追加上述两个精确文件白名单与同一 Client Owner 独占锁；目标协议修复为显式 canonical lifecycle command 直通，不增加服务端兜底。验收新增：两个独立用户同房 ready 成功及广播同步，人数满足后进入正确开局/等待状态，并回归另一扑克玩法；由非实施者独立 QA。
- `2026-11-26T20:20:00Z` `/root/aoo_runtime_lead`：独立 QA 已在房 `892380`、账号 `2485/2486` 证明 Client outer msgId 修复为 `common.room.ready_req`；新首失败 trace `pdk:892380:2486:4bbb3e20-86bb-4f13-93fc-e03147f02df9` 为 `command idempotency lease missing`。只读闭合唯一根因：`routeGameplayReady` 进入 `JdbcGatewayRoomAuthority.ready` 后直接 `session.execute`，未执行同类 execute 路径的 lease acquire/release/markUnknown；committer 按同 requestId 锁行时数据库确无预建行。并非 body key 缺失、scope/key 不一致或玩法特例。已追加上述精确公共框架白名单与唯一 Writer；修复后必须用新房间、双会话和另一扑克玩法重新独立验收。
- `2026-11-26T21:10:00Z` `/root/aoo_runtime_lead`：lease 修复部署后用户真实房 `801383` 暴露新首失败 `ROOM_ORCHESTRATOR_READY_STATE_VERSION_CONFLICT`。只读证据证明首次 READY trace `pdk:801383:2489:85da626e-662d-48dd-9446-994e0d779923` 已成功（expected/current `0`，提交后 `stateVersion=1`、seat0 READY）；随后 trace `pdk:801383:2489:16ce9bf1-cd9c-4329-88be-44e6d2681f4e` 入口 expected/current 均为 `1`，因 seat 已 READY 而执行为 no-op，authority 正确拒绝伪造 next=2。唯一根因是 `PdkRuntime.request()` 只对 play 成功响应调用 `applyAuthoritativePacket`，READY 成功响应未落客户端模型，按钮重新启用并以新 requestId 重复提交；不是 join 广播缺失或版本值错误。已追加上述精确 Client 白名单/锁，禁止以版本绕过或盲重试修复。
- `2026-11-26T21:36:00Z` `/root/aoo_runtime_lead/room_right_seat_ui`：仅在两项白名单内完成 READY 成功响应落模纠正；明确 allowlist lifecycle 成功 packet 在 resolve 前调用既有 `applyAuthoritativePacket`，失败不投影、区域动作仍 dispatch。定向测试 `6/6 PASS`、Client typecheck exit 0、未解析标识 644 files PASS；Creator PID `13511` 的 7457 预览已刷新且 served chunk 包含修复。扩展回归 47/50，3 个失败均为白名单外既有 stale regex/旧契约断言，未越权修改；状态仅 `DEV_SELF_CHECK PASS`，已交全新非实施者真实房间 QA。

## 用户终验

- 仅用户可确认最终完成；当前：`未请求用户终验`。
