package com.aoo.bcg.gamespi;

public record RuleResult(boolean accepted, String code, String message) {
    public RuleResult {
        code = code == null ? "" : code;
        message = message == null ? "" : message;
    }

    public static RuleResult accept() {
        return new RuleResult(true, "OK", "");
    }

    public static RuleResult reject(String code, String message) {
        return new RuleResult(false, code, message);
    }
}
