package com.aoo.bcg.hall.room;

import com.aoo.bcg.hall.http.HallError;
import java.math.BigDecimal;
import java.util.*;

/** Validates room rules against the immutable UI schema locked by the active release. */
final class RoomRuleSchemaValidator {
    private RoomRuleSchemaValidator() {}

    static Map<String,Object> validate(Object schema,Object submitted) {
        if (!(submitted instanceof Map<?,?> input)) {
            throw HallError.bad("HALL_RULES_INVALID","rules must be an object");
        }
        Map<String,Map<String,Object>> fields=fields(schema);
        if(fields.isEmpty())throw new HallError(503,"HALL_RULE_SCHEMA_UNAVAILABLE","published room rule schema is unavailable");
        Map<String,Object> normalized=new LinkedHashMap<>();
        for(Object rawKey:input.keySet()){
            String key=String.valueOf(rawKey);
            if(!fields.containsKey(key))throw HallError.bad("HALL_RULE_UNKNOWN_FIELD","unsupported room rule: "+key);
        }
        for(Map.Entry<String,Map<String,Object>> entry:fields.entrySet()){
            String key=entry.getKey();Map<String,Object> field=entry.getValue();
            // 禁用字段代表当前发布版本没有开放该能力；客户端即使伪造请求也必须拒绝。
            if(Boolean.TRUE.equals(field.get("disabled"))){
                if(input.containsKey(key))throw HallError.bad("HALL_RULE_DISABLED","disabled room rule: "+key);
                continue;
            }
            Object value=input.containsKey(key)?input.get(key):field.get("defaultValue");
            boolean required=Boolean.TRUE.equals(field.get("required"));
            if(value==null){if(required)throw HallError.bad("HALL_RULE_REQUIRED",key+" is required");continue;}
            String control=String.valueOf(field.getOrDefault("control","SINGLE_SELECT")).toUpperCase(Locale.ROOT);
            List<?> options=field.get("options") instanceof List<?> list?list:List.of();
            if(control.contains("MULTI")||control.contains("CHECKBOX")){
                if(!(value instanceof Collection<?> values))throw HallError.bad("HALL_RULE_VALUE_INVALID","invalid value for "+key);
                for(Object item:values)allowed(key,item,options);
            }else{
                if(value instanceof Collection<?>)throw HallError.bad("HALL_RULE_VALUE_INVALID","invalid value for "+key);
                if(!options.isEmpty())allowed(key,value,options);
                else if(control.contains("STEPPER")||control.contains("NUMBER")||control.contains("COUNTER"))
                    numeric(key,value,field);
            }
            normalized.put(key,value);
        }
        constraints(schema, normalized);
        return Map.copyOf(normalized);
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
    private static boolean equal(Object left,Object right){
        if(left instanceof Number||right instanceof Number){try{return new BigDecimal(String.valueOf(left)).compareTo(new BigDecimal(String.valueOf(right)))==0;}catch(NumberFormatException ignored){return false;}}
        return Objects.equals(String.valueOf(left),String.valueOf(right));
    }
}
