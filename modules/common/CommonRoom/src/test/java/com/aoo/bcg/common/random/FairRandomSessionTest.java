package com.aoo.bcg.common.random;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FairRandomSessionTest {
    @Test void commitsBeforePlayHidesSecretAndReplaysExactlyAfterSeal() {
        byte[] secret = new byte[32];
        java.util.Arrays.fill(secret, (byte) 7);
        FairRandomSession live = FairRandomSession.deterministic(secret, 9, 3, "rules-v4");
        String commitment = live.commitment();
        assertEquals(64, commitment.length());
        assertThrows(UnsupportedOperationException.class, live::seed);
        List<Integer> decisions = List.of(live.nextInt(54), live.nextInt(7), live.nextInt(2));
        RandomAuditReveal reveal = live.seal("final-state-hash");
        assertEquals("final-state-hash", reveal.finalStateHash());
        assertFalse(reveal.toString().contains(reveal.secretBase64()));
        assertThrows(IllegalStateException.class, () -> live.nextInt(4));

        FairRandomSession replay = FairRandomSession.replay(reveal);
        assertEquals(decisions, List.of(replay.nextInt(54), replay.nextInt(7), replay.nextInt(2)));
        assertEquals(reveal.drawCount(), replay.drawCount());
    }

    @Test void fisherYatesPreservesEveryItemAndBoundsAreAlwaysRespected() {
        byte[] secret = new byte[32];
        FairRandomSession random = FairRandomSession.deterministic(secret, 10, 0, "v1");
        List<Integer> cards = new ArrayList<>();
        for (int card = 0; card < 54; card++) cards.add(card);
        random.shuffle(cards);
        assertEquals(54, new HashSet<>(cards).size());
        for (int sample = 0; sample < 10_000; sample++) {
            int value = random.nextInt(7);
            assertTrue(value >= 0 && value < 7);
        }
    }
}
