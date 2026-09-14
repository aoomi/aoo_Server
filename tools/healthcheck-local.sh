#!/usr/bin/env bash
set -euo pipefail

failed=0
check_port() {
  local name="$1" port="$2"
  if nc -z 127.0.0.1 "$port" 2>/dev/null; then
    printf 'OK   %-16s 127.0.0.1:%s\n' "$name" "$port"
  else
    printf 'FAIL %-16s 127.0.0.1:%s\n' "$name" "$port"
    failed=1
  fi
}

check_port "正式网关" 8080
check_port "房间权威内网端口" 18080
check_port "版本服务" 8095
check_port "账号服务" 8096
check_port "大厅服务" 8093
check_port "社交服务" 8097
check_port "MySQL" 3306
check_port "Redis" 16379
check_port "MongoDB" 27017

# 904/9998/9888/9996/9886 等旧服务端口已经退出正式本地拓扑。
# Web 预览由 Creator 按需启动，不属于后端健康检查。

exit "$failed"
