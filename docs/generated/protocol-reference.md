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

## `mahjong.afmj.dispatch`

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
  "msgId": "mahjong.afmj.dispatch",
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

## `mahjong.afmj.state_push`

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
  "msgId": "mahjong.afmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ahhbmj.dispatch`

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
  "msgId": "mahjong.ahhbmj.dispatch",
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

## `mahjong.ahhbmj.state_push`

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
  "msgId": "mahjong.ahhbmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ahhnmj.dispatch`

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
  "msgId": "mahjong.ahhnmj.dispatch",
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

## `mahjong.ahhnmj.state_push`

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
  "msgId": "mahjong.ahhnmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ahmj.dispatch`

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
  "msgId": "mahjong.ahmj.dispatch",
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

## `mahjong.ahmj.state_push`

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
  "msgId": "mahjong.ahmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ak159mj.dispatch`

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
  "msgId": "mahjong.ak159mj.dispatch",
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

## `mahjong.ak159mj.state_push`

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
  "msgId": "mahjong.ak159mj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.aqmj.dispatch`

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
  "msgId": "mahjong.aqmj.dispatch",
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

## `mahjong.aqmj.state_push`

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
  "msgId": "mahjong.aqmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.asmj.dispatch`

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
  "msgId": "mahjong.asmj.dispatch",
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

## `mahjong.asmj.state_push`

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
  "msgId": "mahjong.asmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.aymj.dispatch`

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
  "msgId": "mahjong.aymj.dispatch",
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

## `mahjong.aymj.state_push`

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
  "msgId": "mahjong.aymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.bamj.dispatch`

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
  "msgId": "mahjong.bamj.dispatch",
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

## `mahjong.bamj.state_push`

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
  "msgId": "mahjong.bamj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.bdjhmj.dispatch`

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
  "msgId": "mahjong.bdjhmj.dispatch",
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

## `mahjong.bdjhmj.state_push`

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
  "msgId": "mahjong.bdjhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.bdmj.dispatch`

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
  "msgId": "mahjong.bdmj.dispatch",
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

## `mahjong.bdmj.state_push`

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
  "msgId": "mahjong.bdmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.bdyxmj.dispatch`

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
  "msgId": "mahjong.bdyxmj.dispatch",
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

## `mahjong.bdyxmj.state_push`

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
  "msgId": "mahjong.bdyxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.bfmj.dispatch`

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
  "msgId": "mahjong.bfmj.dispatch",
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

## `mahjong.bfmj.state_push`

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
  "msgId": "mahjong.bfmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.bsmj.dispatch`

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
  "msgId": "mahjong.bsmj.dispatch",
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

## `mahjong.bsmj.state_push`

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
  "msgId": "mahjong.bsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.bzmj.dispatch`

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
  "msgId": "mahjong.bzmj.dispatch",
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

## `mahjong.bzmj.state_push`

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
  "msgId": "mahjong.bzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.bzqzmj.dispatch`

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
  "msgId": "mahjong.bzqzmj.dispatch",
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

## `mahjong.bzqzmj.state_push`

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
  "msgId": "mahjong.bzqzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.bztdh.dispatch`

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
  "msgId": "mahjong.bztdh.dispatch",
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

## `mahjong.bztdh.state_push`

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
  "msgId": "mahjong.bztdh.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.caooszmj.dispatch`

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
  "msgId": "mahjong.caooszmj.dispatch",
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

## `mahjong.caooszmj.state_push`

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
  "msgId": "mahjong.caooszmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ccmj.dispatch`

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
  "msgId": "mahjong.ccmj.dispatch",
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

## `mahjong.ccmj.state_push`

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
  "msgId": "mahjong.ccmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.chengdu1001.dispatch`

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
  "msgId": "mahjong.chengdu1001.dispatch",
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

## `mahjong.chengdu1001.state_push`

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
  "msgId": "mahjong.chengdu1001.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.chmj.dispatch`

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
  "msgId": "mahjong.chmj.dispatch",
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

## `mahjong.chmj.state_push`

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
  "msgId": "mahjong.chmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.cnmj.dispatch`

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
  "msgId": "mahjong.cnmj.dispatch",
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

## `mahjong.cnmj.state_push`

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
  "msgId": "mahjong.cnmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.csmj.dispatch`

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
  "msgId": "mahjong.csmj.dispatch",
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

## `mahjong.csmj.state_push`

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
  "msgId": "mahjong.csmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.cxmj.dispatch`

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
  "msgId": "mahjong.cxmj.dispatch",
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

## `mahjong.cxmj.state_push`

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
  "msgId": "mahjong.cxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.cxyxmj.dispatch`

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
  "msgId": "mahjong.cxyxmj.dispatch",
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

## `mahjong.cxyxmj.state_push`

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
  "msgId": "mahjong.cxyxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.cylymj.dispatch`

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
  "msgId": "mahjong.cylymj.dispatch",
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

## `mahjong.cylymj.state_push`

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
  "msgId": "mahjong.cylymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.czczmj.dispatch`

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
  "msgId": "mahjong.czczmj.dispatch",
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

## `mahjong.czczmj.state_push`

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
  "msgId": "mahjong.czczmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.czdzmj.dispatch`

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
  "msgId": "mahjong.czdzmj.dispatch",
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

## `mahjong.czdzmj.state_push`

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
  "msgId": "mahjong.czdzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.czmj.dispatch`

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
  "msgId": "mahjong.czmj.dispatch",
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

## `mahjong.czmj.state_push`

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
  "msgId": "mahjong.czmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.dcwdmj.dispatch`

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
  "msgId": "mahjong.dcwdmj.dispatch",
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

## `mahjong.dcwdmj.state_push`

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
  "msgId": "mahjong.dcwdmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.dczbmj.dispatch`

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
  "msgId": "mahjong.dczbmj.dispatch",
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

## `mahjong.dczbmj.state_push`

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
  "msgId": "mahjong.dczbmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.demomj.dispatch`

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
  "msgId": "mahjong.demomj.dispatch",
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

## `mahjong.demomj.state_push`

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
  "msgId": "mahjong.demomj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.dkgmj.dispatch`

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
  "msgId": "mahjong.dkgmj.dispatch",
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

## `mahjong.dkgmj.state_push`

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
  "msgId": "mahjong.dkgmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.dlaoomj.dispatch`

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
  "msgId": "mahjong.dlaoomj.dispatch",
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

## `mahjong.dlaoomj.state_push`

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
  "msgId": "mahjong.dlaoomj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.dnmj.dispatch`

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
  "msgId": "mahjong.dnmj.dispatch",
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

## `mahjong.dnmj.state_push`

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
  "msgId": "mahjong.dnmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.dphmj.dispatch`

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
  "msgId": "mahjong.dphmj.dispatch",
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

## `mahjong.dphmj.state_push`

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
  "msgId": "mahjong.dphmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.dsmj.dispatch`

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
  "msgId": "mahjong.dsmj.dispatch",
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

## `mahjong.dsmj.state_push`

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
  "msgId": "mahjong.dsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.dtlgfmj.dispatch`

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
  "msgId": "mahjong.dtlgfmj.dispatch",
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

## `mahjong.dtlgfmj.state_push`

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
  "msgId": "mahjong.dtlgfmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.dtmj.dispatch`

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
  "msgId": "mahjong.dtmj.dispatch",
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

## `mahjong.dtmj.state_push`

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
  "msgId": "mahjong.dtmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.dxbjmj.dispatch`

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
  "msgId": "mahjong.dxbjmj.dispatch",
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

## `mahjong.dxbjmj.state_push`

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
  "msgId": "mahjong.dxbjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.dymj.dispatch`

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
  "msgId": "mahjong.dymj.dispatch",
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

## `mahjong.dymj.state_push`

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
  "msgId": "mahjong.dymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.dzmj.dispatch`

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
  "msgId": "mahjong.dzmj.dispatch",
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

## `mahjong.dzmj.state_push`

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
  "msgId": "mahjong.dzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.dzsjzmj.dispatch`

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
  "msgId": "mahjong.dzsjzmj.dispatch",
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

## `mahjong.dzsjzmj.state_push`

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
  "msgId": "mahjong.dzsjzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.fcmj.dispatch`

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
  "msgId": "mahjong.fcmj.dispatch",
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

## `mahjong.fcmj.state_push`

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
  "msgId": "mahjong.fcmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.fctdhmj.dispatch`

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
  "msgId": "mahjong.fctdhmj.dispatch",
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

## `mahjong.fctdhmj.state_push`

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
  "msgId": "mahjong.fctdhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.fdmj.dispatch`

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
  "msgId": "mahjong.fdmj.dispatch",
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

## `mahjong.fdmj.state_push`

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
  "msgId": "mahjong.fdmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.fjyxmj.dispatch`

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
  "msgId": "mahjong.fjyxmj.dispatch",
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

## `mahjong.fjyxmj.state_push`

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
  "msgId": "mahjong.fjyxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.fxmj.dispatch`

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
  "msgId": "mahjong.fxmj.dispatch",
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

## `mahjong.fxmj.state_push`

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
  "msgId": "mahjong.fxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.fyddzmj.dispatch`

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
  "msgId": "mahjong.fyddzmj.dispatch",
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

## `mahjong.fyddzmj.state_push`

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
  "msgId": "mahjong.fyddzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.fymj.dispatch`

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
  "msgId": "mahjong.fymj.dispatch",
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

## `mahjong.fymj.state_push`

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
  "msgId": "mahjong.fymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.fzgcmj.dispatch`

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
  "msgId": "mahjong.fzgcmj.dispatch",
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

## `mahjong.fzgcmj.state_push`

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
  "msgId": "mahjong.fzgcmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.fzjxmj.dispatch`

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
  "msgId": "mahjong.fzjxmj.dispatch",
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

## `mahjong.fzjxmj.state_push`

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
  "msgId": "mahjong.fzjxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.fzmj.dispatch`

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
  "msgId": "mahjong.fzmj.dispatch",
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

## `mahjong.fzmj.state_push`

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
  "msgId": "mahjong.fzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.gamj.dispatch`

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
  "msgId": "mahjong.gamj.dispatch",
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

## `mahjong.gamj.state_push`

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
  "msgId": "mahjong.gamj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.gcbgmj.dispatch`

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
  "msgId": "mahjong.gcbgmj.dispatch",
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

## `mahjong.gcbgmj.state_push`

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
  "msgId": "mahjong.gcbgmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.gcmj.dispatch`

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
  "msgId": "mahjong.gcmj.dispatch",
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

## `mahjong.gcmj.state_push`

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
  "msgId": "mahjong.gcmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.gdczmj.dispatch`

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
  "msgId": "mahjong.gdczmj.dispatch",
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

## `mahjong.gdczmj.state_push`

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
  "msgId": "mahjong.gdczmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.gdjymj.dispatch`

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
  "msgId": "mahjong.gdjymj.dispatch",
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

## `mahjong.gdjymj.state_push`

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
  "msgId": "mahjong.gdjymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.gdmj.dispatch`

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
  "msgId": "mahjong.gdmj.dispatch",
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

## `mahjong.gdmj.state_push`

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
  "msgId": "mahjong.gdmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.gft258mj.dispatch`

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
  "msgId": "mahjong.gft258mj.dispatch",
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

## `mahjong.gft258mj.state_push`

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
  "msgId": "mahjong.gft258mj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.gjmj.dispatch`

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
  "msgId": "mahjong.gjmj.dispatch",
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

## `mahjong.gjmj.state_push`

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
  "msgId": "mahjong.gjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.glqzmj.dispatch`

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
  "msgId": "mahjong.glqzmj.dispatch",
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

## `mahjong.glqzmj.state_push`

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
  "msgId": "mahjong.glqzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.gnmj.dispatch`

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
  "msgId": "mahjong.gnmj.dispatch",
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

## `mahjong.gnmj.state_push`

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
  "msgId": "mahjong.gnmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.gsjmj.dispatch`

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
  "msgId": "mahjong.gsjmj.dispatch",
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

## `mahjong.gsjmj.state_push`

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
  "msgId": "mahjong.gsjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.gslzmj.dispatch`

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
  "msgId": "mahjong.gslzmj.dispatch",
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

## `mahjong.gslzmj.state_push`

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
  "msgId": "mahjong.gslzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.gsmj.dispatch`

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
  "msgId": "mahjong.gsmj.dispatch",
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

## `mahjong.gsmj.state_push`

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
  "msgId": "mahjong.gsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.gxmj.dispatch`

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
  "msgId": "mahjong.gxmj.dispatch",
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

## `mahjong.gxmj.state_push`

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
  "msgId": "mahjong.gxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.gymj.dispatch`

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
  "msgId": "mahjong.gymj.dispatch",
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

## `mahjong.gymj.state_push`

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
  "msgId": "mahjong.gymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.gyzjmj.dispatch`

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
  "msgId": "mahjong.gyzjmj.dispatch",
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

## `mahjong.gyzjmj.state_push`

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
  "msgId": "mahjong.gyzjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.gzmj.dispatch`

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
  "msgId": "mahjong.gzmj.dispatch",
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

## `mahjong.gzmj.state_push`

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
  "msgId": "mahjong.gzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hamj.dispatch`

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
  "msgId": "mahjong.hamj.dispatch",
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

## `mahjong.hamj.state_push`

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
  "msgId": "mahjong.hamj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.haxymj.dispatch`

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
  "msgId": "mahjong.haxymj.dispatch",
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

## `mahjong.haxymj.state_push`

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
  "msgId": "mahjong.haxymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hbhbmj.dispatch`

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
  "msgId": "mahjong.hbhbmj.dispatch",
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

## `mahjong.hbhbmj.state_push`

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
  "msgId": "mahjong.hbhbmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hbmj.dispatch`

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
  "msgId": "mahjong.hbmj.dispatch",
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

## `mahjong.hbmj.state_push`

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
  "msgId": "mahjong.hbmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hbtdhmj.dispatch`

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
  "msgId": "mahjong.hbtdhmj.dispatch",
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

## `mahjong.hbtdhmj.state_push`

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
  "msgId": "mahjong.hbtdhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hbwhmj.dispatch`

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
  "msgId": "mahjong.hbwhmj.dispatch",
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

## `mahjong.hbwhmj.state_push`

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
  "msgId": "mahjong.hbwhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hbyxmj.dispatch`

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
  "msgId": "mahjong.hbyxmj.dispatch",
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

## `mahjong.hbyxmj.state_push`

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
  "msgId": "mahjong.hbyxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hebmj.dispatch`

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
  "msgId": "mahjong.hebmj.dispatch",
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

## `mahjong.hebmj.state_push`

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
  "msgId": "mahjong.hebmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hfbzmj.dispatch`

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
  "msgId": "mahjong.hfbzmj.dispatch",
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

## `mahjong.hfbzmj.state_push`

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
  "msgId": "mahjong.hfbzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hfmj.dispatch`

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
  "msgId": "mahjong.hfmj.dispatch",
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

## `mahjong.hfmj.state_push`

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
  "msgId": "mahjong.hfmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hhhgw.dispatch`

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
  "msgId": "mahjong.hhhgw.dispatch",
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

## `mahjong.hhhgw.state_push`

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
  "msgId": "mahjong.hhhgw.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hjmj.dispatch`

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
  "msgId": "mahjong.hjmj.dispatch",
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

## `mahjong.hjmj.state_push`

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
  "msgId": "mahjong.hjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hldmj.dispatch`

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
  "msgId": "mahjong.hldmj.dispatch",
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

## `mahjong.hldmj.state_push`

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
  "msgId": "mahjong.hldmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hnaymj.dispatch`

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
  "msgId": "mahjong.hnaymj.dispatch",
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

## `mahjong.hnaymj.state_push`

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
  "msgId": "mahjong.hnaymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hncsmj.dispatch`

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
  "msgId": "mahjong.hncsmj.dispatch",
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

## `mahjong.hncsmj.state_push`

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
  "msgId": "mahjong.hncsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hnhbmj.dispatch`

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
  "msgId": "mahjong.hnhbmj.dispatch",
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

## `mahjong.hnhbmj.state_push`

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
  "msgId": "mahjong.hnhbmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hnjymj.dispatch`

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
  "msgId": "mahjong.hnjymj.dispatch",
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

## `mahjong.hnjymj.state_push`

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
  "msgId": "mahjong.hnjymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hnjzmj.dispatch`

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
  "msgId": "mahjong.hnjzmj.dispatch",
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

## `mahjong.hnjzmj.state_push`

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
  "msgId": "mahjong.hnjzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hnmj.dispatch`

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
  "msgId": "mahjong.hnmj.dispatch",
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

## `mahjong.hnmj.state_push`

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
  "msgId": "mahjong.hnmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hnpdsmj.dispatch`

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
  "msgId": "mahjong.hnpdsmj.dispatch",
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

## `mahjong.hnpdsmj.state_push`

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
  "msgId": "mahjong.hnpdsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hnsymj.dispatch`

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
  "msgId": "mahjong.hnsymj.dispatch",
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

## `mahjong.hnsymj.state_push`

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
  "msgId": "mahjong.hnsymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hnxcmj.dispatch`

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
  "msgId": "mahjong.hnxcmj.dispatch",
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

## `mahjong.hnxcmj.state_push`

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
  "msgId": "mahjong.hnxcmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hnxymj.dispatch`

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
  "msgId": "mahjong.hnxymj.dispatch",
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

## `mahjong.hnxymj.state_push`

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
  "msgId": "mahjong.hnxymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hnyjmj.dispatch`

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
  "msgId": "mahjong.hnyjmj.dispatch",
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

## `mahjong.hnyjmj.state_push`

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
  "msgId": "mahjong.hnyjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hnzzmj.dispatch`

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
  "msgId": "mahjong.hnzzmj.dispatch",
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

## `mahjong.hnzzmj.state_push`

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
  "msgId": "mahjong.hnzzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hsmj.dispatch`

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
  "msgId": "mahjong.hsmj.dispatch",
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

## `mahjong.hsmj.state_push`

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
  "msgId": "mahjong.hsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hstdhmj.dispatch`

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
  "msgId": "mahjong.hstdhmj.dispatch",
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

## `mahjong.hstdhmj.state_push`

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
  "msgId": "mahjong.hstdhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.htmj.dispatch`

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
  "msgId": "mahjong.htmj.dispatch",
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

## `mahjong.htmj.state_push`

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
  "msgId": "mahjong.htmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hyhsmj.dispatch`

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
  "msgId": "mahjong.hyhsmj.dispatch",
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

## `mahjong.hyhsmj.state_push`

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
  "msgId": "mahjong.hyhsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hylymj.dispatch`

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
  "msgId": "mahjong.hylymj.dispatch",
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

## `mahjong.hylymj.state_push`

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
  "msgId": "mahjong.hylymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hzbdmj.dispatch`

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
  "msgId": "mahjong.hzbdmj.dispatch",
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

## `mahjong.hzbdmj.state_push`

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
  "msgId": "mahjong.hzbdmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hzjdmj.dispatch`

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
  "msgId": "mahjong.hzjdmj.dispatch",
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

## `mahjong.hzjdmj.state_push`

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
  "msgId": "mahjong.hzjdmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hzmj.dispatch`

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
  "msgId": "mahjong.hzmj.dispatch",
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

## `mahjong.hzmj.state_push`

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
  "msgId": "mahjong.hzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.hzwmj.dispatch`

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
  "msgId": "mahjong.hzwmj.dispatch",
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

## `mahjong.hzwmj.state_push`

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
  "msgId": "mahjong.hzwmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jamj.dispatch`

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
  "msgId": "mahjong.jamj.dispatch",
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

## `mahjong.jamj.state_push`

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
  "msgId": "mahjong.jamj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jayxmj.dispatch`

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
  "msgId": "mahjong.jayxmj.dispatch",
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

## `mahjong.jayxmj.state_push`

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
  "msgId": "mahjong.jayxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jcahmj.dispatch`

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
  "msgId": "mahjong.jcahmj.dispatch",
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

## `mahjong.jcahmj.state_push`

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
  "msgId": "mahjong.jcahmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jchhmj.dispatch`

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
  "msgId": "mahjong.jchhmj.dispatch",
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

## `mahjong.jchhmj.state_push`

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
  "msgId": "mahjong.jchhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jcmj.dispatch`

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
  "msgId": "mahjong.jcmj.dispatch",
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

## `mahjong.jcmj.state_push`

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
  "msgId": "mahjong.jcmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jdmj.dispatch`

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
  "msgId": "mahjong.jdmj.dispatch",
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

## `mahjong.jdmj.state_push`

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
  "msgId": "mahjong.jdmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jdzmj.dispatch`

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
  "msgId": "mahjong.jdzmj.dispatch",
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

## `mahjong.jdzmj.state_push`

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
  "msgId": "mahjong.jdzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jjmj.dispatch`

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
  "msgId": "mahjong.jjmj.dispatch",
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

## `mahjong.jjmj.state_push`

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
  "msgId": "mahjong.jjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jlmj.dispatch`

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
  "msgId": "mahjong.jlmj.dispatch",
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

## `mahjong.jlmj.state_push`

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
  "msgId": "mahjong.jlmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jmgghmj.dispatch`

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
  "msgId": "mahjong.jmgghmj.dispatch",
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

## `mahjong.jmgghmj.state_push`

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
  "msgId": "mahjong.jmgghmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jmhhmj.dispatch`

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
  "msgId": "mahjong.jmhhmj.dispatch",
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

## `mahjong.jmhhmj.state_push`

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
  "msgId": "mahjong.jmhhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jmjsmj.dispatch`

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
  "msgId": "mahjong.jmjsmj.dispatch",
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

## `mahjong.jmjsmj.state_push`

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
  "msgId": "mahjong.jmjsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jmskmj.dispatch`

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
  "msgId": "mahjong.jmskmj.dispatch",
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

## `mahjong.jmskmj.state_push`

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
  "msgId": "mahjong.jmskmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jnmj.dispatch`

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
  "msgId": "mahjong.jnmj.dispatch",
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

## `mahjong.jnmj.state_push`

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
  "msgId": "mahjong.jnmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jsczmj.dispatch`

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
  "msgId": "mahjong.jsczmj.dispatch",
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

## `mahjong.jsczmj.state_push`

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
  "msgId": "mahjong.jsczmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jsgymj.dispatch`

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
  "msgId": "mahjong.jsgymj.dispatch",
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

## `mahjong.jsgymj.state_push`

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
  "msgId": "mahjong.jsgymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jshamj.dispatch`

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
  "msgId": "mahjong.jshamj.dispatch",
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

## `mahjong.jshamj.state_push`

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
  "msgId": "mahjong.jshamj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jsmj.dispatch`

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
  "msgId": "mahjong.jsmj.dispatch",
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

## `mahjong.jsmj.state_push`

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
  "msgId": "mahjong.jsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jsnyzmj.dispatch`

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
  "msgId": "mahjong.jsnyzmj.dispatch",
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

## `mahjong.jsnyzmj.state_push`

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
  "msgId": "mahjong.jsnyzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jssnmj.dispatch`

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
  "msgId": "mahjong.jssnmj.dispatch",
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

## `mahjong.jssnmj.state_push`

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
  "msgId": "mahjong.jssnmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jssqmj.dispatch`

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
  "msgId": "mahjong.jssqmj.dispatch",
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

## `mahjong.jssqmj.state_push`

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
  "msgId": "mahjong.jssqmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jstdhmj.dispatch`

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
  "msgId": "mahjong.jstdhmj.dispatch",
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

## `mahjong.jstdhmj.state_push`

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
  "msgId": "mahjong.jstdhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jsxymj.dispatch`

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
  "msgId": "mahjong.jsxymj.dispatch",
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

## `mahjong.jsxymj.state_push`

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
  "msgId": "mahjong.jsxymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jsycmj.dispatch`

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
  "msgId": "mahjong.jsycmj.dispatch",
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

## `mahjong.jsycmj.state_push`

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
  "msgId": "mahjong.jsycmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jsyzmj.dispatch`

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
  "msgId": "mahjong.jsyzmj.dispatch",
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

## `mahjong.jsyzmj.state_push`

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
  "msgId": "mahjong.jsyzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jtmj.dispatch`

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
  "msgId": "mahjong.jtmj.dispatch",
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

## `mahjong.jtmj.state_push`

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
  "msgId": "mahjong.jtmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jxfzmj.dispatch`

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
  "msgId": "mahjong.jxfzmj.dispatch",
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

## `mahjong.jxfzmj.state_push`

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
  "msgId": "mahjong.jxfzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jxmj.dispatch`

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
  "msgId": "mahjong.jxmj.dispatch",
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

## `mahjong.jxmj.state_push`

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
  "msgId": "mahjong.jxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jxndmj.dispatch`

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
  "msgId": "mahjong.jxndmj.dispatch",
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

## `mahjong.jxndmj.state_push`

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
  "msgId": "mahjong.jxndmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jxxymj.dispatch`

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
  "msgId": "mahjong.jxxymj.dispatch",
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

## `mahjong.jxxymj.state_push`

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
  "msgId": "mahjong.jxxymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jyessz.dispatch`

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
  "msgId": "mahjong.jyessz.dispatch",
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

## `mahjong.jyessz.state_push`

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
  "msgId": "mahjong.jyessz.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jymj.dispatch`

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
  "msgId": "mahjong.jymj.dispatch",
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

## `mahjong.jymj.state_push`

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
  "msgId": "mahjong.jymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jzmj.dispatch`

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
  "msgId": "mahjong.jzmj.dispatch",
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

## `mahjong.jzmj.state_push`

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
  "msgId": "mahjong.jzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.jzwzmj.dispatch`

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
  "msgId": "mahjong.jzwzmj.dispatch",
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

## `mahjong.jzwzmj.state_push`

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
  "msgId": "mahjong.jzwzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.kfmj.dispatch`

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
  "msgId": "mahjong.kfmj.dispatch",
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

## `mahjong.kfmj.state_push`

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
  "msgId": "mahjong.kfmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.klmj.dispatch`

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
  "msgId": "mahjong.klmj.dispatch",
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

## `mahjong.klmj.state_push`

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
  "msgId": "mahjong.klmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ksmj.dispatch`

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
  "msgId": "mahjong.ksmj.dispatch",
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

## `mahjong.ksmj.state_push`

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
  "msgId": "mahjong.ksmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lamj.dispatch`

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
  "msgId": "mahjong.lamj.dispatch",
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

## `mahjong.lamj.state_push`

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
  "msgId": "mahjong.lamj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lbhzmj.dispatch`

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
  "msgId": "mahjong.lbhzmj.dispatch",
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

## `mahjong.lbhzmj.state_push`

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
  "msgId": "mahjong.lbhzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lbmj.dispatch`

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
  "msgId": "mahjong.lbmj.dispatch",
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

## `mahjong.lbmj.state_push`

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
  "msgId": "mahjong.lbmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lcmj.dispatch`

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
  "msgId": "mahjong.lcmj.dispatch",
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

## `mahjong.lcmj.state_push`

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
  "msgId": "mahjong.lcmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lfphmj.dispatch`

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
  "msgId": "mahjong.lfphmj.dispatch",
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

## `mahjong.lfphmj.state_push`

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
  "msgId": "mahjong.lfphmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lgmj.dispatch`

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
  "msgId": "mahjong.lgmj.dispatch",
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

## `mahjong.lgmj.state_push`

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
  "msgId": "mahjong.lgmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lhgmmj.dispatch`

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
  "msgId": "mahjong.lhgmmj.dispatch",
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

## `mahjong.lhgmmj.state_push`

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
  "msgId": "mahjong.lhgmmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lhmj.dispatch`

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
  "msgId": "mahjong.lhmj.dispatch",
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

## `mahjong.lhmj.state_push`

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
  "msgId": "mahjong.lhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lkmj.dispatch`

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
  "msgId": "mahjong.lkmj.dispatch",
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

## `mahjong.lkmj.state_push`

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
  "msgId": "mahjong.lkmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.llfymj.dispatch`

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
  "msgId": "mahjong.llfymj.dispatch",
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

## `mahjong.llfymj.state_push`

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
  "msgId": "mahjong.llfymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lnjzmj.dispatch`

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
  "msgId": "mahjong.lnjzmj.dispatch",
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

## `mahjong.lnjzmj.state_push`

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
  "msgId": "mahjong.lnjzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lnmj.dispatch`

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
  "msgId": "mahjong.lnmj.dispatch",
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

## `mahjong.lnmj.state_push`

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
  "msgId": "mahjong.lnmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lnsymj.dispatch`

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
  "msgId": "mahjong.lnsymj.dispatch",
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

## `mahjong.lnsymj.state_push`

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
  "msgId": "mahjong.lnsymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lpmj.dispatch`

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
  "msgId": "mahjong.lpmj.dispatch",
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

## `mahjong.lpmj.state_push`

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
  "msgId": "mahjong.lpmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lpsmj.dispatch`

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
  "msgId": "mahjong.lpsmj.dispatch",
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

## `mahjong.lpsmj.state_push`

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
  "msgId": "mahjong.lpsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ls13579.dispatch`

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
  "msgId": "mahjong.ls13579.dispatch",
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

## `mahjong.ls13579.state_push`

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
  "msgId": "mahjong.ls13579.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lskjjmj.dispatch`

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
  "msgId": "mahjong.lskjjmj.dispatch",
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

## `mahjong.lskjjmj.state_push`

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
  "msgId": "mahjong.lskjjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lslwzmj.dispatch`

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
  "msgId": "mahjong.lslwzmj.dispatch",
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

## `mahjong.lslwzmj.state_push`

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
  "msgId": "mahjong.lslwzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lsmj.dispatch`

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
  "msgId": "mahjong.lsmj.dispatch",
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

## `mahjong.lsmj.state_push`

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
  "msgId": "mahjong.lsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lsxzmj.dispatch`

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
  "msgId": "mahjong.lsxzmj.dispatch",
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

## `mahjong.lsxzmj.state_push`

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
  "msgId": "mahjong.lsxzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lsyjmj.dispatch`

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
  "msgId": "mahjong.lsyjmj.dispatch",
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

## `mahjong.lsyjmj.state_push`

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
  "msgId": "mahjong.lsyjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lwmj.dispatch`

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
  "msgId": "mahjong.lwmj.dispatch",
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

## `mahjong.lwmj.state_push`

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
  "msgId": "mahjong.lwmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lxmj.dispatch`

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
  "msgId": "mahjong.lxmj.dispatch",
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

## `mahjong.lxmj.state_push`

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
  "msgId": "mahjong.lxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lygcmj.dispatch`

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
  "msgId": "mahjong.lygcmj.dispatch",
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

## `mahjong.lygcmj.state_push`

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
  "msgId": "mahjong.lygcmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lygmj.dispatch`

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
  "msgId": "mahjong.lygmj.dispatch",
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

## `mahjong.lygmj.state_push`

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
  "msgId": "mahjong.lygmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lymj.dispatch`

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
  "msgId": "mahjong.lymj.dispatch",
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

## `mahjong.lymj.state_push`

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
  "msgId": "mahjong.lymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lzmj.dispatch`

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
  "msgId": "mahjong.lzmj.dispatch",
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

## `mahjong.lzmj.state_push`

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
  "msgId": "mahjong.lzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.lzxzmj.dispatch`

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
  "msgId": "mahjong.lzxzmj.dispatch",
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

## `mahjong.lzxzmj.state_push`

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
  "msgId": "mahjong.lzxzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.masmj.dispatch`

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
  "msgId": "mahjong.masmj.dispatch",
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

## `mahjong.masmj.state_push`

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
  "msgId": "mahjong.masmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.mmmj.dispatch`

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
  "msgId": "mahjong.mmmj.dispatch",
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

## `mahjong.mmmj.state_push`

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
  "msgId": "mahjong.mmmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.msmj.dispatch`

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
  "msgId": "mahjong.msmj.dispatch",
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

## `mahjong.msmj.state_push`

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
  "msgId": "mahjong.msmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.mymj.dispatch`

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
  "msgId": "mahjong.mymj.dispatch",
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

## `mahjong.mymj.state_push`

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
  "msgId": "mahjong.mymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.mzmj.dispatch`

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
  "msgId": "mahjong.mzmj.dispatch",
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

## `mahjong.mzmj.state_push`

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
  "msgId": "mahjong.mzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.namj.dispatch`

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
  "msgId": "mahjong.namj.dispatch",
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

## `mahjong.namj.state_push`

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
  "msgId": "mahjong.namj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.nbcxmj.dispatch`

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
  "msgId": "mahjong.nbcxmj.dispatch",
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

## `mahjong.nbcxmj.state_push`

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
  "msgId": "mahjong.nbcxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.nbmj.dispatch`

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
  "msgId": "mahjong.nbmj.dispatch",
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

## `mahjong.nbmj.state_push`

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
  "msgId": "mahjong.nbmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ncaymj.dispatch`

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
  "msgId": "mahjong.ncaymj.dispatch",
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

## `mahjong.ncaymj.state_push`

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
  "msgId": "mahjong.ncaymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ncmj.dispatch`

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
  "msgId": "mahjong.ncmj.dispatch",
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

## `mahjong.ncmj.state_push`

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
  "msgId": "mahjong.ncmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ndmj.dispatch`

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
  "msgId": "mahjong.ndmj.dispatch",
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

## `mahjong.ndmj.state_push`

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
  "msgId": "mahjong.ndmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.nhmj.dispatch`

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
  "msgId": "mahjong.nhmj.dispatch",
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

## `mahjong.nhmj.state_push`

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
  "msgId": "mahjong.nhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.njlhmj.dispatch`

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
  "msgId": "mahjong.njlhmj.dispatch",
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

## `mahjong.njlhmj.state_push`

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
  "msgId": "mahjong.njlhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.njmj.dispatch`

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
  "msgId": "mahjong.njmj.dispatch",
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

## `mahjong.njmj.state_push`

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
  "msgId": "mahjong.njmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.nkbhmj.dispatch`

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
  "msgId": "mahjong.nkbhmj.dispatch",
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

## `mahjong.nkbhmj.state_push`

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
  "msgId": "mahjong.nkbhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.nmgyzmj.dispatch`

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
  "msgId": "mahjong.nmgyzmj.dispatch",
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

## `mahjong.nmgyzmj.state_push`

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
  "msgId": "mahjong.nmgyzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.npgzmj.dispatch`

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
  "msgId": "mahjong.npgzmj.dispatch",
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

## `mahjong.npgzmj.state_push`

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
  "msgId": "mahjong.npgzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.npmj.dispatch`

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
  "msgId": "mahjong.npmj.dispatch",
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

## `mahjong.npmj.state_push`

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
  "msgId": "mahjong.npmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ntcp.dispatch`

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
  "msgId": "mahjong.ntcp.dispatch",
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

## `mahjong.ntcp.state_push`

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
  "msgId": "mahjong.ntcp.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.nxkwmj.dispatch`

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
  "msgId": "mahjong.nxkwmj.dispatch",
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

## `mahjong.nxkwmj.state_push`

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
  "msgId": "mahjong.nxkwmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.nxmj.dispatch`

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
  "msgId": "mahjong.nxmj.dispatch",
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

## `mahjong.nxmj.state_push`

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
  "msgId": "mahjong.nxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.nykwxmj.dispatch`

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
  "msgId": "mahjong.nykwxmj.dispatch",
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

## `mahjong.nykwxmj.state_push`

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
  "msgId": "mahjong.nykwxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.nythmj.dispatch`

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
  "msgId": "mahjong.nythmj.dispatch",
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

## `mahjong.nythmj.state_push`

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
  "msgId": "mahjong.nythmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.nyxxmj.dispatch`

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
  "msgId": "mahjong.nyxxmj.dispatch",
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

## `mahjong.nyxxmj.state_push`

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
  "msgId": "mahjong.nyxxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.nzmj.dispatch`

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
  "msgId": "mahjong.nzmj.dispatch",
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

## `mahjong.nzmj.state_push`

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
  "msgId": "mahjong.nzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.pbyhmj.dispatch`

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
  "msgId": "mahjong.pbyhmj.dispatch",
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

## `mahjong.pbyhmj.state_push`

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
  "msgId": "mahjong.pbyhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.pcmj.dispatch`

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
  "msgId": "mahjong.pcmj.dispatch",
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

## `mahjong.pcmj.state_push`

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
  "msgId": "mahjong.pcmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.pdsjxmj.dispatch`

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
  "msgId": "mahjong.pdsjxmj.dispatch",
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

## `mahjong.pdsjxmj.state_push`

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
  "msgId": "mahjong.pdsjxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.pdslsmj.dispatch`

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
  "msgId": "mahjong.pdslsmj.dispatch",
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

## `mahjong.pdslsmj.state_push`

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
  "msgId": "mahjong.pdslsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.pdsyxmj.dispatch`

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
  "msgId": "mahjong.pdsyxmj.dispatch",
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

## `mahjong.pdsyxmj.state_push`

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
  "msgId": "mahjong.pdsyxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.phmj.dispatch`

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
  "msgId": "mahjong.phmj.dispatch",
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

## `mahjong.phmj.state_push`

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
  "msgId": "mahjong.phmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.pnyhmj.dispatch`

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
  "msgId": "mahjong.pnyhmj.dispatch",
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

## `mahjong.pnyhmj.state_push`

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
  "msgId": "mahjong.pnyhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.pt13mj.dispatch`

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
  "msgId": "mahjong.pt13mj.dispatch",
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

## `mahjong.pt13mj.state_push`

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
  "msgId": "mahjong.pt13mj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ptmj.dispatch`

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
  "msgId": "mahjong.ptmj.dispatch",
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

## `mahjong.ptmj.state_push`

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
  "msgId": "mahjong.ptmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.px258mj.dispatch`

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
  "msgId": "mahjong.px258mj.dispatch",
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

## `mahjong.px258mj.state_push`

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
  "msgId": "mahjong.px258mj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.pxmj.dispatch`

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
  "msgId": "mahjong.pxmj.dispatch",
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

## `mahjong.pxmj.state_push`

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
  "msgId": "mahjong.pxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.pxzzmj.dispatch`

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
  "msgId": "mahjong.pxzzmj.dispatch",
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

## `mahjong.pxzzmj.state_push`

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
  "msgId": "mahjong.pxzzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.pymj.dispatch`

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
  "msgId": "mahjong.pymj.dispatch",
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

## `mahjong.pymj.state_push`

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
  "msgId": "mahjong.pymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.pzmj.dispatch`

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
  "msgId": "mahjong.pzmj.dispatch",
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

## `mahjong.pzmj.state_push`

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
  "msgId": "mahjong.pzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.qdmj.dispatch`

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
  "msgId": "mahjong.qdmj.dispatch",
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

## `mahjong.qdmj.state_push`

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
  "msgId": "mahjong.qdmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.qjfbbmj.dispatch`

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
  "msgId": "mahjong.qjfbbmj.dispatch",
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

## `mahjong.qjfbbmj.state_push`

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
  "msgId": "mahjong.qjfbbmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.qjfxjmj.dispatch`

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
  "msgId": "mahjong.qjfxjmj.dispatch",
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

## `mahjong.qjfxjmj.state_push`

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
  "msgId": "mahjong.qjfxjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.qxwq.dispatch`

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
  "msgId": "mahjong.qxwq.dispatch",
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

## `mahjong.qxwq.state_push`

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
  "msgId": "mahjong.qxwq.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.qymj.dispatch`

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
  "msgId": "mahjong.qymj.dispatch",
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

## `mahjong.qymj.state_push`

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
  "msgId": "mahjong.qymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.qyphmj.dispatch`

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
  "msgId": "mahjong.qyphmj.dispatch",
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

## `mahjong.qyphmj.state_push`

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
  "msgId": "mahjong.qyphmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.qzcsmj.dispatch`

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
  "msgId": "mahjong.qzcsmj.dispatch",
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

## `mahjong.qzcsmj.state_push`

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
  "msgId": "mahjong.qzcsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.qzkhmj.dispatch`

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
  "msgId": "mahjong.qzkhmj.dispatch",
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

## `mahjong.qzkhmj.state_push`

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
  "msgId": "mahjong.qzkhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.qzmj.dispatch`

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
  "msgId": "mahjong.qzmj.dispatch",
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

## `mahjong.qzmj.state_push`

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
  "msgId": "mahjong.qzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.rbmj.dispatch`

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
  "msgId": "mahjong.rbmj.dispatch",
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

## `mahjong.rbmj.state_push`

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
  "msgId": "mahjong.rbmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.rcmj.dispatch`

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
  "msgId": "mahjong.rcmj.dispatch",
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

## `mahjong.rcmj.state_push`

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
  "msgId": "mahjong.rcmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.rgjamj.dispatch`

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
  "msgId": "mahjong.rgjamj.dispatch",
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

## `mahjong.rgjamj.state_push`

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
  "msgId": "mahjong.rgjamj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.rjmj.dispatch`

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
  "msgId": "mahjong.rjmj.dispatch",
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

## `mahjong.rjmj.state_push`

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
  "msgId": "mahjong.rjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.rqmj.dispatch`

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
  "msgId": "mahjong.rqmj.dispatch",
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

## `mahjong.rqmj.state_push`

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
  "msgId": "mahjong.rqmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.rxew.dispatch`

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
  "msgId": "mahjong.rxew.dispatch",
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

## `mahjong.rxew.state_push`

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
  "msgId": "mahjong.rxew.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.rzmj.dispatch`

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
  "msgId": "mahjong.rzmj.dispatch",
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

## `mahjong.rzmj.state_push`

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
  "msgId": "mahjong.rzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.scdymj.dispatch`

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
  "msgId": "mahjong.scdymj.dispatch",
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

## `mahjong.scdymj.state_push`

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
  "msgId": "mahjong.scdymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.scgamj.dispatch`

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
  "msgId": "mahjong.scgamj.dispatch",
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

## `mahjong.scgamj.state_push`

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
  "msgId": "mahjong.scgamj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.scgxmj.dispatch`

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
  "msgId": "mahjong.scgxmj.dispatch",
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

## `mahjong.scgxmj.state_push`

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
  "msgId": "mahjong.scgxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.scmj.dispatch`

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
  "msgId": "mahjong.scmj.dispatch",
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

## `mahjong.scmj.state_push`

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
  "msgId": "mahjong.scmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.scnjmj.dispatch`

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
  "msgId": "mahjong.scnjmj.dispatch",
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

## `mahjong.scnjmj.state_push`

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
  "msgId": "mahjong.scnjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sdfjmj.dispatch`

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
  "msgId": "mahjong.sdfjmj.dispatch",
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

## `mahjong.sdfjmj.state_push`

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
  "msgId": "mahjong.sdfjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sdhzmj.dispatch`

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
  "msgId": "mahjong.sdhzmj.dispatch",
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

## `mahjong.sdhzmj.state_push`

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
  "msgId": "mahjong.sdhzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sdjnmj.dispatch`

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
  "msgId": "mahjong.sdjnmj.dispatch",
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

## `mahjong.sdjnmj.state_push`

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
  "msgId": "mahjong.sdjnmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sdlcmj.dispatch`

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
  "msgId": "mahjong.sdlcmj.dispatch",
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

## `mahjong.sdlcmj.state_push`

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
  "msgId": "mahjong.sdlcmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sdlymj.dispatch`

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
  "msgId": "mahjong.sdlymj.dispatch",
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

## `mahjong.sdlymj.state_push`

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
  "msgId": "mahjong.sdlymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sdzzmj.dispatch`

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
  "msgId": "mahjong.sdzzmj.dispatch",
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

## `mahjong.sdzzmj.state_push`

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
  "msgId": "mahjong.sdzzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sfphmj.dispatch`

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
  "msgId": "mahjong.sfphmj.dispatch",
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

## `mahjong.sfphmj.state_push`

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
  "msgId": "mahjong.sfphmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sgew.dispatch`

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
  "msgId": "mahjong.sgew.dispatch",
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

## `mahjong.sgew.state_push`

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
  "msgId": "mahjong.sgew.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sgmj.dispatch`

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
  "msgId": "mahjong.sgmj.dispatch",
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

## `mahjong.sgmj.state_push`

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
  "msgId": "mahjong.sgmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.shqmmj.dispatch`

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
  "msgId": "mahjong.shqmmj.dispatch",
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

## `mahjong.shqmmj.state_push`

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
  "msgId": "mahjong.shqmmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.smmj.dispatch`

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
  "msgId": "mahjong.smmj.dispatch",
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

## `mahjong.smmj.state_push`

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
  "msgId": "mahjong.smmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sqmj.dispatch`

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
  "msgId": "mahjong.sqmj.dispatch",
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

## `mahjong.sqmj.state_push`

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
  "msgId": "mahjong.sqmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sqsymj.dispatch`

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
  "msgId": "mahjong.sqsymj.dispatch",
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

## `mahjong.sqsymj.state_push`

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
  "msgId": "mahjong.sqsymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sqycmj.dispatch`

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
  "msgId": "mahjong.sqycmj.dispatch",
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

## `mahjong.sqycmj.state_push`

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
  "msgId": "mahjong.sqycmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.srmj.dispatch`

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
  "msgId": "mahjong.srmj.dispatch",
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

## `mahjong.srmj.state_push`

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
  "msgId": "mahjong.srmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sse.dispatch`

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
  "msgId": "mahjong.sse.dispatch",
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

## `mahjong.sse.state_push`

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
  "msgId": "mahjong.sse.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ssmj.dispatch`

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
  "msgId": "mahjong.ssmj.dispatch",
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

## `mahjong.ssmj.state_push`

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
  "msgId": "mahjong.ssmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sspmj.dispatch`

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
  "msgId": "mahjong.sspmj.dispatch",
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

## `mahjong.sspmj.state_push`

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
  "msgId": "mahjong.sspmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.stmj.dispatch`

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
  "msgId": "mahjong.stmj.dispatch",
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

## `mahjong.stmj.state_push`

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
  "msgId": "mahjong.stmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ststmj.dispatch`

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
  "msgId": "mahjong.ststmj.dispatch",
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

## `mahjong.ststmj.state_push`

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
  "msgId": "mahjong.ststmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.swmj.dispatch`

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
  "msgId": "mahjong.swmj.dispatch",
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

## `mahjong.swmj.state_push`

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
  "msgId": "mahjong.swmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sxhtmj.dispatch`

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
  "msgId": "mahjong.sxhtmj.dispatch",
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

## `mahjong.sxhtmj.state_push`

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
  "msgId": "mahjong.sxhtmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sxlsmj.dispatch`

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
  "msgId": "mahjong.sxlsmj.dispatch",
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

## `mahjong.sxlsmj.state_push`

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
  "msgId": "mahjong.sxlsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sxmj.dispatch`

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
  "msgId": "mahjong.sxmj.dispatch",
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

## `mahjong.sxmj.state_push`

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
  "msgId": "mahjong.sxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sxmmj.dispatch`

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
  "msgId": "mahjong.sxmmj.dispatch",
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

## `mahjong.sxmmj.state_push`

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
  "msgId": "mahjong.sxmmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sxzjmj.dispatch`

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
  "msgId": "mahjong.sxzjmj.dispatch",
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

## `mahjong.sxzjmj.state_push`

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
  "msgId": "mahjong.sxzjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.symj.dispatch`

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
  "msgId": "mahjong.symj.dispatch",
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

## `mahjong.symj.state_push`

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
  "msgId": "mahjong.symj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.sysybp.dispatch`

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
  "msgId": "mahjong.sysybp.dispatch",
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

## `mahjong.sysybp.state_push`

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
  "msgId": "mahjong.sysybp.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.szmj.dispatch`

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
  "msgId": "mahjong.szmj.dispatch",
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

## `mahjong.szmj.state_push`

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
  "msgId": "mahjong.szmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.tamj.dispatch`

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
  "msgId": "mahjong.tamj.dispatch",
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

## `mahjong.tamj.state_push`

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
  "msgId": "mahjong.tamj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.tbhmj.dispatch`

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
  "msgId": "mahjong.tbhmj.dispatch",
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

## `mahjong.tbhmj.state_push`

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
  "msgId": "mahjong.tbhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.tbzfbmj.dispatch`

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
  "msgId": "mahjong.tbzfbmj.dispatch",
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

## `mahjong.tbzfbmj.state_push`

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
  "msgId": "mahjong.tbzfbmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.tcmj.dispatch`

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
  "msgId": "mahjong.tcmj.dispatch",
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

## `mahjong.tcmj.state_push`

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
  "msgId": "mahjong.tcmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.tdhmj.dispatch`

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
  "msgId": "mahjong.tdhmj.dispatch",
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

## `mahjong.tdhmj.state_push`

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
  "msgId": "mahjong.tdhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.tgmj.dispatch`

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
  "msgId": "mahjong.tgmj.dispatch",
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

## `mahjong.tgmj.state_push`

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
  "msgId": "mahjong.tgmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.thgjmj.dispatch`

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
  "msgId": "mahjong.thgjmj.dispatch",
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

## `mahjong.thgjmj.state_push`

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
  "msgId": "mahjong.thgjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.tjmj.dispatch`

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
  "msgId": "mahjong.tjmj.dispatch",
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

## `mahjong.tjmj.state_push`

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
  "msgId": "mahjong.tjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.tjtgmj.dispatch`

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
  "msgId": "mahjong.tjtgmj.dispatch",
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

## `mahjong.tjtgmj.state_push`

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
  "msgId": "mahjong.tjtgmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.tjtjmj.dispatch`

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
  "msgId": "mahjong.tjtjmj.dispatch",
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

## `mahjong.tjtjmj.state_push`

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
  "msgId": "mahjong.tjtjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.tmhhmj.dispatch`

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
  "msgId": "mahjong.tmhhmj.dispatch",
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

## `mahjong.tmhhmj.state_push`

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
  "msgId": "mahjong.tmhhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.twmj.dispatch`

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
  "msgId": "mahjong.twmj.dispatch",
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

## `mahjong.twmj.state_push`

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
  "msgId": "mahjong.twmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.txmj.dispatch`

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
  "msgId": "mahjong.txmj.dispatch",
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

## `mahjong.txmj.state_push`

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
  "msgId": "mahjong.txmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.tzjjmj.dispatch`

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
  "msgId": "mahjong.tzjjmj.dispatch",
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

## `mahjong.tzjjmj.state_push`

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
  "msgId": "mahjong.tzjjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.tzkzmj.dispatch`

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
  "msgId": "mahjong.tzkzmj.dispatch",
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

## `mahjong.tzkzmj.state_push`

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
  "msgId": "mahjong.tzkzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.tzmj.dispatch`

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
  "msgId": "mahjong.tzmj.dispatch",
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

## `mahjong.tzmj.state_push`

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
  "msgId": "mahjong.tzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.wabjmj.dispatch`

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
  "msgId": "mahjong.wabjmj.dispatch",
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

## `mahjong.wabjmj.state_push`

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
  "msgId": "mahjong.wabjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.wgfhmj.dispatch`

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
  "msgId": "mahjong.wgfhmj.dispatch",
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

## `mahjong.wgfhmj.state_push`

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
  "msgId": "mahjong.wgfhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.whmj.dispatch`

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
  "msgId": "mahjong.whmj.dispatch",
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

## `mahjong.whmj.state_push`

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
  "msgId": "mahjong.whmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.wjmj.dispatch`

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
  "msgId": "mahjong.wjmj.dispatch",
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

## `mahjong.wjmj.state_push`

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
  "msgId": "mahjong.wjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.wnmj.dispatch`

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
  "msgId": "mahjong.wnmj.dispatch",
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

## `mahjong.wnmj.state_push`

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
  "msgId": "mahjong.wnmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.wnyh.dispatch`

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
  "msgId": "mahjong.wnyh.dispatch",
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

## `mahjong.wnyh.state_push`

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
  "msgId": "mahjong.wnyh.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.wxmj.dispatch`

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
  "msgId": "mahjong.wxmj.dispatch",
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

## `mahjong.wxmj.state_push`

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
  "msgId": "mahjong.wxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.wxzmmj.dispatch`

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
  "msgId": "mahjong.wxzmmj.dispatch",
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

## `mahjong.wxzmmj.state_push`

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
  "msgId": "mahjong.wxzmmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.wzmj.dispatch`

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
  "msgId": "mahjong.wzmj.dispatch",
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

## `mahjong.wzmj.state_push`

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
  "msgId": "mahjong.wzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.wzqsmj.dispatch`

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
  "msgId": "mahjong.wzqsmj.dispatch",
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

## `mahjong.wzqsmj.state_push`

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
  "msgId": "mahjong.wzqsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xcmj.dispatch`

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
  "msgId": "mahjong.xcmj.dispatch",
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

## `mahjong.xcmj.state_push`

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
  "msgId": "mahjong.xcmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xfgzmj.dispatch`

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
  "msgId": "mahjong.xfgzmj.dispatch",
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

## `mahjong.xfgzmj.state_push`

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
  "msgId": "mahjong.xfgzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xgkwxmj.dispatch`

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
  "msgId": "mahjong.xgkwxmj.dispatch",
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

## `mahjong.xgkwxmj.state_push`

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
  "msgId": "mahjong.xgkwxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xhbbmj.dispatch`

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
  "msgId": "mahjong.xhbbmj.dispatch",
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

## `mahjong.xhbbmj.state_push`

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
  "msgId": "mahjong.xhbbmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xhmj.dispatch`

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
  "msgId": "mahjong.xhmj.dispatch",
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

## `mahjong.xhmj.state_push`

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
  "msgId": "mahjong.xhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xhzmj.dispatch`

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
  "msgId": "mahjong.xhzmj.dispatch",
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

## `mahjong.xhzmj.state_push`

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
  "msgId": "mahjong.xhzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xjbjmj.dispatch`

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
  "msgId": "mahjong.xjbjmj.dispatch",
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

## `mahjong.xjbjmj.state_push`

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
  "msgId": "mahjong.xjbjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xjlshmj.dispatch`

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
  "msgId": "mahjong.xjlshmj.dispatch",
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

## `mahjong.xjlshmj.state_push`

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
  "msgId": "mahjong.xjlshmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xjmj.dispatch`

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
  "msgId": "mahjong.xjmj.dispatch",
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

## `mahjong.xjmj.state_push`

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
  "msgId": "mahjong.xjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xjxzmj.dispatch`

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
  "msgId": "mahjong.xjxzmj.dispatch",
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

## `mahjong.xjxzmj.state_push`

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
  "msgId": "mahjong.xjxzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xl2vs2mj.dispatch`

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
  "msgId": "mahjong.xl2vs2mj.dispatch",
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

## `mahjong.xl2vs2mj.state_push`

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
  "msgId": "mahjong.xl2vs2mj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xlhzmj.dispatch`

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
  "msgId": "mahjong.xlhzmj.dispatch",
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

## `mahjong.xlhzmj.state_push`

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
  "msgId": "mahjong.xlhzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xlqmj.dispatch`

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
  "msgId": "mahjong.xlqmj.dispatch",
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

## `mahjong.xlqmj.state_push`

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
  "msgId": "mahjong.xlqmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xmmj.dispatch`

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
  "msgId": "mahjong.xmmj.dispatch",
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

## `mahjong.xmmj.state_push`

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
  "msgId": "mahjong.xmmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xnmj.dispatch`

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
  "msgId": "mahjong.xnmj.dispatch",
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

## `mahjong.xnmj.state_push`

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
  "msgId": "mahjong.xnmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xplp.dispatch`

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
  "msgId": "mahjong.xplp.dispatch",
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

## `mahjong.xplp.state_push`

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
  "msgId": "mahjong.xplp.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xsmj.dispatch`

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
  "msgId": "mahjong.xsmj.dispatch",
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

## `mahjong.xsmj.state_push`

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
  "msgId": "mahjong.xsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xtlhmj.dispatch`

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
  "msgId": "mahjong.xtlhmj.dispatch",
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

## `mahjong.xtlhmj.state_push`

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
  "msgId": "mahjong.xtlhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xtmj.dispatch`

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
  "msgId": "mahjong.xtmj.dispatch",
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

## `mahjong.xtmj.state_push`

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
  "msgId": "mahjong.xtmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
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

## `mahjong.xwmj.dispatch`

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
  "msgId": "mahjong.xwmj.dispatch",
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

## `mahjong.xwmj.state_push`

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
  "msgId": "mahjong.xwmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xxfqmj.dispatch`

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
  "msgId": "mahjong.xxfqmj.dispatch",
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

## `mahjong.xxfqmj.state_push`

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
  "msgId": "mahjong.xxfqmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xxmj.dispatch`

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
  "msgId": "mahjong.xxmj.dispatch",
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

## `mahjong.xxmj.state_push`

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
  "msgId": "mahjong.xxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xxtdhmj.dispatch`

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
  "msgId": "mahjong.xxtdhmj.dispatch",
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

## `mahjong.xxtdhmj.state_push`

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
  "msgId": "mahjong.xxtdhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xygsmj.dispatch`

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
  "msgId": "mahjong.xygsmj.dispatch",
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

## `mahjong.xygsmj.state_push`

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
  "msgId": "mahjong.xygsmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xyhcmj.dispatch`

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
  "msgId": "mahjong.xyhcmj.dispatch",
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

## `mahjong.xyhcmj.state_push`

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
  "msgId": "mahjong.xyhcmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xymj.dispatch`

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
  "msgId": "mahjong.xymj.dispatch",
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

## `mahjong.xymj.state_push`

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
  "msgId": "mahjong.xymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xyscmj.dispatch`

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
  "msgId": "mahjong.xyscmj.dispatch",
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

## `mahjong.xyscmj.state_push`

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
  "msgId": "mahjong.xyscmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xyxmj.dispatch`

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
  "msgId": "mahjong.xyxmj.dispatch",
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

## `mahjong.xyxmj.state_push`

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
  "msgId": "mahjong.xyxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xyxxmj.dispatch`

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
  "msgId": "mahjong.xyxxmj.dispatch",
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

## `mahjong.xyxxmj.state_push`

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
  "msgId": "mahjong.xyxxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.xzmj.dispatch`

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
  "msgId": "mahjong.xzmj.dispatch",
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

## `mahjong.xzmj.state_push`

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
  "msgId": "mahjong.xzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ybgxmj.dispatch`

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
  "msgId": "mahjong.ybgxmj.dispatch",
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

## `mahjong.ybgxmj.state_push`

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
  "msgId": "mahjong.ybgxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ycfxmj.dispatch`

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
  "msgId": "mahjong.ycfxmj.dispatch",
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

## `mahjong.ycfxmj.state_push`

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
  "msgId": "mahjong.ycfxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ycmj.dispatch`

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
  "msgId": "mahjong.ycmj.dispatch",
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

## `mahjong.ycmj.state_push`

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
  "msgId": "mahjong.ycmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ycsgmj.dispatch`

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
  "msgId": "mahjong.ycsgmj.dispatch",
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

## `mahjong.ycsgmj.state_push`

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
  "msgId": "mahjong.ycsgmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yctjmj.dispatch`

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
  "msgId": "mahjong.yctjmj.dispatch",
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

## `mahjong.yctjmj.state_push`

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
  "msgId": "mahjong.yctjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yddgmj.dispatch`

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
  "msgId": "mahjong.yddgmj.dispatch",
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

## `mahjong.yddgmj.state_push`

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
  "msgId": "mahjong.yddgmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yfcgmj.dispatch`

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
  "msgId": "mahjong.yfcgmj.dispatch",
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

## `mahjong.yfcgmj.state_push`

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
  "msgId": "mahjong.yfcgmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yfmj.dispatch`

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
  "msgId": "mahjong.yfmj.dispatch",
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

## `mahjong.yfmj.state_push`

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
  "msgId": "mahjong.yfmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ygjzmj.dispatch`

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
  "msgId": "mahjong.ygjzmj.dispatch",
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

## `mahjong.ygjzmj.state_push`

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
  "msgId": "mahjong.ygjzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ygmj.dispatch`

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
  "msgId": "mahjong.ygmj.dispatch",
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

## `mahjong.ygmj.state_push`

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
  "msgId": "mahjong.ygmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yhmj.dispatch`

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
  "msgId": "mahjong.yhmj.dispatch",
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

## `mahjong.yhmj.state_push`

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
  "msgId": "mahjong.yhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yhzmj.dispatch`

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
  "msgId": "mahjong.yhzmj.dispatch",
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

## `mahjong.yhzmj.state_push`

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
  "msgId": "mahjong.yhzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yjlymj.dispatch`

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
  "msgId": "mahjong.yjlymj.dispatch",
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

## `mahjong.yjlymj.state_push`

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
  "msgId": "mahjong.yjlymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yjmj.dispatch`

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
  "msgId": "mahjong.yjmj.dispatch",
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

## `mahjong.yjmj.state_push`

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
  "msgId": "mahjong.yjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yjnxmj.dispatch`

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
  "msgId": "mahjong.yjnxmj.dispatch",
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

## `mahjong.yjnxmj.state_push`

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
  "msgId": "mahjong.yjnxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ykmj.dispatch`

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
  "msgId": "mahjong.ykmj.dispatch",
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

## `mahjong.ykmj.state_push`

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
  "msgId": "mahjong.ykmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yldgzmj.dispatch`

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
  "msgId": "mahjong.yldgzmj.dispatch",
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

## `mahjong.yldgzmj.state_push`

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
  "msgId": "mahjong.yldgzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ysmj.dispatch`

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
  "msgId": "mahjong.ysmj.dispatch",
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

## `mahjong.ysmj.state_push`

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
  "msgId": "mahjong.ysmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yszmj.dispatch`

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
  "msgId": "mahjong.yszmj.dispatch",
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

## `mahjong.yszmj.state_push`

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
  "msgId": "mahjong.yszmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ytmj.dispatch`

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
  "msgId": "mahjong.ytmj.dispatch",
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

## `mahjong.ytmj.state_push`

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
  "msgId": "mahjong.ytmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.ytyjmj.dispatch`

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
  "msgId": "mahjong.ytyjmj.dispatch",
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

## `mahjong.ytyjmj.state_push`

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
  "msgId": "mahjong.ytyjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yxbzmj.dispatch`

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
  "msgId": "mahjong.yxbzmj.dispatch",
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

## `mahjong.yxbzmj.state_push`

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
  "msgId": "mahjong.yxbzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yxmj.dispatch`

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
  "msgId": "mahjong.yxmj.dispatch",
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

## `mahjong.yxmj.state_push`

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
  "msgId": "mahjong.yxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yxtdh.dispatch`

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
  "msgId": "mahjong.yxtdh.dispatch",
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

## `mahjong.yxtdh.state_push`

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
  "msgId": "mahjong.yxtdh.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yymj.dispatch`

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
  "msgId": "mahjong.yymj.dispatch",
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

## `mahjong.yymj.state_push`

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
  "msgId": "mahjong.yymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yzgymj.dispatch`

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
  "msgId": "mahjong.yzgymj.dispatch",
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

## `mahjong.yzgymj.state_push`

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
  "msgId": "mahjong.yzgymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yzmj.dispatch`

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
  "msgId": "mahjong.yzmj.dispatch",
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

## `mahjong.yzmj.state_push`

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
  "msgId": "mahjong.yzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.yzyzmj.dispatch`

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
  "msgId": "mahjong.yzyzmj.dispatch",
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

## `mahjong.yzyzmj.state_push`

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
  "msgId": "mahjong.yzyzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.za13mj.dispatch`

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
  "msgId": "mahjong.za13mj.dispatch",
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

## `mahjong.za13mj.state_push`

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
  "msgId": "mahjong.za13mj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.za16mj.dispatch`

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
  "msgId": "mahjong.za16mj.dispatch",
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

## `mahjong.za16mj.state_push`

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
  "msgId": "mahjong.za16mj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zass.dispatch`

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
  "msgId": "mahjong.zass.dispatch",
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

## `mahjong.zass.state_push`

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
  "msgId": "mahjong.zass.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zcmj.dispatch`

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
  "msgId": "mahjong.zcmj.dispatch",
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

## `mahjong.zcmj.state_push`

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
  "msgId": "mahjong.zcmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zgmj.dispatch`

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
  "msgId": "mahjong.zgmj.dispatch",
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

## `mahjong.zgmj.state_push`

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
  "msgId": "mahjong.zgmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zgqzmj.dispatch`

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
  "msgId": "mahjong.zgqzmj.dispatch",
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

## `mahjong.zgqzmj.state_push`

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
  "msgId": "mahjong.zgqzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zjgmj.dispatch`

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
  "msgId": "mahjong.zjgmj.dispatch",
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

## `mahjong.zjgmj.state_push`

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
  "msgId": "mahjong.zjgmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zjhzmj.dispatch`

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
  "msgId": "mahjong.zjhzmj.dispatch",
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

## `mahjong.zjhzmj.state_push`

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
  "msgId": "mahjong.zjhzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zjjhmj.dispatch`

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
  "msgId": "mahjong.zjjhmj.dispatch",
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

## `mahjong.zjjhmj.state_push`

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
  "msgId": "mahjong.zjjhmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zjmj.dispatch`

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
  "msgId": "mahjong.zjmj.dispatch",
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

## `mahjong.zjmj.state_push`

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
  "msgId": "mahjong.zjmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zjqzmj.dispatch`

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
  "msgId": "mahjong.zjqzmj.dispatch",
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

## `mahjong.zjqzmj.state_push`

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
  "msgId": "mahjong.zjqzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zjshzmj.dispatch`

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
  "msgId": "mahjong.zjshzmj.dispatch",
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

## `mahjong.zjshzmj.state_push`

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
  "msgId": "mahjong.zjshzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zjtzmj.dispatch`

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
  "msgId": "mahjong.zjtzmj.dispatch",
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

## `mahjong.zjtzmj.state_push`

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
  "msgId": "mahjong.zjtzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zjwzmj.dispatch`

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
  "msgId": "mahjong.zjwzmj.dispatch",
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

## `mahjong.zjwzmj.state_push`

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
  "msgId": "mahjong.zjwzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zklymj.dispatch`

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
  "msgId": "mahjong.zklymj.dispatch",
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

## `mahjong.zklymj.state_push`

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
  "msgId": "mahjong.zklymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zkmj.dispatch`

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
  "msgId": "mahjong.zkmj.dispatch",
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

## `mahjong.zkmj.state_push`

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
  "msgId": "mahjong.zkmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zmdmj.dispatch`

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
  "msgId": "mahjong.zmdmj.dispatch",
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

## `mahjong.zmdmj.state_push`

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
  "msgId": "mahjong.zmdmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zpmj.dispatch`

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
  "msgId": "mahjong.zpmj.dispatch",
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

## `mahjong.zpmj.state_push`

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
  "msgId": "mahjong.zpmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zxmj.dispatch`

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
  "msgId": "mahjong.zxmj.dispatch",
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

## `mahjong.zxmj.state_push`

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
  "msgId": "mahjong.zxmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zymj.dispatch`

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
  "msgId": "mahjong.zymj.dispatch",
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

## `mahjong.zymj.state_push`

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
  "msgId": "mahjong.zymj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zzmj.dispatch`

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
  "msgId": "mahjong.zzmj.dispatch",
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

## `mahjong.zzmj.state_push`

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
  "msgId": "mahjong.zzmj.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `mahjong.zzph.dispatch`

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
  "msgId": "mahjong.zzph.dispatch",
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

## `mahjong.zzph.state_push`

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
  "msgId": "mahjong.zzph.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.anyue2005.dispatch`

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
  "msgId": "poker.anyue2005.dispatch",
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

## `poker.anyue2005.state_push`

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
  "msgId": "poker.anyue2005.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.bozhou2009.dispatch`

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
  "msgId": "poker.bozhou2009.dispatch",
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

## `poker.bozhou2009.state_push`

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
  "msgId": "poker.bozhou2009.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.chengdu2001.dispatch`

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
  "msgId": "poker.chengdu2001.dispatch",
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

## `poker.chengdu2001.state_push`

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
  "msgId": "poker.chengdu2001.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.chengdu2999.dispatch`

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
  "msgId": "poker.chengdu2999.dispatch",
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

## `poker.fuzhou2028.dispatch`

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
  "msgId": "poker.fuzhou2028.dispatch",
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

## `poker.fuzhou2028.state_push`

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
  "msgId": "poker.fuzhou2028.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.gaoan2025.dispatch`

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
  "msgId": "poker.gaoan2025.dispatch",
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

## `poker.gaoan2025.state_push`

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
  "msgId": "poker.gaoan2025.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.gushi2012.dispatch`

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
  "msgId": "poker.gushi2012.dispatch",
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

## `poker.gushi2012.state_push`

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
  "msgId": "poker.gushi2012.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.henan2019.dispatch`

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
  "msgId": "poker.henan2019.dispatch",
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

## `poker.henan2019.state_push`

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
  "msgId": "poker.henan2019.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.huaibin2021.dispatch`

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
  "msgId": "poker.huaibin2021.dispatch",
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

## `poker.huaibin2021.state_push`

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
  "msgId": "poker.huaibin2021.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.huaihua2015.dispatch`

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
  "msgId": "poker.huaihua2015.dispatch",
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

## `poker.huaihua2015.state_push`

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
  "msgId": "poker.huaihua2015.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.huangchuan2022.dispatch`

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
  "msgId": "poker.huangchuan2022.dispatch",
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

## `poker.huangchuan2022.state_push`

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
  "msgId": "poker.huangchuan2022.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.jian2011.dispatch`

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
  "msgId": "poker.jian2011.dispatch",
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

## `poker.jian2011.state_push`

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
  "msgId": "poker.jian2011.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.jiangsu2018.dispatch`

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
  "msgId": "poker.jiangsu2018.dispatch",
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

## `poker.jiangsu2018.state_push`

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
  "msgId": "poker.jiangsu2018.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.jingdezhen2017.dispatch`

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
  "msgId": "poker.jingdezhen2017.dispatch",
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

## `poker.jingdezhen2017.state_push`

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
  "msgId": "poker.jingdezhen2017.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.jintan2024.dispatch`

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
  "msgId": "poker.jintan2024.dispatch",
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

## `poker.jintan2024.state_push`

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
  "msgId": "poker.jintan2024.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.leping2008.dispatch`

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
  "msgId": "poker.leping2008.dispatch",
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

## `poker.leping2008.state_push`

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
  "msgId": "poker.leping2008.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.liangshan2003.dispatch`

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
  "msgId": "poker.liangshan2003.dispatch",
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

## `poker.liangshan2003.state_push`

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
  "msgId": "poker.liangshan2003.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.mianning2004.dispatch`

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
  "msgId": "poker.mianning2004.dispatch",
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

## `poker.mianning2004.state_push`

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
  "msgId": "poker.mianning2004.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.neijiang2002.dispatch`

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
  "msgId": "poker.neijiang2002.dispatch",
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

## `poker.neijiang2002.state_push`

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
  "msgId": "poker.neijiang2002.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.ningdu2014.dispatch`

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
  "msgId": "poker.ningdu2014.dispatch",
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

## `poker.ningdu2014.state_push`

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
  "msgId": "poker.ningdu2014.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.panzhihua2006.dispatch`

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
  "msgId": "poker.panzhihua2006.dispatch",
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

## `poker.panzhihua2006.state_push`

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
  "msgId": "poker.panzhihua2006.state_push",
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

## `poker.poyang2023.dispatch`

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
  "msgId": "poker.poyang2023.dispatch",
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

## `poker.poyang2023.state_push`

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
  "msgId": "poker.poyang2023.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.quanguo2997.dispatch`

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
  "msgId": "poker.quanguo2997.dispatch",
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

## `poker.quanguo2998.dispatch`

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
  "msgId": "poker.quanguo2998.dispatch",
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

## `poker.taizhou2020.dispatch`

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
  "msgId": "poker.taizhou2020.dispatch",
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

## `poker.taizhou2020.state_push`

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
  "msgId": "poker.taizhou2020.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.wannian2007.dispatch`

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
  "msgId": "poker.wannian2007.dispatch",
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

## `poker.wannian2007.state_push`

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
  "msgId": "poker.wannian2007.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.xiaogan2013.dispatch`

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
  "msgId": "poker.xiaogan2013.dispatch",
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

## `poker.xiaogan2013.state_push`

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
  "msgId": "poker.xiaogan2013.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.xinxian2016.dispatch`

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
  "msgId": "poker.xinxian2016.dispatch",
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

## `poker.xinxian2016.state_push`

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
  "msgId": "poker.xinxian2016.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.xuancheng2027.dispatch`

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
  "msgId": "poker.xuancheng2027.dispatch",
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

## `poker.xuancheng2027.state_push`

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
  "msgId": "poker.xuancheng2027.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.yixing2026.dispatch`

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
  "msgId": "poker.yixing2026.dispatch",
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

## `poker.yixing2026.state_push`

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
  "msgId": "poker.yixing2026.state_push",
  "kind": "push",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {}
}
```

## `poker.yugan2010.dispatch`

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
  "msgId": "poker.yugan2010.dispatch",
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

## `poker.yugan2010.state_push`

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
  "msgId": "poker.yugan2010.state_push",
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

## `room.handoff`

| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |
|---|---|---|---|---|---|---|---|---|
| 2.0 | client_to_server | WSS | req | M8 | room_session | 是 | required | 3001, 3005 |

### 请求字段

| 字段 | 类型 | 必填 |
|---|---|---|
| action | string | 是 |
| payload | object | 是 |
| roomId | string | 是 |

### 响应字段

| 字段 | 类型 | 必填 |
|---|---|---|
| accepted | boolean | 是 |
| roomInstanceId | string | 是 |
| lifecycleEpoch | string | 是 |
| sessionId | string | 是 |
| stateVersion | integer | 是 |
| eventSequence | string | 是 |
| eventId | string | 是 |
| connectionGeneration | string | 是 |

### 示例

```json
{
  "protocolVersion": "2.0",
  "msgId": "room.handoff",
  "kind": "req",
  "requestId": "request-id",
  "seq": 1,
  "timestamp": 0,
  "traceId": "trace-id",
  "body": {
    "action": "string",
    "payload": {},
    "roomId": "string"
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
| roomInstanceId | string | 是 |
| lifecycleEpoch | string | 是 |
| sessionId | string | 是 |
| stateVersion | integer | 是 |
| eventSequence | string | 是 |
| eventId | string | 是 |

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

