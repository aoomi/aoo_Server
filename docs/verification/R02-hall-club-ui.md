# R02 大厅与亲友圈 UI 整改验证

日期：2026-08-24

## 范围与边界

本轮仅整改 `Client` 大厅、亲友圈以及非游戏房间公共 UI/组件。未修改登录场景、`Client/profiles/v2/packages/scene.json`、游戏玩法 UI、客户端 Bootstrap/Manager 核心、协议 DTO、Server/Admin 生产代码，也未恢复旧 prefab 或旧接口。游戏内 UI 继续留给 V07。

## 已完成整改

- 公共交互：修复弹窗栈中的失效节点、重复入栈、激活和层级；增强返回栈与 modal 遮罩输入阻断契约。
- 公共组件：修复数字设置缺失时错误回落为 `0`；修复节点池重复释放、销毁节点复用与清理；为虚拟列表和分页补充非法尺寸、overscan、NaN、小数及越界页防护。
- 亲友圈入口：列表加载、创建、置顶、进入、数字键盘加入均加入请求 single-flight/重复点击保护。
- 亲友圈主界面：邀请搜索/发送、保险箱、切圈、房间详情与快速加入加入并发保护；异步回调使用 epoch、表单显示状态和节点有效性检查，避免关闭后回写。
- 状态呈现：亲友圈列表、切圈、快速加入和房间列表支持 loading/content/empty/error 终态切换，失败后不会遗留阻塞遮罩；空列表和缺失滚动节点安全处理。
- 首开与资源：资源换肤回调增加 visual epoch，旧请求不能覆盖新画面，降低首开及快速切换闪屏。
- 输入与遮罩：输入在请求前完成 trim/数字校验；亲友圈弹窗根节点吞掉触摸开始、移动、结束、取消阶段，阻止遮罩穿透。

## 自动化覆盖

新增：

- `Client/tests/ui/r02-hall-club-interaction-contract.test.mjs`
- `Client/tests/Club/club-ui-resilience.test.mjs`

覆盖按钮、弹窗、滚动、输入、返回、加载、空态、错误态、重复点击、遮罩穿透、异步取消、首开延迟终态以及公共组件复用契约。

## 验证结果

在 `Client` 目录执行：

```sh
for f in tests/ui/*.test.mjs tests/Club/*.test.mjs; do node "$f" || exit 1; done
node scripts/verify-component-registration.mjs
node scripts/verify-client-manager.mjs
node scripts/audit-client-runtime-deps.mjs
```

结果：

- UI 测试 6 个文件全部通过；`lobby-ui-v05` 6/6 子测试通过。
- Club 测试 2 个文件全部通过。
- Creator 组件注册门禁通过：38 个 `ccclass`、33648 个 UUID。
- Client manager 门禁 5/5 通过。
- 客户端运行时依赖审计通过，核心禁区引用计数均为 0。
- TypeScript `tsc --noEmit` 未列为通过项：当前 `Client` 依赖未安装 `tsc`，`pnpm exec tsc` 返回 `Command "tsc" not found`。

## 主要生产文件

- `Client/assets/Common/Code/UI/Interaction.ts`
- `Client/assets/Common/Code/UI/Presentation.ts`
- `Client/assets/Common/Code/UI/UnifiedScroll.ts`
- `Client/assets/Club/Code/Runtime/LegacyClubEntryController.ts`
- `Client/assets/Club/Code/Runtime/LegacyJoinClubController.ts`
- `Client/assets/Club/Code/Runtime/LegacyClubMainController.ts`

