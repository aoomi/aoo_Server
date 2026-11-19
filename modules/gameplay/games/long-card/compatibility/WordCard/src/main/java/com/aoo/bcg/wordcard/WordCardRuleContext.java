package com.aoo.bcg.wordcard;
public record WordCardRuleContext(int huXi, int redCardCount) {
    public WordCardRuleContext { if (huXi < 0 || redCardCount < 0) throw new IllegalArgumentException("negative rule value"); }
}
