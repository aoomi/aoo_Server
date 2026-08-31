# Netty Buffer 所有权规范

接收 ByteBuf 后立即放入 `NettyBufferLease`。切片只能使用 `retainedSlice` 并产生新的 lease；同步和异常路径使用 try-with-resources。异步传递必须调用 `transfer`，接收者成为唯一释放方。

测试启用 PARANOID 泄漏检测并断言根 Buffer、切片和转移引用最终 `refCnt=0`。禁止保存未 retain 的切片或重复 release。
