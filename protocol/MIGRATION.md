# Aoo 通信协议完整迁移规范

## 目标

所有 HTTP、WSS、登录、大厅、亲友圈、公共房间和子游戏共用一份机器协议源，业务代码不得自行拼装报文。迁移期间 V1、V2 双栈运行，可按账号、渠道、玩法和房间灰度，可一键回退 V1。

## 固定目录

| 内容 | 唯一位置 |
|---|---|
| 机器协议源 | `Server/protocol/aoo-protocol-v2.json` |
| 协议发布说明 | `Server/protocol/README.md` |
| 服务端运行时 | `Server/server/common/src/com/ddm/server/protocol/v2` |
| 客户端运行时 | `Client/assets/Common/Code/Runtime/network` |
| 人读规范 | `Client/docs/前端框架规范/架构设计/前后端通信规范.md` |

## 强制依赖方向

`业务模块 -> ProtocolV2/ProtocolRegistry -> Transport`。业务模块禁止直接依赖 `LegacyPacketCodec`、旧 `MessageHeader` 或自行操作 WebSocket。

## 迁移批次

| 批次 | 范围 | 放量顺序 | 回滚单位 |
|---|---|---|---|
| M1 | 登录、Token、一次性 wsTicket | 内部账号→测试渠道→全量 | 账号/渠道 |
| M2 | 大厅目录、资源版本、公告 | 1%→10%→50%→100% | 接口版本 |
| M3 | 亲友圈成员、房间、权限、战绩 | 单亲友圈→地区→全量 | clubId |
| M4 | CommonRoom 入退房、准备、聊天、托管、重连 | 测试房→灰度房→全量 | roomId |
| M5 | 内江跑得快 | 新房间灰度，存量房保持原版本 | roomId/playVersion |
| M6 | 成都血战麻将 | 新房间灰度，存量房保持原版本 | roomId/playVersion |
| M7 | 其他子游戏 | 按玩法逐个迁移 | gameId/playVersion |
| M8 | V1 下线 | 调用量连续 30 天为零后删除 | 整体回退版本 |

## 当前完成状态

| 范围 | 代码状态 | 验证状态 |
|---|---|---|
| M1-M7 双端协议运行时、兼容桥、HTTP/WSS 接入 | 已完成 | 协议 CI、Creator 3.8.8 TypeScript、JDK 26 Maven Reactor 已通过 |
| 机器协议源、双端生成元数据、破坏性变更基线 | 已完成 | 生成结果一致性和契约完整性检查已通过 |
| 幂等、防重放、wsTicket、房间版本锁、玩家视角裁剪边界 | 已完成 | 已接入统一协议运行时 |
| M8 自动采集、活跃 V1 房间计数、退休检查、演练审计 | 已完成 | 运维门禁已形成闭环 |
| M8 线上 30 天零流量与真实回滚演练 | 等待真实生产周期 | 不允许伪造或提前标记完成 |

## 每条消息的完成标准

1. 机器协议源登记 `msgId`、方向、请求体、响应体、权限、错误码和版本。
2. 客户端只通过 `ProtocolV2Client` 调用，服务端只通过 `ProtocolRegistry` 注册。
3. 写请求具备 `requestId` 幂等、`seq` 防乱序、`timestamp` 防重放。
4. 房间消息校验 `roomId`、`roundNo`、`playVersion`，身份从连接上下文读取。
5. 下行 DTO 按玩家视角生成，禁止完整房间对象广播后由前端隐藏。
6. 重连返回权威快照与缺失事件，不返回其他玩家暗牌。
7. 具备 V1/V2 对照回放、错误码映射、指标监控和灰度回滚开关。

## 可持续发布流程

1. 先改 `aoo-protocol-v2.json`，禁止先写业务代码。
2. 协议生成器产出 TypeScript/Java 契约，生成文件禁止手改。
3. CI 检查重复 `msgId`、重复错误码、破坏性字段修改和未登记消息。
4. 同主版本只允许新增可选字段；删除、改类型、改语义必须升主版本。
5. 房间创建时锁定 `playVersion` 与协议版本，牌局中途不得漂移。
6. 发布后按 `protocolVersion/msgId/code/playVersion` 监控成功率、延迟、重连和拒绝量。

## V1 下线门槛

V2 全链路覆盖；线上 V1 调用连续 30 天为零；全部活跃房间结束；回放和重连兼容数据完成迁移；应急回滚演练通过。未同时满足时不得删除兼容层。

## M8 运维执行规范

1. 游戏大厅每 15 分钟自动写入一次当日协议采集心跳，心跳与业务流量分离；每日由监控任务执行 `tools/protocol-v1-retirement-status.sh`，输出必须归档到发布记录，脚本只读 Redis，不修改业务状态。
2. 观察周期使用 UTC 自然日。最近 30 天每天都必须存在采集标记且 V1 请求量为零，缺少采集数据不能按零流量处理。
3. `protocol:v1:active-rooms` 必须为零。计数由房间创建和销毁生命周期自动维护，禁止人工改数绕过门禁。
4. 回滚演练必须验证 V2 灰度切回 V1、存量房间不中断、重连成功、账务与结算一致。完成后使用 `tools/protocol-v1-record-rollback-drill.sh` 登记演练证据。
5. 演练登记必须包含演练编号、执行人和证据地址；脚本写入通过状态及结构化审计记录，不接受无证据通过。
6. 任一天出现 V1 流量，30 天零流量观察期从该日之后重新计算。门禁未通过时禁止删除 V1 兼容代码、协议映射和回滚开关。
7. 门禁通过只代表允许进入删除发布流程。删除前仍须创建可整体回退的发布版本，并保留数据库、Redis 和回放数据兼容性。

## WebSocket 生产入口配置

浏览器 WebSocket 必须配置官方来源白名单，多个域名使用英文逗号分隔。服务端按完整 `scheme://host:port` 精确匹配，不使用后缀或模糊匹配。原生 App 没有 `Origin` 请求头时仍由一次性 `wsTicket` 完成身份校验。

```bash
export ALLOWED_WEBSOCKET_ORIGINS='https://h5.example.com,https://club.example.com'
```

未配置白名单时拒绝所有携带 `Origin` 的浏览器连接，防止生产环境误开放；本地 H5 调试必须显式加入本地开发地址。

### 标准命令

```bash
# 默认连接 127.0.0.1:6379，可使用 REDIS_HOST/REDIS_PORT/REDIS_PASSWORD 覆盖
tools/protocol-v1-retirement-status.sh

# 仅在真实演练通过后执行
DRILL_ID=DRILL-2026-001 \
OPERATOR=operator_name \
EVIDENCE_URL=https://internal.example/drills/DRILL-2026-001 \
CONFIRM=YES \
tools/protocol-v1-record-rollback-drill.sh
```

状态检查退出码：`0` 表示允许进入 V1 删除发布流程，`2` 表示仍有门禁阻塞，其他退出码表示工具或 Redis 连接异常。
