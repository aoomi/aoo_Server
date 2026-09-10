package com.aoo.bcg.poker;
import com.aoo.bcg.gamespi.*;import java.util.*;

/** Catalog bindings for the source-backed Poker engines; no code-specific SPI entries. */
public final class PokerCatalogRuntimeRegistry{
 private PokerCatalogRuntimeRegistry(){}
 public static Optional<GameProvider>providerFor(GameDescriptor d){
  if(d.category()!=GameCategory.POKER)return Optional.empty();
  // 跑得快的地区名称和 gameId 只是发布配置，不得选择不同运行实现。
  // 由权威 family 绑定唯一 Provider，新增玩法只增加 catalog/rule config。
  GameProvider p=PaoDeKuaiFamily.CODE.equals(d.family())?new PdkGameProvider(d):switch(d.code()){case"cp"->new CpGameProvider();case"hndzp"->new HndzpGameProvider();case"lhzp"->new LhzpGameProvider();default->null;};
  return p==null?Optional.empty():Optional.of(new PokerFamilyCatalogProvider(d,p));
 }
}

final class PokerFamilyCatalogProvider implements GameProvider{
 private final GameDescriptor descriptor;private final GameProvider delegate;
 PokerFamilyCatalogProvider(GameDescriptor d,GameProvider p){descriptor=Objects.requireNonNull(d);delegate=Objects.requireNonNull(p);}
 public GameDescriptor descriptor(){return descriptor;}public GameRoomFactory roomFactory(){return delegate.roomFactory();}public Optional<GameCommandHandler>commandHandler(){return delegate.commandHandler();}public GameCommandCommitter commandCommitter(){return delegate.commandCommitter();}public Optional<ReconnectViewProvider<?>>reconnectViewProvider(){return delegate.reconnectViewProvider();}public Optional<SettlementProvider>settlementProvider(){return delegate.settlementProvider();}public Optional<EventReplayProvider>eventReplayProvider(){return delegate.eventReplayProvider();}public Optional<AuthoritativeGameSession>createAuthoritativeSession(RoomCreationContext c){return delegate.createAuthoritativeSession(c);}public Optional<AuthoritativeGameSession>restoreAuthoritativeSession(Map<String,Object>s){return delegate.restoreAuthoritativeSession(s);}public List<RuleComponent<GameCommandRequest>>ruleComponents(){return delegate.ruleComponents();}public GameCapabilityManifest capabilityManifest(){return delegate.capabilityManifest();}
 public Map<String,Object>defaultConfiguration(){var out=new LinkedHashMap<String,Object>(delegate.defaultConfiguration());out.put("regionalProvider",out.getOrDefault("provider","poker-family-runtime"));out.put("provider","poker-family-runtime");out.put("gameCode",descriptor.code());out.put("playVersion",descriptor.version());return Map.copyOf(out);}
}

abstract class RegionalPokerProvider implements GameProvider{
 final String code;final int id;final String version;private final String family,province;
 RegionalPokerProvider(String c,int i,String v,String f,String p){code=c;id=i;version=v;family=f;province=p;}
 public GameDescriptor descriptor(){return new GameDescriptor(id,code,code.toUpperCase(),GameCategory.POKER,family,RegionScope.PROVINCE,province,"",version);}
 public GameRoomFactory roomFactory(){return c->new GameRoomHandle(c.roomId(),id,version,create(c));}abstract AuthoritativeGameSession create(RoomCreationContext c);abstract AuthoritativeGameSession restore(Map<String,Object>s);abstract StatePayload replay(StatePayload b,List<Object>e);
 public Optional<GameCommandHandler>commandHandler(){return Optional.of(this instanceof PokerGameProvider?new PokerDispatchCommandHandler():new AuthoritativeSessionCommandHandler());}public Optional<ReconnectViewProvider<?>>reconnectViewProvider(){return Optional.of((p,r)->r.requireAuthoritativeSession().viewFor(p));}public Optional<SettlementProvider>settlementProvider(){return Optional.of((r,n)->r.requireAuthoritativeSession().settlement(n,version));}public Optional<AuthoritativeGameSession>restoreAuthoritativeSession(Map<String,Object>s){return Optional.of(restore(s));}public Optional<EventReplayProvider>eventReplayProvider(){return Optional.of(this::replay);}
 public List<RuleComponent<GameCommandRequest>>ruleComponents(){return List.of(new RuleComponent<>(){public String ruleId(){return family+".provider-boundary";}public String componentVersion(){return version;}public RuleStage stage(){return RuleStage.PLAY;}public int priority(){return 100;}public RuleResult execute(GameCommandRequest request){return request.playVersion().equals(version)&&(request.msgId().startsWith("poker.")||request.msgId().startsWith("common.room."))?RuleResult.accept():RuleResult.reject("POKER_MESSAGE_REQUIRED","message does not belong to Poker runtime");}});}
}

final class PdkGameProvider extends RegionalPokerProvider implements PokerGameProvider {
 static final int GAME_ID=8; static final String VERSION="1.0.0";
 private static final PaoDeKuaiConfig GENERIC_BASE=
   new PaoDeKuaiConfig(5,true,true,false,null,true).withCardsPerPlayer(16);
 private final String playVersion; private final boolean chengdu;
 private final PdkRegionRules regionalRules;
 private final PaoDeKuaiConfig baseConfig; private final PaoDeKuaiFamily family;

 PdkGameProvider(){this(new GameDescriptor(GAME_ID,PdkBusinessCodes.CHENGDU,"成都跑得快",
   GameCategory.POKER,PaoDeKuaiFamily.CODE,RegionScope.CITY,"四川","成都",VERSION));}
 PdkGameProvider(GameDescriptor descriptor){
  super(descriptor.code(),descriptor.gameId(),descriptor.version(),PaoDeKuaiFamily.CODE,"");
  playVersion=descriptor.version();
  chengdu=descriptor.gameId()==GAME_ID&&PdkBusinessCodes.CHENGDU.equals(descriptor.code());
  regionalRules=switch(descriptor.code()){case PdkBusinessCodes.NEIJIANG->new NeijiangPdkRules();case PdkBusinessCodes.LIANGSHAN->new LiangshanPdkRules();default->null;};
  baseConfig=chengdu?ChengduPdkRules.defaults():regionalRules==null?GENERIC_BASE:regionalRules.defaults();
  if(chengdu)family=familyForNewRoom(baseConfig,Map.of("deckMode","CUT_40"),false);
  else if(regionalRules!=null){
   Map<String,Object>defaults=regionalRules.authoritativeRules(Map.of(),regionalRules.defaultPlayers());
   PaoDeKuaiConfig config=PdkPublishedRuleOptions.apply(defaults,baseConfig);
   family=familyForRestore(config,defaults,false);
  }else family=familyForRestore(baseConfig,Map.of(),false);
 }
 public PokerRuleFamily pokerFamily(){return family;}
 AuthoritativeGameSession create(RoomCreationContext c){
  Map<String,Object>publishedRules=PdkPublishedRuleOptions.normalizePublishedRoomFields(c.immutableRules());
  int seats=number(publishedRules,"playerCount",chengdu?2:regionalRules==null?3:regionalRules.defaultPlayers());
  int rounds=number(publishedRules,"roundCount",8);
  long seed=publishedRules.get("shuffleSeed")instanceof Number n?n.longValue():c.roomId();
  Map<String,Object>effectiveRules=chengdu?chengduRulesForSeats(publishedRules,seats):
    regionalRules==null?publishedRules:regionalRules.authoritativeRules(publishedRules,seats);
  PaoDeKuaiConfig config=PdkPublishedRuleOptions.apply(effectiveRules,baseConfig);
  if(regionalRules==null)config=config.withCardsPerPlayer(16);
  boolean allowPassByRoomRule=explicitlyAllowsPass(publishedRules);
  PaoDeKuaiFamily selected=chengdu?familyForNewRoom(config,effectiveRules,allowPassByRoomRule):
    familyForRestore(config,effectiveRules,allowPassByRoomRule);
  selected.profile().validatePlayerCount(seats,config.cardsPerPlayer());
  return new PokerAuthoritativeSession(c.roomId(),c.ownerId(),seats,seed,selected,rounds);
 }
 AuthoritativeGameSession restore(Map<String,Object>s){
  Object raw=s.get("pdkRuleOptions");
  if(!(raw instanceof Map<?,?>map))
   throw new IllegalStateException("missing immutable PDK rule options");
  Map<String,Object>options=new LinkedHashMap<>();
  map.forEach((k,v)->options.put(String.valueOf(k),v));
  PaoDeKuaiConfig restoredConfig=PdkPublishedRuleOptions.apply(options,baseConfig);
  if(regionalRules==null)restoredConfig=restoredConfig.withCardsPerPlayer(16);
  PaoDeKuaiFamily restoredFamily=familyForRestore(restoredConfig,options,
    Boolean.TRUE.equals(options.get("allowPassByRoomRule")));
  Map<String,Object> restoredState=PdkPublishedRuleOptions.migrateVerifiedLegacySnapshotIdentity(
    s,restoredFamily);
  return PokerAuthoritativeSession.restore(restoredState,restoredFamily);
 }
 private static int number(Map<String,Object>rules,String key,int fallback){
  Object value=rules.get(key); return value instanceof Number n?n.intValue():fallback;
 }
 private static boolean explicitlyAllowsPass(Map<String,Object>rules){
  return rules.containsKey("roomBeatWhenPossible")
    ?Boolean.FALSE.equals(rules.get("roomBeatWhenPossible"))
    :rules.containsKey("mustBeatWhenPossible")
      &&Boolean.FALSE.equals(rules.get("mustBeatWhenPossible"));
 }
 /** 三人成都局必须使用 48 张标准牌组，确保三家各 16 张且只保留 3A/1个2。 */
 private static Map<String,Object>chengduRulesForSeats(Map<String,Object>rules,int seats){
  if(seats!=3)return rules;
  Map<String,Object>effective=new LinkedHashMap<>(rules);
  effective.put("deckMode","STANDARD_48");
  effective.remove("deckCards");
  return Map.copyOf(effective);
 }
 /** Hall/client payloads can select a published deck mode, but never supply the deck itself. */
 private PaoDeKuaiFamily familyForNewRoom(PaoDeKuaiConfig config,Map<String,Object>rules,
   boolean allowPassByRoomRule){
  if(!chengdu)return familyForRestore(config,rules,allowPassByRoomRule);
  boolean cut=ChengduPdkRules.cutDeckSelected(rules);
  Map<String,Object>authoritative=new LinkedHashMap<>(rules);
  authoritative.put("deckMode",cut?"CUT_40":"STANDARD_48");
  authoritative.put("deckCards",cut?ChengduPdkRules.cutDeck():ChengduPdkRules.standardDeck());
  PokerRuleProfile base=ChengduPdkRules.profile(playVersion,cut);
  return new PaoDeKuaiFamily(config,
    PdkPublishedRuleOptions.profile(playVersion,authoritative,config,base),allowPassByRoomRule);
 }
 /** Restore keeps an already-started historical release byte-for-byte replayable. */
 private PaoDeKuaiFamily familyForRestore(PaoDeKuaiConfig config,Map<String,Object>rules,
   boolean allowPassByRoomRule){
  Object rawDeck=rules.get("deckCards");
  boolean historicalGeneric=chengdu&&rawDeck instanceof List<?>deck
    &&!ChengduPdkRules.standardDeck().equals(deck)&&!ChengduPdkRules.cutDeck().equals(deck);
  PokerRuleProfile base=chengdu&&!historicalGeneric
    ?ChengduPdkRules.profile(playVersion,ChengduPdkRules.cutDeckSelected(rules))
    :regionalRules!=null?regionalRules.profile(playVersion,rules,config)
    :PdkRuleProfiles.flexibleTwoToFourPlayers(playVersion,config.requiredFirstCard());
  return new PaoDeKuaiFamily(config,
    PdkPublishedRuleOptions.profile(playVersion,rules,config,base),allowPassByRoomRule);
 }
 StatePayload replay(StatePayload b,List<Object>e){return PokerAuthoritativeSession.replay(b,e);}
 public Map<String,Object>defaultConfiguration(){return Map.of("enabled",true,
   "playVersion",playVersion,"provider",regionalRules==null?"native-pdk":regionalRules.providerKey(),
   "gameCode",code,"deckSize",family.profile().deckSize(),
   "deckMode",chengdu?"CUT_40":regionalRules==null?"GENERIC_48":regionalRules.defaultDeckMode(),
   "minimumStraightLength",family.rules().config().minimumStraightLength(),"allowFourWithTwo",family.rules().config().allowFourWithTwo(),
   "cardsPerPlayer",regionalRules==null?16:regionalRules.defaultCardsPerPlayer());}
}

final class CpGameProvider extends RegionalPokerProvider{static final int GAME_ID=392;static final String VERSION="1.0.0";CpGameProvider(){super("cp",GAME_ID,VERSION,"poker:compare-hand","chongqing");}AuthoritativeGameSession create(RoomCreationContext c){return new CpSession(c.roomId(),c.ownerId(),c.immutableRules());}AuthoritativeGameSession restore(Map<String,Object>s){return CpSession.restore(s);}StatePayload replay(StatePayload b,List<Object>e){return CpSession.replay(b,e);}public Map<String,Object>defaultConfiguration(){return Map.of("enabled",true,"playVersion",VERSION,"provider","native-cp","players",3,"deckSize",84,"moshi",0,"fanshushangxian",0,"difen",0);}}
final class HndzpGameProvider extends RegionalPokerProvider{static final int GAME_ID=267;static final String VERSION="1.0.0";HndzpGameProvider(){super("hndzp",GAME_ID,VERSION,"poker:landlord","hainan");}AuthoritativeGameSession create(RoomCreationContext c){return new HndzpSession(c.roomId(),c.ownerId(),c.immutableRules());}AuthoritativeGameSession restore(Map<String,Object>s){return HndzpSession.restore(s);}StatePayload replay(StatePayload b,List<Object>e){return HndzpSession.replay(b,e);}public Map<String,Object>defaultConfiguration(){return Map.of("enabled",true,"playVersion",VERSION,"provider","native-hndzp");}}
final class LhzpGameProvider extends RegionalPokerProvider{static final int GAME_ID=129;static final String VERSION="1.0.0";LhzpGameProvider(){super("lhzp",GAME_ID,VERSION,"poker:510k","jiangxi");}AuthoritativeGameSession create(RoomCreationContext c){return new LhzpSession(c.roomId(),c.ownerId(),c.immutableRules());}AuthoritativeGameSession restore(Map<String,Object>s){return LhzpSession.restore(s);}StatePayload replay(StatePayload b,List<Object>e){return LhzpSession.replay(b,e);}public Map<String,Object>defaultConfiguration(){return Map.of("enabled",true,"playVersion",VERSION,"provider","native-lhzp");}}
