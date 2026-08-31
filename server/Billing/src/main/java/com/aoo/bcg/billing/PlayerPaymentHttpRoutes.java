package com.aoo.bcg.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Sole player-facing order surface. The client can observe authority state but can never mark an order paid. */
public final class PlayerPaymentHttpRoutes {
    private final JdbcBillingQueries queries;private final PaymentOrderProcessor payments;private final HostedPaymentProviderAdapter provider;private final ObjectMapper json;private final PlayerAuth auth;private final Clock clock;
    public PlayerPaymentHttpRoutes(JdbcBillingQueries queries,PaymentOrderProcessor payments,HostedPaymentProviderAdapter provider,ObjectMapper json,String accountAuthSecret,Clock clock){this.queries=queries;this.payments=payments;this.provider=provider;this.json=json;this.auth=new PlayerAuth(accountAuthSecret,clock);this.clock=clock;}
    public void mount(HttpServer server){server.createContext("/v1/player/payments",this::handle);}
    private void handle(HttpExchange ex)throws IOException{try{
        if(!"1".equals(ex.getRequestHeaders().getFirst("X-API-Version")))throw new Api(426,"API_VERSION_REQUIRED","X-API-Version: 1 required");
        long playerId=auth.authenticate(ex.getRequestHeaders().getFirst("Authorization"));String path=ex.getRequestURI().getPath();
        if("/v1/player/payments".equals(path)&&"POST".equals(ex.getRequestMethod())){Map<String,Object> body=body(ex);String orderId=text(body,"orderId"),productCode=text(body,"productCode"),channelCode=text(body,"channelCode");var product=queries.product(productCode,channelCode,clock.instant());var order=payments.create(orderId,new CurrencyAccount(playerId,product.assetCurrency(),numberOr(body,"scopeId",0)),product);reply(ex,200,Map.of("code","OK","data",view(order,provider.checkout(order))));return;}
        if(path.matches("/v1/player/payments/[A-Za-z0-9_.:-]+")&&"GET".equals(ex.getRequestMethod())){PaymentOrder order=owned(path.substring(path.lastIndexOf('/')+1),playerId);if(order.state()==PaymentOrder.State.CREATED&&order.createdAt().plusSeconds(900).isBefore(clock.instant()))order=payments.cancel(order.orderId(),playerId,"PAYMENT_TIMEOUT");reply(ex,200,Map.of("code","OK","data",view(order,null)));return;}
        if(path.matches("/v1/player/payments/[A-Za-z0-9_.:-]+/cancel")&&"POST".equals(ex.getRequestMethod())){String id=path.substring("/v1/player/payments/".length(),path.length()-"/cancel".length());owned(id,playerId);reply(ex,200,Map.of("code","OK","data",view(payments.cancel(id,playerId,"CANCELLED_BY_PLAYER"),null)));return;}
        throw new Api(404,"PAYMENT_ROUTE_NOT_FOUND","player payment route not found");
    }catch(Api e){reply(ex,e.status,Map.of("code",e.code,"message",e.getMessage()));}catch(SecurityException e){reply(ex,401,Map.of("code","PAYMENT_UNAUTHORIZED","message","valid player session required"));}catch(IllegalArgumentException e){reply(ex,400,Map.of("code","PAYMENT_INVALID_REQUEST","message",String.valueOf(e.getMessage())));}catch(IllegalStateException e){reply(ex,409,Map.of("code","PAYMENT_STATE_CONFLICT","message",String.valueOf(e.getMessage())));}catch(Exception e){reply(ex,500,Map.of("code","PAYMENT_UNAVAILABLE","message","payment service unavailable"));}}
    private PaymentOrder owned(String id,long player){PaymentOrder order=queries.order(id).orElseThrow(()->new IllegalArgumentException("payment order not found"));if(order.buyer().playerId()!=player)throw new SecurityException("payment order owner mismatch");return order;}
    private static Map<String,Object> view(PaymentOrder o,HostedPaymentProviderAdapter.Checkout checkout){var data=new java.util.LinkedHashMap<String,Object>();data.put("orderId",o.orderId());data.put("state",o.state());data.put("productCode",o.product().productCode());data.put("assetCurrency",o.product().assetCurrency());data.put("assetUnits",o.product().assetUnits());data.put("fiatCurrency",o.product().fiatCurrency());data.put("amountMinor",o.product().amountMinor());data.put("failureCode",o.failureCode());data.put("createdAt",o.createdAt());data.put("paidAt",o.paidAt());data.put("deliveredAt",o.deliveredAt());if(checkout!=null){data.put("checkoutUrl",checkout.checkoutUrl());data.put("expiresAt",checkout.expiresAt());}return data;}
    @SuppressWarnings("unchecked")private Map<String,Object> body(HttpExchange ex)throws IOException{return json.readValue(ex.getRequestBody(),Map.class);}private static String text(Map<String,Object>b,String k){Object v=b.get(k);if(!(v instanceof String s)||s.isBlank())throw new IllegalArgumentException(k+" required");return s;}private static long numberOr(Map<String,Object>b,String k,long d){Object v=b.get(k);if(v==null)return d;if(v instanceof Number n)return n.longValue();throw new IllegalArgumentException(k+" invalid");}
    private void reply(HttpExchange ex,int status,Object value)throws IOException{byte[] bytes=json.writeValueAsBytes(value);ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");ex.getResponseHeaders().set("Cache-Control","no-store");ex.sendResponseHeaders(status,bytes.length);try(var out=ex.getResponseBody()){out.write(bytes);}}
    static final class PlayerAuth{private final byte[] secret;private final Clock clock;PlayerAuth(String secret,Clock clock){if(secret==null||secret.length()<32)throw new IllegalArgumentException("account auth secret must be at least 32 characters");this.secret=secret.getBytes(StandardCharsets.UTF_8);this.clock=clock;}long authenticate(String header){try{if(header==null||!header.startsWith("Bearer "))throw new SecurityException();String[] p=header.substring(7).split("\\.",-1);if(p.length!=3)throw new SecurityException();long player=Long.parseLong(p[0]),expires=Long.parseLong(p[1]);if(player<=0||expires<clock.instant().getEpochSecond()||expires>clock.instant().plusSeconds(300).getEpochSecond())throw new SecurityException();Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));if(!MessageDigest.isEqual(mac.doFinal((p[0]+"."+p[1]).getBytes(StandardCharsets.UTF_8)),HexFormat.of().parseHex(p[2])))throw new SecurityException();return player;}catch(SecurityException e){throw e;}catch(Exception e){throw new SecurityException();}}}
    private static final class Api extends RuntimeException{final int status;final String code;Api(int status,String code,String message){super(message);this.status=status;this.code=code;}}
}
