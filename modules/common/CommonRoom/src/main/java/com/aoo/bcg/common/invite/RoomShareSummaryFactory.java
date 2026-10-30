package com.aoo.bcg.common.invite;
import com.aoo.bcg.common.config.RoomRuleSnapshot;import com.aoo.bcg.common.mapping.FieldConservation;import java.util.Map;import java.util.Set;
/** Share text may only be projected from the immutable rule snapshot locked to the room. */
public final class RoomShareSummaryFactory{
public static final Set<String> OUTPUT_FIELDS=Set.of("gameId","playVersion","regionName","playerCount","roundCount","ruleDescription");
public static final Set<String> SOURCE_RULE_FIELDS=Set.of("regionName","playerCount","roundCount","ruleDescription");
public static final Set<String> SENSITIVE_RULE_FIELDS=Set.of("randomSeed","wall","hands","accessToken","reconnectToken","deviceFingerprint");
static{FieldConservation.requireExactRecordFields(RoomShareSummary.class,OUTPUT_FIELDS);FieldConservation.requireNoSensitiveFields(SOURCE_RULE_FIELDS,SENSITIVE_RULE_FIELDS);}
public RoomShareSummary from(RoomRuleSnapshot snapshot){Map<String,Object>rules=snapshot.immutableRules();return new RoomShareSummary(snapshot.gameId(),snapshot.playVersion(),requiredText(rules,"regionName"),requiredInt(rules,"playerCount"),requiredInt(rules,"roundCount"),requiredText(rules,"ruleDescription"));}
private static String requiredText(Map<String,Object>rules,String key){Object value=rules.get(key);if(value==null||String.valueOf(value).isBlank())throw new IllegalStateException("locked rule missing "+key);return String.valueOf(value);}
private static int requiredInt(Map<String,Object>rules,String key){Object value=rules.get(key);if(!(value instanceof Number number))throw new IllegalStateException("locked rule missing "+key);return number.intValue();}}
