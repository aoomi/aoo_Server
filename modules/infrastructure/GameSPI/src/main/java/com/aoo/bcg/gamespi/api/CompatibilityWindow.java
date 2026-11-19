package com.aoo.bcg.gamespi.api;

import java.time.Clock;
import java.time.Instant;

/** One compatibility decision for HTTP and WSS, with version-level observability. */
public final class CompatibilityWindow {
    public enum Decision { ACCEPTED, UPGRADE_REQUIRED, WINDOW_EXPIRED }
    @FunctionalInterface public interface Observer { void record(SemanticVersion clientVersion, Decision decision); }
    private final SemanticVersion minimum,maximum;
    private final Instant endsAt;
    private final Clock clock;
    private final Observer observer;
    public CompatibilityWindow(SemanticVersion minimum,SemanticVersion maximum,Instant endsAt,Clock clock,Observer observer){
        if(minimum==null||maximum==null||minimum.compareTo(maximum)>0||endsAt==null||clock==null||observer==null)throw new IllegalArgumentException("invalid compatibility window");
        this.minimum=minimum;this.maximum=maximum;this.endsAt=endsAt;this.clock=clock;this.observer=observer;
    }
    public Decision negotiate(SemanticVersion clientVersion){
        if(clientVersion==null)throw new IllegalArgumentException("client version is required");
        Decision decision=!clock.instant().isBefore(endsAt)?Decision.WINDOW_EXPIRED
            :clientVersion.compareTo(minimum)<0||clientVersion.compareTo(maximum)>0?Decision.UPGRADE_REQUIRED:Decision.ACCEPTED;
        observer.record(clientVersion,decision);return decision;
    }
    public SemanticVersion minimum(){return minimum;}public SemanticVersion maximum(){return maximum;}public Instant endsAt(){return endsAt;}
}
