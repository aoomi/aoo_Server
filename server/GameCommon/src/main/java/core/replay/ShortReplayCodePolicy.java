package core.replay;

/** Capacity and retry policy; threshold is intentionally configurable. */
public record ShortReplayCodePolicy(double capacityThreshold, int retriesPerLength) {
    public ShortReplayCodePolicy {
        if (!(capacityThreshold > 0 && capacityThreshold <= 1)) throw new IllegalArgumentException("capacity threshold must be in (0,1]");
        if (retriesPerLength < 1 || retriesPerLength > 1000) throw new IllegalArgumentException("retries must be 1..1000");
    }
    public static ShortReplayCodePolicy defaults() { return new ShortReplayCodePolicy(0.70d, 32); }
    public static ShortReplayCodePolicy configured() {
        String threshold=System.getProperty("aoo.replay.code.capacity-threshold",System.getenv().getOrDefault("AOO_REPLAY_CODE_CAPACITY_THRESHOLD","0.70"));
        String retries=System.getProperty("aoo.replay.code.retries-per-length",System.getenv().getOrDefault("AOO_REPLAY_CODE_RETRIES_PER_LENGTH","32"));
        return new ShortReplayCodePolicy(Double.parseDouble(threshold),Integer.parseInt(retries));
    }
}
