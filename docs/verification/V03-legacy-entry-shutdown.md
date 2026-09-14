# V03｜旧入口与重复入口关闭验证

审计日期：2026-08-24

## 结论

本专项已关闭 Gateway 与客户端可直接利用的旧 WS/PHP/重复入口：Gateway 仅精确允许
`/api/v2/gateway/ws_ticket` 和 `/api/v2/gateway/ws`，其余旧 WS、PHP、`ClientPack`、
`JavaServerPack`、`/legacy`、`/v1`、`/api/v1` 均 fail-closed。客户端 WSS 在主网络实现和
CompatibilityApp 传输实现两层都强制校验唯一 WSS path、TLS、无凭据 URL、唯一 ticket 参数；
玩法服地址不得再从服务端下发的 IP/port 或 URL 直连，而是统一回到 gatewayOrigin。

客户端生产源码已移除 PHP 外跳：亲友圈汇总复用原生战绩页，举报进入受鉴权的 Support/Room
Safety 流程。Admin 的旧通用 CRUD 页面、路由和可配置 endpoint 已删除。未扩大 87 allowlist。

## 自动证据

- Gateway 定向 Maven 测试通过：`HttpGatewayContractTest`。
- V02 机器门禁中 `noLegacyHttpEntry=true`、`noLegacySendPackEntry=true`、
  `allClientHttpPathsHaveServerContext=true`、`noDirectExternalHttpBypass=true`，最终 `PASS` 且
  `findings=[]`。Admin 第三方地图请求已收口到受控的 `/api/v2/admin/map/ip-location` 服务端代理。
- `room-common-controller`、`room-reconnect-state`、`spectator-flow` 共 7 项通过。
- gameServer 六模块 reactor 在 `-Dexec.skip=true -DskipTests compile` 下通过。
- V09 指定 GameSPI、GameCommon、Poker、Mahjong、LongCard、WordCard、Gateway 全 reactor 通过。
- 87 legacy-isolation 最终完整扫描：`filesScanned=25320`、`findings=61031`、`blocking=0`、
  `passed=true`；未扩大 allowlist。

## 联合门禁签署

并发任务产生的 `Client/profiles/v2/packages/scene.json` 陈旧 UUID 已由对应 owner 清零；随后在同一
合并快照重新执行 V02、V03、V06、V09 和 87 门禁，自动化阻断均已归零。V06 的 WSS envelope 已把
common room 身份字段提升到顶层，并补齐 shuffle/kick Bridge 白名单；客户端专项测试、Hall saga
测试和 gameServer 编译均通过。

`ProductionRoomRealtimeService` 与 JDBC journal 已使用持久化 `stateVersion` CAS，不再把 transport
seq 当权威版本；event、snapshot、outbox 在同一事务提交。WordCard 权威结算回归修复后，V09 全
reactor 再次退出码 0。

人工、真实 TLS/WSS、真实数据库和真实设备验收按要求暂缓。

## 2026-08-24 V09 CAS 联合复验

此前记录的 JDBC 权威版本阻塞已关闭：生产提交器已删除房间 `AtomicLong` 事件序列，改从持久化快照读取已提交 `state_version`，只接受 `persisted + 1`；`JdbcRoomEventJournal` 在同一事务中以 `SELECT ... FOR UPDATE` 和 `UPDATE ... WHERE room_id=? AND fencing_token<=? AND state_version=?` 执行 CAS，并原子写入 room event、snapshot、outbox。公共房间实时写也不再使用 transport `command.sequence()` 生成 `stateVersion`。

V03 静态复扫未发现 `stateVersion = transport seq` 或房间 `AtomicLong` 替代持久化版本。旧入口关闭结论不变，V03 自动化验收通过。
