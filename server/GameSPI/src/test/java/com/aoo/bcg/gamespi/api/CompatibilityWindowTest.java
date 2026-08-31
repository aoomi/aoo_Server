package com.aoo.bcg.gamespi.api;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;import java.time.Instant;import java.time.ZoneOffset;import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class CompatibilityWindowTest{
 @Test void enforcesConfiguredRangeObservesCallsAndExpires(){
  Instant now=Instant.parse("2026-08-23T00:00:00Z");var observed=new ArrayList<String>();
  var active=new CompatibilityWindow(SemanticVersion.parse("2.1.0"),SemanticVersion.parse("2.9.9"),now.plusSeconds(60),Clock.fixed(now,ZoneOffset.UTC),(version,decision)->observed.add(version+":"+decision));
  assertEquals(CompatibilityWindow.Decision.ACCEPTED,active.negotiate(SemanticVersion.parse("2.5.0")));
  assertEquals(CompatibilityWindow.Decision.UPGRADE_REQUIRED,active.negotiate(SemanticVersion.parse("1.9.0")));assertEquals(2,observed.size());
  var expired=new CompatibilityWindow(SemanticVersion.parse("2.1.0"),SemanticVersion.parse("2.9.9"),now,Clock.fixed(now,ZoneOffset.UTC),(version,decision)->{});
  assertEquals(CompatibilityWindow.Decision.WINDOW_EXPIRED,expired.negotiate(SemanticVersion.parse("2.5.0")));
 }
}
