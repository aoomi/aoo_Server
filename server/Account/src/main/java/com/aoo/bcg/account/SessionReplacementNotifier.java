package com.aoo.bcg.account;

/** Publishes a committed single-device login so an older live socket is terminated immediately. */
@FunctionalInterface
public interface SessionReplacementNotifier {
    void replaced(long accountId, String currentSessionId, long authGeneration);

    static SessionReplacementNotifier none() {
        return (accountId, currentSessionId, authGeneration) -> { };
    }
}
