package com.aoo.bcg.poker.nn;

import static org.junit.jupiter.api.Assertions.*;
import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.RegionScope;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;

class CN298PokerFamilyProviderFactoryTest {
  private static GameDescriptor descriptor(String code,String family,GameCategory category){
    return new GameDescriptor(5,code,"全国牛牛",category,family,RegionScope.NATIONAL,"","",NiuNiuRules.PLAY_VERSION);
  }
  @Test void bindsOnlyStableCodeAndFamily(){
    var factory=new CN298PokerFamilyProviderFactory();
    assertTrue(factory.create(descriptor("CN298","poker:betting",GameCategory.POKER)).isPresent());
    assertTrue(factory.create(descriptor("nn","poker:betting",GameCategory.POKER)).isEmpty());
    assertTrue(factory.create(descriptor("CN298","CN298",GameCategory.POKER)).isEmpty());
    assertTrue(factory.create(descriptor("CN298","poker:betting",GameCategory.MAHJONG)).isEmpty());
    assertTrue(NiuNiuRules.PLAY_VERSION.matches("^[a-z][a-z0-9._-]*$"));
    assertFalse(factory.create(new GameDescriptor(5,"CN298","全国牛牛",GameCategory.POKER,
        "poker:betting",RegionScope.NATIONAL,"","","CN298-v1.0.0")).isPresent());
  }
  @Test void serviceLoaderPublishesFactoryButNotGameProvider(){
    assertTrue(ServiceLoader.load(com.aoo.bcg.poker.PokerFamilyProviderFactory.class).stream()
        .anyMatch(p->p.type()==CN298PokerFamilyProviderFactory.class));
    assertTrue(ServiceLoader.load(com.aoo.bcg.gamespi.GameProvider.class).stream()
        .noneMatch(p->p.type().getName().startsWith("com.aoo.bcg.poker.nn")));
  }
  @Test void providerPublishesAuthoritativeCommandSpi(){
    var provider=new CN298PokerFamilyProviderFactory()
        .create(descriptor("CN298","poker:betting",GameCategory.POKER)).orElseThrow();
    assertInstanceOf(com.aoo.bcg.gamespi.AuthoritativeSessionCommandHandler.class,
        provider.commandHandler().orElseThrow());
    assertTrue(provider.reconnectViewProvider().isPresent());
    assertTrue(provider.settlementProvider().isPresent());
    var rule=provider.ruleComponents().getFirst();
    assertTrue(rule.execute(command("poker.cn298.sit_req",NiuNiuRules.PLAY_VERSION)).accepted());
    assertEquals("CN298_PLAY_VERSION_MISMATCH",rule.execute(
        command("poker.cn298.sit_req","CN298-v1.0.0")).code());
    assertEquals("CN298_COMMAND_NOT_ALLOWED",rule.execute(
        command("poker.cn298.ready_req",NiuNiuRules.PLAY_VERSION)).code());
  }
  private static com.aoo.bcg.gamespi.GameCommandRequest command(String msgId,String version){
    return new com.aoo.bcg.gamespi.GameCommandRequest(msgId,"rule",1,298001,0,version,"1001",0,java.util.Map.of());
  }
}
