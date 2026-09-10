package com.aoo.bcg.poker;

import java.util.Set;

/** Stable business identities for the single PDK family. */
final class PdkBusinessCodes {
    static final String CHENGDU = "CD201";
    static final String NEIJIANG = "NJ201";
    static final String LIANGSHAN = "LS201";
    static final Set<String> ALL = Set.of(CHENGDU, NEIJIANG, LIANGSHAN);

    private PdkBusinessCodes() { }
}
