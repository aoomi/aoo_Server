package com.aoo.bcg.common.replay;

/** Complete version identity required to replay a room without loading an old runtime class. */
public record ReplayVersionManifest(long roomId, int gameId, String playVersion, String ruleVersion,
                                    String algorithmVersion, String cardEncodingVersion,
                                    int eventSchemaVersion, String initialStateHash,
                                    String randomCommitment) {
    public ReplayVersionManifest {
        if (roomId <= 0 || gameId <= 0 || eventSchemaVersion <= 0
                || blank(playVersion) || blank(ruleVersion) || blank(algorithmVersion)
                || blank(cardEncodingVersion) || blank(initialStateHash) || blank(randomCommitment))
            throw new IllegalArgumentException("incomplete replay version manifest");
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
