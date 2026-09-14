package com.aoo.bcg.gamespi.api;

/** Protocol-facing parser that gives stable outcomes for absent and malformed client versions. */
public final class VersionNegotiator {
    public enum Outcome { ACCEPTED, UPGRADE_REQUIRED, WINDOW_EXPIRED, MISSING_VERSION, INVALID_VERSION }
    private final CompatibilityWindow window;
    public VersionNegotiator(CompatibilityWindow window){this.window=window;}
    public Outcome negotiate(String clientVersion){
        if(clientVersion==null||clientVersion.isBlank())return Outcome.MISSING_VERSION;
        final SemanticVersion parsed;try{parsed=SemanticVersion.parse(clientVersion);}catch(IllegalArgumentException error){return Outcome.INVALID_VERSION;}
        return switch(window.negotiate(parsed)){case ACCEPTED->Outcome.ACCEPTED;case UPGRADE_REQUIRED->Outcome.UPGRADE_REQUIRED;case WINDOW_EXPIRED->Outcome.WINDOW_EXPIRED;};
    }
}
