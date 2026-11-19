package com.aoo.bcg.common.random;

/** Immutable, post-round audit material. Its string form deliberately never exposes the secret. */
public final class RandomAuditReveal {
    private final String algorithmVersion;
    private final String domain;
    private final String commitment;
    private final String secretBase64;
    private final String finalStateHash;
    private final long drawCount;

    RandomAuditReveal(String algorithmVersion, String domain, String commitment,
                      String secretBase64, String finalStateHash, long drawCount) {
        this.algorithmVersion = algorithmVersion;
        this.domain = domain;
        this.commitment = commitment;
        this.secretBase64 = secretBase64;
        this.finalStateHash = finalStateHash;
        this.drawCount = drawCount;
    }

    public String algorithmVersion() { return algorithmVersion; }
    public String domain() { return domain; }
    public String commitment() { return commitment; }
    public String secretBase64() { return secretBase64; }
    public String finalStateHash() { return finalStateHash; }
    public long drawCount() { return drawCount; }
    @Override public String toString() {
        return "RandomAuditReveal[algorithmVersion=" + algorithmVersion + ", domain=" + domain
                + ", commitment=" + commitment + ", secretBase64=<redacted>, drawCount=" + drawCount + "]";
    }
}
