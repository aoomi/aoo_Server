# R12 Admin 安全、权限与生产边界复验

日期：2026-08-24  
范围：`Admin`、`server/AdminApi`、Admin 专属迁移与本文件。未修改 Client、玩家 Gateway、公共协议 DTO、登录大厅链路、Provider 或 owner 文件。

## 结论

R12 自动审计发现的阻塞缺口已完成整改：浏览器端此前调用不存在的 `/api/auth/*`，且无法生成控制面 HMAC，因此 Admin 真实操作接口实际上不可达；非生产环境还能退回内存仓储、空审计和空对账；浏览器配置含硬编码 HTTP API 与公网头像地址。现已接入数据库管理员账号与持久化会话，使用 HttpOnly/Secure/SameSite Cookie、CSRF 双令牌、精确 HTTPS Origin、服务端 RBAC 与范围校验；AdminApi 在所有环境都强制数据库及密钥配置，已移除运行时内存/空实现路径。

## 审计与整改覆盖

| 检查面 | 结果与证据 |
|---|---|
| 真实接口 | `/api/auth/login`、`logout`、`me` 已在独立 AdminApi 注册；21 个运营接口由权限目录、HTTP handler 和 JDBC repository 闭环，`Admin/docs/admin-api-ledger.json` 为 21/21。 |
| RBAC/最小权限 | `AdminPermissionCatalog` 是按钮、HTTP 权限、目标类型和风险等级的单一目录；`JdbcAdminAuthorizationService` 合并直授与角色授权；未知路由 fail closed。 |
| 越权/水平越权 | 授权同时校验 permission、targetType、targetId；玩法、房间、俱乐部、区域和导出目标从规范路由提取；高风险操作要求不同操作员的短时、一次性二次审批。 |
| 审计 | 允许和拒绝决定均写 `admin_authorization_audit`；资源变更、发布、代理与敏感导出分别保留持久化日志/流水。响应不回传异常详情。 |
| 防重放/CSRF/CORS | 机器调用使用时间窗、请求路径绑定和数据库 nonce；浏览器会话只存散列，写请求校验 CSRF；仅接受 `AOO_ADMIN_ALLOWED_ORIGIN` 指定的单一 HTTPS Origin。 |
| 敏感信息 | 浏览器不再保存 bearer token；会话 Cookie 为 HttpOnly/Secure/SameSite=Strict；密码采用 PBKDF2-SHA256 verifier；列表列名裁剪 token/secret/password/signature；通用错误不泄露服务端细节。 |
| 密钥/生产配置 | Admin token、DB 凭证和地图 key 通过严格配置/secret resolver 获取；所有环境强制真实数据库；地图 key 仅存在后端代理。前端 API 和头像基址改为同源相对路径。 |
| 第三方代理 | IP 定位仅经 `AdminMapProxy`，具备超时、限流、缓存、审计和上游错误归一化，Admin 前端不持有 provider key。 |
| 分页/导出 | 列表协议使用 cursor/limit 且 limit 有界；敏感导出含申请、异人审批、生成、到期下载、字段掩码、行数上限、内容哈希及审计。 |
| 上传下载 | 生产入口未开放通用上传；敏感下载固定服务端文件名且返回内容哈希，不接受客户端文件路径。旧模板上传页不在生产路由。 |
| 幂等/并发 | 写命令要求 requestId 与签名请求 ID 一致；数据库 dedup；资源更新要求 If-Match/ETag，冲突为 412；发布和导出工作流持久化状态。 |
| 生产隔离 | AdminApi 独立 artifact、仅 loopback 监听、与玩家入口不共享 classpath/listener；部署策略拒绝 player gateway、game server 与 public internet 身份。 |

## 变更清单

- 新增 `JdbcAdminSessionService` 与 `V20260824_76__admin_browser_auth.sql`，只持久化 session/CSRF 哈希。
- 新增真实管理员 login/logout/me handler；前端改用 HttpOnly Cookie 与短期 CSRF，不再把 bearer token 写入 JavaScript 可读 Cookie。
- AdminApi 删除开发期内存资源、内存玩法配置、空授权审计、空对账、不可用调查仓储和内存导出的运行时装配。
- 前端 `config.json` 删除硬编码 `http://127.0.0.1:18088` 和公网头像服务，统一同源访问。
- Axios 错误提示改为稳定公共消息，不显示上游/网络异常原文。

## 自动化证据

| 命令 | 结果 |
|---|---|
| `pnpm run typecheck` | PASS |
| `pnpm test`（Node 24.19 bundled runtime） | PASS，5 files / 15 tests |
| `pnpm run build:pro` | PASS，Vite 生产构建完成 |
| `pnpm run ledger:verify` | PASS，21/21 接口闭环 |
| `./mvnw -pl server/AdminApi -am compiler:testCompile surefire:test` | PASS；AdminApi 17 tests，0 failure / 0 error（1 个真实 DB 集成测试因未提供 `AOO_DB_IT_URL` 跳过） |
| `ruby scripts/audit-real10-admin-isolation.rb` | PASS |
| `ruby scripts/audit-dbreal07-migration-order.rb` | PASS，86 个迁移顺序唯一且确定 |

完整父 reactor 的首次复验在新增迁移字段名命中敏感词静态规则时失败，随后已将字段改为语义明确的 `credential_verifier`；Admin 专属编译、测试、构建和边界门禁均已重新通过。

## 跨边界与环境残余项

- 未配置真实 `AOO_DB_IT_URL`，因此没有冒充完成真实数据库集成执行；部署前必须在隔离测试库执行迁移并复跑 `AdminRbacIntegrationTest`。
- TLS 终止、正式管理域名、WAF/入口身份和生产数据库属于 M04 环境验收；代码强制 `AOO_ADMIN_ALLOWED_ORIGIN` 为单一 HTTPS Origin，并仅监听 loopback。
- 仓库内玩家侧/旧 2.22 管理入口的物理删除涉及 V01/V03 owner 文件，本次仅由 Admin 独立入口策略隔离，未越界修改。
