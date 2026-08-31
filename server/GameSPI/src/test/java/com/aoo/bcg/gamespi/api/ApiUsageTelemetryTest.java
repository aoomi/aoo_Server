package com.aoo.bcg.gamespi.api;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ApiUsageTelemetryTest{
 @Test void countsActualHttpAndWssCallsByClientVersion(){
  var telemetry=new ApiUsageTelemetry(ApiLifecycleCatalog.standard(),100);
  var old=SemanticVersion.parse("2.0.0");var current=SemanticVersion.parse("2.8.0");
  telemetry.record(ApiOwnershipCatalog.Transport.HTTP,"/api/v2/admin/game-profiles",old);
  telemetry.record(ApiOwnershipCatalog.Transport.HTTP,"/api/v2/admin/game-profiles",old);
  telemetry.record(ApiOwnershipCatalog.Transport.WSS,"poker.paodekuai.play_cards_req",current);
  assertEquals(2L,telemetry.snapshot().get(new ApiUsageTelemetry.Key(ApiOwnershipCatalog.Transport.HTTP,"/api/v2/admin/game-profiles",old)));
  assertEquals(1L,telemetry.snapshot().get(new ApiUsageTelemetry.Key(ApiOwnershipCatalog.Transport.WSS,"poker.paodekuai.play_cards_req",current)));
  assertThrows(IllegalStateException.class,()->telemetry.record(ApiOwnershipCatalog.Transport.WSS,"shadow.action_req",current));
 }
}
