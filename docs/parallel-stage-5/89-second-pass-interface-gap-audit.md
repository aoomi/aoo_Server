# 89｜第二次全接口遗漏审计

审计快照：2026-08-24；当前树 `/Users/aoo/Code/Game/BCG/Aoo`；旧树 `/Users/aoo/Code/Game/BCG/Test` 只读。

## 1. 结论

**二次终审不通过。** 63 的 25 个缺口在 64—87 中虽已有大量专项产物，但专项报告“完成”不等于当前合并快照的端到端验收。本次从入口语义、反向消费者、数据库权威和运维入口四个方向重建可达图，新增 18 个未被 63 独立列项的缺口（`R89-001`—`R89-018`）。其中 P0 7 项、P1 10 项、P2 1 项。

最关键的新事实是：账号服务存在注册、刷新和服务端注销，但当前客户端只接游客注册/刷新，`AuthService.logout()` 仅清本地状态；通知只消费 notification，不消费 mail；公告仍走旧 WSS `game.CSystemNotice`；举报仍打开旧 PHP URL；管理后台存在多处 `/admin/**` 直接写操作和占位上传，任务 80 的新 ledger 不能覆盖这些未迁移按钮。任务 84—86 也不能证明所有玩法接通：86 明确记录 18 个玩法缺客户端实现，85 记录 XCPDK 独立测试 classpath 阻断。

排除项：按要求不审计内容审核、未成年人保护、机器人智能。语音/头像/客服附件仅审计上传下载鉴权、生命周期与消费者，不评价内容。

## 2. 审计方法与可达图

每项能力按下图取证；任一节点仅在测试树、只有类定义、只有路由、只有 prefab 名称或只有专项自报时，均不算接通。

```text
UI按钮 / prefab事件 / 启动入口 / 定时任务
  -> Client controller -> 唯一 gateway
  -> HTTP(S) / WSS edge route
  -> 账号鉴权 + 角色权限 + 字段/版本/幂等校验
  -> service / 跨服务 authority port
  -> JDBC表 / 权威房间状态 / 持久事件
  -> 标准 response / push / catch-up
  -> controller状态机 -> 实际 UI / 定时任务消费者
```

交叉面：当前 `Client/Server/Admin` 全量源码与 prefab/scene；旧版活动 `SendPack` 基线和当前旧入口；SQL 表引用；任务 59—88 报告。新增只读扫描器为 `Server/tools/interface-gap-audit/second_pass_audit.py`，它只枚举事实，不自动判“完成”，避免接口名命中冒充闭环。

状态口径：`已接通` 只代表当前源码静态八段闭合；`任务处理中` 表示仍在总清单中；其他标记分别为 `缺 UI`、`缺客户端`、`缺服务端`、`缺生产装配`、`缺数据库`、`缺响应/推送`、`仅测试可达`、`旧入口仍可达`、`字段/错误码不一致`。

## 3. 二次全域矩阵

| 能力域 | 可达链复核 | 状态 |
|---|---|---|
| 注册/验证码 | `AccountHttpRoutes /v1/account/register` 与 JDBC 注册存在；Login gateway 只暴露 `registerGuest`，没有账号注册 UI；账号注册无 captcha/OTP challenge，手机验证码属于实名绑定而非注册防滥用 | **缺 UI、缺客户端、缺服务端** |
| 刷新 Token | `LegacyHttpAuthGateway.loginWithToken/executeWithRefresh` → `/api/v1/account/token/refresh` → JDBC rotating session → 本地 token 更新 | **已接通(静态)**；错误 envelope/header 见 R89-014 |
| 退出/踢下线/设备/封禁 | 服务端有 `/logout`、operator ban/revoke，Gateway 有 `system.kick_out` 契约；客户端 logout 只清本地；无设备列表/单设备撤销 API/UI；未证 kick push 消费到登录页；后台写链未纳入新 RBAC ledger | **缺客户端、缺服务端、缺响应/推送** |
| 公告/邮件/红点/系统消息 | Social JDBC 有 notification/mail/read cursor；客户端只轮询 notification；公告仍 `game.CSystemNotice`；系统消息仍有重复 Legacy service；无统一 mail UI/red-dot 聚合 | **缺 UI、旧入口仍可达** |
| 版本/热更/配置/功能开关 | Version 服务已挂；客户端 `LegacyHotUpdateService` 和 `ResourceVersionGate` 存在但未追到 Version routes；platform bridge 对 checkVersion 返回 undefined；无玩家功能开关客户端契约 | **缺客户端、缺生产装配** |
| 服务器列表/路由 | Edge 任务 82 有单一 apiBaseUrl；未发现玩家可消费的服务器/区服列表权威接口，旧 role gateway 仍固定版本/旧入口语义 | **缺服务端、旧入口仍可达** |
| 房卡/钻石/金币流水 | Billing 支持 ROOM_CARD/CRYSTAL/GOLD ledger；玩家 Lobby 仍从 role packet 读余额，未发现玩家流水 UI gateway；Admin 多处旧 `/admin/giveRoomCard` | **缺客户端、旧入口仍可达** |
| 退款/补偿/支付回调 | Billing callback/refund 与支付轮询存在；玩家 gateway 状态含 refund，但 controller 专项主要覆盖成功/失败/取消/超时，未见退款发起/补偿明细消费；外部真实 webhook 暂缓 | **缺 UI、缺响应/推送** |
| 亲友圈角色/审批/模板/战绩账务 | Club 新旧链并存；任务 63/67 已指出 authority/Saga；Admin/Client 仍大量旧 controller 与 `/admin/**`；未形成每角色×每写操作×ledger 的机器守恒 | **任务处理中、旧入口仍可达、缺生产装配** |
| GPS/距离/举报/禁聊 | 风控准入任务 73 已接 JDBC；Lobby 举报按钮仍打开 `index.php?module=Report`；部分玩法 GPS 按钮只 toggle/隐藏；禁聊可见旧 gameServer 状态但未见新 UI/权限链 | **旧入口仍可达、缺服务端/客户端** |
| 头像媒体 | 任务 74 组合链和任务 79 语音链已有专项证据 | **已接通(静态)**；真实存储/CDN/设备暂缓 |
| 战绩详情/回放下载播放 | history/detail/chunks 已接；任务 84—86 不能覆盖缺客户端的 18 个玩法，故“所有玩法可播放”不成立 | **局部接通、缺客户端** |
| 邀请分享回流 | 任务 77 仍列处理中；deep link 新链存在，旧分享入口与最终奖励/真机回流仍未统一验收 | **任务处理中、旧入口仍可达** |
| 客服附件 | 工单/live 文本链任务 70 已接；Support model/routes 未形成玩家附件 upload→asset ACL→case message→download 授权链 | **缺客户端、缺服务端** |
| 活动/任务/签到/领取 | Activity gateway 有 task/sign-in/claim 链；旧活动 SendPack 守恒依赖 64 台账，合并快照仍需统一点击/幂等复验 | **已接通(静态)、待统一稽核** |
| 商城库存兑换 | Inventory gateway/service 已接；库存并发与 Billing port 有专项实现；玩家流水/补偿展示缺失 | **已接通(静态)**，关联 R89-007/008 |
| 匹配赛事 | 68 自报静态闭合但总清单仍为处理中，且真实 push 用轮询替代；不得直接验收 | **任务处理中、缺生产环境验收** |
| 观战 | 76/78 有申请、授权、延迟视角、增量与重连 | **已接通(静态)**；麻将/扑克真机泄露复核暂缓 |
| 房间通用命令 | ready/unready/start/dissolve/kick/shuffle/托管/reconnect/leave 等分散在 canonical 与 CompatibilityApp；67 明确生产 Saga fail-closed，86 明确 18 玩法无客户端 | **任务处理中、旧入口仍可达、缺生产装配** |
| 麻将全操作/结算 | 84 对已列玩法有八段证据；不能外推到 86 缺客户端玩法 | **局部接通、缺客户端** |
| 扑克全操作/结算 | 85 有 provider/authority/UI；XCPDK 定向测试仍被 runtime classpath 阻断 | **仅部分测试可达、缺生产装配证明** |
| 后台每个写操作 | 80 ledger 覆盖新 operationsApi 区域；现有 Vue 仍有 `/admin/giveRoomCard`、`setClubFreeRoomCardTime` 等旧直写及 jsonplaceholder 上传 | **旧入口仍可达、仅测试可达** |
| 运维 health/metrics/log/config refresh | 81/82 具部署静态证据；真实 ACL/TLS/告警暂缓；未发现统一受权 config-refresh 写操作及其审计闭环 | **缺生产环境验收、缺服务端(配置刷新控制面)** |

## 4. 新增缺口台账

| ID | 缺口与证据 | 级别 | 可并行整改边界 |
|---|---|---:|---|
| R89-001 | 账号注册服务端存在但客户端只有游客注册；`Client/assets/Login/Code/Runtime/auth/LegacyHttpAuthGateway.ts` 对比 `Server/server/Account/.../AccountHttpRoutes.java` | P0 | Client Login 注册 UI/gateway + Account 注册契约；不碰 WSS |
| R89-002 | 注册验证码/captcha challenge、校验、限流权威链缺失；Identity 手机 OTP 不能替代账号注册验证码 | P1 | Account captcha/OTP 独立模块、Client Login；不含内容审核 |
| R89-003 | `AuthService.logout()` 只清本地，未调用 `/v1/account/logout` 撤销 token family | P0 | Client Login/Auth 单域；Account 路由保持兼容 |
| R89-004 | 无设备列表和单设备撤销 API/UI；现有 `aoo_account_device` 仅登录时 upsert，operator revoke 是全账号 | P1 | Account devices routes/JDBC + Client Settings |
| R89-005 | ban/revoke 后 `system.kick_out` 从 Gateway 发出并被客户端消费、清 session、回登录页的链无证据 | P0 | Gateway session push + Client network/Auth；与账号 owner 串行装配 |
| R89-006 | Social client 只调用 notifications/read，不调用 `/v1/mail`；公告仍由 `LegacyNoticeBarController` 请求 `game.CSystemNotice`，无统一红点 | P1 | Client Social/Mail/Notice + Social push/cursor；公告旧 WS 关闭由 Gateway owner |
| R89-007 | 玩家无房卡/钻石/金币流水 UI，余额仍来自 `LegacyRoleGateway` packet；Billing `ledger` 仅有服务能力 | P1 | Client Wallet + Billing player-query route；不改结算引擎 |
| R89-008 | refund/compensation 的玩家状态、原因、到账流水和通知消费不完整；PaymentGateway 虽声明 REFUND 状态，未形成退款 UI 链 | P1 | Client Billing + Billing refund query/notification adapter |
| R89-009 | 举报入口仍打开旧 PHP `index.php?module=Report`；`LegacyLobbyScreen.ts:880` | P0 | Client Report UI + 新 Report service/JDBC/Admin；可独立于聊天 |
| R89-010 | 禁聊/房间举报/GPS距离的统一新 gateway、权限与错误回调缺失，部分玩法 GPS 仅本地 toggle | P1 | Client Room Safety + Gateway authority；按玩法 UI 可并行 |
| R89-011 | 客服附件没有 asset upload ticket、case 归属 ACL、下载授权与生命周期组合链 | P1 | Support + Media attachment purpose + Client Support |
| R89-012 | Version routes 未接启动更新链；`LegacyPlatformBridge.checkVersion` 返回 undefined，功能开关/配置刷新无客户端权威消费 | P0 | Client Bootstrap/HotUpdate + Version；运维刷新控制面另 owner |
| R89-013 | 玩家服务器列表/区服选择及服务端路由权威缺失，无法证明维护/灰度/迁服选择 | P1 | Version/Directory 新只读路由 + Client Login；不改 edge ingress |
| R89-014 | API 版本头仍并存：统一客户端用 `X-Aoo-Api-Version`，多条新 routes/gateways 要求 `X-API-Version`；Account error 仍为裸 `{"error":...}`，Social/Payment 为 code/data envelope | P0 | 跨 Client/Server 契约 owner，先兼容双读再收敛；不可按模块并发乱改 |
| R89-015 | Admin 任务 80 ledger 未覆盖旧 `/admin/**` 直写按钮；房卡赠送/免费时段等仍直接 request，且 personal 上传指向 jsonplaceholder | P0 | Admin 页面逐域 + AdminApi；先扩展机器 ledger，业务 authority 只走端口 |
| R89-016 | 86 明确 18 个地区玩法缺客户端实现，故任务 84—86 不能支持“所有麻将/扑克/地区玩法已接通” | P0 | 每玩法独占 Client bundle/provider，可并行；共享 Gateway 不并改 |
| R89-017 | XCPDK 定向测试因 runtime classpath 缺 `jsproto...BaseRoomConfigure` 阻断，属于缺生产装配证明而非通过 | P1 | XCPDK module POM/runtime classpath；不改玩法语义 |
| R89-018 | 运维 config refresh 没有受权写路由→持久配置版本→实例确认→审计日志→运维响应的完整链 | P2 | Ops/config-control 独立模块；不得把 health GET 当刷新完成 |

## 5. 与旧版、数据库和任务 59—88 的交叉结论

1. 旧版 283 个活动 SendPack 的 64 台账解决“有无映射”，不等于每个映射在当前合并快照可从实际 prefab 点击到 UI 回调。当前正式 assets 的 `game.CSystemNotice`、旧 PHP 举报、CompatibilityApp/Club 入口证明“旧入口仍可达”仍是活事实。
2. 数据库表存在只证明 authority 候选：`aoo_account_session/device`、`social_mail/notification/read_cursor`、Billing ledger、room event/snapshot 等必须同时有调用者和消费者。R89-004/006/007 即是“表和服务存在、消费者缺失”的反例。
3. 59—63 是审计基线；64—87 是整改自报/专项证据；88 尚在总清单处理中。68、77、87、88 仍被主清单标为处理中，不能因阶段报告措辞升级状态。
4. 67 明确建房生产 Saga fail-closed；86 明确 18 玩法缺客户端；85 明确 XCPDK 测试阻断；87 明确 2.22 隔离门禁失败。这四项直接否定“全接口已闭合”。
5. 任务 80 的机器 ledger 是有价值的局部证明，但必须将所有现存 Vue 写按钮和旧 `/admin/**` 请求纳入输入后才可宣称“后台每个写操作”覆盖。

## 6. 验收条件

1. R89-001—018 各自由独占边界 owner 修复，并由本报告之外的协调稽核在同一最新快照复验。
2. 每个能力必须产出入口、路由、auth/RBAC、authority/JDBC、response/push、实际消费者六类机器证据；测试-only 与 mock 不得升级为生产可达。
3. 发布包中旧 PHP URL、未授权 `SendPack`、重复 CompatibilityApp 入口为零；保留兼容项必须在 64 台账有明确 allowlist、拒绝策略和运行遥测。
4. 全量 Server reactor、Client 严格 TypeScript、Admin build、接口矩阵、prefab 事件解析、SQL/table ownership 与浏览器点击在同一 commit 通过。
5. 生产 TLS/WSS、真实支付/微信/对象存储、多设备与真机项目继续按主清单 M01—M04 暂缓，不得伪装为自动完成。
