# V12 性能与稳定性验证

验证时间：2026-08-24（Asia/Shanghai）

## 结论

自动化范围内通过。V12 静态门禁扫描 4,347 个 Server、Client、Admin 生产源文件，死循环、无界 JDK 队列、缓存线程池、未关闭调度器、未清理定时器、未解绑全局监听和事件循环阻塞调用均为 0。人工浏览器、Cocos 编辑器和真实设备验收按任务要求暂缓。

## 先验证后整改

整改前执行现有门禁，确认以下真实失败：

- `LOOP01`：`RoomAccountingSaga` 与 `WeChatOAuthClient` 存在源码级无界循环。
- `LOOP10`：Outbox 调度器启动幂等门禁失败。
- `LOOP14`：表单关闭未停止节点树动画。
- `LOCK08`：Gateway Netty 事件循环直接执行 JDBC 鉴权/票据操作。
- Admin 静态检查发现 chart/home/iconSelector/noticeBar 的全局监听或动画监听缺少卸载清理。
- Bootstrap 的 media、competition 周期任务没有显式关闭入口。

## 已完成整改

- 房间记账 Saga 的 CAS 竞争改为最多 16 次，超限明确失败，禁止永久自旋。
- WeChat JSON 解析循环改为显式完成条件；OAuth 请求仍保留最大 3 次、超时、退避和熔断边界。
- Gateway 权威 JDBC 操作从 Netty EventLoop 移至 8 线程、512 队列的有界工作池；饱和时返回 `503/BACKPRESSURE`，并在进程关闭时终止工作池。
- Gateway 启动数据库探针拆分到独立启动边界，避免阻塞代码进入网络事件处理器。
- Outbox relay 启动保持原子幂等；media 与 competition 调度器注册关闭钩子。
- Client 表单关闭时递归停止节点树 Tween；Admin 的窗口 resize 与 animationend 监听在卸载时解绑，Chart 实例同时释放。
- 新增 `tools/v12_performance_stability_gate.rb` 并接入 Maven `validate` 的 `enforce-v12-performance-stability`，覆盖死循环、无界队列、缓存线程池、调度器/定时器/监听泄漏、EventLoop 阻塞、Outbox/Media 清理批次和日志滚动策略。

## 自动证据

| 验证项 | 结果 | 证据 |
|---|---:|---|
| V12 静态门禁 | 通过 | `work/audit/v12-performance-stability-static.json`：4,347 文件，0 失败 |
| 无界循环门禁 | 通过 | `java tools/UnboundedLoopCheck.java . ../Client` |
| 调度器所有权 | 通过 | `work/audit/loop10-scheduler-ownership.json` |
| UI 动画生命周期 | 通过 | `work/audit/loop14-ui-animation.json` |
| EventLoop 阻塞 IO | 通过 | `work/audit/lock08-eventloop-blocking.json` |
| GameCommon 回归 | 通过 | `ScheduledOutboxRelayTest`、`RoomAccountingSagaTest` |
| 稳定性/基准型测试 | 通过 | `SchedulerDriftMonitorTest`（100,000 样本）、`ManagedAsyncExecutorTest`、`ControlledRetryTest`、`ResourceScopeTest` |
| Account 回归 | 通过 | `WeChatOAuthClientTest` |
| Gateway 全模块测试 | 通过 | `./mvnw -Dexec.skip=true -pl server/Gateway -am test` |
| 相关生产模块编译 | 通过 | GameCommon、Account、Gateway、Bootstrap 及 reactor 依赖，`maven.test.skip=true package` |
| Admin 类型、测试、生产构建 | 通过 | vue-tsc；Vitest 4 文件/14 用例；Vite production build |

## 全 reactor 阻塞（非 V12 修改边界）

未使用跳过项的 Maven 全链路验证仍被并发工作区中的既有问题阻塞：品牌门禁命中 `tools/legacy-isolation` 内容；随后还观察到 `ConfigCenter/RoomIsolationAndApprovalTest` 与当前生产构造器签名不一致。未修改这些非独占边界，也未覆盖其他 AI 的文件。V12 自身门禁、相关生产编译与专项测试均已独立执行通过。

## 暂缓项

- Cocos Creator 编辑器人工打开与逐场景观察。
- 浏览器、移动端和真实设备人工操作。
- 生产容量、高并发压测、真实数据库长期运行及日志保留周期观察。
