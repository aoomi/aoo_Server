# Aoo 当前实际使用依赖、插件与版本完整清单

> 用途：交付其他 AI 或人工进行版本适配、安全、许可证、冗余和升级风险复审。本文只陈列当前项目实际锁定或实际消费的版本，不把“可升级版本”误写成当前版本。

- 清单生成时间：2026-08-25T03:15:48Z
- 官方源查询日期：2026-08-25
- 覆盖范围：Server、Client、Admin
- 采集方法：exact resolved Maven tree + POM/plugin declarations + pnpm lock materialization + official registries/releases
- 机器明细源：`Server/docs/generated/aoo-project-latest-stable-dependency-audit.json`
- 版本判定优先级：锁文件/解析后的依赖树 > POM 或项目配置 > 文档说明。

## 1. 数量汇总

- `mavenComponents`：112
- `mavenPlugins`：9
- `npmPackageVersions`：389
- `mavenVulnerabilities`：0
- `missingMavenLicenses`：0

## 2. 工具链、引擎、数据库与运行服务

| 名称 | 当前实际版本 | 用途 | 当前状态 | 官方来源 |
|---|---:|---|---|---|
| Oracle JDK | 25.0.4.1 | Server compile/runtime, release=25 | 已锁定受支持线 | https://www.oracle.com/java/technologies/javase/jdk25-archive-downloads.html |
| Apache Maven Wrapper | 3.9.16 / wrapper 3.3.4 | reproducible Maven lifecycle | 已最新 | https://maven.apache.org/wrapper/ |
| Node.js | 24.19.0 | Admin tooling | 因 LTS 与 Vite/Vitest engine 兼容保留 | https://nodejs.org/en/about/previous-releases |
| pnpm | 11.19.0 | Admin/Client frozen lock replay | 锁文件兼容线 | https://registry.npmjs.org/pnpm |
| Cocos Creator | 3.8.8 | Client engine/editor/build pipeline | 项目 engine 锁定；不可脱离 Creator 兼容矩阵盲升 | https://docs.cocos.com/creator/3.8/manual/en/ |
| MySQL container | 8.0 | migration and integration verification | 因 SQL compatibility baseline 保留 | https://dev.mysql.com/doc/relnotes/mysql/8.0/en/ |
| RocketMQ container/client | 5.3.2 / 5.5.0 | outbox and MQ runtime | 客户端按 Maven 兼容矩阵管理 | https://rocketmq.apache.org/release-notes/ |
| Redis container | 7.4-alpine | cache runtime | 部署基线保留 | https://redis.io/docs/latest/operate/oss_and_stack/stack-with-enterprise/release-notes/ |
| MongoDB Server container | mongo:8.0.29-noble | production/default document persistence runtime | 生产基线精确锁定 8.0.29；8.3.8 仅可作为非默认未来兼容测试目标 | https://www.mongodb.com/docs/manual/release-notes/8.0/ |
| Flyway migrations | 13.3.0 | sole schema migration authority | 由 Maven compatibility matrix 管理 | https://documentation.red-gate.com/flyway/reference/release-notes |

## 3. 兼容性矩阵

| 上游 | 下游/约束 | 当前验证结论 |
|---|---|---|
| JDK 25 | maven-compiler release 25 / Lombok / Error Prone annotations | clean verify PASS; retain pinned JDK patch line |
| Node 24.19 + pnpm 11.19 | Vite 8.2.2 / Vitest 4.1.11 / ESLint 10 / vue-tsc | peer/engine compatible; Admin full quality PASS |
| Vue 3.5.41 | plugin-vue 6.0.8 / Router 5.2.0 / Pinia 4.0.3 / Element Plus 2.14.5 | peer constraints satisfied |
| Cocos Creator 3.8.8 | Client cc imports, project manifest, web-desktop template | engine-owned modules; no npm runtime substitution |
| MySQL 8.0 + Flyway | 87 migrations and JDBC integration tests | strict UTC/utf8mb4 migration + clean verify PASS |
| RocketMQ 5.5 client | GameCommon/gameServer outbox | unused telemetry transitives excluded; runtime feature retained |
| MongoDB Server 8.0.29 | MongoDB Java Driver 5.10.0 | 生产运行组合；驱动保持 5.10.0，8.3.8 仅限显式未来兼容验证 |

## 4. Maven 实际依赖（112 项，含直接、受管和传递依赖）

| 坐标 | 当前版本 | 关系 | 消费模块 | 许可证 | SHA-256 |
|---|---:|---|---|---|---|
| `at.yawk.lz4:lz4-java` | `1.11.2` | direct-or-managed | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `8a7458657938e9b692224db83aac458c13abd7a59ab75c3d3b08bd2233a646a2` |
| `ch.qos.logback:logback-classic` | `1.6.3` | direct-or-managed | AooKernel, Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | EPL-2.0, LGPL-2.1-only | `beebede8db065fe1b72909ecc66bb49acda618901635d86c25fce14a5915e37e` |
| `ch.qos.logback:logback-core` | `1.6.3` | transitive | AooKernel, Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | EPL-2.0, LGPL-2.1-only | `a6967a28c8b086dee75a694d2f6fb8830c71153539866d7d8af065c9b6df91e0` |
| `com.alibaba.fastjson2:fastjson2-extension` | `2.0.64` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache 2 | `9c680abdccd4a800270fa8ff8778b81b5958e1a1fbaed898d5712d41e03245b4` |
| `com.alibaba.fastjson2:fastjson2` | `2.0.64` | direct-or-managed | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache 2 | `c3de8edf8705981deaf53f344c01544479f6db6925968bf934e3b9ef6f0b1f98` |
| `com.alibaba:druid` | `1.2.28` | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache 2 | `c4cec3792390a2bbe6020d59be52160fd38d15de0f00dc52702eb91128c695a5` |
| `com.alibaba:fastjson` | `2.0.64` | direct-or-managed | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache 2 | `655d520f9849abf48b33ef82501490872cdbbc449d0cc4f3dc11500d33676a19` |
| `com.esotericsoftware:kryo` | `5.6.2` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 3-Clause BSD License | `4e8b1d2f4977187af8a51a957329722dc1cdc56a7c94fbb5a791e82897629cff` |
| `com.esotericsoftware:minlog` | `1.3.1` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 3-Clause BSD License | `5d4d632cfbebfe0a7644501cc303570b691406181bee65e9916b921c767d7c72` |
| `com.esotericsoftware:reflectasm` | `1.11.9` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 3-Clause BSD License | `712b44da79a5b7f47a28cbfcb3d8ecfc872fae349c48aa4d3e38a5d69956afce` |
| `com.fasterxml.jackson.core:jackson-annotations` | `2.22` | transitive | Account, Activity, AdminApi, Billing, Bootstrap, Club, ConfigCenter, Families, GameCommon, Gateway, Gifting, Hall, IdentityVerification, Inventory, Location, LuckDraw, Mahjong, Matchmaking, Media, NJPDK, PlayerProfile, Poker, Privacy, Ranking, Referral, RoomSafety, SCJYMJ, Share, Social, Spectator, Support, Telemetry, VersionNotice, XCPDK, ZJH, ZYPK, gameServer | The Apache Software License, Version 2.0 | `21ddb598807d3a51a876704eb979d9296e1c6a6f47ab1826ff88c6d6a127a2d0` |
| `com.fasterxml.jackson.core:jackson-core` | `2.22.2` | transitive | Account, Activity, AdminApi, Billing, Bootstrap, Club, ConfigCenter, Families, GameCommon, Gateway, Gifting, Hall, IdentityVerification, Inventory, Location, LuckDraw, Mahjong, Matchmaking, Media, NJPDK, PlayerProfile, Poker, Privacy, Ranking, Referral, RoomSafety, SCJYMJ, Share, Social, Spectator, Support, Telemetry, VersionNotice, XCPDK, ZJH, ZYPK, gameServer | The Apache Software License, Version 2.0 | `ff167a6317be15895706c26668f45b898efe40ab8780970658210fe1393d52a6` |
| `com.fasterxml.jackson.core:jackson-databind` | `2.22.2` | transitive | Account, Activity, AdminApi, Billing, Bootstrap, Club, ConfigCenter, Families, GameCommon, Gateway, Gifting, Hall, IdentityVerification, Inventory, Location, LuckDraw, Matchmaking, Media, NJPDK, PlayerProfile, Privacy, Ranking, Referral, RoomSafety, SCJYMJ, Share, Social, Spectator, Support, Telemetry, VersionNotice, XCPDK, ZJH, ZYPK, gameServer | The Apache Software License, Version 2.0 | `d0da14c12b16b5d54719aa172d83b542ff4abeb8b0fb7db476fde8ceece760ca` |
| `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` | `2.22.2` | transitive | Account, AdminApi, Billing, Bootstrap, Club, ConfigCenter, Families, GameCommon, Gateway, Hall, IdentityVerification, Location, Mahjong, Matchmaking, NJPDK, Poker, SCJYMJ, Social, Spectator, XCPDK, ZJH, ZYPK, gameServer | The Apache Software License, Version 2.0 | `9df71cc7fb3781fd0bed6c05eebbfa8b39292a49f5619e76fdd0d889f64c8f33` |
| `com.github.luben:zstd-jni` | `1.5.2-2` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | BSD 2-Clause License | `fedeb4c59d9593e84bc0b1cca30c90da7f3452e78d3a1b9d5dd7370efc474b1d` |
| `com.google.code.gson:gson` | `2.14.0` | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache-2.0 | `2cbd119bf1961c28788310963dc80ba65f58cdeec1dd139c8bdb1240faa2c36f` |
| `com.google.errorprone:error_prone_annotations` | `2.50.0` | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache 2.0 | `4667724877f1d37a689202da191e23efa7657c62eef93ccdac406eccfe5cdd0a` |
| `com.google.guava:failureaccess` | `1.0.3` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `cbfc3906b19b8f55dd7cfd6dfe0aa4532e834250d7f080bd8d211a3e246b59cb` |
| `com.google.guava:guava` | `33.7.1-jre` | direct-or-managed | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `796d8e28ac64e83a47c4c5935a8fecc4682650a04bbdead738ef0f5a3a0e6c46` |
| `com.google.guava:listenablefuture` | `9999.0-empty-to-avoid-conflict-with-guava` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | The Apache Software License, Version 2.0 | `b372a037d4230aa57fbeffdef30fd6123f9c0c2db85d0aced00c91b974f33f99` |
| `com.google.j2objc:j2objc-annotations` | `3.1` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `84d3a150518485f8140ea99b8a985656749629f6433c92b80c75b36aba3b099b` |
| `com.google.protobuf:protobuf-java` | `4.36.0` | direct-or-managed | Account, AdminApi, Billing, Bootstrap, Club, Gateway, Hall, Location, Matchmaking, Media, NJPDK, PlayerProfile, RoomSafety, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | BSD-3-Clause | `06515776b10bfed334b610273ffdd8884098a9333cb816314b4e4876bfcd2041` |
| `com.lmax:disruptor` | `4.0.0` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | The Apache License, Version 2.0 | `c2ba80841541272bc815bcadab910d2d716aa563eca15762450ab4c889440505` |
| `com.mysql:mysql-connector-j` | `26.7.0` | direct-or-managed | Account, AdminApi, Billing, Bootstrap, Club, Gateway, Hall, Location, Matchmaking, Media, NJPDK, PlayerProfile, RoomSafety, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | The GNU General Public License, v2 with Universal FOSS Exception, v1.0 | `69084713593a4aa8d07c383619b9639276f08bccf8faf1c562178147d389b1e1` |
| `com.squareup.okio:okio-jvm` | `3.18.1` | direct-or-managed | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | The Apache Software License, Version 2.0 | `b97b640557a650d15411f2be29aef558c4e422a83653cb18ba494311dbed18be` |
| `commons-beanutils:commons-beanutils` | `1.11.0` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache-2.0 | `9e44ba68ec9a3f21286fa2a8bbb003b735c0f69101bb43144b79f4f8aaa74709` |
| `commons-codec:commons-codec` | `1.22.1` | direct-or-managed | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache-2.0 | `78a5d732fbd715e2d10bd7150d2f8030bae57267f8aacc5c88f642cb6c2e5d3f` |
| `commons-collections:commons-collections` | `3.2.2` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `eeeae917917144a68a741d4c0dff66aa5c5c5fd85593ff217bced3fc8ca783b8` |
| `commons-dbutils:commons-dbutils` | `1.8.1` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache-2.0 | `e64501edba68fb4e674a3c350955e743b1f55a878c8fd348adf0ebffc008e7b0` |
| `commons-digester:commons-digester` | `2.1` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | The Apache Software License, Version 2.0 | `e0b2b980a84fc6533c5ce291f1917b32c507f62bcad64198fff44368c2196a3d` |
| `commons-logging:commons-logging` | `1.3.5` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache-2.0 | `6d7a744e4027649fbb50895df9497d109f98c766a637062fe8d2eabbb3140ba4` |
| `commons-validator:commons-validator` | `1.10.0` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache-2.0 | `0d1600cddc24af7f49b5eb01e31adda3ce3b25f236c8c75eaecdf78428469eb9` |
| `io.github.aliyunmq:rocketmq-logback-classic` | `1.0.1` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `5c1a776192eb54ffceb00a8fbb474077bf74982ebf2512b1101485f4c1982386` |
| `io.github.aliyunmq:rocketmq-slf4j-api` | `1.0.1` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `6ebbefd23c89e5617577100585e2205fe6d91413f7fae7af75e4eb397b1ee895` |
| `io.netty:netty-all` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `fd66659c35601336df8c3586bfc504549bbedce29755dac6572287f9443c4237` |
| `io.netty:netty-buffer` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `0d249178ec0204b35a6ad3e5ac8a6c2ecb3eb496c6cdae40b8eca4fa3cb6a0c7` |
| `io.netty:netty-codec-base` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `3b92bc3b7d231fa6f4889b2b9281cae6b634428b8121fb1b9b5fccba1543d352` |
| `io.netty:netty-codec-classes-quic` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `542e718fdd8fa4f6d57bef9f91a5fa01200940cb8b381fd4c21fb5c170276c71` |
| `io.netty:netty-codec-compression` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `425b287a1f36986a5c8edd76379a49decdf99d284f3454f3fa607a073bfb2c18` |
| `io.netty:netty-codec-dns` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `394f4649a824683735f28b299d0a48caff059ae30e654e2abf8b3f392af4d374` |
| `io.netty:netty-codec-haproxy` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `6650894f9c429fc29ad7876092a25cc973e8435d13fa70c0e48aab1ca73a6d37` |
| `io.netty:netty-codec-http2` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `450af97424cf58d2d72ad4d476d9e290f2fdf69fa181c289a6e6dd176bb86a7b` |
| `io.netty:netty-codec-http3` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `9fcf9873e31b2899c407b42d320aaf6afeb69807e63d9a865c391b4339fa24c7` |
| `io.netty:netty-codec-http` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `a5bde91ac82ab90579df6559c0bd7ea6ae37460e3f18116ab4122194283b3669` |
| `io.netty:netty-codec-marshalling` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `ee5ffed638da6b7e2b4567559bd5f78b18f635cdaa4f3b6d23aa27ccf6cb57c2` |
| `io.netty:netty-codec-memcache` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `7627b976a405de4169d972acf7e170a4dfc27d5a0d6e5e2bcc41ea2cb5f6fef9` |
| `io.netty:netty-codec-mqtt` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `420ddb6d8135165f8755804230033d6b1093da146169c30eb36dbac3ed3b394d` |
| `io.netty:netty-codec-native-quic` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `d8c48589adfafd798d1c4b3996d9b19dc7bd9f1785524b821388e2db73a52dd2` |
| `io.netty:netty-codec-protobuf` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `44b2fcc4bc1d6c01edafe6ae6c8a7de7c3c5bad59697a93b6b879a45a78a5387` |
| `io.netty:netty-codec-redis` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `7d72146f09c2e63be307a6a24516e1bc208bc345328b9c59540da131ccf5754c` |
| `io.netty:netty-codec-smtp` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `32e30ffe89acda2a457f16cf6f8b366f58d0b4b025563803bb0854962b81de3c` |
| `io.netty:netty-codec-socks` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `6b5760cc512cee1f7515e25010892471f3995045f84cd6c1d1b6e6cee534733c` |
| `io.netty:netty-codec-stomp` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `179dd61246bbe15468032a071faa21d392bfeaff9341a80e5ce2b744e509e687` |
| `io.netty:netty-codec-xml` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `c453be5dda52127ac82e4883cf03b3326741ff460a4ceebe9a54b23058537fb1` |
| `io.netty:netty-codec` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `ac76f972446a2f72818441f746d0e73c58519a4687bf680d2b687d7dc5e28e2e` |
| `io.netty:netty-common` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `502aae2a6680e9bca3558a3fa098f06dc4d87340b8b714e925a6d690871c092a` |
| `io.netty:netty-handler-proxy` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `94089bea6ba6f3248725f2bf233aa2bc70ac1547c21b9bdfc7548f710a845aa0` |
| `io.netty:netty-handler-ssl-ocsp` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `6cf70966f679e58c9c5fcb3e85cac30ab112911bdfd0645522bb5ee15ceffc7f` |
| `io.netty:netty-handler` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `4df11c7520b556c5e2c84b939182699d48fcbb09e97a6bb5e1c0b6835227d126` |
| `io.netty:netty-resolver-dns-classes-macos` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `e5708301fe173ce03d31f1a76ac0682d0a7b955451c968314a13e661855b33c5` |
| `io.netty:netty-resolver-dns-native-macos` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `69ba9124d88cde3c5d96237aabd7cadcdf416200e52f2933c275863d5ee8831e` |
| `io.netty:netty-resolver-dns` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `d332481c22136a68c2ee9e45e86212174932587d93a5dd4d201e196828bf9f91` |
| `io.netty:netty-resolver` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `7a4b599b9c0c29a505d3b7311f142df883fb27d0f67495a7cbaf533b0e456623` |
| `io.netty:netty-transport-classes-epoll` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `4e21d8a2e3429d9de967a716d58119f57585786bd2f7213bf5b27770425269de` |
| `io.netty:netty-transport-classes-io_uring` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `c6dafc0fd8514b816b9ba724a031c21de6b0fe3215773feb38b6b0bff15a9f34` |
| `io.netty:netty-transport-classes-kqueue` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `f0b2dd4757a22345b0b28e2856dc7ee44e5dea256cc520ddc27e4dce4ea9efb3` |
| `io.netty:netty-transport-native-epoll` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `c295b1d7f46e3c7375c93fa4c8abcc3944398c5e4eb53e31587cbbf0bf559d4f` |
| `io.netty:netty-transport-native-io_uring` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `4864d7aa6a0491363e9e73714da73c129713c9c160900a06b3e32db2502ffe6f` |
| `io.netty:netty-transport-native-kqueue` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `de98c8c8aecb90160921abc52ee64acaac64947e359daa7121b895fc3d3b15ab` |
| `io.netty:netty-transport-native-unix-common` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `e01aa467b9104de54a57c08c290f70b22e53ec97cf5583c87c112170ef5832cc` |
| `io.netty:netty-transport-rxtx` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `2940945bcf9b249ebf06c1c2cefa9266eafdf4abd45dc178a9127e4824208bb0` |
| `io.netty:netty-transport-sctp` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `5345dabce2cbf10bc9c6a80a8d7beeaa4cd975028aa04f1903a96b8e440529f6` |
| `io.netty:netty-transport-udt` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `bd3d40e949077dd0f0ba62e87db5f0f55afc8fab99d20450ec22c2b84267badb` |
| `io.netty:netty-transport` | `4.2.17.Final` | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `d2409ce3a735cbab4188d0ea5d82abed823a6a07a8389f49af4a9e1243a562bc` |
| `jakarta.activation:jakarta.activation-api` | `2.1.4` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | EDL 1.0 | `c9db52100ce6c8aac95cc39075f95720d2e561b11f8051b81c121ad4effd7004` |
| `jakarta.xml.bind:jakarta.xml.bind-api` | `4.0.4` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Eclipse Distribution License - v 1.0 | `c507ca69a8c6dd11bf4afeec9e0d412c4fa3933fffb0a84680ea5727e8472124` |
| `javax.cache:cache-api` | `1.1.0` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `6c980ad1ae4a6dda3bdb62986c3ef5b41ccf766e12353587ee4e4307e27e155a` |
| `joda-time:joda-time` | `2.14.3` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `43533e9df6431839d0865c897d7d43a9ca75378a52b7f60b3e08b037a5c94986` |
| `net.bytebuddy:byte-buddy` | `1.18.12` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `2ed11da684a8f5b088e0222baa87461cd757e433a8dc4a03671457229d91d5fa` |
| `org.apache.commons:commons-collections4` | `4.6.0` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache-2.0 | `4e0d0896bac5356cead277bac0283f09813e14417e3e84e0c86b573b54d218a3` |
| `org.apache.commons:commons-lang3` | `3.20.0` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache-2.0 | `69e5c9fa35da7a51a5fd2099dfe56a2d8d32cf233e2f6d770e796146440263f4` |
| `org.apache.commons:commons-pool2` | `2.13.1` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache-2.0 | `f77a5060d6936a9144023584232f9de3f2248f1abfe156e9d31795f18770674f` |
| `org.apache.mina:mina-core` | `2.2.9` | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache 2.0 License | `09b4b5e416834e5281dd0dfccac1a10413d6f42c89f133b1c43641e34f33e840` |
| `org.apache.rocketmq:rocketmq-client` | `5.5.0` | direct-or-managed | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `50a00394c13545f6cc03a6dae3708142d81ca11a34fb60583c3bcc507ca21962` |
| `org.apache.rocketmq:rocketmq-common` | `5.5.0` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `74b7942dbc2ac66353717d22c104fcc6bb6e5c65108b1b9120e960e5d7c0b595` |
| `org.apache.rocketmq:rocketmq-remoting` | `5.5.0` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `1273e2c5cd49692ac954f36a40251c7c89cbb7c5093a66bfcfbce7ce597dfd95` |
| `org.apache.rocketmq:rocketmq-rocksdb` | `1.0.6` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License 2.0, GNU General Public License, version 2 | `28944d11fd6e5216b6275eb51eedd253c7f521e565ba44008fe2e5cdca4498a0` |
| `org.apache.tomcat:annotations-api` | `6.0.53` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `253829d3c12b7381d1044fc22c6436cff025fe0d459e4a329413e560a7d0dd13` |
| `org.awaitility:awaitility` | `4.1.0` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache 2.0 | `831b3ec4ce597575dcb25544b3f8e5bb412d62925a2dc6c0163d0636808c8acd` |
| `org.ehcache:ehcache` | `3.12.0` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | The Apache Software License, Version 2.0 | `b8313aeff14294dac6cd0f7d18d2797afb23b1cc7fba452fa2e30369bd9791cf` |
| `org.flywaydb:flyway-core` | `13.3.0` | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `a2892437063f8808be72432c74dd429add4f414e50018d0ea4674ff36fbcbe41` |
| `org.flywaydb:flyway-mysql` | `13.3.0` | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `7ab21a609088550c9ebbcc12f2770d8fbed8e9bab2895b4d2629c35649f6a479` |
| `org.hamcrest:hamcrest` | `2.1` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | BSD Licence 3 | `ba93b2e3a562322ba432f0a1b53addcc55cb188253319a020ed77f824e692050` |
| `org.javassist:javassist` | `3.32.0-GA` | direct-or-managed | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | MPL 1.1, LGPL 2.1, Apache License 2.0 | `712ef75bc3406782bb4529b0408cce8155b53f2124c6ae03d2c5fbfa13d62c1c` |
| `org.jctools:jctools-core` | `4.0.6` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `374fb4660436a70ba131e15ca7bab8423424c737898a894fb276841c94bdd962` |
| `org.json:json` | `20260719` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Public Domain | `c243f45f9590c12694a4142ed3f07fc70dfb71e4daebd05ae234bf92a2da92a6` |
| `org.jspecify:jspecify` | `1.0.0` | transitive | Bootstrap, GameCommon, NJPDK | The Apache License, Version 2.0 | `1fad6e6be7557781e4d33729d49ae1cdc8fdda6fe477bb0cc68ce351eafdfbab` |
| `org.jspecify:jspecify` | `1.0.1` | transitive | SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | The Apache License, Version 2.0 | `070d75f261fe4c5b8202508366715f7f2d4660f88c8ef7e6d3575e48c9683b66` |
| `org.lionsoul:ip2region` | `3.3.7` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | The Apache Software License, Version 2.0 | `0d8f392d55b6fd4acb6b33fc26e851b2717ac870c758dba74b61303efed54cab` |
| `org.mongodb:bson-record-codec` | `5.10.0` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | The Apache License, Version 2.0 | `46c2c0f4863530b08b99927e0aed4f63459a591ad2d2b9778eb2add0cf9ddc4a` |
| `org.mongodb:bson` | `5.10.0` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | The Apache License, Version 2.0 | `1b0a3d8830e89574557cd4067f73d17b72e6e30d8699c529044abe79bd7f0edf` |
| `org.mongodb:mongodb-driver-core` | `5.10.0` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | The Apache License, Version 2.0 | `5fa92c726704a7dd3211874e34db6cb7ddfedfd1ad32883b588267ee077829f1` |
| `org.mongodb:mongodb-driver-sync` | `5.10.0` | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | The Apache License, Version 2.0 | `7a89c4e454ea102efaa8b379068f2d24d191f5f68583622c7764d1ce0d970e38` |
| `org.objenesis:objenesis` | `3.4` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `95488102feaf2e2858adf6b299353677dac6c15294006f8ed1c5556f8e3cd251` |
| `org.quartz-scheduler:quartz` | `2.5.2` | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | The Apache License, Version 2.0 | `452e418739c0da1bff255f7c1343dd3a92e1fdd3ceb126b185939edae9922091` |
| `org.reflections:reflections` | `0.9.11` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | WTFPL, The New BSD License | `cca88428f8a8919df885105833d45ff07bd26f985f96ee55690551216b58b4a1` |
| `org.slf4j:slf4j-api` | `2.0.17` | direct-or-managed | Account, AdminApi, AooKernel, Billing, Bootstrap, Club, ConfigCenter, Families, GameCommon, Gateway, Hall, Mahjong, Matchmaking, NJPDK, Poker, SCJYMJ, Social, Spectator, XCPDK, ZJH, ZYPK, gameServer | MIT | `7b751d952061954d5abfed7181c1f645d336091b679891591d63329c622eb832` |
| `org.yaml:snakeyaml` | `2.0` | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | Apache License, Version 2.0 | `880c9d896e4b74a06c549c15ca496450165d6909fa15d7e662bee8f6a66d7afa` |
| `redis.clients.authentication:redis-authx-core` | `0.1.1-beta2` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | MIT | `cc56edb08b3df8562cddac108dca61907d21cc4ad04de05e9f5d1b01058390af` |
| `redis.clients:jedis` | `8.0.0` | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | MIT | `df9e2d505ae2b577979e7ea0bc8ea6094f01f9a1deabc7c98f675122b87b3c4b` |
| `tools.jackson.core:jackson-core` | `3.1.5` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | The Apache Software License, Version 2.0 | `9431b7fa2673bbb618c11d865fe15e13222fd182a214ff998cb7e56afd8f35d2` |
| `tools.jackson.core:jackson-databind` | `3.1.5` | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | The Apache Software License, Version 2.0 | `3a2338d996fd3056791df8d335fa9ba8a62a706ed4245ecf81b3e583df37d08a` |

## 5. Maven 构建插件（9 项）

| 插件坐标 | 当前声明版本 | 使用位置 | 用途 |
|---|---:|---|---|
| `org.apache.maven.plugins:maven-compiler-plugin` | `3.15.0` | pom.xml, server/Activity/pom.xml, server/AdminApi/pom.xml, server/Bootstrap/pom.xml, server/CDXZMJ/pom.xml, server/Hall/pom.xml, server/IdentityVerification/pom.xml, server/Inventory/pom.xml, server/LegacyAccountServer/pom.xml, server/LegacyGameHall/pom.xml, server/LuckDraw/pom.xml, server/Matchmaking/pom.xml, server/NJPDK/pom.xml, server/Ranking/pom.xml, server/Referral/pom.xml, server/SCJYMJ/pom.xml, server/Social/pom.xml, server/Support/pom.xml, server/Telemetry/pom.xml, server/XCPDK/pom.xml, server/ZJH/pom.xml, server/ZYPK/pom.xml, server/gameServer/pom.xml | Maven build, validation, test, packaging or release lifecycle plugin |
| `org.apache.maven.plugins:maven-dependency-plugin` | `${maven.dependency.plugin.version}, 3.11.0` | pom.xml, server/LegacyAccountServer/pom.xml | Maven build, validation, test, packaging or release lifecycle plugin |
| `org.apache.maven.plugins:maven-enforcer-plugin` | `3.6.3` | pom.xml, server/LegacyAccountServer/pom.xml | Maven build, validation, test, packaging or release lifecycle plugin |
| `org.apache.maven.plugins:maven-jar-plugin` | `3.5.1` | pom.xml, server/Activity/pom.xml, server/AdminApi/pom.xml, server/Bootstrap/pom.xml, server/CDXZMJ/pom.xml, server/Club/pom.xml, server/Gateway/pom.xml, server/Hall/pom.xml, server/IdentityVerification/pom.xml, server/Inventory/pom.xml, server/LegacyAccountServer/pom.xml, server/LegacyGameHall/pom.xml, server/LuckDraw/pom.xml, server/NJPDK/pom.xml, server/Ranking/pom.xml, server/Referral/pom.xml, server/SCJYMJ/pom.xml, server/Support/pom.xml, server/Telemetry/pom.xml, server/XCPDK/pom.xml, server/gameServer/pom.xml | Maven build, validation, test, packaging or release lifecycle plugin |
| `org.apache.maven.plugins:maven-resources-plugin` | `3.4.0` | pom.xml | Maven build, validation, test, packaging or release lifecycle plugin |
| `org.apache.maven.plugins:maven-surefire-plugin` | `3.5.4` | server/LegacyAccountServer/pom.xml | Maven build, validation, test, packaging or release lifecycle plugin |
| `org.codehaus.mojo:build-helper-maven-plugin` | `3.6.1` | server/gameServer/pom.xml | Maven build, validation, test, packaging or release lifecycle plugin |
| `org.codehaus.mojo:exec-maven-plugin` | `3.6.3` | pom.xml, server/AdminApi/pom.xml, server/GameCommon/pom.xml | Maven build, validation, test, packaging or release lifecycle plugin |
| `org.flywaydb:flyway-maven-plugin` | `${flyway.version}` | pom.xml | Maven build, validation, test, packaging or release lifecycle plugin |

## 6. Admin pnpm/npm 锁定包（389 个版本实例）

> `direct-runtime`、`direct-dev` 表示直接声明；`transitive` 表示由上游包带入。传递依赖不应脱离上游包单独升级。

| 包名 | 当前锁定版本 | 关系 | 用途/来源 |
|---|---:|---|---|
| `@asamuzakjp/css-color` | `6.0.7` | transitive | transitive |
| `@asamuzakjp/dom-selector` | `8.3.2` | transitive | transitive |
| `@babel/generator` | `8.0.0` | transitive | transitive |
| `@babel/helper-string-parser` | `7.29.7` | transitive | transitive |
| `@babel/helper-string-parser` | `8.0.0` | transitive | transitive |
| `@babel/helper-validator-identifier` | `7.29.7` | transitive | transitive |
| `@babel/helper-validator-identifier` | `8.0.4` | transitive | transitive |
| `@babel/parser` | `7.29.8` | transitive | transitive |
| `@babel/parser` | `8.0.4` | transitive | transitive |
| `@babel/types` | `7.29.8` | transitive | transitive |
| `@babel/types` | `8.0.4` | transitive | transitive |
| `@bcoe/v8-coverage` | `1.0.2` | transitive | transitive |
| `@bramus/specificity` | `2.4.2` | transitive | transitive |
| `@csstools/color-helpers` | `6.1.1` | transitive | transitive |
| `@csstools/css-calc` | `3.3.0` | transitive | transitive |
| `@csstools/css-color-parser` | `4.2.0` | transitive | transitive |
| `@csstools/css-parser-algorithms` | `4.0.0` | transitive | transitive |
| `@csstools/css-syntax-patches-for-csstree` | `1.1.8` | transitive | transitive |
| `@csstools/css-tokenizer` | `4.0.0` | transitive | transitive |
| `@ctrl/tinycolor` | `4.2.0` | transitive | transitive |
| `@element-plus/icons-vue` | `2.3.2` | direct-runtime, transitive | direct-runtime, transitive |
| `@eslint-community/eslint-utils` | `4.10.1` | transitive | transitive |
| `@eslint-community/regexpp` | `4.12.2` | transitive | transitive |
| `@eslint/config-array` | `0.23.5` | transitive | transitive |
| `@eslint/config-helpers` | `0.7.0` | transitive | transitive |
| `@eslint/core` | `1.2.1` | transitive | transitive |
| `@eslint/object-schema` | `3.0.5` | transitive | transitive |
| `@eslint/plugin-kit` | `0.7.2` | transitive | transitive |
| `@exodus/bytes` | `1.15.1` | transitive | transitive |
| `@floating-ui/core` | `1.8.0` | transitive | transitive |
| `@floating-ui/dom` | `1.8.0` | transitive | transitive |
| `@floating-ui/utils` | `0.2.12` | transitive | transitive |
| `@humanfs/core` | `0.19.2` | transitive | transitive |
| `@humanfs/node` | `0.16.8` | transitive | transitive |
| `@humanfs/types` | `0.15.0` | transitive | transitive |
| `@humanwhocodes/module-importer` | `1.0.1` | transitive | transitive |
| `@humanwhocodes/retry` | `0.4.3` | transitive | transitive |
| `@intlify/core-base` | `11.4.9` | transitive | transitive |
| `@intlify/devtools-types` | `11.4.9` | transitive | transitive |
| `@intlify/message-compiler` | `11.4.9` | transitive | transitive |
| `@intlify/shared` | `11.4.9` | transitive | transitive |
| `@jridgewell/gen-mapping` | `0.3.13` | transitive | transitive |
| `@jridgewell/remapping` | `2.3.5` | transitive | transitive |
| `@jridgewell/resolve-uri` | `3.1.2` | transitive | transitive |
| `@jridgewell/sourcemap-codec` | `1.5.5` | transitive | transitive |
| `@jridgewell/trace-mapping` | `0.3.31` | transitive | transitive |
| `@oxc-project/types` | `0.146.0` | transitive | transitive |
| `@parcel/watcher` | `2.6.0` | transitive | transitive |
| `@parcel/watcher-android-arm64` | `2.6.0` | transitive | transitive |
| `@parcel/watcher-darwin-arm64` | `2.6.0` | transitive | transitive |
| `@parcel/watcher-darwin-x64` | `2.6.0` | transitive | transitive |
| `@parcel/watcher-freebsd-x64` | `2.6.0` | transitive | transitive |
| `@parcel/watcher-linux-arm-glibc` | `2.6.0` | transitive | transitive |
| `@parcel/watcher-linux-arm-musl` | `2.6.0` | transitive | transitive |
| `@parcel/watcher-linux-arm64-glibc` | `2.6.0` | transitive | transitive |
| `@parcel/watcher-linux-arm64-musl` | `2.6.0` | transitive | transitive |
| `@parcel/watcher-linux-x64-glibc` | `2.6.0` | transitive | transitive |
| `@parcel/watcher-linux-x64-musl` | `2.6.0` | transitive | transitive |
| `@parcel/watcher-win32-arm64` | `2.6.0` | transitive | transitive |
| `@parcel/watcher-win32-x64` | `2.6.0` | transitive | transitive |
| `@popperjs/core` | `2.11.8` | transitive | transitive |
| `@rolldown/binding-android-arm-eabi` | `1.2.5` | transitive | transitive |
| `@rolldown/binding-android-arm64` | `1.2.5` | transitive | transitive |
| `@rolldown/binding-darwin-arm64` | `1.2.5` | transitive | transitive |
| `@rolldown/binding-darwin-x64` | `1.2.5` | transitive | transitive |
| `@rolldown/binding-freebsd-x64` | `1.2.5` | transitive | transitive |
| `@rolldown/binding-linux-arm-gnueabihf` | `1.2.5` | transitive | transitive |
| `@rolldown/binding-linux-arm64-gnu` | `1.2.5` | transitive | transitive |
| `@rolldown/binding-linux-arm64-musl` | `1.2.5` | transitive | transitive |
| `@rolldown/binding-linux-ppc64-gnu` | `1.2.5` | transitive | transitive |
| `@rolldown/binding-linux-s390x-gnu` | `1.2.5` | transitive | transitive |
| `@rolldown/binding-linux-x64-gnu` | `1.2.5` | transitive | transitive |
| `@rolldown/binding-linux-x64-musl` | `1.2.5` | transitive | transitive |
| `@rolldown/binding-openharmony-arm64` | `1.2.5` | transitive | transitive |
| `@rolldown/binding-win32-arm64-msvc` | `1.2.5` | transitive | transitive |
| `@rolldown/binding-win32-x64-msvc` | `1.2.5` | transitive | transitive |
| `@rolldown/pluginutils` | `1.0.1` | transitive | transitive |
| `@standard-schema/spec` | `1.1.0` | transitive | transitive |
| `@types/chai` | `5.2.3` | transitive | transitive |
| `@types/deep-eql` | `4.0.2` | transitive | transitive |
| `@types/esrecurse` | `4.3.1` | transitive | transitive |
| `@types/estree` | `1.0.9` | transitive | transitive |
| `@types/jsesc` | `2.5.1` | transitive | transitive |
| `@types/json-schema` | `7.0.15` | transitive | transitive |
| `@types/lodash` | `4.17.25` | transitive | transitive |
| `@types/lodash-es` | `4.17.12` | transitive | transitive |
| `@types/node` | `26.2.0` | direct-development, transitive | direct-development, transitive |
| `@types/nprogress` | `0.2.3` | direct-development | direct-development |
| `@types/qrcode` | `1.5.6` | direct-development | direct-development |
| `@types/sortablejs` | `1.15.9` | direct-development | direct-development |
| `@types/web-bluetooth` | `0.0.21` | transitive | transitive |
| `@typescript-eslint/parser` | `8.67.0` | direct-development, transitive | direct-development, transitive |
| `@typescript-eslint/project-service` | `8.67.0` | transitive | transitive |
| `@typescript-eslint/scope-manager` | `8.67.0` | transitive | transitive |
| `@typescript-eslint/tsconfig-utils` | `8.67.0` | transitive | transitive |
| `@typescript-eslint/types` | `8.67.0` | transitive | transitive |
| `@typescript-eslint/typescript-estree` | `8.67.0` | transitive | transitive |
| `@typescript-eslint/visitor-keys` | `8.67.0` | transitive | transitive |
| `@vitejs/plugin-vue` | `6.0.8` | direct-development | direct-development |
| `@vitest/coverage-v8` | `4.1.11` | direct-development, transitive | direct-development, transitive |
| `@vitest/expect` | `4.1.11` | transitive | transitive |
| `@vitest/mocker` | `4.1.11` | transitive | transitive |
| `@vitest/pretty-format` | `4.1.11` | transitive | transitive |
| `@vitest/runner` | `4.1.11` | transitive | transitive |
| `@vitest/snapshot` | `4.1.11` | transitive | transitive |
| `@vitest/spy` | `4.1.11` | transitive | transitive |
| `@vitest/utils` | `4.1.11` | transitive | transitive |
| `@volar/language-core` | `2.4.28` | transitive | transitive |
| `@volar/source-map` | `2.4.28` | transitive | transitive |
| `@volar/typescript` | `2.4.28` | transitive | transitive |
| `@vue-macros/common` | `3.1.4` | transitive | transitive |
| `@vue/compiler-core` | `3.5.41` | transitive | transitive |
| `@vue/compiler-dom` | `3.5.41` | transitive | transitive |
| `@vue/compiler-sfc` | `3.5.41` | direct-development, transitive | direct-development, transitive |
| `@vue/compiler-ssr` | `3.5.41` | transitive | transitive |
| `@vue/devtools-api` | `6.6.4` | transitive | transitive |
| `@vue/devtools-api` | `8.2.1` | transitive | transitive |
| `@vue/devtools-kit` | `8.2.1` | transitive | transitive |
| `@vue/devtools-shared` | `8.2.1` | transitive | transitive |
| `@vue/language-core` | `3.3.11` | transitive | transitive |
| `@vue/reactivity` | `3.5.41` | transitive | transitive |
| `@vue/runtime-core` | `3.5.41` | transitive | transitive |
| `@vue/runtime-dom` | `3.5.41` | transitive | transitive |
| `@vue/server-renderer` | `3.5.41` | transitive | transitive |
| `@vue/shared` | `3.5.41` | transitive | transitive |
| `@vueuse/core` | `14.4.0` | transitive | transitive |
| `@vueuse/metadata` | `14.4.0` | transitive | transitive |
| `@vueuse/shared` | `14.4.0` | transitive | transitive |
| `acorn` | `8.18.0` | transitive | transitive |
| `acorn-jsx` | `5.3.2` | transitive | transitive |
| `agent-base` | `6.0.2` | transitive | transitive |
| `ajv` | `6.15.0` | transitive | transitive |
| `alien-signals` | `3.2.1` | transitive | transitive |
| `ansi-regex` | `5.0.1` | transitive | transitive |
| `ansi-styles` | `4.3.0` | transitive | transitive |
| `assertion-error` | `2.0.1` | transitive | transitive |
| `ast-kit` | `2.2.0` | transitive | transitive |
| `ast-v8-to-istanbul` | `1.0.5` | transitive | transitive |
| `ast-walker-scope` | `0.9.0` | transitive | transitive |
| `async-validator` | `4.2.5` | transitive | transitive |
| `asynckit` | `0.4.0` | transitive | transitive |
| `axios` | `1.19.0` | direct-runtime | direct-runtime |
| `balanced-match` | `4.0.4` | transitive | transitive |
| `bidi-js` | `1.0.3` | transitive | transitive |
| `birpc` | `2.9.0` | transitive | transitive |
| `boolbase` | `1.0.0` | transitive | transitive |
| `brace-expansion` | `5.0.9` | transitive | transitive |
| `call-bind-apply-helpers` | `1.0.2` | transitive | transitive |
| `call-bound` | `1.0.4` | transitive | transitive |
| `camelcase` | `5.3.1` | transitive | transitive |
| `chai` | `6.2.2` | transitive | transitive |
| `chokidar` | `5.0.0` | transitive | transitive |
| `cliui` | `6.0.0` | transitive | transitive |
| `color-convert` | `2.0.1` | transitive | transitive |
| `color-name` | `1.1.4` | transitive | transitive |
| `combined-stream` | `1.0.8` | transitive | transitive |
| `confbox` | `0.1.8` | transitive | transitive |
| `confbox` | `0.2.4` | transitive | transitive |
| `convert-source-map` | `2.0.0` | transitive | transitive |
| `cross-spawn` | `7.0.6` | transitive | transitive |
| `css-tree` | `3.2.1` | transitive | transitive |
| `cssesc` | `3.0.0` | transitive | transitive |
| `csstype` | `3.2.3` | transitive | transitive |
| `data-urls` | `7.0.0` | transitive | transitive |
| `dayjs` | `1.11.23` | direct-runtime, transitive | direct-runtime, transitive |
| `debug` | `4.4.3` | transitive | transitive |
| `decamelize` | `1.2.0` | transitive | transitive |
| `decimal.js` | `10.6.0` | transitive | transitive |
| `deep-is` | `0.1.4` | transitive | transitive |
| `delayed-stream` | `1.0.0` | transitive | transitive |
| `detect-libc` | `2.1.2` | transitive | transitive |
| `dijkstrajs` | `1.0.3` | transitive | transitive |
| `dunder-proto` | `1.0.1` | transitive | transitive |
| `echarts` | `6.1.0` | direct-runtime | direct-runtime |
| `element-plus` | `2.14.5` | direct-runtime | direct-runtime |
| `emoji-regex` | `8.0.0` | transitive | transitive |
| `entities` | `7.0.1` | transitive | transitive |
| `entities` | `8.0.0` | transitive | transitive |
| `es-define-property` | `1.0.1` | transitive | transitive |
| `es-errors` | `1.3.0` | transitive | transitive |
| `es-module-lexer` | `2.3.2` | transitive | transitive |
| `es-object-atoms` | `1.1.2` | transitive | transitive |
| `es-set-tostringtag` | `2.1.0` | transitive | transitive |
| `escape-string-regexp` | `4.0.0` | transitive | transitive |
| `eslint` | `10.9.0` | direct-development, transitive | direct-development, transitive |
| `eslint-plugin-vue` | `10.10.0` | direct-development | direct-development |
| `eslint-scope` | `9.1.2` | transitive | transitive |
| `eslint-visitor-keys` | `3.4.3` | transitive | transitive |
| `eslint-visitor-keys` | `5.0.1` | transitive | transitive |
| `espree` | `11.2.0` | transitive | transitive |
| `esquery` | `1.7.0` | transitive | transitive |
| `esrecurse` | `4.3.0` | transitive | transitive |
| `estraverse` | `5.3.0` | transitive | transitive |
| `estree-walker` | `2.0.2` | transitive | transitive |
| `estree-walker` | `3.0.3` | transitive | transitive |
| `esutils` | `2.0.3` | transitive | transitive |
| `expect-type` | `1.4.0` | transitive | transitive |
| `exsolve` | `1.1.1` | transitive | transitive |
| `fast-deep-equal` | `3.1.3` | transitive | transitive |
| `fast-json-stable-stringify` | `2.1.0` | transitive | transitive |
| `fast-levenshtein` | `2.0.6` | transitive | transitive |
| `fdir` | `6.5.0` | transitive | transitive |
| `file-entry-cache` | `8.0.0` | transitive | transitive |
| `find-up` | `4.1.0` | transitive | transitive |
| `find-up` | `5.0.0` | transitive | transitive |
| `flat-cache` | `4.0.1` | transitive | transitive |
| `flatted` | `3.4.4` | transitive | transitive |
| `follow-redirects` | `1.16.0` | transitive | transitive |
| `form-data` | `4.0.6` | transitive | transitive |
| `fsevents` | `2.3.3` | transitive | transitive |
| `function-bind` | `1.1.2` | transitive | transitive |
| `get-caller-file` | `2.0.5` | transitive | transitive |
| `get-intrinsic` | `1.3.0` | transitive | transitive |
| `get-proto` | `1.0.1` | transitive | transitive |
| `glob-parent` | `6.0.2` | transitive | transitive |
| `gopd` | `1.2.0` | transitive | transitive |
| `has-flag` | `4.0.0` | transitive | transitive |
| `has-symbols` | `1.1.0` | transitive | transitive |
| `has-tostringtag` | `1.0.2` | transitive | transitive |
| `hasown` | `2.0.4` | transitive | transitive |
| `hookable` | `5.5.3` | transitive | transitive |
| `html-encoding-sniffer` | `6.0.0` | transitive | transitive |
| `html-escaper` | `2.0.2` | transitive | transitive |
| `https-proxy-agent` | `5.0.1` | transitive | transitive |
| `ignore` | `5.3.2` | transitive | transitive |
| `immutable` | `5.1.9` | transitive | transitive |
| `imurmurhash` | `0.1.4` | transitive | transitive |
| `is-extglob` | `2.1.1` | transitive | transitive |
| `is-fullwidth-code-point` | `3.0.0` | transitive | transitive |
| `is-glob` | `4.0.3` | transitive | transitive |
| `is-potential-custom-element-name` | `1.0.1` | transitive | transitive |
| `isexe` | `2.0.0` | transitive | transitive |
| `istanbul-lib-coverage` | `3.2.2` | transitive | transitive |
| `istanbul-lib-report` | `3.0.1` | transitive | transitive |
| `istanbul-reports` | `3.2.0` | transitive | transitive |
| `js-cookie` | `3.0.8` | direct-runtime | direct-runtime |
| `js-md5` | `0.9.2` | direct-runtime | direct-runtime |
| `js-table2excel` | `1.1.2` | direct-runtime | direct-runtime |
| `js-tokens` | `10.0.0` | transitive | transitive |
| `jsdom` | `30.0.1` | direct-development, transitive | direct-development, transitive |
| `jsesc` | `3.1.0` | transitive | transitive |
| `json-buffer` | `3.0.1` | transitive | transitive |
| `json-schema-traverse` | `0.4.1` | transitive | transitive |
| `json-stable-stringify-without-jsonify` | `1.0.1` | transitive | transitive |
| `json5` | `2.2.3` | transitive | transitive |
| `keyv` | `4.5.4` | transitive | transitive |
| `levn` | `0.4.1` | transitive | transitive |
| `lightningcss` | `1.33.0` | transitive | transitive |
| `lightningcss-android-arm64` | `1.33.0` | transitive | transitive |
| `lightningcss-darwin-arm64` | `1.33.0` | transitive | transitive |
| `lightningcss-darwin-x64` | `1.33.0` | transitive | transitive |
| `lightningcss-freebsd-x64` | `1.33.0` | transitive | transitive |
| `lightningcss-linux-arm-gnueabihf` | `1.33.0` | transitive | transitive |
| `lightningcss-linux-arm64-gnu` | `1.33.0` | transitive | transitive |
| `lightningcss-linux-arm64-musl` | `1.33.0` | transitive | transitive |
| `lightningcss-linux-x64-gnu` | `1.33.0` | transitive | transitive |
| `lightningcss-linux-x64-musl` | `1.33.0` | transitive | transitive |
| `lightningcss-win32-arm64-msvc` | `1.33.0` | transitive | transitive |
| `lightningcss-win32-x64-msvc` | `1.33.0` | transitive | transitive |
| `local-pkg` | `1.2.1` | transitive | transitive |
| `locate-path` | `5.0.0` | transitive | transitive |
| `locate-path` | `6.0.0` | transitive | transitive |
| `lodash` | `4.18.1` | direct-runtime, transitive | direct-runtime, transitive |
| `lodash-es` | `4.18.1` | transitive | transitive |
| `lodash-unified` | `1.0.3` | transitive | transitive |
| `lru-cache` | `11.5.2` | transitive | transitive |
| `magic-string` | `0.30.21` | transitive | transitive |
| `magic-string-ast` | `1.0.3` | transitive | transitive |
| `magicast` | `0.5.4` | transitive | transitive |
| `make-dir` | `4.0.0` | transitive | transitive |
| `math-intrinsics` | `1.1.0` | transitive | transitive |
| `mdn-data` | `2.27.1` | transitive | transitive |
| `memoize-one` | `6.0.0` | transitive | transitive |
| `mime-db` | `1.52.0` | transitive | transitive |
| `mime-types` | `2.1.35` | transitive | transitive |
| `minimatch` | `10.2.6` | transitive | transitive |
| `mitt` | `3.0.1` | direct-runtime | direct-runtime |
| `mlly` | `1.8.2` | transitive | transitive |
| `ms` | `2.1.3` | transitive | transitive |
| `muggle-string` | `0.4.1` | transitive | transitive |
| `nanoid` | `3.3.18` | transitive | transitive |
| `natural-compare` | `1.4.0` | transitive | transitive |
| `node-addon-api` | `7.1.1` | transitive | transitive |
| `normalize-wheel-es` | `1.2.0` | transitive | transitive |
| `nostics` | `1.2.0` | transitive | transitive |
| `nprogress` | `0.2.0` | direct-runtime | direct-runtime |
| `nth-check` | `2.1.1` | transitive | transitive |
| `object-inspect` | `1.13.4` | transitive | transitive |
| `obug` | `2.1.4` | transitive | transitive |
| `optionator` | `0.9.4` | transitive | transitive |
| `p-limit` | `2.3.0` | transitive | transitive |
| `p-limit` | `3.1.0` | transitive | transitive |
| `p-locate` | `4.1.0` | transitive | transitive |
| `p-locate` | `5.0.0` | transitive | transitive |
| `p-try` | `2.2.0` | transitive | transitive |
| `parse5` | `8.0.1` | transitive | transitive |
| `path-browserify` | `1.0.1` | transitive | transitive |
| `path-exists` | `4.0.0` | transitive | transitive |
| `path-key` | `3.1.1` | transitive | transitive |
| `pathe` | `2.0.3` | transitive | transitive |
| `perfect-debounce` | `2.1.0` | transitive | transitive |
| `picocolors` | `1.1.1` | transitive | transitive |
| `picomatch` | `4.0.5` | transitive | transitive |
| `pinia` | `4.0.3` | direct-runtime, transitive | direct-runtime, transitive |
| `pkg-types` | `1.3.1` | transitive | transitive |
| `pkg-types` | `2.3.1` | transitive | transitive |
| `pngjs` | `5.0.0` | transitive | transitive |
| `postcss` | `8.5.26` | transitive | transitive |
| `postcss-selector-parser` | `7.1.5` | transitive | transitive |
| `prelude-ls` | `1.2.1` | transitive | transitive |
| `print-js` | `1.6.0` | direct-runtime | direct-runtime |
| `proxy-from-env` | `2.1.0` | transitive | transitive |
| `punycode` | `2.3.1` | transitive | transitive |
| `qrcode` | `1.5.4` | direct-runtime | direct-runtime |
| `qs` | `6.15.3` | direct-runtime | direct-runtime |
| `quansync` | `0.2.11` | transitive | transitive |
| `readdirp` | `5.1.1` | transitive | transitive |
| `require-directory` | `2.1.1` | transitive | transitive |
| `require-from-string` | `2.0.2` | transitive | transitive |
| `require-main-filename` | `2.0.0` | transitive | transitive |
| `rolldown` | `1.2.5` | transitive | transitive |
| `sass` | `1.103.1` | direct-development, transitive | direct-development, transitive |
| `saxes` | `6.0.0` | transitive | transitive |
| `screenfull` | `6.0.2` | direct-runtime | direct-runtime |
| `scule` | `1.3.0` | transitive | transitive |
| `semver` | `7.8.5` | transitive | transitive |
| `set-blocking` | `2.0.0` | transitive | transitive |
| `shebang-command` | `2.0.0` | transitive | transitive |
| `shebang-regex` | `3.0.0` | transitive | transitive |
| `side-channel` | `1.1.1` | transitive | transitive |
| `side-channel-list` | `1.0.1` | transitive | transitive |
| `side-channel-map` | `1.0.1` | transitive | transitive |
| `side-channel-weakmap` | `1.0.2` | transitive | transitive |
| `siginfo` | `2.0.0` | transitive | transitive |
| `sortablejs` | `1.15.7` | direct-runtime | direct-runtime |
| `source-map-js` | `1.2.1` | transitive | transitive |
| `stackback` | `0.0.2` | transitive | transitive |
| `std-env` | `4.2.0` | transitive | transitive |
| `string-width` | `4.2.3` | transitive | transitive |
| `strip-ansi` | `6.0.1` | transitive | transitive |
| `supports-color` | `7.2.0` | transitive | transitive |
| `symbol-tree` | `3.2.4` | transitive | transitive |
| `tinybench` | `2.9.0` | transitive | transitive |
| `tinyexec` | `1.3.0` | transitive | transitive |
| `tinyglobby` | `0.2.17` | transitive | transitive |
| `tinyrainbow` | `3.1.1` | transitive | transitive |
| `tldts` | `7.4.10` | transitive | transitive |
| `tldts-core` | `7.4.10` | transitive | transitive |
| `tough-cookie` | `6.0.2` | transitive | transitive |
| `tr46` | `6.0.0` | transitive | transitive |
| `ts-api-utils` | `2.5.0` | transitive | transitive |
| `tslib` | `2.3.0` | transitive | transitive |
| `type-check` | `0.4.0` | transitive | transitive |
| `typescript` | `6.0.3` | direct-development, transitive | direct-development, transitive |
| `ufo` | `1.6.4` | transitive | transitive |
| `undici` | `8.10.0` | transitive | transitive |
| `undici-types` | `8.3.0` | transitive | transitive |
| `unplugin` | `3.3.0` | transitive | transitive |
| `unplugin-utils` | `0.3.2` | transitive | transitive |
| `uri-js` | `4.4.1` | transitive | transitive |
| `util-deprecate` | `1.0.2` | transitive | transitive |
| `vite` | `8.2.2` | direct-development, transitive | direct-development, transitive |
| `vitest` | `4.1.11` | direct-development, transitive | direct-development, transitive |
| `vscode-uri` | `3.1.0` | transitive | transitive |
| `vue` | `3.5.41` | direct-runtime, transitive | direct-runtime, transitive |
| `vue-component-type-helpers` | `3.3.11` | transitive | transitive |
| `vue-eslint-parser` | `10.4.1` | direct-development, transitive | direct-development, transitive |
| `vue-i18n` | `11.4.9` | direct-runtime | direct-runtime |
| `vue-router` | `5.2.0` | direct-runtime | direct-runtime |
| `vue-tsc` | `3.3.11` | direct-development | direct-development |
| `w3c-xmlserializer` | `5.0.0` | transitive | transitive |
| `webidl-conversions` | `8.0.1` | transitive | transitive |
| `webpack-virtual-modules` | `0.6.2` | transitive | transitive |
| `whatwg-mimetype` | `5.0.0` | transitive | transitive |
| `whatwg-url` | `16.0.1` | transitive | transitive |
| `whatwg-url` | `17.1.0` | transitive | transitive |
| `which` | `2.0.2` | transitive | transitive |
| `which-module` | `2.0.1` | transitive | transitive |
| `why-is-node-running` | `2.3.0` | transitive | transitive |
| `word-wrap` | `1.2.5` | transitive | transitive |
| `wrap-ansi` | `6.2.0` | transitive | transitive |
| `xml-name-validator` | `5.0.0` | transitive | transitive |
| `xmlchars` | `2.2.0` | transitive | transitive |
| `y18n` | `4.0.3` | transitive | transitive |
| `yaml` | `2.9.0` | transitive | transitive |
| `yargs` | `15.4.1` | transitive | transitive |
| `yargs-parser` | `18.1.3` | transitive | transitive |
| `yocto-queue` | `0.1.0` | transitive | transitive |
| `zrender` | `6.1.0` | transitive | transitive |

## 7. Client / Cocos Creator 插件与 npm 边界

- Client npm 生产依赖：`0`。Cocos 的 `cc` API 由 Creator 3.8.8 引擎提供，不作为项目 npm 包重复安装。
- 项目插件：
  - `adsense-h5g-plugin`：enabled=`false`，engine=`3.8.8`，status=`disabled-not-runtime`。

## 8. 当前治理决策与保留边界

### `safeAutomaticUpgradesApplied`

- 无。

### `retainedForCompatibility`

- JDK/Node/Cocos pinned lines
- BOM-managed Java graph
- pnpm peer-compatible lock

### `redundantRemoved`

- unused RocketMQ OpenTelemetry/gRPC/Prometheus transitives
- unused web-mobile build template

### `knownVulnerabilities`

- 无。

### `licenseRisks`

- 无。

## 9. 当前验证状态

- `serverCleanVerify`：PASS
- `clientFrozenAuditRuntime`：PASS
- `adminFrozenQualityAudit`：PASS

## 10. 给复审 AI 的审计要求

1. 以“当前实际版本”列为审计基线，不得把 latestStableVersion 当成项目已使用版本。
2. 分别审计 JDK/Maven、服务端直接依赖、服务端传递依赖、Maven 插件、Admin 直接依赖、Admin 传递依赖、Cocos 引擎边界、数据库与中间件。
3. 每项升级建议必须同时说明兼容性、迁移成本、安全收益、许可证变化、性能影响、回滚方法和所需验证。
4. 不得建议单独强升传递依赖；应优先升级其直接上游、BOM 或框架版本。
5. 不得把 Creator 引擎内置模块改成重复 npm 依赖，不得启用当前禁用的项目插件，除非有明确业务依据。
6. 对“保留但仅兼容使用”的依赖，应验证真实消费者后再建议删除；禁止仅凭名称判断冗余。
7. 复审输出需区分：必须立即处理、建议升级、保持锁定、传递依赖随上游处理、可删除候选、需要运行验证。

## 11. 可追溯来源

- 完整机器清单：`Server/docs/generated/aoo-project-latest-stable-dependency-audit.json`
- 依赖治理终验：`docs/verification/CUR-04-dependency-final.md`
- 项目版本稳定性审计：`docs/Aoo-全项目版本插件依赖清单与最新稳定性审计.md`
