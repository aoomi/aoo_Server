# 2.22 只读参考制品

`common-lib/` 是从 `server/common/lib` 物理移出的 2.22 Jar/工具制品，仅用于行为对照、历史格式分析和隔离转换。

- 禁止加入 Maven/Creator 生产 classpath。
- 禁止从启动脚本、容器、发布包或玩法代码直接引用。
- 需读取旧数据时，必须通过独立 adapter/转换工具产生新格式，生产运行时不加载本目录 Jar。
- 删除须执行 UNUSED37 审批门禁。

`root-common-runtime/` 保存从活动目录移出的 `common/bin` 与 `LegacyCommon/bin` Ehcache 磁盘状态，仅用于历史运行行为取证，不进入生产运行链。

全部文件的来源、哈希、推断版本、用途和发布处置由 `docs/generated/legacy-common-binary-inventory.json` 记录；其中所有项 `productionRequired=false`。
