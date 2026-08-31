# MongoDB 8.0.29 生产部署基线

生产与默认运行唯一权威镜像为 `mongo:8.0.29-noble`，清单位于 `deploy/ops/kubernetes/mongodb.yaml`，由现有 Kustomize 生产体系装配。Java MongoDB Driver 独立固定为 `5.10.0`，不随 Server 镜像版本变化。

凭据仅由 `aoo-runtime-secrets` 注入；持久化使用 StatefulSet PVC。发布前执行 `tools/audit_mongodb_baseline.rb` 与 `deploy/ops/scripts/verify-static.sh`。MongoDB `8.3.8` 仅是未来显式兼容验证目标，默认禁用，不得写入生产清单。
