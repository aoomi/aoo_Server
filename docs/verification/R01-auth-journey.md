# R01 账号认证全旅程复验

日期：2026-08-24  
范围：账号/游客登录、会话缓存、access/refresh token、退出、封禁、单端登录、断线重连、wsTicket/WSS、大厅跳转。

## 结论

R01 专属自动化与静态复验通过，阻塞项为 0。认证链以 JDBC 中的账号、设备、会话和一次性网关票据为权威；客户端只持有凭证和展示资料，不决定账号状态、封禁、会话代际或票据有效性。

## 本轮整改

1. Account 唯一公开入口从残留的 `/api/v1/account` 收敛为边缘路由及客户端已使用的 `/api/v2/account`，没有保留兼容入口。
2. 修复浏览器 WSS 握手不可自定义 `X-Device-Id` 导致的真实链路不可达：升级请求只提交一次性 ticket 和浏览器 Origin，设备身份从签发时已经绑定的数据库票据恢复。
3. WSS 消费票据时重新联查 session、account、device，强制检查会话撤销、设备撤销、auth_generation、access 过期和实时封禁；签发后、消费前发生的封禁或踢线不能穿透。
4. 游客缓存恢复遇到网络超时/断网时保留原 credential 并向上报错，不再错误注册新游客身份；只有凭证明确失效才清缓存并重新注册。

## 验收矩阵

| 场景 | 权威证据 | 结果 |
|---|---|---|
| 账号登录 | `/api/v2/account/login`，密码哈希校验，JDBC 会话签发 | 通过 |
| 游客登录 | guest credential 哈希持久化，register/login 分离，断网不复制身份 | 通过 |
| 登录态缓存 | restore single-flight、epoch 隔离，退出后迟到结果不能复活 | 通过 |
| access/refresh token | access 15 分钟、refresh 30 天、refresh 原子旋转、旧 refresh 拒绝 | 通过 |
| 退出 | 本地先清理，服务端按 token family 撤销，旧 access 不可授权 | 通过 |
| 封禁 | account ban 提升 auth_generation、撤销会话，登录与 WSS 消费均拒绝 | 通过 |
| 单端登录 | single 模式提升 auth_generation 并撤销账号全部旧会话 | 通过 |
| wsTicket/WSS | JDBC 一次性票据、30 秒 TTL、Origin/设备绑定、防重放、消费时重验权威状态 | 通过 |
| 断线重连 | 每次重连刷新 wsTicket，使用服务端 sign 恢复大厅角色会话，重连次数有界 | 通过 |
| 大厅跳转 | LoginScene → NetworkRuntime → LegacyRoleGateway → MainScene；退出精确返回 LoginScene | 通过 |
| 旧接口/mock/内存占位 | 登录客户端无 v1/compat/ClientPack；Account/Gateway 权威实现均为 JDBC | 通过 |

## 实测命令

1. `node --test tests/unit/login-lobby-flow.test.mjs tests/unit/production-edge-routing.test.mjs tests/unit/room-reconnect-state.test.mjs`：14/14 通过。
2. `../Admin/node_modules/.bin/tsc --noEmit --strict --ignoreDeprecations 6.0 --project tsconfig.json`：通过。
3. `./mvnw -pl server/Gateway -am -Dexec.skip=true test` 所覆盖 Gateway 测试：33/33 通过，其中 `JdbcWsTicketServiceTest` 5/5 通过。
4. `./mvnw -pl server/Account -am -Dexec.skip=true -Dtest=JdbcAccountSessionServiceTest,AccountHttpRoutesContractTest -Dsurefire.failIfNoSpecifiedTests=false test`：4/4 通过，BUILD SUCCESS。

## 构建协同说明

首次带全局 validate 门禁的局部 reactor 命令在不属于 R01 的 `MIGREAL04 consumed-event-lifecycle` 门禁失败；随后按专属模块隔离运行并通过。一次并发 reactor 还出现 Account target 被另一构建覆盖造成的 Surefire 类缺失，串行复跑 R01 Account 测试后 4/4 通过。最终全项目构建由 V01 统一执行，本报告不修改 V01 报告。

## 暂缓项

正式域名、证书、WAF/CDN、生产数据库以及真实设备点击仍属于总清单 M02/M04；不以本轮自动化冒充真实生产环境验收。
