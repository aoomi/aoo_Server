package com.aoo.bcg.billing;

import com.aoo.bcg.common.persistence.DriverManagerDataSource;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="AOO_DB_IT_URL",matches=".+")
class JdbcCurrencyAuthorityIntegrationTest {
    @Test void catalogScopedCurrencyAndConcurrentDebitsAreAuthoritative() throws Exception {
        var dataSource=new DriverManagerDataSource(System.getenv("AOO_DB_IT_URL"),System.getenv("AOO_DB_IT_USER"),System.getenv("AOO_DB_IT_PASSWORD"));
        long player=7_000_000_000L+Math.abs(UUID.randomUUID().getLeastSignificantBits()%1_000_000_000L);
        var ids=new AtomicLong(System.currentTimeMillis()*1000);var billing=new JdbcBillingService(dataSource,ids::incrementAndGet,Clock.systemUTC());
        for(String currency:java.util.List.of("ROOM_CARD","GOLD","CRYSTAL"))assertEquals(100,billing.credit("seed:"+player+":"+currency,player,currency,100,"MIGRATION").balanceAfter());
        assertEquals(25,billing.credit("seed:"+player+":city",new CurrencyAccount(player,"CITY_ROOM_CARD",510100),25,"MIGRATION").balanceAfter());
        assertThrows(IllegalArgumentException.class,()->billing.credit("bad:"+player,new CurrencyAccount(player,"CITY_ROOM_CARD",0),1,"MIGRATION"));
        AtomicInteger succeeded=new AtomicInteger();
        try(var executor=Executors.newFixedThreadPool(16)){
            var tasks=java.util.stream.IntStream.range(0,200).<java.util.concurrent.Callable<Void>>mapToObj(i -> () ->{try{billing.debit("spend:"+player+":"+i,player,"GOLD",1,"GAME");succeeded.incrementAndGet();}catch(IllegalStateException insufficient){assertTrue(insufficient.getMessage().contains("insufficient"));}return null;}).toList();
            for(var future:executor.invokeAll(tasks))future.get();
        }
        assertEquals(100,succeeded.get());
        try(var connection=dataSource.getConnection();var statement=connection.prepareStatement("SELECT balance,version FROM aoo_currency_balance WHERE player_id=? AND currency='GOLD' AND currency_scope_id=0")){statement.setLong(1,player);try(var row=statement.executeQuery()){assertTrue(row.next());assertEquals(0,row.getLong(1));assertEquals(101,row.getLong(2));}}
    }
}
