package com.aoo.bcg.lobby;

import com.aoo.bcg.hall.http.HallError;
import java.math.BigDecimal;
import java.util.*;

/** Validates room rules against the immutable UI schema locked by the active release. */
public final class LobbyRoomRuleSchemaValidator {
    private static final String BASE_SCORE = "baseScore";
    private static final String ROOM_END_SELECTION = "roomEndSelection";
    private static final String ROOM_DURATION_MINUTES = "roomDurationMinutes";
    private static final String ROUND_COUNT = "roundCount";
    private LobbyRoomRuleSchemaValidator() {}

    public static Map<String,Object> validate(Object schema,Object submitted) {
        return validate(schema,submitted,null,null);
    }

    public static Map<String,Object> validate(Object schema,Object submitted,long expectedGameId,String expectedPlayVersion) {
        return validate(schema,submitted,Long.valueOf(expectedGameId),expectedPlayVersion);
    }

    private static Map<String,Object> validate(Object schema,Object submitted,Long expectedGameId,String expectedPlayVersion) {
        if (!(submitted instanceof Map<?,?> input)) {
            throw HallError.bad("HALL_RULES_INVALID","rules must be an object");
        }
        Map<String,Map<String,Object>> fields=fields(schema);
        if(fields.isEmpty())throw new HallError(503,"HALL_RULE_SCHEMA_UNAVAILABLE","published room rule schema is unavailable");
        Map<String,Object> identityInput=normalizeLegacyFieldAliases(schema,fields,input,expectedGameId,expectedPlayVersion);
        Map<String,Object> validationInput=projectRoomEndSelection(fields,identityInput);
        Map<String,Object> normalized=new LinkedHashMap<>();
        for(Object rawKey:validationInput.keySet()){
            String key=String.valueOf(rawKey);
            if(BASE_SCORE.equals(key))continue;
            if(!fields.containsKey(key))throw HallError.bad("HALL_RULE_UNKNOWN_FIELD","unsupported room rule: "+key);
        }
        for(Map.Entry<String,Map<String,Object>> entry:fields.entrySet()){
            String key=entry.getKey();Map<String,Object> field=entry.getValue();
            // 禁用字段代表当前发布版本没有开放该能力；客户端即使伪造请求也必须拒绝。
            if(Boolean.TRUE.equals(field.get("disabled"))){
                if(validationInput.containsKey(key))throw HallError.bad("HALL_RULE_DISABLED","disabled room rule: "+key);
                Object defaultValue=field.get("defaultValue");
                if(defaultValue!=null)normalized.put(key,defaultValue);
                continue;
            }
            Object value=validationInput.containsKey(key)?validationInput.get(key):field.get("defaultValue");
            String control=String.valueOf(field.getOrDefault("control","SINGLE_SELECT")).toUpperCase(Locale.ROOT);
            boolean multi=control.contains("MULTI")||control.contains("CHECKBOX");
            // 选择基数由控件类型决定，不能由陈旧发布数据放宽：radio 恰好一个，checkbox 可为空。
            boolean required=!multi;
            if(value==null){if(required)throw HallError.bad("HALL_RULE_REQUIRED",key+" is required");continue;}
            List<?> options=field.get("options") instanceof List<?> list?list:List.of();
            if(multi){
                if(booleanFlag(options)&&value instanceof Boolean){/* checked=true, unchecked=false */}
                else{
                    if(!(value instanceof Collection<?> values))throw HallError.bad("HALL_RULE_VALUE_INVALID","invalid value for "+key);
                    for(Object item:values)allowed(key,item,options);
                }
            }else{
                if(value instanceof Collection<?>)throw HallError.bad("HALL_RULE_VALUE_INVALID","invalid value for "+key);
                if(!options.isEmpty())allowed(key,value,options);
                else if(control.contains("STEPPER")||control.contains("NUMBER")||control.contains("COUNTER"))
                    numeric(key,value,field);
            }
            normalized.put(key,value);
        }
        // 底分是所有玩法共用的房间协议字段，不属于地区规则表；缺省值固定为 1。
        Object baseScore=validationInput.containsKey(BASE_SCORE)?validationInput.get(BASE_SCORE):1;
        positiveInteger(BASE_SCORE,baseScore);
        normalized.put(BASE_SCORE,baseScore);
        constraints(schema, normalized);
        return Map.copyOf(unprojectRoomEndSelection(normalized));
    }

    private static Map<String,Object> normalizeLegacyFieldAliases(
            Object schema,Map<String,Map<String,Object>> fields,Map<?,?> input,
            Long expectedGameId,String expectedPlayVersion){
        if(!(schema instanceof Map<?,?> root))return copyInput(input);
        Object rawAliases=root.get("legacyFieldAliases");
        if(rawAliases==null)return copyInput(input);
        if(!(rawAliases instanceof Map<?,?> aliases))
            throw new HallError(503,"HALL_RULE_SCHEMA_INVALID","legacyFieldAliases must be an object");
        long schemaGameId=identityGameId(root.get("gameId"));
        String schemaGameCode=identityText(root.get("gameCode"),"gameCode");
        String schemaPlayVersion=identityText(root.get("playVersion"),"playVersion");
        if(expectedGameId!=null&&(schemaGameId!=expectedGameId||!schemaPlayVersion.equals(expectedPlayVersion)))
            throw new HallError(503,"HALL_RULE_SCHEMA_INVALID","legacy field aliases do not match the room identity");
        Map<String,Object> normalized=copyInput(input);
        for(Map.Entry<?,?> entry:aliases.entrySet()){
            String alias=String.valueOf(entry.getKey());
            if(!(entry.getValue() instanceof Map<?,?> descriptor))
                throw new HallError(503,"HALL_RULE_SCHEMA_INVALID","legacy field alias descriptor must be an object");
            long aliasGameId=identityGameId(descriptor.get("gameId"));
            String aliasGameCode=identityText(descriptor.get("gameCode"),"legacy alias gameCode");
            String aliasPlayVersion=identityText(descriptor.get("playVersion"),"legacy alias playVersion");
            String canonicalKey=identityText(descriptor.get("canonicalKey"),"legacy alias canonicalKey");
            if(alias.equals(canonicalKey)||aliasGameId!=schemaGameId||!aliasGameCode.equals(schemaGameCode)
                    ||!aliasPlayVersion.equals(schemaPlayVersion)||!fields.containsKey(canonicalKey))
                throw new HallError(503,"HALL_RULE_SCHEMA_INVALID","legacy field alias identity or target is invalid");
            if(expectedGameId!=null&&(aliasGameId!=expectedGameId||!aliasPlayVersion.equals(expectedPlayVersion)))
                throw new HallError(503,"HALL_RULE_SCHEMA_INVALID","legacy field alias does not match the room identity");
            if(!normalized.containsKey(alias))continue;
            Object oldValue=normalized.remove(alias);
            if(normalized.containsKey(canonicalKey)&&!Objects.equals(normalized.get(canonicalKey),oldValue))
                throw HallError.bad("HALL_RULE_ALIAS_CONFLICT","conflicting legacy and canonical room rules: "+canonicalKey);
            normalized.putIfAbsent(canonicalKey,oldValue);
        }
        return normalized;
    }

    private static Map<String,Object> copyInput(Map<?,?> input){
        Map<String,Object> copy=new LinkedHashMap<>();input.forEach((key,value)->copy.put(String.valueOf(key),value));return copy;
    }

    private static long identityGameId(Object value){
        try{long id=Long.parseLong(String.valueOf(value));if(id<1)throw new NumberFormatException();return id;}
        catch(NumberFormatException error){throw new HallError(503,"HALL_RULE_SCHEMA_INVALID","legacy field alias gameId is invalid");}
    }

    private static String identityText(Object value,String name){
        String text=value==null?"":String.valueOf(value).trim();
        if(text.isEmpty()||"null".equals(text))throw new HallError(503,"HALL_RULE_SCHEMA_INVALID",name+" is required");
        return text;
    }

    private static Map<String,Object> projectRoomEndSelection(Map<String,Map<String,Object>> fields,Map<?,?> input){
        Map<String,Object> projected=new LinkedHashMap<>();
        input.forEach((key,value)->projected.put(String.valueOf(key),value));
        if(!fields.containsKey(ROOM_END_SELECTION))return projected;
        if(projected.containsKey(ROOM_END_SELECTION))
            throw HallError.bad("HALL_RULE_UNKNOWN_FIELD","unsupported room rule: "+ROOM_END_SELECTION);
        boolean duration=projected.containsKey(ROOM_DURATION_MINUTES),rounds=projected.containsKey(ROUND_COUNT);
        if(duration==rounds)
            throw HallError.bad("HALL_RULE_END_DIMENSION_INVALID","exactly one room end dimension is required");
        String kind=duration?"DURATION_MINUTES":"ROUND_COUNT";
        String key=duration?ROOM_DURATION_MINUTES:ROUND_COUNT;
        Object value=projected.remove(key);
        projected.put(ROOM_END_SELECTION,kind+":"+canonicalPositiveInteger(key,value));
        return projected;
    }

    private static Map<String,Object> unprojectRoomEndSelection(Map<String,Object> normalized){
        if(!normalized.containsKey(ROOM_END_SELECTION))return normalized;
        Map<String,Object> out=new LinkedHashMap<>(normalized);
        String selection=String.valueOf(out.remove(ROOM_END_SELECTION));
        String[] parts=selection.split(":",-1);
        if(parts.length!=2)throw new HallError(503,"HALL_RULE_SCHEMA_INVALID","invalid room end selection");
        String key=switch(parts[0]){
            case "DURATION_MINUTES" -> ROOM_DURATION_MINUTES;
            case "ROUND_COUNT" -> ROUND_COUNT;
            default -> throw new HallError(503,"HALL_RULE_SCHEMA_INVALID","unknown room end dimension");
        };
        out.put(key,canonicalPositiveInteger(key,parts[1]));
        return out;
    }

    private static int canonicalPositiveInteger(String key,Object value){
        try{
            BigDecimal number=new BigDecimal(String.valueOf(value));
            if(number.stripTrailingZeros().scale()>0||number.compareTo(BigDecimal.ONE)<0||number.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE))>0)
                throw HallError.bad("HALL_RULE_VALUE_INVALID","invalid value for "+key);
            return number.intValueExact();
        }catch(NumberFormatException|ArithmeticException error){
            throw HallError.bad("HALL_RULE_VALUE_INVALID","invalid value for "+key);
        }
    }

    private static void constraints(Object schema,Map<String,Object> values){
        if(!(schema instanceof Map<?,?> root)||root.get("constraints")==null)return;
        if(!(root.get("constraints") instanceof List<?> constraints))
            throw new HallError(503,"HALL_RULE_SCHEMA_INVALID","constraints must be an array");
        for(Object raw:constraints){
            if(!(raw instanceof Map<?,?> constraint))
                throw new HallError(503,"HALL_RULE_SCHEMA_INVALID","constraint must be an object");
            String type=String.valueOf(constraint.get("type")).toUpperCase(Locale.ROOT);
            String field=constraintText(constraint,"field"),other=constraintText(constraint,"otherField");
            List<?> when=constraintList(constraint,"whenValues"),allowed=constraintList(constraint,"allowedValues");
            if(!matches(values.get(field),when))continue;
            if("REQUIRES".equals(type)){
                if(!values.containsKey(other)||!matches(values.get(other),allowed))
                    throw HallError.bad("HALL_RULE_DEPENDENCY_INVALID",field+" requires "+other);
            }else if("CONFLICTS".equals(type)){
                if(values.containsKey(other)&&matches(values.get(other),allowed))
                    throw HallError.bad("HALL_RULE_CONFLICT",field+" conflicts with "+other);
            }else throw new HallError(503,"HALL_RULE_SCHEMA_INVALID","unknown constraint type: "+type);
        }
    }

    private static String constraintText(Map<?,?> constraint,String key){Object value=constraint.get(key);String text=value==null?"":String.valueOf(value).trim();if(text.isEmpty())throw new HallError(503,"HALL_RULE_SCHEMA_INVALID",key+" is required");return text;}
    private static List<?> constraintList(Map<?,?> constraint,String key){Object value=constraint.get(key);if(!(value instanceof List<?> list)||list.isEmpty())throw new HallError(503,"HALL_RULE_SCHEMA_INVALID",key+" must be a non-empty array");return list;}
    private static boolean matches(Object value,List<?> allowed){if(value instanceof Collection<?> values)return values.stream().anyMatch(item->allowed.stream().anyMatch(option->equal(item,option)));return allowed.stream().anyMatch(option->equal(value,option));}

    private static Map<String,Map<String,Object>> fields(Object schema){
        if(!(schema instanceof Map<?,?> root)||!(root.get("fields") instanceof List<?> list))return Map.of();
        Map<String,Map<String,Object>> out=new LinkedHashMap<>();
        for(Object raw:list){if(!(raw instanceof Map<?,?> source))continue;Object rawKey=source.containsKey("key")?source.get("key"):source.get("fieldId");String key=String.valueOf(rawKey);if(key.isBlank()||"null".equals(key))continue;Map<String,Object> field=new LinkedHashMap<>();source.forEach((k,v)->field.put(String.valueOf(k),v));out.put(key,field);}
        return out;
    }

    private static void allowed(String key,Object value,List<?> options){
        for(Object raw:options){if(raw instanceof Map<?,?> map&&Boolean.TRUE.equals(map.get("disabled")))continue;Object option=raw instanceof Map<?,?> map?(map.containsKey("value")?map.get("value"):map.get("id")):raw;if(equal(option,value))return;}
        throw HallError.bad("HALL_RULE_VALUE_INVALID","invalid value for "+key);
    }
    private static boolean booleanFlag(List<?> options){
        if(options.size()!=1)return false;
        Object raw=options.get(0);
        Object option=raw instanceof Map<?,?> map?(map.containsKey("value")?map.get("value"):map.get("id")):raw;
        return option instanceof Boolean;
    }
    private static void numeric(String key,Object value,Map<String,Object> field){
        try{
            BigDecimal number=new BigDecimal(String.valueOf(value));
            BigDecimal min=new BigDecimal(String.valueOf(field.getOrDefault("min",0)));
            BigDecimal max=new BigDecimal(String.valueOf(field.getOrDefault("max",Long.MAX_VALUE)));
            BigDecimal step=new BigDecimal(String.valueOf(field.getOrDefault("step",1)));
            if(step.signum()<=0||number.compareTo(min)<0||number.compareTo(max)>0
                    || number.subtract(min).remainder(step).compareTo(BigDecimal.ZERO)!=0)
                throw HallError.bad("HALL_RULE_VALUE_INVALID","invalid value for "+key);
        }catch(NumberFormatException|ArithmeticException error){
            throw HallError.bad("HALL_RULE_VALUE_INVALID","invalid value for "+key);
        }
    }
    private static void positiveInteger(String key,Object value){
        try{
            BigDecimal number=new BigDecimal(String.valueOf(value));
            if(number.stripTrailingZeros().scale()>0||number.compareTo(BigDecimal.ONE)<0||number.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE))>0)
                throw HallError.bad("HALL_RULE_VALUE_INVALID","invalid value for "+key);
        }catch(NumberFormatException error){
            throw HallError.bad("HALL_RULE_VALUE_INVALID","invalid value for "+key);
        }
    }
    private static boolean equal(Object left,Object right){
        if(left instanceof Number||right instanceof Number){try{return new BigDecimal(String.valueOf(left)).compareTo(new BigDecimal(String.valueOf(right)))==0;}catch(NumberFormatException ignored){return false;}}
        return Objects.equals(String.valueOf(left),String.valueOf(right));
    }
}
