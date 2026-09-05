# CUR-04 依赖、插件与构建版本最终核验

核验时间：2026-08-25（Asia/Shanghai）

## 结论

CUR-04 最终合并态复核已经闭合，单一结论为 **PASS**。Server、Client、Admin 的依赖、锁定、漏洞、许可证、生命周期、体积、构建插件与根构建门禁均通过。

- Java、Node、Cocos Creator、Admin 与构建插件均按真实 peer、engine 和上游兼容约束审计，没有盲目追新。
- 删除未被 Creator 3.8.8 构建配置引用的 `Client/build-templates/web-mobile/index.html`；唯一活动目标和模板为 `web-desktop`。
- 移除 RocketMQ 引入但项目未使用的 OpenTelemetry、gRPC 和 Prometheus 传递链，消除其中两个已知 GHSA；保留 RocketMQ 业务能力。
- 修复 `RoomSnapshotStore` 测试接口同步问题后的目标测试通过；清除了麻将会话中常量假条件形成的不可达死代码，未改变可达业务语义。
- 2.22 对照能力仍在只读隔离区，活动 POM、运行 classpath 与发布包均不可达；旧实现没有成为运行依赖。
- Admin 与 Client 的 pnpm 锁可冻结重放；当前 reactor 的 43 个 Maven 项目均生成依赖树；38 项依赖治理门禁全部通过。

## 版本与上下游兼容矩阵

| 层 | 锁定版本 | 实测/上游约束 | 判定 |
|---|---:|---|---|
| Server JDK | Oracle Java 25.0.4.1；`release=25` | Enforcer `[25,26)`；编译器实际使用 release 25 | 兼容 |
| Maven | 3.9.16 / Wrapper 3.3.4 | Wrapper URL 和 SHA-512 固定；`./mvnw -version` 实测 3.9.16 | 兼容、可复现 |
| Node | 24.19.0 | Vite 8 要求 `^20.19 || >=22.12`；Vitest 4 接受 `>=24`；ESLint 10 接受 `>=24`；vue-i18n 11 要求 `>=22` | 兼容 |
| pnpm | 11.19.0 | Admin/Client `packageManager`、engines、版本文件一致；冻结安装成功 | 兼容、可复现 |
| Cocos Creator | 3.8.8 | Client manifest 双重锁定；无活动 npm 运行依赖；模板脚本仅引用 Creator 3.8.8 生成物 | 兼容 |
| Vue 工具链 | Vue/compiler-sfc 3.5.41、Vite 8.2.2、plugin-vue 6.0.8 | plugin-vue 接受 Vue `^3.2.25`、Vite `^5..^8` | 兼容 |
| 测试工具链 | Vitest/coverage-v8 4.1.11、jsdom 30.0.1 | coverage-v8 精确 peer Vitest 4.1.11；Vitest 接受 Vite 8 | 兼容 |
| Router/状态 | vue-router 5.2.0、Pinia 4.0.3 | Router 要求 Vue `^3.5.34`、Pinia `^3.0.4 || ^4.0.2`、Vite `^7.3 || ^8` | 兼容 |
| UI | Element Plus 2.14.5 | peer Vue `^3.3.7` | 兼容 |
| TypeScript | 6.0.2 / vue-tsc 3.2.5 | vue-tsc 要求 TypeScript `>=5`；实际 typecheck 通过 | 兼容 |

peer/engine 数据在核验日由 npm registry 对应锁定版本读取，并以实际构建结果复核；没有用“最新版”代替兼容性判断。

## 依赖与插件处置

### 保留

- Server BOM、JDBC 驱动、日志 provider、Jackson Java Time、RocketMQ、SPI/反射加载依赖均有编译、运行或动态加载证据。
- Creator 自带 `cc` 模块不是 npm 依赖；Client manifest 保持零 npm 生产依赖。
- `Server/reference/legacy-2.22` 的历史 Jar 继续只读隔离，仅用于 2.22 业务等价审计，不进入活动 POM、生产 classpath 或发布包。
- Admin 的 Vue、Element Plus、ECharts、路由、状态、国际化、表格导出和二维码等包均有源码引用与生产构建证据。

### 清除与收敛

- 删除未配置的 `web-mobile` Creator 构建模板，保留真实活动的 `web-desktop` 模板。
- 在 `GameCommon` 与 `gameServer` 的 RocketMQ 依赖上排除未被源码使用的 `io.opentelemetry:*`、`io.grpc:*`、`io.prometheus:*` 传递组件。
- 清除的传递链包含 `io.grpc:grpc-netty-shaded:1.53.0`（GHSA-prj3-ccx8-p6x4）和 `io.opentelemetry:opentelemetry-api:1.44.1`（GHSA-rcgg-9c38-7xpx）；当前精确解析树已无这两个组件。
- 活动树无竞争 lockfile、Gradle 旁路、`system` scope、旧 `account_server`/`com.aoo.legacy` 坐标或生产 POM 对 2.22 隔离目录的引用。

## 锁定、依赖树、许可证、漏洞与生命周期

| 项目 | 结果 | 证据/说明 |
|---|---|---|
| 版本/锁门禁 | PASS | `verify_v10_dependencies.rb`：45 个活动 POM、0 Gradle、2 个唯一 pnpm 锁、0 旧运行坐标 |
| 依赖治理 | PASS | `audit-unused02` 至 `audit-unused38` 全部通过，覆盖 scope、optional、BOM、插件、处理器、日志、驱动、native、npm、Creator 模板和 2.22 隔离 |
| Maven 依赖树 | PASS | 当前 reactor 的 43 个项目重新执行 `dependency:tree`；逐项目证据为 `work/audit/cur04-dependency-tree.txt` |
| Maven convergence | PASS | 已执行模块的 Enforcer `dependencyConvergence` 通过 |
| pnpm 可复现安装 | PASS | Admin 与 Client 均以 `--frozen-lockfile` 安装成功且锁文件未变 |
| Node 漏洞 | PASS | Admin、Client `pnpm audit --audit-level=high`：无已知漏洞 |
| 活动 Maven 漏洞 | PASS | 从当前 44 模块解析树提取 112 个外部组件，按精确坐标在线提交 OSV；0 findings。证据：`Server/work/audit/active-dependency-evidence.json`、`Server/docs/generated/active-dependency-osv.json` |
| 活动 Maven 许可证 | PASS | 112/112 组件均解析到许可证声明，并记录本地制品 SHA-256；Maven aggregate third-party report 位于 `Server/target/reports/aggregate-third-party-report.html` |
| 归档漏洞/SBOM | PASS | THIRD08：4 个归档文件均有 SHA-256、许可证处置和 CycloneDX 条目；OSV 快照 0 findings；全部 `scope=excluded` |
| 归档许可证 | PASS | THIRD07：Apache-2.0 文本、哈希、notice 完整；私有 legacy kernel 禁止再分发且发布排除 |
| 生命周期台账 | PASS | 26/26 根管理依赖均有官方来源、owner、引入原因、兼容/安全响应规则、迁移估算与退役条件；不伪造上游不存在的固定 EOL 日期 |
| 依赖体积归因 | PASS | 114 个直接 artifactId；解析到 200 个唯一 Jar 文件名，共 128,043,459 bytes，逐 Jar 记录精确字节、SHA-256、消费模块及直接候选归属；5 个 Client bundle 共 405,640,088 bytes |
| 应用镜像层归因 | PASS | 当前应用依赖层为 128,043,459 bytes；12 个内部 SNAPSHOT 副本尺寸差异被如实列出，并由 clean reactor 门禁约束，不构成外部依赖漏项 |

`audit_active_dependency_evidence.rb` 同时校验当前树、精确本地制品、许可证和 OSV 响应，最终结果为 `components=112, vulnerabilities=0, missingLicenses=0, passed=true`。`audit_dependency_lifecycle_governance.rb` 为 `26/26` 完整。`audit_dependency_size_contribution.rb` 的全直接/传递归因和镜像贡献字段均通过。

## 网络诊断与证据边界

- Maven Central TLS 首次在解析 `org.semver4j:semver4j:5.8.0` 时握手失败；重试后 OWASP Dependency-Check 已进入 NVD 全量初始化（约 382k 条），证明不是坐标或 POM 配置错误。因首次数据库同步耗时异常，本轮没有把不完整的 Dependency-Check 数据作为结论。
- 同一环境下精确坐标 OSV 批量查询随后成功，当前活动树 112 个组件为 0 findings；因此 Java 漏洞结论有在线证据，不依赖失败的 NVD 半成品。
- `docker manifest inspect eclipse-temurin:25-jre` 因 registry 超时未取得基础镜像 digest；未离线臆测基础镜像体积。应用自身的完整依赖层已经按本地实际 Jar 精确归因。

## 编译与质量验证

| 命令/门禁 | 结果 |
|---|---|
| Admin lint / typecheck / Vitest / ledger / production build | PASS：5 个 test files、15 个 tests；Vite 8.2.2 构建 2653 modules |
| Client frozen install / audit / runtime dependency audit | PASS：零 npm 漏洞；运行入口依赖审计通过 |
| Server `dependency:tree`（当前 43 项目 reactor） | PASS |
| `./mvnw -q -Dexec.skip=true -pl server/GameCommon -am test` | PASS；`RoomSnapshotStore` 测试接口已同步 |
| `java tools/UnreachableBranchCheck.java . ../Client` | PASS；不可达常量假分支已清除 |
| `audit-unused02` 至 `audit-unused38` | PASS；`audit-unused38` clean reactor 重建 33 个 Jar、5212 个 class |
| 根 `-DskipTests -Dexec.skip=true validate` | PASS |
| 全 reactor `-DskipTests -Dexec.skip=true package` | PASS；最终稳定重跑完成全项目编译和打包 |

最终 package 首次遇到并行生成 Jar 的短暂 IO 竞争；不清理业务文件直接重跑后全 reactor 成功。最终签署仅采用稳定合并态结果。

## CUR-04 退出判定

CUR-04 自有范围的依赖删除、scope/BOM/插件治理、锁定重放、活动与归档漏洞、许可证、全依赖树、生命周期台账、体积归因、目标测试和不可达分支均已闭合。MIGREAL06 已移至 GameCommon `verify`，严格消费本次 clean 后真实测试报告；正式入口 `Server/tools/verify-clean-with-integration-db.sh` 自动建立隔离 MySQL schema、执行 Flyway 并运行原样 `./mvnw clean verify`，最终 43/43 reactor 项目成功。

截至 2026-08-25 的官方源最新稳定性全面清单见 `docs/Aoo-全项目版本插件依赖清单与最新稳定性审计.md`；机器明细见 `Server/docs/generated/aoo-project-latest-stable-dependency-audit.json`，覆盖 112 个 Maven 活动组件、9 个 Maven 插件坐标、389 个 pnpm 锁定包版本实例及工具链/容器/Creator 兼容矩阵。官方源查询错误为 0。

任务按 **PASS** 签署。

签署：CUR-04 dependency governance / 2026-08-25 Asia/Shanghai / final merged-state PASS
