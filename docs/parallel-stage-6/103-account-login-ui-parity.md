# 103 Creator 3.8.8 账号登录 UI 对齐

## 结论

Creator 3.8.8 `LoginScene` 已同时序列化游客登录和明确文字标识的账号登录入口。账号入口打开场景内静态 `mobile` 弹窗；弹窗默认隐藏，账号/密码、确认、返回、加载和错误节点均可在 Creator 编辑器直接查看和编辑，没有运行时创建或移动布局。

## 客户端实现

- `LoginScene.scene` 保留 Aoo 本地化视觉语义并补齐 `AccountLoginEntryText`；账号入口使用原生序列化的 257×101 `UITransform/Button/Label`，不新增或恢复 legacy SpriteFrame UUID，位于游客入口下方可视区域；账号弹窗默认隐藏，`AccountLoadingIndicator`、`AccountErrorLabel` 默认隐藏。
- 密码 `EditBox` 使用 Creator PASSWORD 输入标志；账号和密码分别限制 3–20、6–64 字符，空值和长度错误就地提示并回焦。
- 账号回车切换密码焦点，密码回车提交；确认、返回、游客和账号入口均有重复触发保护。
- 提交期间锁定所有登录/返回按钮并显示加载节点；失败后恢复可操作状态。关闭和组件销毁时清理焦点、密码、错误文本、事件监听和服务引用。
- 账号/密码错误、封禁、超时、断网、维护/服务不可用、响应异常和版本不兼容均映射为可重试中文提示。
- `AccountAuthGateway` 只走唯一 HTTPS origin 下的 `/api/v2/account/*`、统一 envelope、rotating access/refresh token 和 `/api/v2/gateway/ws_ticket`；WSS 继续由统一入口策略强制。无 PHP、旧 WS 或 legacy HTTP fallback。
- 会话继续通过 `AuthSession` 的公开 `login`、`loginAsGuest`、`acceptExternalSession` 接口接入，未覆盖并行 101 的生命周期/启动恢复实现。

## 服务端契约

未修改 Server Account。现有 Account/Gateway 所需字段已覆盖登录名、密码、设备、渠道、客户端版本、accessToken、refreshToken、accountId、封禁/鉴权错误和一次性 wsTicket；本任务只在客户端 adapter 兼容统一 envelope 并细化错误映射。

## 自动验证

- `node --test tests/unit/login-lobby-flow.test.mjs`：8/8 通过，覆盖账号/游客会话、重复提交、持久化、退出后隔离、场景默认显隐、入口和密码掩码。
- `tsc -p tests/tsconfig.account-login.json`：严格 TypeScript 通过。
- `scripts/verify-component-registration.mjs`：本任务场景 JSON、meta/UUID 引用均可解析；全仓门禁被范围外既有问题阻塞：`XpphzSwitchCoordinator.ts` 缺少 `.meta`。
- 隔离应用内浏览器打开 `http://127.0.0.1:7456/` 成功，但运行环境报告 `This device does not support WebGL`，Cocos canvas 未初始化，因此无法执行账号/游客画布自动点击。此项为环境阻塞，不伪造通过结论。
- 2.22 `Test` 目录仅用于只读语义对照；Aoo 运行文件不含 Test 路径或 2.22 UUID 引用。

人工 Creator 编辑器与真机验收按任务要求暂缓。
