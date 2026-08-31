# CUR-06 全项目构建与回归测试

更新时间：2026-08-25（Asia/Shanghai）

## 结论

**协调复核中：新增“进入游戏”端到端链路阻塞，撤销签署。**

CUR-01 至 CUR-05、CUR-07 均已完成并通过协调稽核。协议分域修复后的全量链已通过，但最新真实现场发现“进入游戏”按钮点击无效，表明按钮事件→建房/入房→权威响应→bundle/scene 加载→场景切换未被当前自动化完整覆盖；当前恢复执行中，等待 owner 修复并新增端到端门禁。

核心结果：Server 受控 clean 全 Reactor 43/43 SUCCESS；Client TypeScript 与 167/167 tests、全部组件/资源门禁通过；Creator 3.8.8 Web Mobile 构建成功；Admin、协议、Flyway/MySQL、架构与品牌门禁全部通过。

## 已关闭的代码/资源阻塞

| 归属 | 已关闭事项 | 最终状态 |
|---|---|---|
| Server owners | Mahjong 常量分支、Bootstrap ServiceLoader/catalog 冲突、架构基线、MIGREAL06 生命周期、Matchmaking 瞬态缺类 | owner 修复并通过协调稽核；最终 clean 全链未复现 |
| Client owners | LoginScene node-creation 误判、A3PK/AYDSS resource、EditBox normalization、登录 TS2722、ProtocolClient WSS legacyEvent | owner 修复并通过协调稽核；最终所有门禁 exit 0 |
| Hall/region catalog owners | `legacy-data/gametype`、`legacy-data/selectCity`、`legacy-data/gameCreate`、`room.CBaseGameIdList` 与正式 Gateway 权威接口缺口 | 运行时 `legacy-data/*` 通配门禁=0；`hall.regions`=35、`hall.catalog`=7；Client 167/167 |
| Client/Gateway/Bootstrap authority owners | 大厅查询被模糊关键词错误归入房间权威域并抛“缺少 roomId” | 显式域矩阵与双向门禁通过；非房间域绕过 SessionResolver；房间域缺 roomId/playVersion 继续 fail-closed |

当前剩余代码/运行链阻塞：1（架构级）：Hall 独立 8093 进程完成 HTTPS 建房/票据后，没有通往 Gateway Room Authority 的跨进程内部创建/恢复端口；Gateway 仍只从进程内 `RuntimeGameRoomRegistry.require(roomId)` 取房间。同时正式 DB 的 `aoo_compiled_index_active`、`aoo_game_service_route` 均为 0，缺少 `njpdk(629)` 正式 release/index/route。禁止用假目录、假 roomId 或放宽校验伪造通过。

## Owner 闭环记录

- Server 三类阻塞已通知 `R04 全玩法目录与运行链路整改`（thread `01a03443-cfe1-7f81-9b77-c4b486eda852`）。
- LoginScene node-creation 验证误判已通知 `aoo开发` 登录链 owner（thread `01a023be-dd02-71e2-9e92-d76f86d9c701`）；owner 已确认只调整验证规则、不写只读 LoginScene。
- A3PK/AYDSS unresolved resource 与两个 prefab normalization 阻塞已通知 `CUR-07 Creator编辑器同步终验`（thread `01a0367f-a163-7202-a058-8d2f38695a82`）。
- 所有早期阻塞均由对应 owner 修复、协调任务自动稽核通过，并纳入本次最新稳定快照。

## 通过项

### Server、协议与数据库

- 正式受控入口 `tools/verify-clean-with-integration-db.sh` exit 0：创建隔离 `aoo_it_*` schema、执行 Flyway 全量迁移并注入 `AOO_DB_IT_*`；JDK 25 原样 `clean verify`，43/43 Reactor SUCCESS，全部测试 0 failure / 0 error / 0 skipped。
- ReplayArchiveIntegrationTest 1/1、Bootstrap 8/8、Matchmaking `JdbcCompetitionServiceTest` 4/4。
- 协议 `check.mjs`、`check-generated.mjs`、`verify-reproducible.mjs` 全部 exit 0；三产物哈希稳定，使用项目固定 Node 24.19.0。
- Maven 模块图、实际项目架构扫描和品牌门禁通过。
- Flyway 单一权威门禁通过。
- legacy config converter、database package audit 通过。
- Docker disposable MySQL 8.4 实际应用全迁移通过，机器证据：`Server/database/.work/audit/mysql-integration-gate.json`。
- 最终日志：`Server/work/cur06-final/20260825-final/server-official-clean-integration-routing-final.log`、`protocol-architecture-flyway-routing-final.log`、`mysql-integration-gate-final.log`。

### Client

- `bash scripts/verify-build.sh` exit 0：TypeScript `--noEmit` 通过，167/167 tests 通过。
- Cocos Creator 3.8.8 `web-mobile` 构建完成，目标 `index.html` 存在；CLI 36 是 Creator 构建完成后的已知清理退出码，按协调稽核口径验收通过。
- 协议检查、生成一致性和可复现性均 exit 0；生成 TypeScript SHA-256：`9ebde316...13cd`。
- client runtime、manager、component registration、node creation、Creator native assets、CUR-07 editor sync、EditBox 与品牌门禁全通过；Editor sync problems=0，EditBox 210 个/104 个资源、0 changed。
- `no-legacy-data-runtime.test.mjs` 通配门禁通过，运行时 `legacy-data/*` 引用为 0；大厅/地区/亲友圈统一使用 `hall.regions`/`hall.catalog`，房间创建使用原生 `NativeRoomCreateConfig`。
- `protocol-domain-routing.test.mjs` 通过；4 个继承命名冲突的大厅查询明确进入 `hall.dispatch`，`account/hall/club/heartbeat` 不进入房间 SessionResolver，房间权威校验未放宽。
- 最终日志：`Server/work/cur06-final/20260825-final/client-verify-build-routing-final.log`、`client-all-gates-routing-final.log`、`client-creator-web-mobile-routing-final.log`。

### Admin

- `pnpm run lint`、`pnpm run typecheck`、`pnpm run test`、`pnpm run build:pro` 全部 exit 0。
- Vitest：5 files / 15 tests passed；production build：2653 modules。
- `pnpm run ledger:verify` 通过；`pnpm audit` 无已知漏洞。
- 最终日志：`Server/work/cur06-final/20260825-final/admin-quality-final2.log`。

## 环境失败

最终计数为 0。Client 固定使用项目 Node 24.19.0；Server 使用 Maven 3.9.16 / Java 25.0.4.1；Docker MySQL 8.4 可用。中间曾因另一任务并发 `clean` 删除 `target` 出现瞬态缺类，稳定独占复跑后消失，不计入最终失败。

## 暂缓人工/生产项

Creator 逐场景视觉点击、真实浏览器/移动设备、真实第三方服务、真实中间件完整牌局与故障注入、生产迁移/恢复/长稳/发布验收继续按既定集中验收边界暂缓，不阻塞 CUR-06 自动化终验。

## 重新签署条件

- 状态：IN PROGRESS（未签署）
- 自动化 blocking：1
- 代码失败：1（归属 Client/Gateway/Bootstrap/游戏路由 owner）
- 环境失败：0
- 人工暂缓：按既定集中验收边界，不阻塞
- 条件：建立持久化 Hall `RoomCreateSaga` → Gateway Room Authority 跨进程内部创建/恢复端口，发布 `njpdk(629)` 正式 release/index/route；owner 与协调稽核通过后，复跑真实点击→房间运行时→bundle/scene、Client、Hall/Gateway/Bootstrap/游戏路由、正式 clean integration、Creator 与端到端门禁；blocking=0 方可重新签署。

## 边界说明

本任务未修改业务源文件、scene、prefab、meta 或 LoginScene。仅修正总构建/审计编排：`scripts/audit-real14-maven-module-graph.rb`、`scripts/audit-migreal04-consumed-events.rb`、`scripts/audit-migreal05-perspective-reconnect.rb`，并维护本报告；构建过程正常刷新 `target`、`Admin/dist`、Creator 临时目录及审计生成物。项目根目录及三个子项目均无 `.git` 元数据，无法用 `git status` 提供共享工作区变更核对证据。
