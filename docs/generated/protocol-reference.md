# Aoo Protocol V2 接口参考

> 由 `protocol/aoo-protocol-v2.json` 自动生成，禁止手工编辑。协议版本：2.0。

## `account.login`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | HTTPS | req | M1 | anonymous | 是 | required | 1001, 1002, 2001, 2002 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| loginType | string | 是 |
| credential | string | 是 |
| deviceId | string | 是 |
| clientVersion | string | 否 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| accessToken | string | 是 |
| refreshToken | string | 是 |
| userId | string | 是 |
| wsTicket | string | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "account.login",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "loginType": "string",
    "credential": "string",
    "deviceId": "string",
    "clientVersion": "string"
  }
}
```

## `account.login_compat`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | HTTPS | req | M1 | anonymous | 是 | required | 1001, 1002, 2001, 2002 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |
| deviceId | string | 是 |
| clientVersion | string | 否 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |
| wsTicket | string | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "account.login_compat",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {},
    "deviceId": "string",
    "clientVersion": "string"
  }
}
```

## `account.session_dispatch`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M1 | ws_ticket | 是 | required | 1001, 1003, 2001, 2003 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "account.session_dispatch",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {}
  }
}
```

## `account.session_push`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | server_to_client | WSS | push | M1 | session | 否 | none |  |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| - | - | - |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "account.session_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `account.token_refresh`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | HTTPS | req | M8 | refresh_token | 是 | required | 1001, 2003, 2004 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| sessionId | string | 是 |
| deviceId | string | 是 |
| refreshToken | string | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| accessToken | string | 是 |
| refreshToken | string | 是 |
| expiresInSeconds | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "account.token_refresh",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "sessionId": "string",
    "deviceId": "string",
    "refreshToken": "string"
  }
}
```

## `account.ws_ticket`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | HTTPS | req | M1 | access_token | 是 | required | 1001, 1003, 2003 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| deviceId | string | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| wsTicket | string | 是 |
| expiresInSeconds | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "account.ws_ticket",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "deviceId": "string"
  }
}
```

## `club.dispatch`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M3 | session | 是 | required | 1001, 1003, 6001, 6002 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |
| clubId | string | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |
| stateVersion | integer | 否 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "club.dispatch",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {},
    "clubId": "string"
  }
}
```

## `club.member_page`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | HTTPS | req | M8 | access_token | 否 | optional | 1001, 6001, 6002 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| clubId | string | 是 |
| cursor | string | 否 |
| limit | integer | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| items | array | 是 |
| nextCursor | string | 否 |
| hasMore | boolean | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "club.member_page",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "clubId": "string",
    "cursor": "string",
    "limit": 0
  }
}
```

## `club.state_push`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | server_to_client | WSS | push | M3 | session | 否 | none |  |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| - | - | - |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "club.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `common.room.compat_state_push`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | server_to_client | WSS | push | M4 | room_session | 否 | none |  |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| - | - | - |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "common.room.compat_state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `common.room.dispatch`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M4 | room_session | 是 | required | 1001, 1003, 3001, 3002, 3003 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |
| lastServerSeq | integer | 否 |
| reconnectToken | string | 否 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |
| stateVersion | integer | 是 |
| serverSeq | integer | 否 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "common.room.dispatch",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {},
    "lastServerSeq": 0,
    "reconnectToken": "string"
  }
}
```

## `common.room.state_push`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | server_to_client | WSS | push | M4 | room_session | 否 | none |  |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| - | - | - |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |
| stateVersion | integer | 是 |
| serverSeq | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "common.room.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `game.action`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M8 | room_session | 是 | required | 3001, 4001, 4002, 4003 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |
| actionId | string | 是 |
| playVersion | string | 是 |
| expectedStateVersion | integer | 是 |
| intent | object | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| accepted | boolean | 是 |
| stateVersion | integer | 是 |
| serverSeq | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "game.action",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {},
    "actionId": "string",
    "playVersion": "string",
    "expectedStateVersion": 0,
    "intent": {}
  }
}
```

## `game.dispatch`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M7 | room_session | 是 | required | 1001, 3001, 4001, 4002 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |
| gameId | string | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |
| stateVersion | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "game.dispatch",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {},
    "gameId": "string"
  }
}
```

## `game.final_settlement`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | server_to_client | WSS | push | M8 | room_session | 否 | none |  |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| - | - | - |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| settlementBusinessId | string | 是 |
| resultProfileId | string | 是 |
| playerEntries | array | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "game.final_settlement",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `game.round_settlement`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | server_to_client | WSS | push | M8 | room_session | 否 | none |  |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| - | - | - |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| settlementBusinessId | string | 是 |
| roundNo | integer | 是 |
| resultProfileId | string | 是 |
| playerEntries | array | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "game.round_settlement",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `game.state_push`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | server_to_client | WSS | push | M7 | room_session | 否 | none |  |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| - | - | - |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "game.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `hall.catalog`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | HTTPS | req | M8 | access_token | 否 | optional | 1001, 1004 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| regionCode | string | 否 |
| clientVersion | string | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| games | array | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "hall.catalog",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "regionCode": "string",
    "clientVersion": "string"
  }
}
```

## `hall.dispatch`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M2 | session | 否 | optional | 1001, 1003, 2003 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |
| stateVersion | integer | 否 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "hall.dispatch",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {}
  }
}
```

## `hall.state_push`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | server_to_client | WSS | push | M2 | session | 否 | none |  |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| - | - | - |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "hall.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `longcard.aycp.dispatch`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M7 | room_session | 是 | required | 1001, 3001, 4001, 4002 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| command | string | 是 |
| action | string | 是 |
| payload | object | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |
| stateVersion | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "longcard.aycp.dispatch",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "command": "string",
    "action": "string",
    "payload": {}
  }
}
```

## `longcard.aydss.dispatch`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M7 | room_session | 是 | required | 1001, 3001, 4001, 4002 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| command | string | 是 |
| action | string | 是 |
| payload | object | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |
| stateVersion | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "longcard.aydss.dispatch",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "command": "string",
    "action": "string",
    "payload": {}
  }
}
```

## `mahjong.xuezhan.dispatch`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M6 | room_session | 是 | required | 1001, 3001, 4001, 4002 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |
| stateVersion | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "mahjong.xuezhan.dispatch",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {}
  }
}
```

## `mahjong.xuezhan.state_push`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | server_to_client | WSS | push | M6 | room_session | 否 | none |  |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| - | - | - |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "mahjong.xuezhan.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.CD201.dispatch`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M5 | room_session | 是 | required | 1001, 3001, 4001, 4002 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |
| stateVersion | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "poker.CD201.dispatch",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {}
  }
}
```

## `poker.CD201.state_push`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | server_to_client | WSS | push | M5 | room_session | 否 | none |  |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| - | - | - |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |
| stateVersion | integer | 是 |
| serverSeq | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "poker.CD201.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.LS201.dispatch`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M5 | room_session | 是 | required | 1001, 3001, 4001, 4002 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |
| stateVersion | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "poker.LS201.dispatch",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {}
  }
}
```

## `poker.LS201.state_push`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | server_to_client | WSS | push | M5 | room_session | 否 | none |  |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| - | - | - |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |
| stateVersion | integer | 是 |
| serverSeq | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "poker.LS201.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.NJ201.dispatch`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M5 | room_session | 是 | required | 1001, 3001, 4001, 4002 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |
| stateVersion | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "poker.NJ201.dispatch",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {}
  }
}
```

## `poker.NJ201.state_push`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | server_to_client | WSS | push | M5 | room_session | 否 | none |  |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| - | - | - |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |
| stateVersion | integer | 是 |
| serverSeq | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "poker.NJ201.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.pdk.dispatch`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M5 | room_session | 是 | required | 1001, 3001, 4001, 4002 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| payload | object | 是 |
| stateVersion | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "poker.pdk.dispatch",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {}
  }
}
```

## `poker.pdk.state_push`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | server_to_client | WSS | push | M5 | room_session | 否 | none |  |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| - | - | - |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "poker.pdk.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `replay.perspective_page`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M8 | session | 否 | optional | 1001, 1003, 3001, 3003 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |
| roomId | string | 是 |
| setId | integer | 是 |
| afterSequence | integer | 否 |
| limit | integer | 否 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| items | array | 是 |
| nextSequence | integer | 是 |
| hasMore | boolean | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "replay.perspective_page",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {},
    "roomId": "string",
    "setId": 0,
    "afterSequence": 0,
    "limit": 0
  }
}
```

## `replay.query`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | HTTPS | req | M8 | access_token | 否 | optional | 1003, 3001, 6002 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| roomId | string | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| viewerReplay | object | 是 |
| viewType | string | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "replay.query",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "roomId": "string"
  }
}
```

## `room.create`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | HTTPS | req | M8 | access_token | 是 | required | 1001, 3002, 5001, 6002 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| gameId | string | 是 |
| roomProfileVersion | string | 是 |
| ruleSnapshotId | string | 是 |
| paymentMode | string | 是 |
| clubId | string | 否 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| roomId | string | 是 |
| roomSessionToken | string | 是 |
| ruleSnapshotId | string | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "room.create",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "gameId": "string",
    "roomProfileVersion": "string",
    "ruleSnapshotId": "string",
    "paymentMode": "string",
    "clubId": "string"
  }
}
```

## `room.dissolve_apply`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M8 | room_session | 是 | required | 3001, 3003, 3004 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |
| reasonCode | string | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| voteId | string | 是 |
| expiresAt | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "room.dissolve_apply",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {},
    "reasonCode": "string"
  }
}
```

## `room.dissolve_vote`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M8 | room_session | 是 | required | 3001, 3004 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |
| voteId | string | 是 |
| agree | boolean | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| accepted | boolean | 是 |
| stateVersion | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "room.dissolve_vote",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {},
    "voteId": "string",
    "agree": false
  }
}
```

## `room.join`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | HTTPS | req | M8 | access_token | 是 | required | 3001, 3002, 3003, 6002 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| roomId | string | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| roomSessionToken | string | 是 |
| playVersion | string | 是 |
| ruleSnapshotId | string | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "room.join",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "roomId": "string"
  }
}
```

## `room.leave`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M8 | room_session | 是 | required | 3001, 3003 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| left | boolean | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "room.leave",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {}
  }
}
```

## `room.reconnect`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M8 | room_session | 否 | optional | 3001, 3005 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |
| roomId | string | 是 |
| lastServerSeq | integer | 是 |
| reconnectToken | string | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| viewerSnapshot | object | 是 |
| events | array | 是 |
| serverSeq | integer | 是 |
| hasMore | boolean | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "room.reconnect",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {},
    "roomId": "string",
    "lastServerSeq": 0,
    "reconnectToken": "string"
  }
}
```

## `room.seat`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M8 | room_session | 是 | required | 3001, 3002, 3003 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |
| seatId | integer | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| stateVersion | integer | 是 |
| serverSeq | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "room.seat",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {},
    "seatId": 0
  }
}
```

## `system.heartbeat`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | bidirectional | WSS | system | M7 | session | 否 | none |  |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| clientTime | integer | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| serverTime | integer | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "system.heartbeat",
  "kind": "system",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "clientTime": 0
  }
}
```

## `system.kick_out`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | server_to_client | WSS | system | M7 | session | 否 | none |  |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| - | - | - |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| reasonCode | string | 是 |
| message | string | 否 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "system.kick_out",
  "kind": "system",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

