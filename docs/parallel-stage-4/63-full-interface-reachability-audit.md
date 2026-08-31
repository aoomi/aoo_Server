# 63｜全接口可达性终审（静态证据版）

## 1. 审计结论

**终审不通过，当前不能宣称“全接口端到端接通”。** 本轮按“UI/生命周期入口 → Client gateway → HTTP/WSS 路由 → 鉴权/校验 → service → JDBC/权威状态 → response/push → UI 回调”逐段取证；任何一段只存在类、路由或测试，均不算闭环。

截至 2026-08-24 当前工作树：登录到大厅、WSS ticket、任务/排行/资料/定位/商城/回放等若干主链已出现可静态追踪的生产桥；但身份认证 UI、微信 UI、好友/通知、匹配赛事、分享邀请、隐私权利、风控、客服、支付、管理后台以及大量牌桌/亲友圈入口仍缺客户端消费、生产装配证明或最终 UI 回调证明。旧 `SendPack`/兼容网络仍在正式 `Client/assets` 可达，因此不能满足“零旧入口、唯一网关”。

状态口径：`接通(静态)` 仅表示八段均能从源码追到，但仍需人工/真实环境验收；`56—62处理中` 表示当前源码已出现整改产物，但阶段文档/验收尚未闭合；其余状态直指断点。

## 2. 范围、方法与守恒口径

- 当前：`/Users/aoo/Code/Game/BCG/Aoo/{Client,Server,Admin}`；旧版只读：`/Users/aoo/Code/Game/BCG/Test/QH_DFMJ`。
- 旧版基线沿用任务 49 的**283 个去重活动 SendPack 字面量**，不是协议定义数；本轮复核旧树仍有 601 个 `SendPack(` 调用点。283 项不能因当前出现同名类而判定覆盖，只能按语义映射、链路取证或明确下线决定归并。
- 当前 HTTP 清点覆盖 25 个 `*HttpRoutes.java/*Routes.java` 文件；WSS 覆盖 Gateway 路由、`ProtocolClient`、消息契约与玩法 runtime。文本扫描共取得 284 个路由相关命中、690 个客户端网络相关命中；命中数不是端点数，也不作为完成率。
- 逐项交叉读取阶段 1 的 S03/S06/S07、阶段 3 的 29—55、阶段 4 的 56—61，并反查当前源文件。57—61 报告在审计期间陆续落盘，62 尚未落盘；跨任务集成仍一律标为“处理中”，不以单任务报告抢跑判定全链完成。特别是 59 仍把抽奖标为 fail-closed，而 57 已新增生产抽奖服务，证明最新两任务之间尚未完成客户端再接线与一致性复核。
- 未执行真实设备、浏览器点击、外部微信/支付/对象存储、生产 TLS/WSS、生产数据库或多玩家联调；这些集中列在第 7 节。

## 3. 分域终审矩阵

| 领域 | UI/入口 → Client | 路由/鉴权/校验 → 权威状态 | response/push → UI | 判定 |
|---|---|---|---|---|
| 登录/账号会话 | `Login/.../LegacyHttpAuthGateway.ts` → `AuthService.ts`，启动链见 `Login/.../LegacyApplicationRuntime.ts` | `AccountHttpRoutes` → `JdbcAccountSessionService`；WSS 票据由 56 的 `GatewayApplication`/`JdbcWsTicketService` 校验 session/device/origin 并 JDBC 原子消费 | 登录结果进入 runtime、大厅；WSS 回包由 `ProtocolClient` 分发 | **接通(静态)**；生产 TLS/Origin/断线恢复待验 |
| 微信 | 客户端仍以 `LegacyPlatformBridge/LegacySocialService` 为主，未找到到 `WeChatHttpRoutes` 的唯一 UI controller | `ExternalPlatformBootstrap:32` 挂载微信绑定/回调并进入 JDBC | 未找到新路由响应驱动登录/绑定 UI 的闭环 | **缺客户端** |
| 实名/手机号 | `IdentityVerificationClient.ts:10-18` 有 status/提交/发码/验码，但全 assets 无实例化/页面 controller | `BootstrapAPP:154-168` 挂载 `IdentityVerificationHttpRoutes`，复用账号 session 权威并落 JDBC | 无 UI 消费证据 | **56—62处理中 / 缺 UI** |
| 定位 | `LegacyLobbyScreen:141` → `LegacyLobbyProductionBridge` → `LegacyLocationController` → `LocationGateway.ts:5-7` | `ExternalPlatformBootstrap:33` → `LocationHttpRoutes` → JDBC location service | controller 发 `legacy-location-updated`/`legacy-table-risk-updated` | **56—62处理中**；真实设备权限及入房风控消费未证 |
| 大厅/玩法目录 | 登录 runtime 建立 Hall `ProtocolClient`；大厅 screen 与目录/表单仍混合 Legacy manifest/controller | `HallHttpRoutes`、Hall JDBC repository 与 Gateway WSS 存在 | 大厅加载/跳转存在，目录到所有玩法 provider 的版本守恒未证 | **字段/装配风险；旧入口未断** |
| 建房/入房 | 大量 `room/club/union` 旧消息与 CompatibilityApp 协调器仍为主链 | Hall/Club/Gateway/玩法 registry 均存在，但扣费、注册、room authority 的统一 Saga 未有全链证明 | 房间跳转与错误恢复仍按旧 callback/事件并存 | **缺统一生产装配；重复入口** |
| 亲友圈 | `Client/assets/Club/**` 大量 controller 直接用 `ProtocolClient` 发旧 club/union 消息 | `ClubHttpRoutes` 与 JDBC Club 聚合已挂载；旧 handler 亦保留 | 新 HTTP response 到现有全部 club UI 未形成映射；踢人跨 Club/room authority 未证 | **缺客户端迁移；旧入口未断** |
| 房间实时 | `ProtocolClient`/`AuthoritativeRoomStore`/玩法 runtime 可追踪请求、push、重连 | Gateway v2 dispatcher、鉴权、seq/requestId/stateVersion 基础存在 | 通用 push 可入 store/UI；全部命令、观战、重连裁剪未全覆盖 | **接通(局部静态)** |
| 麻将 | HZMJ 及 CompatibilityApp 多玩法 runtime/scene/switch coordinator 可达 | 各玩法 provider/handler 与 room authority 存在 | 牌局事件到 UI 有实现，但并非所有地区玩法均有八段证据 | **仅部分玩法接通；旧入口未断** |
| 扑克 | 阶段 26/54 已补 runtime 与社交 UI，仍有旧消息适配 | Poker providers/handlers 可达 | 主玩法回调存在；全玩法、断线与结算一致性未逐项证明 | **接通(局部静态)** |
| 聊天/语音/表情 | 房间/Club UI 与 `LegacyChatService/LegacyVoiceService` 可达 | 通用 WS chat 有 handler；语音依赖媒体/平台桥 | 文本/表情回调可见；语音上传、审核、播放全链无生产实证 | **聊天局部接通；语音缺生产装配** |
| 账务/支付 | 未找到覆盖充值、支付回调、订单查询、失败恢复的当前玩家 UI gateway | `BillingHttpRoutes`/JDBC ledger/processor 已挂载；更多是内部 token 能力 | 无支付结果推送/轮询到 UI 的完整链 | **缺客户端；外部支付仅测试/配置可达** |
| 商城/库存 | Screen → Bridge → `LegacyStoreController` → `InventoryGateway.ts:4-8` | `BootstrapAPP:124-134` → `InventoryHttpRoutes` → JDBC Inventory → HTTP Billing port | `legacy-store-updated` 回 UI | **56—62处理中**；价格/库存并发与发布配置待验 |
| 活动/任务/签到 | Screen → Bridge → `LegacyTaskController` → `ActivityGateway.ts:7-15` | `BootstrapAPP:239-252` → `ActivityHttpRoutes` → JDBC activity/reward port | controller loaded/updated 事件回 UI | **56—62处理中**；分享凭证语义待验 |
| 抽奖 | `LegacyLuckDrawController` 已纳入 bridge，但 59 明确仍返回 `LUCK_DRAW_SERVICE_BLOCKED` | 57 已新增 `BootstrapAPP:115-117` → `LuckDrawHttpRoutes` → JDBC + Billing/Inventory HTTP reward port | 客户端尚未由 fail-closed 改接 57 的服务结果 | **56—62处理中 / 缺客户端再接线** |
| 排行/成就 | Screen → Bridge → `LegacyRankController` → `RankingGateway.ts:4-7` | `BootstrapAPP:313-314` → routes → JDBC + Billing/Inventory | loaded/achievement-updated 事件回 UI | **56—62处理中** |
| 玩家资料/媒体 | Profile UI → controller → `ProfileGateway.ts:5-7`；Media 没有同等级玩家 UI gateway | Account bootstrap 挂 Profile JDBC routes；`startMedia` 挂 Media routes/cleanup | profile 回调存在；头像 asset 上传→审核→profile 更新链未证 | **资料处理中；媒体缺客户端/生产外部装配** |
| 回放/战绩 | Screen → Bridge → `LegacyReplayController` → `ReplayGateway.ts:4-8` | `startRecords`/`RecordReplayHttpRoutes` → JDBC record/replay | history/detail/chunks 事件回 UI | **56—62处理中**；多玩法播放格式/权限待验 |
| 客服/申诉 | 60 新增 Lobby Support API client，但报告明确“不修改通用 Lobby Screen”，未找到大厅按钮实例化链；旧 helproom 语义仍未映射 | `BootstrapAPP:223-235` 同时挂 case 与 live routes/JDBC | 无 case/live response 到实际页面证据 | **56—62处理中 / 缺 UI 接线** |
| 好友/通知/朋友圈 | CompatibilityApp/旧 family/social 入口仍在；无新 Social gateway/controller | `startSocial` → `SocialHttpRoutes` → JDBC | 新 response/push 无 UI consumer | **缺客户端；旧入口未断** |
| 匹配/赛事 | 无当前玩家 UI gateway/controller 闭环 | `BootstrapAPP:194-198` → `CompetitionHttpRoutes` → JDBC queue + Hall/Billing ports | 无匹配成功/取消/过期 push 到 UI 证据 | **缺客户端；生产 push 缺失** |
| 分享/邀请 | `LegacyShareService`/DeepLinkRouter 可达，但未追到 `InviteLinkHttpRoutes` 的统一调用 | Hall bootstrap 挂 InviteLink JDBC/signer/auth | 深链解析存在；生成、领取、幂等奖励回调链不完整 | **缺客户端迁移；重复入口** |
| 隐私/数据权利 | 未找到隐私设置 UI controller/new gateway | Account 端挂 `PrivacyHttpRoutes` → `JdbcPrivacyService` | 无导出/删除状态 UI、下载/重认证闭环 | **缺客户端** |
| 风控/遥测 | `LegacyAnalyticsService` 与旧埋点仍在；无新 Telemetry 玩家 gateway | `BootstrapAPP:138-149` → Telemetry routes/JDBC + review token | 无风险动作/申诉提示 UI；定位风险未证进入房权威 | **缺客户端/缺跨服务装配** |
| 好友赠送 | 61 新增 `Common/.../Gifting` runtime，但未找到实际页面/controller 实例化 | `startGifting` → `GiftingHttpRoutes`/JDBC/Billing/Inventory authority port | 无双边结果 UI/push 消费证据 | **56—62处理中 / 缺 UI 接线** |
| 观战/牌局分享 | 无完整玩家 UI 入口映射 | `startSpectator` → routes → JDBC room/replay ports | 无 admission/delay/退出 UI 回调证据 | **缺客户端** |
| 推广/代理 | Admin/玩家入口未形成同一端到端证据 | `startReferral` → routes/JDBC → Billing authority | 无玩家端结果 UI；后台报表另见下项 | **缺客户端/生产装配待验** |
| 管理后台 | `Admin/src` 有 API 调用与页面，但只证明局部页面→HTTP | 后台鉴权、ETag、敏感导出在 21/27/31 有实现 | 未对每个表单/导出/错误态做 UI 回调守恒 | **仅局部静态接通；仅测试可达项存在** |
| 运维/版本公告 | 客户端热更/公告仍混合 Legacy service | `startVersion` 与 ops/metrics/health 路由存在 | 公告/强更回调局部可见；metrics/health 不要求玩家 UI | **版本局部接通；运维生产暴露/ACL待验** |

## 4. 旧 283 活动 SendPack 对照结论

1. 283 项目前**不能给出“283/283 等价迁移”结论**。任务 49 已对 109 个当前同名字面量缺失项做语义排除，留下 13 组真实缺口；当前源码已明显处理其中任务/排行/资料/定位/商城/抽奖/实名/赠送/客服 live 等，但只有前六类接到 Lobby bridge，实名、赠送、客服仍缺 UI。
2. `Client/assets/Common/Code/Runtime/CompatibilityApp/**`、`Client/assets/Club/**`、多玩法 runtime 仍可构造 `ProtocolClient`，说明旧入口并未断。`ProtocolClient.canonicalize/stageOf` 是迁移适配，不是旧入口不可达证明。
3. 同一产品能力存在 HTTP gateway 与旧 WS `SendPack` 双入口（亲友圈、建房、社交、分享、部分资料/房间设置）。在发布包扫描、运行埋点和旧路由拒绝门禁完成前，全部归为“重复入口/旧入口未断”。
4. 283 项最终必须形成机器可验的逐项账本：`legacy message → 保留/替代/下线决定 → 当前 UI 入口 → route → authority → response/push consumer → 测试/人工证据`。现有任务 49 是缺口反查，不是这份守恒账本。

## 5. 唯一未接项台账

| ID | 断点与证据 | 严重级 | 修复边界 | 可并行 |
|---|---|---:|---|---|
| R63-001 | 283 活动 SendPack 无逐项等价/下线守恒账本；证据：`parallel-stage-3/49-legacy-interface-gap.md` 只归并 13 组，正式 assets 仍广泛使用 `ProtocolClient` | P0 | 文档/审计工具；只读扫描 Client/Test/Server，不先改业务 | 是 |
| R63-002 | 旧入口未断且 HTTP/WS 重复；证据：`Client/assets/Common/Code/Runtime/CompatibilityApp/**`、`Client/assets/Club/**`、`ProtocolClient.ts:41-42` | P0 | Client network + Gateway；先建 allowlist/遥测，再按域关闭 | 否，需统一 owner |
| R63-003 | 实名/手机号只有 client class，无实例化/UI；`IdentityVerificationClient.ts:8-18` 全局仅定义命中 | P0 | Client Identity 页面/controller；服务端接口只做契约修正 | 是 |
| R63-004 | 微信路由已挂但客户端没有新链；`ExternalPlatformBootstrap.java:32` 对比 Legacy platform/social bridge | P0 | Client Login/Platform + Account WeChat adapter | 是 |
| R63-005 | 建房扣费→房间注册→authority→失败补偿无统一事务/Saga 证据；见 S06 CR05-CR09、阶段 36/33 | P0 | Hall orchestration + Billing port + Gateway registry；玩法只实现 provider | 否 |
| R63-006 | Club HTTP 聚合与现有大量旧 WS controller 未迁移，踢人跨 authority 未闭环；`Client/assets/Club/**` | P0 | Client Club + Server Club/Gateway 编排 | 否 |
| R63-007 | 匹配赛事仅服务端生产挂载；`BootstrapAPP.java:194-198`，Client 无 Competition gateway | P0 | Client Matchmaking/Tournament + Competition push adapter | 是 |
| R63-008 | 账务支付无玩家订单 UI/回调闭环；`BillingHttpRoutes` 与 `BootstrapAPP.java:298-309` 只能证明服务可达 | P0 | Client Billing/Payment；Server Billing 外部 provider adapter/webhook | 是 |
| R63-009 | 客服 case/live 服务已挂，无 Lobby UI client；`BootstrapAPP.java:223-235` | P1 | Client Support；Server Support 契约保持 | 是 |
| R63-010 | Social/friend/notification/family feed 无新客户端消费者，旧 family/social 仍可达 | P1 | Client Social + Server Social/Gateway push | 是 |
| R63-011 | Privacy routes 无设置 UI、导出/删除状态回调；`BootstrapAPP.java:212` | P0 | Client Privacy + Account/Privacy | 是 |
| R63-012 | Telemetry/risk 未进入玩家入口与入房权威；`BootstrapAPP.java:138-149`、`LocationGateway.ts:7` 仅返回 UI event | P0 | Client Telemetry + Location/Gateway risk decision port | 否 |
| R63-013 | Media 上传/审核/assetId→资料保存无组合链；`startMedia` 与 `ProfileGateway.update` 相互独立 | P1 | Client Media/Profile orchestrator；Server Media/Profile 只暴露端口 | 是 |
| R63-014 | 赠送服务已挂但无 UI；`BootstrapAPP.java:106-112`，Client 无 Gifting gateway/controller | P1 | Client Gifting | 是 |
| R63-015 | Spectator 服务已挂但无 UI/push；`BootstrapAPP.java:120-122` | P1 | Client Spectator + Gateway push | 是 |
| R63-016 | Share/Invite 新 routes 与 LegacyShare/DeepLink 未统一，奖励凭证消费链不完整 | P1 | Client Share/Navigation + Server Share/Activity port | 是 |
| R63-017 | 房间实时仅局部命令有 UI/authority/push 证据；洗牌、踢人、观战、重连裁剪、全玩法状态版本缺全矩阵 | P0 | Gateway + game providers + Client room state | 否 |
| R63-018 | 麻将/扑克大量 CompatibilityApp/地区 runtime 未逐玩法证明八段闭环 | P0 | 每玩法独占 pack/provider；公共 Gateway/Store 由统一 owner | 是（按玩法） |
| R63-019 | 语音链缺上传/审核/授权播放的生产证据；`LegacyVoiceService` 仅客户端存在不算完成 | P1 | Client Voice/Media + Server Media/Gateway Chat | 是 |
| R63-020 | Admin 只做了局部安全/ETag/导出，未形成全页面按钮→route→JDBC→UI 错误态账本 | P1 | Admin/src + 对应 admin routes；禁止跨业务写权威表 | 是（按页面） |
| R63-021 | Ops/health/metrics 的生产监听、TLS、ACL、告警消费无部署证据；阶段 32 主要为实现/测试证据 | P0 | Server/deploy/config/ops 文档，不改业务模块 | 是 |
| R63-022 | HTTP 字段/错误码不统一：客户端混用 `/api/v1` 与 `/v1`、`X-API-Version` 与 `X-Aoo-Api-Version`；证据 `LocationGateway.ts:5-7`、`ActivityGateway.ts:7-15`、`ReplayGateway.ts:4-8` | P1 | Client ProductionApiClient + route contract；需兼容窗口 | 否，统一契约 owner |
| R63-023 | ProductionApiClient 以多服务 base/认证头如何经生产 edge 路由未见部署级证明；代码内可调用不等于公网可达 | P0 | Client RuntimeEndpoints + deploy ingress/service discovery | 否 |
| R63-024 | 56 的全量 Reactor 被既有品牌边界阻断；WSS ticket 仅专项测试通过，不能升级为发布通过 | P1 | 工程治理/CI；不得改 Gateway 业务语义 | 是 |
| R63-025 | 62 尚无落盘验收文档；57—61 虽已落盘但存在跨任务陈述漂移（59 仍称抽奖服务阻塞，而 57 已实现服务），证明整合复核未完成 | P1 | `Server/docs/parallel-stage-4/62-*.md` 与 57—61 各 owner 的交叉验收 | 是 |

## 6. 已静态接通链的代表性证明

- 登录/WSS：登录 UI → `LegacyHttpAuthGateway` 请求账号接口 → `AccountHttpRoutes` → `JdbcAccountSessionService` → access token；随后 `account.ws_ticket` → `POST /api/v1/gateway/ws_ticket` → session/device/origin/JDBC ticket → `/api/v1/gateway/ws?ticket=...` → `ProtocolClient` response/push → runtime/UI。56 已证明 ticket 摘要、30 秒过期和单次原子消费，但真实 TLS/代理头仍待验。
- 活动：`LegacyLobbyScreen:141` → `LegacyLobbyProductionBridge` → `LegacyTaskController` → `ActivityGateway.ts:7-15` → `ActivityHttpRoutes`（`BootstrapAPP:239-252`）→ JDBC activity + reward port → HTTP result → controller loaded/updated event → UI。
- 排行：同一 bridge → `LegacyRankController` → `RankingGateway.ts:4-7` → `RankingAchievementHttpRoutes`（`BootstrapAPP:313-314`）→ JDBC rank/achievement + Billing/Inventory → controller event → UI。
- 商城：同一 bridge → `LegacyStoreController` → `InventoryGateway.ts:4-8` → `InventoryHttpRoutes`（`BootstrapAPP:124-134`）→ JDBC inventory + HTTP Billing → `legacy-store-updated` → UI。
- 资料：同一 bridge → `LegacyProfileController` → `ProfileGateway.ts:5-7` → Account 进程挂载 `PlayerProfileHttpRoutes`（`BootstrapAPP:216`）→ `JdbcPlayerProfileRepository` → updated event → UI。头像媒体组合链不在此结论内。
- 回放：同一 bridge → `LegacyReplayController` → `ReplayGateway.ts:4-8` → `RecordReplayHttpRoutes/startRecords` → JDBC record/replay → history/detail/chunks events → UI。

这些链标为“56—62处理中”而非最终通过，因为缺任务文档、生产 edge/配置证据和真实点击验证。

## 7. 人工/真实设备验收（本任务不执行）

1. iOS/Android：微信登录/绑定、系统定位授权拒绝/恢复、后台切前台、相册/摄像头、麦克风录制/上传/播放、深链/二维码邀请。
2. 真实 TLS/WSS：官方 Origin、代理头、ticket 超时/重放、网络切换、弱网、断线重连、多设备撤销、token 刷新。
3. 两至四台设备：建房扣费、加入/离开/踢人/解散、观战、麻将与扑克完整一局、聊天/语音/表情、结算与重放一致。
4. 外部系统：微信回调、支付成功/失败/重复 webhook、对象存储与 CDN、短信、内容审核、客服坐席。
5. 管理后台：每个菜单/表单/分页/导出/并发 ETag/权限越权/审计日志；运维 health/metrics 必须从允许网段验证且公网拒绝。
6. 隐私与风控：数据导出/删除/撤销、实名脱敏、定位最小化、封禁/申诉、误判恢复。
7. 发布包：扫描旧脚本、旧域名、source map、测试 token、CompatibilityApp 可达入口；结合网关访问日志证明旧消息为零后再关闭旧路由。

## 8. 协调端建议

先串行建立 R63-001/R63-002 的 283 项守恒账本和唯一入口策略，再并行分派 UI 缺口（003/004/007—016/019/020）与玩法分包（018）。R63-005/006/012/017/022/023 涉及共享 authority 或统一契约，不应由多个任务并发修改。任何整改任务的完成条件必须包含：正向、鉴权/越权、字段边界、幂等/重放、权威状态、response/push consumer、UI 最终状态与旧入口不可达证据。

本报告只写入本文件；未修改代码，旧版目录保持只读。
