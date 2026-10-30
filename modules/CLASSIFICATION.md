# server/modules 分类索引

本索引登记当前磁盘上的真实物理路径与职责分类，不改变 Java package、Maven artifactId、协议、Hall 兼容别名或运行时行为。
分类容器使用本次用户批准的小写与连字符命名：`common`、`infrastructure`、`user-domain`、`social-ops`、`gameplay`、`compatibility`；`server/build-only` 为尚不存在的独立输出目标，本批不创建。

## 七类职责边界

| 分类 | 职责边界 | 当前顶层容器 |
|---|---|---|
| `Common` | 公共房间、公共运行能力和跨玩法公共接口 | `server/modules/common` |
| `Infrastructure` | Bootstrap、Gateway、配置、SPI、Family、管理控制面和基础设施 | `server/modules/infrastructure` |
| `UserDomain` | Account、身份、用户资产、资料和用户域服务 | `server/modules/user-domain` |
| `SocialOps` | Lobby、Club、社交、匹配、分享、观战、活动和运营服务 | `server/modules/social-ops` |
| `Gameplay` | 玩法聚合、麻将、扑克、长牌及玩法 Provider | `server/modules/gameplay` |
| `Compatibility` | Legacy 模块、旧 Room/runtime、WordCard 历史兼容入口 | `server/modules/compatibility`；`gameServer` 物理保留原位 |
| `BuildOnly` | target、IDE 锁、生成物和临时构建输出，不进入运行时 classpath | 顶层 `server/build-only` 当前不存在 |

## 当前顶层目录状态

| 实际路径 | 逻辑分类 | 状态 | 说明 |
|---|---|---|---|
| `server/modules/common` | `Common` | Current | 公共运行能力容器 |
| `server/modules/infrastructure` | `Infrastructure` | Current | 基础设施与管理控制面容器 |
| `server/modules/user-domain` | `UserDomain` | Current | 用户域容器 |
| `server/modules/social-ops` | `SocialOps` | Current | Lobby、Club 与运营容器 |
| `server/modules/gameplay` | `Gameplay` | Current | 玩法聚合容器 |
| `server/modules/compatibility` | `Compatibility` | Current | Legacy 兼容容器 |
| `server/build-only` | `BuildOnly` | ABSENT | 当前没有该顶层目录；不得为填分类而创建或移动构建产物 |
| `server/modules/gameServer` | `Compatibility`（逻辑） | USER_RESERVED | 用户保留自身名称与内容；本批仅随已授权祖先目录移动，不单独移动、重命名、删除或编辑内部内容 |

## Common

| 实际路径 | 状态 | 职责 |
|---|---|---|
| `server/modules/common/AooKernel` | Current | 公共基础内核 |
| `server/modules/common/CommonRoom` | WAITING_CUSTODY | CommonRoom 公共能力唯一拥有者；保留 dirty custody |
| `server/modules/common/RoomSafety` | Current | 房间安全 |

## Infrastructure

| 实际路径 | 状态 | 职责 |
|---|---|---|
| `server/modules/infrastructure/AdminApi` | Current | 管理身份与权限，管理控制面 |
| `server/modules/infrastructure/Bootstrap` | WAITING_CUSTODY | 启动与服务装配；保留 HELD/dirty custody |
| `server/modules/infrastructure/ConfigCenter` | WAITING_CUSTODY | 配置中心；保留 dirty custody |
| `server/modules/infrastructure/ExternalIntegration` | Current | 外部集成 |
| `server/modules/infrastructure/Families` | WAITING_CUSTODY | 身份目录与 Family 注册 |
| `server/modules/infrastructure/GameSPI` | WAITING_CUSTODY | 身份合同与 SPI |
| `server/modules/infrastructure/Gateway` | WAITING_CUSTODY | Gateway 聚合入口；保留 dirty custody |

## UserDomain

| 实际路径 | 状态 | 职责 |
|---|---|---|
| `server/modules/user-domain/Account` | Current | 账户与会话 |
| `server/modules/user-domain/Billing` | Current | 资产与计费 |
| `server/modules/user-domain/IdentityVerification` | Current | 身份验证 |
| `server/modules/user-domain/Inventory` | Current | 用户资产库存 |
| `server/modules/user-domain/Location` | Current | 地域与位置 |
| `server/modules/user-domain/PlayerProfile` | Current | 玩家资料 |
| `server/modules/user-domain/Privacy` | Current | 隐私域 |

## SocialOps

| 实际路径 | 状态 | 职责 |
|---|---|---|
| `server/modules/social-ops/Activity` | Current | 活动 |
| `server/modules/social-ops/Club` | WAITING_CUSTODY | Club owner；保留 dirty custody |
| `server/modules/social-ops/Gifting` | Current | 礼物 |
| `server/modules/social-ops/Lobby` | WAITING_CUSTODY | Lobby owner；外部 Hall 兼容路径不变 |
| `server/modules/social-ops/LuckDraw` | Current | 抽奖 |
| `server/modules/social-ops/Matchmaking` | Current | 匹配 |
| `server/modules/social-ops/Media` | Current | 媒体 |
| `server/modules/social-ops/Ranking` | Current | 排行 |
| `server/modules/social-ops/Referral` | Current | 邀请与归因 |
| `server/modules/social-ops/Share` | Current | 分享 |
| `server/modules/social-ops/Social` | Current | 社交 |
| `server/modules/social-ops/Spectator` | Current | 观战 |
| `server/modules/social-ops/Support` | Current | 客服支持 |
| `server/modules/social-ops/Telemetry` | Current | 监控与遥测 |
| `server/modules/social-ops/VersionNotice` | Current | 版本公告 |

## Gameplay

| 实际路径 | 状态 | 职责 |
|---|---|---|
| `server/modules/gameplay/games` | WAITING_CUSTODY | 玩法聚合；保留 LongCard/WordCard 相关 custody |
| `server/modules/gameplay/games/mahjong` | Current | 麻将玩法族 |
| `server/modules/gameplay/games/mahjong/CDXZMJ` | Current/Compatibility workload | 成都血战麻将 Provider；兼容运行入口按发布配置控制 |
| `server/modules/gameplay/games/mahjong/SCJYMJ` | Current | 四川简阳麻将 Provider |
| `server/modules/gameplay/games/poker` | Current | 扑克玩法族 |
| `server/modules/gameplay/games/poker/cx` | Current | 扯旋 |
| `server/modules/gameplay/games/poker/nn` | Current | 牛牛 |
| `server/modules/gameplay/games/poker/PDK` | Current | 跑得快 |
| `server/modules/gameplay/games/poker/zjh` | Current | 金花 |
| `server/modules/gameplay/games/poker/ZYPK` | Current | 自由扑克 |
| `server/modules/gameplay/games/long-card` | Current | 长牌玩法族 |
| `server/modules/gameplay/games/long-card/compatibility/WordCard` | Compatibility | WordCard 历史兼容入口，不进入当前 LongCard 生产路径 |

## Compatibility

| 实际路径 | 状态 | 职责 |
|---|---|---|
| `server/modules/compatibility/LegacyAccountServer` | Compatibility | 历史账户兼容 |
| `server/modules/compatibility/LegacyCommDef` | Compatibility | 历史公共定义兼容 |
| `server/modules/compatibility/LegacyCommon` | Compatibility | 历史公共实现与资源兼容 |
| `server/modules/compatibility/LegacyGameHall` | Compatibility | Hall 历史兼容 facade |
| `server/modules/gameServer` | USER_RESERVED | 新旧运行时混合；用户后续自行处理，本项目批次不接管 |

## BuildOnly 边界

当前顶层 `server/build-only` 不存在。各模块内部的 `target/`、`.direct-lock-*`、IDE 文件和临时构建输出仍按 build-only 规则处理，不能进入 Maven runtime classpath、发布清单或服务启动清单；本批不创建、移动或删除这些内容。

## 迁移状态

物理改名 `Server/server` → `Server/Modules` 由用户完成；R21/R22 已完成部分物理归类和直接 POM 路径闭合。当前索引已按磁盘真实大小写与分类容器更新；后续引用、部署、脚本和构建修复必须按独立白名单实施。任何未完成 custody 的目录继续保持 WAITING_CUSTODY，不能据本索引宣称运行、API、重连、handoff 或 E2E 完成。
