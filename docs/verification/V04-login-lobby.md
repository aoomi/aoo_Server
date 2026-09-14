# V04 Login → Lobby 验收

日期：2026-08-24  
代码快照：Aoo 当前工作区（未读取、运行或引用旧 Test 工程）

| 场景 | 自动证据 | 结果 |
|---|---|---|
| 账号登录 | Aoo `/api/v1/account/login`、session 主体校验、ws_ticket | 通过 |
| 游客登录 | guest/register + guest/login，credential 持久化 | 通过 |
| 自动登录 | single-flight restore、refresh rotation、迟到结果隔离 | 通过 |
| accessToken 刷新 | wsTicket 401 后 rotate refresh 并覆盖持久化 | 通过 |
| refreshToken 失效 | 401 映射 INVALID_CREDENTIALS，缓存清理、回登录页 | 通过 |
| 单端踢线 | JDBC auth_generation / revoked session 测试 | 通过 |
| 断线重连 | 新 wsTicket + reconnect authenticator；状态事件单调恢复测试 | 通过 |
| 退出登录 | 本地清理 + `/api/v1/account/logout` token-family revoke | 通过 |
| 再次登录 | logout 后新账号登录会话测试 | 通过 |
| Lobby 场景/交互装配 | LoginScene/MainScene UUID 静态契约、mount 生命周期测试 | 通过 |
| 隔离应用内浏览器点击 | Creator 预览可达，但隔离浏览器无 WebGL，canvas 未初始化 | 阻塞 |

## 命令与结果

1. `node --test tests/unit/login-lobby-flow.test.mjs tests/unit/production-edge-routing.test.mjs tests/unit/room-reconnect-state.test.mjs`：12/12 通过。
2. `tsc --noEmit --strict --ignoreDeprecations 6.0 --project tsconfig.json`：通过。
3. JDK 25 + Maven 3.9.16，Account/Gateway/Hall reactor（`-Dexec.skip=true`）：BUILD SUCCESS；相关模块 61/61 通过。
4. 隔离应用内浏览器访问 Creator 预览：引擎在 WebGL swapchain 初始化前失败，日志为 `This device does not support WebGL`，不能执行登录 canvas 点击。

## 结论

代码与非浏览器自动回归闭环通过；V04 尚不能标记为“全部通过”，唯一剩余项是隔离应用内浏览器缺少 WebGL 能力。人工/真实设备验收暂缓，不作为本轮自动验证替代品。
