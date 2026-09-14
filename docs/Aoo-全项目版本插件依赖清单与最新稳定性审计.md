# Aoo 全项目版本、插件、依赖清单与最新稳定性审计

查询日期：2026-08-25（官方源在线核验）

## 单一结论

当前锁定组合通过真实全链回归。没有未处置漏洞或许可证缺口；发现的版本差异按 peer、engine、BOM、数据库和运行兼容矩阵保留，不能仅因 registry 存在更高版本即升级。完整逐组件字段见同目录机器可读 JSON。

## 汇总

- Maven 活动组件：112（直接/管理与必要传递均逐项列入 JSON）。
- Maven 构建插件坐标：9。
- Admin pnpm 锁定包版本实例：389；Client 无 npm 生产依赖。
- 活动 Maven 漏洞：0；许可证缺失：0。

## 工具链、引擎、容器与迁移

| 项目 | 当前锁定 | 最新稳定/支持线 | 用途 | 判定 | 官方来源 |
|---|---|---|---|---|---|
| Oracle JDK | 25.0.4.1 | 25.0.4.1 | Server compile/runtime, release=25 | 已锁定受支持线 | https://www.oracle.com/java/technologies/javase/jdk25-archive-downloads.html |
| Apache Maven Wrapper | 3.9.16 / wrapper 3.3.4 | 3.9.16 / wrapper 3.3.4 | reproducible Maven lifecycle | 已最新 | https://maven.apache.org/wrapper/ |
| Node.js | 24.19.0 | 24.x LTS | Admin tooling | 因 LTS 与 Vite/Vitest engine 兼容保留 | https://nodejs.org/en/about/previous-releases |
| pnpm | 11.19.0 | 11.19.0 | Admin/Client frozen lock replay | 锁文件兼容线 | https://registry.npmjs.org/pnpm |
| Cocos Creator | 3.8.8 | 3.8.8 | Client engine/editor/build pipeline | 项目 engine 锁定；不可脱离 Creator 兼容矩阵盲升 | https://docs.cocos.com/creator/3.8/manual/en/ |
| MySQL container | 8.0 | 8.0 supported line | migration and integration verification | 因 SQL compatibility baseline 保留 | https://dev.mysql.com/doc/relnotes/mysql/8.0/en/ |
| RocketMQ container/client | 5.3.2 / 5.5.0 | official 5.x line | outbox and MQ runtime | 客户端按 Maven 兼容矩阵管理 | https://rocketmq.apache.org/release-notes/ |
| Redis container | 7.4-alpine | 7.4 supported line | cache runtime | 部署基线保留 | https://redis.io/docs/latest/operate/oss_and_stack/stack-with-enterprise/release-notes/ |
| MongoDB Server container | mongo:8.0.29-noble | 8.0 LTS series | production/default document persistence runtime | 生产基线精确锁定 8.0.29；8.3.8 仅可作为非默认未来兼容测试目标 | https://www.mongodb.com/docs/manual/release-notes/8.0/ |
| Flyway migrations | 13.3.0 | 13.3.0 | sole schema migration authority | 由 Maven compatibility matrix 管理 | https://documentation.red-gate.com/flyway/reference/release-notes |

## 上下游兼容矩阵

| 上游 | 下游 | 结论 |
|---|---|---|
| JDK 25 | maven-compiler release 25 / Lombok / Error Prone annotations | clean verify PASS; retain pinned JDK patch line |
| Node 24.19 + pnpm 11.19 | Vite 8.2.2 / Vitest 4.1.11 / ESLint 10 / vue-tsc | peer/engine compatible; Admin full quality PASS |
| Vue 3.5.41 | plugin-vue 6.0.8 / Router 5.2.0 / Pinia 4.0.3 / Element Plus 2.14.5 | peer constraints satisfied |
| Cocos Creator 3.8.8 | Client cc imports, project manifest, web-desktop template | engine-owned modules; no npm runtime substitution |
| MySQL 8.0 + Flyway | 87 migrations and JDBC integration tests | strict UTC/utf8mb4 migration + clean verify PASS |
| RocketMQ 5.5 client | GameCommon/gameServer outbox | unused telemetry transitives excluded; runtime feature retained |
| MongoDB Server 8.0.29 | MongoDB Java Driver 5.10.0 | 生产运行组合；驱动保持 5.10.0，8.3.8 仅限显式未来兼容验证 |

## Maven 直接、管理与必要传递依赖

| 坐标 | 当前 | 最新稳定 | 关系 | 使用位置/作用 | 判定 | 官方源 |
|---|---:|---:|---|---|---|---|
| at.yawk.lz4:lz4-java | 1.11.2 | 1.11.2 | direct-or-managed | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/at/yawk/lz4/lz4-java/maven-metadata.xml |
| ch.qos.logback:logback-classic | 1.6.3 | 1.6.3 | direct-or-managed | AooKernel, Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/ch/qos/logback/logback-classic/maven-metadata.xml |
| ch.qos.logback:logback-core | 1.6.3 | 1.6.3 | transitive | AooKernel, Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/ch/qos/logback/logback-core/maven-metadata.xml |
| com.alibaba.fastjson2:fastjson2-extension | 2.0.64 | 2.0.64 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/alibaba/fastjson2/fastjson2-extension/maven-metadata.xml |
| com.alibaba.fastjson2:fastjson2 | 2.0.64 | 2.0.64.android8 | direct-or-managed | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 因兼容矩阵保留 | https://repo.maven.apache.org/maven2/com/alibaba/fastjson2/fastjson2/maven-metadata.xml |
| com.alibaba:druid | 1.2.28 | 1.2.28 | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/alibaba/druid/maven-metadata.xml |
| com.alibaba:fastjson | 2.0.64 | 2.0.64 | direct-or-managed | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/alibaba/fastjson/maven-metadata.xml |
| com.esotericsoftware:kryo | 5.6.2 | 5.6.2 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/esotericsoftware/kryo/maven-metadata.xml |
| com.esotericsoftware:minlog | 1.3.1 | 1.3.1 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/esotericsoftware/minlog/maven-metadata.xml |
| com.esotericsoftware:reflectasm | 1.11.9 | 1.11.9 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/esotericsoftware/reflectasm/maven-metadata.xml |
| com.fasterxml.jackson.core:jackson-annotations | 2.22 | 2.22 | transitive | Account, Activity, AdminApi, Billing, Bootstrap, Club, ConfigCenter, Families, GameCommon, Gateway, Gifting, Hall, IdentityVerification, Inventory, Location, LuckDraw, Mahjong, Matchmaking, Media, NJPDK, PlayerProfile, Poker, Privacy, Ranking, Referral, RoomSafety, SCJYMJ, Share, Social, Spectator, Support, Telemetry, VersionNotice, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/fasterxml/jackson/core/jackson-annotations/maven-metadata.xml |
| com.fasterxml.jackson.core:jackson-core | 2.22.2 | 2.22.2 | transitive | Account, Activity, AdminApi, Billing, Bootstrap, Club, ConfigCenter, Families, GameCommon, Gateway, Gifting, Hall, IdentityVerification, Inventory, Location, LuckDraw, Mahjong, Matchmaking, Media, NJPDK, PlayerProfile, Poker, Privacy, Ranking, Referral, RoomSafety, SCJYMJ, Share, Social, Spectator, Support, Telemetry, VersionNotice, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/fasterxml/jackson/core/jackson-core/maven-metadata.xml |
| com.fasterxml.jackson.core:jackson-databind | 2.22.2 | 2.22.2 | transitive | Account, Activity, AdminApi, Billing, Bootstrap, Club, ConfigCenter, Families, GameCommon, Gateway, Gifting, Hall, IdentityVerification, Inventory, Location, LuckDraw, Matchmaking, Media, NJPDK, PlayerProfile, Privacy, Ranking, Referral, RoomSafety, SCJYMJ, Share, Social, Spectator, Support, Telemetry, VersionNotice, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/fasterxml/jackson/core/jackson-databind/maven-metadata.xml |
| com.fasterxml.jackson.datatype:jackson-datatype-jsr310 | 2.22.2 | 2.22.2 | transitive | Account, AdminApi, Billing, Bootstrap, Club, ConfigCenter, Families, GameCommon, Gateway, Hall, IdentityVerification, Location, Mahjong, Matchmaking, NJPDK, Poker, SCJYMJ, Social, Spectator, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/fasterxml/jackson/datatype/jackson-datatype-jsr310/maven-metadata.xml |
| com.github.luben:zstd-jni | 1.5.2-2 | 1.5.7-15 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/com/github/luben/zstd-jni/maven-metadata.xml |
| com.google.code.gson:gson | 2.14.0 | 2.14.0 | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/google/code/gson/gson/maven-metadata.xml |
| com.google.errorprone:error_prone_annotations | 2.50.0 | 2.50.0 | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/google/errorprone/error_prone_annotations/maven-metadata.xml |
| com.google.guava:failureaccess | 1.0.3 | 1.0.3 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/google/guava/failureaccess/maven-metadata.xml |
| com.google.guava:guava | 33.7.1-jre | 33.7.1-jre | direct-or-managed | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/google/guava/guava/maven-metadata.xml |
| com.google.guava:listenablefuture | 9999.0-empty-to-avoid-conflict-with-guava | 9999.0-empty-to-avoid-conflict-with-guava | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/google/guava/listenablefuture/maven-metadata.xml |
| com.google.j2objc:j2objc-annotations | 3.1 | 3.1 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/google/j2objc/j2objc-annotations/maven-metadata.xml |
| com.google.protobuf:protobuf-java | 4.36.0 | 4.36.0 | direct-or-managed | Account, AdminApi, Billing, Bootstrap, Club, Gateway, Hall, Location, Matchmaking, Media, NJPDK, PlayerProfile, RoomSafety, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/google/protobuf/protobuf-java/maven-metadata.xml |
| com.lmax:disruptor | 4.0.0 | 4.0.0 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/lmax/disruptor/maven-metadata.xml |
| com.mysql:mysql-connector-j | 26.7.0 | 26.7.0 | direct-or-managed | Account, AdminApi, Billing, Bootstrap, Club, Gateway, Hall, Location, Matchmaking, Media, NJPDK, PlayerProfile, RoomSafety, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/maven-metadata.xml |
| com.squareup.okio:okio-jvm | 3.18.1 | 3.18.1 | direct-or-managed | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/com/squareup/okio/okio-jvm/maven-metadata.xml |
| commons-beanutils:commons-beanutils | 1.11.0 | 1.11.0 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/commons-beanutils/commons-beanutils/maven-metadata.xml |
| commons-codec:commons-codec | 1.22.1 | 1.22.1 | direct-or-managed | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/commons-codec/commons-codec/maven-metadata.xml |
| commons-collections:commons-collections | 3.2.2 | 20040616 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/commons-collections/commons-collections/maven-metadata.xml |
| commons-dbutils:commons-dbutils | 1.8.1 | 1.8.1 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/commons-dbutils/commons-dbutils/maven-metadata.xml |
| commons-digester:commons-digester | 2.1 | 2.1 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/commons-digester/commons-digester/maven-metadata.xml |
| commons-logging:commons-logging | 1.3.5 | 1.4.0 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/commons-logging/commons-logging/maven-metadata.xml |
| commons-validator:commons-validator | 1.10.0 | 1.11.0 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/commons-validator/commons-validator/maven-metadata.xml |
| io.github.aliyunmq:rocketmq-logback-classic | 1.0.1 | 1.0.1 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/github/aliyunmq/rocketmq-logback-classic/maven-metadata.xml |
| io.github.aliyunmq:rocketmq-slf4j-api | 1.0.1 | 1.0.1 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/github/aliyunmq/rocketmq-slf4j-api/maven-metadata.xml |
| io.netty:netty-all | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-all/maven-metadata.xml |
| io.netty:netty-buffer | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-buffer/maven-metadata.xml |
| io.netty:netty-codec-base | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-base/maven-metadata.xml |
| io.netty:netty-codec-classes-quic | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-classes-quic/maven-metadata.xml |
| io.netty:netty-codec-compression | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-compression/maven-metadata.xml |
| io.netty:netty-codec-dns | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-dns/maven-metadata.xml |
| io.netty:netty-codec-haproxy | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-haproxy/maven-metadata.xml |
| io.netty:netty-codec-http2 | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-http2/maven-metadata.xml |
| io.netty:netty-codec-http3 | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-http3/maven-metadata.xml |
| io.netty:netty-codec-http | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-http/maven-metadata.xml |
| io.netty:netty-codec-marshalling | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-marshalling/maven-metadata.xml |
| io.netty:netty-codec-memcache | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-memcache/maven-metadata.xml |
| io.netty:netty-codec-mqtt | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-mqtt/maven-metadata.xml |
| io.netty:netty-codec-native-quic | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-native-quic/maven-metadata.xml |
| io.netty:netty-codec-protobuf | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-protobuf/maven-metadata.xml |
| io.netty:netty-codec-redis | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-redis/maven-metadata.xml |
| io.netty:netty-codec-smtp | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-smtp/maven-metadata.xml |
| io.netty:netty-codec-socks | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-socks/maven-metadata.xml |
| io.netty:netty-codec-stomp | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-stomp/maven-metadata.xml |
| io.netty:netty-codec-xml | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec-xml/maven-metadata.xml |
| io.netty:netty-codec | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-codec/maven-metadata.xml |
| io.netty:netty-common | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-common/maven-metadata.xml |
| io.netty:netty-handler-proxy | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-handler-proxy/maven-metadata.xml |
| io.netty:netty-handler-ssl-ocsp | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-handler-ssl-ocsp/maven-metadata.xml |
| io.netty:netty-handler | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-handler/maven-metadata.xml |
| io.netty:netty-resolver-dns-classes-macos | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-resolver-dns-classes-macos/maven-metadata.xml |
| io.netty:netty-resolver-dns-native-macos | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-resolver-dns-native-macos/maven-metadata.xml |
| io.netty:netty-resolver-dns | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-resolver-dns/maven-metadata.xml |
| io.netty:netty-resolver | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-resolver/maven-metadata.xml |
| io.netty:netty-transport-classes-epoll | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-transport-classes-epoll/maven-metadata.xml |
| io.netty:netty-transport-classes-io_uring | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-transport-classes-io_uring/maven-metadata.xml |
| io.netty:netty-transport-classes-kqueue | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-transport-classes-kqueue/maven-metadata.xml |
| io.netty:netty-transport-native-epoll | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-transport-native-epoll/maven-metadata.xml |
| io.netty:netty-transport-native-io_uring | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-transport-native-io_uring/maven-metadata.xml |
| io.netty:netty-transport-native-kqueue | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-transport-native-kqueue/maven-metadata.xml |
| io.netty:netty-transport-native-unix-common | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-transport-native-unix-common/maven-metadata.xml |
| io.netty:netty-transport-rxtx | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-transport-rxtx/maven-metadata.xml |
| io.netty:netty-transport-sctp | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-transport-sctp/maven-metadata.xml |
| io.netty:netty-transport-udt | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-transport-udt/maven-metadata.xml |
| io.netty:netty-transport | 4.2.17.Final | 4.2.17.Final | transitive | Bootstrap, GameCommon, Gateway, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/io/netty/netty-transport/maven-metadata.xml |
| jakarta.activation:jakarta.activation-api | 2.1.4 | 2.2.0-M2 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/jakarta/activation/jakarta.activation-api/maven-metadata.xml |
| jakarta.xml.bind:jakarta.xml.bind-api | 4.0.4 | 4.1.0-M1 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/jakarta/xml/bind/jakarta.xml.bind-api/maven-metadata.xml |
| javax.cache:cache-api | 1.1.0 | 1.1.1 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/javax/cache/cache-api/maven-metadata.xml |
| joda-time:joda-time | 2.14.3 | 2.14.3 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/joda-time/joda-time/maven-metadata.xml |
| net.bytebuddy:byte-buddy | 1.18.12 | 1.18.12-jdk5 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/net/bytebuddy/byte-buddy/maven-metadata.xml |
| org.apache.commons:commons-collections4 | 4.6.0 | 4.6.0 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/apache/commons/commons-collections4/maven-metadata.xml |
| org.apache.commons:commons-lang3 | 3.20.0 | 3.20.0 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/apache/commons/commons-lang3/maven-metadata.xml |
| org.apache.commons:commons-pool2 | 2.13.1 | 2.13.1 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/apache/commons/commons-pool2/maven-metadata.xml |
| org.apache.mina:mina-core | 2.2.9 | 3.0.0-M2 | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 因兼容矩阵保留 | https://repo.maven.apache.org/maven2/org/apache/mina/mina-core/maven-metadata.xml |
| org.apache.rocketmq:rocketmq-client | 5.5.0 | 5.5.0 | direct-or-managed | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/apache/rocketmq/rocketmq-client/maven-metadata.xml |
| org.apache.rocketmq:rocketmq-common | 5.5.0 | 5.5.0 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/apache/rocketmq/rocketmq-common/maven-metadata.xml |
| org.apache.rocketmq:rocketmq-remoting | 5.5.0 | 5.5.0 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/apache/rocketmq/rocketmq-remoting/maven-metadata.xml |
| org.apache.rocketmq:rocketmq-rocksdb | 1.0.6 | 1.0.6 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/apache/rocketmq/rocketmq-rocksdb/maven-metadata.xml |
| org.apache.tomcat:annotations-api | 6.0.53 | 6.0.53 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/apache/tomcat/annotations-api/maven-metadata.xml |
| org.awaitility:awaitility | 4.1.0 | 4.3.0 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/org/awaitility/awaitility/maven-metadata.xml |
| org.ehcache:ehcache | 3.12.0 | 3.12.0 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/ehcache/ehcache/maven-metadata.xml |
| org.flywaydb:flyway-core | 13.3.0 | 13.3.0 | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/flywaydb/flyway-core/maven-metadata.xml |
| org.flywaydb:flyway-mysql | 13.3.0 | 13.3.0 | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/flywaydb/flyway-mysql/maven-metadata.xml |
| org.hamcrest:hamcrest | 2.1 | 3.0 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/org/hamcrest/hamcrest/maven-metadata.xml |
| org.javassist:javassist | 3.32.0-GA | 3.33.0-GA | direct-or-managed | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 因兼容矩阵保留 | https://repo.maven.apache.org/maven2/org/javassist/javassist/maven-metadata.xml |
| org.jctools:jctools-core | 4.0.6 | 4.0.7 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/org/jctools/jctools-core/maven-metadata.xml |
| org.json:json | 20260719 | 20260814 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/org/json/json/maven-metadata.xml |
| org.jspecify:jspecify | 1.0.0 | 1.0.1 | transitive | Bootstrap, GameCommon, NJPDK | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/org/jspecify/jspecify/maven-metadata.xml |
| org.jspecify:jspecify | 1.0.1 | 1.0.1 | transitive | SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/jspecify/jspecify/maven-metadata.xml |
| org.lionsoul:ip2region | 3.3.7 | 3.3.7 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/lionsoul/ip2region/maven-metadata.xml |
| org.mongodb:bson-record-codec | 5.10.0 | 5.10.0 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/mongodb/bson-record-codec/maven-metadata.xml |
| org.mongodb:bson | 5.10.0 | 5.10.0 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/mongodb/bson/maven-metadata.xml |
| org.mongodb:mongodb-driver-core | 5.10.0 | 5.10.0 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/mongodb/mongodb-driver-core/maven-metadata.xml |
| org.mongodb:mongodb-driver-sync | 5.10.0 | 5.10.0 | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/mongodb/mongodb-driver-sync/maven-metadata.xml |
| org.objenesis:objenesis | 3.4 | 3.6 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/org/objenesis/objenesis/maven-metadata.xml |
| org.quartz-scheduler:quartz | 2.5.2 | 2.5.2 | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/org/quartz-scheduler/quartz/maven-metadata.xml |
| org.reflections:reflections | 0.9.11 | 0.10.2 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/org/reflections/reflections/maven-metadata.xml |
| org.slf4j:slf4j-api | 2.0.17 | 2.0.18 | direct-or-managed | Account, AdminApi, AooKernel, Billing, Bootstrap, Club, ConfigCenter, Families, GameCommon, Gateway, Hall, Mahjong, Matchmaking, NJPDK, Poker, SCJYMJ, Social, Spectator, XCPDK, ZJH, ZYPK, gameServer | 因兼容矩阵保留 | https://repo.maven.apache.org/maven2/org/slf4j/slf4j-api/maven-metadata.xml |
| org.yaml:snakeyaml | 2.0 | 2.6 | transitive | Bootstrap, GameCommon, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/org/yaml/snakeyaml/maven-metadata.xml |
| redis.clients.authentication:redis-authx-core | 0.1.1-beta2 | 0.1.0 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/redis/clients/authentication/redis-authx-core/maven-metadata.xml |
| redis.clients:jedis | 8.0.0 | 8.0.0 | direct-or-managed | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 已最新 | https://repo.maven.apache.org/maven2/redis/clients/jedis/maven-metadata.xml |
| tools.jackson.core:jackson-core | 3.1.5 | 3.2.2 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/tools/jackson/core/jackson-core/maven-metadata.xml |
| tools.jackson.core:jackson-databind | 3.1.5 | 3.2.2 | transitive | Bootstrap, NJPDK, SCJYMJ, XCPDK, ZJH, ZYPK, gameServer | 必要传递依赖，由上游管理 | https://repo.maven.apache.org/maven2/tools/jackson/core/jackson-databind/maven-metadata.xml |

## Maven 构建插件

| 坐标 | 声明版本 | 最新稳定 | 使用 POM | 官方源 |
|---|---:|---:|---|---|
| org.apache.maven.plugins:maven-compiler-plugin | 3.15.0 | 3.15.0 | pom.xml, server/Activity/pom.xml, server/AdminApi/pom.xml, server/Bootstrap/pom.xml, server/CDXZMJ/pom.xml, server/Hall/pom.xml, server/IdentityVerification/pom.xml, server/Inventory/pom.xml, server/LegacyAccountServer/pom.xml, server/LegacyGameHall/pom.xml, server/LuckDraw/pom.xml, server/Matchmaking/pom.xml, server/NJPDK/pom.xml, server/Ranking/pom.xml, server/Referral/pom.xml, server/SCJYMJ/pom.xml, server/Social/pom.xml, server/Support/pom.xml, server/Telemetry/pom.xml, server/XCPDK/pom.xml, server/ZJH/pom.xml, server/ZYPK/pom.xml, server/gameServer/pom.xml | https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-compiler-plugin/maven-metadata.xml |
| org.apache.maven.plugins:maven-dependency-plugin | ${maven.dependency.plugin.version}, 3.11.0 | 3.11.0 | pom.xml, server/LegacyAccountServer/pom.xml | https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-dependency-plugin/maven-metadata.xml |
| org.apache.maven.plugins:maven-enforcer-plugin | 3.6.3 | 3.6.3 | pom.xml, server/LegacyAccountServer/pom.xml | https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-enforcer-plugin/maven-metadata.xml |
| org.apache.maven.plugins:maven-jar-plugin | 3.5.1 | 3.5.1 | pom.xml, server/Activity/pom.xml, server/AdminApi/pom.xml, server/Bootstrap/pom.xml, server/CDXZMJ/pom.xml, server/Club/pom.xml, server/Gateway/pom.xml, server/Hall/pom.xml, server/IdentityVerification/pom.xml, server/Inventory/pom.xml, server/LegacyAccountServer/pom.xml, server/LegacyGameHall/pom.xml, server/LuckDraw/pom.xml, server/NJPDK/pom.xml, server/Ranking/pom.xml, server/Referral/pom.xml, server/SCJYMJ/pom.xml, server/Support/pom.xml, server/Telemetry/pom.xml, server/XCPDK/pom.xml, server/gameServer/pom.xml | https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-jar-plugin/maven-metadata.xml |
| org.apache.maven.plugins:maven-resources-plugin | 3.4.0 | 3.5.0 | pom.xml | https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-resources-plugin/maven-metadata.xml |
| org.apache.maven.plugins:maven-surefire-plugin | 3.5.4 | 3.6.0-M1 | server/LegacyAccountServer/pom.xml | https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-surefire-plugin/maven-metadata.xml |
| org.codehaus.mojo:build-helper-maven-plugin | 3.6.1 | 3.6.1 | server/gameServer/pom.xml | https://repo.maven.apache.org/maven2/org/codehaus/mojo/build-helper-maven-plugin/maven-metadata.xml |
| org.codehaus.mojo:exec-maven-plugin | 3.6.3 | 3.6.3 | pom.xml, server/AdminApi/pom.xml, server/GameCommon/pom.xml | https://repo.maven.apache.org/maven2/org/codehaus/mojo/exec-maven-plugin/maven-metadata.xml |
| org.flywaydb:flyway-maven-plugin | ${flyway.version} | 13.3.0 | pom.xml | https://repo.maven.apache.org/maven2/org/flywaydb/flyway-maven-plugin/maven-metadata.xml |

## Admin/Client Node 与 pnpm 锁

Client 的 package manifest 保持零 npm 生产依赖；Creator 的 `cc` 由引擎提供。Admin 每个锁定包版本、直接/开发/传递关系、安装路径和 npm 官方 registry 最新稳定版本均在机器 JSON 的 `npm` 数组中，避免在本文重复数百行路径。

## 使用性、漏洞、许可证与处置

- 静态引用、Maven dependency tree/analyze 治理、Admin lint/typecheck/test/build、Client runtime import 审计共同证明保留项真实使用。
- 2.2.2 历史能力只读隔离；活动 POM、classpath 和发布物不可达，不因表面静态未引用删除业务等价能力。
- 精确 112 个 Maven 坐标经 OSV 在线查询为 0 findings，112/112 有许可证声明和本地制品 SHA-256。Admin/Client `pnpm audit` 为 0。
- 已移除 RocketMQ 未使用的 telemetry/gRPC/Prometheus 传递链和未配置 web-mobile 模板；没有新的安全升级可在不改变兼容矩阵的前提下自动应用。

## 完整回归

- Server：真实 MySQL 8 隔离库、87 项 Flyway migration，`./mvnw clean verify` exit 0；43/43 reactor 项目成功。
- MIGREAL06：本次 clean 后 ReplayArchiveIntegrationTest 1/1，0 failure/error/skipped；verify 阶段消费本次 XML SHA-256。
- Admin：Node 24.19.0 下 frozen install、lint、typecheck、5 files/15 tests、ledger、production build（2653 modules）、audit 全通过。
- Client：frozen install、pnpm audit、runtime dependency audit 全通过。

机器可读证据：`Server/docs/generated/aoo-project-latest-stable-dependency-audit.json`。
