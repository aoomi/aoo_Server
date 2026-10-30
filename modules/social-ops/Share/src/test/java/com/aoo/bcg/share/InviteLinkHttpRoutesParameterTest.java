package com.aoo.bcg.share;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InviteLinkHttpRoutesParameterTest {
 private final ObjectMapper json=new ObjectMapper();
 private Map<String,Object> body(String value)throws Exception{return json.readValue(value,new TypeReference<>(){});}
 private Object invoke(String helper,Map<String,Object> b)throws Throwable{Method m=helper.equals("number")?InviteLinkHttpRoutes.class.getDeclaredMethod(helper,Map.class,String.class):InviteLinkHttpRoutes.class.getDeclaredMethod(helper,Map.class);m.setAccessible(true);try{return helper.equals("number")?m.invoke(null,b,"targetId"):m.invoke(null,b);}catch(InvocationTargetException x){throw x.getCause();}}
 private void rejected(String helper,String input)throws Exception{InviteLinkError e=assertThrows(InviteLinkError.class,()->invoke(helper,body(input)));assertEquals(400,e.status());assertEquals("INVITE_INVALID_REQUEST",e.code());}
 @Test void jsonExactIntegersPreserveMeaning()throws Throwable{assertEquals(7L,invoke("number",body("{\"targetId\":7}")));assertEquals(7L,invoke("number",body("{\"targetId\":7.0}")));assertEquals(Long.MAX_VALUE,invoke("number",body("{\"targetId\":9223372036854775807}")));assertEquals(Long.MIN_VALUE,invoke("number",body("{\"targetId\":-9223372036854775808}")));}
 @Test void jsonFractionsOverflowAndWrongTypesAreRejected()throws Exception{for(String v:new String[]{"7.9","1e30","9223372036854775808","-9223372036854775809","null","\"7\""})rejected("number","{\"targetId\":"+v+"}");rejected("number","{}");}
 @Test void javaNonFiniteNumbersAreRejected()throws Exception{for(double n:new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}){InviteLinkError e=assertThrows(InviteLinkError.class,()->invoke("number",Map.of("targetId",n)));assertEquals(400,e.status());}}
 @Test void jsonMaxUsesPreservesRangeAndRejectsBeforeIntConversion()throws Throwable{assertEquals(1,invoke("maxUses",body("{\"maxUses\":1}")));assertEquals(10000,invoke("maxUses",body("{\"maxUses\":10000}")));for(String n:new String[]{"0","10001","2147483648","1e30","1.9","null"})rejected("maxUses","{\"maxUses\":"+n+"}");}
}
