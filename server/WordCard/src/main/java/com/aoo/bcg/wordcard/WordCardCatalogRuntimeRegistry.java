package com.aoo.bcg.wordcard;
import com.aoo.bcg.gamespi.*;import java.util.Optional;
/** Expands ten word-card catalog rows onto one family Provider class. */
public final class WordCardCatalogRuntimeRegistry{
 private WordCardCatalogRuntimeRegistry(){}
 public static Optional<GameProvider>providerFor(GameDescriptor d){if(d.category()!=GameCategory.WORD_CARD)return Optional.empty();GameProvider r=switch(d.code()){
  case"byzp"->new ByzpGameProvider();case"bzp"->new BzpGameProvider();case"ycsdr"->new YcsdrGameProvider();case"pxphz"->new PxphzGameProvider();case"xpphz"->new XpphzGameProvider();
  case"yzchz"->new YzchzGameProvider();case"glzp"->new GlzpGameProvider();case"ychp"->new YchpGameProvider();case"ahphz"->new AhphzGameProvider();case"lczp"->new LczpGameProvider();default->null;};
  return r==null?Optional.empty():Optional.of(new WordCardFamilyProvider(d,r));}
}

class RegionalWordCardProvider implements GameProvider{
 static final String VERSION="1.0.0";private final String code;private final int id;private final RegionScope scope;private final String province,city;
 RegionalWordCardProvider(String c,int i,RegionScope s,String p,String y){code=c;id=i;scope=s;province=p;city=y;}
 public GameDescriptor descriptor(){return new GameDescriptor(id,code,code.toUpperCase(),GameCategory.WORD_CARD,"word-card-pao-hu-zi",scope,province,city,VERSION);}
 public GameRoomFactory roomFactory(){return c->new GameRoomHandle(c.roomId(),id,VERSION,switch(code){case"byzp"->new ByzpSession(c.roomId(),c.ownerId(),c.immutableRules());case"bzp"->new BzpSession(c.roomId(),c.ownerId(),c.immutableRules());case"ycsdr"->new YcsdrSession(c.roomId(),c.ownerId(),c.immutableRules());case"pxphz"->new PxphzSession(c.roomId(),c.ownerId(),c.immutableRules());case"xpphz"->new XpphzSession(c.roomId(),c.ownerId(),c.immutableRules());case"yzchz"->new YzchzSession(c.roomId(),c.ownerId(),c.immutableRules());case"glzp"->new GlzpSession(c.roomId(),c.ownerId(),c.immutableRules());case"ychp"->new YchpSession(c.roomId(),c.ownerId(),c.immutableRules());case"ahphz"->new AhphzSession(c.roomId(),c.ownerId(),c.immutableRules());case"lczp"->new LczpSession(c.roomId(),c.ownerId(),c.immutableRules());default->throw new IllegalStateException(code);});}
 public Optional<GameCommandHandler>commandHandler(){return Optional.of(new AuthoritativeSessionCommandHandler());}public Optional<ReconnectViewProvider<?>>reconnectViewProvider(){return Optional.of((p,r)->r.requireAuthoritativeSession().viewFor(p));}public Optional<SettlementProvider>settlementProvider(){return Optional.of((r,n)->r.requireAuthoritativeSession().settlement(n,VERSION));}
 public Optional<AuthoritativeGameSession>restoreAuthoritativeSession(java.util.Map<String,Object>s){return Optional.of(switch(code){case"byzp"->ByzpSession.restore(s);case"bzp"->BzpSession.restore(s);case"ycsdr"->YcsdrSession.restore(s);case"pxphz"->PxphzSession.restore(s);case"xpphz"->XpphzSession.restore(s);case"yzchz"->YzchzSession.restore(s);case"glzp"->GlzpSession.restore(s);case"ychp"->YchpSession.restore(s);case"ahphz"->AhphzSession.restore(s);case"lczp"->LczpSession.restore(s);default->throw new IllegalStateException(code);});}
 public Optional<EventReplayProvider>eventReplayProvider(){return Optional.of((base,events)->switch(code){case"byzp"->ByzpSession.replay(base,events);case"bzp"->BzpSession.replay(base,events);case"ycsdr"->YcsdrSession.replay(base,events);case"pxphz"->PxphzSession.replay(base,events);case"xpphz"->XpphzSession.replay(base,events);case"yzchz"->YzchzSession.replay(base,events);case"glzp"->GlzpSession.replay(base,events);case"ychp"->YchpSession.replay(base,events);case"ahphz"->AhphzSession.replay(base,events);case"lczp"->LczpSession.replay(base,events);default->throw new IllegalStateException(code);});}
 public java.util.Map<String,Object>defaultConfiguration(){return java.util.Map.of("enabled",true,"playVersion",VERSION,"provider","word-card-family-runtime","regionCode",code);}
 public java.util.List<RuleComponent<GameCommandRequest>>ruleComponents(){return java.util.List.of(new RuleComponent<>(){public String ruleId(){return"wordcard.family."+code;}public String componentVersion(){return VERSION;}public RuleStage stage(){return RuleStage.PLAY;}public int priority(){return 100;}public RuleResult execute(GameCommandRequest r){return("wordcard."+code+".dispatch").equals(r.msgId())?RuleResult.accept():RuleResult.reject("FAMILY_MESSAGE_REQUIRED","wrong word-card family message");}});}
}
final class ByzpGameProvider extends RegionalWordCardProvider{ByzpGameProvider(){super("byzp",136,RegionScope.PROVINCE,"guangxi","");}}
final class XpphzGameProvider extends RegionalWordCardProvider{XpphzGameProvider(){super("xpphz",153,RegionScope.PROVINCE,"hunan","");}}
final class AhphzGameProvider extends RegionalWordCardProvider{AhphzGameProvider(){super("ahphz",176,RegionScope.PROVINCE,"hunan","");}}
final class GlzpGameProvider extends RegionalWordCardProvider{GlzpGameProvider(){super("glzp",302,RegionScope.PROVINCE,"guangxi","");}}
final class YzchzGameProvider extends RegionalWordCardProvider{YzchzGameProvider(){super("yzchz",342,RegionScope.CITY,"hunan","yongzhou");}}
final class YcsdrGameProvider extends RegionalWordCardProvider{YcsdrGameProvider(){super("ycsdr",404,RegionScope.PROVINCE,"hubei","");}}
final class YchpGameProvider extends RegionalWordCardProvider{YchpGameProvider(){super("ychp",407,RegionScope.PROVINCE,"hubei","");}}
final class BzpGameProvider extends RegionalWordCardProvider{BzpGameProvider(){super("bzp",462,RegionScope.PROVINCE,"guizhou","");}}
final class LczpGameProvider extends RegionalWordCardProvider{LczpGameProvider(){super("lczp",490,RegionScope.PROVINCE,"hunan","");}}
final class PxphzGameProvider extends RegionalWordCardProvider{PxphzGameProvider(){super("pxphz",596,RegionScope.PROVINCE,"jiangxi","");}}
