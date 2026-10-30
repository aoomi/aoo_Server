package com.aoo.bcg.billing;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.h2.jdbcx.JdbcDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.core.JsonGenerator;
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

class JdbcRetiredPaymentHistoryTest {
    static final Clock CLOCK=Clock.fixed(Instant.parse("2026-10-17T03:00:00Z"),ZoneOffset.UTC);
    static final String SECRET="01234567890123456789012345678901";
    @Test void existingGetPreservesExpiredCreatedAndTerminalRetiredOrders()throws Exception {
        var ds=fixture();var routes=routes(ds);var repo=new JdbcPaymentOrderRepository(ds);
        for(String code:List.of("CRYSTAL","SPORTS_POINT"))for(String state:List.of("CREATED","PAID","DELIVERED","REFUND_PENDING","REFUNDED","CLOSED","REVIEW")){
            String id=code+"-"+state;seed(ds,id,code,state,code.equals("SPORTS_POINT")?12:0);String before=row(ds,id);
            var ex=new Exchange("GET",id,7);routes.handle(ex);assertEquals(200,ex.status,ex.output.toString());var data=new ObjectMapper().readTree(ex.output.toByteArray()).get("data");
            assertEquals(code,data.get("assetCurrency").asText());assertEquals(state,data.get("state").asText());assertTrue(data.get("retiredAsset").asBoolean());assertEquals(100,data.get("assetUnits").asLong());assertEquals(1000,data.get("amountMinor").asLong());assertFalse(data.has("buyerId"));assertFalse(data.has("checkoutUrl"));assertEquals(before,row(ds,id));assertThrows(IllegalArgumentException.class,()->repo.find(id));
        }
    }
    @Test void nonOwnerAndCancelCannotMutateRetiredHistory()throws Exception {
        var ds=fixture();seed(ds,"old","CRYSTAL","CREATED",0);var routes=routes(ds);String before=row(ds,"old");
        var other=new Exchange("GET","old",8);routes.handle(other);assertEquals(401,other.status);assertEquals(before,row(ds,"old"));
        var cancel=new Exchange("POST","old/cancel",7);routes.handle(cancel);assertNotEquals(200,cancel.status);assertEquals(before,row(ds,"old"));
        assertTrue(new JdbcPaymentOrderRepository(ds).findRetiredHistory("missing").isEmpty());
    }
    @Test void activeRoomCardAndGoldRetainOriginalTimeoutBehavior()throws Exception {
        var ds=fixture();var routes=routes(ds);
        for(String code:List.of("ROOM_CARD","GOLD")) {seed(ds,code,code,"CREATED",0);var ex=new Exchange("GET",code,7);routes.handle(ex);assertEquals(200,ex.status,ex.output.toString());var d=new ObjectMapper().readTree(ex.output.toByteArray()).get("data");assertEquals("CLOSED",d.get("state").asText());assertEquals("PAYMENT_TIMEOUT",d.get("failureCode").asText());assertFalse(d.has("retiredAsset"));assertTrue(new JdbcPaymentOrderRepository(ds).findRetiredHistory(code).isEmpty());assertEquals(2,new JdbcPaymentOrderRepository(ds).find(code).orElseThrow().version());}
    }
    static JdbcDataSource fixture()throws Exception {var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");try(var c=ds.getConnection();var s=c.createStatement()){s.execute("CREATE TABLE aoo_payment_order(order_id VARCHAR(128) PRIMARY KEY,buyer_player_id BIGINT,asset_currency VARCHAR(32),asset_scope_id BIGINT,product_code VARCHAR(64),product_version BIGINT,asset_units BIGINT,fiat_currency VARCHAR(16),amount_minor BIGINT,channel_code VARCHAR(32),product_locked_at TIMESTAMP,product_fingerprint VARCHAR(64),state VARCHAR(32),provider_transaction_id VARCHAR(128),callback_digests VARCHAR(2000),failure_code VARCHAR(128),version BIGINT,created_at TIMESTAMP,paid_at TIMESTAMP,delivered_at TIMESTAMP,refunded_at TIMESTAMP)");}return ds;}
    static void seed(JdbcDataSource ds,String id,String code,String state,long scope)throws Exception {try(var c=ds.getConnection();var s=c.prepareStatement("INSERT INTO aoo_payment_order VALUES(?,7,?,?, 'PRODUCT',1,100,'CNY',1000,'DEV',?,? ,?,NULL,'',NULL,1,?,?,?,?)")){var t=java.sql.Timestamp.from(CLOCK.instant().minusSeconds(2000));s.setString(1,id);s.setString(2,code);s.setLong(3,scope);s.setTimestamp(4,t);s.setString(5,List.of("ROOM_CARD","GOLD").contains(code)?PaymentProductSnapshot.lock("PRODUCT",1,code,100,"CNY",1000,"DEV",t.toInstant()).fingerprint():"a".repeat(64));s.setString(6,state);s.setTimestamp(7,t);s.setTimestamp(8,state.equals("CREATED")||state.equals("CLOSED")||state.equals("REVIEW")?null:t);s.setTimestamp(9,List.of("DELIVERED","REFUND_PENDING","REFUNDED").contains(state)?t:null);s.setTimestamp(10,state.equals("REFUNDED")?t:null);s.executeUpdate();}}
    static String row(JdbcDataSource ds,String id)throws Exception {try(var c=ds.getConnection();var s=c.prepareStatement("SELECT * FROM aoo_payment_order WHERE order_id=?")){s.setString(1,id);try(var r=s.executeQuery()){assertTrue(r.next());StringBuilder out=new StringBuilder();for(int i=1;i<=21;i++)out.append(r.getString(i)).append('|');return out.toString();}}}
    static PlayerPaymentHttpRoutes routes(JdbcDataSource ds){var mapper=new ObjectMapper();var module=new SimpleModule();module.addSerializer(Instant.class,new JsonSerializer<Instant>(){public void serialize(Instant i,JsonGenerator g,SerializerProvider p)throws IOException{g.writeString(i.toString());}});mapper.registerModule(module);var processor=new PaymentOrderProcessor(new JdbcPaymentOrderRepository(ds),new InMemoryBillingService(new InMemoryLedgerRepository(),CLOCK,Map.of(),"ROOM_CARD"),new PaymentCallbackVerifier(SECRET.getBytes(StandardCharsets.UTF_8)),CLOCK);return new PlayerPaymentHttpRoutes(new JdbcBillingQueries(ds),processor,new HostedPaymentProviderAdapter(URI.create("https://example.test/pay"),"merchant",SECRET,CLOCK),mapper,SECRET,CLOCK);}
    static final class Exchange extends HttpExchange {
        final Headers request=new Headers(),response=new Headers();final ByteArrayOutputStream output=new ByteArrayOutputStream();final String method,path;int status;
        Exchange(String method,String suffix,long player)throws Exception {this.method=method;path="/v1/player/payments/"+suffix;request.set("X-API-Version","1");String p=player+"."+CLOCK.instant().plusSeconds(60).getEpochSecond();Mac m=Mac.getInstance("HmacSHA256");m.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));request.set("Authorization","Bearer "+p+"."+HexFormat.of().formatHex(m.doFinal(p.getBytes(StandardCharsets.UTF_8))));}
        public Headers getRequestHeaders(){return request;}public Headers getResponseHeaders(){return response;}public URI getRequestURI(){return URI.create(path);}public String getRequestMethod(){return method;}public HttpContext getHttpContext(){return null;}public void close(){}public InputStream getRequestBody(){return new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8));}public OutputStream getResponseBody(){return output;}public void sendResponseHeaders(int code,long length){status=code;}public InetSocketAddress getRemoteAddress(){return new InetSocketAddress(0);}public int getResponseCode(){return status;}public InetSocketAddress getLocalAddress(){return new InetSocketAddress(0);}public String getProtocol(){return "HTTP/1.1";}public Object getAttribute(String n){return null;}public void setAttribute(String n,Object v){}public void setStreams(InputStream i,OutputStream o){}public HttpPrincipal getPrincipal(){return null;}
    }
}
