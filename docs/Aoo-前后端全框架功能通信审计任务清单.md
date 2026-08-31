# Aoo 前后端全框架功能通信审计任务清单

本清单已于 2026-08-24 拆分为阶段一并行责任包。已完成项已从活动任务中清理；当前可独立处理的任务进入互不重叠的写入责任包，跨目录、前置依赖、人工与环境类任务进入阶段二暂缓清单。

## 并行执行规则

- 每个 AI 只能领取一个责任包，并严格遵守该文件内的独占写入边界。
- 不同责任包没有共同可写源码文件；Spark 仅写各自独立报告目录，全项目其他内容只读。
- 各 AI 持续执行到本包没有可执行未完成项后再统一汇报。
- 阶段一期间不执行跨包收口、全仓构建、全链路测试或最终人工验收。
- 阶段一完成后再从阶段二暂缓清单生成下一批任务，避免先后依赖和并发覆盖。

## 阶段一责任包

- [01 内核、SPI 与公共基础层](parallel-stage-1/01-kernel-common.md)：80 项
- [02 网关与新协议服务端](parallel-stage-1/02-gateway-protocol.md)：32 项
- [03 账号、会话与大厅后端](parallel-stage-1/03-account-hall.md)：20 项
- [04 账务与计费后端](parallel-stage-1/04-billing.md)：19 项
- [05 配置中心与规则配置后端](parallel-stage-1/05-config-center.md)：23 项
- [06 管理后台服务端](parallel-stage-1/06-admin-backend.md)：8 项
- [07 麻将玩法后端](parallel-stage-1/07-mahjong-backend.md)：14 项
- [08 扑克玩法后端](parallel-stage-1/08-poker-backend.md)：14 项
- [09 长牌、字牌与其他玩法后端](parallel-stage-1/09-other-game-backend.md)：8 项
- [10 遗留运行时隔离与兼容收口](parallel-stage-1/10-legacy-runtime.md)：15 项
- [11 数据库、迁移、索引与数据治理](parallel-stage-1/11-database.md)：75 项
- [12 客户端公共层、登录与大厅](parallel-stage-1/12-client-common.md)：66 项
- [13 客户端亲友圈](parallel-stage-1/13-client-club.md)：22 项
- [14 构建、依赖、门禁与工程治理](parallel-stage-1/14-engineering-governance.md)：96 项

## Spark 同阶段并行盘点包

- [S01 命名、品牌与遗留标识静态审计](parallel-stage-1/S01-naming-brand-inventory.md)：12 项
- [S02 目录膨胀、作用域与死代码静态审计](parallel-stage-1/S02-scope-size-deadcode-inventory.md)：18 项
- [S03 调用链、入口与未引用接口静态审计](parallel-stage-1/S03-callgraph-api-inventory.md)：12 项
- [S04 文档、证据与可观测性缺口盘点](parallel-stage-1/S04-docs-evidence-inventory.md)：21 项

## Spark 第二批同阶段只读盘点包

- [S05 依赖、插件与运行版本清单](parallel-stage-1/S05-dependency-plugin-manifest.md)：31 项
- [S06 HTTP、WebSocket 与跨端接口矩阵](parallel-stage-1/S06-endpoint-protocol-matrix.md)：58 项
- [S07 全 UI、按钮、弹窗与交互测试目录](parallel-stage-1/S07-ui-interaction-catalog.md)：114 项
- [S08 玩法、地区、规则与分类目录](parallel-stage-1/S08-game-region-rule-catalog.md)：44 项
- [S09 遗留接口、旧依赖与迁移隔离清单](parallel-stage-1/S09-legacy-isolation-inventory.md)：28 项
- [S10 安全、服务端权威与数据暴露检查表](parallel-stage-1/S10-security-authority-inventory.md)：44 项
- [S11 测试类型、场景与验收覆盖矩阵](parallel-stage-1/S11-test-coverage-matrix.md)：122 项
- [S12 运维、任务、灾备与发布门禁清单](parallel-stage-1/S12-operations-resilience-inventory.md)：49 项

## 阶段二暂缓

- [阶段二暂缓与协调清单](parallel-stage-1/90-stage-2-deferred.md)：811 项

## 本次拆分统计

- 未闭环活动任务：1303 项
- 阶段一已分配：492 项
- 阶段二暂缓：811 项
- 已完成项：已从活动清单清理，不再重复分发
