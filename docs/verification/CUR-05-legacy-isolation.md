# CUR-05｜旧版隔离终验

## 状态

**最终结论：PASS，CUR-05 已签署完成。**

CUR-01/R04、CUR-02/登录资源、CUR-03 权威恢复与 CUR-04 构建治理的相关修改稳定后，已于 2026-08-25 10:50 +0800 使用只读旧树执行最终完整复核。报告满足 `mode=full`、`legacyRootPresent=true`、旧资源根与 UUID 实际载入、`blocking=0`、`passed=true`、`signable=true`，门禁单测 11/11 通过。

CUR-01、CUR-02、CUR-03、CUR-04 合并稳定后，必须使用只读目录 `/Users/aoo/Code/Game/BCG/Test` 执行完整模式。只有报告同时满足 `mode=full`、`legacyRootPresent=true`、旧资源根与 UUID 实际载入、`blocking=0`、`passed=true`、`signable=true` 且门禁单测通过，才允许签署 CUR-05。任何中间快照、`--no-legacy-uuid` 文本模式或历史报告都不能作为完成证据。

## 独占门禁范围

- 门禁：`Server/tools/legacy-isolation/gate.py`
- 单测：`Server/tools/legacy-isolation/test_gate.py`
- 说明：`Server/tools/legacy-isolation/README.md`
- 审计输出：`Server/tools/legacy-isolation/audit.json`
- 本报告：`Server/docs/verification/CUR-05-legacy-isolation.md`

未修改 Client、Server、Admin 的业务源码、构建装配、场景、预制体、资源或其他 owner 的验证文件。门禁业务命中仅登记责任方，CUR-05 不跨边界整改。

## 固化检测面

Client、Server、Admin、CI/构建、Scene、Prefab、Meta/资源索引、动态加载路径、2.22、Legacy 运行时、Native prefab 重复副本、qh/QH/情怀标识、旧接口、旧 UUID、旧依赖和重复入口。完整模式从只读旧树提取 Prefab/Scene UUID，并与当前资产引用交叉核对。

## 复现

```sh
python3 Server/tools/legacy-isolation/test_gate.py
python3 Server/tools/legacy-isolation/gate.py \
  --legacy-root /Users/aoo/Code/Game/BCG/Test \
  --output Server/tools/legacy-isolation/audit.json
```

## 基线与责任方

初始基线不构成最终复跑或完成签署。2026-08-25 初始完整扫描为：`mode=full`、`filesScanned=25888`、`legacyUuids=3665`、`findings=64166`、`blocking=1`、`passed=false`、`signable=false`；门禁单测 11/11 通过。

初始唯一阻断归 Server/LuckDraw owner：`Server/server/LuckDraw/src/main/java/com/aoo/bcg/luckdraw/LuckDrawHttpRoutes.java:9` 注册可执行业务处理的 `/legacy/v1/luck-draw`。owner 移除该旧路由后，CUR-05 立即完整复跑：`filesScanned=25740`、`legacyUuids=3665`、`findings=64165`、`blocking=0`、`passed=true`、`signable=true`，门禁单测 11/11 通过。CUR-05 未修改该业务文件。

阻断按首段路径归属 Client、Server、Admin 或构建/配置 owner；CUR-05 只报告，不修改其业务文件。

## 最终合并复跑签署

| 验收项 | 最终结果 |
|---|---:|
| 模式 | `full` |
| 扫描文件 | 27,700 |
| 只读旧树 UUID | 3,665 |
| 全部 findings | 64,547 |
| 未复核只读证据 | 7,341 |
| blocking | **0** |
| passed | **true** |
| signable | **true** |
| 门禁单测 | **11/11 PASS** |

最终完整扫描覆盖 Client、Server、Admin、构建与 CI、场景、预制体、资源、动态路径，以及 2.22、Legacy、Native prefab、qh/QH/情怀、旧接口、旧 UUID、旧依赖和重复入口。`Server/tools/legacy-isolation/audit.json` 为本次最终机器证据；未使用 `--no-legacy-uuid`，未使用 allowlist 掩盖生产/配置/构建命中。
