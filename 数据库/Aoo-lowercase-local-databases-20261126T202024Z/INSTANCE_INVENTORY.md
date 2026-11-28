# 实例与范围清单

## 纳入交付

| 角色 | 引擎/版本 | 地址 | 数据库 | 使用证据 |
|---|---|---|---|---|
| PRIMARY_RUNTIME | MySQL 8.0.46 | `127.0.0.1:3306`（容器 `aoo-mysql`） | `aoo_login_local` | `work/local-runtime-aoo.py` 的当前 DB URL；小写项目 Hall/Gateway 进程到 3306 的 ESTABLISHED 连接；容器端口和卷证据 |

源实例 UUID、容器 ID、卷名和捕获时间保存在 `evidence/`，不包含凭据。

## 明确排除

- 主实例中的 22 个 `*_scratch_*`、`*_verify*`、`*_shadow*`、`*_fresh*`、`*_clone` 等临时/测试 schema：当前启动配置未引用，运行进程无连接证据，不冒充业务交付。完整名字见 `evidence/all-schemas.txt`。
- `127.0.0.1:13306` 的 `aoo-local-lobby-r1-mysql`：标签归属隔离验证环境，当前小写运行栈无连接证据。
- `127.0.0.1:13328` 的 `aoo-source-clone-v6-r7-mysql`：标签归属 source-clone 隔离环境，当前小写运行栈无连接证据。
- 已停止的历史容器/卷：未启动、未挂载、未导出。
- `/Users/aoo/Code/Game/BCG/Aoo` 和既有 `Aoo-local-databases-20261126T200052Z`：未读取、未修改、未覆盖，均不属于本次交付。
