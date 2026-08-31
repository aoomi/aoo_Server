package com.aoo.bcg.billing;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** HMAC-SHA256 verification with constant-time comparison; secrets remain outside order state. */
public final class PaymentCallbackVerifier {
    private final byte[] secret;
    public PaymentCallbackVerifier(byte[] secret){if(secret==null||secret.length<32)throw new IllegalArgumentException("payment secret must be at least 256 bits");this.secret=secret.clone();}
    public String sign(String canonicalPayload){
        if(canonicalPayload==null)throw new IllegalArgumentException("payload required");
        try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(canonicalPayload.getBytes(StandardCharsets.UTF_8)));}
        catch(GeneralSecurityException impossible){throw new IllegalStateException(impossible);}
    }
    public boolean verify(String canonicalPayload,String signature){
        if(signature==null||!signature.matches("[0-9a-fA-F]{64}"))return false;
        return MessageDigest.isEqual(sign(canonicalPayload).getBytes(StandardCharsets.US_ASCII),signature.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }
}
