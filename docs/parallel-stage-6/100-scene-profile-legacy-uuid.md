# 100｜Scene Profile Legacy UUID 清理

验证时间：2026-08-24  
目标：`Client`（Cocos Creator 3.8.8）

## 结论

`Client/profiles/v2/packages/scene.json` 中 6 个旧 UUID 均属于 Creator 编辑器的 Scene 相机视图缓存，对应资源已从当前工程删除，不存在可解析且语义等价的新 Meta UUID。依据 Creator 3.8.8 当前 profile schema，已同时删除 `camera-infos` 中的整项定义及 `camera-uuids` 中的引用；未生成 UUID、未修改资源 Meta、未扩大 allowlist。

并发任务 103 完成 `assets/Login/Scenes/LoginScene.scene` 写入后重新复核：新增账号入口使用原生 `UITransform/Button/Label`，SpriteFrame 为 `null`，未新增或恢复上述 UUID。为避免 Creator/构建再次写回陈旧 profile 后只能依赖旧工程交叉扫描发现，87 gate 现内置这 6 个已退役 Scene profile UUID 的永久拒绝集合；即使使用 `--no-legacy-uuid`、旧工程未挂载或本地 UUID 分类发生变化，定义或引用任一命中仍按 `configuration-or-build` 阻断。

新增 `Server/tools/legacy-isolation/sanitize_scene_profile.py`，在 Creator/构建完成后以同一权威拒绝集合同时清理定义与引用，并原子替换 profile；它校验 Creator schema、UUID 列表唯一性和定义/引用集合一致性，不会生成或猜测 UUID。门禁单测同时覆盖永久拒绝集合和清理器的定义/引用同步行为。

87 完整门禁复验结果：`blocking=0`、`passed=true`。

## 逐项定位

| 旧 UUID | Creator AssetDB 历史 URL | 当前资源/Meta | 处理 |
|---|---|---|---|
| `3842e73c-4567-42eb-b5b6-7a1e499f42cd` | `db://assets/Login/Prefab/Native/Login01.prefab` | 不存在 | 删除相机缓存定义与引用 |
| `40995e53-6714-4fb1-8d39-6e25f4c0aa90` | `db://assets/Login/Prefab/Native/Login02.prefab` | 不存在 | 删除相机缓存定义与引用 |
| `e293c14b-a711-44ce-83f0-89a1e70e6361` | `db://assets/Login/Prefab/Scenes/reloadScene.scene` | 不存在 | 删除相机缓存定义与引用 |
| `ef7520f3-b8be-4b69-a3c3-205dd36c5b42` | `db://assets/Login/Prefab/Scenes/mainScene.scene` | 不存在 | 删除相机缓存定义与引用 |
| `1ebde2bc-327d-4f89-ad64-60fdf976469c` | `db://assets/Login/Prefab/Scenes/launchScene.scene` | 不存在 | 删除相机缓存定义与引用 |
| `2e1d38db-1cf9-41e2-88af-6a182561a07b` | `db://assets/Login/Prefab/Scenes/Bootstrap.scene` | 不存在 | 删除相机缓存定义与引用 |

历史 URL 取自 Creator 3.8.8 本地导入索引 `Client/library/.assets-data.json`；对当前 `Client/assets` 的资源与 `.meta` 核验均为缺失，因此不能以任意本地 UUID 替换。

## 验证

- Scene profile JSON 解析通过；`camera-infos` 键集合与 `camera-uuids` 集合一致，列表无重复，当前共 44 项。
- V11 UUID 门禁：33,634 个 Meta UUID，重复 0、Meta 解析错误 0；145,675 条直接引用，缺失 0；21,836 条 Creator 内建引用。
- V11 Prefab/Scene 门禁：1,626 个序列化资源（1,616 Prefab、10 Scene），JSON 解析错误 0；PREF01-PREF12 组件映射审计通过，共 43 类组件。
- 87 门禁单测：7/7 通过，包含已退役 Scene profile UUID 永久门禁及清理器同步测试。
- 87 完整扫描：25,320 个文件，61,031 条证据，生产/配置/构建阻断 0，`passed=true`。

## 协调提示

Creator 3.8.8 编辑器若仍打开本工程，退出或触发 profile 保存时可能把内存中的旧相机缓存重新写回 `scene.json`。永久门禁会使此类回归稳定失败且不能被 allowlist；V03 最终复跑仍应在 Creator 写入结束后执行。
