# V01 全项目构建与测试复验

复验时间：2026-08-24 21:20—21:39（Asia/Shanghai）

结论：**未通过**。Admin 全门禁通过；Client 的旧测试夹具/转译聚合已整改，但同一时段业务源码仍被并发修改，最终严格 TypeScript 有 20 项错误且自动测试有 1 项版本路由断言失败；Server 全 reactor 构建至第 38/44 个模块时在 `RoomSafety` 编译失败，根验证门禁另有 DEAD09 的 20 项异常/静默回退发现。因此 V01 不得关闭。

## 可复现命令

```bash
cd /Users/aoo/Code/Game/BCG/Aoo/Server
tools/verify-v01-reactor.sh

cd /Users/aoo/Code/Game/BCG/Aoo/Client
scripts/verify-build.sh

cd /Users/aoo/Code/Game/BCG/Aoo/Admin
scripts/verify-build.sh
```

Server 脚本先原样执行根门禁 `./mvnw clean verify`；若根 `exec` 门禁提前失败，再执行 `./mvnw -Dexec.skip=true verify`，仅用于继续覆盖全部 Maven 模块的编译、Surefire 测试和打包。第一次门禁的失败状态仍被保留并使脚本失败，不会把跳过门禁的第二次 reactor 当成通过。

## 已整改

- Server 品牌门禁不再扫描 `legacy-isolation` 审计夹具及 `build-*` 生成产物，并增加对应回归夹具。
- Server ProductionStub 门禁不再把明确的 fail-closed `UnsupportedOperationException` 当作占位实现；`TODO/FIXME/not implemented/mock/dummy/stub` 等有效规则仍保留。
- Client 校验脚本固定使用 Cocos Creator 3.8.8 自带 TypeScript 及项目 Node 24.19.0，严格编译后聚合所有 `*.test.mjs` 和 `*.test.ts`，零测试时失败。
- Client 修复 7 项既有旧夹具：TypeScript 转译、大小写/扩展名、已更名生产桥接、错误对象断言和版本化路由断言；未删除或跳过测试。
- Client `tsconfig.json` 补充 DOM iterable / ES2022 类型库，并隔离同一 inode 的 `Bootstrap/bootstrap` 大小写重复纳入。
- Admin 校验脚本固定 Node/npm 工具链并执行现有完整 `quality` 门禁。

## 最终失败清单

### Server

1. 根门禁：`DEAD09 exception policy failed: 20`，证据为 `work/audit/dead09-exception-policy.json` 和 `work/v01/server-root-validate-final.log`。这些是业务源码异常处理发现，不在本 V01“只改构建/聚合、不改业务源码”边界内。
2. 全 reactor：前 37 个子模块完成，第 38 个 `room-safety` 编译失败；`JdbcRoomSafetyService.java` 第 18/23 行共 6 个语法错误，后续 6 个模块未执行。完整日志：`work/v01/server-full-reactor.log`。
3. 并发期间根 reactor 从 43 个变为 44 个模块（新增 `RoomSafety`），且 Client 生成目录/Server POM 与源码持续变化；本轮未覆盖成“同一静态快照通过”。

### Client

1. 严格 TypeScript：20 项业务源码错误，集中在重复请求头、缺失 `requestV2`、`LegacyNetManagerFacade` 与 `SendPack` 不一致、隐式 `any`、Cocos API 类型签名及 `Component.start` 覆盖签名。
2. 自动测试最终有 1 项失败：`room-safety-production-chain.test.mjs` 要求 `/api/v2/room-safety`，运行时业务源码在并发修改后变为 `/api/v1/room-safety`。此前同一轮曾观测到该源码为 `/api/v2`，故不覆盖另一 AI 的业务修改，保留为真实快照漂移/接口坐标失败。

### Admin

- 通过：ESLint、严格类型检查、4 个测试文件/14 个测试、API ledger、生产构建、依赖审计（0 已知漏洞）。

## 暂缓项

按要求未执行 Cocos 编辑器、浏览器、移动端和真实设备人工验收。
