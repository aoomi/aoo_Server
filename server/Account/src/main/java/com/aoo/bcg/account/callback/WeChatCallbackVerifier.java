package com.aoo.bcg.account.callback;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/** Verifies the SHA-1 signature used by WeChat server URL validation and plaintext callbacks. */
public final class WeChatCallbackVerifier {
    private final char[] token;
    private final Clock clock;
    private final Duration allowedSkew;
    private final ReplayGuard replayGuard;

    public WeChatCallbackVerifier(char[] token, Clock clock, Duration allowedSkew, ReplayGuard replayGuard) {
        if(token==null||token.length<16)throw new IllegalArgumentException("callback token is too short");this.token=token.clone();
        this.clock=Objects.requireNonNull(clock,"clock");this.allowedSkew=Objects.requireNonNull(allowedSkew,"allowedSkew");
        if(allowedSkew.isNegative()||allowedSkew.isZero())throw new IllegalArgumentException("allowedSkew must be positive");
        this.replayGuard=Objects.requireNonNull(replayGuard,"replayGuard");
    }

    public VerifiedCallback verify(String signature,String timestamp,String nonce) {
        if(signature==null||!signature.matches("[0-9a-fA-F]{40}"))throw new CallbackException(ErrorCode.INVALID_SIGNATURE,"invalid WeChat callback signature");
        long seconds;
        try{seconds=Long.parseLong(text(timestamp,"timestamp"));}catch(NumberFormatException invalid){throw new CallbackException(ErrorCode.INVALID_TIMESTAMP,"invalid WeChat callback timestamp");}
        Instant occurredAt;
        try{occurredAt=Instant.ofEpochSecond(seconds);}catch(RuntimeException invalid){throw new CallbackException(ErrorCode.INVALID_TIMESTAMP,"invalid WeChat callback timestamp");}
        Instant now=clock.instant();if(occurredAt.isBefore(now.minus(allowedSkew))||occurredAt.isAfter(now.plus(allowedSkew)))throw new CallbackException(ErrorCode.EXPIRED,"expired WeChat callback");
        nonce=text(nonce,"nonce");if(nonce.length()>256)throw new CallbackException(ErrorCode.INVALID_NONCE,"invalid WeChat callback nonce");
        String expected=signature(timestamp,nonce);
        if(!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),signature.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII)))throw new CallbackException(ErrorCode.INVALID_SIGNATURE,"invalid WeChat callback signature");
        Instant retainUntil=occurredAt.plus(allowedSkew);
        if(!replayGuard.claim(timestamp+":"+nonce+":"+signature.toLowerCase(java.util.Locale.ROOT),retainUntil))throw new CallbackException(ErrorCode.REPLAYED,"replayed WeChat callback");
        return new VerifiedCallback(occurredAt,nonce);
    }

    /** URL setup uses the same signature but must echo the caller-provided echostr only after verification. */
    public String verifyUrl(String signature,String timestamp,String nonce,String echoString){verify(signature,timestamp,nonce);return text(echoString,"echoString");}

    String signature(String timestamp,String nonce){String[] values={new String(token),timestamp,nonce};Arrays.sort(values);try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(String.join("",values).getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}finally{values[0]=null;}}
    private static String text(String v,String n){if(v==null||v.isBlank())throw new CallbackException(ErrorCode.MALFORMED,"missing "+n);return v.strip();}
    public record VerifiedCallback(Instant occurredAt,String nonce){}
    public enum ErrorCode { MALFORMED, INVALID_SIGNATURE, INVALID_TIMESTAMP, INVALID_NONCE, EXPIRED, REPLAYED }
    public static final class CallbackException extends SecurityException{private final ErrorCode code;public CallbackException(ErrorCode code,String message){super(message);this.code=Objects.requireNonNull(code);}public ErrorCode code(){return code;}}
    @FunctionalInterface public interface ReplayGuard { /** Atomically returns true only for the first claim before retainUntil. */ boolean claim(String replayKey,Instant retainUntil); }
}
