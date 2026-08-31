# R11 性能与稳定性整改验证

验证时间：2026-08-24（Asia/Shanghai）

## 结论

R11 在 Server 基础设施、运行时、配置和观测独占边界内通过。先执行源码与配置审计，再修复了 2 类真实运行时配置缺陷：Legacy Account 的 OkHttp 毫秒/秒单位错配，以及 MongoDB 无限读等待、连接永不老化。业务接口、权威状态和协议未变。

## 已整改

- `OKHttpConfig` 原先把 `*-timeout-ms` 当作秒使用（默认 50 实际成为 50 秒），同时把 `keep-alive-duration-sec` 当作分钟使用。现分别使用 `MILLISECONDS` 与 `SECONDS`，并新增运行时对象断言测试。
- `MongoSettingsProperties` 原先 `socketTimeout=0`、连接 idle/lifetime 均为 0，可造成故障连接无限阻塞和陈旧连接永久驻留。现默认读超时 30 秒、空闲回收 5 分钟、最长寿命 30 分钟，并启用 TCP keepalive；MongoClient 已由 Spring `destroyMethod="close"` 托管释放。
- 新增 `tools/r11_performance_stability_gate.rb`，扫描 954 个非 Provider 生产 Java 文件，阻断源码级死循环、缓存线程池、无界 JDK 队列和上述配置回归，并记录目录最大文件证据。

## 验证证据

| 验证项 | 结果 | 证据 |
|---|---:|---|
| R11 专属门禁 | 通过 | `ruby tools/r11_performance_stability_gate.rb`；`work/audit/r11-performance-stability.json`，954 文件，0 失败 |
| 既有 V12 稳定性门禁 | 通过 | `ruby tools/v12_performance_stability_gate.rb`；4,411 文件，0 失败 |
| Legacy Account reactor | 通过 | JDK 26 执行 `./mvnw -f server/LegacyAccountServer/pom.xml -Dtest=OKHttpConfigTest -Dsurefire.failIfNoSpecifiedTests=false test -q` |
| OkHttp 配置专项测试 | 通过 | `OKHttpConfigTest`：1 用例，0 失败、0 错误、0 跳过 |

## 目录与包审计

- 最大静态文件为 `database/original/` 下的历史原始 SQL（约 1.20 GB、672 MB、132 MB）；属于数据库 owner 的历史输入，本任务未删除或改写。
- 多个模块 `target/runtime/lib` 各自包含约 34 MB 的 `rocketmq-rocksdb`，属于构建产物重复展开，不是运行期持续增长；现有发布打包门禁负责制品清单，本任务未对其他 owner 的构建结构做越界调整。
- 未发现 Server 生产源码中的无界 JDK 队列、缓存线程池或源码级永久循环；日志配置已有 `maxHistory`/`totalSizeCap` 门禁，Outbox 和清理任务已有有限批次与关闭生命周期门禁。

## 精确边界外缺口

以下项需要修改玩法实现或协议路由，按独占边界仅记录：

- `server/Account/src/main/java/com/aoo/bcg/account/AccountHttpRoutes.java` 的表单读取使用 `readAllBytes()`，未在读取前限制请求体；同模块 `JdbcAccountSessionService.audit(long)` 无分页上限。应由账号/协议 owner 增加流式大小上限和游标分页。
- `server/Activity/src/main/java/com/aoo/bcg/activity/JdbcActivityRepository.java` 的 `active()` 对每个活动调用 `readActivity()` 再查询任务，形成 N+1。应由玩法 owner 改为一次 join/批量查询并保持聚合语义。
- `server/GameCommon/src/main/java/com/aoo/bcg/common/invite/InviteRevocationRegistry.java`、`room/InMemoryRoomCodeStore.java` 和 `time/TemporalEffectGate.java` 的内存表只按同 key 覆盖，缺少统一生命周期驱动的过期清扫接线。修改会触及房间/邀请生命周期 owner，需在其合并后补充显式 purge 调用与容量指标。

## 暂缓

生产数据库长时间运行、真实流量压测、故障注入下连接池耗尽恢复、日志保留周期观察和发布包长期增长趋势需在生产等价环境执行，不以本地静态或单元测试冒充通过。
