# V08 数据库与玩法配置专项验证

日期：2026-08-24

## 结论

专项代码边界已整改：地区、玩法、规则、版本、组件、房费及房间配置均由 `aoo_*` 迁移表承载；建房只从活动编译索引取值并在事务内锁定版本；ConfigCenter 不再直连旧 `game_profile_version`，也不再用进程内 Map 保存房间配置。

## 机器证据

- `V20260824_01__game_catalog_component_model.sql`：地区、地区别名、玩法目录、地区可用性、玩法版本、规则定义/选项、UI 绑定、房费、组件及依赖。
- `V20260824_02__immutable_game_release_index.sql`：不可变发布包、地区发布、预编译建房索引、活动指针、缓存 epoch、房间规则锁。
- `V20260824_55__canonical_game_configuration_repository.sql`：新增唯一序列化配置仓储，并仅回填已进入 `aoo_play_version` 的旧数据。
- `JdbcHallRepository.create`：事务内 `FOR UPDATE` 读取活动索引并把发布版本写入房间，不存在索引时拒绝建房。
- `JdbcGameConfigurationRepositoryTest`：验证只读写新表、版本不可覆盖。
- `GameCatalogLoader` 对目录 ID、code、分类、family、地区范围和版本与原生 Provider 做逐项等值校验；任何交叉占用、重复行或分类冲突均在启动前失败。`CatalogLifecycleContractTest` 断言 528/528 唯一映射。
- 528 行静态唯一性复验：`awk` 检查 gameId/code 重复，输出为空；CDXZMJ 原生 Provider 与目录历史冲突已收敛为唯一 `(516,cdxzmj,CITY,SC,CD,legacy-equivalent-1)` 映射，517 不再存在。AYCP `(138,aycp)` 与 BYZP `(136,byzp)` 已注册原生 Provider。ServiceLoader 精确集合现为 9 个，声明无重复；新增项都有生命周期测试，不是目录桥或重复注册。
- R89-016 多源核对发现 NTCP、CP/CQCP、HNDZP/LHZP 的旧目录分类错误。519/ntcp 为 `MAHJONG/mahjong-lai-zi`；392/cp 为 `POKER/poker-510k`；394/cqcp 为 `POKER/poker-trick-taking`；267/hndzp 是海南地主牌，精确归入 `POKER/poker-hainan-landlord`；129/lhzp 是四人爬牌，精确归入 `POKER/poker-climbing`，两者均不得归入跑得快或字牌族。ZGCP/ZGDSS 虽复用 MahjongRoom 基类，权威客户端分别明确显示“自贡长牌”和“自贡斗十四”，因此保留 LONG_CARD 并改为精确 `long-card-zigong`、`long-card-zigong-da-si-shi`，不得由麻将标准族冒充。已同步 tool02/03/04、backend baseline 与 Bootstrap catalog；分类守恒为 MAHJONG 364、POKER 150、WORD_CARD 10、LONG_CARD 4，enabled 仍为 222。专属 Provider 均须在各自权威规则完整后才注册，不以分类修复冒充玩法完成。

验证命令：`JAVA_HOME=.../jdk-25.jdk/Contents/Home ./mvnw -Dexec.skip=true -pl server/ConfigCenter -am test -DskipTests=false`

结果：九个 reactor 模块 `BUILD SUCCESS`；ConfigCenter 17 个测试全部通过。`-Dexec.skip=true` 用于隔离其他并发任务写入 `tools/legacy-isolation/*` 导致的品牌门禁失败；首次正常门禁构建在编译前因此失败。

Bootstrap 全 reactor 复验命令：`./mvnw -Dexec.skip=true -pl server/Bootstrap -am test -DskipTests=false`。最新执行中前 27 个模块及其测试通过，之后被边界外 `server/RoomSafety/.../JdbcRoomSafetyService.java` 的 6 个语法错误阻断，Bootstrap 未进入执行。静态唯一性、映射和 Provider 审计均已通过，但在 RoomSafety 修复前不能宣称同快照全 Reactor 通过。

## 暂缓与边界

- 未执行真实 MySQL 迁移和真实设备验收；按任务要求暂缓人工/真实环境验收。
- AdminApi 仍有旧 profile 表直连，属于其他专项边界，本次未覆盖；旧表 contract/drop 必须等待该调用方迁移完成。当前 ConfigCenter 与建房链路已无旧表旁路。
