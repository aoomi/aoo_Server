package com.aoo.bcg.share;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class InviteLinkSigner {
    private final byte[] key; private final SecureRandom random;
    public InviteLinkSigner(String key){this(key,new SecureRandom());}
    InviteLinkSigner(String key,SecureRandom random){if(key==null||key.getBytes(StandardCharsets.UTF_8).length<32)throw new IllegalArgumentException("invite signing key must be at least 32 bytes");this.key=key.getBytes(StandardCharsets.UTF_8);this.random=random;}
    public String issue(){byte[] b=new byte[24];random.nextBytes(b);String code=Base64.getUrlEncoder().withoutPadding().encodeToString(b);return code+"."+signature(code);}
    public String verify(String token){if(token==null||!token.matches("[A-Za-z0-9_-]{32}\\.[A-Za-z0-9_-]{22}"))throw invalid();int dot=token.indexOf('.');String code=token.substring(0,dot);byte[] supplied=token.substring(dot+1).getBytes(StandardCharsets.US_ASCII);byte[] expected=signature(code).getBytes(StandardCharsets.US_ASCII);if(!MessageDigest.isEqual(supplied,expected))throw invalid();return code;}
    private String signature(String code){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));byte[] full=mac.doFinal(code.getBytes(StandardCharsets.US_ASCII));byte[] shortValue=java.util.Arrays.copyOf(full,16);return Base64.getUrlEncoder().withoutPadding().encodeToString(shortValue);}catch(Exception e){throw new IllegalStateException(e);}}
    private static byte[] decode(String value){try{return Base64.getUrlDecoder().decode(value);}catch(IllegalArgumentException e){throw invalid();}}
    private static InviteLinkError invalid(){return new InviteLinkError(404,"INVITE_NOT_FOUND","invite link is unavailable");}
}
