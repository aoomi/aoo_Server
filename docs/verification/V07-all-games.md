# V07 全玩法自动验收

更新时间：2026-08-24

## 结论

V07 **未通过**，不得关闭。自动验收工具 `tools/verify_v07_all_games.rb` 已建立并实际执行；它只承认通过生产 `ServiceLoader` 注册的原生 Provider 和现有测试报告，不把 `CatalogGameProvider` 元数据桥当作玩法实现。

当前机器证据：玩法目录 528 项，原生生产 Provider 9 项；其中与目录 code 直接重合的 CDXZMJ、AYDSS、AYCP、BYZP 之外，目录仍有 524 项由元数据桥接，不能当作权威玩法完成。9 个原生 Provider 均已有九阶段源码/测试证据；AYDSS、ZJH 的恢复回放缺口已补齐，AYCP、BYZP 已从只读旧版真实源码迁移并完成真链。全目录仍未完成，因此 V07 继续未通过。

## 已验证与整改

- 修正 `tools/audit_authoritative_game_provider_coverage.rb`：仅审计生产服务注册的具体 Provider，并识别牌类接口继承能力；不再把 `GameProvider` SPI、`PokerGameProvider`、`MahjongGameProvider` 误报成具体玩法，也不再误报 NJPDK/XCPDK 继承的命令适配器缺失。
- 新增 `tools/verify_v07_all_games.rb`：逐个生产 Provider 聚合源码、测试、Surefire 结果，对九个生命周期阶段生成机器可读证据到 `work/audit/v07-all-games.json`；元数据桥、缺源码和测试缺口会使工具非零退出。
- 修正 NJPDK 客户端玩法测试：聊天/社交只接受唯一生产 `common.room.dispatch` 适配器，并明确拒绝已退休的 `CNJPDKChat` 旧入口。`node Client/tests/games/njpdk/runtime-contract.test.mjs` 已通过。
- 客户端扫描到 14 个实际 `NetworkAdapter` 类，没有同玩法重复实现；CompatibilityApp 中 HZMJ 文件只是指向正式玩法实现的 re-export，不计为第二套。

## 原生玩法矩阵

| 玩法 | Provider 测试报告 | 九阶段静态/测试证据 | 阻塞 |
|---|---:|---:|---|
| CDXZMJ | 通过 | 齐全 | 无自动阻塞 |
| SCJYMJ | 通过 | 齐全 | 无自动阻塞 |
| NJPDK | 通过 | 齐全 | 无自动阻塞 |
| XCPDK | 通过 | 齐全 | 无自动阻塞 |
| ZYPK | 通过 | 齐全 | 无自动阻塞 |
| AYDSS | 通过 | 齐全 | 已补版本化快照、严格恢复与有序事件回放 |
| AYCP | 通过 | 齐全 | 真实客户端、唯一协议入口及原生权威 Provider 已迁移；5 项玩法测试通过 |
| BYZP | 通过 | 齐全 | 81张专属牌、醒/油/鬼牌规则、权威会话、真实客户端及唯一入口已迁移 |
| ZJH | 局部编译通过 | 齐全 | 已补全员准备门禁、快照恢复和事件回放；全 Reactor 受 RoomSafety 阻断 |

“证据齐全”只表示现有源码和测试覆盖九阶段关键词且测试报告通过，不替代真实中间件端到端验收，也不扩展为 528 个玩法均已完成。

## 源码缺失阻塞

目录中的 524 项当前没有同 code 的独立生产 Provider。现有 `CatalogGameProvider` 只生成通用目录会话，不能证明地区规则、牌义、操作优先级、结算和历史回放等价。R89-016 进一步核验发现，原先标记“18 项均无源码”并不准确：只读旧版中 16 项同时具有独立客户端和服务端权威源码；其中 AYCP、BYZP 已完成真实迁移，余下 14 项可继续逐项迁移。AHPHZ、LCZP 只有服务端源码而缺独立牌桌客户端，继续阻塞。NTCP 又发现目录牌类错误，已修正为 `MAHJONG/mahjong-lai-zi`，但其专属双金、喜儿补花、22分起胡、过碰过胡、飘分和结算规则尚无原生 Provider，仍不得由目录桥冒充。详细证据见 `docs/verification/r89-016-regional-source-inventory.md`。

## 执行命令

```sh
cd /Users/aoo/Code/Game/BCG/Aoo/Server
ruby tools/verify_v07_all_games.rb
ruby tools/audit_authoritative_game_provider_coverage.rb
node ../Client/tests/games/njpdk/runtime-contract.test.mjs
```

本机未安装清单要求的 Java 26（当前只有 Java 25 与 17），且仓库不存在文档曾提到的 `mvnw26`，所以本轮无法重跑 Maven；没有以旧 Surefire 报告替代“本轮构建通过”的表述。Cocos/浏览器/真实设备人工验收按要求暂缓。
