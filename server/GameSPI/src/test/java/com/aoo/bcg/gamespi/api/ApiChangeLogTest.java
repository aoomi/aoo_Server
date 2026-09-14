package com.aoo.bcg.gamespi.api;
import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;
class ApiChangeLogTest{@Test void frontendOperationsAndSupportCanQuerySameBehaviorHistory(){var log=ApiChangeLog.standard();for(var audience:ApiChangeLog.Audience.values())assertFalse(log.query(audience,SemanticVersion.parse("2.0.0")).isEmpty());var change=log.endpoint("common.*").getFirst();assertFalse(change.behavior().isBlank());assertFalse(change.migration().isBlank());}}
