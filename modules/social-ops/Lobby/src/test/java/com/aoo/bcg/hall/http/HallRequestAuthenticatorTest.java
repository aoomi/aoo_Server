package com.aoo.bcg.hall.http;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;
class HallRequestAuthenticatorTest{
 @Test void acceptsSignedShortLivedAssertionAndRejectsReplayWindow()throws Exception{Instant now=Instant.parse("2026-08-24T00:00:00Z");var a=new HallRequestAuthenticator("01234567890123456789012345678901",Clock.fixed(now,ZoneOffset.UTC));String payload="42."+now.plusSeconds(30).getEpochSecond();String token=payload+"."+HexFormat.of().formatHex(a.sign(payload));assertEquals(42,a.authenticate("Bearer "+token));assertThrows(HallError.class,()->a.authenticate("Bearer 42."+now.minusSeconds(1).getEpochSecond()+".00"));}
}
