package com.aoo.bcg.gateway;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

final class GatewayStageOneContractTest {
 @Test void ticketIsBoundToOriginDeviceExpiresAndIsSingleUse(){
  Instant now=Instant.parse("2026-08-24T00:00:00Z");var clock=Clock.fixed(now,ZoneOffset.UTC);var service=new SecureWsTicketService(new SecureRandom(),clock);
  String ticket=service.issue(7,"device-hash","https://game.example",Duration.ofSeconds(30));
  assertThrows(SecurityException.class,()->service.consumeOnce(ticket,"https://game.example","wrong"));
  assertThrows(SecurityException.class,()->service.consumeOnce(ticket,"https://game.example","device-hash"));
  String good=service.issue(7,"device-hash","https://game.example",Duration.ofSeconds(30));assertEquals(7,service.consumeOnce(good,"https://game.example","device-hash").userId());assertThrows(SecurityException.class,()->service.consumeOnce(good,"https://game.example","device-hash"));
  assertThrows(IllegalArgumentException.class,()->service.issue(7,"d","o",Duration.ofSeconds(31)));
 }
 @Test void envelopeNamesAndSerializationAreStrictAndLossless(){
  assertThrows(IllegalArgumentException.class,()->frame("Legacy1001",1,Map.of()));
  assertThrows(IllegalArgumentException.class,()->frame("room.play",1,Map.of("amount",0.1d)));
  assertEquals("9007199254740992",ProtocolValuePolicy.id(9_007_199_254_740_992L));assertEquals("12.34",ProtocolValuePolicy.decimal(new BigDecimal("12.3400")));
  assertTrue(ProtocolValuePolicy.enumValue(Thread.State.class,"FUTURE").isEmpty());
  for(Object invalid:List.of(new BigDecimal("1.0"),new BigInteger("9007199254740992"),9_007_199_254_740_992L,new Object()))assertThrows(IllegalArgumentException.class,()->ProtocolValuePolicy.validate(Map.of("x",invalid)));
  var nullValue=new HashMap<String,Object>();nullValue.put("x",null);assertThrows(IllegalArgumentException.class,()->ProtocolValuePolicy.validate(nullValue));
  var codec=new GatewayFrameCodec();var payload=new LinkedHashMap<String,Object>();payload.put("userId","9007199254740992");payload.put("amountMinor","1234");payload.put("enabled",true);payload.put("items",List.of());
  byte[] bytes=codec.encode(payload,true);assertEquals(payload,codec.decode(bytes,true));assertThrows(IllegalArgumentException.class,()->codec.decode(new byte[GatewayFrameCodec.MAX_WIRE_BYTES+1],false));assertThrows(IllegalArgumentException.class,()->codec.decode("[]".getBytes(java.nio.charset.StandardCharsets.UTF_8),false));assertThrows(IllegalArgumentException.class,()->codec.decode("{\"x\":null}".getBytes(java.nio.charset.StandardCharsets.UTF_8),false));
  byte[] bomb=new byte[GatewayFrameCodec.MAX_EXPANDED_BYTES+1];Arrays.fill(bomb,(byte)'a');var compressed=new java.io.ByteArrayOutputStream();try(var gzip=new java.util.zip.GZIPOutputStream(compressed)){gzip.write(bomb);}catch(java.io.IOException impossible){throw new AssertionError(impossible);}assertThrows(IllegalArgumentException.class,()->codec.decode(compressed.toByteArray(),true));
 }
 @Test void heartbeatBackpressureAndQueueOrderingAreDeterministic(){
  Instant t=Instant.EPOCH;var policy=new WebSocketConnectionPolicy(1,2,2,Duration.ofSeconds(10),Duration.ofSeconds(30),t);policy.activate(t);
  assertEquals(WebSocketConnectionPolicy.EnqueueResult.ACCEPTED,policy.enqueue(frame("room.play",1,Map.of())));assertEquals(WebSocketConnectionPolicy.EnqueueResult.BACKPRESSURE,policy.enqueue(frame("room.play",2,Map.of())));assertEquals(1,policy.poll().orElseThrow().seq());assertEquals(WebSocketConnectionPolicy.State.SUSPECT,policy.inspect(t.plusSeconds(11)));policy.heartbeat(t.plusSeconds(12));assertEquals(WebSocketConnectionPolicy.State.CLOSED,policy.inspect(t.plusSeconds(43)));
 }
 @Test void routesUseVersionsTtlFencesDrainOrphansAndStructuredFailures(){
  var registry=new RoomOwnershipRegistry();registry.registerNode("n1");registry.registerNode("n2");Instant now=Instant.EPOCH;
  var first=registry.acquire(10,"inc-1","n1","2.0",Duration.ofSeconds(30),now);registry.assertWrite(10,"n1",first.fencingToken(),now);
  assertThrows(IllegalStateException.class,()->registry.acquire(10,"inc-evil","n2","2.0",Duration.ofSeconds(30),now));
  var moved=registry.migrate(10,"n2",Duration.ofSeconds(30),now);assertEquals(GatewayErrorCode.ROUTE_MOVED,registry.resolve(10,first.routeVersion(),"2.0",now).error().code());assertThrows(SecurityException.class,()->registry.assertWrite(10,"n1",first.fencingToken(),now));
  registry.drain("n2");assertThrows(IllegalStateException.class,()->registry.acquire(11,"inc","n2","2.0",Duration.ofSeconds(2),now));assertEquals(List.of(moved),registry.orphans(Set.of(),now));
  assertTrue(registry.release(10,"inc-1",moved.fencingToken()));var reused=registry.acquire(10,"inc-2","n1","2.0",Duration.ofSeconds(30),now);assertNotEquals(first.incarnationId(),reused.incarnationId());
  assertDoesNotThrow(()->new RoomOwnershipRegistry.TimeoutBudget(Duration.ofSeconds(3),Duration.ofMillis(200),Duration.ofMillis(800),Duration.ofSeconds(1),1));
  assertThrows(IllegalArgumentException.class,()->new RoomOwnershipRegistry.TimeoutBudget(Duration.ofSeconds(3),Duration.ofMillis(-1),Duration.ofMillis(800),Duration.ofSeconds(1),1));
 }
 @Test void roomExecutorIsExclusiveParallelAcrossRoomsAndRecoversAfterFailure() throws Exception {
  var executor=new RoomOrderedExecutor();var pool=Executors.newFixedThreadPool(12);var start=new CountDownLatch(1);var inRoom=new java.util.concurrent.atomic.AtomicInteger();var maxInRoom=new java.util.concurrent.atomic.AtomicInteger();var enteredDifferentRooms=new CountDownLatch(2);var releaseDifferentRooms=new CountDownLatch(1);var futures=new ArrayList<Future<?>>();
  for(int i=0;i<200;i++)futures.add(pool.submit(()->{start.await();executor.execute(1,()->{int active=inRoom.incrementAndGet();maxInRoom.accumulateAndGet(active,Math::max);Thread.yield();inRoom.decrementAndGet();return null;});return null;}));start.countDown();for(var future:futures)future.get();assertEquals(1,maxInRoom.get());
  Future<?> roomOne=pool.submit(()->executor.execute(10,()->{enteredDifferentRooms.countDown();assertTrue(releaseDifferentRooms.await(2,TimeUnit.SECONDS));return null;}));Future<?> roomTwo=pool.submit(()->executor.execute(11,()->{enteredDifferentRooms.countDown();assertTrue(releaseDifferentRooms.await(2,TimeUnit.SECONDS));return null;}));assertTrue(enteredDifferentRooms.await(2,TimeUnit.SECONDS));releaseDifferentRooms.countDown();roomOne.get();roomTwo.get();
  assertThrows(IllegalStateException.class,()->executor.execute(12,()->{throw new IllegalStateException("boom");}));assertEquals(7,executor.execute(12,()->7));pool.shutdown();assertTrue(pool.awaitTermination(2,TimeUnit.SECONDS));
 }
 private static WebSocketFrame frame(String msg,long seq,Map<String,Object> body){return new WebSocketFrame("2.0",msg,"req","r"+seq,seq,"t"+seq,"1",1,"v1",1,body);}
}
