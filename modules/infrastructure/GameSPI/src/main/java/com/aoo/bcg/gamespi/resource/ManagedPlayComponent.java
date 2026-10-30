package com.aoo.bcg.gamespi.resource;

/** Mandatory lifecycle for stateful play components. */
public interface ManagedPlayComponent {
    String componentId();
    void initialize();
    void start();
    void resetBetweenRounds();
    void destroy();
}
