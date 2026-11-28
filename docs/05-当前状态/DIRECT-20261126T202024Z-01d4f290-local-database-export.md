# 小写 aoo 本地实际数据库安全导出

## 任务合同

- Task ID：`DIRECT-20261126T202024Z-01d4f290`
- Owner / 唯一 Writer：`/root/aoo_runtime_lead`
- 用户目标：只盘点并导出 `/Users/aoo/Code/Game/aoo` 当前本地运行实际使用的全部数据库，形成可交付的结构+数据归档和隔离恢复证据。
- 当前状态：`DEV_SELF_CHECK_DELIVERABLE_READY`
- 验收：实际引用库逐库一致性导出；清单/导入顺序/校验和/对象与行数摘要；同主版本隔离恢复验证；非引用的辅助/测试库明确排除。

## 权威证据与范围

- 运行入口：`work/local-runtime-aoo.py`，其当前 `DB_URL` 指向 `127.0.0.1:3306/aoo_login_local`。
- 运行证据：当前小写项目 Java 进程与 `127.0.0.1:3306` 存在 ESTABLISHED 连接；端口由 `aoo-mysql` 容器映射。
- 写入白名单：本卡、本卡 `.direct-lock/**`，以及新建 `/Users/aoo/Code/Game/数据库/Aoo-lowercase-local-databases-20261126T202024Z/**`。
- 数据源权限：仅一致性读取与导出，不修改/删除源库，不启停目标运行栈。

## 禁改与停止条件

- 不读取、修改或启动 `/Users/aoo/Code/Game/BCG/Aoo`；不触碰旧交付 `Aoo-local-databases-20261126T200052Z`。
- 不连接远程、不做 Git 操作、不输出密码/Token，不启动无当前引用证据的历史容器。
- 若发现 metadata lock、长事务、DDL/迁移、非本任务活动查询或空间不足，立即 fail-closed。
- 导出使用 `--single-transaction --quick --skip-lock-tables --routines --events --triggers --hex-blob --set-gtid-purged=OFF --default-character-set=utf8mb4 --no-tablespaces --databases`，临时文件经退出码、非零、`gzip -t` 后才原子改名。

## Ready / Evidence

- [x] 用户明确纠正为小写项目并授权本地只读导出。
- [x] 精确白名单、唯一 Writer、禁改和验收已闭合。
- [x] 目标项目无自身 `AGENTS.md`；已读全局治理、目标 `SWARM.md`、task-dispatch/task-scope-guard 及 SWARM skill。
- [x] 两次运行安全守卫：连续两次 metadata lock、超过 60 秒事务、DDL/迁移及业务库非本任务活动查询均为 0。
- [x] 导出、校验和、隔离恢复：同版本 MySQL 8.0.46 恢复通过；367 张基础表逐表精确行数一致，9 个视图、4 个存储过程、24 个触发器一致，事件为 0。
- [x] 释放 Writer 锁并记录交付证据。

## 交付证据

- 交付目录：`/Users/aoo/Code/Game/数据库/Aoo-lowercase-local-databases-20261126T202024Z`
- dump：`dumps/aoo_login_local.sql.gz`，16,763,736 bytes，SHA-256 `b1538b989a9960082f6939c9f6082d979bb9626b9609b1abb5a9f7a63fcc001f`。
- 校验清单：30 个交付文件全部纳入 `CHECKSUMS.sha256`；清单自身 SHA-256 `2dc9a378d5a8ff20adec9120710fc626cd7325f0828d969905027f4c07390cf8`。
- 隔离恢复：新建容器 `aoo-lowercase-db-restore-20261126t202024z`，`--network none`、无端口映射；恢复和对账通过后已停止。
- 排除：主实例 22 个 scratch/test/verify/shadow schema，以及 13306、13328 两个隔离环境；均无当前运行引用证据，未导出、未启动或修改。
- 边界：未连接远程；未做 Git；未修改、删除或停止源库；未读取或修改 BCG/Aoo；未触碰既有旧交付目录。

