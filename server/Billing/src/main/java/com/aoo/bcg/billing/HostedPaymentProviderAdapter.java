package com.aoo.bcg.billing;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Produces a short-lived, server-signed redirect to the configured production payment provider. */
public final class HostedPaymentProviderAdapter {
    private final URI checkoutEndpoint;private final String merchantId;private final byte[] secret;private final Clock clock;
    public HostedPaymentProviderAdapter(URI checkoutEndpoint,String merchantId,String signingSecret,Clock clock){
        if(checkoutEndpoint==null||!"https".equalsIgnoreCase(checkoutEndpoint.getScheme())||checkoutEndpoint.getHost()==null)throw new IllegalArgumentException("payment provider checkout endpoint must be HTTPS");
        if(merchantId==null||!merchantId.matches("[A-Za-z0-9_.-]{1,64}"))throw new IllegalArgumentException("invalid payment merchant id");
        if(signingSecret==null||signingSecret.getBytes(StandardCharsets.UTF_8).length<32)throw new IllegalArgumentException("payment provider signing secret must be at least 32 bytes");
        this.checkoutEndpoint=checkoutEndpoint;this.merchantId=merchantId;this.secret=signingSecret.getBytes(StandardCharsets.UTF_8);this.clock=clock;
    }
    public Checkout checkout(PaymentOrder order){
        Instant expiresAt=clock.instant().plusSeconds(900);
        String canonical=String.join("|",merchantId,order.orderId(),Long.toString(order.product().amountMinor()),order.product().fiatCurrency(),order.product().fingerprint(),Long.toString(expiresAt.getEpochSecond()));
        String query="merchantId="+q(merchantId)+"&orderId="+q(order.orderId())+"&amountMinor="+order.product().amountMinor()+"&currency="+q(order.product().fiatCurrency())+"&productFingerprint="+q(order.product().fingerprint())+"&expiresAt="+expiresAt.getEpochSecond()+"&signature="+q(sign(canonical));
        return new Checkout(URI.create(checkoutEndpoint.toString()+(checkoutEndpoint.getRawQuery()==null?"?":"&")+query).toString(),expiresAt);
    }
    String sign(String value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("cannot sign provider checkout",e);}}
    private static String q(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    public record Checkout(String checkoutUrl,Instant expiresAt){}
}
