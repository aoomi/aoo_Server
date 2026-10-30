package com.aoo.bcg.lobby;

import com.aoo.bcg.hall.http.HallError;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.*;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;
import javax.sql.DataSource;

/** Offline real repository guard regression; no database, service or issuance. */
public final class JdbcLobbyRepositoryUiPublicationHashContractTest {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final String H="0".repeat(64);
    private static int passed;
    public static void main(String[] args)throws Exception{
        Map<?,?> bundle=JSON.readValue(Files.readString(Path.of(args[0])),Map.class);
        DataSource ds=(DataSource)Proxy.newProxyInstance(DataSource.class.getClassLoader(),new Class<?>[]{DataSource.class},(p,m,a)->{throw new AssertionError("DATABASE_FORBIDDEN");});
        LobbyMembershipAuthorityPort authority=(LobbyMembershipAuthorityPort)Proxy.newProxyInstance(LobbyMembershipAuthorityPort.class.getClassLoader(),new Class<?>[]{LobbyMembershipAuthorityPort.class},(p,m,a)->{throw new AssertionError("AUTHORITY_FORBIDDEN");});
        JdbcLobbyRepository repo=new JdbcLobbyRepository(ds,JSON,Clock.systemUTC(),authority);
        for(Object raw:(List<?>)bundle.get("targets")){
            Map<?,?> target=(Map<?,?>)raw,old=(Map<?,?>)target.get("old"),pub=(Map<?,?>)target.get("publication");
            long game=((Number)old.get("gameId")).longValue();String code=(String)old.get("gameCode"),play=(String)old.get("playVersion"),source=(String)old.get("sourceHash");
            String ui=JSON.writeValueAsString(pub.get("uiSchema"));
            String hash=game==2001?"7872ef4c46334ecf3347d8087ddcb270a3a0476b2c0d9223e093a5e9f1e2c2be":"81931ea0c80e08343e8bad23cd5b2a34b66c4c7e1680ddc79124b24e42dee23b";
            if(hash.equals(source))throw new AssertionError("POSITIVE_MUST_DISTINGUISH_HASHES");
            Map<String,Object> index=new HashMap<>();
            index.put("gameCode",code);index.put("regionCode",old.get("regionCode"));index.put("sourceHash",source);
            index.put("uiSchema",ui);index.put("uiSnapshot",ui);index.put("uiHash",hash);index.put("componentChain","[]");
            index.put("ruleValidator",JSON.writeValueAsString(Map.of("gameId",game,"gameCode",code,"playVersion",play,"sourceHash",source)));
            index.put("catalogSnapshot",JSON.writeValueAsString(Map.of("gameId",game,"gameCode",code)));
            check(repo,game,play,index,false);
            mutate(repo,game,play,index,"uiHash",source);
            mutate(repo,game,play,index,"uiSnapshot",ui.replace("GameRoom2D","TamperedScene"));
            mutate(repo,game,play,index,"uiSchema",ui.replace("GameRoom2D","DifferentScene"));
            Map<String,Object> badSource=new LinkedHashMap<>((Map<String,Object>)pub.get("uiSchema"));badSource.put("roomRuleSourceHash",H);
            String badUI=JSON.writeValueAsString(badSource);
            Method canon=JdbcLobbyRepository.class.getDeclaredMethod("canonicalUiJson",Object.class);canon.setAccessible(true);
            Method sha=JdbcLobbyRepository.class.getDeclaredMethod("sha256",String.class);sha.setAccessible(true);
            Map<String,Object> altered=new HashMap<>(index);altered.put("uiSnapshot",badUI);altered.put("uiSchema",badUI);altered.put("uiHash",sha.invoke(null,canon.invoke(repo,badSource)));
            check(repo,game,play,altered,true);
        }
        System.out.println(passed+" real repository UI publication guard cases PASS");
    }
    private static void mutate(JdbcLobbyRepository r,long g,String p,Map<String,Object> base,String key,Object value)throws Exception{Map<String,Object> changed=new HashMap<>(base);changed.put(key,value);check(r,g,p,changed,true);}
    private static void check(JdbcLobbyRepository repo,long game,String play,Map<String,Object> values,boolean reject)throws Exception{
        Class<?> type=Class.forName("com.aoo.bcg.lobby.JdbcLobbyRepository$Index");RecordComponent[] fields=type.getRecordComponents();Object[] args=new Object[fields.length];Class<?>[] types=new Class<?>[fields.length];
        for(int i=0;i<fields.length;i++){String name=fields[i].getName();types[i]=fields[i].getType();args[i]=values.containsKey(name)?values.get(name):types[i]==long.class?1L:name.endsWith("Version")?"v1":H;}
        Constructor<?> ctor=type.getDeclaredConstructor(types);ctor.setAccessible(true);Object index=ctor.newInstance(args);
        Method method=JdbcLobbyRepository.class.getDeclaredMethod("requireCanonicalIndex",long.class,String.class,type);method.setAccessible(true);
        try{method.invoke(repo,game,play,index);if(reject)throw new AssertionError("CORRUPTION_ACCEPTED");}
        catch(InvocationTargetException error){if(!reject||!(error.getCause() instanceof HallError h)||!h.code().equals("HALL_RULE_SCHEMA_UNAVAILABLE"))throw error;}
        passed++;
    }
}
