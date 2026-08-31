# V09 服务端权威与安全验证

更新时间：2026-08-24

## 结论

本专项已完成代码整改和自动化复验。统一 WSS 游戏写入口现在拒绝客户端提交服务端拥有的手牌集合、牌墙/牌堆、随机种子、余额、分数增量、结算、胜者、当前座位、`serverSeq`、`stateVersion` 等状态；`game.action` 必须携带与服务端当前版本一致的 `expectedStateVersion`。响应中的 `stateVersion` 与 `serverSeq` 由服务端覆盖生成。

扑克、麻将、长牌和目录桥接会话改为维护独立、单调递增的服务端状态版本，不再把客户端 `seq` 当成 `stateVersion`。扑克与麻将的恢复快照同时保存、恢复该版本。客户端房间控制器不再把本地 `stateVersion` 作为权威字段回传，只发送 `expectedStateVersion` 乐观并发条件。

现有公共权威链继续负责：连接会话身份与座位绑定、房间/玩法版本、连续 `seq`、时间窗、`requestId` 幂等、规则链、服务端游戏状态执行、服务端结算校验与零和校验，以及玩家/观战/重连视角裁剪。暗牌视图由各权威会话按认证玩家裁剪，其他座位仅返回牌数和零值掩码。

## 新增安全验证

- `ServerAuthorityInputGuardTest.requiresAndMatchesServerStateVersionForCanonicalActions`：缺失或过期版本被拒绝，匹配版本和纯操作意图可通过。
- `ServerAuthorityInputGuardTest.rejectsNestedClientClaimsForServerOwnedState`：嵌套伪造余额、手牌集合和 `stateVersion` 被拒绝。
- `PokerDispatchCommandHandlerTest`：证明响应版本来自权威会话，且不等于客户端请求序列。

## 自动化证据

执行命令：

```text
JAVA_HOME=/Users/aoo/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home \
./mvnw -pl server/GameSPI,server/GameCommon,server/Poker,server/Mahjong,server/LongCard,server/WordCard,server/Gateway \
  -am test -DskipITs -Dexec.skip=true -q
```

最新合并快照复跑退出码 `0`；共 `344` 个测试，`0` failure，`0` error，`8` skipped（数据库集成环境测试）。模块明细：GameSPI 48、GameCommon 169、Poker 25、Mahjong 42、LongCard 14、WordCard 14、Gateway 32。

全 reactor 的首次基线验证在专项代码编译前被品牌边界门禁阻断，原因是并发任务生成的 `Server/tools/legacy-isolation/{audit.json,test_gate.py,gate.py}` 含受禁历史标识；本专项未修改或覆盖这些边界外文件。客户端目录当前无 `node_modules/.bin/tsc`，因此未伪造 TypeScript 构建通过记录；改动为类型安全的对象解构，留待 V01 同快照统一构建复验。

V07 合并后重新核验生产 Provider：9 个 ServiceLoader 声明均唯一，`audit_authoritative_game_provider_coverage.rb` 输出 `providerCount=9, complete=9, duplicateServiceLoaderDeclarations={}, passed=true`。AYCP 和 BYZP 均已完成真实权威迁移及九阶段测试，不是重复声明或目录桥。

原 `517/cdxzmj` 冲突已消除：528 目录只有 `(516,cdxzmj,CITY,SC,CD,legacy-equivalent-1)`，不存在 517 行；全目录 gameId/code 重复扫描退出 0。再次执行 `./mvnw -Dexec.skip=true -pl server/Bootstrap -am test -DskipTests=false` 时，前 27 个 reactor 模块及其测试通过，随后被边界外 `RoomSafety/JdbcRoomSafetyService.java` 的 6 个 Java 语法错误阻断，Bootstrap 未进入执行。故重复 ServiceLoader 与 517 映射不再是当前阻塞，统一 Reactor 仍因 RoomSafety 并发损坏未通过。

人工浏览器、编辑器和真实设备验收按任务要求暂缓。

## JDBC stateVersion CAS 闭环复验

V03 复验指出的持久化缺口已完成代码整改：

- `ProductionRoomCommandCommitter` 删除按房间维护的进程内 `AtomicLong`，以数据库最新快照版本作为唯一已提交基线；只允许权威会话提交精确的下一版本，读请求不制造修订。
- `JdbcRoomEventJournal.appendAndSnapshotAndOutboxCas` 要求 `nextStateVersion = expectedStateVersion + 1`，在同一 JDBC 事务内锁定当前快照，并以包含 `room_id`、fencing token 和 `state_version` 的条件更新完成 compare-and-set；CAS 失败时 event、snapshot、outbox 全部回滚。
- `ProductionRoomRealtimeService` 与 shuffle/kick 管理入口不再把 `command.sequence()` 或客户端 `stateVersion` 当成权威修订；它们读取 JDBC 版本、校验 `expectedStateVersion`，并原子持久化下一版本及裁剪后的房间状态。
- 新增 `JdbcRoomEventJournalCasContractTest`，覆盖首次版本、合法下一版本、过期 expected、旧 fencing、跳版本及无基线非零 expected 的拒绝。

V09 reactor 复跑：GameSPI 48、GameCommon 169、Poker 25、Mahjong 42、LongCard 14、Gateway 32，共 330 项，0 failure、0 error、8 skipped；GameCommon/gameServer 联合 reactor 退出码 0。生产提交及恢复故障自测两项均 PASS。静态复扫仅保留 transport `serverSeq` 输出，不存在以 transport seq 或 `AtomicLong` 生成权威 `stateVersion` 的生产代码。
