package com.aoo.bcg.lobby;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
final class LobbyCreateDiagnosticTest {
 @Test void disabledByDefaultAndRedactsSensitiveKeys(){
  assertTrue(LobbyCreateDiagnostic.project(2001,Map.of("token","x","rules",Map.of("roundCount",10)),Map.of("playVersion","v1","rules",Map.of("roundCount",10)),200).isEmpty());
  System.setProperty("aoo.hall.clubCreateDiagnostic2001.enabled","true");
  try { var d=LobbyCreateDiagnostic.project(2001,Map.of("requestId","r","password","x","rules",Map.of("roundCount",10,"playerCount",4)),Map.of("playVersion","v1","releaseId",11,"indexGeneration",2,"sourceHash","a".repeat(64),"rules",Map.of("roundCount",10,"playerCount",4)),200); assertEquals(List.of("playerCount","roundCount"),d.get("ruleKeyNames")); assertFalse(String.valueOf(d).contains("password")); }
  finally {System.clearProperty("aoo.hall.clubCreateDiagnostic2001.enabled");}
 }
 @Test void exceptionStatusProjectionIsSafe(){assertTrue(LobbyCreateDiagnostic.project(2002,Map.of(),Map.of(),500).isEmpty()); assertTrue(LobbyCreateDiagnostic.project(2001,Map.of(),Map.of(),500,"10.0.0.8").isEmpty()); assertTrue(LobbyCreateDiagnostic.loopback("127.0.0.1")); assertTrue(LobbyCreateDiagnostic.loopback("::1")); assertTrue(LobbyCreateDiagnostic.loopback("localhost")); assertFalse(LobbyCreateDiagnostic.loopback("10.0.0.8"));}
}
