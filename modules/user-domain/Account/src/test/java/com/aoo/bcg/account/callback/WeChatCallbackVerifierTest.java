package com.aoo.bcg.account.callback;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class WeChatCallbackVerifierTest {
    @Test void validatesEchoConstantTimeFreshnessAndReplay(){
        Instant now=Instant.ofEpochSecond(1787529600L);var claimed=new HashSet<String>();
        var verifier=new WeChatCallbackVerifier("callback-token-value".toCharArray(),Clock.fixed(now,ZoneOffset.UTC),Duration.ofMinutes(5),(key,until)->claimed.add(key));
        String timestamp=Long.toString(now.getEpochSecond()),nonce="nonce-123",signature=verifier.signature(timestamp,nonce);
        assertEquals("echo-safe",verifier.verifyUrl(signature,timestamp,nonce,"echo-safe"));
        assertEquals(WeChatCallbackVerifier.ErrorCode.REPLAYED,assertThrows(WeChatCallbackVerifier.CallbackException.class,()->verifier.verify(signature,timestamp,nonce)).code());
        assertEquals(WeChatCallbackVerifier.ErrorCode.INVALID_SIGNATURE,assertThrows(WeChatCallbackVerifier.CallbackException.class,()->verifier.verify("0".repeat(40),timestamp,"different")).code());
        String old=Long.toString(now.minus(Duration.ofMinutes(6)).getEpochSecond());
        assertEquals(WeChatCallbackVerifier.ErrorCode.EXPIRED,assertThrows(WeChatCallbackVerifier.CallbackException.class,()->verifier.verify(verifier.signature(old,"old"),old,"old")).code());
    }
}
