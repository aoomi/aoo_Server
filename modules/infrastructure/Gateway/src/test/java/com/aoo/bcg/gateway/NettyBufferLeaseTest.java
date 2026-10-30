package com.aoo.bcg.gateway;
import static org.junit.jupiter.api.Assertions.*;import io.netty.buffer.*;import io.netty.util.*;import org.junit.jupiter.api.*;
class NettyBufferLeaseTest{
 @BeforeAll static void paranoidLeakDetection(){ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);}
 @Test void releasesRootAndRetainedSliceAcrossExceptionPath(){ByteBuf root=Unpooled.buffer().writeLong(7);assertThrows(IllegalStateException.class,()->{try(var owner=NettyBufferLease.takeOwnership(root);var slice=owner.retainedSlice(0,4)){assertEquals(2,root.refCnt());throw new IllegalStateException("handler failed");}});assertEquals(0,root.refCnt());}
 @Test void asyncTransferHasExactlyOneRemainingOwner(){ByteBuf root=Unpooled.buffer(1);var lease=NettyBufferLease.takeOwnership(root);ByteBuf transferred=lease.transfer();lease.close();assertEquals(1,transferred.refCnt());ReferenceCountUtil.safeRelease(transferred);assertEquals(0,root.refCnt());assertThrows(IllegalStateException.class,lease::view);}
}
