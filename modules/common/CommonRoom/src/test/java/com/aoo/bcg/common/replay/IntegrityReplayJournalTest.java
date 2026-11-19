package com.aoo.bcg.common.replay;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IntegrityReplayJournalTest {
    @Test void freezesVersionsChainsFramesRejectsGapsDuplicatesAndBudgetOverflow() {
        ReplayVersionManifest manifest = manifest();
        IntegrityReplayJournal journal = new IntegrityReplayJournal(manifest, 2, 16);
        IntegrityReplayFrame first = journal.append(1, 0, "event-1", "DEALT", 3, bytes("cards"));
        IntegrityReplayFrame second = journal.append(2, 0, "event-2", "PLAYED", 3, bytes("card"));
        assertEquals(first.hash(), second.previousHash());
        IntegrityReplayJournal.verify(manifest, journal.frames());
        assertThrows(IllegalArgumentException.class,
                () -> new IntegrityReplayJournal(manifest, 3, 32).append(2, 0, "gap", "PLAYED", 3, bytes("x")));
        IntegrityReplayJournal duplicate = new IntegrityReplayJournal(manifest, 3, 32);
        duplicate.append(1, 0, "same", "DEALT", 3, bytes("x"));
        assertThrows(IllegalStateException.class,
                () -> duplicate.append(2, 0, "same", "PLAYED", 3, bytes("x")));
        assertThrows(IllegalStateException.class,
                () -> journal.append(3, 0, "event-3", "PLAYED", 3, bytes("x")));
    }

    @Test void detectsPayloadAndManifestTampering() {
        ReplayVersionManifest manifest = manifest();
        IntegrityReplayJournal journal = new IntegrityReplayJournal(manifest, 4, 64);
        IntegrityReplayFrame original = journal.append(1, 1, "event-1", "PLAYED", 3, bytes("payload"));
        IntegrityReplayFrame tampered = new IntegrityReplayFrame(original.sequence(), original.roundNo(),
                original.eventId(), original.eventType(), original.schemaVersion(), bytes("altered"),
                original.previousHash(), original.hash());
        assertThrows(SecurityException.class, () -> IntegrityReplayJournal.verify(manifest, List.of(tampered)));
        ReplayVersionManifest drifted = new ReplayVersionManifest(7, 62, "play-v2", "rule-v9",
                "random-v1", "cards-v1", 3, "initial", "commitment");
        assertThrows(SecurityException.class, () -> IntegrityReplayJournal.verify(drifted, journal.frames()));
    }

    private static ReplayVersionManifest manifest() {
        return new ReplayVersionManifest(7, 62, "play-v2", "rule-v1", "random-v1",
                "cards-v1", 3, "initial", "commitment");
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
}
