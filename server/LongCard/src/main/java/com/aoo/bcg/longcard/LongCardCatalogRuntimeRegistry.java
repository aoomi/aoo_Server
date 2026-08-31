package com.aoo.bcg.longcard;

import com.aoo.bcg.gamespi.*;
import java.util.*;

/** Expands long-card catalog configuration rows onto one family Provider class. */
public final class LongCardCatalogRuntimeRegistry {
    private LongCardCatalogRuntimeRegistry() {}
    public static Optional<GameProvider> providerFor(GameDescriptor descriptor) {
        if (descriptor.category() != GameCategory.LONG_CARD) return Optional.empty();
        GameProvider regional = switch (descriptor.code()) {
            case "aydss" -> new AydssGameProvider();
            case "aycp" -> new AycpGameProvider();
            case "zgcp" -> new ZgcpGameProvider();
            case "zgdss" -> new ZgdssGameProvider();
            default -> null;
        };
        return regional == null ? Optional.empty() : Optional.of(new LongCardFamilyProvider(descriptor, regional));
    }
}

/** Compatibility names now share one provider implementation; only region sessions/rules differ. */
class RegionalLongCardProvider implements GameProvider {
    static final String VERSION="1.0.0";
    private final String code; private final int gameId;
    RegionalLongCardProvider(String code,int gameId){this.code=code;this.gameId=gameId;}
    public GameDescriptor descriptor(){return new GameDescriptor(gameId,code,code.toUpperCase(),GameCategory.LONG_CARD,"long-card-regional",RegionScope.PROVINCE,"sichuan","",VERSION);}
    public GameRoomFactory roomFactory(){return c->new GameRoomHandle(c.roomId(),gameId,VERSION,switch(code){case"aydss"->new AydssAuthoritativeSession(c.roomId(),c.ownerId(),c.immutableRules());case"aycp"->new AycpAuthoritativeSession(c.roomId(),c.ownerId(),c.immutableRules());case"zgcp"->new ZgcpAuthoritativeSession(c.roomId(),c.ownerId(),c.immutableRules());case"zgdss"->new ZgdssAuthoritativeSession(c.roomId(),c.ownerId(),c.immutableRules());default->throw new IllegalStateException(code);});}
    public Optional<GameCommandHandler>commandHandler(){return Optional.of(new AuthoritativeSessionCommandHandler());}
    public Optional<ReconnectViewProvider<?>>reconnectViewProvider(){return Optional.of((p,r)->r.requireAuthoritativeSession().viewFor(p));}
    public Optional<SettlementProvider>settlementProvider(){return Optional.of((r,n)->r.requireAuthoritativeSession().settlement(n,VERSION));}
    public Optional<AuthoritativeGameSession>restoreAuthoritativeSession(Map<String,Object>s){return Optional.of(switch(code){case"aydss"->AydssAuthoritativeSession.restore(s);case"aycp"->AycpAuthoritativeSession.restore(s);case"zgcp"->ZgcpAuthoritativeSession.restore(s);case"zgdss"->ZgdssAuthoritativeSession.restore(s);default->throw new IllegalStateException(code);});}
    public Optional<EventReplayProvider>eventReplayProvider(){return Optional.of((base,events)->switch(code){case"aydss"->AydssAuthoritativeSession.replay(base,events);case"aycp"->AycpAuthoritativeSession.replay(base,events);case"zgcp"->ZgcpAuthoritativeSession.replay(base,events);case"zgdss"->ZgdssAuthoritativeSession.replay(base,events);default->throw new IllegalStateException(code);});}
    public List<RuleComponent<GameCommandRequest>>ruleComponents(){return List.of(new RuleComponent<>(){public String ruleId(){return"long-card.family."+code;}public String componentVersion(){return VERSION;}public RuleStage stage(){return RuleStage.PLAY;}public int priority(){return 100;}public RuleResult execute(GameCommandRequest r){return("longcard."+code+".dispatch").equals(r.msgId())?RuleResult.accept():RuleResult.reject("FAMILY_MESSAGE_REQUIRED","wrong long-card family message");}});}
    public Map<String,Object>defaultConfiguration(){Map<String,Object>out=new LinkedHashMap<>();out.put("enabled",true);out.put("playVersion",VERSION);out.put("provider","long-card-family-runtime");if(code.startsWith("zg")){out.put("fanshushangxian",3);out.put("chaofanjiadi",0);}if(code.equals("zgcp"))out.put("minimumHuPoints",14);if(code.equals("zgdss"))out.put("laizishuliang",0);return Map.copyOf(out);}
}
final class AydssGameProvider extends RegionalLongCardProvider{static final int GAME_ID=80;static final String VERSION=RegionalLongCardProvider.VERSION;AydssGameProvider(){super("aydss",GAME_ID);}}
final class AycpGameProvider extends RegionalLongCardProvider{static final int GAME_ID=138;static final String VERSION=RegionalLongCardProvider.VERSION;AycpGameProvider(){super("aycp",GAME_ID);}}
final class ZgcpGameProvider extends RegionalLongCardProvider{static final int GAME_ID=210;static final String VERSION=RegionalLongCardProvider.VERSION;ZgcpGameProvider(){super("zgcp",GAME_ID);}}
final class ZgdssGameProvider extends RegionalLongCardProvider{static final int GAME_ID=211;static final String VERSION=RegionalLongCardProvider.VERSION;ZgdssGameProvider(){super("zgdss",GAME_ID);}}
