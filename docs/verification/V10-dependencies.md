# V10 依赖、插件与运行版本核验

核验时间：2026-08-24（Asia/Shanghai）

## 结论

V10 专项门禁通过。生产依赖基线统一为 Java 25、Maven 3.9.16、Node.js 24.19.0、pnpm 11.19.0、Cocos Creator 3.8.8；Client 与 Admin 均只保留 `pnpm-lock.yaml`。44 个活动 Maven POM 无重复直接坐标、无 `system` 依赖、无旧 `account_server`/`com.aoo.legacy` 坐标，也没有 POM 指向 `reference/legacy-2.22`。必要的 2.22 对照二进制只读隔离保留，不进入生产依赖图。

全项目统一验收尚不能据此关闭：根 reactor 的 `validate` 当前被业务源码生产桩门禁拦截，Client 的全量 Node 测试也存在 6 个与依赖整改无关的既有失败；本专项未越界修改业务源码。

## 锁定矩阵与兼容依据

| 层 | 锁定值 | 机器证据 | 上游依据 |
|---|---:|---|---|
| Server JDK/API/字节码 | Java 25 | 本机 `java 25.0.4.1`；Compiler `release=25`；Enforcer `[25,26)` | [Oracle Java SE Support Roadmap](https://www.oracle.com/java/technologies/java-se-support-roadmap.html) |
| Server 构建 | Maven 3.9.16 / Wrapper 3.3.4 | Wrapper URL 与 SHA-512 固定；`./mvnw -version` 实测匹配 | [Maven 3.9.16 release notes](https://maven.apache.org/docs/3.9.16/release-notes.html) |
| Server/Client/Admin Node 工具 | Node 24.19.0 | 三端版本文件及两端 manifest 一致；Admin 在该运行时完成类型、测试、构建、安全审计 | [Node.js releases](https://nodejs.org/en/about/previous-releases) |
| Node 包管理器 | pnpm 11.19.0 | 两个 manifest 固定 `packageManager`；Admin engines 同步；锁文件重算 | [pnpm installation and compatibility](https://pnpm.io/installation) |
| Client 引擎 | Cocos Creator 3.8.8 | 根 manifest 与迁移对照 manifest 均为 3.8.8；无启用的项目扩展或云插件 | [Cocos Creator 3.8 manual](https://docs.cocos.com/creator/3.8/manual/en/) |

Node 24 为当前工具链 LTS 基线。Cocos 工程运行依赖 Creator 自带模块，Client manifest 没有 npm 生产依赖；Node 只服务仓库协议、门禁和测试工具，不替代 Creator 编辑器运行时。

## 已整改

- Admin 删除重复且已过期的 `package-lock.json`，明确 pnpm 为唯一包管理器；移除把 Node 可执行文件作为 devDependency 的做法，并重新生成 `pnpm-lock.yaml`。
- Admin 的 `quality` 链改为 pnpm 自调用，避免在已锁定 pnpm 的工程中旁路到 npm。
- Server、Client、Admin 的 Node 版本统一到 24.19.0；Client 新增 `.node-version`。
- Club、Location、IdentityVerification 的 `jackson-datatype-jsr310` 改为 `runtime`，与 `ObjectMapper.findAndRegisterModules()` 的真实加载方式一致，避免暴露无用编译 API。
- 修正本地二进制门禁将 `unzip -Z1/-p` 只读检查误判为解包的规则；把 `LegacyRuntimeContractSelfTest` 归入隔离契约自检，不再误判为生产旧核心入口。
- 新增 `tools/verify_v10_dependencies.rb` 并挂入 Maven `validate`：持续检查版本漂移、竞争锁文件、重复直接依赖、Gradle 旁路、旧 Maven 坐标和 2.22 隔离边界。

## 自动验证

| 命令 | 结果 |
|---|---|
| `ruby tools/verify_v10_dependencies.rb` | PASS：44 个活动 POM；0 个活动 Gradle 构建；0 个竞争锁；0 个旧坐标/2.22 生产 POM 引用 |
| `ruby scripts/audit-unused04-scopes.rb` | PASS：扫描 260 个依赖声明；作用域违规 0 |
| `ruby scripts/audit-unused15-native.rb` | PASS：直接 native/classifier、手工 JNI、真实解包、宽泛 native access 均为 0 |
| `ruby scripts/audit-unused36-isolated-retention.rb` | PASS：110 个历史 Jar 留在只读隔离区；生产 classpath 引用 0 |
| Admin `pnpm run typecheck` | PASS |
| Admin `pnpm run lint` | PASS |
| Admin `pnpm test` | PASS：4 files / 14 tests |
| Admin `pnpm run build:pro` | PASS：Vite 8.2.2，2657 modules transformed |
| Admin `pnpm audit` | PASS：无已知漏洞 |

`./mvnw -q -DskipTests validate` 已实际执行，但在到达全部依赖门禁前被 `ProductionStubCheck` 报出的 3 个业务源码问题阻断；这不是依赖清单或锁文件失败。Client `node --test tests/unit/*.test.mjs` 为 39/45，通过项与 Node 24 正常运行，失败项分别是 4 个测试未转译 TypeScript、1 个已删除旧桥文件断言和 1 个旧类名断言，均属于业务测试/源码边界。

人工打开 Cocos Creator 和真实设备验收按任务约束暂缓。
