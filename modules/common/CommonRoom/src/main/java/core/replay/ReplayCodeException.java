package core.replay;

/** Stable protocol errors for replay-code allocation and lookup. */
public final class ReplayCodeException extends RuntimeException {
    private final String code;
    public ReplayCodeException(String code, String message) { super(message); this.code = code; }
    public String code() { return code; }
}
