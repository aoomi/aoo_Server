package com.aoo.bcg.wordcard;
import com.aoo.bcg.gamespi.*;import java.util.*;
/** One catalog-bound Provider class for the pao-hu-zi lifecycle family. */
public final class WordCardFamilyProvider implements GameProvider{
 private final GameDescriptor descriptor;private final GameProvider regional;
 WordCardFamilyProvider(GameDescriptor d,GameProvider r){descriptor=Objects.requireNonNull(d);regional=Objects.requireNonNull(r);}
 public GameDescriptor descriptor(){return descriptor;}public GameRoomFactory roomFactory(){return regional.roomFactory();}
 public Optional<GameCommandHandler>commandHandler(){return regional.commandHandler();}public GameCommandCommitter commandCommitter(){return regional.commandCommitter();}
 public Optional<ReconnectViewProvider<?>>reconnectViewProvider(){return regional.reconnectViewProvider();}public Optional<SettlementProvider>settlementProvider(){return regional.settlementProvider();}
 public Optional<EventReplayProvider>eventReplayProvider(){return regional.eventReplayProvider();}public Optional<AuthoritativeGameSession>createAuthoritativeSession(RoomCreationContext c){return regional.createAuthoritativeSession(c);}
 public Optional<AuthoritativeGameSession>restoreAuthoritativeSession(Map<String,Object>s){return regional.restoreAuthoritativeSession(s);}public List<RuleComponent<GameCommandRequest>>ruleComponents(){return regional.ruleComponents();}
 public Map<String,Object>defaultConfiguration(){var out=new LinkedHashMap<String,Object>(regional.defaultConfiguration());out.put("provider","word-card-family-runtime");out.put("regionCode",descriptor.code());return Map.copyOf(out);}
 public GameCapabilityManifest capabilityManifest(){return regional.capabilityManifest();}
}
