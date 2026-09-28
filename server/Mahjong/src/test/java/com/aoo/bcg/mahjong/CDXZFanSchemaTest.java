package com.aoo.bcg.mahjong;

// 本单由 Codex 执行器实施、岗位核验=majiang；Java 实跑登记待环境条件，禁止 mvn/javac，本测试为离线用例对照

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CDXZFanSchemaTest {
    @Test void schemaHasFortyOneUniqueValidRulesAndCompiles() {
        var schema = CDXZFanSchema.schema();
        assertEquals(41, schema.size());
        assertEquals(41, schema.stream().map(MahjongRegionalRules.FanRule::code).collect(Collectors.toSet()).size());
        assertTrue(schema.stream().allMatch(rule -> rule.fan() >= 0));
        assertDoesNotThrow(() -> MahjongRegionalRules.compileFanSchema(schema));
        assertEquals(41, MahjongRegionalRules.compileFanSchema(schema).size());
    }

    @Test void fengDingUsesCeoAuthoritativeFourCaseMapping() {
        assertEquals(0L, CDXZFanSchema.capFromFengDing(0));
        assertEquals(50L, CDXZFanSchema.capFromFengDing(1));
        assertEquals(100L, CDXZFanSchema.capFromFengDing(2));
        assertEquals(Long.MAX_VALUE, CDXZFanSchema.capFromFengDing(3));
        assertThrows(IllegalArgumentException.class, () -> CDXZFanSchema.capFromFengDing(-1));
        assertThrows(IllegalArgumentException.class, () -> CDXZFanSchema.capFromFengDing(4));
    }

    @Test void pendingAuthorityNotesKeepBothAmountsUnimplementedAndPendingConfirmation() {
        var notes = CDXZFanSchema.pendingAuthorityNotes();
        assertEquals(Set.of("CHA_JIAO_AMOUNT", "HUA_ZHU_AMOUNT"), notes.keySet());
        assertEquals(2, notes.values().stream().filter(note -> note.contains("待用户确认")).count());
    }

    @Test void representativeXqpOfflineDefaultsMatchTheMappingArchive() {
        Map<String, MahjongRegionalRules.FanRule> rules = CDXZFanSchema.schema().stream()
                .collect(Collectors.toMap(MahjongRegionalRules.FanRule::code, Function.identity()));
        assertEquals(0, rules.get(CDXZFanSchema.PING_HU).fan());
        assertEquals(2, rules.get(CDXZFanSchema.QING_YI_SE).fan());
        assertEquals(3, rules.get(CDXZFanSchema.LONG_QI_DUI).fan()); // 当前映射原始行 #4（差异）
        assertEquals(3, rules.get(CDXZFanSchema.YAO_JIU).fan()); // 混/清幺九合并分支，当前映射原始行 #9
        assertEquals(2, rules.get(CDXZFanSchema.TIAO_WU).fan());
        assertEquals(1, rules.get(CDXZFanSchema.GANG_SHANG_PAO).fan()); // 派工称“#24杠上花”；当前41项无杠上花，原始行 #32 有通用tag
        assertEquals(5, rules.get(CDXZFanSchema.QING_YI_SE_LONG_QI_DUI).fan());
        assertEquals(3, rules.get(CDXZFanSchema.QING_YAO_JIU).fan()); // 派工称“#44清幺九”；当前映射原始行 #54
    }
}
