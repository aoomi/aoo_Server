# 跑得快当前权威快照全链路闭环

> 任务卡格式依用户当轮提供的 Aoo 治理入口及已批准的平铺卡结构建立；本卡和全部写入仅限 `/Users/aoo/Code/Game/aoo`。旧 `/Users/aoo/Code/Game/BCG/Aoo` 只作已完成的只读 oracle，不修改、不启动。目标项目缺失现行知识路由词表，故不伪造 `knowledge_tags`。

## 任务合同

- Task ID：`DIRECT-20261126T085757Z-fff5fe8a`
- 需求版本：`R1`
- 标题：跑得快创建、进入、READY、发牌、PLAY/PASS、结算与继续游戏的当前权威快照全链路
- 用户授权与终点：按当前权威快照架构实施，修正 READY/PLAY/PASS 统一提交后观察者 fanout 及共享座位 ready 投影/复位；真实双账号、至少两个 PDK 子玩法覆盖断线重连、幂等、单局/大结算及大结算后继续，交非实施者独立验收。
- 协调人、卡维护者与集成 LEAD：`/root/aoo_runtime_lead`
- 当前状态：`DEV_SELF_CHECK_INTEGRATION`
- 合同版本与轮次：`v1 / 2`

## 唯一目标

保持现有权威会话、提交器、幂等与规则内核，使 READY/PLAY/PASS 在成功提交后对房内每个观察者发布其个性化权威快照，并使公共座位头像正确投影及在开局/新局复位 READY，从而闭环完整 PDK 流程。

## 非目标与禁止

- 禁止修改或启动 `/Users/aoo/Code/Game/BCG/Aoo/**`；禁止把 legacy 实现回退成并行链路。
- 禁止 Git commit/push/merge/rebase/upload，禁止不可恢复数据操作。
- 禁止忽略 stateVersion、固定版本、盲重试、吞错、按玩法特判或注册假命令。
- 暂不修改 PDK 牌型/比较/结算内核；只有新证据证明当前内核本身缺口，才先回卡扩精确白名单。
- 不修改 LoginScene、房卡架构、定价或其他卡外治理缺口。

## 权威、现场与唯一根因

- 服务端玩法权威：`PokerAuthoritativeSession` + `PokerDispatchCommandHandler`，已有 READY、全员准备自动发牌、PLAY/PASS、牌型/比较、单局/大结算、幂等与按观察者隐藏手牌快照。
- 当前唯一服务端缺口：`GameWebSocketRouter` 对 READY/execute 返回 `broadcast=false`，而 frame handler 只在 true 时发布；JOIN 已由 authority 显式发布。因此 READY/PLAY/PASS 提交成功但同房其他观察者不收到快照。
- 当前唯一客户端共享座位缺口：PDK adapter/runtime 已读取 ready，但公共 seat view 未携带 ready，`CommonHeadController` 已有 `showReady`/`Img_Ready` 却未由座位渲染驱动，导致 READY 头像标识不同步/不复位。
- 已核对目标与只读 oracle 的 PDK 规则核 115/115 文件 SHA256 一致；旧项目未启动，该结论仅为源码证据。

## 允许读取路径

- `/Users/aoo/Code/Game/aoo/Server/modules/infrastructure/Gateway/**`
- `/Users/aoo/Code/Game/aoo/Server/modules/infrastructure/Bootstrap/**` 仅限 authority/幂等/提交与相关测试
- `/Users/aoo/Code/Game/aoo/Server/modules/gameplay/games/poker/PDK/**`
- `/Users/aoo/Code/Game/aoo/Client/assets/Games/Common/Code/Runtime/**`
- `/Users/aoo/Code/Game/aoo/Client/assets/Games/Poker/PDK/Common/**`
- `/Users/aoo/Code/Game/aoo/Client/assets/Common/Code/UI/CommonHeadController.ts`
- `/Users/aoo/Code/Game/aoo/Client/tests/**` 仅限 PDK/公共房间投影相关
- `/Users/aoo/Code/Game/aoo/work/**` 仅限本任务去敏日志、测试证据与可恢复临时物

## 精确写入白名单、Owner 与锁

> 卡 Writer 仅 `/root/aoo_runtime_lead`。以下两个实施 Owner 文件互斥，不得跨锁写入；非实施者 QA 仅可在本卡追加 Evidence。

- 服务端 fanout Owner：`/root/aoo_runtime_lead/pdk_authority_fanout_impl`
  - `Server/modules/infrastructure/Gateway/src/main/java/com/aoo/bcg/gateway/GameWebSocketRouter.java`
  - `Server/modules/infrastructure/Gateway/src/test/java/com/aoo/bcg/gateway/GameWebSocketRouterTest.java`
- 客户端 READY 投影 Owner：`/root/aoo_runtime_lead/pdk_ready_projection_impl`
  - `Client/assets/Games/Common/Code/Runtime/GameRuntimeEntry.ts`
  - `Client/assets/Games/Common/Code/Runtime/GameRuntimeEntries.ts`
  - `Client/assets/Common/Code/UI/CommonHeadController.ts`
  - `Client/assets/Games/Poker/PDK/Common/Code/Runtime/PdkRuntime.ts`
  - `Client/tests/unit/common-room-seat-lifecycle.test.mjs`
  - `Client/tests/unit/poker-authoritative-chain.test.mjs`
  - `Client/tests/unit/pdk-room-membership-projection.test.mjs`
- 客户端牌面资源路径 Owner：`/root/aoo_runtime_lead/pdk_card_atlas_path_impl`
  - `Client/assets/Games/Poker/Common/Code/Card/Poker_Card_Factory.ts`
  - `Client/tests/unit/public-poker-card-runtime.test.mjs`
- 客户端公共房间异步挂载 Owner：`/root/aoo_runtime_lead/common_room_mount_race_impl`
  - `Client/assets/Games/Common/Code/Room/CommonRoomShellMount.ts`
  - `Client/assets/Games/Common/Code/Runtime/GameRuntimeEntries.ts`（仅 `mountCommonRoomShellCancellable` 的共享 root 取消/ownership 语义）
  - `Client/tests/unit/common-room-shell-mount.test.mjs`（允许新增）
- 服务端 provider dispatch authority Owner：`/root/aoo_runtime_lead/pdk_provider_authority_impl`
  - `Server/modules/infrastructure/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java`（仅 execute 的 provider commandHandler dispatch，保留 fencing/idempotency/carry/atomic persistence）
  - `Server/modules/infrastructure/Bootstrap/src/test/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthorityProviderDispatchTest.java`（允许新增）
- 卡与锁：本文件及其 `.direct-lock/owner.txt` / `lock.json`。
- 客户端牌型反馈返工 Owner：`/root/aoo_runtime_lead/pdk_ready_projection_impl`
  - `Client/assets/Games/Poker/PDK/Common/Code/Runtime/PdkPlayController.ts`（仅权威比较牌型的本地化失败提示；不得改牌型识别、比较、出牌渲染或命令发送）
  - `Client/tests/unit/pdk-authoritative-auto-pass.test.mjs`（仅上述提示的定向契约）
- 客户端零候选自动过牌返工 Owner：`/root/aoo_runtime_lead/pdk_ready_projection_impl`
  - `Client/assets/Games/Poker/PDK/Common/Code/Runtime/PdkHintController.ts`（仅权威 PLAYING 响应回合、零合法候选时的单次 PASS；不得改牌型识别/比较、出牌渲染、绿色手数或有候选/首出行为）
  - `Client/tests/unit/pdk-authoritative-auto-pass.test.mjs`（仅零候选自动 PASS、首出/有候选禁发、同权威回合去重与发送前 authority 复核契约）
- 客户端局间继续游戏返工 Owner：`/root/aoo_runtime_lead`
  - `Client/assets/Games/Poker/PDK/Common/Code/Runtime/PdkSwitchCoordinator.ts`（仅缓存小结算表单显示时幂等补绑当前 `PdkResultController`）
  - `Client/assets/Games/Poker/PDK/Common/Code/Runtime/PdkAuthorityPhase.ts`（新增；可续局完成阶段的单一判定源）
  - `Client/assets/Games/Poker/PDK/Common/Code/Runtime/PdkResultController.ts`（仅完成结算阶段的 continue 发送门禁）
  - `Client/assets/Games/Poker/PDK/Common/Code/Runtime/PdkRuntime.ts`（仅 canonical continue 对完成结算阶段的放行契约）
  - `Client/tests/unit/poker-authoritative-chain.test.mjs`（仅续局阶段一致性定向契约）
- 客户端空响应候选返工 Owner：`/root/aoo_runtime_lead`
  - `Client/assets/Games/Poker/PDK/Common/Code/Runtime/logic/PdkHintCandidateGenerator.ts`（仅单牌响应不存在任何合法 root 时返回空候选；不得改牌型、比较、候选排序或其他响应语义）
  - `Client/tests/unit/pdk-complete-decomposition-v53.test.mjs`（仅“上家单 A、下家无更大牌/炸弹”空候选回归）
- 凉山合法候选自动出牌返工 Owner：`/root/aoo_runtime_lead/pdk_ready_projection_impl`
  - `Client/assets/Games/Poker/PDK/Common/Code/Runtime/PdkHintController.ts`（仅 `liangshan2003` 抢庄结束后的正式 PLAYING 响应回合，将现有确定性首个合法候选在完整 authority 身份复核后单次提交；新墩首出、其他玩法、牌型比较/渲染/绿色手数禁改）
  - `Client/tests/unit/pdk-authoritative-auto-pass.test.mjs`（仅凉山有合法候选自动 PLAY、其他玩法仍只预选、首出/抢庄阶段禁发、身份漂移与同回合防重契约）
- 扩围规则：若精确测试需要新测试文件或真实缺口位于其他生产文件，Owner 必须先停写，返回证据，由卡 Writer 追加精确路径/锁后才可继续。

## Ready 核验

- [x] 用户明确授权当前项目跨 Gateway/Client/PDK 的完整修复与独立验收。
- [x] 唯一目标、非目标、BCG 禁触、无 Git 与无不可恢复操作已闭合。
- [x] 服务端/客户端 Owner 与逐文件互斥锁已分配，集成 LEAD 不与独立 QA 混同。
- [x] 已有唯一根因与精确首波白名单；核心规则文件当前只读。
- [x] 真实验收、重连、幂等、结算、继续游戏、失败门禁已定义。
- Ready 结论：`READY_FOR_IMPLEMENTATION`，时间 `2026-11-26T08:57:57Z`。

## 验收步骤与通过条件

1. 定向单测证明 READY 与区域 execute(PLAY/PASS) 在权威提交成功后返回可发布结果，失败/未提交不发布；不改 JOIN、幂等和错误语义。
2. 客户端座位投影携带 `ready`，头像 ready 标识随快照显示，开局/新局快照中 false 后立即复位；不修改玩法 Prefab 座标。
3. 编译与受影响 Gateway/Client/PDK 测试通过；核心牌型/比较/结算测试不回归。
4. 只重启目标 aoo 必要服务；真实双账号对至少两个 PDK 子玩法执行创房/加入/READY/发牌/PLAY/PASS/单局/大结算/继续，核对双端快照、头像 READY、隐藏手牌与服务日志。
5. 断线重连恢复权威快照；相同 requestId 重放不产生二次动作/结算；错误 stateVersion 依然 fail-closed。
6. 由全新非实施者 QA 使用隔离无头浏览器与独立会话验收；开发者自测不代替验收。

## 检查点、重试与停止条件

- Checkpoint A：本卡/白名单/锁/Ready；B：两个 Owner 定向实施与自测；C：LEAD 集成编译/回归；D：真实双账号 E2E；E：非实施者独立 QA。
- 首波实施：服务端 fanout 与客户端 READY 投影并行，文件完全不重叠；合并前分别交 `DEV_SELF_CHECK`。
- 重试上限：每个唯一失败指纹自动修复最多 1 次；同一指纹重复两次或评审第二轮仍失败，立即停止自动重试并升级人工。
- SAFE_CHECKPOINT：每阶段记录实际变更、测试、去敏证据、首错、回滚面与下一步。
- Git 授权：未授权，且用户明确禁止。

## 子任务

- `PDK-FANOUT-R1`：Owner `/root/aoo_runtime_lead/pdk_authority_fanout_impl`；交付提交后 fanout 服务端补丁与单测；接受路由为 LEAD 集成审查。
- `PDK-READY-VIEW-R1`：Owner `/root/aoo_runtime_lead/pdk_ready_projection_impl`；交付共享座位 READY 投影/复位与定向测试；接受路由为 LEAD 集成审查。
- `PDK-CARD-ATLAS-R2`：Owner `/root/aoo_runtime_lead/pdk_card_atlas_path_impl`；修正牌面工厂使用当前 `common` bundle 实际登记的 SpriteAtlas 路径，并以生成配置/四个 frame 可加载为定向契约；不得改资源或遗留 meta。
- `COMMON-ROOM-MOUNT-R2`：Owner `/root/aoo_runtime_lead/common_room_mount_race_impl`；消除同 host 异步并发挂载重复 root，并保证取消者不得销毁仍由存活 lifecycle 使用的 root；不得改 socket/fanout。
- `PDK-PROVIDER-DISPATCH-R3`：Owner `pdk_provider_authority_impl`；authority execute 必须在既有原子提交外壳内调用当前 provider `commandHandler` 解析玩法 envelope，不得把 outer dispatch msgId 原样交给 session，不得硬编码 PDK/地区命令。
- QA 在两个实施 Owner 都释放锁且集成候选冻结后再指定，不提前参与实施。

## 执行与交测记录

- `2026-11-26T08:57:57Z` `/root/aoo_runtime_lead`：已建立独立平铺卡，根因、互斥 Owner、首波白名单、验收与重试门禁闭合；状态 `READY_FOR_IMPLEMENTATION`。
- `2026-11-26T09:18:00Z` `/root/aoo_runtime_lead/pdk_authority_fanout_impl`：仅在白名单 Router 成功 READY 与 execute 返回上开启 observer fanout，异常及未提交路径不变；Gateway 定向测试 13/13、Gateway 与依赖编译通过，状态仅 `DEV_SELF_CHECK`。
- `2026-11-26T09:18:00Z` `/root/aoo_runtime_lead/pdk_ready_projection_impl`：仅在白名单公共 seat 合同、PDK 投影、头像渲染与三项测试中补齐 ready true/false/空座复位；新增定向测试 3/3、项目现有 typecheck 校验入口通过，状态仅 `DEV_SELF_CHECK`。
- `2026-11-26T09:19:00Z` `/root/aoo_runtime_lead`：集成静态审阅确认两波改动未越白名单，服务端 fanout 位于权威提交、identity/stateVersion 校验及 timeline complete 之后；客户端每次权威 occupied seat render 写 ready 双边、vacant 强制 false。进入目标栈真实链路验证，尚未交独立验收。
- `2026-11-26T09:24:22Z` `/root/aoo_runtime_lead`：Poker 权威链定向回归 `PokerDispatchCommandHandlerTest,PokerAuthoritativeSessionPlayedCardVisibilityTest,PokerCommonRoomPublicViewTest,PokerDissolveLifecycleTest` 通过（exit 0），覆盖命令适配、发牌、逐观察者暗牌、单局结算/续局及解散。全 Poker reactor 160 项中的 4 failure/10 error 位于旧/并行规则契约用例，生产 diff 不涉及其文件；已判定当前无本轮因果，不扩围，保留为基线发现。真实双账号/双 PDK E2E 继续运行。
- `2026-11-26T09:34:00Z` `/root/aoo_runtime_lead/pdk_full_chain_integration`：当前权威确认 `liangshan2003`、`chengdu2001` 为 `poker:pao-de-kuai`；真实创建 `liangshan2003` 房 `426148`（账号 2503 seat0、2502 seat1，2 人/8 局/8 张），双方初始 UI 房号与 `0/8` 正确、服务端 stateVersion=0。首个真实失败：2503 点击 READY 后本地按钮隐藏，但 Gateway 未收到该房 READY 命令、无 trace/广播/发牌；首错位于客户端 READY 出站前。已停止把它误归因到 fanout，进入 console 与 lifecycle→runtime→socket send 唯一根因核验；同指纹第二次按门禁停止自动重试。
- `2026-11-26T09:42:00Z` `/root/aoo_runtime_lead`：将房 `426148` 轮次重新分类为 `INVALID_TEST_ENV`，不计业务失败指纹且不据此扩代码白名单。只读证据显示两隔离浏览器实际加载旧 gameplay chunk `be4643b595e5dabb60f84b86be4ea730b53e71eb.js`（HTTP Last-Modified `2026-11-14 09:51:45`），而当前候选为 `be4cb14e...js`；包含现行 `CommonPdkActionIntent` 的 `f343c380...js` 在失败点击后才生成。原 CDP 点击脚本亦未核 Cocos hit-test、监听目标及 handler 命中，故“按钮隐藏但未 send”不能归因现行源码。下一轮先刷新目标 Creator，使用全新隔离 profile 证明 loaded chunk/hash 与工作树候选一致，再做一次 handler→intent→socket.send→Gateway trace→双方 observer snapshot 仪器化验证。
- `2026-11-26T10:13:00Z` `/root/aoo_runtime_lead/pdk_fresh_preview_e2e`：fresh 候选房 `855054`、账号 2508/2509；双方真实 Cocos click 均命中 handler→ready intent→socket.send，Gateway/Hall 全员 READY 并持久化 stateVersion=3、发牌 8 张。新首错为客户端 `HAND_RENDER: Bundle common doesn't contain Atlas/Poker_Common`；当前 `common/config.json` 仅登记 `Atlas/xxxxxxxxxxxxxxxx/Poker_Common`，故牌面工厂旧逻辑路径在 bundle 名称解析即失败，阻断手牌呈现及后续 PLAY/PASS，但不阻断 READY/服务端发牌。另 B 现场出现两个 active CommonRoom root；静态闭合为 `CommonRoomShellMount` 异步 load 前单次查重、回调后无复查/in-flight ownership。A 只有一个 root，且玩法 socket 订阅独立于 UI root，故 A observer v3 缺口仍为 `UNVERIFIED`，不据此授权生产修复。已冻结当前房，按两个互斥 R2 白名单分别修复与定向测试。
- `2026-11-26T10:18:00Z` `/root/aoo_runtime_lead`：COMMON-ROOM-MOUNT-R2 实施前门禁发现 `GameRuntimeEntries.ts:550-574` 的 cancelled waiter 会在共享 mount resolve 后无条件 destroy root；仅修 mount callback 会让一个取消者销毁另一存活 lifecycle 的共享 root。按唯一根因将该函数精确加入同一 Owner 白名单，允许改为取消者只拒绝、root 生命周期继续由拥有者 lifecycle 管理；其余 runtime 逻辑禁改。
- `2026-11-26T10:25:00Z` `/root/aoo_runtime_lead/pdk_card_atlas_path_impl`：仅在白名单将牌面工厂改为 Creator 当前 `common` bundle 登记的单一路径，并增强测试核对 SpriteAtlas importer 与四个核心 frame；定向 7/7、项目 client manager typecheck 通过，状态仅 `DEV_SELF_CHECK`。
- `2026-11-26T10:25:00Z` `/root/aoo_runtime_lead/common_room_mount_race_impl`：以 per-host in-flight ownership、异步回调后二次查重及 waiter-only abort 修复重复 root/误销毁；动态并发测试 2/2、项目 client manager typecheck 通过，状态仅 `DEV_SELF_CHECK`。
- `2026-11-26T10:27:00Z` `/root/aoo_runtime_lead`：使用目标项目现有 `work/local-runtime-aoo.py` 精确重启当前 aoo 栈，PID/端口为 avatar 38773/8765、version 38780/8095、account 38820/8096、social 38821/8097、hall 38822/8093、gateway 38824/8080+18080；Creator 仍为目标 7457。进入 fresh 候选双玩法真实链路集成，最终独立 QA 尚未开始。
- `2026-11-26T10:39:00Z` `/root/aoo_runtime_lead`：有效 observer 首错证明运行 Gateway jar 陈旧（运行字节码 READY broadcast=false，工作树/测试已 true）；直接模块重编译并部署同 SHA256 产物，三项 Gateway 定向测试 32/32，通过后目标栈重启。新房 `388892` 双端 raw push/projection 已越过该指纹，故不是第二次失败。
- `2026-11-26T10:47:00Z` `/root/aoo_runtime_lead/pdk_fresh_preview_e2e`：房 `388892` 双 READY、v3 发牌、逐观察者暗牌与 CardAtlas 实际 8 张均通过；B 在权威 `COMPETE_DEALER` 阶段单次 `robDealer` 返回 `unsupported poker command: poker.liangshan2003.dispatch`。只读闭合唯一根因：`JdbcGatewayRoomAuthority.execute` 绕过当前 provider `commandHandler`，将 outer dispatch msgId 原样传给 `AuthoritativeGameSession.execute`；玩法 handler 已正确将 robDealer normalize 为规范命令。房冻结，按 R3 精确白名单修 authority，不改 poker 内核/客户端。
- `2026-11-26T10:56:00Z` `/root/aoo_runtime_lead/pdk_fresh_preview_e2e`：按 authority 而非按钮文字核验，房 `388892` 持久化及双方 private replay 均为 `stateVersion=3`、`phase=COMPETE_DEALER`、`currentSeat=1/currentPlayerId=2518`、`cardsDealt=false`，内部 `competeDealerPhase=true/competeCursor=1`，rule lock 为 `robDealerRule=dealer_first`。当前真实要求是 cursor 玩家提交一次抢/不抢布尔决定；客户端阶段按钮正确。点击后仍为 v3，证明失败发生在 provider 适配被绕过、权威状态提交之前。R3 Owner 已锁定为 `/root/aoo_runtime_lead/pdk_provider_authority_impl`。
- `2026-11-26T10:06:28Z` `/root/aoo_runtime_lead/pdk_provider_authority_impl`：R3 仅修改获批的 `JdbcGatewayRoomAuthority.java` 与新增 `JdbcGatewayRoomAuthorityProviderDispatchTest.java`；`execute` 在既有 fencing、runtime identity、stateVersion 与请求范围校验内调用当前 provider `commandHandler`，并将完全相同 durable receipt 的解析前移到阶段 permission 判定前，未放宽业务身份、actor、payload 或 hash 一致性。H2 仅测试 fixture 适配 MySQL `DATE_SUB`，生产 committer 未改；Bootstrap 定向测试 4/4、模块 reactor 43/43 通过，状态仅 `DEV_SELF_CHECK`。
- `2026-11-26T10:06:28Z` `/root/aoo_runtime_lead`：独立复跑 `JdbcGatewayRoomAuthorityProviderDispatchTest`，结果 `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`、exit 0；真实凉山 provider outer dispatch 提交至 stateVersion 6，持久化 authority/snapshot/receipt，完全相同 requestId 重放不二次变更，错误 business/actor fail-closed。静态核对实际写入仍限上述两文件，任务卡 Writer 锁仍由 LEAD 持有；进入 Bootstrap/Gateway 候选重建与新房 E2E。
- 基线/卡外阻断：父 reactor validate 缺 `work/spreadsheet-authoring/build_create_room_rules_workbook.mjs`，upstream CommonRoom testCompile 缺 H2；客户端三文件全量 36 项中 33 PASS，另 3 项为既有并行契约漂移。当前证据未显示它们由本轮改动造成；若阻断定向验收，必须先回卡扩围，不得顺手修复。
- `2026-11-26T10:21:02Z` `/root/aoo_runtime_lead`：追加用户房 `122775` 牌型反馈返工。去敏权威 snapshot、public replay 与 Gateway trace 一致证明上家本手为单次 `TRIPLE_WITH_ONE`（四张 `9+777`，playIndex=1），客户端渲染集合与服务端比较集合一致；绿色 `1` 为第 1 手出牌，按用户纠正排除为缺陷。当前手牌没有同型或炸弹，服务端拒绝单牌响应正确；`[CommonRoomHintUnavailable]` 是零合法候选诊断，物理“提示”按钮已有“没有可出的牌”用户提示。唯一 UX 缺口为 authority 拒绝仍显示泛化“压不过上家”，未说明权威目标是“三带一”，容易被误读为单牌比较。批准仅以上两文件做本地化上下文提示与定向测试；禁止改牌型内核、出牌区、角标、提示候选或服务端。
- `2026-11-26T10:21:02Z` `/root/aoo_runtime_lead/pdk_ready_projection_impl`：返工仅在批准两文件新增权威比较牌型本地化展示；`type=6` 的 authority beat rejection 现在显示“上家是三带一，所选牌不能压过”，未知类型保留原泛化提示。未读取本地 Hint/lastCard，未改识别、比较、候选、发送、渲染或绿色序号。定向 `pdk-authoritative-auto-pass.test.mjs` 30/30 PASS，项目现有 Client typecheck exit 0；状态仅 `DEV_SELF_CHECK`。
- `2026-11-26T10:21:02Z` `/root/aoo_runtime_lead`：独立复跑同一定向测试 30/30 PASS、Client typecheck exit 0，并复核实际改动仅上述两文件。按用户要求不启动测试 AI、不进入房间操作；本返工状态为 `DEV_SELF_CHECK_AWAITING_USER_VERIFICATION`。
- `2026-11-26T10:35:00Z` `/root/aoo_runtime_lead`：按用户纠正恢复同卡返工。唯一根因已由现行源码闭合：`PdkHintController.autoHintForAuthoritativeTurn` 在权威 PLAYING 响应回合已计算出零合法候选，但注释与分支明确把零候选仅当诊断并直接返回；托管零候选也仅隐藏操作区，二者均不调用既有 authority-bound `pass(roomId, seat, expectedStateVersion, trickId, operationId)`，故权威回合不会推进。批准以上两文件最小修改：只在非首出、确为本地权威回合、当前 operationId/stateVersion/trick 未漂移且候选为零时单次 PASS；复用 turn key/in-flight 防并发与同投影重复，发送前再次复核 authority；有合法候选、首出、新墩均禁止自动过。保留当前出牌区与绿色手数；按用户要求不启用测试 AI、不代替用户继续游戏。
- `2026-11-26T10:41:00Z` `/root/aoo_runtime_lead/pdk_ready_projection_impl`：仅在批准的 `PdkHintController.ts` 与定向测试实施。托管零候选与普通零候选汇入同一 authority PASS 路径；发送前重新读取并复核 room/stateVersion/operationId/turnSeat/trickId/comparison/turn key，随后调用既有 lifecycle PASS。候选存在时仍分别自动出牌或仅预选，leading/new trick 仍提前返回；未改渲染、绿色手数与牌型比较。Owner 定向测试 32/32、Client typecheck exit 0，状态 `DEV_SELF_CHECK`，已释放 Writer。
- `2026-11-26T10:43:00Z` `/root/aoo_runtime_lead`：独立复核实现边界并复跑 `node --test tests/unit/pdk-authoritative-auto-pass.test.mjs`，32/32 PASS；复跑 `node scripts/verify-client-manager.mjs --typecheck` exit 0。未启动浏览器、未发送游戏动作、未启动或修改 BCG/Aoo、未执行 Git。按用户要求交回本人测试，当前状态 `DEV_SELF_CHECK_AWAITING_USER_VERIFICATION`。
- `2026-11-26T11:00:00Z` `/root/aoo_runtime_lead`：追加用户“凉山抢庄后响应回合有合法候选则自动出牌”返工。唯一根因由现行源码闭合：`autoHintForAuthoritativeTurn` 已在正式 PLAYING 响应回合按当前权威 comparison 生成排序合法候选并确定 `tips[0]`，但普通非托管分支只写入选择态、记录 `CommonRoomAutoHintSelection` 后直接返回，未调用 `outCard`；因此有牌可压时仍停留等待人工提交。现有 `isFormalCardPlayPhase` 已隔离抢庄阶段，`leading` 分支已隔离首出/新墩。批准同一 Owner 仅修改以上两文件：以权威 `runtime.getGameCode() === PDK_BUSINESS_CODES.LIANGSHAN` 为玩法门禁，仅响应回合自动提交现有确定性候选；发送前复核 roomId/stateVersion/operationId/turnSeat/trickId/comparison/turnKey，复用 in-flight/turnKey 防重连重复；零候选继续自动 PASS，其他玩法继续只预选。按用户要求不启用测试 AI、不做浏览器自动化。
- `2026-11-26T11:06:00Z` `/root/aoo_runtime_lead/pdk_ready_projection_impl`：仅在批准的 `PdkHintController.ts` 与定向测试实施。引入现有 `PDK_BUSINESS_CODES` 权威身份；凉山普通响应的确定性 `tips[0]` 在发送前重新读取并复核 formal PLAYING、roomId/stateVersion/operationId/turnSeat/trickId/comparison 与重建 turnKey，稳定后调用既有 `outCard`。其他玩法仍只预选，leading/抢庄禁发，零候选 PASS 保持；复用同 turn key 与 in-flight 去重。Owner 定向测试 34/34、Client typecheck exit 0，状态仅 `DEV_SELF_CHECK`。
- `2026-11-26T11:08:00Z` `/root/aoo_runtime_lead`：独立静态复核实际修改仍限两份白名单文件，玩法门禁使用当前权威常量，发送前完整 identity/comparison/turnKey 复核且未改比较、渲染或其他玩法分支。复跑 `node --test tests/unit/pdk-authoritative-auto-pass.test.mjs` 34/34 PASS；复跑 `node scripts/verify-client-manager.mjs --typecheck` exit 0。未启动浏览器/测试 AI、未发送游戏动作、未触碰 BCG/Aoo、未执行 Git；按用户要求状态 `DEV_SELF_CHECK_AWAITING_USER_VERIFICATION`。
- `2026-11-26T11:18:00Z` `/root/aoo_runtime_lead`：用户真实复测证明凉山抢庄后合法响应仍未自动提交，上一轮静态契约不足，恢复同卡返工且不把问题归为动画。现行链路唯一阻断已闭合：首次 turn identity 的 comparison cards 来自 `logic.lastCardList`，提交前重读 identity 的 cards 来自 `comparisonState.cards`；代码先用 `sameCards` 证明二者为同一物理牌组，却随后以 `join(',')` 生成两个顺序敏感 key 并要求 `responseKey === key`。权威快照与本地牌逻辑可对同一组合保留不同序列顺序，因此合法候选已选中后仍在发送前 fail-closed 返回，`outCard` 根本未调用。批准原 Owner 继续仅改既有两份白名单文件：抽取单一、无序的 comparison identity 表达供首次/响应/PASS key 共用，保留 roomId/round/stateVersion/operationId/turnSeat/trickId/tableOperations/type 及 `sameCards` 全部门禁；新增异序同牌可单次提交、真实牌组变化必须拒绝、同回合不得重复的定向测试。不得改牌型比较、候选排序、动画/渲染、其他玩法、抢庄或新墩领出；不启动测试 AI/浏览器。
- `2026-11-26T11:24:00Z` `/root/aoo_runtime_lead/pdk_ready_projection_impl`：仅在批准两文件实施 canonical comparison identity，对物理牌 ID 数值排序并保留重复；initial/response/PASS 三处 authority key 共用该表达，其他身份与阶段门禁未放松。新增可执行 VM harness 覆盖异序同牌凉山合法响应恰好提交一次、同回合重复不再提交、真实牌组漂移拒绝、非凉山与 leading 拒绝自动提交；Owner 定向测试 36/36、Client typecheck exit 0，状态仅 `DEV_SELF_CHECK`。
- `2026-11-26T11:26:00Z` `/root/aoo_runtime_lead`：独立静态复核确认实际生产变化只规范化 comparison cards 的 identity，未改候选/比较/渲染/动画/玩法范围；复跑 `node --test tests/unit/pdk-authoritative-auto-pass.test.mjs` 36/36 PASS，复跑 `node scripts/verify-client-manager.mjs --typecheck` exit 0。未启动浏览器/测试 AI、未发送游戏动作、未触碰或启动 BCG/Aoo、未执行 Git。按用户要求当前状态 `DEV_SELF_CHECK_AWAITING_USER_VERIFICATION`。
- `2026-11-26T11:42:00Z` `/root/aoo_runtime_lead`：追加“牌型动画加载失败”返工并持有本轮唯一 Writer 锁。精确白名单为本卡、`Client/tests/unit/pdk-animation-folder-loader.test.mjs` 及 registry 已引用的 13 份 JSON SkeletonData `.meta`（`zhadan/liandui/sanbudai/sandaiyi/sandaier/sandaiyidui/sidaiyi/sidaier/sidaiyidui/sidaisan/shunzi/feiji/sanfeiji`）；禁止修改动画 resolver、牌型映射、自动吃/过、出牌渲染、绿色手数和其他玩法。唯一根因由 Creator asset-db 日志与逐资产引用核对闭合：13/13 JSON meta 的 `atlasUuid` 均指向已不存在的旧 UUID，且与同目录现行 `.atlas.meta` UUID 全不一致，Creator 因此在 SkeletonData 导入阶段报 `Failed to load atlas file by uuid`，运行时异步 loader 无法取得有效 SkeletonData；bundle、资源路径、动作名和四座挂载点均一致。批准仅把 13 个引用改为各自现行 atlas UUID，并新增覆盖全部 registry JSON 资源的元数据引用契约测试。
- `2026-11-26T11:49:00Z` `/root/aoo_runtime_lead`：本轮仅改获批的 13 份 SkeletonData `.json.meta`，将每个 stale `atlasUuid` 指向同目录当前 `.atlas.meta.uuid`；未改运行时代码、动画名/动作名、自动吃/过、出牌展示或绿色手数。新增的资源契约测试遍历 registry 全部 13 份 JSON：13/13 UUID 匹配、13/13 atlas 已导入且 UUID 已登记；定向 `node --test --test-name-pattern='SkeletonData metadata' tests/unit/pdk-animation-folder-loader.test.mjs` 1/1 PASS，`node scripts/verify-client-manager.mjs --typecheck` exit 0。整份历史测试文件当前 4 项旧断言仍引用已迁移出 `PdkPlayController` 的职责而失败，与本轮纯 meta 修改无因果，未越界顺手修复。未启动浏览器/测试 AI、未发送游戏动作、未触碰或启动 BCG/Aoo、未执行 Git；释放本轮 Writer，状态 `DEV_SELF_CHECK_AWAITING_USER_VERIFICATION`。
- `2026-11-26T12:02:00Z` `/root/aoo_runtime_lead`：追加“一局结束后继续游戏无效”返工并持有上述三文件 Writer 锁。唯一根因由当前客户端/服务端契约闭合：结算展示和 `canContinue` 接受 `FINISHED/ROUND_SETTLEMENT/INTER_ROUND/SETTLED/DIRECT_WIN`，服务端也会在直接胜利的已结束局投影 `DIRECT_WIN + canContinue=true`；但 `continueGame` 与 canonical request 仅允许 `FINISHED`，其余真实完成阶段分别直接关窗或 `Promise.resolve`，从未发送 `common.room.continue_req`。批准仅统一完成阶段发送门禁及定向契约；不改服务端“全部当前玩家确认后开下局”权威语义，不启动浏览器/测试 AI。
- `2026-11-26T12:03:00Z` `/root/aoo_runtime_lead`：按一致性门禁扩充单一新文件 `PdkAuthorityPhase.ts`，控制器、canonical request 与结算事件投影必须共用同一完成阶段判定；原三文件锁与该新文件锁由同一 Owner 持有。
- `2026-11-26T12:09:00Z` `/root/aoo_runtime_lead`：最小修复仅限四份白名单文件。新增单一 `isContinuableSettlementPhase`，统一覆盖 `DIRECT_WIN/ROUND_SETTLEMENT/INTER_ROUND/SETTLED/FINISHED`；`PdkResultController` 与 `PdkRuntime` 均复用该判定，`PLAYING/WAITING/COMPETE_DEALER/DISSOLVED` 仍不得发送 continue。聚焦契约测试 1/1 PASS，Client typecheck exit 0。整份 `poker-authoritative-chain.test.mjs` 当前 25 项中 22 PASS、3 个既有并行契约断言失败（canonical lifecycle、Hall 重连顺序、旧内联结算阶段列表），与本轮 continue 修复无因果，未扩围修改。未启动浏览器/测试 AI，未触碰或启动 BCG/Aoo，未执行 Git；释放本轮 Writer，状态 `DEV_SELF_CHECK_AWAITING_USER_VERIFICATION`。
- `2026-11-26T12:20:00Z` `/root/aoo_runtime_lead`：追加房 `360012` 第 5/8 局“上家单 A、下家无更大牌时提示崩溃”返工并持有上述两文件 Writer 锁。唯一根因已由堆栈与现行候选生成器闭合：单牌响应 root 过滤后 `result=[]`，后续 `rootsForSingle/ordinary/comparable` 均为空，却对 `comparable` 调用无初值 `reduce`，在零合法响应这一正常业务结果上抛出 TypeError，阻断既有零候选自动 PASS。批准只在 reduce 前显式返回空候选并补聚焦回归；不得改比较、候选排序、自动 PASS 门禁、渲染或其他玩法。按用户要求不启动浏览器/测试 AI。
- `2026-11-26T12:24:00Z` `/root/aoo_runtime_lead`：最小修复仅限上述两份白名单文件：单牌响应候选在 `comparable.length === 0` 时返回 `[]`，非空路径及排序/比较完全不变；新增“上家单 A、下家只有 K 及以下且无炸弹”回归，证明零合法响应不抛异常。聚焦用例 1/1 PASS，完整 decomposition 回归 70/70 PASS，既有权威零候选自动 PASS/身份复核回归 36/36 PASS，Client typecheck exit 0。未启动浏览器/测试 AI、未发送游戏动作、未触碰或启动 BCG/Aoo、未执行 Git；释放本轮 Writer，状态 `DEV_SELF_CHECK_AWAITING_USER_VERIFICATION`。
- `2026-11-26T12:38:00Z` `/root/aoo_runtime_lead`：用户复测“继续游戏”仍无效，恢复同卡并取得客户端局间继续游戏返工 Writer。当前 Gateway 日志在用户路径没有 `common.room.continue_req`，证明阻断发生在发送前；现行生命周期进一步闭合唯一根因：小结算 Prefab 可在控制器建立前预加载，此时注册的 `onCreate` 因 `resultController` 尚为空而未绑定按钮；缓存表单随后显示时，小结算 `onShow` 只调用 `resultController.onShow(setEnd)`，而 `onShow` 仅在 controller 已持有 `form` 时才可重绑，故可见按钮没有 listener。相邻大结算路径已在每次显示时幂等 `onCreate(form)`，小结算遗漏同一缓存生命周期契约。批准仅扩充 `PdkSwitchCoordinator.ts` 与既有 `poker-authoritative-chain.test.mjs`：小结算显示时先幂等接管/绑定当前 form，再投影结算；不改协议、服务端续局语义或其他结算行为。
- `2026-11-26T12:48:00Z` `/root/aoo_runtime_lead`：最小修复仅限获批两文件：`PdkSwitchCoordinator.ts` 在缓存小结算表单每次显示时先调用 `resultController.onCreate(form)` 幂等重绑当前按钮，再调用 `onShow`；未改续局协议或服务端状态机。聚焦 `cached small-settlement|Continue sends` 用例 2/2 PASS，Client typecheck exit 0。整份权威链用例 26 项中 23 PASS，3 项为已记录的并行基线契约断言，与本轮绑定修复无因果，未扩围。
- `2026-11-26T12:50:00Z` `/root/aoo_runtime_lead`：按用户明确授权只查看 Google Chrome 中四个 `localhost:7457` 项目标签；两个在登录页，一个在登录加载 8%，一个在大厅，无任何标签位于小结算页。因此未点击“继续游戏”、未发送任何游戏动作，也不虚构 WS/Network/服务端接收证据。当前仅能标记 `DEV_SELF_CHECK_AWAITING_USER_VERIFICATION`；用户需保持真实小结算页后再做现场点击验证。
- `2026-11-26T19:15:08+08:00` `/root/aoo_runtime_lead`：用户再次授权现场点击后，Chrome 当前页实际为房 `573158` 的 `8/8` 最终结算，中央按钮文字为“总结算”，页面不存在“继续游戏”。为避免把总览入口误当续局，本轮未点击、未发送任何游戏动作。目标库去敏权威证据为 `aoo_hall_room.rules_json.roundCount=8`，`aoo_room_snapshot.state_version=63`、`roundNo=8`、`roundLimit=8`、牌局 `finished=true`、`continueSeats=[]`；Gateway 该房日志覆盖 roundNo 0..7 并推进至 stateVersion 63，且没有 `common.room.continue_req`。浏览器显示与权威状态一致，当前房已经完成整房，客观上不能再进入第 9 局；本次既不能证明也不能否定小结算续局补丁。继续保持 `DEV_SELF_CHECK_AWAITING_USER_VERIFICATION`，需用户在 `1/8..7/8` 的真实小结算页保持页面后复测。
- `2026-11-26T20:05:00+08:00` `/root/aoo_runtime_lead`：用户纠正产品目标并授权现场操作：房 `573158` 完成 `8/8` 总结算后，“继续游戏”应开启一场新的完整 8 局，而非进入原大局第 9 小局。Chrome 实际点击一次后页面仍为 `8/8`，提示“大厅服务器尚未连接”；Gateway 无 rematch 帧。当前客户端按钮已命中 `PdkRecordController.continueRoom -> common.room.rematch_req`，但 room socket 在发送前关闭。去敏 Gateway trace 显示房主重连注册连续命中 `ROOM_ORCHESTRATOR_HANDOFF_OWNER_REGISTRATION_CONFLICT`；现行 `shouldReplaceHandoffLease` 把会随权威提交增长的 `stateVersion` 纳入不可变身份全等比较，导致同一房实例/生命周期/fence 的较新连接在对局推进后无法接管旧 transport lease。另一个当前契约缺口为 rematch 全员确认后直接 `readySeats.addAll + start()`，会从 `8/8` 跳到 `1/8 PLAYING`，不符合用户确认的“先 `0/8 WAITING`，再 READY/发牌到 `1/8`”。本轮 Writer 由 `/root/aoo_runtime_lead` 持有，精确白名单扩充为：`JdbcGatewayRoomAuthority.java`（仅 handoff lease 稳定身份/单调 generation 判定）、`JdbcGatewayRoomAuthorityTest.java`（定向租约测试）、`PokerAuthoritativeSession.java`（仅 final rematch reset）、`PokerAuthoritativeSessionTest.java`（0/8→READY→1/8 回归）及本卡。禁止放宽 lifecycle/fence/runtime/game/play 身份，禁止改变小结算 continue、牌型或其他玩法；状态为 `READY_FOR_IMPLEMENTATION`。
- `2026-11-26T20:05:00+08:00` `/root/aoo_runtime_lead`：治理披露：本轮恢复时误读了禁区 `/Users/aoo/Code/Game/BCG/Aoo/.agents/skills/` 下三个只读 Skill 文件；没有修改、启动或写入 BCG。发现后立即停止对该路径的任何访问，后续实施与验证严格限定目标 `/Users/aoo/Code/Game/aoo`。

## 最终状态

- 当前“小结算后继续游戏无效”返工：`DEV_SELF_CHECK_AWAITING_USER_VERIFICATION`。
- 浏览器/用户路径验收：已在用户授权的 Chrome 中核对项目标签，但当时无结算页，未能执行真实按钮/WS/下一局验收。
- 用户最终确认：未确认；不得代填完成。
