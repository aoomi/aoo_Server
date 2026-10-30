# R20 task-only runtime launcher

`launcher-r20.sh` is a fail-closed launcher for a disposable `/tmp` runtime. It is not an alternative to the shared local launcher and must never read `Server/work/local-runtime`, existing target staging, shared MySQL/Redis, shared ports, or shared four-terminal services.

## Contract

The caller must first create a task-owned directory matching:

```text
/tmp/aoo-c3-runtime-matrix-r26-<attempt>/
```

Copy `config-r20.env.example` into that directory, replace every placeholder with explicit task-owned paths/ports, and keep all bundle runners, manifest, JARs, DB preimage and token files under that attempt. The launcher rejects shell expressions, non-`R20_` keys, paths outside the attempt/bundle, symlinks, shared paths, shared ports, duplicate ports, missing token files and missing executable tools.

The sealed bundle must provide executable runner wrappers for disposable DB, DB cleanup, Version, Account, Hall, Social, Gifting, Media and Gateway. The Gateway runner must start `GatewayApplication` with the accepted R19 provider bundle. Game-WS and room-authority are not separate processes: they are readiness obligations of the Gateway runner.

The launcher validates:

- Java major 25 and `UTC`;
- accepted R19 `r9-v4` canonical manifest SHA `d2856d6c...097f7`;
- manifest JSON shape: 32 accepted rows and all seven runtime metadata fields;
- exactly one canonical manifest resource in the Bootstrap JAR;
- embedded manifest bytes equal the accepted manifest;
- exactly one `GatewayRuntimeProvider` service entry;
- complete sealed bundle SHA list (every regular JAR/runner/resource exactly once) and accepted DB preimage SHA;
- explicit disposable DB URL/schema/user/password file;
- disposable DB port and an attempt-owned readiness marker published by the DB runner (the launcher never treats an HTTP request to a MySQL port as readiness);
- unique non-shared DB, client, service and authority ports;
- task-local readiness URLs and the exact singleton official origin;
- all runner and token-file inputs.

## Commands

```sh
server/engineering/local-dev/task-only-runtime/launcher-r20.sh preflight \
  /tmp/aoo-c3-runtime-matrix-r26-<attempt>/config.env

server/engineering/local-dev/task-only-runtime/launcher-r20.sh start \
  /tmp/aoo-c3-runtime-matrix-r26-<attempt>/config.env

server/engineering/local-dev/task-only-runtime/launcher-r20.sh status \
  /tmp/aoo-c3-runtime-matrix-r26-<attempt>/config.env

server/engineering/local-dev/task-only-runtime/launcher-r20.sh cleanup \
  /tmp/aoo-c3-runtime-matrix-r26-<attempt>/config.env
```

`preflight` is the only safe operation before an independently accepted sealed bundle exists. `start` runs the complete dependency order only after preflight passes; a later runner/readiness failure triggers cleanup of the attempt-owned processes. It never reports downstream matrix stages as executed when startup fails.

`status` and `cleanup` require the same explicit config. Cleanup only uses attempt-owned PID files and the sealed DB cleanup runner. Credentials are read by task-owned runners from token files and are never printed into Evidence.

## Current R26 boundary

This implementation does not build the Java bundle, create a database, apply migrations, or start services. The current repository still lacks an independently accepted complete task-only bundle/service graph. Running `preflight` with the template therefore fails closed on the first missing/placeholder input; that is the expected safe result. No shared launcher or old target JAR is a valid substitute.

R26 hardening is fail-closed at the filesystem boundary: the attempt root and every child root must be canonical real directories, `..` and symlink traversal are rejected, and a non-directory bundle is rejected. The SHA manifest must contain every regular bundle file exactly once (except the manifest itself), so all JARs and runner files are covered by the sealed hash contract. Every sealed JAR is scanned: exactly one `GatewayRuntimeProvider` and at least one non-empty `GameProvider` service resource are required. `R20_OFFICIAL_ORIGINS` is exactly one value equal to the task client origin.

Each started runner is placed in its own session/process group by the explicit process-control tool. The launcher records the group owner, terminates the whole group, verifies the group is gone, and writes a cleanup receipt. `cleanup`/`stop` validate only the canonical attempt roots and cleanup timeout; they do not rerun bundle/hash/tool preflight, so cleanup remains available if a sealed bundle is later removed or corrupted.

## 隔离环境登录失败的处置与恢复边界

本节适用于已批准的任务独立环境，不替代上文 R20/R26 launcher 合同。先按该次合同识别使用的是 launcher 还是后续独立 runner；不得把旧 `config.env`、端口、容器 ID 或清理命令套到另一次运行。手册提供处置次序；未取得执行 Ready 和对应资源锁时只核公开记录，不运行下述恢复动作。

### 识别与停止

1. 核对合同给定的 namespace、当前资源 ID、网络/端口归属、制品与配置摘要、有效锁及独立结果；发现差异停止该作用域，交资源 Owner 核实。不要读取或记录完整 Env、Token、密码、账号/会话标识。
2. 将健康探针、注册、登录、会话与后续业务分别记结果。已有隔离 HTTP 证据曾出现健康 200、注册 200、登录 503；因此健康通过不能解除登录故障。首次业务断言失败立即停止剩余请求和 worker 旁证，保留请求名称/方法/路径、状态、去敏错误类别、响应摘要、时间及首错。不得重跑注册、换账号、循环登录或吞错继续。
3. 登录返回 503 不证明持久 session 不存在；事务提交与通知返回可能分开。先保留原请求/账号的受限关联信息在合同准许的私有位置，公开证据只存摘要与布尔关联结果。查询原 session 或恢复同一账号必须另有精确只读/恢复许可，禁止以新注册替代未知结果。
4. 保存首次失败、原 runner 输入和资源前像；更正追加到新 attempt，不能覆盖历史失败。相同失败指纹重复两次或第二轮实质独立失败时停止自动恢复并交主协调人工门禁，不换代理规避。

### 诊断与受控恢复

1. 首先由对应 Owner 对照已冻结的公开 endpoint 结构、实际 listener、运行 classpath/制品与目标 class 的来源。仅源码中的绑定地址、某 JAR 存在、TCP 存活或 503 响应都不能单独证明运行根因。
2. 已有诊断通过 endpoint/listener/运行字节码三证确认过一个缺陷：Account 跨容器通知指向 Gateway 网卡，而 Gateway 内部端口只监听本容器 loopback。内部会话路由保持内部认证，不得改走公开 ingress、开放 wildcard/public bind、关闭 Token 校验或忽略通知失败。私网绑定修正的独立源验证只证明限定源码行为，不证明真实登录恢复。
3. 修复采用精确方案→独立复核→源锁实施→源实际独立验证→新制品→runner/resource delta 独立复核→精确执行 Ready 的顺序；每一步核当前摘要与既有 custody。不得重用旧资源许可替换 JAR、重启、重导 DB 或更改网络。
4. **条件步骤，仍需实际恢复证据：**资源 Owner 在该次合同允许的身份和网络内确认目标 listener，再按准许次数进行内部连通/认证健康观察；测试 Owner 按批准的同账号/原请求和会话查询范围验证恢复。保留身份世代、原结果和撤销语义；通过后才继续原业务链的未完成部分，不以健康或源测试结果代替会话、WSS、浏览器、原桌与热备保护验证。具体命令、次数、目标 ID 和端口只从当次已冻结 runner/资源合同取得，本节不生成新的执行授权。

### 回滚、清理与恢复确认

- 若仍使用 R20/R26 launcher，上文 `status`/`cleanup` 只使用同一显式 attempt config；cleanup 以该 attempt 的 PID/进程组和已封 DB cleanup runner 为边界，不要求再次通过损坏 bundle 的完整 preflight。
- 后续独立环境由持有资源锁的 Owner 按该次已批准 recipe 执行 guarded rollback/清理：先核精确 ID/labels/前像与当前阶段，出现漂移停止。tester 只释放自身连接/进程，不代清主线容器、卷、网络、保留现场或他人锁。没有对应回滚许可时保留现场返回 Owner，不能用旧 launcher cleanup 批量删除。
- 单次恢复失败保留原始退出码/首错和部分成功；不能因 rollback 成功宣称业务恢复。真实恢复后分别确认会话与合法下一操作、旧授权失效、房间状态及保护重建；尚未验证的边界在独立报告中保留未验。备份隔离实例补 SQL 对象与本节登录恢复不同，不能写成生产容灾、删库恢复或 1 秒接管通过。

### 依据与未验范围

公开冻结依据位于 `docs/05-当前状态/evidence/DIRECT-20261008T084000Z-4a7f1c9e/`：
- `w8-current-b0-http-independent-20261014-low-r1/publication-health-recovery-r1/report.json`：隔离 HTTP 的健康/注册通过与登录失败、停止剩余请求事实。
- `w1-login-503-endpoint-route-diagnostic-20261014-low-r1/report.md`：公开 endpoint、listener 与运行字节码绑定缺陷；未保存的原 Java 异常不能补造。
- `w8-login-private-bind-source-independent-20261014-low-r1/receipt.json`：限定源码独立接受，明确 `runtimeAccepted=false`。

这些证据支持对应已验证事实及必要停止边界。本节未执行任何命令、真实登录恢复、重启/回滚、告警通知、浏览器/原桌/热备或生产演练；运行前仍需当次合同、独立门禁及有效锁。


## 隔离备份部分恢复故障与现场保留边界

本节只复用任务专属 synthetic 源/恢复实例的已验证处置。执行前按当次合同取得精确资源 ID、有效 Owner lease、fresh receipt、制品/runner 摘要与执行 Ready；旧实例名、凭证路径或昨日 gate 不授权再次运行。Owner 负责变更，非实施者负责只读后验；本节不授权生产操作。

### 首错停止与部分恢复判定

1. dump、加密、解密和 import 分别保留退出码、阶段、时间及去敏首错。任一阶段失败即停止，不用后续命令成功覆盖失败；超时或子进程启动异常保留已有输出，DDL 是否开始/完成未知时记 UNKNOWN，禁止盲重试。
2. 本次 ERROR1419 后，独立只读核对发现四表 schema/全量排序行摘要相同、账额一致，但 ledger_post trigger 缺失：只能记“部分恢复”。binary logging 开启、trust=0、restore_operator 无 SUPER 的实际条件解释该对象失败；不能把表行相同当完整恢复通过。
3. 保留原密文/sidecar、首错、源对象定义与部分恢复前像。不再次解密、重新导入或删除恢复实例，不提权 restore_operator、不授 SUPER、不改全局 trust 来绕过失败。相同指纹两次或第二轮实质独立失败停止自动恢复，交人工门禁。
4. 本次后续 ERROR1045 发生于只读前置且零 CREATE：原 localhost 角色被新 runner 强制改用 TCP127.0.0.1，改变认证 host 匹配。独立确认后仅把非 root SELECT/SHOW 恢复为已验证的容器内 socket 方式；root 管理用途的独立已批准 TCP 边界不变。不得猜密码、改账号或把连接失败误记为 DDL 已执行。更正、独立复核和新执行 Ready 均在原失败保留之后进行。

### 单对象显式管理补齐与独立验证

1. **需当次精确授权：**仅目标实例一次管理 CREATE TRIGGER，使用冻结源 SHOW CREATE、保留原 definer；先核目标 trigger 缺失、四表 schema/行摘要未变、definer 存在及约定上下文。现成 root 凭证仅允许该次专门用途，不输出内容，不转授给普通恢复角色。
2. 所有前置/输入/资源 gate 成立才由资源 Owner 执行批准的一次 runner；失败保存原始阶段/退出码，UNKNOWN 时停止，不重跑 CREATE。完成后关闭写连接，冻结 writesFinished 与 fresh 资源收据，交非实施者只读核验。
3. 已完成的独立后验检查包括：同两实例四表 SHOW CREATE 和全部排序行摘要、金额一致、trigger 前六字段及上下文、routines/events、角色自身 grants、binlog/trust 未变、密文 SHA/0600 与 sidecar 一致。该结果只证明“专属 synthetic 加密备份恢复数据与 schema，并经显式管理补齐 trigger 后等价”；原低权限 import 失败仍是失败。

### 验证后停止保留与再启动边界

1. 先封存完整独立后验，确认 QA 连接已关、Owner 写入结束及有效独占资源锁；再按已接受的生命周期合同核精确两实例 ID、名称/Owner、网络与卷创建身份、只读挂载及密文/秘密目录元数据。漂移停止，不读秘密内容。
2. 本次批准顺序为源实例后恢复实例，使用合同限定的 stop -t30；首错停止，不 kill、restart、换 ID，不 rm/prune，不删除容器、网络、卷、秘密或密文。命令参数仅取当次冻结 runner，本节不提供可直接复用的实例 ID。
3. 独立只读后验已确认同 ID exited、挂载/卷创建身份和内部网络保留、秘密目录保留且未读内容、密文 SHA 不变、主线四实例未动。容量回收只是该时点观测，不证明持续余量或安全业务容量。
4. 停止不含再启动许可。恢复需要另行 exact same-ID start、只读 readiness、有效 lease/fresh receipt 与精确合同；容器仍存在不能直接重启，停止后未运行的路径保持未验。最终删除保留对象也需另行明确授权。

### 依据与未验范围

公开冻结证据相对根目录 docs/05-当前状态/evidence/DIRECT-20261008T084000Z-4a7f1c9e/：
- w8-isolated-backup-actual-independent-20261014-low-r1/report.json 与 receipt.json：1419、四表部分一致及 trigger 缺失，原恢复拒绝。
- w8-isolated-backup-plan-review-20261014-low-r1/restore-ledger-trigger-admin-review-r1/connection-1045-diagnostic-r1/report.json 与 receipt.json：只读传输/host 匹配根因，零 DDL。
- 同一 restore-ledger-trigger-admin-review-r1/actual-socket-checkpoint-r1/report.json 与 receipt.json：显式管理补齐后的完整独立后验。
- w8-isolated-backup-plan-review-20261014-low-r1/lifecycle-stop-retain-actual-20261015-r1/report.json 与 receipt.json：同两实例停止且对象保留的独立后验。

本节没有运行任何命令或新增演练。生产脚本修复、低权限完整 import、PITR、篡改后合法交易补放、生产灾难恢复、重启后可恢复性和 1 秒接管均未由这些证据通过；不把手册写成 D15/D17 全部完成。
