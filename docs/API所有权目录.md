# API 所有权目录

可执行目录位于 `ApiOwnershipCatalog.standard()`。HTTP 精确到公开路径，WSS 按消息命名空间归属模块和维护团队。新增路由或消息在注册前必须加入目录；无所有者入口由 `requireOwner` 拒绝。

当前职责：平台控制面维护管理 API，实时平台维护 `common.*`，麻将、扑克、长牌、字牌和复合玩法团队分别维护自身 WSS 命名空间。
