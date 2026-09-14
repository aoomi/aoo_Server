# R89-016 地区玩法权威源码核查

核查日期：2026-08-24  
当前仓：`/Users/aoo/Code/Game/BCG/Aoo`  
只读旧版：`/Users/aoo/Code/Game/BCG/Test`

## 结论

R89-016 所列 18 项在 Aoo 当前正式客户端中均没有独立 runtime/scene/controller，且服务端均没有对应原生 `*GameProvider`；资源、建房/战绩 prefab、类别规则核心和 `CatalogGameProvider` 均不计为玩法实现。

旧版 Test 的源码事实与先前“18 项均缺源码”口径不同：

- **16 项可建立迁移分片**：BYZP、BZP、GLZP、HNDZP、LHZP、PXPHZ、XPPHZ、YCHP、YCSDR、YZCHZ、AYCP、CP、CQCP、NTCP、ZGCP、ZGDSS。每项均有独立 `Client_subgame/Client_<code>` 客户端工程和 `Server_game_split/<CODE>/src` Java 服务端源码。
- **2 项客户端源码阻塞**：AHPHZ、LCZP。两项均有独立服务端 Java 源码，但旧版只找到大厅建房/战绩脚本或零散资源，没有对应 `Client_subgame` 牌桌工程，不能以同类复制或空壳补齐。
- 18 项服务端均有可审阅的旧权威源码，因此不存在“服务端确实无源码”项；迁移时仍须逐项重建新协议、持久化、恢复、回放和原生 Provider，不能直接把旧 MQ/旧协议旁路接回生产。

## 逐项证据

下表的“客户端源码数”只统计旧版独立 `Client_subgame/Client_<code>` 下 `.js/.ts`；“服务端源码数”只统计旧版 `Server_game_split/<CODE>/src` 下 `.java`。数量用于证明源码树存在，不代表已完成新框架验收。

| 类别 | 玩法 | Aoo 当前状态 | Test 客户端源码数 | Test 服务端 Java 数 | 只读权威来源 | 判定 |
|---|---|---|---:|---:|---|---|
| WORD_CARD | AHPHZ | 无独立客户端；无原生 Provider | 0 | 256 | `情怀后端原代码/Server_game_split/AHPHZ/src`；客户端仅 `QH_DFMJ/client/assets/script/ui/uiGame/ahphz` 大厅脚本及零散纹理 | **客户端阻塞；服务端可迁移** |
| WORD_CARD | BYZP | 无独立客户端；无原生 Provider | 504 | 315 | `情怀1.0前端/Client_subgame/Client_byzp`；`情怀后端原代码/Server_game_split/BYZP/src` | **客户端与服务端可迁移** |
| WORD_CARD | BZP | 无独立客户端；无原生 Provider | 105 | 228 | `情怀1.0前端/Client_subgame/Client_bzp`；`情怀后端原代码/Server_game_split/BZP/src` | **客户端与服务端可迁移** |
| WORD_CARD | GLZP | 无独立客户端；无原生 Provider | 128 | 236 | `情怀1.0前端/Client_subgame/Client_glzp`；`新情怀服务端源码/Server_game_2024元旦/GLZP/src`（另有 split 副本） | **客户端与服务端可迁移** |
| WORD_CARD | HNDZP | 无独立客户端；无原生 Provider | 106 | 220 | `情怀1.0前端/Client_subgame/Client_hndzp`；`情怀后端原代码/Server_game_split/HNDZP/src` | **客户端与服务端可迁移** |
| WORD_CARD | LCZP | 无独立客户端；无原生 Provider | 0 | 236 | `情怀后端原代码/Server_game_split/LCZP/src`；未找到 `Client_subgame/Client_lczp` | **客户端阻塞；服务端可迁移** |
| WORD_CARD | LHZP | 无独立客户端；无原生 Provider | 105 | 222 | `情怀1.0前端/Client_subgame/Client_lhzp`；`情怀后端原代码/Server_game_split/LHZP/src` | **客户端与服务端可迁移** |
| WORD_CARD | PXPHZ | 无独立客户端；无原生 Provider | 125 | 250 | `情怀1.0前端/Client_subgame/Client_pxphz`；`情怀后端原代码/Server_game_split/PXPHZ/src` | **客户端与服务端可迁移** |
| WORD_CARD | XPPHZ | 无独立客户端；无原生 Provider | 125 | 250 | `情怀1.0前端/Client_subgame/Client_xpphz`；`情怀后端原代码/Server_game_split/XPPHZ/src` | **客户端与服务端可迁移** |
| WORD_CARD | YCHP | 无独立客户端；无原生 Provider | 133 | 210 | `情怀1.0前端/Client_subgame/Client_ychp`；`情怀后端原代码/Server_game_split/YCHP/src` | **客户端与服务端可迁移** |
| WORD_CARD | YCSDR | 无独立客户端；无原生 Provider | 133 | 208 | `情怀1.0前端/Client_subgame/Client_ycsdr`；`情怀后端原代码/Server_game_split/YCSDR/src` | **客户端与服务端可迁移** |
| WORD_CARD | YZCHZ | 无独立客户端；无原生 Provider | 128 | 258 | `情怀1.0前端/Client_subgame/Client_yzchz`；`情怀后端原代码/Server_game_split/YZCHZ/src` | **客户端与服务端可迁移** |
| LONG_CARD | AYCP | 只有 native-ui 资源，无 runtime；无原生 Provider | 97 | 306 | `情怀1.0前端/Client_subgame/Client_aycp`；`情怀后端原代码/Server_game_split/AYCP/src`；另有 `QH_DFMJ/client-next-3.8.8/assets/app/aycp` TS 实现可交叉核对 | **客户端与服务端可迁移** |
| LONG_CARD | CP | 无独立客户端；无原生 Provider | 99 | 238 | `情怀1.0前端/Client_subgame/Client_cp`；`情怀后端原代码/Server_game_split/CP/src` | **客户端与服务端可迁移** |
| LONG_CARD | CQCP | 无独立客户端；无原生 Provider | 103 | 221 | `情怀1.0前端/Client_subgame/Client_cqcp`；`情怀后端原代码/Server_game_split/CQCP/src` | **客户端与服务端可迁移** |
| LONG_CARD | NTCP | 无独立客户端；无原生 Provider | 226 | 198 | `情怀1.0前端/Client_subgame/Client_ntcp`；`情怀后端原代码/Server_game_split/NTCP/src` | **客户端与服务端可迁移** |
| LONG_CARD | ZGCP | 无独立客户端；无原生 Provider | 98 | 214 | `情怀1.0前端/Client_subgame/Client_zgcp`；`情怀后端原代码/Server_game_split/ZGCP/src` | **客户端与服务端可迁移** |
| LONG_CARD | ZGDSS | 无独立客户端；无原生 Provider | 99 | 220 | `情怀1.0前端/Client_subgame/Client_zgdss`；`情怀后端原代码/Server_game_split/ZGDSS/src` | **客户端与服务端可迁移** |

## 迁移边界建议

16 个可迁移项应按玩法独占分片，每个分片同时拥有自己的 Client bundle、原生 Provider、协议映射和玩法测试；共享 Gateway 只消费统一 envelope，不在多个玩法分片中并改。旧客户端源码用于提取真实操作语义、场景、控制器和资源绑定，旧服务端源码用于提取发牌、动作、轮转、结算规则；旧 MQ consumer、旧消息号和旧持久化不得作为生产旁路保留。

AHPHZ、LCZP 在获得匹配的真实牌桌客户端源码前保持客户端阻塞。可以先迁移并验证服务端原生 Provider，但不得以大厅脚本、资源目录、类别核心、AYDSS/其他同类客户端复制品或 `CatalogGameProvider` 宣称客户端闭环。

## 核查方式

- `rg --files` 分别枚举 Aoo 与 Test；按精确玩法目录段核对源码、场景和资源。
- Aoo 以 `class <CODE>GameProvider` / `<CODE>GameProvider` 搜索 18 项原生 Provider，结果均为 0。
- Test 客户端只把独立 `Client_subgame/Client_<code>` 中 `.js/.ts` 计为牌桌源码；大厅 `uiGame` 建房/战绩脚本、图片、prefab 和构建产物不计。
- Test 服务端只把 `Server_game_split/<CODE>/src/**/*.java`（GLZP 同时存在更新源码树）计为可审阅权威来源；jar/class 不单独作为源码证据。

## CP 权威源码阻塞与类别纠正（2026-08-24）

再次按继承链而不是名称后缀核验后，CP 是 `business/global/pk/cp` 下的长汀 510K 扑克：`CPRoom extends PKRoom`、`CPRoomSet extends AbsPKSetRoom`、`CPSetOp extends AbsPKSetOp`。目录原来的 `LONG_CARD/long-card-regional` 错误，现纠正为 `POKER/poker-510k`，不得映射为跑得快。

六份可定位的 CP 后端副本中，决定可否形成完整权威结算的两个文件内容完全一致：`CPSetCard.java` SHA-1 为 `4966fadf28fd160a219ef202aae64dc2d286cc1c`，`CPCalcPosEnd.java` SHA-1 为 `317f996de5d4396d24654f923e3a1ffadcb622dc`。所有副本的 `CPCalcPosEnd.calcPoint()` 均为空；`CPSetCard.pop()`、`appointPopCard()` 和 `popList(int,int)` 均直接返回 `null`。因此源码能证明专属牌组以及单张、对子、三张、四张、顺牌、十八长操作，但不能证明权威摸牌路径和任何计分公式。当前不注册 CP Provider；在获得计分与缺失发牌实现前，补写数值会变成臆造规则。

同轮核对确认 CQCP 是 PKRoom 的叫档/叫主/埋牌/甩牌扑克。ZGCP、ZGDSS 虽复用 MahjongRoom/AbsMJSetRoom 框架，但客户端权威显示名分别为“自贡长牌”“自贡斗十四”，牌义也包含红黑、坨、偷、爆叫、三张、巴牌等长牌语义；因此两项保留 LONG_CARD，改为精确 `long-card-zigong`、`long-card-zigong-da-si-shi`，且从错误的通用 `long-card-regional` profile 移除。三项专属规则未完成前均不注册 Provider。

旧客户端协议号与新目录 ID 不是同一命名空间，而是对本分片稳定存在 `+1` 转换：CP `391→392`、CQCP `393→394`、ZGCP `209→210`、ZGDSS `210→211`、NTCP `518→519`。因此 ZGCP/ZGDSS 在旧客户端中的 209/210 不构成当前目录 210/211 冲突，也不得直接拿旧号覆盖新目录主键；Provider 注册必须使用当前目录 ID，并由 adapter 显式完成旧协议号转换。

### CQCP 权威源码阻塞

CQCP 的核心流程不是普通 trick-taking：权威枚举包含发牌、叫档、叫主、埋牌、打牌，以及甩牌失败约束和 5/10/K 共 100 分计分。六份后端副本的 `CQCPSetCard.java` 均为同一 SHA-1 `515564c2b4dc4294b4145df5c49d81fc668efe2a`，其中定向发牌 `appointPopCard(int)` 未实现并直接返回 `null`；更关键的全甩牌实现 `CQCPAllShuaiCardTypeImpl.java` 六份均为同一 SHA-1 `cb131aa968349f8d3e115797026b596bde4c8a7d`，权威超时/自动操作所需的 `findBiggerPointCards(...)` 与 `firstAutoOut(...)` 均无实现并直接返回 `null`。这使甩牌操作和断线/超时轮转无法从任何可定位源码等价迁移。当前保留精确类别，但不注册 CQCP Provider；自行补算法会臆造“优先分牌”和甩牌失败行为。

### ZGCP / ZGDSS 权威源码阻塞

ZGCP 客户端存在 `CZGCP_PiaoHua` 且结算明确包含 `ChiPiao` 倍率，但包括最新 `ServerAll/Server_game/ZGCP` 在内的所有可定位服务端版本中，唯一房间入口 `ZGCPRoom.opPiao(WebSocketRequest,long,int)` 均为空方法；新版同时保留空的 `sendSetPosCard()`，机器人操作仍标注 `TODO 机器AI处理`。因此无法确定飘花值的合法范围、写入时机、广播/重连字段以及对吃飘结算的可信输入，不能通过自行补 setter 冒充权威链，ZGCP 暂不注册。

ZGDSS 的最新 `ServerAll` 仍在 `ZGDSSSetRound.autoOutCard(int)` 的爆叫自动轮转中留下两个关键空分支：庄家无爆听牌时不执行任何动作；候选含 `Hu/JiePao/Gang` 时同样不执行任何动作。旧主线文件 SHA-1 `a4adcc8c6fa90a19ef0442ab501dd7e2ee01f6b3` 与新版虽结构不同，但这两个分支在两版都为空。九阶段要求覆盖轮转与断线/托管后的权威推进，无法从源码判断应胡、杠、过或结束；ZGDSS 暂不注册。

## NTCP 独占迁移阻塞（2026-08-24）

目录把 `519/ntcp` 标为 `LONG_CARD/long-card-regional`，但 Test 权威源码证明 NTCP 实际是麻将，不是长牌：

- 客户端独立工程含 `ntcp_MJConfig.js`、`ntcp_UIMJZhaMa.js`、换张、定缺、暗杠 UI，以及麻将牌桌/补花模型。
- 服务端位于 `business/global/mj/ntcp`，`NTCPSetOp` 继承 `AbsMJSetOp`，动作包含 `AnGang`、`JieGang`、`Gang`、`Peng`、`Chi`、`Ting`、`BaoTing`、`QiangGangHu`、`Hu`；规则含 `daimenpiao`、`xier`、`sanlaojuhui`、`ererhuqihu`、`loupeng`、`louhu`、`angangkejian`。
- `NTCPRoomSet` 使用麻将摸牌/杠摸牌牌墙，客户端也存在扎码、金牌、换张、定缺等专属流程。现有 `LongCardCoreEngine` 不具备麻将牌墙、杠、听、定缺、换张、扎码及上述胡型状态，使用它注册 NTCP 会丢失权威规则，属于以类别空壳冒充。

进一步对照权威牌墙与胡牌实现后，最精确的现有大类是 **`MAHJONG / mahjong-lai-zi`**：`NTCPKaiJinImpl` 从牌墙翻出 `fanjiang + 1` 张“金牌”，金牌属于赖子机制；牌墙由万、条、筒、箭牌组成，`xier` 可选花牌并自动补花。它不是血战/血流（无胡后继续）、推倒胡，也不是无赖子的标准麻将。

但 `mahjong-lai-zi` 只能作为 family 归类，不能直接冒充 NTCP 实现。当前通用 LaiZi family 尚不支持 NTCP 的双金开金递归避重、可选喜儿花牌及补花、22 分起胡、丫子/单吊/文钱、三老聚会、闷飘/飘胡、天听/海底捞月、过碰/过胡、暗杠可见、飘分以及对应结算倍率。必须新增 NTCP 独占 rule profile/session 与上述规则测试后才可注册。

进一步全量核对新旧服务端后，飘分链本身缺失，不能从现有源码迁移：权威客户端 `NTCPRoomMgr.SendPiaoFen` 实际发送 `CNTCPBiaoShi {roomID, choose}`，但所有可定位 NTCP 服务端源码中均不存在 `BiaoShi`/`choose` 请求类或处理器；只留下未被请求入口驱动的 `SNTCP_PiaoFen` 响应 DTO。`NTCPRoomSet.update` 的 `SetState.WaitingEx` 分支完全为空，而 `NTCPSetPos.calcResults()` 声明 `piaoFenCount = 0` 后从未读取或写入该值。客户端确实展示/恢复 `piaoFenList`，因此这不是未启用的死功能。缺失选择入口、状态推进和总结算三个环节后，无法确定飘分如何参与专属结算；在取得缺失服务端实现前不得注册 NTCP Provider。

因此本分片没有新增 `NtcpGameProvider`、协议或客户端注册死代码，也没有只改 catalog 留下不可运行映射。后续应原子完成：把 519 改为 `MAHJONG/mahjong-lai-zi`，同步生成/数据库映射，同时落 NTCP 独占 Mahjong Provider、真实客户端和九阶段测试。`CatalogGameProvider` 不能作为过渡实现。
