# 成都跑得快 XQP 等价移植验收

## 移植结论

成都跑得快已接入 Aoo 的 `poker:pao-de-kuai` 权威房间链路。标准牌组固定为 48 张，裁剪牌组固定为 40 张；两者均严格保留 3 张 A 与 1 张 2。牌组、首出、必压、报单、提示、牌型比较、炸弹、计分、断线恢复与历史房间兼容均由服务端负责。

规则来源为 XQP 的 `chessAreaRule_150.xml`、`chessRule_50.xml`、`PDKRoom.java`、`PDKPlayRule.java`、`PDKRule.java`、`PDKAuto.java` 和成都开房界面配置。读取 XQP 仅用于规则核对，未修改 XQP 项目。

## XQP 逐项对照与保留花色

- XQP 标准样例 `500092`：`103..115 | 203..214 | 303..314 | 403..413`，共 48 张，备注为“3-K、2 一张、A 三张”。
- XQP 去 3/4 样例 `500114`：`105..115 | 205..214 | 305..314 | 405..413`，共 40 张，备注同上。
- Aoo/XQP 协议花色一致：`1=方块、2=梅花、3=红桃、4=黑桃`。因此唯一保留的 2 是方块 2（`115`），移除 `215/315/415`；保留 `114/214/314` 三张 A，移除黑桃 A（`414`）。
- 两人去 3/4：每人 16 张、未发 8 张；三人标准牌组：每人 16 张、无未发牌。牌型、必压、报单、首出、提示、断线恢复及结算均继续走同一个 `poker:pao-de-kuai` Provider，没有另建旁路玩法实现。

## 四张 2 问题根因与服务端修复

失败房 `737578` 的数据库权威快照属于退役发布 `8000016`，其 `pdkRuleOptions.deckCards` 是旧通用 48 张牌堆：四张 A、四张 2。快照的手牌、已出牌与未发牌也真实包含多个 2，故根因是服务端旧发布/旧房权威状态，不是客户端贴图，也不是重连重复。

新房创建现在只接受发布规则的 `deckMode` 选择，并由服务端按成都规则重新生成完整 `deckCards`；Hall 或客户端即使携带旧四张 2 的牌堆也会被覆盖。历史已开局房恢复仍按原快照逐字节重放，避免擅自改写中局状态。对应回归用例 `retiredFourTwosPayloadCannotOverrideANewChengduRoom` 明确注入旧 48 张四个 2 的 payload，并验证实际新房仍得到精确 40 张成都牌堆和守恒结果。

## 配置发布

- 人工配置：`开房规则表/跑得快/成都跑得快开房规则表.xlsx`
- 生成配置：`Server/work/generated/room-rules/成都跑得快.generated.json`
- 工作簿 SHA-256：`97ef2253456293149e1f4a6ebb314178d1b5f59314f386f6880e8e1441a364af`
- 发布校验：成都跑得快 7 个字段通过，XQP 跑得快迁移稽查覆盖 135 条、已迁移 132 条、明确阻塞 3 条。

## 自动化验证

- Poker 生产代码定向编译通过；Bootstrap 全量依赖打包通过。
- 从当前源码定向编译并执行 `ChengduPdkRulesTest` 8 项，8/8 通过（含旧四张 2 payload 不可覆盖新房）。
- 既有 `PokerRuleMatrixTest` 16 项通过。
- Poker 模块全量 `testCompile` 仍被本任务前已有的缺失类阻断：`AypdkNineStageAcceptanceTest` 缺 `AypdkGameProvider`，`HbpdkNineStageAcceptanceTest` 缺 `HbpdkSession`；本次未修改这两个玩法范围，也未把旧编译产物冒充当前源码测试结果。
- 全仓默认门禁仍被本任务前已有的 4 处前端注释代码命中；位置为 `CommonPdkGameLogic.ts` 第 164、171 行与 `CommonPdkRoomPosManager.ts` 第 80、193 行，本次未改动这些范围。

## Creator 与真实 E2E

Creator 3.8.8 已通过资源辅助进程重导入并更新 Preview 转译缓存；未改动任何 Scene、Prefab 或 `.meta`。公共跑得快分享窗的逻辑路径已映射到现有原生 Prefab，导航在页面卸载后的迟到 Hall 挂载会安全丢弃。

当前部署运行目录为 `bootstrap-20260907103251-pdkdeck`，Poker JAR SHA-256 为 `7273ad22aad7e8a0ffa636e237c9b7442c9d1558716740783b7db6ee415bb134`。实际 Gateway 已重启并从该目录加载。

最终 10 局证据位于 `Client/docs/前端框架规范/历史记录与证据/证据/chengdu-pdk-ten-round-2026-09-07T02-45-56-376Z/`：

- 桌面：`http://127.0.0.1:7456/`，1280×720，鼠标事件。
- 手机：`http://192.168.1.133:7456/`，852×393，触摸事件与移动 UA。
- 同一 buildId：`aoo-preview-20260906-unified-room-4`；协议版本 `2.0`；玩法版本 `1.0.0`；API `:8080`、Hall `:8093`、Hall/Game WebSocket 均记录在报告中。
- 新房 `549473` 路由到当前发布 `8000018`，选择 12 局并连续完成前 10 局，结算后解散清理；各局结算状态版本为 `18/39/63/75/90/95/116/131/145/161`。
- 每一局都联合核对桌面 seat 0、手机 seat 1 与数据库 `state.hands/undealtCards/pdkRuleOptions.deckCards`：40 张全量唯一且集合完全守恒、3 张 A、1 张 2、两端各 16 张、未发 8 张；10 次手牌分布互不相同。
- 首局整页重载重连保持同一 `roomId` 与单调 `stateVersion`；10 局均有真实 `提示/出牌/继续` Canvas 点击、权威 WebSocket 请求及唯一 requestId。桌面发送/接收 141/384 帧，手机 150/390 帧，两端错误数组均为空。
- 旧房 `737578` 的四张 2 权威牌堆、发布号、状态版本和两端截图保存在同一报告的 `regressionFixture`，原始截图位于前一次失败现场 `chengdu-pdk-ten-round-2026-09-07T02-42-03-709Z/`；该旧房已通过真实界面解散。
