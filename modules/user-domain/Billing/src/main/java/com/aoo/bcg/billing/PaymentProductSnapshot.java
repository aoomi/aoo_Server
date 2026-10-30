package com.aoo.bcg.billing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

/** Immutable amount/product snapshot created by the server at order creation. */
public record PaymentProductSnapshot(String productCode,long version,String assetCurrency,long assetUnits,
        String fiatCurrency,long amountMinor,String channelCode,Instant lockedAt,String fingerprint){
    public PaymentProductSnapshot{
        if(productCode==null||!productCode.matches("[A-Z0-9_.-]{1,64}")||version<=0
                ||assetCurrency==null||!assetCurrency.matches("[A-Z0-9_]{1,32}")||assetUnits<=0
                ||fiatCurrency==null||!fiatCurrency.matches("[A-Z]{3}")||amountMinor<=0
                ||channelCode==null||!channelCode.matches("[A-Z0-9_]{1,32}"))throw new IllegalArgumentException("invalid product snapshot");
        Objects.requireNonNull(lockedAt,"lockedAt");
        String expected=digest(productCode+"|"+version+"|"+assetCurrency+"|"+assetUnits+"|"+fiatCurrency
                +"|"+amountMinor+"|"+channelCode+"|"+lockedAt);
        if(fingerprint==null||fingerprint.isBlank())fingerprint=expected;
        else if(!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),fingerprint.getBytes(StandardCharsets.US_ASCII)))
            throw new IllegalArgumentException("product snapshot fingerprint mismatch");
    }
    public static PaymentProductSnapshot lock(String productCode,long version,String assetCurrency,long assetUnits,
            String fiatCurrency,long amountMinor,String channelCode,Instant lockedAt){
        return new PaymentProductSnapshot(productCode,version,assetCurrency,assetUnits,fiatCurrency,amountMinor,channelCode,lockedAt,null);
    }
    static String digest(String value){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
