package com.aoo.bcg.gateway;
public enum ErrorDomain {
    COMMON(1000, 1999), ACCOUNT(2000, 2999), ROOM(3000, 3999), GAME(4000, 4999), BILLING(5000, 5999), CLUB(6000, 6999);
    private final int min; private final int max;
    ErrorDomain(int min, int max) { this.min = min; this.max = max; }
    public boolean contains(int code) { return code >= min && code <= max; }
}
