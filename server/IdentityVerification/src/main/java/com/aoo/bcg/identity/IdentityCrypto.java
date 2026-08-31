package com.aoo.bcg.identity;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Envelope encryption and stable, non-reversible lookup tokens for regulated identity values. */
public final class IdentityCrypto {
    private final byte[] encryptionKey, tokenKey; private final SecureRandom random;
    public IdentityCrypto(String encryptionKeyBase64,String tokenKey,SecureRandom random){
        encryptionKey=Base64.getDecoder().decode(encryptionKeyBase64);if(encryptionKey.length!=32)throw new IllegalArgumentException("identity encryption key must be 32 bytes");
        this.tokenKey=tokenKey.getBytes(StandardCharsets.UTF_8);if(this.tokenKey.length<32)throw new IllegalArgumentException("identity token key must be at least 32 bytes");this.random=random;
    }
    public String encrypt(String value){try{byte[] iv=new byte[12];random.nextBytes(iv);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(encryptionKey,"AES"),new GCMParameterSpec(128,iv));byte[] sealed=c.doFinal(value.getBytes(StandardCharsets.UTF_8));byte[] all=new byte[iv.length+sealed.length];System.arraycopy(iv,0,all,0,iv.length);System.arraycopy(sealed,0,all,iv.length,sealed.length);return Base64.getEncoder().encodeToString(all);}catch(Exception e){throw new IllegalStateException("identity encryption failed",e);}}
    public String decrypt(String value){try{byte[] all=Base64.getDecoder().decode(value),iv=java.util.Arrays.copyOfRange(all,0,12),sealed=java.util.Arrays.copyOfRange(all,12,all.length);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(encryptionKey,"AES"),new GCMParameterSpec(128,iv));return new String(c.doFinal(sealed),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException("identity decryption failed",e);}}
    public String token(String namespace,String value){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(tokenKey,"HmacSHA256"));return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal((namespace+'\0'+value).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    public String codeHash(String challenge,String code){return token("sms:"+challenge,code);}
    public static boolean equal(String a,String b){return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
    public static String maskName(String s){if(s==null||s.isBlank())return "";return s.substring(0,1)+"*".repeat(Math.max(1,s.length()-1));}
    public static String maskId(String s){return s.length()<8?"****":s.substring(0,3)+"********"+s.substring(s.length()-4);}
    public static String maskPhone(String s){return s.length()<7?"****":s.substring(0,3)+"****"+s.substring(s.length()-4);}
}
