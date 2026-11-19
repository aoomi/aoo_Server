package com.aoo.bcg.common.resource;
import static org.junit.jupiter.api.Assertions.*;import java.time.Duration;import org.junit.jupiter.api.Test;
class HeapReclaimVerifierTest{static final class CompletedRoom implements HeapReclaimVerifier.CycleResource{byte[]players=new byte[4096],events=new byte[8192],snapshot=new byte[4096];Runnable listener=()->{};public void close(){players=null;events=null;snapshot=null;listener=null;}}
 @Test void createCompleteGameAndDissolveCyclesAreCollectable(){var report=HeapReclaimVerifier.verify(CompletedRoom::new,500,Duration.ofSeconds(10));assertTrue(report.passed(),()->"retained rooms="+report.retained());assertEquals(500,report.reclaimed());}}
