# 导入顺序

1. 校验：在本目录执行 `shasum -a 256 -c CHECKSUMS.sha256`，并对 dump 执行 `gzip -t dumps/aoo_login_local.sql.gz`。
2. 准备全新 MySQL 8.0.46 实例，字符集 `utf8mb4`；不要与现有实例或其他归档合并。
3. 先审查 `DEFINERS.tsv`，确认目标实例具备相应账户/权限策略。dump 已包含 `CREATE DATABASE` 与 `USE`。
4. 导入：`gzip -dc dumps/aoo_login_local.sql.gz | mysql -u <管理员用户> -p`。不要把密码写入命令历史或文档。
5. 对照 `OBJECT_COUNTS.tsv` 与 `TABLE_ROW_COUNTS.tsv` 核验对象数和每张基础表精确行数。
6. 仅在隔离验证通过并由接收方确认后，才将应用指向恢复实例；本交付未执行该切换。
