# Aoo 跨语言协议类型映射

| 机器类型 | Java 边界 | TypeScript 边界 | JSON 线格式 |
|---|---|---|---|
| string | String | string | string |
| integer/number | long/double（非 ID） | number | number |
| boolean | boolean | boolean | boolean |
| object/map | Map<String,Object> | Readonly<Record<string,unknown>> | object |
| array | List<?> | ReadonlyArray<unknown> | array |
| bytes | byte[] | Uint8Array | Base64 string |
| enum | Java enum | string literal union | string |
| optional | Optional/nullable adapter | `?` 字段 | 缺失字段，禁止用 null 代替 |

业务 ID、序列号溢出风险字段和货币最小单位跨端统一用十进制 `string`，禁止暴露 Java `long` 给 JavaScript。`bytes` 只允许 Base64；动态 map 的键必须为字符串。机器源的 `required` 是唯一可选性依据。
