# V2 序列化与字节预算

本文件与 `aoo-protocol-v2.json`、Gateway 的 `ProtocolValuePolicy`/`GatewayFrameCodec` 共同构成强制边界。

| 语义 | JSON 线格式 | 规则 |
|---|---|---|
| userId/roomId/clubId/recordId/transactionId | 十进制 string | 正整数、无前导零；不得转 JS number |
| 金额、积分、小数 | 十进制定点 string | 金额优先最小货币单位；禁止 float/double 上线 |
| 时间点 | integer | UTC Unix epoch milliseconds，字段以 `At`/`Time` 结尾 |
| 持续时间/倒计时 | integer | milliseconds，字段以 `DurationMillis`/`RemainingMillis` 结尾 |
| boolean | JSON boolean | 禁止 0/1、`"true"` |
| enum | string | 稳定名称；接收端未知值进入 UNKNOWN/Optional，禁止 ordinal |
| optional | 缺失 | required 字段不得缺失；optional 缺失使用契约默认值；禁止以 null 代替缺失 |
| collection | JSON array | 线顺序即语义顺序；手牌按服务端牌序、座位按 seatId、事件按 sequence |

默认值演进只能新增 optional 字段，客户端缺省时使用协议文档声明的默认值；删除/重命名/改类型必须提升主版本。旧快照读取器必须忽略未知字段。

单字符串最多 16 KiB、集合最多 4096 项、线帧最多 256 KiB、解压后最多 1 MiB、嵌套最多 32 层。快照、战绩、回放、亲友圈列表分别使用 `afterSequence`/`nextCursor` 分页，不得依赖深分页；超过预算必须分片，禁止静默截断。压缩仅在双方协商 gzip 后启用，解压严格执行 1 MiB 上限以抵御压缩炸弹。

所有源文件和 JSON 使用 UTF-8（无 BOM）；畸形 UTF-8、非法 JSON、非字符串 map key、NaN/Infinity 及超限输入直接返回稳定错误码，不进行宽松猜测。历史异常文本仅在离线迁移中显式指定原编码并记录摘要，网关不得自动转码。

黄金样本由 `golden-v2.json` 保存 UTF-8 规范字节，Java 编解码测试与 `check_protocol.py` 必须同时通过；修改黄金样本必须与机器源变更同审。
