# V11｜Prefab、Scene 与资源机器验证

验证时间：2026-08-24  
工程：`Client`（Cocos Creator 3.8.8）  
人工逐项打开：按要求暂缓

## 结论

V11 机器验证通过：当前 1,801 个 Prefab/Scene 均可被 Creator 3.8.8 导入并完成 Web Desktop 原生构建；Meta、顶层资源 UUID 与图片/字体/Spine 等子资源 UUID 已纳入同一引用闭包，缺失引用、重复 UUID、Meta 解析错误均为 0。构建产出包含 `main`、`common-prefab`、`club-prefab`、`lobby-prefab`、`mahjong01-prefab`、`poker01-prefab`、`Result` 和 `internal` 八个 Bundle，构建日志无资源 warning/error。

## 先验证后整改

首次复验发现两类真实问题：

1. 旧资源审计器只登记 `.meta` 顶层 UUID，错误地把 SpriteFrame 等 `subMetas` UUID 报为缺失，且不能发现子资源 UUID 冲突。
2. `Client/profiles/v2/packages/scene.json` 保留已删除 Scene 的编辑器相机索引；87 隔离门禁将其中旧 UUID 判为 `configuration-or-build` 阻断。

整改内容：

- `Server/tools/audit_cocos_uuid_references.rb` 改为递归登记 Meta 内所有 `uuid` 字段，保留 Creator 内建资源的显式识别，并按资源文件去重后判断冲突。
- 为 6 个重复的 Font/SpriteFrame 子资源 UUID 生成独立 UUID；现有 Prefab 引用继续指向唯一的原生主资源，消除 Creator AssetDB 歧义。
- 清除 Scene profile 的陈旧相机缓存；该缓存不参与生产 Scene/Prefab 构建，Creator 命令行构建后再次清理并由门禁复验。未建立任何 2.22、`Test` 路径、旧 Bundle 或 allowlist 旁路。

## 机器证据

| 检查 | 结果 |
|---|---|
| Prefab/Scene JSON | 1,801 个，解析错误 0 |
| Meta UUID 闭包 | 33,738 个 UUID，重复 0，解析错误 0 |
| 直接序列化引用 | 167,845 条；缺失 0；Creator 内建引用 25,157 条 |
| Creator 3.8.8 导入与构建 | `web-desktop` 35 秒完成，退出码 0 |
| Bundle | 8 个配置均生成并成功压缩、输出 |
| 图片/字体/Spine | 均经 Creator AssetDB 导入并进入同一 UUID/构建闭包；最新构建日志资源 warning/error 为 0 |
| 87 Scene UUID 阻断 | `Client/profiles/v2/packages/scene.json` 相关 finding 为 0 |
| 87 门禁单测 | 5/5 通过 |

主要证据：

- `Server/work/audit/cocos-uuid-reference-audit.json`
- `Server/docs/generated/creal08-meta-integrity.json`
- `Server/docs/generated/creal09-prefab-scene.json`
- `Server/docs/generated/pref01-json-version.json` 至 `pref12-test-mapping.json`
- `Client/temp/builder/log/web-desktop8-24-2026 21-28.log`
- `Server/tools/legacy-isolation/audit.json`

## 边界外阻塞与暂缓项

- `Client/build-v11` 经确认仅为本次机器验证临时产物，已移出工程树；后续 Creator 验证构建必须输出到工程外临时目录，`Client/.gitignore` 同时排除 `/build-*/`，历史构建资源不再进入生产源码扫描。
- 87 完整门禁最终复验为 `blocking=0`、`passed=true`，完整旧树 UUID 交叉扫描已开启，未使用 allowlist。
- 全 Client 严格 TypeScript 在最终并发快照仍被 Common/Lobby 业务源码诊断阻断，属于 V11 边界外；Creator 3.8.8 的实际项目脚本打包及资源构建已成功。
- Cocos Creator 编辑器逐个打开、保存全部 Prefab/Scene 的人工验收继续列为 M01 暂缓，不在本次机器通过结论中冒充完成。
