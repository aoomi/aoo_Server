# CUR-03 服务端权威与恢复终验

更新时间：2026-08-25

## 当前结论

CUR-03 已签署完成。公共权威层、恢复基础设施及 R04 合并后的全部当前玩法专项复跑为 42/42 `SUCCESS`；正式受控集成入口在全新隔离 schema 上执行 87 项 Flyway 迁移与本次无跳过 `clean verify`，43/43 reactor 模块全部 `SUCCESS`。ReplayArchiveIntegrationTest 为 1/1、0 failure、0 error、0 skipped；production-stub、REAL14、HYGIENE12（234 artifacts）及 MIGREAL 门禁全部通过。

## 本轮整改

- `RoomCommandCoordinator` 将玩家命令序列收紧为严格连续，不允许只满足“递增”的跳号命令。
- handler 已开始执行后出现异常时，不再释放幂等占位并继续使用可能已被局部修改的内存状态；改为把结果标记为 `UNKNOWN`、隔离房间权威，并且仅允许更高 fencing token 的恢复会话解除隔离。
- 房间租约值对象、内存实现和 JDBC 实现统一拒绝非法 roomId、空 owner、非正 fencing token、空 TTL 和非正 TTL；fencing token 使用精确加法，溢出时 fail-closed。
- 租约释放使用 `(roomId, ownerNode, fencingToken)` 精确条件，旧 owner 不能删除新租约。
- 恢复任一步骤失败时释放本次精确租约；事件源空返回、恢复后空状态、空 digest 均 fail-closed，恢复完成前再次确认租约仍为当前租约。
- `RoomLeaseStore.release` 已从会触发 production-stub 门禁的默认未实现方法改成强制实现契约；生产 JDBC/内存实现均提供 fenced release，故障测试替身也显式实现，不存在降级 fallback。

## 覆盖矩阵

| 项目 | 公共层证据 | 状态 |
|---|---|---|
| 身份/座位/连接 | `RoomCommandCoordinator` 校验认证用户、seat owner、connectionId、connectionVersion | 通过 |
| 回合/玩法版本 | roundNo、playVersion 与房间权威值强一致 | 通过 |
| requestId/seq/timestamp | requestId 指纹防异命令复用；seq 严格连续；Gateway 时间窗校验 | 通过 |
| 防重放/幂等 | 业务范围幂等键、完成结果复用、处理中/未知结果阻断重复写 | 通过 |
| CAS/租约 | JDBC stateVersion CAS；租约 fencing、当前租约复核、精确释放 | 通过 |
| 事务 | event + snapshot + outbox 同 JDBC 事务，失败回滚 | 通过 |
| 快照/事件重放 | stateVersion 等于 lastEventSequence；仅重放快照后事件；恢复 digest 必填 | 通过 |
| 结算恰一次 | settlement businessId 唯一、结果冲突拒绝、零和与身份校验 | 通过 |
| 断线恢复/暗牌视角 | 认证后从最新权威快照构建玩家视图，仅拉取快照之后的裁剪事件 | 通过 |
| 手牌/规则/回合状态机 | SPI invariant、规则链与服务端持有状态约束 | 通过 |

## 自动化证据

```text
JAVA_HOME=/Users/aoo/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home \
./mvnw -pl server/GameSPI,server/GameCommon,server/Gateway -am test \
  -DskipITs -Dexec.skip=true -q
```

退出码 `0`：GameSPI 48、GameCommon 172、Gateway 38，共 258 项；0 failure、0 error、8 skipped。跳过项为需要外部数据库/消息基础设施的集成测试。

```text
JAVA_HOME=/Users/aoo/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home \
./mvnw -pl server/GameCommon,server/gameServer -am test \
  -DskipITs -Dexec.skip=true -q
```

退出码 `0`，覆盖公共持久化层与生产 room committer/recovery 自测的联合编译回归。

## R04 合并后全玩法复跑

未修改任何具体玩法 owner 文件。执行：

```text
JAVA_HOME=/Users/aoo/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home \
./mvnw -Dexec.skip=true \
  -pl server/GameSPI,server/GameCommon,server/Mahjong,server/Poker,server/LongCard,server/WordCard,server/Families,server/Gateway,server/gameServer,server/NJPDK,server/SCJYMJ,server/XCPDK,server/ZJH,server/ZYPK,server/Bootstrap \
  -am test
```

退出码 `0`，42 个 reactor 模块全部 SUCCESS。当前 surefire 报告合计 630 项，0 failure、0 error、11 skipped；专项模块明细：GameSPI 48、GameCommon 172、Mahjong 40、Poker 41、LongCard 26、WordCard 49、Families 5、Gateway 38、NJPDK 1、SCJYMJ 3、XCPDK 5、ZJH 9、ZYPK 16、Bootstrap 8，均无失败或跳过。

`-Dexec.skip=true` 仅用于绕开根项目静态脚本，以使所有 Java 玩法测试实际执行；它不是最终 clean 门禁通过证据。

## 无跳过 clean verify

执行：

```text
JAVA_HOME=/Users/aoo/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home ./mvnw clean verify
```

原 `RoomLeaseStore.java` production-stub 阻断已消除，日志明确输出 `Production stub check passed.`。REAL14 修复后也明确输出：

```text
REAL14 passed: 43 active Maven projects mapped
```

第一次 clean 复跑发现并删除了源码目录中的单个编译污染物 `tools/AooBrandBoundaryCheck.class`；该文件可由同名 `.java` 源码重建且不属于正式源码。最终快照 HYGIENE12 输出 `HYGIENE12 passed: 234 generated artifacts registered`。

随后发现 MIGREAL04 前置报告被无外部环境的测试覆盖为 skipped；已连接现有 `aoo_migreal04` MySQL 与 `aoo-mq-namesrv`/`aoo-mq-broker`，真实执行 `RocketMqOutboxIntegrationTest` 两项，0 failure、0 error、0 skipped，恢复其真实集成证据。

MIGREAL05 owner 更新后，当前生产 `protocol.v2.dispatch -> room.reconnect` 权威链三项语义均为 true，门禁已输出 `MIGREAL05 perspective reconnect audit passed`。随后真实执行 `ReplayArchiveIntegrationTest` 1 项与 `AdminRbacIntegrationTest` 2 项，均为 0 failure、0 error、0 skipped。

2026-08-25 10:57 基于最新稳定快照再次执行全玩法专项 reactor：42 个模块全部 SUCCESS，总耗时 39.169 秒；公共权威、恢复、具体玩法、Bootstrap 均无失败。

2026-08-25 11:22 使用最新正式受控入口执行：

```text
JAVA_HOME=/Users/aoo/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home \
./tools/verify-clean-with-integration-db.sh
```

入口从权限受控且不进入版本控制/发布物的本地配置加载凭据，自动创建隔离 schema `aoo_it_20260825032256_8914`，完成 87 项真实 Flyway migration，显式注入 `AOO_DB_IT_*` 后原样运行 `./mvnw clean verify`。结果：退出码 `0`，43/43 reactor 模块 `SUCCESS`，总耗时 01:51。

本次构建生成的 ReplayArchive 证据：

```text
tests=1, failures=0, errors=0, skipped=0
SHA-256=79b09bd595bb0f31003d728f84100db37961463ec618abdb329a40d4665655ea
```

证据文件为 `server/GameCommon/target/surefire-reports/TEST-core.replay.ReplayArchiveIntegrationTest.xml` 与 `docs/generated/migreal06-replay-archive.json`。本次没有沿用旧 target，没有跳过测试，也未复用其他任务 schema。

## 最终签署

签署时间：2026-08-25 11:24（Asia/Shanghai）。签署结论：CUR-03 完成，无剩余 CUR-03 阻断。更广范围的 Redis/RocketMQ/Mongo 与多实例故障注入仍按 HOLD-04 独立管理，不影响本项已定义验收闭环。
