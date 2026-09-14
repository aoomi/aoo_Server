# V13 文档与台账一致性复核

日期：2026-08-24

## 结论

V13 未验收通过，状态保持“待统一稽核”。本轮已完成可在文档/工具边界内确定处理的整改：确立唯一权威清单，删除两份过期重复当前台账，修正六份阶段二任务包的断链引用，解耦分类冲突生成器对旧任务台账的回写，并新增可重复的 V13 门禁与负向测试。

阻塞验收的当前事实是：协议中 `longcard.aydss.dispatch` 没有在生产 `ApiOwnershipCatalog` 登记所有者；玩法/地区冲突台账仍是 528/528 玩法有冲突；玩法说明生成仍阻塞于 profile 发布；文档中 292 条“已完成”声明有 273 条未达现行证据规则。这些项涉及生产源码责任方或大量历史证据补正，本轮没有越界修改，也没有把专项自报完成写成验收通过。

## 整改结果

- 唯一权威台账：`docs/Aoo-当前剩余问题与处理任务清单.md`。
- 已删除重复台账：`docs/Aoo-当前未完成任务清单.md`、`docs/未完成任务清单.md`。它们的结论与当前四项处理中任务及 V01–V13 统一稽核口径冲突，且包含无法仅凭文档验证的“已完成”状态。
- 历史专项文档、生成 JSON、测试类名和阻塞记录均保留为审计证据，未将其作为当前状态源。
- `tools/generate_classification_conflict_ledger.rb` 现在只生成事实报告，不再回写历史任务清单，避免第二状态源。
- `scripts/audit-v13-doc-ledger.rb` 覆盖权威台账唯一性、活动状态、接口、玩法、地区、数据库、依赖和测试证据。`scripts/test-audit-v13-doc-ledger.rb` 验证未解决证据必须阻断通过。

## 验证记录

| 领域 | 命令 | 结果 |
|---|---|---|
| 依赖/版本 | `ruby scripts/audit-drift02-version-build.rb` | 通过；4 项版本和 wrapper 检查均为 true |
| 接口/路由 | `ruby scripts/audit-drift03-interface-routing.rb` | 失败；注册表、文档和元数据一致，但 `longcard.*` 所有权缺失 |
| 数据库 | `ruby scripts/audit-drift04-schema-dictionary.rb` | 通过；86 个 SQL 文件、1182 个 schema 条目受控 |
| 玩法文档 | `ruby scripts/audit-drift05-play-documentation.rb` | 审计执行成功，业务结论 `blocked-profile-publication` |
| 玩法/地区 | `ruby tools/generate_classification_conflict_ledger.rb` | 生成成功；528 个玩法全部仍有冲突，2635 个冲突项 |
| 完成声明证据 | `ruby scripts/audit-drift09-ledger-evidence.rb` | 审计执行成功，结论 `blocked-ledger-remediation`；292 条声明中 273 条有问题 |
| V13 综合门禁 | `ruby scripts/audit-v13-doc-ledger.rb` | 按预期阻断；7/9 项通过，接口所有权和完成声明证据两项未通过 |
| V13 负向测试 | `ruby scripts/test-audit-v13-doc-ledger.rb` | 通过；确认未解决证据会阻断验收，不会被误写为通过 |

机器可读结果：`docs/generated/v13-doc-ledger-consistency.json`。

## 边界与暂缓

本轮未修改任何生产源码、数据库迁移、Client 或 Admin。旧版 `/Users/aoo/Code/Game/BCG/Test` 仅作现有审计证据来源，未写入。人工、真实设备、真实第三方与生产环境验收按要求暂缓。
