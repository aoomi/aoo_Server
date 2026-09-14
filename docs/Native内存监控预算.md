# Native/Direct 内存监控预算

Direct Buffer、压缩、TLS 和原生 SDK 在分配前通过 `NativeMemoryBudget.reserve` 申请额度。类别上限与进程总上限同时生效，超限拒绝分配；Reservation 关闭归还额度。

监控输出每类 current、high watermark、limit 和总占用。告警在高水位达到 70%/85%/95% 时分级触发，容量评估禁止只观察 Java heap。
