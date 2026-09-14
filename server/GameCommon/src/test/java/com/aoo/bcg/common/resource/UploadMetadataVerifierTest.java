package com.aoo.bcg.common.resource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UploadMetadataVerifierTest {
    @Test void recomputesEverySecurityRelevantFieldAndRejectsClientMismatch() throws Exception {
        byte[] payload = "server-observed-audio".getBytes(StandardCharsets.UTF_8);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        var verifier = new UploadMetadataVerifier(64, 10_000, Set.of("audio/ogg"));
        var claim = new UploadMetadataVerifier.Claim("voice.ogg", "audio/ogg", payload.length, 1200, hash);
        var verified = verifier.verify(claim, new ByteArrayInputStream(payload), "audio/ogg", 1200);
        assertEquals(hash, verified.sha256());
        assertThrows(SecurityException.class, () -> verifier.verify(
                new UploadMetadataVerifier.Claim("../voice.ogg", "audio/ogg", payload.length, 1200, hash),
                new ByteArrayInputStream(payload), "audio/ogg", 1200));
        assertThrows(SecurityException.class, () -> verifier.verify(
                new UploadMetadataVerifier.Claim("voice.ogg", "audio/mpeg", payload.length, 1200, hash),
                new ByteArrayInputStream(payload), "audio/ogg", 1200));
        assertThrows(SecurityException.class, () -> verifier.verify(
                new UploadMetadataVerifier.Claim("voice.ogg", "audio/ogg", payload.length + 1, 1200, hash),
                new ByteArrayInputStream(payload), "audio/ogg", 1200));
    }
}
