# 110 Native prefab 目录归位验证

日期：2026-08-25  
客户端：`/Users/aoo/Code/Game/BCG/Aoo/Client`  
Creator：3.8.8

## 结论

已实际完成全部七处 `Native` 资源树的迁移，而非只做审计。共处理 1,603 个资源文件：正常移动 1,597 个，内容完全相同的重复 prefab 合并 6 个。`Client/assets` 中名为 `Native/native` 的目录为 0，源码、配置、测试、正式文档及迁移清单中的 `Prefab/Native`、`/Native/` 物理路径引用为 0。Prefab 及其 `.meta` 成对移动；正常迁移保留原 UUID，合并项把引用统一到保留 UUID。

Creator 3.8.8 实际导入和 Web Desktop 构建成功，六个 bundle 均以所属域的 `Prefab` 目录为新根输出：`common-prefab`、`club-prefab`、`lobby-prefab`、`mahjong01-prefab`、`poker01-prefab`，以及既有 `Result`。严格 TypeScript 与资源完整性门禁通过。87 完整隔离门禁 `blocking=0`、`passed=true`，7/7 门禁单测通过。

唯一未能宣称全绿的项目是受保护的 `LoginScene.scene`：110 没有主动编辑该文件，但 Creator 3.8.8 命令行导入在 00:08:07 写回了它；随后 109 的 EditBox 门禁报告该场景不再规范化，登录场景结构测试也发现既有节点缺失。依据根目录 `AGENTS.md` 的绝对只读规则，本任务没有用规范化脚本写回、没有猜测恢复、也没有改测试掩盖问题。需由 LoginScene 所有者从 109 完成快照/界面权威版本恢复后复跑该门禁。

## 迁移映射

完整逐文件映射保存在客户端工作证据 `Client/work/110-native-migration-map.tsv`，UUID 合并记录保存在 `Client/work/110-native-uuid-rewrites.tsv`。规则和数量如下：

| 原目录 | 新目录规则 | 数量 | 子层保留理由 |
|---|---|---:|---|
| `Club/Prefab/Native/default` | `Club/Prefab` | 164（含 Club 其他层） | `skin-1`、`skin-2` 为明确皮肤复用域；桌型子目录存在大量同名 prefab 冲突 |
| `Common/Prefab/Native/common` | `Common/Prefab` 直属 | 15 | `WaitForm`、`WaitNet` 直属；Club 的 `btn_table` 迁到 `Club/Prefab` |
| `Games/Common/Prefab/Native` | `Games/Common/Prefab` | 4 | `scene/Layer` 为动态加载路径语义 |
| `Games/Mahjong/Packs/Pack01/Prefab/Native` | `.../Prefab` | 113 prefab / 118 资源 | `game/hzmj` 为 pack、玩法和加载路径边界 |
| `Games/Poker/Packs/Pack01/Atlas/Native` | `.../Atlas` | 4 资源 | 非 prefab 图集随原 Atlas 域，仅移除 Native 层 |
| `Games/Poker/Packs/Pack01/Prefab/Native` | `.../Prefab` | 52 prefab / 52 资源 | `njpdk` 为玩法与加载路径边界 |
| `Lobby/Prefab/Native` | `Lobby/Prefab` | 1,245 | 普通 prefab 与 `uiChild` 已压平；`uiGame/<玩法>` 因 508 个玩法隔离和动态路径语义保留 |

## 重复 prefab 处理

- `btn_table.prefab` 的三个完全相同副本统一为 `Club/Prefab/btn_table.prefab`，保留 Club UUID `a6993e5f-8d0a-4973-a256-2067b518ad92`；Common 中的 clubmain 目录一并删除。
- `Games/Common/Prefab/scene/Layer` 四个既有目标副本与 Native 来源内容完全一致，保留目标 UUID，并将来源 UUID 引用统一后删除来源副本。
- 全项目剩余内容完全相同组共 12 组。10 组位于 `Lobby/Prefab/uiGame/<玩法>` 或具体 Games 域，因玩法隔离保留。另两组为 `WaitForm`/`WaitNet` 的 `Common/Prefab` bundle 版本与 `Common/Spine` 资源版本，因 Asset Bundle 边界与既有动态加载入口保留。
- 没有覆盖任何同名但内容不同的 prefab，没有伪合并。

## Bundle、路径和引用

- 原 `Native.meta` 中的 bundle 配置迁到所属 `Prefab.meta`，目录 UUID 使用所属 Prefab 原 UUID，bundle 名称、优先级和配置 ID 不变。
- 迁移清单 `development/migration/aoo_DFMJ-3.8.8/file-map.csv` 已改为新物理路径；109 正式文档与测试 fixture 已同步。
- `assets`、`development`、`docs`、`tests`、`scripts`、`settings` 扫描结果：Native 物理路径引用 0。
- `Client/assets` 下 Native/native 目录和对应目录 meta：0。
- 玩法 Provider/客户端适配器未修改；本轮没有发现必须由其 owner 文件改写的 Native prefab 物理路径。

## 验证证据

| 验证 | 结果 |
|---|---|
| Cocos Creator 3.8.8 命令行导入 + `web-desktop` build | PASS；六个迁移 bundle 均成功输出 |
| Creator scene/prefab/meta/UUID 门禁 | PASS：1,620 scenes/prefabs，25,326 assets/meta，33,638 UUID，blocking=0 |
| 严格 TypeScript（Creator 3.8.8 自带 TypeScript） | PASS |
| 87 完整隔离门禁 | PASS：25,400 files，57,273 findings，blocking=0，passed=true |
| 87 门禁单测 | PASS：7/7 |
| `common-ui-reference-gate` | PASS；已允许有真实理由的 Club skin 复用域，不再把它误判为旧重复根 |
| EditBox 109 门禁（迁移后、Creator 导入前） | PASS：210 EditBox / 104 assets，0 changed；组件与 prefab 内容在移动时保持不变 |
| EditBox 109 门禁（Creator 导入后） | BLOCKED：`LoginScene.scene` 被 Creator 写回后不再规范化；受只读规则保护，110 未写回 |
| 全量 Node tests | 118/120 PASS；Native 迁移导致的 prefab 根测试已修复并单独复跑通过；另一个失败为上述 LoginScene 受保护问题 |

## 保护边界

- 未修改任何具体玩法 Provider 或客户端适配器业务代码。
- Prefab 文件只做路径移动；109 的 `EditBoxLabelLayout`、标签绑定和 prefab 序列化内容均未被迁移脚本改写。
- 未主动编辑 `assets/Login/Scenes/LoginScene.scene`。Creator 自动写回造成的受保护文件漂移没有在本任务中二次覆盖或猜测修复。

## 编辑器资源数据库同步补验

2026-08-25 根据编辑器可见性反馈追加执行：关闭并通过 Cocos Dashboard 重新打开 Client 项目，使当前 Cocos Creator 3.8.8 实例重新装载 AssetDB。随后在资源管理器中展开 `Common/Prefab`，14 个 prefab 已全部可见；迁移遗留的空 `effect`、`texture` 目录及 meta 已删除，重启后不再显示。磁盘、AssetDB 和编辑器资源树现已一致。后续涉及 Creator 资源增删或移动的任务，必须把当前编辑器实例中的导入/刷新与资源树可见性作为完成条件。
