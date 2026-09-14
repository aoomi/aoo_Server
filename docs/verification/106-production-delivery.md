# 106 生产交付验证

## 结论

静态装配与本地可执行门禁已完成；真实生产环境验收暂缓。不得据此文档宣称生产发布或业务终验通过。

## 已完成

- Kubernetes 单一 kustomization 装配 Account、Gateway、Bootstrap、Hall、NJPDK、CDXZMJ 与 Admin，共 7 个 Deployment/Service。
- 所有工作负载包含滚动策略、修订历史、发布超时、三类探针、资源配额、非 root/只读根文件系统、优雅终止及 PDB。
- 运行配置来自 ConfigMap，敏感配置仅引用发布环境预置的 `aoo-runtime-secrets`；清单不创建、伪造或硬编码 Secret。
- 渲染脚本要求全部镜像使用完整 registry 引用与 sha256 digest，缺失或 tag-only 镜像会失败关闭。
- 发布流水线下载并校验构建产物，使用 GitHub OIDC/Sigstore keyless cosign 签名，并按工作流身份和 issuer 验签；签名材料随产物归档。
- 回滚脚本要求显式历史 revision，对全部工作负载执行并等待回滚完成。

## 本地验证

```sh
deploy/ops/scripts/verify-static.sh
python3 -m unittest discover -s deploy/ops/tests -v
```

## 暂缓项

- 正式镜像仓库推送、实际 digest 与签名透明日志验证。
- 生产 Secret 管理器注入、TLS/WAF/CDN、真实集群 server-side dry-run。
- 灰度、故障注入、容量、多实例一致性及真实 revision 回滚演练。
- 登录→大厅→房间→完整牌局→结算/战绩/回放的真实生产环境业务验收。
