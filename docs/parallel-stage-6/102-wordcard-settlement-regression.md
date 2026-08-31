# 102 WordCard BZP settlement regression

## 结论

已修复 `BzpNineStageAcceptanceTest.authoritativeOutFinishesAndSettles` 的 `expected 2 but was 7`。偏差项是**结算分数**，不是赢家、座位、终局状态或事件顺序：赢家座位 0 / 玩家 10 正常，终局正常进入 `FINISHED`；`0x35` 是 5 分牌，旧实现先把该牌面分累计进 `points`（5），又把无报玩法的胜负基础分（2）写入同一个 `points`，最后把混合值作为 `SettlementPayload.scoreDelta`，因此得到 7。

BZP 权威状态中牌局过程分（5/10/K 与牌型奖分）和账户结算输赢是两个业务量。修复后 `point` 仍显示赢家取得的 5 分牌面分，而 `scoreDelta` 只按终局赢家和报庄倍率计算，两人普通局为玩家 10 `+2`、玩家 20 `-2`，总和为 0。

## 修复范围

- `BzpSession.finish()` 只执行不可逆的 `PLAYING -> FINISHED` 状态转换，不再把胜负基础分写入过程 `points`。
- `settlement()` 从权威 `ranking/challenge/suBao/players` 独立、纯函数式生成账户分差，重复调用不会再次累计。
- `requestId` 去重记录进入权威快照和事件，重试同一请求不会再次出牌、推进 `stateVersion` 或新增事件；同一 `requestId` 携带不同序列/负载会被拒绝。恢复和事件重放后仍保持该幂等边界。
- 出牌先在手牌副本上完整校验，再一次性提交，客户端提交“部分持有、部分伪造”的组合时不会留下半次扣牌。
- 保留既有 `stateVersion` 连续递增与 replay gap 检查。检查 `BzpGameProvider` 后确认 provider、服务目录映射均唯一指向原生 BZP 实现，没有座位反向映射或状态枚举混用。

未改通用协议、Gateway、其他玩法，也未修改预期值规避错误。回归用例恢复使用触发缺陷的 `0x35`，并同时断言过程牌面分为 5、结算分为 2。

## 新增回归覆盖

`BzpNineStageAcceptanceTest` 当前覆盖：

- 正向终局：5 分牌出完后赢家、终局、过程分与结算分分别正确。
- 九阶段正向流程：创建、加入、准备、开始、报庄、报牌、操作、恢复/重放、结算。
- 边界：非法混合手牌原子拒绝，状态不变；事件版本跳号拒绝。
- 重复 `requestId`：相同请求幂等，不增加版本/事件；冲突复用拒绝。
- 重放与重连恢复：事件重放结果、玩家视角和恢复快照一致。
- 结算幂等：原会话和恢复会话重复结算结果一致。

## 可复现命令与证据

为避免根 `validate` 中与本任务无关的全仓生成物门禁干扰，以下命令直接调用同一 Maven 编译器和 Surefire goal，并仍使用 `-am` 构建 `GameSPI -> WordCard` 相关 reactor。

```bash
cd /Users/aoo/Code/Game/BCG/Aoo/Server

./mvnw -pl server/WordCard -am \
  compiler:compile compiler:testCompile surefire:test \
  -Dtest=BzpNineStageAcceptanceTest,BzpRulesTest \
  -Dsurefire.failIfNoSpecifiedTests=false
# Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
# Reactor: Aoo Server / Aoo Game SPI / game-category-word-card — BUILD SUCCESS

./mvnw -pl server/WordCard -am \
  compiler:compile compiler:testCompile surefire:test \
  -Dtest='*NineStageAcceptanceTest' \
  -Dsurefire.failIfNoSpecifiedTests=false
# Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS

./mvnw -pl server/WordCard -am \
  compiler:compile compiler:testCompile surefire:test
# GameSPI: Tests run: 48, Failures: 0, Errors: 0, Skipped: 0
# WordCard: Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS
```

最后成功复跑时间：2026-08-24 22:27 CST。可交由 V03 做最终全 reactor 复验。

## V03 合并后 YZCHZ 稳定性复验（2026-08-24）

V03 报告的 `YzchzGameProviderTest.creationJoinReadyStartOperationRotationReconnectSettlementAndReplay` / `YZCHZ requires 15 hu-xi` 在当前合并工作区首次定向复跑及全 WordCard 复跑均未再次出现。该异常的业务含义是：终局赢家传入结算公式的权威胡息低于永州扯胡子最低 15 胡息，并非分数上限、飘花或王惩罚错误。检查确认胡牌准入使用 `game.huXi + lockedHuXi`，结算也必须使用同一权威合计；不能降低 `YzchzRules.settlement` 的 15 胡息规则。

为防止合并/恢复状态造成“已胡但不可结算”的分叉，`YzchzSession` 现在显式固定以下边界：

- HU 准入先固定读取同一时刻的手牌胡息和锁定胡息，交给规则集判断。
- 结算再次通过相同的 `YzchzRules.canHu(handHuXi, lockedHuXi)` 权威门禁，再将两者之和传入计分公式；低于 15 的伪终局状态拒绝结算，不绕过真实规则。
- `invariantViolations()` 对恢复/重放得到的非法终局输出 `WINNER_HU_XI_BELOW_15`，使 V03 可直接识别状态污染，而不是等到计分公式抛出含混异常。
- 新增回归构造低于 15 胡息的恢复终局，验证不变量和结算均 fail-closed；原创建、加入、准备、开始、操作、重连、结算、事件重放正向链路继续通过。

复验命令与证据：

```bash
cd /Users/aoo/Code/Game/BCG/Aoo/Server

./mvnw -pl server/WordCard -am \
  compiler:compile compiler:testCompile surefire:test \
  -Dtest=YzchzGameProviderTest,YzchzRulesTest \
  -Dsurefire.failIfNoSpecifiedTests=false
# WordCard: Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS

./mvnw -pl server/WordCard -am \
  compiler:compile compiler:testCompile surefire:test \
  -Dsurefire.failIfNoSpecifiedTests=false
# GameSPI: Tests run: 48, Failures: 0, Errors: 0, Skipped: 0
# WordCard: Tests run: 47, Failures: 0, Errors: 0, Skipped: 0
# Reactor: Aoo Server / Aoo Game SPI / game-category-word-card — BUILD SUCCESS
```

最后成功复跑时间：2026-08-24 22:42 CST。修改仍仅限 WordCard YZCHZ 会话、其测试和本报告；未修改协议、Gateway 或其他玩法。可交由 V03 执行最终全 reactor 复验。
