# 108 架构门禁与正式文档对账

## 结论

- `Bootstrap` 是唯一组装根；它直接依赖具体游戏 JAR，再由 `ServiceLoader<GameProvider>` 发现 Provider，是生产装配的预期方向。
- 除 `Bootstrap` 外，大厅、公共层、SPI、牌类和玩法族均不得依赖具体游戏；`GameSPI -> GameCommon` 等由内向外的反向依赖同样会被拒绝。
- 目录基线按已完成结构变更的实际受控文件数重新审定；后续增长仍会失败，不是无上限放宽。
- 根 POM 当前声明 43 个子模块，连根工程共 44 个 Reactor projects。正式文档已对齐 Jedis `8.0.0`、MongoDB Driver `5.10.0` 和客户端 Node.js `24.19.0`。

## 验证

```bash
ruby tools/test/check_architecture_boundaries_test.rb
ruby tools/check_architecture_boundaries.rb
ruby scripts/audit-real14-maven-module-graph.rb
```

专属测试分别证明：Bootstrap 可装配具体游戏、SPI 反向依赖必须失败、Bootstrap 以外的具体游戏耦合必须失败。生产门禁同时对当前正确代码通过。
