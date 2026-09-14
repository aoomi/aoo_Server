package com.aoo.bcg.gateway;

import io.netty.buffer.ByteBuf;import io.netty.util.ReferenceCountUtil;import java.util.concurrent.atomic.AtomicBoolean;

/** One explicit owner for a reference-counted buffer or retained slice. */
public final class NettyBufferLease implements AutoCloseable{
 private final ByteBuf buffer;private final AtomicBoolean owned=new AtomicBoolean(true);
 private NettyBufferLease(ByteBuf buffer){if(buffer==null||buffer.refCnt()<=0)throw new IllegalArgumentException("live ByteBuf required");this.buffer=buffer;}
 public static NettyBufferLease takeOwnership(ByteBuf buffer){return new NettyBufferLease(buffer);}
 public NettyBufferLease retainedSlice(int index,int length){if(!owned.get())throw new IllegalStateException("buffer ownership transferred");return new NettyBufferLease(buffer.retainedSlice(index,length));}
 /** Transfers the existing reference to an async consumer; that consumer must release it. */
 public ByteBuf transfer(){if(!owned.compareAndSet(true,false))throw new IllegalStateException("buffer ownership already released");return buffer;}
 public ByteBuf view(){if(!owned.get())throw new IllegalStateException("buffer ownership transferred");return buffer;}
 @Override public void close(){if(owned.compareAndSet(true,false))ReferenceCountUtil.safeRelease(buffer);}
}
