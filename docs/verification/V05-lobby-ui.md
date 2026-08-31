# V05 大厅全 UI 交互验证

更新时间：2026-08-24

## 结论

V05 的可自动执行部分已完成整改并通过专项门禁；人工、真实设备验收按任务要求暂缓。
隔离应用内浏览器已实际打开最新现存 Web Desktop 构建，但该浏览器上下文不提供
WebGL/WebGL2，Cocos 在引擎初始化阶段失败，因此不能把空白 Canvas 误报为视觉验收通过。

## 先验发现与整改

- `btn_addQuanCard` 原来命中空 `return`，属于可点击入口无结果；现改为调用唯一生产
  `AooStoreController`，进入 `btn_table2`，没有新增旧接口或旁路。
- 大厅入口只有 220ms 事件去重，网络请求超过该窗口时仍可再次发起；新增
  `mainActionBusy` 请求期单飞，成功和失败都释放，覆盖资料、商店、战绩、排行、任务、
  隐私、客服、实名/手机、赠送、活动、位置等异步入口。
- `destroy()` 原未主动清除延迟加载计时器；现清除 `formLoadingTimer`、恢复加载标志并清空
  请求期防重集合，避免退出大厅后遗留定时器或锁状态。

## 自动化资产

- `Client/tests/fixtures/lobby-ui-v05.json`：登记 `NewMain.prefab` 全部按钮/品牌节点；32 个生产
  动作入口逐项覆盖，5 个非动作节点逐项说明。`btn_uploadImg_test` 在正式 Prefab 中为 inactive，
  未作为生产入口激活。
- `Client/tests/ui/lobby-ui-v05.test.mjs`：覆盖按钮路由、弹窗入口、加载与首次加载终态、
  失败提示、空返回、防重复点击、返回键、销毁清理及禁止假网关/内存演示/旧抽奖旁路。
- `Client/tests/ui/README.md`：记录稳定执行入口。
- `RoomNetworkControllerMount`：由大厅生产入口创建并销毁，绑定 NJPDK/通用房间装配事件、
  准备/刷新/退出/重连/录音入口，以及网络、权威状态、动作和语音状态回调；三个公共网络
  Controller 不再是只定义未消费的死代码。

## 机器验证

| 验证 | 结果 |
|---|---|
| `node --test tests/ui/lobby-ui-v05.test.mjs` | 6/6 通过 |
| `node --test tests/unit/login-lobby-flow.test.mjs tests/unit/legacy-lobby-production-bridge.test.mjs` | 11/11 通过 |
| TypeScript 6.0.3：`tsc -p tsconfig.json --noEmit --pretty false --ignoreDeprecations 6.0` | 通过，0 诊断 |
| 全量 `tests/unit/*.test.mjs` 稽核 | 41/48 通过；7 项为边界外既有测试夹具/过期路径失败，详见下节 |
| 隔离应用内浏览器访问 `http://127.0.0.1:4175/` | 页面和正式 Cocos 包加载；WebGL/WebGL2 不可用，引擎在 `getExtension` 前失败 |

## 边界外既有失败

全量单测中的 7 项不由本次改动引入，且不修改其所属边界：

- `deep-link-router`、`qr-invite-policy`、`room-exit-cleanup`、`seat-perspective`：旧自制源码
  剥离器无法解析当前 TypeScript 的 `export interface`、`export type` 或类型标注。
- `media-profile-flow`：仍读取已不存在的 `LegacyLobbyProductionBridge.ts`，当前生产文件为
  `AooLobbyBridge.ts`。
- `privacy-ui-flow`：断言旧类名 `LegacyPrivacyController`，生产装配已是
  `AooPrivacyController`。
- `production-edge-routing`：仍断言已关闭的 `ClientPack` 路径，与当前唯一网关策略冲突。

## 暂缓项

- Cocos Creator 编辑器人工逐弹窗和逐资源验收。
- Chrome、Safari、移动端和真实设备点击验收。
- 具备 WebGL 的浏览器视觉截图与像素/布局复验；当前隔离浏览器能力限制已保留为明确阻塞，
  未用 DOM 假大厅、内存演示或其他浏览器替代。
