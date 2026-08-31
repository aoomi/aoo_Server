package com.aoo.bcg.gamespi.api;
import static org.junit.jupiter.api.Assertions.*;import java.time.*;import org.junit.jupiter.api.Test;
class VersionNegotiatorTest{
 private VersionNegotiator negotiator(){Instant now=Instant.parse("2026-08-23T00:00:00Z");return new VersionNegotiator(new CompatibilityWindow(SemanticVersion.parse("2.1.0"),SemanticVersion.parse("2.9.9"),now.plusSeconds(60),Clock.fixed(now,ZoneOffset.UTC),(version,decision)->{}));}
 @Test void coversMinimumMaximumUnknownAndMissingVersions(){var value=negotiator();assertEquals(VersionNegotiator.Outcome.ACCEPTED,value.negotiate("2.1.0"));assertEquals(VersionNegotiator.Outcome.ACCEPTED,value.negotiate("2.9.9"));assertEquals(VersionNegotiator.Outcome.UPGRADE_REQUIRED,value.negotiate("2.0.9"));assertEquals(VersionNegotiator.Outcome.UPGRADE_REQUIRED,value.negotiate("3.0.0"));assertEquals(VersionNegotiator.Outcome.INVALID_VERSION,value.negotiate("future"));assertEquals(VersionNegotiator.Outcome.MISSING_VERSION,value.negotiate(null));assertEquals(VersionNegotiator.Outcome.MISSING_VERSION,value.negotiate(" "));}
}
