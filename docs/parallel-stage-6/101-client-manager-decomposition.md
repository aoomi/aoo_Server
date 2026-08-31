# 101 Client Manager Decomposition

## 结论

`AooClientManager` 已彻底移除；客户端不再创建或注册任何 Cocos 持久根节点。原 God Object 被拆为不依赖 Cocos Node 生命周期的 `AuthSession`、`NetworkRuntime`、`SceneRouter`，唯一 `ClientBootstrap` 只做依赖装配、启动顺序和启动失败回收，不保存业务状态。Login、Lobby、NJPDK、HZMJ 和原 CompatibilityApp auth 调用已迁至明确接口；旧 manager、AuthRuntime/AuthService locator、CompatibilityApp auth 别名及对应 meta/UUID 已删除。

没有保留轻量持久 Component 或 Bootstrap Node。场景里原有的 `LoginScreenBootstrap` 仍是普通场景组件，只负责设计分辨率、启动目录 UI、登录 UI 绑定和调用装配入口；它不会跨场景存活。

## 拆分前机器依赖图与职责判定

拆分前权威文件为 `assets/Login/Code/Runtime/bootstrap/AooClientManager.ts`，其机器盘点如下：

| 类别 | 成员/依赖 | 判定与去向 |
|---|---|---|
| 字段 | `persistentNode`, `stopped` | Cocos 持久生命周期；完全删除 |
| 字段 | `hallClient`, `heartbeat` | socket/ticket/reconnect/heartbeat；迁入 `NetworkRuntime` |
| 字段 | `lobby`, `enterEpoch`, `enterLobbyPending` | 场景挂载、迟到结果取消、重复进入门禁；迁入 `SceneRouter` |
| 方法 | `bootstrap`, `require`, `lookup`, `claimPersistentNode`, `removeDuplicateManagerNodes`, `retireNode`, `managerNodes`, `isAlive` | manager authority/转发壳；全部删除 |
| 方法 | `enterLobby`, `performEnterLobby`, `reset`, `disposeLobby` | 混合网络登录和场景编排；分别迁入 Network/Router |
| 方法 | `shutdown` | 拆为三个服务的 `stop/dispose` 和 Bootstrap 失败回收 |
| 方法 | `diagnostics`, `getLobbyFormManager` | 生产调用为 0 的死代码；删除 |
| 事件 | `Game.EVENT_HIDE`, `Game.EVENT_SHOW`, `SPlayerHeartBeat`, socket activity | 由 `HallHeartbeatService` 继续实现，唯一所有者改为 `NetworkRuntime` |
| timer | heartbeat 1 秒 interval、请求 timeout、lobby/UI timers | heartbeat/socket timer 由 NetworkRuntime 的唯一 transport 间接持有；UI timer 仍归各 UI 生命周期 |
| socket | `ProtocolClient`、WSS ticket、role login、reconnect authenticator | 全部由 `NetworkRuntime` 单实例持有 |
| scene/prefab | 运行时 `new Node('AooClientManager')`; scene/prefab 序列化引用 0 | 运行时 Node 和 UUID/meta 均删除；1626 个 scene/prefab 扫描为 0 |
| 动态依赖 | `AooLobbyScreen`, `LegacyRoleGateway`, `bootstrapRuntime`, endpoint resolver, platform/audio runtime | Lobby/Role 按职责注入；平台与音频仍使用现有公共服务，没有塞入三个新服务 |

机器可重复盘点入口：`pnpm audit:client-runtime`。脚本输出每个入口文件的 imports、fields、methods、events、timers 和 scenes，便于后续 diff 和门禁审计。

## 拆分后依赖方向

```text
LoginScreenBootstrap
  -> startClientServices()              (唯一装配调用)
       -> AuthSession                   (gateway + durable stores)
       -> NetworkRuntime                (ProtocolClient + role gateway + heartbeat)
       -> SceneRouter(AuthSession, NetworkRuntime)
  -> services.scenes.recoverStartup()
  -> LoginView.configure(AuthSession)

SceneRouter
  -> AuthSession                        (恢复/退出会话)
  -> NetworkRuntime                     (大厅登录/重置 transport)
  -> AooLobbyScreen                     (场景局部 UI runtime)

AooLobbyScreen
  -> injected ProtocolClient
  -> injected logout callback           (无 locator、无 manager alias)
```

唯一热重载登记键为 `Symbol.for('aoo.client.runtime.services')`，登记值是冻结的强类型 `ClientServices`。该入口只在 `LoginScreenBootstrap` 调用一次；业务代码既不能查找登记表，也不能构造服务，因此不是 service locator 或双入口。三个构造器的生产 `new` 各恰好 1 处。

## 生命周期与防重复

- `AuthSession.start/stop/dispose` 幂等；自动恢复 single-flight；stop/logout 增加 epoch，迟到 token 结果不能复活会话；重试 timer 可取消并释放等待。
- `NetworkRuntime.start/stop/reset/dispose` 幂等；每次大厅登录前依次 `heartbeat.stop -> client.close -> createTransport`，旧 socket、listener、pending request、fragment、reconnect authenticator 和 timer 被清空。
- `SceneRouter.start/stop/dispose` 幂等；大厅进入 single-flight 且有 epoch；退出先销毁 Lobby，再重置 NetworkRuntime，再导航登录场景。
- 登录→大厅→游戏→大厅→退出→重登复用同一冻结服务组；只替换 session transport，不新增第二服务实例。热重载再次执行装配入口会返回同一组实例。
- 音频继续使用现有 `LegacyAudioService`。SceneRouter 在切场景前调用新增的轻量 `detach()`，下一场景 Bootstrap 再 `attach()`，因此移除持久根节点后不会把音频 Node 随旧场景销毁。

## 删除与迁移清单

- 删除 `AooClientManager.ts/.meta`，UUID `fa435291-894d-4e9f-a76a-6005c0ca3701`。
- 删除 `AuthService`、`AuthRuntime` 及其 meta；新增 `AuthSession`。
- 删除整个 `Common/Code/Runtime/CompatibilityApp/auth` re-export 目录及目录 meta；其 6 个脚本 UUID 在 scene/prefab 中原本均为 0。
- NJPDK、HZMJ、LegacyNativeSubgameCoordinator 的账号类型改为 canonical `Login/.../AuthTypes`。
- LoginView 改为 `configure(AuthSession)` 显式注入；Lobby 只接收 logout callback。
- 所有 `director.loadScene` 生产调用收敛到 `SceneRouter` 1 处。
- Creator 自动导入已为 `AuthSession`、`NetworkRuntime`、`SceneRouter`、`ClientBootstrap` 及两个新目录生成 meta。

## 架构门禁

`tests/unit/client-manager-lifecycle.test.mjs` 新门禁覆盖：

1. assets 中 `AooClientManager` 名称、旧 registry key、旧 UUID、`addPersistRootNode` 全部为 0。
2. 三个服务只能在唯一 composition root 构造，Bootstrap 无 Node/cc/any 依赖。
3. 三服务必须显式提供 `start/stop/dispose`，禁止 `Component` 继承和新万能 Manager。
4. 禁止 CompatibilityApp auth、AuthRuntime/AuthService locator、manager fallback 或全局 any 回流。
5. reset 必须先停止 heartbeat、关闭 client、再创建唯一 transport；heartbeat start 必须先 stop。

## 验证结果

| 验证 | 结果 |
|---|---|
| 101 架构/生命周期门禁 | 5/5 通过 |
| Login/Lobby 会话真实单测 | 8/8 通过（含 restore single-flight、迟到结果、重复登录、logout/relogin） |
| scene/prefab 旧名称、旧 UUID 扫描 | 1626 文件，0 命中 |
| 全 assets meta 数 | 13630；101 新文件 meta 均已生成 |
| Creator 3.8.8 自动导入 | asset-db ready、101 metas 成功生成；CLI headless 的内建 `menu` 插件报错，未做人工编辑器验收 |
| 全量客户端测试 | 69 项中 68 通过；唯一失败来自并行边界外新文件 `XpphzSwitchCoordinator.ts` 缺 meta |
| 严格 TypeScript | 全量 `tsc -p tsconfig.json --noEmit` 通过 |

全量测试唯一失败文件属于并行中的玩法/scene profile 工作，本任务遵守独占边界未修改：

```text
assets/Games/WordCard/Packs/Pack01/Code/Runtime/xpphz/XpphzSwitchCoordinator.ts(.meta missing)
```

待对应并行任务完成后只需重跑：

```bash
pnpm verify:client-manager
node --test $(find tests -type f -name '*.test.mjs' -print | LC_ALL=C sort)
```

人工 Creator 编辑器和真机验收按要求暂缓。Server/Admin、99 组件注册和 100 scene profile 文件均未修改。
