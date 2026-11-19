package business.global.pk.zjh;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ZjhConfigMgrTest {
    @Test void loadsAuthoritativeLegacyConfiguration() {
        ZjhConfigMgr config = new ZjhConfigMgr();
        assertEquals(java.util.List.of(0, 100, 200, 500), config.getEndPointList());
        assertEquals(java.util.List.of(1, 2, 5, 10), config.getBottomPointList());
        assertEquals(80, config.getRobotOpenCard());
        assertEquals(5, config.getAddScoreAll());
    }

    @Test void rejectsIncompleteConfiguration() throws Exception {
        Path file = Files.createTempFile("zjh-invalid-", ".txt");
        Files.writeString(file, "difenList=[1];\n");
        try {
            assertThrows(IllegalStateException.class, () -> new ZjhConfigMgr(file));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
