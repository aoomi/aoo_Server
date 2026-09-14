package com.aoo.bcg.admin;

/** The submitted representation was based on a stale or missing target version. */
public final class AdminPreconditionException extends RuntimeException {
    private final String currentEtag;
    public AdminPreconditionException(String message, String currentEtag) {
        super(message); this.currentEtag = currentEtag;
    }
    public String currentEtag() { return currentEtag; }
}
