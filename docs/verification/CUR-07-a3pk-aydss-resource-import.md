# CUR-06/07 A3PK、AYDSS 玩法资源导入证据

日期：2026-08-25（Asia/Shanghai）

## 只读来源核验

- A3PK 完整 Creator 3.8.8 资源树存在于 `/Users/aoo/Code/Game/BCG/Test/QH_DFMJ/client-next-3.8.8/assets/resources/native-ui/game/a3pk`，约 4.4 MB、160 个文件；目标入口 `resources/game/A3PK/ui/A3PKPlay.prefab` 与其 `.meta` 均存在。
- AYDSS 完整旧客户端资源存在于 `/Users/aoo/Code/Game/BCG/Test/情怀1.0前端/Client_subgame/Client_aydss/assets`，约 22 MB、1793 个文件、55 个 prefab；不是只能取得单 prefab 的残片。
- 当前加载代码仍分别指向 `native-ui/game/a3pk/resources/game/A3PK/ui/A3PKPlay` 与 `native-ui/game/aydss/resources/game/AYDSS/ui/UIAYDSSPlay`。

## 当前门禁结果

执行：

```text
cd Client
node scripts/verify-cur07-editor-sync.mjs
node scripts/verify-creator-native-assets.mjs
```

结果：

- CUR-07 静态预检统计 `scenes=10, prefabs=1610, metas=13692, uuids=33712, dynamicPaths=10`；唯一未解析动态路径正是 A3PK、AYDSS 上述两项。
- Creator native asset gate 通过：`1620 scenes/prefabs, 25443 assets/meta files, 33706 UUIDs, blocking=0`。该结果只证明现存资源树一致，不证明两个缺失玩法树已导入。

## Creator 3.8.8 阻塞

Computer Use 只读检查确认官方 Creator 3.8.8 正打开当前项目，窗口标题为 `LoginScene.scene* - aoo-card-game-frontend-architecture - Cocos Creator 3.8.8`。星号表示登录场景存在未保存编辑器状态。登录场景属于强制只读布局禁区，因此本轮没有关闭、重开、保存场景或启动会覆盖该内存状态的竞争导入实例。

未执行二进制资源复制：完整迁移需要保留来源资源树中的 prefab、纹理、音频、动画、脚本及 UUID 依赖，不能通过 `apply_patch` 伪造二进制资源或 `.meta`。也未把 A3PK/AYDSS 指向公共房间 UI；A3PK 可找到 NJPDK 族 UI，但语义不等价，AYDSS 当前项目没有可证明等价的长牌玩法 prefab。

## 状态

`待编辑器同步（登录场景存在未保存状态）`。解除条件：用户先处理或明确放弃当前 LoginScene 未保存状态，再由 Creator 3.8.8 正常导入两套完整依赖树，复核 Asset Browser 两条目标路径、Console 相关 warning/error 为 0，并关闭重开验证一致性。CUR-06/07 不得据此标记通过。
