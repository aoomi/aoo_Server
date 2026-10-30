package com.aoo.bcg.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

/** Versioned production HTTP surface for wallet, orders, callbacks and reconciliation. */
public final class BillingHttpRoutes {
    private final JdbcBillingQueries queries;private final BillingService billing;private final PaymentOrderProcessor payments;
    private final ObjectMapper json;private final byte[] internalToken;private final Clock clock;
    public BillingHttpRoutes(JdbcBillingQueries queries,BillingService billing,PaymentOrderProcessor payments,ObjectMapper json,String internalToken,Clock clock){
        this.queries=Objects.requireNonNull(queries);this.billing=Objects.requireNonNull(billing);this.payments=Objects.requireNonNull(payments);this.json=Objects.requireNonNull(json);this.clock=Objects.requireNonNull(clock);
        if(internalToken==null||internalToken.length()<32)throw new IllegalArgumentException("billing internal token must be at least 32 characters");this.internalToken=internalToken.getBytes(StandardCharsets.UTF_8);
    }
    public void mount(HttpServer server){
        server.createContext("/health/billing",ex->reply(ex,200,Map.of("status","UP","persistence","jdbc","apiVersion","1")));
        server.createContext("/v1/billing",this::billing);
        server.createContext("/v1/payments/callback",this::callback);
        server.createContext("/v1/payments",this::payments);
    }
    private void billing(HttpExchange ex)throws IOException{handle(ex,true,()->{Map<String,Object> f=body(ex);String action=text(f,"action");CurrencyAccount a=account(f);return switch(action){
        case "balance"->queries.balance(a);
        case "debit"->billing.debit(idempotency(ex),a,number(f,"amount"),text(f,"reasonCode"));
        case "credit","refund"->billing.credit(idempotency(ex),a,number(f,"amount"),text(f,"reasonCode"));
        case "ledger"->queries.ledger(a.playerId(),a.currency(),a.scopeId(),(int)numberOr(f,"limit",50));
        case "reconcile"->queries.reconcile(LocalDate.parse(text(f,"businessDate")),textOr(f,"entryStatus","POSTED"),(String)f.get("channelCode"));
        default->throw new IllegalArgumentException("unknown billing action");};});}
    private void payments(HttpExchange ex)throws IOException{handle(ex,true,()->{Map<String,Object> f=body(ex);String action=text(f,"action"),orderId=text(f,"orderId");return switch(action){
        case "create"->{String channel=text(f,"channelCode");var product=queries.product(text(f,"productCode"),channel,clock.instant());yield payments.create(orderId,new CurrencyAccount(number(f,"playerId"),product.assetCurrency(),numberOr(f,"scopeId",0)),product);}
        case "get"->queries.order(orderId).orElseThrow(()->new IllegalArgumentException("payment order not found"));
        case "deliver"->payments.deliver(orderId);
        case "refund"->payments.refund(orderId,idempotency(ex));
        default->throw new IllegalArgumentException("unknown payment action");};});}
    private void callback(HttpExchange ex)throws IOException{handle(ex,false,()->{Map<String,Object> f=body(ex);var callback=new PaymentOrderProcessor.Callback(text(f,"eventId"),text(f,"orderId"),text(f,"providerTransactionId"),text(f,"channelCode"),number(f,"amountMinor"),text(f,"fiatCurrency"),text(f,"productFingerprint"),Instant.parse(text(f,"paidAt")),text(f,"signature"));PaymentOrder paid=payments.callback(callback);return paid.state()==PaymentOrder.State.PAID?payments.deliver(paid.orderId()):paid;});}
    private void handle(HttpExchange ex,boolean auth,Action action)throws IOException{try{if(!"POST".equals(ex.getRequestMethod()))throw new Api(405,"METHOD_NOT_ALLOWED","POST required");if(!"1".equals(ex.getRequestHeaders().getFirst("X-API-Version")))throw new Api(426,"API_VERSION_REQUIRED","X-API-Version: 1 required");if(auth)authenticate(ex);reply(ex,200,Map.of("code","OK","data",action.run()));}catch(Api e){reply(ex,e.status,Map.of("code",e.code,"message",e.getMessage()));}catch(SecurityException e){reply(ex,401,Map.of("code","UNAUTHORIZED","message",e.getMessage()));}catch(IllegalArgumentException e){reply(ex,400,Map.of("code","INVALID_REQUEST","message",String.valueOf(e.getMessage())));}catch(IllegalStateException e){int status=String.valueOf(e.getMessage()).contains("insufficient")?409:503;reply(ex,status,Map.of("code",status==409?"INSUFFICIENT_BALANCE":"BILLING_UNAVAILABLE","message",String.valueOf(e.getMessage())));}catch(Exception e){reply(ex,500,Map.of("code","INTERNAL_ERROR","message","billing request failed"));}}
    private void authenticate(HttpExchange ex){String h=ex.getRequestHeaders().getFirst("Authorization");byte[] supplied=h!=null&&h.startsWith("Bearer ")?h.substring(7).getBytes(StandardCharsets.UTF_8):new byte[0];if(!MessageDigest.isEqual(internalToken,supplied))throw new SecurityException("invalid billing credential");}
    @SuppressWarnings("unchecked") private Map<String,Object> body(HttpExchange ex)throws IOException{return json.readValue(ex.getRequestBody(),Map.class);}
    private CurrencyAccount account(Map<String,Object> f){return new CurrencyAccount(number(f,"playerId"),text(f,"currency"),numberOr(f,"scopeId",0));}
    private String idempotency(HttpExchange ex){String value=ex.getRequestHeaders().getFirst("Idempotency-Key");if(value==null||!value.matches("[A-Za-z0-9_.:-]{1,128}"))throw new IllegalArgumentException("valid Idempotency-Key required");return value;}
    private static String text(Map<String,Object> f,String k){Object v=f.get(k);if(!(v instanceof String s)||s.isBlank())throw new IllegalArgumentException(k+" required");return s;}
    private static String textOr(Map<String,Object> f,String k,String d){Object v=f.get(k);return v==null?d:String.valueOf(v);}
    private static long number(Map<String,Object> f,String k){Object v=f.get(k);if(v instanceof Number n)return n.longValue();throw new IllegalArgumentException(k+" required");}
    private static long numberOr(Map<String,Object> f,String k,long d){Object v=f.get(k);return v==null?d:number(f,k);}
    private void reply(HttpExchange ex,int status,Object value)throws IOException{byte[] b=json.writeValueAsBytes(value);ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");ex.getResponseHeaders().set("Cache-Control","no-store");ex.sendResponseHeaders(status,b.length);try(var out=ex.getResponseBody()){out.write(b);}}
    @FunctionalInterface private interface Action{Object run() throws Exception;}private static final class Api extends RuntimeException{final int status;final String code;Api(int status,String code,String message){super(message);this.status=status;this.code=code;}}
}
