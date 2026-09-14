# 87｜2.22 隔离审计与强制门禁

## 结论

截至 2026-08-24，Aoo 尚未达到 2.22 完全隔离。只读机器审计覆盖 Client、Server、Admin、配置、构建、数据库迁移、Prefab、Scene、Meta、资源索引和软链接；门禁当前必须失败，不能以“兼容”或“名称叫 Legacy”为理由宣布闭合。完整逐行证据为 `tools/legacy-isolation/audit.json`，稳定规则与失败语义为 `tools/legacy-isolation/gate.py`。

当前完整扫描为 `filesScanned=26083`、旧 Prefab/Scene UUID `3665`、阻断 `37`。阻断集中在生产客户端、Admin 和 Creator scene 构建配置：客户端仍存在旧 PHP 外跳、旧 external subgame 事件链、Legacy HTTP 登录网关和运行时 bundle 加载；Admin 仍通过可配置 endpoint 装配 legacy CRUD 页面；`Client/profiles/v2/packages/scene.json` 有 12 行旧 UUID 未能在 Aoo 本地 `.meta` 中解析。以上均不在 allowlist 资格范围内。另有 70,252 个旧 UUID 引用可解析到 Aoo 本地 meta，明确归类为 `localized-generated-resource`，不作为外部依赖失败。Server 中旧入口 blocklist、流量归零计数器、LegacyCommon/LegacyGameHall/LegacyAccountServer 被单独标为 `isolated-compatibility`，不会仅因类名或目录名含 Legacy 被判为生产引用；但任何其他生产构建对这些隔离目录的 classpath/module 依赖仍会阻断。

## 分类和失败语义

| 分类 | 语义 | 门禁 |
|---|---|---|
| `production-reachable` | Client/Admin/Server 生产源码或运行时资源可触达 | 必须失败 |
| `configuration-or-build` | 配置、发布或构建装配可触达 | 必须失败 |
| `isolated-compatibility` | 明确隔离的协议 guard、流量证明或兼容模块 | 留证，不因名称失败 |
| `localized-generated-resource` | 已复制并由 Aoo 本地构建生成的资源 | 留证；仍做 UUID/依赖检查 |
| `readonly-evidence` | 文档、测试、迁移、reference、审计脚本 | 留证；仅此类可精确 allowlist |

allowlist 使用 finding 的 20 位稳定 ID 和非空理由；扫描器会拒绝将生产、配置或构建 finding 变成允许项。当前 allowlist 为空，避免批量掩盖历史材料。文档/迁移证据未逐条 allowlist 不会阻断，但全部保留在 JSON 供复核。

## 检测面与证据方式

- 路径与身份：绝对/相对 `Test`、`QH/qh` 包或工程身份、带迁移语义的 `2.2.2/2.22`；普通 `LegacyFoo` 标识符不单独匹配。
- 入口与运行时：旧 HTTP/WS/PHP、legacy external subgame/bundle/endpoint、运行时 fallback、非字面量 JS `require/import`。Java 普通 `require()` 不被误当动态模块加载。
- 复制与构建：复制命令桥接 Test/QH/2.22、Maven/Gradle classpath/module 旧依赖、构建产物中的依赖引用。
- 数据库：SQL AST 近似的 `FROM/JOIN/UPDATE/INSERT/DELETE` 旧表直连；文档、dump 和迁移证据单独分类。
- Cocos：从六个只读旧客户端 assets 根提取 Prefab/Scene `.meta` UUID，与 Aoo `.prefab/.scene/.asset/资源 JSON` 交叉；不是单纯搜索 `Legacy` 文本。
- 文件系统：不跟随软链接，逐个记录 link target；依赖目录、编辑器 cache、临时 work、backup/archived 和构建 target 不混入生产源码结论。

## 阻断责任与并行整改边界

| 责任任务 | 当前证据 | 可独占整改边界 |
|---|---|---|
| Client-旧 Web 入口 | `LegacyClubRecordListController.ts:100`、`LegacyLobbyScreen.ts:880` 的线上 `.php` URL | 仅对应 Club/Lobby controller 与新 API adapter；不要跨改 Server |
| Client-外部子游戏 | `LegacyClubMainController`、`LegacyLobbyScreen/SessionService`、`LegacyNativeSubgameCoordinator`、HZMJ/NJPDK coordinator 的 `legacy-*-subgame/replay` 事件链 | 仅 Client runtime subgame/对应玩法 pack；先证明 bundle 已本地化并改为新入口 |
| Client-认证兼容 | `LoginScreenBootstrap.ts:41` 生产装配 `LegacyHttpAuthGateway` | 仅 Login auth gateway/bootstrap；不得以重命名替代移除生产可达性 |
| Client-bundle 依赖 | `Common/.../LegacySubgameBundleService.ts` 仍运行时加载 bundle | 仅 Common platform/subgame loader 与资源 bundle 配置；需补 bundle dependency 闭包证据 |
| Client-scene 索引 | `Client/profiles/v2/packages/scene.json` 的 12 行旧 UUID 未在当前 Aoo meta 集解析 | 仅 Creator scene profile/对应本地 scene、prefab；先判定陈旧索引或缺失本地资源 |
| Admin-legacy CRUD | `Admin/src/views/legacy/index.vue:38,41,42` 以配置 endpoint 调用 legacy API | 仅 Admin legacy view/api/router；确认废弃或映射到当前权威 API |
| Server-隔离复核 | DeprecatedEntryPointBlocklist、LegacyProtocolUsage、LegacyCommon 等当前仅隔离留存 | 仅对应 guard/隔离模块及其独立测试；禁止把模块重新接回聚合 reactor |

这些边界可并行，但每个任务只能修改自己列出的生产区域；本任务未修改任何生产源码、Prefab、资源、构建文件或数据库迁移。

## CI 与复现

完整发布审计（旧树必须以只读方式挂载）：

```sh
python3 Server/tools/legacy-isolation/gate.py \
  --legacy-root /Users/aoo/Code/Game/BCG/Test \
  --output Server/tools/legacy-isolation/audit.json
```

无旧树的普通 CI 可执行文本/装配门禁，但必须明确关闭 UUID 交叉项，报告会记录 UUID 数为 0，不能用来签署完整隔离：

```sh
python3 Server/tools/legacy-isolation/gate.py --no-legacy-uuid
python3 Server/tools/legacy-isolation/test_gate.py
```

退出码 `2` 表示至少一个生产/配置/构建 finding；退出码 `0` 才表示本门禁通过。当前应为 `2`。负向单测验证了：名称叫 Legacy 不自动失败、动态 import 与字面量 import 可区分、旧表 SQL 能检出、生产 finding 不能被 allowlist 降级。

## 验收条件

只有在完整模式 `legacyRootPresent=true`、旧 Prefab/Scene UUID 集已实际载入、`blocking=0`、单测通过，且 JSON 中不存在未解释的生产/配置/构建 finding 时才可闭合。历史材料数量、兼容代码数量或“已本地复制”本身都不是通过依据；本地化资源仍须证明没有旧 UUID/外部 bundle/classpath/module 可达边。
