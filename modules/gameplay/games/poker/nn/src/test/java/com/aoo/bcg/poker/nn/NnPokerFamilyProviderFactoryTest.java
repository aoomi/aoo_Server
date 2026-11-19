package com.aoo.bcg.poker.nn;

import static org.junit.jupiter.api.Assertions.*;
import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.RegionScope;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

class NnPokerFamilyProviderFactoryTest {
  private static GameDescriptor descriptor(String code,String family,GameCategory category){
    return new GameDescriptor(5,code,"全国牛牛",category,family,RegionScope.NATIONAL,"","",NnRules.PLAY_VERSION);
  }
  @Test void bindsOnlyStableCodeAndFamily(){
    var factory=new NnPokerFamilyProviderFactory();
    assertTrue(factory.create(descriptor("quanguo2998","poker:betting",GameCategory.POKER)).isPresent());
    assertTrue(factory.create(descriptor("nn","poker:betting",GameCategory.POKER)).isEmpty());
    assertTrue(factory.create(descriptor("quanguo2998","quanguo2998",GameCategory.POKER)).isEmpty());
    assertTrue(factory.create(descriptor("quanguo2998","poker:betting",GameCategory.MAHJONG)).isEmpty());
    assertTrue(NnRules.PLAY_VERSION.matches("^[a-z][a-z0-9._-]*$"));
    assertFalse(factory.create(new GameDescriptor(5,"quanguo2998","全国牛牛",GameCategory.POKER,
        "poker:betting",RegionScope.NATIONAL,"","","NN-v1.0.0")).isPresent());
  }
  @Test void serviceLoaderPublishesFactoryButNotGameProvider(){
    assertTrue(ServiceLoader.load(com.aoo.bcg.poker.PokerFamilyProviderFactory.class).stream()
        .anyMatch(p->p.type()==NnPokerFamilyProviderFactory.class));
    assertTrue(ServiceLoader.load(com.aoo.bcg.gamespi.GameProvider.class).stream()
        .noneMatch(p->p.type().getName().startsWith("com.aoo.bcg.poker.nn")));
  }
  @Test void providerPublishesAuthoritativeCommandSpi(){
    var provider=new NnPokerFamilyProviderFactory()
        .create(descriptor("quanguo2998","poker:betting",GameCategory.POKER)).orElseThrow();
    assertTrue(provider.commandHandler().isPresent());
    assertTrue(provider.reconnectViewProvider().isPresent());
    assertTrue(provider.settlementProvider().isPresent());
    var rule=provider.ruleComponents().getFirst();
    assertTrue(rule.execute(command("poker.quanguo2998.sit_req",NnRules.PLAY_VERSION)).accepted());
    assertEquals("NN_PLAY_VERSION_MISMATCH",rule.execute(
        command("poker.quanguo2998.sit_req","NN-v1.0.0")).code());
    assertEquals("NN_COMMAND_NOT_ALLOWED",rule.execute(
        command("poker.quanguo2998.ready_req",NnRules.PLAY_VERSION)).code());
  }
  @Test void outerDispatchIsStrictlyUnwrappedBeforeAuthoritativeExecution(){
    var provider=new NnPokerFamilyProviderFactory()
        .create(descriptor("quanguo2998","poker:betting",GameCategory.POKER)).orElseThrow();
    var room=provider.roomFactory().create(new com.aoo.bcg.gamespi.RoomCreationContext(298001,1001,java.util.Map.of()));
    var request=new com.aoo.bcg.gamespi.GameCommandRequest("poker.quanguo2998.dispatch","dispatch-state",1,
        298001,0,NnRules.PLAY_VERSION,"1001",-1,
        java.util.Map.of("action","poker.quanguo2998.state_req","payload",java.util.Map.of()));
    assertTrue(provider.ruleComponents().getFirst().execute(request).accepted());
    var response=provider.commandHandler().orElseThrow().handle(room,request);
    assertEquals("poker.quanguo2998.state_resp",response.msgId());
    assertEquals("quanguo2998",response.body().asMap().get("gameCode"));
    assertThrows(IllegalArgumentException.class,()->provider.commandHandler().orElseThrow().handle(room,
        new com.aoo.bcg.gamespi.GameCommandRequest("poker.quanguo2998.dispatch","dispatch-bad",2,
            298001,0,NnRules.PLAY_VERSION,"1001",-1,
            java.util.Map.of("action","poker.quanguo2998.unknown_req","payload",java.util.Map.of()))));
  }
  @Test void roomFactoryCarriesExplicitSnapshotMetadataSource(){
    var provider=new NnPokerFamilyProviderFactory()
        .create(descriptor("quanguo2998","poker:betting",GameCategory.POKER)).orElseThrow();
    var source=new com.aoo.bcg.gamespi.RoomSnapshotMetadataSource(7L,"a".repeat(64),"b".repeat(64),
        "card-v1","events-v1","protocol-v1","snapshot-v1",11L,"CN-SC","c".repeat(64));
    var room=provider.roomFactory().create(new com.aoo.bcg.gamespi.RoomCreationContext(298002,1001,
        java.util.Map.of(),null,source));
    assertSame(source,room.requireRoomSnapshotMetadataSource());
  }
  @Test void providerRestoresDurableAuthorityForStateRequests(){
    var provider=new NnPokerFamilyProviderFactory()
        .create(descriptor("quanguo2998","poker:betting",GameCategory.POKER)).orElseThrow();
    var original=provider.createAuthoritativeSession(
        new com.aoo.bcg.gamespi.RoomCreationContext(298011,11001,java.util.Map.of())).orElseThrow();
    var restored=provider.restoreAuthoritativeSession(original.authoritativeState()).orElseThrow();
    assertEquals(original.authoritativeState(),restored.authoritativeState());
    var state=new com.aoo.bcg.gamespi.GameCommandRequest("poker.quanguo2998.state_req","restored-state",1,
        298011,0,NnRules.PLAY_VERSION,"11001",-1,java.util.Map.of());
    assertEquals(original.execute(state).body(),restored.execute(state).body());
  }
  private static com.aoo.bcg.gamespi.GameCommandRequest command(String msgId,String version){
    return new com.aoo.bcg.gamespi.GameCommandRequest(msgId,"rule",1,298001,0,version,"1001",0,java.util.Map.of());
  }
}
