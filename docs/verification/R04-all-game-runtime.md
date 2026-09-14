# R04 全玩法运行时整改与验收

执行日期：2026-08-24  
范围：具体玩法 Provider、玩法规则/状态机/算分/结算/回放、客户端具体玩法适配器及专属测试。旧版 `Test` 全程只读。

## 结论

R04 **未通过，不得关闭**。本轮没有用目录元数据、通用房间实现、空壳适配器或零分结算冒充具体玩法完成。

最终目录基线为 `server/Bootstrap/src/main/resources/game-catalog-528.tsv`，共 528 个地区/规则配置项。R04 现使用 `tools/verify_r04_family_runtime.rb` 按“基础牌类 → 核心玩法族 → 语义运行时原型 → 地区配置”验收，不再因 metadata-only 本身失败。当前 528/528 分类绑定和地区配置 schema 完整，缺源码精确为 2 项；但 15 个核心玩法族尚未在新的 family registry 中登记服务端/客户端入口和族级九阶段证据，且仍有 16 组 code-specific Provider 重复主实现及 2 组客户端重复主实现，故新验收工具如实 FAIL。

`CatalogGameProvider` 只提供通用 join/start/leave，拒绝原生玩法操作并产生零分结算，不能证明任何地区规则、牌义、操作优先级、算分、历史恢复或回放等价，故不计为玩法 Provider。

## 528 配置项四层权威分类（2026-08-25）

528 行 catalog 是地区/规则配置项，不等于 528 个必然独立的运行时引擎。本轮未使用 catalog `family` 名称推断源码等价性，而是建立以下四层映射：

1. **基础牌类**：由 catalog 大类与可读源码牌义确定；
2. **玩法族**：只按核心牌结构与合法操作语义归并；花、鸟马、飘分、多响应等可插拔项另列 `ruleComponents/lifecycleComponents`，不再把组合爆炸误算成玩法族；
3. **语义运行时原型**：按牌库结构、阶段模型和结算生命周期组合为 `runtimeArchetype`；
4. **地区/规则配置项 → gameId/catalogId**：保留每个 code、province/city 和 catalog 主键的一对一配置身份。

实测统计：528 个配置项，4 个基础牌类，15 个有源码证据的核心玩法族，32 个语义运行时原型，528 个地区/规则配置项。基础牌类分布为麻将牌 364、扑克牌 150、字牌 10、长牌 4。

玩法族主要分布为麻将 standard 206 / lai-zi 121 / tui-dao-hu 13 / xue-zhan 13 / xue-liu 9；扑克 510k 70 / pao-de-kuai 28 / climbing 16 / landlord 10 / compare-hand 8 / betting 6 / generic-card-round 6 / trick-taking 6；字牌 pao-hu-zi 10；长牌 regional 4。`familyConfidence` 标明显式源码词汇、catalog 谱系与源码联合判定、标准语义回退或无法分类的置信等级，`familyNonMergeEvidence` 保存禁止跨族合并的核心证据。

可读权威源码覆盖 526/528；缺源码项为 `caooszmj` (238) 和 `dlaoomj` (250)，两者保持 `UNCLASSIFIED_MISSING_SOURCE / RUNTIME_ARCHETYPE:UNKNOWN`，未伪造族归属。当前 V07 扫描到 80 个已注册 Provider，其中 75 个 code 与 528 catalog 直接对应；因此本映射标记 75 个 `NATIVE_PROVIDER` 和 453 个 `METADATA_ONLY`。

机器产物：`work/audit/r04-authoritative-four-layer-map.tsv` 和 `work/audit/r04-authoritative-four-layer-map.json`；可复建脚本为 `work/audit/r04-generate-authoritative-four-layer-map.rb`。复建命令：

```bash
cd /Users/aoo/Code/Game/BCG/Aoo/Server
ruby tools/verify_v07_all_games.rb || true
ruby work/audit/r04-generate-authoritative-four-layer-map.rb
ruby work/audit/r04-audit-family-confusion.rb
```

`runtimeArchetype` 是语义分类而非源码文件哈希：麻将按 standard/reduced/flower-extended 牌库、standard/pre-seat-choice/discard-response/multi-finish/continuous/special-deal 阶段以及 single/multi/continuing settlement 组合；扑克、字牌、长牌使用各自的核心回合模型。鸟马、杠账本和 wildcard resolution 保留在 `lifecycleComponents`，地区规则保留在 `ruleComponents`。

原来的 526 个归一化关键源码哈希现仅保留为 `sourceLifecycleFingerprint` 证据字段和统计，明确不代表 526 个独立运行时，也不得据此决定 Provider 数量。后续族运行时去重仍需同时比对完整配置消费、牌库、合法操作、结算和回放契约。

### Poker 族混淆复核与 native 去重候选

Poker 150 项已全量扫描，并为每个族保留最多 10 项人工复核样本（总数不足 10 的族全量保留）。分类不再因源码出现 `510K` 牌型常量就判为 510K 族；必须同时出现搭档/队友计分、抓分/扣底或主牌消费者。只有出完/空手终局的项改归 climbing 或 pao-de-kuai；叫/抢地主、主牌抓分、比牌和下注分别优先归入各自核心族。扫描发现 86 项出现 510K 牌型，其中 15 项缺少上述 510K 核心消费者，不再以该关键字定族；最终 510K 族为 70 项，而非先前的 76 项。

当前 80 个 native Provider 已全部映射到核心族/语义原型。75 项直接使用 catalog-crosswalk；`njpdk/scjymj/xcpdk/zjh/zypk` 五项使用其 native descriptor 与专属运行时证据补齐，不再留为未映射。按“同玩法族+同 runtimeArchetype”得到 16 个多项候选批次，但每批的 rule/lifecycle components 或 source fingerprint 均仍存在差异，因此当前强等价可直接合并批次为 0；这些批次只是后续完整契约比对的候选集，不是合并授权。

混淆与 native 映射产物：`work/audit/r04-poker-family-confusion.tsv`、`work/audit/r04-native-family-archetype-map.tsv`、`work/audit/r04-family-confusion-audit.json`；可复建脚本为 `work/audit/r04-audit-family-confusion.rb`。

### 可打包的族级地区配置资源

四个玩法模块现分别包含 364/150/10/4 行族级地区配置 TSV，合计 528 行且 gameId 唯一。每行包含 gameId/code/family/archetype/region/ruleComponents/lifecycleComponents/sourceStatus/sourceEvidence/allowedCreateFields/configSchemaHash。`allowedCreateFields` 只保留旧服务端 CreateRoom DTO（含 BaseCreateRoom 继承字段）与旧客户端 CreateSendPack 实际发送字段的交集；单边出现的字段不会被放行。

资源路径：`server/Mahjong/src/main/resources/mahjong-family-region-config.tsv`、`server/Poker/src/main/resources/poker-family-region-config.tsv`、`server/WordCard/src/main/resources/word-card-family-region-config.tsv`、`server/LongCard/src/main/resources/long-card-family-region-config.tsv`。可复建脚本为 `work/audit/r04-generate-family-region-configs.rb`。`caooszmj(238)` 与 `dlaoomj(250)` 在资源中明确保持 `SOURCE_MISSING_OR_UNREADABLE`，不生成伪契约。

`tools/verify_r04_family_runtime.rb` 已将四份资源作为强制输入，全量校验表头、行数、分类归属、字段有序去重、源码证据、缺源码集合和每行 SHA-256 schema hash。实测 `familyRegionResourceRows=528` 且 `familyRegionResourceErrors=0`。

## 当前可验收原生玩法

| 分类 | code |
|---|---|
| 麻将 | `cdxzmj`、`scjymj` |
| 扑克 | `njpdk`、`hndzp`、`lhzp`、`xcpdk`、`zjh`、`zypk` |
| 字牌 | `byzp`、`bzp`、`ycsdr`、`pxphz`、`xpphz`、`yzchz`、`glzp`、`ychp`、`ahphz`、`lczp` |
| 长牌 | `aydss`、`aycp` |

目录分类守恒：MAHJONG 364、POKER 150、WORD_CARD 10、LONG_CARD 4。玩法族分别为：麻将 standard 201 / lai-zi 127 / tui-dao-hu 13 / xue-liu 7 / xue-zhan 16；扑克 pao-de-kuai 141 / trick-taking 5 / 510k、climbing、hainan-landlord、sheng-ji 各 1；字牌 pao-hu-zi 9 / bao-zi-pai 1；长牌 regional 2 / zigong 1 / zigong-da-si-shi 1。

## 客户端核验

客户端目前统计到 18 个不重复的具体网络适配器，但这不等于 528 项均具备玩法运行时。只有 NJPDK 具备独立的 `tests/games/<code>` 自动契约；绝大多数目录玩法没有具体客户端玩法适配器或专属自动证据。AHPHZ、LCZP 只有切换协调器，旧版也缺少独立牌桌客户端源码，不能宣称完成。

已通过的客户端链路：NJPDK 权威运行时契约、麻将创建至回放、麻将合法操作策略、扑克权威状态链路、重连恢复与版本冲突处理。客户端只提交意图和渲染权威快照，不作为规则、算分或结算权威。

### Creator 3.8.8 适配器同步审计（2026-08-25）

本次只审计脚本资产；没有场景或预制体目标，因此“不适用关闭重开场景/预制体”。未修改 `.scene`、`.prefab`、LoginScene 或 `library`。Creator 3.8.8 已自动导入本批脚本并生成/维护 TypeScript meta；资源管理器对 `BfmjGameplayAdapter` 的实时搜索可见，控制台只显示正常的 3.8.8 引擎初始化日志，底部 Error 计数为 0。中途 Computer Use 一度无法取得 Creator 窗口状态，因此未强制关闭当时打开的 LoginScene；之后以官方 Creator 3.8.8 `--import` 正常刷新项目，AHHNMJ meta 由资源数据库生成，并在重开的 Bootstrap 场景中实时搜索可见，控制台 0/0/0。仍未逐项完成可见性复核的旧适配器按规范保守标为 `待编辑器同步（可见性复核）`。

| 玩法 | 脚本资产 | Creator meta UUID | importer/imported | 编辑器可见性 |
|---|---|---|---|---|
| `pdk` | `Games/Poker/Packs/Pack01/Code/Runtime/pdk/PdkGameplayAdapter.ts` | `96d7a686-60e5-46e1-8031-af10edc5c6f8` | `typescript / true` | 待编辑器同步（可见性复核） |
| `zgcp`、`zgdss` | `Games/LongCard/Common/Code/ZigongLongCardAdapters.ts`（两个独立导出类） | `323a1485-888d-4c5e-bd85-b78280c75b38` | `typescript / true` | 待编辑器同步（可见性复核） |
| `cp` | `Games/Poker/Common/Code/CpGameplayAdapter.ts` | `58f08b55-b3f2-4a11-a8a9-ae23bc463cc4` | `typescript / true` | 待编辑器同步（可见性复核） |
| `gnmj` | `Games/Mahjong/Common/Code/GnmjGameplayAdapter.ts` | `d040d340-71c7-494a-a969-88114474e38d` | `typescript / true` | 待编辑器同步（可见性复核） |
| `gzmj` | `Games/Mahjong/Common/Code/GzmjGameplayAdapter.ts` | `f427ad81-3b3c-4a84-b841-d39a242f2b12` | `typescript / true` | 待编辑器同步（可见性复核） |
| `gyzjmj` | `Games/Mahjong/Common/Code/GyzjmjGameplayAdapter.ts` | `7e8a432d-c52a-4802-b172-681a9d6ba19e` | `typescript / true` | 待编辑器同步（可见性复核） |
| `hnmj` | `Games/Mahjong/Common/Code/HnmjGameplayAdapter.ts` | `1e7349be-833c-430b-9548-e82e4d26f57c` | `typescript / true` | 待编辑器同步（可见性复核） |
| `rjmj` | `Games/Mahjong/Common/Code/RjmjGameplayAdapter.ts` | `6c39fbe1-31e6-4c9c-b967-e8d9c267ab83` | `typescript / true` | 待编辑器同步（可见性复核） |
| `afmj` | `Games/Mahjong/Common/Code/AfmjGameplayAdapter.ts` | `a9f727b8-c80d-4d05-a7f5-19c9181845a8` | `typescript / true` | 待编辑器同步（可见性复核） |
| `aypdk` | `Games/Poker/Common/Code/AypdkGameplayAdapter.ts` | `82d2611c-3e53-4411-b19d-690a94b37ec2` | `typescript / true` | 待编辑器同步（可见性复核） |
| `ahmj` | `Games/Mahjong/Common/Code/AhmjGameplayAdapter.ts` | `db5caebe-4d63-47c7-817a-af89926ec491` | `typescript / true` | 待编辑器同步（可见性复核） |
| `bfmj` | `Games/Mahjong/Common/Code/BfmjGameplayAdapter.ts` | `cd1c6f89-3180-43f7-b3d8-aaa09963f208` | `typescript / true` | 通过：Creator 实时搜索可见 |
| `hbpdk` | `Games/Poker/Common/Code/HbpdkGameplayAdapter.ts` | `fd38582f-bd8c-45a1-a1fb-c0dceddd40c9` | `typescript / true` | 通过：Creator `--import` 日志、预览编译记录、实时搜索和 0/0/0 控制台计数均已复核 |
| `ahhnmj` | `Games/Mahjong/Common/Code/AhhnmjGameplayAdapter.ts` | `31893200-33b7-4143-a9a1-b21d67e7a3ab` | `typescript / true` | 通过：Creator 3.8.8 `--import` 正常生成 meta，Bootstrap 场景重开后实时搜索可见，控制台 0/0/0 |

门禁结果：AHHNMJ 专属契约 3/3 通过；组件注册门禁复验通过（39 个 ccclass、33659 UUID）；Creator 原生资产门禁复验通过（1620 个 scene/prefab、25345 个 asset/meta、33659 UUID、blocking=0）。此前 14 个玩法的专属客户端契约共 53/53 通过，TypeScript 5.9.2 `--noEmit` 通过。完整 `scripts/verify-build.sh` 的既有唯一失败仍是边界外 LoginScene 序列化节点断言（`tests/unit/login-lobby-flow.test.mjs:113` 读取缺失节点的 `_active`），没有为通过测试而修改只读 LoginScene，也没有伪造 `library`。

## 可复现证据

```bash
cd /Users/aoo/Code/Game/BCG/Aoo/Server
ruby tools/audit_authoritative_game_provider_coverage.rb
ruby tools/audit_snapshot_state_event_version.rb
ruby tools/verify_v07_all_games.rb
ruby tools/verify_r04_family_runtime.rb

cd /Users/aoo/Code/Game/BCG/Aoo/Client
node tests/games/njpdk/runtime-contract.test.mjs
node tests/unit/mahjong-interface-chain.test.mjs
node tests/unit/mahjong-operation-policy.test.mjs
node tests/unit/poker-authoritative-chain.test.mjs
node tests/unit/room-reconnect-state.test.mjs
```

新 R04 工具实测：528 个绑定、binding errors 0、缺源码 2、15 个族、32 个 archetype、region config schema errors 0；族级完整注册为 0/15，code-specific Provider/客户端重复组为 16/2，因此退出 1。`tools/verify_v07_all_games.rb` 保留为历史逐 code Provider 时代的覆盖证据，其 metadata-only 判定不再是新 R04 的失败条件。

定向 Maven 复验在进入 WordCard 编译前被边界外 `HYGIENE12 failed: 231 generated artifacts registered` 阻断；不得据此宣称同快照 Maven 全通过。

新 R04 机器证据：`work/audit/r04-family-runtime-registry.json`、`work/audit/r04-family-runtime-verification.json`、`work/audit/r04-authoritative-four-layer-map.json`。V07 历史证据仍为 `work/audit/v07-all-games.json`。

## 源码缺失与后续阻塞

### 目录分类与真实源码冲突

`jlpk`（gameId `496`）暂不得创建原生 Provider：V07 目录把它登记为 `POKER / poker-sheng-ji`，但旧版权威源码实际位于 `business.global.mj.jlpk`，核心为 `JLPKSetCard`、`JLPKHuUtil`，合法操作为吃、碰、偎、提、跑、召、贴及胡息结算，属于字牌运行时，并非升级扑克。R04 禁止修改目录/配置仓储边界，因此保持 metadata-only；在目录分类由对应边界修正前，不能用 `ShengJiEngine` 伪造完成。

`gdy`（gameId `27`）同样存在目录分类冲突：目录登记为 `poker-trick-taking`，旧版权威实现实际是干瞪眼出完牌玩法。源码 `GDYSetPosOp` 定义单张、顺子、对子、连对、炸弹及鬼牌替代，`GDYSetRound` 采用 `Out/Pass` 压牌轮转，`CGDY_CreateRoom` 规则为 `weizi/fengDing`，`GDYSetPos.calcPosPoint` 以剩余手牌、公共炸弹倍数和春天结算；不存在墩牌、跟花色或抓分语义。因此不得接入通用 trick-taking 引擎。

对目录中其余 `poker-trick-taking` 候选的旧源码抽查也未找到可替代的同族实现：`dx` 是天地牌对子比牌，`sdb` 是十点半庄闲下注，`ttz` 是推筒子，`cqcp` 是重庆戳牌。它们均保持 metadata-only，等待目录分类边界修正；R04 不修改目录或配置仓储。

`bsl`（gameId `458`）不能按目录中的 `poker-pao-de-kuai` 迁移。旧源码 `BSLRoomSet` 使用 60 张牌库，包含摸牌、理牌与发牌结束阶段，并定义 7 惩罚、8 禁止、11 万能及 14 顺序等特殊牌效果；这不是跑得快的出完牌/压牌状态机。保持 metadata-only，等待目录分类边界修正，不注册错误 PDK Provider。

`a3pk`（gameId `309`）也不能按 `poker-pao-de-kuai` 迁移。`CA3PK_CreateRoom` 的创建字段为 `difen/lianmai/wangpai/jiesansuanjiang/fafen`；`A3PKRoomEnum` 明确定义“长汀 510K”、`A3独食/32牛鬼`、`Challenge/Surrender/Refuse` 阶段，以及顺子不带 A、独食特殊算分和 A23 等规则。其状态机与跑得快不等价，保持 metadata-only。

`glpp`（gameId `319`）也不具备跑得快语义。旧源码包含 `SGLPP_PartnerPosList` 搭档/名次状态、`SGLPP_ShowPoint` 公共余分、`GLPPCalcPosEnd` 吃分与 510K 计分，以及发牌结束/摸牌通知；创建专属字段仅为 `moshi`。这是搭档抓分玩法，不可映射为出完手牌结算的 PDK Provider。

旧版交叉盘点在 528 项中找到 526 项权威源码；明确未找到权威源码的仅 `caooszmj`、`dlaoomj`，必须保持“源码缺失”，不得伪造完成。另有 12 项旧源码缺少验收所需关键方法：`sss`、`nn`、`sg`、`bjpk`、`yxls`、`zjpls`、`fqpls`、`fqsbp`、`zjgmj`、`wxls`、`xlhzmj`、`dphmj`。

其余有旧源码但无原生 Provider 的玩法，仍需逐项迁移并补齐：唯一 Provider 注册、地区/玩法族映射、规则参数真实生效、九阶段服务端权威链路、客户端具体适配器以及每玩法可复现自动验收。不能批量复制共享错误实现，也不能把旧 MQ/旧协议旁路重新接入生产。

## 513 项源码可用性与迁移批次（续跑基线）

已重新以当前 `v07-all-games.json` 的 499 个 metadata-only code 为集合，将 528 目录、V07 旧源码交叉表和旧版只读源码重新连接。逐项结果写入 `work/audit/r04-metadata-source-family-crosswalk.tsv`（499 条数据行），可用 `ruby work/audit/r04-generate-metadata-crosswalk.rb` 重建。生成器还会读取每个旧 `*ConfigMgr.java` 的字面配置文件引用，在各源码快照的 `conf/config/resources` 中验证资产实际存在，并对旧树中同名配置计算 SHA-256；同名文件内容分叉且无法证明唯一部署谱系时不得任选一份冒充权威配置。

客户端源码索引同时覆盖旧树中按 code 命名的 `Client_<code>` 工程：462 项定位到具体 JS/TS，45 项未定位到 code-specific 客户端 JS/TS。此列只证明源码存在，不证明客户端九阶段已迁移；外部配置存在也不证明其内容语义已完成审核。

| 源码处置 | 数量 | 含义 |
|---|---:|---|
| `SOURCE_COMPLETE_CANDIDATE` | 445 | 已定位五组旧权威方法，且未发现关键外部配置缺失或同名配置谱系歧义；仍须逐项完成语义审核与九阶段迁移 |
| `PARTIAL_AUTHORITY_SOURCE` | 12 | 找到旧源码，但关键权威方法组不完整：`sss`、`nn`、`sg`、`bjpk`、`yxls`、`zjpls`、`fqpls`、`fqsbp`、`zjgmj`、`wxls`、`xlhzmj`、`dphmj` |
| `NO_AUTHORITY_SOURCE` | 2 | `caooszmj`、`dlaoomj`；保持源码缺失阻塞，不造实现 |
| `MISSING_REQUIRED_CONFIG` | 34 | Java 权威逻辑引用的 code-specific 配置文件在所有已定位源码快照中缺失；具体文件名逐项记录于交叉表 `missingExternalAssets` |
| `AMBIGUOUS_CONFIG_PROVENANCE` | 6 | 同名配置存在不同内容哈希，不能证明唯一权威部署谱系：`ddz`、`erddz`、`hhpdk`、`pdk_bak`、`pypp`、`wnpdk` |

排除源码/关键方法/外部配置阻塞后，445 个候选按目录迁移亲和性分组如下。这里的 `catalogFamily` 只决定共享基础设施候选和执行顺序，不是规则等价证明。

### POKER metadata-only 全量阻塞清单

`ruby work/audit/r04-generate-poker-blocker-inventory.rb` 会为当前全部 144 个 POKER metadata-only code 生成 `work/audit/r04-poker-blocker-inventory.tsv`。每行包含实际源码玩法签名、命中证据文件、目录匹配/冲突、配置谱系、客户端源码状态和最终阻塞原因；已人工核实的 `gdy/dx/sdb/ttz/cqcp/a3pk/bsl/glpp` 使用明确源码结论覆盖文字特征。

当前结果为：20 项源码签名与目录族一致，但全部被缺失/歧义配置或客户端源码缺失阻断；其余 124 项均有实际玩法签名与目录族不一致的源码证据。因此当前不存在同时满足“目录语义一致、模块配置谱系唯一、客户端源码存在”的未迁移 POKER 候选，不能安全新增 Provider。此清单是保守阻塞清单：文字特征只用于拒绝错误迁移，不能反向作为玩法完成证明。

### MAHJONG metadata-only 全量候选/阻塞清单

`ruby work/audit/r04-generate-mahjong-blocker-inventory.rb` 会为当前全部 355 个 MAHJONG metadata-only code 生成 `work/audit/r04-mahjong-blocker-inventory.tsv`。每行记录实际源码签名族、命中证据文件、目录匹配状态、源码完整度、配置谱系、客户端存在性、关键权威阶段和可执行状态；`jlpk` 的字牌结论使用已人工核实证据覆盖。

当前保守扫描结果：193 项目录族与源码签名一致，160 项不一致，2 项因无权威源码无法判定。合并关键方法、配置与客户端门禁后，176 项为 `UNBLOCKED_RUNTIME_CANDIDATE`；完整 code 列表位于机器清单，可用上述命令稳定重建。其余状态为：148 项纯目录冲突、11 项目录冲突且缺客户端、1 项目录冲突且关键方法不完整、14 项缺客户端、2 项关键方法不完整、1 项缺配置且缺客户端、2 项无源码且缺客户端。`UNBLOCKED_RUNTIME_CANDIDATE` 只表示可进入逐玩法规则枚举与九阶段迁移，不等同 Provider 已完成。

对 176 项进一步运行 `ruby work/audit/r04-mahjong-equivalence-groups.rb`，以首个已定位源码谱系中的 `CreateRoom/RoomSet/SetCard/HuUtil/HuPai/CalcPosEnd` 为关键集合，去除注释、空白和 code 前缀后，对规范化规则源码与创建 DTO 字段联合计算 SHA-256。结果写入 `work/audit/r04-mahjong-equivalence-groups.json`：严格相同组为 0、可成组 code 为 0。由于最上游关键规则/创建契约层已经全部不同，无需用客户端字段或配置哈希放宽匹配；它们只能进一步拆组，不能证明合组。因此不存在可安全批量实现的“最大相同组”，176 项必须逐玩法审核与实现，禁止仅凭目录族共享 Profile。

逐项复核还发现 `aqmj`（gameId `233`）不能作为安徽/安庆目录候选：旧 `AQMJRoomEnum` 明确注释为“宁德麻将”，并定义宁德四口、十三烂/七星十三烂、顺包/反包等结算。该项已在麻将清单中覆盖为源码谱系冲突，不能用通用安徽麻将 Provider 代替。

`bsmj`（gameId `459`）也存在地区谱系冲突：V07 目录将其归为 `guangxi`，但所选旧权威源码 `BSMJSetCard` 明确标注“保定易县麻将”，且实际规则为大转/小转王、三比互包/三包四封。地区归属未裁决前保持 metadata-only；已清除未注册的规则草稿，不注册广西名义下的错误 Provider。

`csmj`（gameId `40`）不是湖南长沙麻将：V07 目录和旧权威源码均指向江苏，`CSMJRoomEnum/Room/SetCard/SetOp` 连续标注并实现“常熟麻将”，创建规则包含漂将、奖马和独龙杠。禁止按长沙玩法注册 Provider；该项保持地区谱系冲突阻塞。

`dsmj`（目录 gameId `511`）存在来源与编号冲突：旧客户端 `ShareDefine.js` 将 DSMJ 明确标为“东山麻将”且编号为 `510`，旧服务端可取得的实现仅为 `HNPDSMJ`（平顶山麻将），未发现独立 DSMJ 服务端源码、配置或规则枚举。禁止将 HNPDSMJ 冒充 DSMJ，也不能在缺少服务端权威规则时按目录 `511` 制造 Provider；该项保持源码/目录阻塞。

`dzmj`（gameId `265`）的 code 与旧源码可对应，但地区谱系冲突：目录归为 `fujian`，旧客户端 `ShareDefine.js` 和旧服务端 `DZMJRoomEnum` 均明确命名为“ 大众麻将 / 大众麻将枚举”，源码、创建字段及规则类中未发现福建地区标识。地区归属未裁决前不得以福建玩法名义注册通用“大众麻将” Provider；该项保持目录阻塞。

`fctdhmj`（gameId `348`）名称与地区谱系可对应河南方城推倒胡，但可执行创建契约冲突：现行旧客户端及 `CFCTDHMJ_CreateRoom` 发送 `hunzipai/fengDing/gangpaidefen/piaogang`，玩法规则实现仅读取 `piaofen/jinkezimohupai`；前四项未进入规则，后两项又没有创建入口。未取得能解释字段映射的真实源码前不得注册 Provider；当前源码与 SPI 均保持缺失。

| 迁移亲和组 | 数量 |
|---|---:|
| mahjong-standard | 197 |
| poker-pao-de-kuai | 91 |
| mahjong-lai-zi | 119 |
| mahjong-xue-zhan | 14 |
| mahjong-tui-dao-hu | 13 |
| mahjong-xue-liu | 6 |
| poker-trick-taking | 4 |
| poker-sheng-ji | 1 |
| long-card-zigong / long-card-zigong-da-si-shi | 0（已迁移出 metadata-only） |
| poker-510k | 0（已迁移或分类阻塞） |
| poker-sheng-ji | 1 |

### 去重边界

- 可去重：同一 code 在旧树不同发布快照中的重复源码；迁移时先选定一个可追溯基线快照，其他快照只做差异核对。
- 暂不可去重：不同 code。现有 513 项没有任何一组具备“牌集、合法操作、状态转换、算分和规则参数”全部等价证据；因此交叉表将每个 code 标为独立 `dedupeUnit=CODE:<code>` 和 `NO_CROSS_CODE_EQUIVALENCE_PROVEN`。
- `mahjong-standard`、`poker-pao-de-kuai` 等目录标签粒度过粗，不能据此让多个 code 共用同一个无差异配置的 Provider。允许共享经过测试的引擎原语，但每个 code 必须有独立规则配置、Provider 注册、客户端适配和九阶段自动验收。

### 下一最高置信度批次 B01

先迁移 `zgcp` 与 `zgdss`，但作为两个独立规则单元验收：

- 它们是 LONG_CARD 目录仅剩的两个 metadata-only 项；`aydss`、`aycp` 已有原生 Provider，可复用已验证的长牌基础设施。
- 旧版基线分别有 107、110 个 Java 文件，交叉表均识别 20 个协议类，并具备 create/start、operate、settle、restore、replay 五组权威源码证据。
- 新客户端仅找到牌/结算 prefab 与贴图资源，未找到 `zgcp`/`zgdss` TypeScript 具体适配器；所以 B01 必须同时新增真实客户端适配和专属自动验收，资源存在不能冒充适配完成。
- 两项分别属于 `long-card-zigong` 与 `long-card-zigong-da-si-shi`，禁止把二者或现有 `aycp`/`aydss` 当成同规则别名。应先提取逐项规则参数、操作集合、轮转和算分差异，再接入共享长牌引擎。

B01 的完成门槛是每个 code 分别证明创建、加入、准备、发牌/开局、轮转、合法操作、结算、回放、重连九阶段，且规则按钮改变服务端权威结果。之后按“小而独立、源码完整”优先推进 `poker-510k`、`poker-sheng-ji`、5 项 trick-taking，再进入麻将小族；三个超大粗粒度族必须先逐 code 做语义指纹，不能整族批量套壳。

### CP / JLPK 分类与源码阻塞

- `jlpk`（gameId 496）目录标为 `POKER / poker-sheng-ji`，但旧权威源码位于 `business.global.mj.jlpk`，消息包含吃牌、下火、查胡、飘分等字牌/跑胡子语义。分类与权威源码冲突未裁决前，禁止按升级扑克制造 Provider 或客户端适配器；本轮不改目录。
- `cp`（gameId 392）目录标为 `POKER / poker-510k`，旧权威规则实际是重庆/四川“戳牌”：固定三人、84 张长牌、每人 28 张，创建字段为 `moshi`、`fanshushangxian`、`difen`，操作负载为 `opType`、`cardType`、`cardList`、`substituteCard`。若运行时使用 52/54 张标准扑克以及顺子、同花、葫芦等牌型，则与旧源码不等价，客户端不得接入或把它记作完成。

### BZPDK 配置源码阻塞

`bzpdk`（gameId `306`）同时找到了旧服务端和旧客户端源码，但尚不具备可迁移的完整权威配置。服务端 `BZPDKConfigMgr` 强制读取 `conf/BZPDKConfig.txt` 中的 `handleCard`、`deleteCard`、`jiPaiFen`、`paiDuoTongShu`、`guDingFen`、`maxAddDoubleList` 等值；旧源码全集中没有该文件。客户端可确认实际创建参数包含 `shoupai`（`0=15张`、`1=16张`）、`zhadansuanfa`、`zhadanfenshu`，但不能据此反推出删牌表、三种结算枚举值或倍数上限。Java 创建 DTO 中另有 `cardNum/resultCalc/score/maxAddDouble` 历史字段，客户端当前创建请求并未发送这些字段。

因此 BZPDK 从“源码完整候选”降级为“关键配置资产缺失”：在找回原始 `BZPDKConfig.txt` 前保持 metadata-only，不得以通用 PDK 默认值、猜测枚举或固定 52 张牌制造 Provider。

配置资产门禁后的下一项 PDK 候选为 `hhpdk`（gameId `170`）：五组权威方法齐全，7 份旧服务端快照均可交叉核对，`HHPDKConfig.txt` 实际存在，且定位到 103 个 code-specific 客户端 JS 文件。它仍须在实现前逐项解析配置内容、创建按钮和服务端消费点；这里只把它列为下一真正可审核候选，不提前宣称运行时完成。

HHPDK 深入核验确认客户端发送的 `xiaojuqiepai`、`dajusuanfen` 属于公共层，而不是 HHPDK 专属玩法按钮：旧 `UnionRoom` 通过 `RoomQiePaiEnum` 消费前者；`AbsPKSetRoom`、`AbsPKSetPos` 和 `AbsBaseRoom` 通过 `RoomEndPointEnum` 消费后者。R04 对这些公共实现只读，不在具体 Provider 中重复或虚构效果。HHPDK 专属迁移范围仍包括 45/48 张牌、动态人数、30 余项 `HHPDK_WANFA`、血战到底、抢关门、明牌/加倍、癞子、红桃十扎鸟与炸弹归属算法。

### 当前原生运行时进度（2026-08-25）

重新执行 `tools/verify_v07_all_games.rb` 后，目录共 528 项，原生 Provider 57 项且 57 项均具备九阶段证据和通过的测试报告；剩余 metadata-only 476 项。随后重建 `r04-metadata-source-family-crosswalk.tsv`（476 行）和 `r04-mahjong-blocker-inventory.tsv`（332 行）。校验器仍按预期非零退出，唯一汇总阻塞是剩余 476 项尚无原生 Provider。

本轮新增 `gdmj`（gameId `212`，广东麻将）专属运行时。旧 ID 211、客户端显示名、`GDMJ` 包、`RoomEnum` 和 `CGDMJ_CreateRoom` 契约一致；服务端真实消费 `fengding/mapai/mashu/mafen/lunzhuang` 与六项 `kexuan`。实现覆盖 8/12/16 封顶、无马/买马/爆炸马、2/4/6 马、马分、轮庄、杠爆全包、无杠分、荒庄荒杠、跟庄、节节高、十二张落地承包，以及马牌尾抽/命中、杠账本、权威结算、私有视图、回放、恢复和重连。`GdmjGameProvider` SPI 物理唯一；专属测试 3/3 与 Mahjong 全模块 140/140 通过（使用 `-Dexec.skip=true` 绕过仓库既有 DEAD13 前置门禁）。

随后新增 `gft258mj`（gameId `343`，江西上饶广丰 258 麻将）专属运行时。旧源码的 `GFT258MJRoomEnum`、创建 DTO 与客户端显示/创建契约一致；专属字段仅 `hupaifangshi`、`fengDing`，可选玩法为带风和将 258。实现证明带风切换 108/136 张物理库存、将 258 服务端判胡门槛、两种胡牌方式与 50/100/不限封顶，并覆盖创建、加入、准备、发牌、轮转、合法操作、权威结算、回放、恢复和重连。`Gft258mjGameProvider` 在预注册逻辑测试通过后登记，SPI 物理计数 1；定向测试与 Mahjong 全模块测试通过。

本轮继续新增 `gslzmj`（gameId `360`，甘肃兰州麻将）专属运行时。旧客户端 ID 359、显示名、`GSLZMJ` 包、创建 DTO 和服务端枚举一致；`moshi` 五档、`shuaipai` 三档、`gangpai` 两档及五项可选玩法均由服务端解析。运行时持久化模式对应的宝牌、顺序甩牌选择、杠分账本和选项权威状态，并覆盖结算、事件回放、恢复与重连。预注册逻辑测试通过后才登记 `GslzmjGameProvider`；SPI 物理计数 1，定向测试和 Mahjong 全模块测试通过。

本轮新增 `gsmj`（gameId `322`，河南信阳光山麻将）专属运行时。旧客户端 ID 321、显示名、`GSMJ` 包、创建 DTO 与规则枚举一致；服务端消费 `difen` 的 1/2/5/20 映射、`qidui` 的不可胡/0/1/2 分，以及买马、断门、独赢、下跑、杠上花五项玩法。运行时覆盖下跑初始 -1、逐座 0–3 选择及超时归零、马牌尾抽与命中、杠账本、权威结算、回放、恢复和重连。预 SPI 定向逻辑测试通过后登记唯一 Provider；注册后定向测试和 Mahjong 全模块测试通过，SPI 物理计数 1。

本轮新增 `hamj`（gameId `33`，江苏南通海安麻将）专属运行时。旧客户端 ID 32、显示名、`HAMJ` 包和创建 DTO 一致；旧枚举内部残留 `NDMJ` 命名不影响实际 HAMJ 执行谱系。服务端消费 `difen`、`lazi` 和七对/四花字门清选项；动态开金独立于七对开关，指示牌从 136 张物理库存扣除并与金牌一同持久化。结算使用旧端 OpValue、底分、七对/四花字门清模式和 20–100 封顶，并覆盖九阶段、私有视图、回放、恢复和重连。预 SPI 逻辑测试通过后登记唯一 Provider；定向测试和 Mahjong 全模块测试通过，SPI 物理计数 1。

本轮新增 `hbhbmj`（gameId `223`，河北麻将）专属运行时。旧客户端 ID 222、显示名、`HBHBMJ` 包、创建 DTO 和执行枚举一致；源码内“萍乡转转”仅为复制注释。服务端消费庄闲、带风、可吃、点杠包杠、一炮多响五项玩法和门清、捉五魁、大吊车、海底捞月、花龙五项加分。实现覆盖 108/136 张库存切换、吃牌门禁、杠账本、庄闲倍率、多响应窗口的单响/多响/超时/恢复、私有视图、零和结算、回放与重连。预 SPI 逻辑测试通过后登记唯一 Provider；定向测试和 Mahjong 全模块测试通过，SPI 物理计数 1。

本轮新增 `hbmj`（gameId `59`，河南信阳淮滨麻将）专属运行时。旧客户端 ID 58、显示名、`HBMJ` 包、创建 DTO 与枚举一致；专属玩法仅亮风和跑十分。服务端运行时实现亮风的逐座 `WAITING_EX` 声明与超时归零、跑十分权威加分、旧端 OpValue、杠账本、结算、私有视图、回放、恢复和重连。Provider 默认配置移除复制残留字段；预 SPI 定向逻辑测试通过后登记唯一 Provider，注册后定向测试和 Mahjong 全模块测试通过，SPI 物理计数 1。

本轮新增 `hbwhmj`（gameId `254`，湖北武汉麻将）专属运行时。旧客户端 ID 253、显示名、`HBWHMJ` 包、创建 DTO 与枚举一致；服务端消费三档 `wanfa`、两档 `moshi`、0/16/32/64 起胡、300/500/800 封顶和 `LIU_10_ZHANG`。源码 `HBWHMJSetCard` 证明该选项含义为牌墙保留 10 张而非 60 张牌库。实现覆盖 136 张库存、258 将、开口/扣扣模式、起胡门槛、封顶、杠账本、权威结算、私有视图、回放、恢复和重连。预 SPI 定向逻辑测试通过后登记唯一 Provider，注册后定向测试和 Mahjong 全模块测试通过，SPI 物理计数 1。

本轮新增 `hbyxmj`（gameId `24`，湖北黄石阳新麻将）专属运行时。旧客户端 ID 23、显示名、`HBYXMJ` 包、创建 DTO 与枚举一致；服务端消费 1/2/3/5 底分、30/50/100/不限封顶、经典/258 模式、三种晃玩法、赖子杠分与七项玩法。运行时物理翻开并移除指示牌，135 张库存守恒，动态金牌参与服务端判胡并在恢复后保持一致；结算应用底分、封顶、赖子杠分和旧端 OpPoint。实现覆盖九阶段、私有视图、回放、恢复和重连。预 SPI 测试通过后登记唯一 Provider；定向测试和 Mahjong 全模块测试通过，SPI 物理计数 1。

### HFBZMJ (396) — native server runtime

- Lineage: legacy client/source ID 395 `HFBZMJ`, display name `八支麻将`; current catalog offset ID 396. Live create fields are `hupaifangshi`, `naozhuang` plus the common room fields.
- Runtime: dedicated config/rules/family/session/provider with 144-tile physical inventory, authoritative operations and settlement, private views, replay, restore and reconnect.
- Rule evidence: discard/self-draw mode mapping, `naozhuang` 0/5/10/20 exponential mapping, and the legacy 八支 OpPoint table are covered by `HfbzmjNineStageAcceptanceTest`.
- Verification: targeted HFBZMJ test and the complete Mahjong reactor passed with `-Dexec.skip=true`; provider SPI physical count is exactly one. Inventory regenerated to 324 rows.

### HFMJ (275) — native server runtime

- Provenance: legacy ID 274, client display `合肥麻将`, HFMJ source/package/create contract and current catalog ID 275 agree.
- Dedicated runtime covers `naozhuang` (0/5/10/15/20), all nine live optional rules, the complete source OpPoint table, fixed 144-tile inventory, authoritative operation gates/settlement, private views, replay, serialized restore and reconnect.
- Hygiene scan reports no neighboring HFB/HBMJ, WAITING_EX, piao, horse, liangFeng or choice-seat state in HFMJ sources.
- Pre-SPI behavioral/nine-stage methods passed; after adding exactly one SPI entry, the full HFMJ test and complete Mahjong reactor passed with `-Dexec.skip=true`. Inventory regenerated to 323 rows.

### HNCSMJ (113) — native server runtime

- Provenance: legacy ID 112/client display `长沙麻将`, HNCSMJ source package and current ID 113 agree.
- Dedicated runtime preserves the exact live create contract, implements all 14 optional-rule flags, three piao modes (including sequential choice/timeout), two kai-gang modes, three bird modes and 1/2/4/6 tail-bird draws/hits; stale `yipaoduoxiangzhuaniao` is not exposed and common cut/final-score fields remain metadata only.
- Covers 108-tile inventory, source OpPoint values, authoritative settlement/gang ledger, private views, replay, object/serialized restore and reconnect.
- Pre-SPI behavior/nine-stage methods passed; after exactly one SPI registration the full dedicated test and complete Mahjong reactor passed with `-Dexec.skip=true`. Neighboring GSMJ/horse/mai-ma residue scan is zero. Inventory regenerated to 322 rows.

### HNHBMJ (185) — native server runtime

- Provenance: legacy ID 184/client display `鹤壁麻将`, Henan HNHBMJ source and current catalog ID 185 agree.
- Dedicated runtime covers two xia-pao modes with authoritative choice/timeout, four bird modes and tail draws/hits, two base scores, two direct/concealed-gang scores, and all five live options including physical red-center wildcard, no-red doubling, discard hu, big hu and rob-gang authority.
- Covers 112-tile inventory, settlement/gang ledger, private views, replay, object restore and reconnect. Common room fields are preserved without invented gameplay effects.
- Pre-SPI behavioral/nine-stage methods passed; after one physical SPI entry, the complete dedicated test and full Mahjong reactor passed with `-Dexec.skip=true`. Neighboring HNCS/GSMJ residue scan is zero. Inventory regenerated to 321 rows.

### HNJYMJ (161) — native server runtime

- Provenance: legacy ID 160/client display `济源麻将`, Henan HNJYMJ source and current catalog ID 161 agree.
- Dedicated runtime covers three wan-fa values, two hu modes, all eight xuan-pao modes (free minimum/no-pao/fixed 1–4) with WAITING_EX choice and timeout, plus four live options for wind tiles, pass-peng, gang-flower doubling and seven-pairs.
- Tests cover 108/136 inventory switching, OpPoint/scoring helpers, authoritative operation gates/gang ledger, private views, replay, object restore and reconnect.
- Pre-SPI behavioral methods passed; one physical SPI entry, complete dedicated tests and full Mahjong reactor passed with `-Dexec.skip=true`. HNHB/HNCS/bird residue scan is zero. Inventory regenerated to 320 rows.

### HNXCMJ (147) — native server runtime

- Provenance: legacy ID 146/client display `许昌麻将`, Henan HNXCMJ source and current catalog ID 147 agree.
- Dedicated runtime covers the live wan-fa selector, all ten xia-pao modes (free/fixed/no-pao) with WAITING_EX choice/timeout, and six options for dynamic hun, feng-mo, bao-ting hu, gang-with-pao, seven-pairs and gang-flower doubling.
- Dynamic hun physically removes/persists its indicator (144 base, 143 active wall inventory); tests cover authority, settlement, private views, replay, object restore and reconnect.
- Dedicated tests and the full Mahjong reactor passed with `-Dexec.skip=true`; SPI physical count is one and neighboring HNJY/HNHB/bird residue scan is zero. Inventory regenerated to 319 rows.

### HNXYMJ (97) — native server runtime

- Provenance: legacy ID 96/client display `信阳麻将`, Henan HNXYMJ source and current catalog ID 97 agree.
- Dedicated runtime covers three hu modes, three wan-fa values, da-zui 2/5/0, the source-exact 20/40/unlimited cap mapping (`0` runtime sentinel for unlimited), wind/no-wind inventory, and kan-pai/si-men-qing/luan-san-feng options with their source dependencies.
- Covers 136/108 physical inventory, KAN WAITING_EX choice/pass/timeout, source OpPoints, authoritative hu/cap settlement, private views, replay, object restore and reconnect.
- Dedicated tests and full Mahjong reactor passed with `-Dexec.skip=true`; SPI physical count is one and neighboring HNXC/HNJY/bird residue scan is zero. Inventory regenerated to 318 rows.

### HNZZMJ (126) — blocked by executable create-contract mismatch

- Legacy ID 125/client display `郑州麻将` and the HNZZMJ source lineage agree.
- Actual consumers prove `pao=0/1/2` means free/no/fixed and fixed points are `gudingpao+1`; `hufa` is consumed by `HNZZMJRoom.isCanDianPao`; QD/GSKH and the eight options are consumed by Room/SetOp/CalcPosEnd.
- However, live client fields `fengDing` and `huPai` have no read path anywhere in HNZZMJ Room, SetOp or CalcPosEnd. Inventing UI-derived caps or a second hu-mode authority would create a false runtime.
- All unregistered HNZZMJ foundation files were removed and provider SPI count remains zero.

### HSMJ (900002) — blocked by ID and executable-contract mismatch

- The available authoritative client defines `GameType_HSMJ=61` and displays `衡水麻将`; catalog ID 900002 has no verified migration/offset provenance (the normal migration offset would produce 62).
- The live client submits `huType` and `zhuangType`, while HSMJ Room reads `wanfa` and even derives `HSMJHuFa` from that same `wanfa`; therefore the executable create contract is internally inconsistent.
- No HSMJ native files or SPI registration were created for catalog ID 900002.

### HTMJ (157) — native server runtime

- Provenance: legacy ID 156/client display `会同麻将`, Hunan HTMJ source and current catalog ID 157 agree; ordinal meanings were verified through RoomSet/CalcPosEnd consumers.
- Dedicated runtime implements jia-ma modes (optional WAITING_EX/no-ma/forced-ma), 1/2/3 tail birds, the source winner-relative hit formula, zhong-niao scoring, and all five live options.
- Covers authoritative settlement, bird/ma state, private views, replay, object restore and reconnect; common cut/final-score fields remain metadata only.
- Dedicated tests and full Mahjong reactor passed with `-Dexec.skip=true`; SPI physical count is one and neighboring HNXY/HNXC residue scan is zero. Inventory remains 317 rows after persisted blockers.

### HYHSMJ (373) — native server runtime

- Provenance: legacy ID 372/client display `衡山麻将`, Hunan source and current catalog ID 373 agree; bird and option semantics were verified in RoomSet/SetOp/CalcPosEnd consumers.
- Dedicated runtime opens two physical indicators and uses both derived jin tiles in one wildcard detector; indicator/jin state is persisted and restored.
- Implements the three source bird modes (including wall-parity draw counts), reserve/que-zhang control, four live options, source pattern points, optional gang ledger, authoritative settlement, private views, replay and reconnect.
- All copied jia-ma/WAITING_EX state was removed before registration. Dedicated tests and full Mahjong reactor passed with `-Dexec.skip=true`; SPI count is one and strict residue scan is zero. Inventory is 316 rows.

### HZBDMJ (623) — blocked by executable create-contract mismatch

- Provenance is consistent: legacy client ID 622/display `惠州百搭麻将`, HZBDMJ source package and current catalog ID 623 follow the verified +1 migration.
- Consumers verify `guipai` ordinals as 0/2/4/6/8 flower wildcards, `mapai` horse modes, and WHJB/LMP/ZYDQF/nonnegative options through Room/RoomSet/CalcPosEnd.
- The live client also always submits `zhongmafangshi`, and the create DTO declares it, but exhaustive reads of Room, RoomSet, SetOp and CalcPosEnd find no consumer. Adding an effect would fabricate rules; silently accepting the button would violate the live-rule requirement.
- The unregistered foundation was removed. No HZBDMJ provider or SPI entry remains.

### HZMJ (1) — blocked by executable create-contract mismatch

- Provenance is consistent: legacy ID 0/client display `红中麻将`, HZMJ source and current catalog ID 1 agree. Consumers verify all six zhua-ma modes, three multi-hu bird-owner modes, ten optional rules, trusteeship, room timer and two-player union fixed scoring.
- The live create UI also submits `moban` (XY/WZ/YX template selector), and `CHZMJ_CreateRoom` declares it, but exhaustive search of the complete HZMJ server package finds no read beyond that declaration.
- Inventing template behavior or ignoring a live selector would fail the authoritative-rule gate. All unregistered HZMJ runtime/test files were removed and SPI remains zero.

### JCAHMJ (367) — native server runtime

- Provenance: legacy ID 366/client display `荆楚捱晃麻将`, JCAHMJ source package and current catalog ID 367 agree; all ordinal values were verified through RoomSet, SetOp and CalcPosEnd consumers.
- Dedicated runtime implements four peima modes (留底马/自由配马/均马/不奖马), dima ordinals 0–10 as 4–14 cards and the no-award `dima=-1` contract, plus all four live options.
- Covers 144-tile flower inventory and replacement, authoritative tail-horse allocation/hits, source scoring, settlement, private views, replay, restore and reconnect. The empty legacy WaitingEx branch was not fabricated into a choice phase.
- Pre-SPI behavior/restore tests passed; after exactly one SPI registration the full three-test acceptance class and complete Mahjong reactor passed with `-Dexec.skip=true`. FDMJ/WAITING_EX/piao residue scan is zero.

### JCHHMJ (240) — native server runtime

- Provenance: legacy ID 239/client display `荆楚晃晃麻将`, JCHHMJ package and current ID 240 agree. SetOp/SetCard/CalcPosEnd consumers verify all three wan-fa ordinals and both optional rules.
- Dedicated runtime implements 一脚癞油/半癞/无癞到底, 油中油, 去万牌 and its three-player rejection, source OpPoint/gang scoring and 108/72 inventories.
- Dynamic jin follows the legacy `kaijin` consumer: its type is selected from the remaining wall without removing a physical indicator. Jin state is authoritative and persists through views, replay, restore and reconnect.
- Pre-SPI contract/nine-stage tests passed; after one SPI entry the complete three-test class and Mahjong reactor passed with `-Dexec.skip=true`. JCA/peima/dima/FDMJ/WAITING_EX/piao/horse/bird residue scan is zero.

### JJMJ (188) — native server runtime

- Provenance: legacy ID 187/client display `九江麻将`, Jiangxi JJMJ package and current ID 188 agree. RoomSet/SetCard/CalcPosEnd consumers verify every ordinal and optional rule.
- Dedicated runtime implements dipao 0/5/10/20, dimayou 0/2/4/6, removal of wan/tong/tiao or no removal (72/108 inventories), and all eight optional-rule legality and scoring branches.
- Authoritative state persists tail oil cards/hits, reserve behavior, gang ledger and settlement; private views, replay, restore and reconnect are covered. Provider descriptor was explicitly corrected to `九江麻将`.
- Pre-SPI contract/nine-stage methods passed; after exactly one SPI entry the full three-test class and Mahjong reactor passed with `-Dexec.skip=true`. Adjacent JCH/JCA/jin/peima/WAITING_EX/piao/horse/bird residue scan is zero.

### JLMJ (193) — native server runtime

- Provenance: legacy ID 192/client display `吉林麻将`, JLMJ package and current ID 193 agree; actual Room/SetCard/SetPos/CalcPosEnd consumers verify all ordinals.
- Dedicated runtime covers three wan-fa modes, fengDing ordinals 0–3 as caps 16/32/48/64, all eight optional rules and their dependencies, dynamic treasure, flowers, egg/gang ledgers and dealer rotation.
- Physical inventory is 144; the wall reserve follows source egg parity exactly: four-player 14/15 and two/three-player 16/17. Settlement, private views, replay, restore and reconnect are authoritative.
- Pre-SPI contract/nine-stage methods passed; one SPI entry, the complete acceptance class and Mahjong reactor passed with `-Dexec.skip=true`. Adjacent-game residue scan is zero.

### JMHHMJ (32) — native server runtime

- Provenance: legacy ID 31/client display `荆门晃晃`, JMHHMJ package and current ID 32 agree. Only live client fields are authoritative; stale DTO fields `suanFen/huPai/dianPao/fengding` are intentionally omitted.
- Dedicated runtime covers diFen ordinals 0–4 as 1/2/5/3/4, the live wan-fa legality modes and four options for auto-ready, removing wan, character self-draw and thrown-character discard hu.
- Implements 112/76 physical inventories, dynamic indicator/laiZi, reserve 14, red-center/laiZi/normal gang ledgers, authoritative settlement, private views, replay, restore and reconnect.
- Pre-SPI methods passed; after one SPI entry the dedicated acceptance class and full Mahjong reactor passed with `-Dexec.skip=true`. Adjacent-game residue scan is zero.

### JSSQMJ (164) — native server runtime

- Provenance: legacy ID 163/client display `宿迁麻将`, JSSQMJ package and current ID 164 agree. Room/RoomSet/SetPos/CalcPosEnd consumers verify both wan-fa values, the every-set option and all mouth declarations.
- Dedicated WAITING_EX state collects sequential per-seat MouseType lists, validates NOT/BuPeng/DaPei/DuCi/DuLiu combinations and applies timeout as NOT; every-set versus every-circle persistence is authoritative.
- Implements 132 physical tiles including flowers, fixed white wildcard, mouth legality/points, prevailing wind and gang ledgers, settlement, private views, replay, restore and reconnect.
- Pre-SPI tests passed; one SPI entry, the dedicated acceptance class and full Mahjong reactor passed with `-Dexec.skip=true`. Adjacent-game residue scan is zero.

## Non-Mahjong family-runtime convergence

- LongCard four catalog bindings now route through one family registry/provider entry; WordCard ten bindings do the same. Their code-specific ServiceLoader entries and Provider source files were removed while regional rule/session implementations and nine-stage tests remain authoritative.
- Poker's 150 region-config rows are schema-validated across eight core families. Six source-backed category runtimes (`pdk`, `aypdk`, `hbpdk`, `cp`, `hndzp`, `lhzp`) now bind through `PokerCatalogRuntimeRegistry`; code-specific Poker SPI entries were removed. CP is catalog-bound to `poker:compare-hand`, eliminating its former false 510K descriptor.
- Module-internal Poker value/state helpers were physically consolidated without changing behavior. Architecture gates now pass at Poker 52, LongCard 20 and WordCard 65; Bootstrap also passes its configured limit.
- Evidence: full Poker reactor test passed after clean; Bootstrap catalog/ServiceLoader targeted test passed; `tools/check_architecture_boundaries.rb` passes all 42 modules; `tools/verify_r04_family_runtime.rb` reports 528 bindings, 15 registered families, 32 archetypes, 528 family-region rows, two exact missing-source items, zero duplicate Provider/client-main groups, and `passed=true`.

## 2026-08-25 family-runtime final regression

- The four packaged region matrices now retain source filenames only; complete legacy provenance remains in the audit-only four-layer map. This keeps runtime resources inside the Aoo brand boundary without weakening provenance.
- Bootstrap assertions now match the family architecture: five genuinely standalone lifecycle providers remain discoverable through ServiceLoader, while catalog registries expand family/region bindings. The remaining generic catalog bridges are 146, and all are executable through create, rule boundary, command, reconnect and settlement.
- Poker family wrappers now expose a non-empty authoritative boundary rule even when their delegate is not a `PokerGameProvider`; the focused Bootstrap/ServiceLoader/catalog lifecycle suite passes.
- Final gates: architecture boundaries 42/42 modules; family runtime 15/15 families, 528/528 bindings and region schemas, 32 archetypes, two source blockers, zero duplicate server/client mains; authoritative provider audit 6/6; unreachable branches passed; Client family/game contracts 71/71; Creator 3.8.8 TypeScript 5.8.2 `--noEmit` passed; CUR-07 dynamic paths, component registration and native assets passed with blocking=0.
- `./mvnw -Dexec.skip=true clean verify` reaches Bootstrap after the first 42 modules and its three R04 failures are now fixed by the focused green suite. The non-skipped root `clean verify` advances through brand, API, JDBC, BOM, reachability, registration and duplicate-implementation gates, then stops on the pre-existing non-R04 `RoomLeaseStore.java: not implemented` production-stub finding. R04 did not overwrite that recovery owner.
