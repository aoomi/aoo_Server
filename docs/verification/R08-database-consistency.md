# R08 数据库一致性整改验证

## 结论

R08 专属范围已整改并通过。全量 87 个 SQL migration 在一次性 MySQL 8.4.5 容器中按数值版本顺序从空库执行成功；数据库一致性门禁、配置仓储测试和转换工具测试均通过。未引入内存占位或旧数据库旁路。

## 实际整改

1. 修复 `V20260824_34` 重复增加 `aoo_ledger.currency_scope_id` 及重复超长索引导致空库迁移中断的问题。
2. 修复 `V20260824_42` 的 `manifest_id + asset_path` 主键超过 InnoDB 3072 字节上限的问题；路径上限收敛为 760 个 utf8mb4 字符。
3. 新增 `V20260824_77`，用复合唯一键和复合外键把编译建房索引、当前激活指针、房间规则锁、模板规则锁及发布配置严格绑定到同一 `game_id + play_version + release_id`，消除独立外键可拼接不同玩法/版本发布的缺陷。
4. 发布配置新增不可变 `release_id`。数据库触发器拒绝无发布版本、非 `VALIDATED/ACTIVE` 版本；JDBC 仓储在事务内锁定最新可发布 release 后写入，拒绝空 payload、非法操作人/原因及解码后的玩法版本身份错配。
5. 补齐成都、永州标准地区与 `sc/cd/yongzhou` 来源别名；528 行启动目录的未映射有效地区从 8 降为 0。标记为 `unpublished` 的禁用隔离行不伪装成地区。
6. 为原治理种子之后新增的全部权威表补齐 owner、分级、留存、法律保留、匿名化和备份清除策略；治理缺口由 116 降为 0。
7. MySQL 门禁新增发布身份负向测试：拒绝“编译索引 A + release B”的房间锁，拒绝无 release 的发布配置，同时保留成功发布路径。

## 覆盖结果

- MySQL 8.4.5：87/87 migrations 成功，幂等业务过程与负向约束门禁通过。
- Schema：216 张基础表，1912 列，1204 个索引列，773 个约束；146 个外键、320 个 CHECK。
- 五个零行一致性视图全部为 0：活动玩法覆盖、组件完整性、组件图、发布预检、数据治理。
- 建房自动索引：激活指针、cache epoch、发布审计、Outbox 在同一事务提交；失败发布保持旧 release `900100`，没有半发布事件。
- 多实例并发：激活过程以玩法目录行 `FOR UPDATE` 串行化；JDBC 发布以 release 行 `FOR UPDATE` 加数据库唯一键收敛竞态。
- 事件/快照/回放：房间锁不可更新/删除；发布删除、缺失玩法版本引用、大小写重复玩法码均被拒绝；事件追加历史和版本 sidecar 门禁保持通过。
- 账务：无 FLOAT/DOUBLE 金额或算分列；币种/范围、账本对账、并发版本元数据通过。
- 备份恢复与治理：恢复、故障转移、归档、隐私、修复审批、迁移成功/失败路径均由数据库门禁评分通过。这里是一次性本地拓扑证据，不冒充生产恢复演练。
- 查询：5 万房间事件下，建房索引为 `const`；事件 keyset 为主键 range，100 行实际读取 100 行。
- 旧配置转换：560 个目录行、75,180 条房费策略、5,984 条创建 UI、5,627 条帮助块；全部保持 DRAFT，未绕过 Provider/组件注册发布门禁。

## 执行证据

```text
python3 database/tools/mysql_integration_gate.py --container aoo-r08-database-gate-20260824
=> passed=true

python3 database/tools/legacy_config_converter.py
python3 database/tools/database_package_audit.py
=> passed=true, catalog=528, enabled=223, providers=20, invalidRegions=0

cd database/tools && python3 -m unittest test_database_tools
=> Ran 6 tests, OK

./mvnw -pl server/ConfigCenter -am -Dexec.skip=true \
  -Dtest=JdbcGameConfigurationRepositoryTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
=> Tests run: 2, Failures: 0, Errors: 0; BUILD SUCCESS
```

机器可读证据：

- `database/.work/audit/mysql-integration-gate.json`
- `database/.work/audit/database-package-static.json`
- `database/target/data-dictionary.json`
- `database/target/legacy-2.22-structured/manifest.json`

## 边界说明

本次只修改数据库 schema/migration、数据库工具与门禁、玩法配置 JDBC 仓储及测试、本文档。未修改 Client/Admin UI、Gateway/公共协议或具体玩法运行时逻辑。生产级备份恢复、主从切换和容量演练仍需要获授权的生产等价拓扑；本次已验证其 schema、约束、评分流程和失败闭锁行为。
