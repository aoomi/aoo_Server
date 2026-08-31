# Aoo Server 项目记忆

> 当前权威基线（2026-08-22）：旧 Java 8/17/25、Ant、Nettosphere、CGLIB 等内容为迁移历史；实际状态以本文末尾“Java 26 最终现代化基线”为准。

## 项目目标

- 在保持业务、协议、数据库和游戏规则等价的前提下，将后端框架及依赖升级到最新稳定版本。
- 后端范围包括登录、大厅、亲友圈、成都血战麻将和内江跑得快。
- 不包含任何前端代码，也不引入其他子游戏。

## 项目路径

- 当前项目：`/Users/aoo/Code/Game/BCG/Aoo/Server`
- 等价迁移来源：`/Users/aoo/Code/Game/BCG/Test/QH_DFMJ`

## 模块映射

| 模块 | 目录 | 职责 |
| --- | --- | --- |
| 公共依赖与配置 | `server/common` | 公共源码、配置和第三方依赖 |
| 公共协议定义 | `server/commdef` | 消息、协议和共享定义 |
| 游戏服务框架 | `server/gameServer` | 游戏服生命周期、网络和房间框架 |
| 平台服务 | `server/gameHall` | 登录、大厅、亲友圈及平台业务 |
| 成都血战麻将 | `server/CDXZMJ` | 成都血战麻将业务与规则 |
| 内江跑得快 | `server/NJPDK` | 内江跑得快业务与规则 |

## 当前技术基线

- 源码兼容级别：Java 8。
- 历史对照运行时：Zulu OpenJDK `1.8.0_502` 已隔离到仓库外 `../.toolchains/legacy-zulu-jdk8`，不进入构建或发布链。
- 构建方式：Ant `build.xml` 与手工 JAR classpath。
- 数据库：MySQL，驱动类为 `com.mysql.cj.jdbc.Driver`。
- 当前依赖目录存在重复及跨代版本，不能直接整体覆盖。
- 原项目后端 698 个模块曾完成编译、初始化和真实启动检查；本项目仅保留上述两个子游戏。

## 等价升级原则

1. 按 JDK/构建链、公共依赖、框架、平台业务、子游戏的顺序串行升级。
2. 每次只跨越一个兼容边界；失败必须记录原因，不隐瞒、不伪造通过。
3. 网络消息号、字段含义、序列化结果、数据库表结构和 SQL 行为不得无意改变。
4. 登录、大厅、亲友圈、创建/加入房间、准备、发牌、操作轮转、结算和退出行为必须保持等价。
5. CDXZMJ 与 NJPDK 的规则计算、牌型判断、操作顺序和结算结果必须与迁移来源一致。
6. 升级后的依赖必须来源明确、版本锁定、无同类重复版本，并优先选择仍受维护的稳定版本。
7. 未完成编译、真实启动和业务回归的项目不得标记为完成。

## 验证门槛

- 全模块干净编译通过。
- 登录、大厅、亲友圈和两个子游戏进程真实启动并监听预期端口。
- 数据库连接、建房、进房、准备、发牌、出牌/操作轮转、结算和解散流程通过。
- 协议兼容检查通过，关键请求和响应字段无非预期变化。
- 依赖冲突、启动异常和高危运行警告清零或形成明确的保留说明。

## 当前断点

- 已完成旧工程运行时、构建描述符和第三方 JAR 初步盘点。
- 已确认存在 Java 8、Ant、手工依赖及多个重复旧版本依赖。
- 已选定 JDK 25 作为目标运行时、Maven 3.9.16 作为稳定构建链，并安装 Maven Wrapper 3.3.4。
- 根 `pom.xml` 暂以 `maven.compiler.release=8` 产生 Java 8 等价字节码，待运行兼容问题清零后再提升源码级别。
- 旧 Ant 仅作等价对照；Zulu JDK 8 保留在仓库外受控工具链目录，不作为构建或运行入口。
- Maven reactor 已建立：统一框架、CDXZMJ、NJPDK、gameHall 四个产物，全部以 `--release 8` 构建成功。
- 公开依赖已坐标化；私有 `kernel` 与定制 `nettosphere` 已隔离在项目内 Maven 仓库，等待对应框架阶段替换。
- 已移除 `ClassHelper` 对 `sun.reflect.ReflectionFactory` 的非标准编译依赖，业务过滤行为保持不变。
- 下一项：升级 Druid、MySQL Connector/J 和连接池，消除旧 Druid POM 对 `tools.jar`/`jconsole.jar` 的依赖。
- 详细状态见 `docs/后端现代化等价升级台账.md`。
## 2026-08-22 数据库层升级

- U04 已完成：Druid 从 `1.1.10` 升级到 `1.2.28`，新增 MySQL Connector/J `26.7.0`。
- 保留现有 Druid 连接池与 `com.mysql.cj.jdbc.Driver` 配置，未改变连接池行为和数据库协议。
- `./mvnw -DskipTests package` 全模块构建成功：框架、成都血战麻将、内江跑得快、登录/大厅/亲友圈均成功产物化。
- 当前断点：U05 网络、HTTP、WebSocket 栈升级。

## 2026-08-22 网络层升级基线

- Netty 采用官方推荐稳定版 `4.2.17.Final`。
- Apache MINA 采用稳定版 `2.2.9`。
- 私有定制 `nettosphere-3.2.2-qh` 暂时隔离保留，避免改变现有 HTTP 服务启动参数、端口和处理器接口。
- HttpComponents 5 使用不同 Java 包名，安排在网络基础依赖构建确认后单独等价适配。
- Netty `4.2.17.Final` 与 MINA `2.2.9` 已完成全模块 Maven 构建确认。
- U05 剩余：HttpComponents 5 等价适配、私有 Nettosphere 的原生替代与启动行为核验。
## 2026-08-22 当前实际版本快照

- 实际运行 JDK：Amazon Corretto OpenJDK `17.0.20 LTS`，不是 JDK 25。
- 目标运行 JDK：OpenJDK `25`，待完成兼容升级后切换。
- 编译兼容级别：Maven Compiler `release 8`，当前产物可运行于 Java 8+。
- 构建工具：Apache Maven `3.9.16`，Maven Wrapper `3.3.4`。
- 项目版本：`1.0.0-SNAPSHOT`。
- 数据库：Druid `1.2.28`、MySQL Connector/J `26.7.0`。
- 网络：Netty `4.2.17.Final`、Apache MINA `2.2.9`、私有 Nettosphere `3.2.2-qh`。
- 序列化：Gson `2.8.6`、Fastjson `1.2.31`、Jackson `2.1.4`、Protobuf `2.5.0`。
- 基础库：Guava `17.0`、Lombok `1.18.46`。
- 中间件：Jedis `2.9.0`、RocketMQ `4.7.1`、MongoDB Java Driver `3.4.2`、Quartz `2.3.0`。
- 日志：SLF4J `1.7.12`、Logback `1.1.3`。
- 新架构文档：`docs/后端架构开发文档.md`。
## 2026-08-22 依赖审计结论

- 当前并非所有依赖都已升级到最新稳定版本。
- 已完成稳定升级：Maven、Maven Wrapper、Netty、MINA、Druid、MySQL Connector/J、Lombok。
- 构建插件待小版本升级：Compiler `3.14.1 -> 3.15.0`、Enforcer `3.6.2 -> 3.6.3`、Jar `3.5.0 -> 3.5.1`。
- 高风险替代项：Nettosphere 私有包、Kernel 私有包、Log4j 1、Fastjson 1、Mongo 旧驱动、HttpComponents 4。
- 序列化依赖必须单独处理，禁止一次性升级 Gson、Jackson、Fastjson、Protobuf、Kryo，以免破坏网络或持久化字节兼容。
- 完整版本矩阵见 `docs/后端现代化等价升级台账.md` 的“依赖与构建插件审计”。
## 2026-08-22 Java 25 严格升级开始

- 最终基线改为 Java `25`，不再继续生成 Java 8 字节码。
- Maven Compiler Plugin 升级到 `3.15.0`，Enforcer Plugin 升级到 `3.6.3`，Jar Plugin 升级到 `3.5.1`。
- Enforcer 已启用：要求 Maven `[3.9.16,4.0.0)`、Java `[25,26)` 和依赖收敛。
- 删除源码零引用依赖：Commons BeanUtils、Commons Compress。
- 仓库内 `work/toolchains` 已清空；历史 JDK/Maven 安装包隔离到 `../.toolchains/reference-work-toolchains-20260823`，当前验证运行时为外部 `../.toolchains/jdk-26.0.2.1.jdk`，编译目标为 Java 25。

## 2026-08-22 Java 25 工具链就绪

- 项目私有 Oracle JDK `25.0.4.1 LTS` 已安装完成。
- 新增 `./mvnw25`，始终使用项目私有 JDK 25 和 Maven Wrapper 3.9.16。
- 基础库批次已更新版本声明：Gson `2.14.0`、Guava `33.7.1-jre`、CGLIB `3.3.0`、Commons Codec `1.22.1`、DbUtils `1.8.1`、Lang3 `3.20.0`、Joda-Time `2.14.3`。
- 原 Temurin 慢镜像下载被取消；未作为项目工具链使用。
## 2026-08-22 官方复核后修正为 Java 26

- Oracle 官方确认 Java `26.0.2.1` 为当前最新 GA；Java 25 是 LTS 线，但不是最新 GA。
- 项目私有 Oracle JDK `26.0.2.1` 已安装，构建入口改为 `./mvnw26`。
- 编译目标改为 `release 26`，Enforcer 要求 Java `[26,27)`。
- 已删除过渡入口 `mvnw25`；JDK 25 工具链仅为历史下载，不再是项目构建基线。
- 依赖收敛审计未通过，冲突源包括旧 Kryo、RocketMQ 4、HttpComponents 4、JFinal/JFinal Weixin；不得标记为全部最新或无冗余。
## 2026-08-22 去冗余批次 1

- JFinal/JFinal-Weixin 的真实使用仅为 `Page` DTO，已由本地不可变分页对象等价替代并删除两个框架依赖。
- Log4j 1 已删除：移除自定义 Appender、重复配置和启动初始化；Druid 日志过滤器改为 SLF4J。
- SLF4J 更新到 `2.0.17`，Logback 更新到 `1.6.3`。
## 2026-08-22 去冗余批次 2

- Kryo `4.0.2 -> 5.6.2`；确认仅用于本地 Ehcache 数据和对象深拷贝，不承担持久化或跨进程协议。
- RocketMQ Client `4.7.1 -> 5.5.0`；保留原生产者/消费者业务接口。
- Commons Collections 3 已迁移到 Commons Collections4 `4.5.0`。
- Error Prone annotations 统一锁定 `2.50.0`，消除 Gson/Guava 传递版本分歧。
## 2026-08-22 HTTP 去依赖迁移

- Apache HttpAsyncClient 4 已由 Java 26 标准 `java.net.http.HttpClient` 等价替代。
- 保留异步 GET/POST、UTF-8 JSON、60 秒连接/请求超时、重定向和业务回调线程切换。
- 删除无调用的 `HttpAsyncTimeoutClient`、`GMParam.toEntity()` 和 PlayerIDCardEvent 冗余 imports。
- `AssertsUtil` 不再继承 Apache HTTP 内部断言工具。
## 2026-08-22 JSON 与传递依赖收敛

- 业务代码唯一 Jackson 用途 `JsonUtil` 已等价改用项目统一 Gson，删除直接 Jackson 依赖。
- RocketMQ 内部 Jackson 仍作为其实现依赖，不暴露给业务层。
- Okio 统一为官方稳定版 `3.18.1`，Commons Codec 统一为 `1.22.1`。
## 2026-08-22 Java 26 编译兼容

- Maven Compiler 已显式注册 Lombok annotation processor，适配 JDK 23+ 默认处理器策略。
- RocketMQ 5 `MessageModel` import 已迁移到 remoting 协议包。
- ClassHelper 空值检查改用 JDK `Objects.requireNonNull`，清除最后一处 Apache HTTP 工具耦合。
- RocketMQ 5.5 创建普通主题使用四参数 API，空属性 Map 保持旧版无附加属性行为。
- AssertsUtil 补充框架无关的状态检查重载，替代旧 Apache Asserts 继承方法。
## 2026-08-22 Fastjson 安全升级

- `com.alibaba:fastjson` 已由 `1.2.31` 升级为 Fastjson2 官方 `1.x` 兼容构件 `2.0.61`。
- 暂时保留 `com.alibaba.fastjson.*` API，避免一次性改写序列化行为；后续可按模块迁移至 `com.alibaba.fastjson2.*` 原生 API。
- Maven Dependency Plugin `3.9.0` 尚不能解析 Java 26（class major 70），冗余依赖分析改用 JDK 26 `jdeps` 与源码引用核对。

## 2026-08-22 Java 26 最终现代化基线

- Oracle JDK `26.0.2.1`，Maven `3.9.16`，Wrapper `3.3.4`，标准入口 `./mvnw26`，编译 `release 26`。
- 构建插件：Compiler `3.15.0`、Enforcer `3.6.3`、Jar `3.5.1`、Build Helper `3.6.1`。
- 数据与中间件：Druid `1.2.28`、MySQL Connector/J `26.7.0`、Jedis `7.5.3`、RocketMQ `5.5.0`、MongoDB Sync Driver `5.9.0`、Quartz `2.5.0`。
- 网络：Netty `4.2.17.Final`、MINA `2.2.9`；Nettosphere/Atmosphere 已由 Netty 原生 HTTP 替换，Servlet API 已删除。
- 序列化与基础库：Gson `2.14.0`、Fastjson2 兼容构件 `2.0.61`、Protobuf `4.35.0`、Kryo `5.6.2`、Guava `33.7.1-jre`。
- 缓存与并发：Ehcache `3.11.1`、Disruptor `4.0.0`；旧 WorkerPool 等价迁移为有界 JDK 执行器。
- 代理与字节码：Byte Buddy `1.18.12`、Javassist `3.32.0-GA`；CGLIB 和外置 ASM 已删除。
- 其他：ip2region Java `3.3.7`、SLF4J `2.0.17`、Logback `1.6.3`、Lombok `1.18.46`。
- 唯一保留私有兼容制品为 `com.aoo.legacy:kernel:1.0.0`。它承载任务、线程、互斥、类扫描、日志桥接和服务生命周期；因缺少源码且无官方新版，不得在没有等价实现和运行回归时强制删除。
- `./mvnw26 -DskipTests validate compile` 五模块全部成功，Java/Maven 规则和 Dependency Convergence 通过。
- 尚未执行真实数据库、Redis、MongoDB、RocketMQ 环境启动及登录、大厅、亲友圈、CDXZMJ、NJPDK 全链路运行回归。
## 2026-08-22 Java 26 本地运行联调

- Maven Reactor 五模块在 Oracle JDK 26.0.2.1 下 `install` 成功，依赖收敛规则通过。
- 大厅、NJPDK、CDXZMJ 已改用 Maven `target/classes` 与 Java 26 运行时类路径，不再使用旧 JDK 8 和旧 `build/*.jar`。
- 三服务并行启动并持续 10 秒健康：大厅 WS/HTTP `9998/9888`、NJPDK `9996/9886`、CDXZMJ `19996/19886`。
- MySQL `3306`、Redis `16379`、MongoDB `27017`、RocketMQ NameServer `19876` 联调健康。
- 修复 Byte Buddy 无参数调用传递 `args=null` 导致数据库版本检查失败；恢复旧代理的空参数数组语义。
- Ehcache 持久化目录按大厅标识或 `game_sid` 隔离，支持多个游戏 JVM 并行运行。
- Druid 配置已移除被淘汰的 Log4j1 filter；NJPDK 主类统一为 `core.server.njpdk.NJPDKAPP`。
- 外部阻塞：Web 客户端 `7460`、账号服务 `904` 不在当前后端目录，未纳入本次启动通过结论。
- 已知非致命噪声：私有 Kernel 的无 stdin 控制台线程会打印 NPE；Druid 后台扩容线程偶发数组越界但当前服务端口持续健康，后续需针对私有 Kernel/连接池生命周期做长期压测。

## 2026-08-22 依赖与性能深度审计

- 完整报告：`docs/后端依赖插件深度审计报告.md`。
- 主游戏 Reactor 编译运行通过不等于依赖最优：Javassist 为零引用冗余，`netty-all` 过宽，且 Netty `4.2.17.Final` 全局覆盖 RocketMQ `5.5.0` 的 Netty 4.1 依赖需要真实消息兼容压测。
- 账号服务当前运行时依赖为 105 个 JAR、约 56.6 MiB；父 POM错误地让六个模块继承 Web、Redis、日志与测试栈。
- 账号服务同时加载 Logback 与 Log4j2、Jackson 2 与 Jackson 3；RocketMQ Spring `2.3.6` 面向 Boot 3，不作为 Boot `4.1.0` 最终集成方案。
- Redisson 为真实遗漏依赖，5 个源码文件依赖分布式锁；候选官方版本 `4.6.1`，必须结合 Boot 4.1 做编译和运行验证。
- 账号服务尚未完成 Java 26/Spring Boot 4.1迁移：旧 Hibernate、Mongo、Disruptor、Tomcat 内部 API和 Collections3 调用仍是编译阻塞。
- 在依赖重构和真实环境长稳压测完成前，不得宣称“全部最新、无冗余、性能已通过”。

## 2026-08-22 深度依赖审计整改完成

- 账号服务父 POM 已解除六模块依赖污染，日志统一为 SLF4J/Logback。
- 版本基线新增 Redisson 4.6.1、OkHttp JVM 5.3.0、Disruptor 4.0.0；删除 Boot-3-only RocketMQ Spring Starter。
- Java 26 API 迁移覆盖 Base64、Collections4、MongoDB 5、Spring Boot 4 JPA、Hibernate 7 和 Disruptor 4。
- 主框架不再直接声明 Javassist 或 netty-all，Netty 已按实际网络能力拆分。
- 主 Reactor 五模块及账号 Reactor 七模块均编译成功，账号 Reactor install 成功，Dependency Convergence 全部通过。
- 完整账号打包类路径仍为 241 JAR/约 169.0 MiB，属于功能栈传递成本；性能结论必须等待真实外部服务与长稳压测，不得仅凭依赖数量宣称性能通过。

## 2026-08-22 U17-08 runtime closeout
- Account service starts successfully on Oracle JDK 26.0.2.1 and Spring Boot 4.1.0 against local MySQL/MongoDB/Redis.
- Upgraded Redisson to 4.7.0 and constrained RocketMQ's transitive Javassist to 3.32.0-GA.
- Removed obsolete Log4j2 config, migrated MySQL driver names, fixed Spring 7 void @Bean, Java 26 Redisson configuration, CORS, constructor injection, disabled JPA open-in-view, and enabled Netty native access in runtime-java26.sh.
- Short runtime/concurrency validation passed; 24-hour soak remains explicitly pending.

## 2026-08-22 second dependency/defect audit
- Clean builds pass: main 5/5 and account 7/7.
- Stable upgrades: MongoDB 5.10.0, Quartz 2.5.2, Jedis 8.0.0, Collections 4.6.0, Pool 2.13.1, Ehcache 3.12.0, Fastjson 2.0.64, Protobuf 4.36.0, Netty 4.2.17, gRPC 1.83.1, OTel 1.65.0, Jackson2 2.22.2, Jackson3 3.2.2, LZ4 1.11.2, Commons IO 2.21.0.
- OSV findings reduced 31 -> 1. Remaining Log4j GHSA has no released fixed artifact as of this date.
- Removed legacy test templates and duplicate Boot repackage execution; fixed Builder defaults.
- Intentional follow-ups: account duplicate Spring config resources and Lombok inherited equals/hashCode semantics require controlled regression review.

## U17-10 第三轮依赖/缺陷审计（2026-08-22）

- 根目录新增 `lombok.config`，以 `callSuper = skip` 显式保持历史默认相等性行为，继承模型告警归零。
- Java 26 主工程和账号工程干净构建均通过。
- 不可使用 `/usr/libexec/java_home -v 26` 判断项目 JDK；本机会回退到 25。统一通过 `tools/runtime-java26.sh` 或项目 Java 26 包装入口执行。
- Lombok 1.18.46 的 JDK 26 `Unsafe` 提示是当前上游稳定版问题。

## U17-11 配置确定性治理（2026-08-22）

- 账号 `common`、`dao` 不再发布同名默认 `application.yml`；分别使用 `application-common.yml`、`application-dao.yml`、`application-dao-mongodb.yml`。
- `server/application.properties` 通过 `spring.config.import` 显式导入上述模块配置。
- DAO 配置禁止设置 `spring.profiles.active`；活动 Profile 只能由启动模块或部署环境决定。
- 跨模块构建资源审计确认第三方 Spring SPI/元数据重复属于正常机制；RocketMQ 引入的 `netty-all` 是无 class 的聚合入口，不可仅按 JAR 名判为冗余。

## U17-12 账号发布包瘦身（2026-08-22）

- Lombok 必须由每个使用模块直接以 `provided` 声明，禁止依靠模块间传递。
- JProtobuf 排除无运行引用的 `auto-value:1.0`。
- 账号发布包不再携带 Lombok/AutoValue，运行库约 166 MiB。
- RocketMQ RocksDB 有真实字节码引用；Netty 多平台库是官方聚合传递能力，未完成消息全链路回归前不得强删。

## U17-13 凭据环境化（2026-08-22）

- 仓库配置禁止保存真实密码、Token 签名密钥和第三方应用密钥。
- 主游戏服普通配置使用 `QH_<规范化键>`；Redis/MongoDB 使用独立环境变量。
- 账号服务统一使用 `ACCOUNT_*` 环境变量，完整映射见 `docs/后端环境变量配置.md`。
- 生产启动前必须注入非空密码及 `ACCOUNT_TOKEN_SECRET`。

## U17-14 危险能力治理（2026-08-22）

- 禁止恢复 `ProcessBuilderUtil` 或通过 `sh -c` 执行维护命令；进程管理统一使用 `ProcessHandle`。
- Java 26 运行入口固定 JEP 290 限制：`maxdepth=64;maxrefs=100000;maxbytes=16777216`。
- 旧 Java 序列化暂为数据兼容边界，不可无迁移方案直接改格式；新增入口必须使用 JSON/Protobuf 等受约束格式。
- MD5/SHA-1 仅保留于旧协议兼容，禁止用于新密码、Token 或签名设计。

## U17-15 第四轮全方位审计（2026-08-22）

- JDK 26.0.2.1 是最新正式版但非 LTS，仅维护至 2026-09；JDK 25 是最新 LTS。当前按项目要求继续 Java 26。
- Spring Boot 4.1.0 与当前 Maven 3 插件均为正式稳定组合，禁止采用 Maven 4 Beta、Boot 4.2 M1、Netty 5 Alpha 等预发布版本。
- JProtobuf 已升级 2.4.23。
- OSV：297 个唯一组件仅余 Log4j 2.25.4 上游阻塞漏洞。
- `server/gameServer/build` 是 177 MiB 旧 Ant 隔离树，禁止加入 Maven 生产构建；其中含未迁移旧游戏，不能未经归档确认直接删除。
- 当前最高工程风险是缺少业务自动化测试；仅剩项目编译告警为 `StandardMJRoomSet` 的 equals/hashCode 语义。

## 2026-08-22 U17-16 多视角审计记忆

- RocketMQ 5.5.0 传递引入 Awaitility 4.3.0 和 Hamcrest 3.0，但客户端 JAR 无对应字节码引用；账户父 POM 已排除 Awaitility，禁止重新带入生产运行包。
- `tools/account-soak-24h.sh` 数据库密码必须由 `ACCOUNT_GAME_DB_PASSWORD`、`ACCOUNT_LOG_DB_PASSWORD` 注入，禁止恢复明文默认值。
- `/JavaServerPack` 是后台写入口，但当前未见签名/鉴权/防重放；协议确认前不可假设公网安全，也不可盲目加拦截破坏调用方。
- 19 处 Java 原生反序列化目前依赖启动脚本的 JEP 290 参数；应迁移为应用内过滤器，并优先消除外部输入反序列化。
- 技术债基线：34 个空 catch、203 个 printStackTrace、342 个 System 输出、9 个 ThreadLocal。后续治理必须保持业务返回值与协议不变。
- SpotBugs 4.9.8.2 已成功审计账户侧 5 个字节码模块：267 条（P1=18、P2=106、P3=143）。优先项为哈希边界、默认字符集、静态状态、流关闭、空指针、整数溢出、equals 对称性和单例竞态；命名/DTO 告警不能破坏现有协议。
- U17-16 修复后账户 Reactor Java 26 构建 7/7 通过，运行包 Awaitility/Hamcrest 均为 0。

## 2026-08-22 U17-17 治理记忆

- JavaServerPack远程调用必须设置`QH_SERVER_PACK_HMAC_SECRET`并发送`X-QH-Timestamp`、`X-QH-Nonce`、`X-QH-Signature`；签名原文为`timestamp + "\\n" + nonce + "\\n" + body`，算法HMAC-SHA256，签名为十六进制。
- 未配置后台HMAC密钥时仅允许127.0.0.1或::1，禁止放宽为公网兼容模式。
- ClientPack当前限制请求体1MiB、单IP每秒300次。
- 游戏框架SpotBugs基线2470条（P1=181、P2=2289）；协议DTO字段/命名不可机械修改，应通过精确过滤基线隔离后持续治理真实执行缺陷。
- U17-17阶段主Reactor 5/5、账户Reactor 7/7均在Java 26下构建通过。

## 2026-08-22 U17-18 治理记忆

- `RedisCollection.isEmpty()`历史实现反向，现已按`llen == 0`修复。
- 亲友圈房间配置Map键为Long、旧请求字段为int，查找时必须转换为long，禁止恢复Integer直接containsKey。
- MongoDbMgr、RedisMgr、MqConsumerMgr、MqProducerMgr路径配置入口负责关闭自己创建的InputStream；接收InputStream的重载不夺取调用方所有权。
- 第一批游戏框架修复使SpotBugs从2470/P1=181下降至2447/P1=157；协议DTO兼容告警不应通过改字段名或私有化强行清零。

## 2026-08-22 U17-19 并发治理记忆

- DB版本管理器、DBFlowMgr、OpenSeverTime使用静态Holder，禁止恢复无同步懒加载。
- Feature的loaded必须保持volatile primitive boolean；连接器ip/port/session必须保持volatile以支持异步重连。
- RedisMgr/DataBaseMgr释放线程连接状态后必须ThreadLocal.remove；Kryo每次操作结束清理三个ThreadLocal，避免线程池长期滞留。
- StandardMJRoomSet的equals和hashCode均以setID为身份，禁止只保留equals。
- U17-19后主Reactor业务源码编译warning为0，主5/5、账户7/7构建通过。

## U17-20（2026-08-22）

- 主框架 SpotBugs 最新基线：2405，P1=153，P2=2252；相较初始基线减少 65 项、P1 减少 28 项。
- 已修复麻将开金/缺金胡、亲友圈排行、协议构造器等确定性缺陷，并清理扑克热路径标准输出。
- RocketMQ、MongoDB、Netty 握手异常已统一结构化日志。
- 验证：主 Reactor 5/5、账户 Reactor 7/7 均在 Java 26.0.2.1 下构建成功，依赖收敛通过。
- 24 小时账户稳态任务仍由 `com.aoo.account-soak-24h` 执行；结束后需以当前代码重新做短时冒烟。

## U17-21（2026-08-22）

- WebSocket 请求序列已按无符号 16 位处理，恢复 50000 到 0 的合法回绕检测。
- 机器人入场随机边界、资源关闭和错误日志已修复；麻将随机热路径改用 `ThreadLocalRandom`。
- Mongo BSON 反射实例化和异常日志已现代化；主 Reactor 5/5 构建通过。

## U17-22（2026-08-22）

- 扑克牌型列表不再依赖不安全强转；共享服务和维护停服重复堆栈输出已清理。
- 停服等待会恢复线程中断标志。
- 生产源码 `printStackTrace` 已从 199 降至 161；主 Reactor 5/5 构建成功。

## U17-23 至 U17-28（2026-08-22）

- 生产源码 `printStackTrace` 已清零；生产启动和核心业务标准输出已转统一日志。
- 本地运行脚本 classpath 生成/读取已修复；大厅、NJPDK、CDXZMJ 均真实启动并监听预期端口。
- 大厅启动期 `DBFlowMgr` 空服务 NPE 已修复，WebSocket 启动失败不再误报成功。
- 全局无参 `String.getBytes()` 显式 UTF-8；四处临时随机数改用 `ThreadLocalRandom`。
- SpotBugs 最新基线：2380，P1=131，P2=2249。
- 主 Reactor 5/5、账户 Reactor 7/7 构建成功；账户 24 小时稳态尚未结束，结束后执行当前版本短时冒烟。

## U17-29 至 U17-30（2026-08-22）

- 网络、Redis、加密和文件文本处理的默认字符集继续收敛为显式 UTF-8/ASCII。
- SpotBugs 最新有效基线：2348，P1=100，P2=2248；主/账户双 Reactor 构建成功。
- 账户稳态在 2026-08-22 15:32 仍运行正常：进程存活、端口存活、RSS 约 792 MiB、线程 70、错误计数 0。
## 2026-08-22 U17-31 至 U17-45

- Java 26.0.2.1 主 Reactor、账号 Reactor 构建/测试均通过；原项目 Maven 测试用例数为 0。
- SpotBugs 最新：2250 total，P1=0，P2=2250；初始为 2470 total，P1=181。
- CDXZMJ、NJPDK 独立 JVM 全类加载和 APP 入口门禁 2/2 通过，台账位于 `docs/Server_game_split-后端运行台账.{json,md}`。
- 大厅、CDXZMJ、NJPDK 真实初始化已到外部连接阶段；当前环境缺少 RocketMQ 127.0.0.1:9876、clark_game MySQL 和 Redis，完整联网启动待环境恢复后复核。
- 账号 24 小时稳态任务 `com.aoo.account-soak-24h` 仍在运行；在计划结束前不得标记最终全部完成。
## 2026-08-22 U17-46 至 U17-53

- 隔离基础设施下大厅、NJPDK、CDXZMJ 均完成真实启动并输出 `[服务器启动完毕]`。
- 新增源码级 `ConsoleTask.ConsoleTaskDealThread` 兼容实现，修复无 stdin 时 NPE。
- 数据库连接池 maxActive=100，启动期 Druid 数组越界复测不再出现。
- 最终主/账号 test 门禁均通过；SpotBugs 2251 total，P1=0，P2=2251。
- 唯一剩余门禁：账号 24 小时稳态任务结束与结束后当前产物短烟测。
## 2026-08-22 U18 业务全方位审计

- 新增 `docs/后端业务全方位审计报告.md`。
- 修复在线人数、模板房计数/等待 CAS、克隆计数隔离、共享玩家重连空值、联盟权限、扑克参数和机器人出牌。
- 修复七对连对、一色双龙条件、建房判空以及两个 DTO 错字段赋值。
- 主/账号依赖树解析无未解决冲突；Starter 静态未使用报告按自动配置误报保留。
- 最新 SpotBugs 2225 total、P1=0；CDXZMJ/NJPDK 类加载门禁 2/2 通过。

## 2026-08-22 U18-11

- 后端业务审计继续收敛：SpotBugs 2219，P1=0；主工程测试通过。
- 当前构建游戏门禁：CDXZMJ、NJPDK 2/2 通过。
- 唯一未闭环项为 24 小时 account soak；完成后需以当前构建再跑一次 account 短时 smoke。

## 2026-08-22 玩法分层框架升级

- 新增 GameSPI、Mahjong、Poker、LongCard、WordCard、Families、Bootstrap 模块。
- Hall 已移除 CDXZMJ/NJPDK 具体依赖；具体玩法由 Bootstrap 经 ServiceLoader 装配。
- CDXZMJ 516 与 NJPDK 629 已提供兼容 GameProvider。
- 前端台账分类 511 项；后端源码扫描 533 个模块，531 个已有源码证据分类，2 个待判定。
- 基于 3560 个聚合源码提取 Common 权威房间基础；四牌类只保留各自牌义，玩法族与地区差异分别进入 Families/Games。
- 已新增 Gateway、ConfigCenter、Games，形成 `GameSPI -> Common/四牌类 -> Families -> Games -> Bootstrap` 的迁移目标骨架。
- `Common/common` 大小写冲突已通过改名 `GameCommon` 解除；2026-08-22 全 Reactor 17/17 `mvn test` 通过。
- 在不修改通信协议文件的前提下，已补内存幂等、事件日志、回放、解散、托管、配置仓库、房间快照、租约/fencing、Outbox 与 Billing 边界。
- 基础框架新增 7 个自动化场景，覆盖注册冲突、状态机、座位、幂等、租约/fencing、规则链和配置不可覆盖；全 Reactor 17/17 测试通过。

## 2026-08-22 非通信基础闭环续作

- GameCommon 新增房间恢复编排：租约、fencing token、快照和增量事件重放形成统一恢复入口。
- 新增结算不变量校验：上下文一致、玩家唯一、分项合计、竞技玩法零和守恒。
- 当前自动化测试覆盖恢复成功路径与结算拒绝路径；全 Reactor 17/17 测试、依赖收敛和架构边界检查通过。
- 仍需接入数据库快照/事件/账务、消息队列 Outbox 发布器和分布式租约；不得把内存实现作为生产实现。
- 并行任务正在统一通信协议，后续非通信基础开发禁止修改 Gateway、HTTP/WSS 帧、消息号、票据、协议 DTO 和 Handler。
- 新增生产持久化能力门禁，七类关键状态必须全部提供持久化且分布式实现；内存实现只能用于开发和自动化测试。
- 玩法标识对账以 code/模块名大小写无关精确匹配，并将基础设施、测试和备份模块从后端游戏数量中剥离；结果位于 `docs/前后端玩法标识对账.tsv`。
- 对账工具支持扫描目标 `server` 目录；原始拆分源码缺失但目标工程已有模块时标记 `TARGET_MIGRATED`，不得继续误报为迁移阻塞。
- 后端源码分类必须优先使用稳定模块后缀（MJ、PDK/DDZ/PK、CP/DSS、PHZ/ZP 等）确定牌类，源码关键词仅负责兜底和玩法族细分，防止公共复制代码污染分类。
- YCHP、YCSDR、YZCHZ 已按稳定后缀归入 WORD_CARD；common、commdef、gameHall、gameServer、Test 使用 INFRASTRUCTURE/backend-framework，不计入玩法待判定数。
- SCJYMJ 628 与 XCPDK 618 的完整业务源码已从 QH_DFMJ 恢复到目标 server，使用统一 Java 26 父 POM 并加入 Games 聚合；不得复制旧 build/bin/IDE 产物。
- XCPDK 的 bombScore 是玩法私有结算字段，使用 `XCPDKVictory` 扩展共享 `Victory`；禁止为单玩法需求修改全局通信 DTO。
- ZJH、ZYPK 仅在 Server_wanzai 等旧工程中以内嵌形式存在，目标工程当前不存在。通信协议工作已经完成，协议不再作为阻塞原因。
- ZJH 权威源码包含 8 个业务类、14 个专属 DTO、4 个处理器；当前真实阻塞仅为旧 `PockerRoom/RoomDelegateAbstract` 房间运行时不兼容。`GameType` 是动态引用数据，配置已存在 ZJH ID 9，不需要修改为枚举。
- ZYPK 权威源码包含 10 个业务类、8 个房间 DTO、17 个消息 DTO、7 个处理器；配置已存在 ID 62。它仍依赖同一 delegate 运行时、旧房间注册器和 PDK 通用模型，应与 ZJH 共享兼容层后再接入，禁止建立第二套重复基础设施。
- 旧扑克玩法迁移统一继承 `business.global.room.compat.LegacyPokerRoomAdapter`；该桥只做旧 API 名称映射，所有状态和副作用必须继续落在新版 `AbsBaseRoom`，禁止复制旧 delegate 内核。
- 普通 `./mvnw` 和 `/usr/libexec/java_home -v 26` 会错误落到系统 JDK 25；项目 Java 26 构建必须使用 `./mvnw26`，它指向私有 Oracle JDK 26.0.2.1。
- `LegacyPokerRoomAdapter` 已通过 `./mvnw26 -pl server/gameServer -am -DskipTests compile`，共编译 2247 个框架源码，Java 26 与依赖收敛门禁通过。
- ZJH 按串行阶段迁移：先独立迁移专属 DTO，再迁移规则核心、房间业务、处理器、GameSPI Provider；禁止一次复制后把未编译模块标记完成。
- ZJH DTO 实际为 17 个 Java 文件，不是初步盘点的 14 个；首次 Java 26 编译产生 21 个旧共享类型映射错误。框架依赖本身编译成功，后续只适配 DTO 到当前共享类型，不复制旧 common/commdef。
- ZJH 的 17 个 DTO 已完成当前共享类型适配并在 Java 26 下编译成功；创建请求继承 `BaseCreateRoom`，房间快照继承 `GetRoomInfo<ZJHRoom_Cfg>`，不得恢复旧公共字段模型。
- ZJH 规则核心复用当前 `jsproto.c2s.cclass.pk.BasePocker` 与 `BasePockerLogic`；旧 `ZJHSetCard` 的 `ZJHRoomSet` 构造参数从未使用，迁移后不得重新引入该无效耦合。
- ZJH 规则测试共 3 项，覆盖散牌、对子、顺子、金花、顺金、豹子、特殊 235、牌型比较与 52 张牌堆无重复发牌；Java 26 测试通过。
- ZJH 配置必须从迁移后的权威 `ZJHConfig.txt` 加载并快速失败；默认解析 `conf/`，开发构建回退 `server/ZJH/conf/`，禁止缺配置时静默使用代码默认值。
- ZJH 配置层 2 个测试与规则层 3 个测试均已通过；模块当前 20 个主源码、2 个测试源码在 Java 26 下构建成功。
- BJC 63、SBP 59 在全部已知后端集合中只有 gametype.json 目录元数据，没有可迁移的权威业务源码；阻塞证据记录于 `docs/后端缺失玩法阻塞.tsv`。
- SCJYMJ 628、XCPDK 618 已正式接入 GameSPI/ServiceLoader；Bootstrap 必须发现 CDXZMJ、NJPDK、SCJYMJ、XCPDK 共 4 个 Provider。
- Bootstrap 支持 hall、cdxzmj、njpdk、scjymj、xcpdk 启动模式；具体玩法仍由 Games 聚合和 ServiceLoader 发现。
- Bootstrap 自动化门禁必须主动初始化 4 个玩法 APP 类；完整联网启动仍需配置 MySQL、Redis、RocketMQ 和游戏注册数据。
- SCJYMJ/XCPDK GameRoomFactory 必须校验 BaseRoomConfigure.gameType.id；缺配置或跨玩法配置快速失败，真实构造由联网集成门禁验证。
- 遗留后端源码 533 个模块已全部完成自动分类，`REVIEW_REQUIRED=0`；台账中早期“Ruby 正则失败、81 项待分类”状态已经失效，不得再作为迁移断点。
- SCJYMJ/XCPDK 遗留模块的主源码根为 `src`，测试必须放在独立 `test` 目录并由 `testSourceDirectory` 声明，禁止使用会被主编译器吞入的 `src/test/java`。
- 具体玩法启动必须由 GameProvider.serviceLauncher 提供；Bootstrap 除 hall 外仅按 code 路由，禁止重新引入具体游戏 APP import/switch。
## Bootstrap 解耦门禁

- 架构检查器已禁止 `game-bootstrap`、`game-hall`、分类层直接依赖具体玩法模块。
- Bootstrap 源码不得导入 CDXZMJ、NJPDK、SCJYMJ、XCPDK 的实现包。
- 新玩法必须通过 `GameProvider`、ServiceLoader 和 `GameServiceLauncher` 接入。
- 所有具体玩法必须显式依赖 `game-common` 和对应牌类模块；麻将为 `game-category-mahjong`，扑克为 `game-category-poker`。该要求已进入架构门禁，禁止只依赖 SPI 和遗留框架。

## 2026-08-22 玩法公共框架审计闭环

- 五个已迁移玩法必须显式依赖 GameCommon 与对应牌类层；架构脚本验证依赖方向和 ServiceLoader 注册。
- 权威玩法随机统一使用 `GameRandomSource`，种子必须可记录并用于回放；禁止在业务核心直接使用 `Random`、`Math.random` 或无随机源的 `Collections.shuffle`。
- NJPDK/XCPDK 手牌是私有权威状态；下行通过 `CardPerspective` 生成脱离状态的玩家视图，禁止公开可变 `privateCards`。
- 进入、续进、准备、取消准备、解散、同意和拒绝解散由框架公共 Handler 实现；玩法协议类仅保留注册壳。
- NJPDK 使用遗留 AYPDK 权威配置，XCPDK 使用字段完全匹配的 PDK 权威配置；配置缺失、键缺失、类型错误和概率越界必须快速失败。
- ZJH gameId 9 已按当前单一权威房间原则接入，不复制旧 Delegate 房间内核；`ZJHTable` 支持入座、发牌、轮转、弃牌、比牌、暗牌裁剪和随机种子审计，Provider 由 ServiceLoader 发现并由共享游戏服务承载。

## 2026-08-22 后端全面审计收口记忆

- 根 Maven Reactor 当前为 28 个模块，使用 `./mvnw26` 执行 Java 26 工具链。
- 全量 `./mvnw26 test` 已通过，28/28 模块 SUCCESS；架构门禁 `ruby tools/check_architecture_boundaries.rb` 通过 21 个框架模块。
- Gateway 已具备一次性 WSS ticket、Origin/设备绑定、seq/timestamp/requestId、roomId/playVersion 权威校验。
- CDXZMJ、SCJYMJ 接入麻将分类 Provider；NJPDK、XCPDK、ZJH 接入扑克分类 Provider。
- accountServer 已纳入根 Reactor，登录设备绑定和兼容 JSON 解析已修复。
- 外部阻塞：生产数据库 Schema/部署配置缺失；ZYPK 缺旧房间内核兼容桥；BJC、SBP 缺权威源码。
- 不得为消除告警盲改 Ehcache/Jedis/Netty/MINA 兼容层，后续必须配套集成与压力测试。

## 2026-08-22 生产持久化与 ZYPK 断点

- 生产持久化代码已补齐：JDBC 幂等、房间事件、快照、租约/fencing、Outbox、玩法配置和账务流水；Bootstrap 通过 `ProductionPersistenceAssembly` 对七能力做快速失败检查。
- 数据库恢复表迁移为 `database/migrations/V20260822_04__room_recovery.sql`。生产不得使用无连接池的 `DriverManagerDataSource`，应注入已有受管 DataSource。
- 真实 MySQL 迁移、并发事务与集群故障注入未在当前环境执行，只能标记“实现完成、环境待验收”。
- ZYPK 权威源码 40 个 Java 文件已归档到 `server/ZYPK`；DTO 兼容问题已收敛，模块已进入根 Reactor、Games 聚合及 ServiceLoader。
- 新版单一权威 `ZYPKTable` 已实现发牌、暗牌裁剪、操作位、补牌/出牌、下注/跟注/加倍、看牌/明牌、比牌双方可见、弃牌与回退，4 项测试通过。
- ZYPK 的旧 delegate 房间、网络处理器、牌型比较策略、结算、重连和持久化尚未等价接入，仍由 Maven 显式隔离；不得标记为完整玩法迁移完成。
- 根 Reactor 当前 29 个模块；架构边界门禁覆盖 22 个服务模块并包含 ZYPK 分类依赖、SPI、随机源、私牌和 Bootstrap 隔离检查。
