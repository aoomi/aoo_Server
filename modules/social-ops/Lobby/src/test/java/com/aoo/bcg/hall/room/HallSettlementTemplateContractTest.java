package com.aoo.bcg.hall.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class HallSettlementTemplateContractTest {
    @Test void pdkDefaultsDoNotRepeatSmallTemplateButAllowExplicitBigTemplate() {
        Map<String,Object> ui=JdbcHallRepository.settlementUi("POKER_PAO_DE_KUAI",
                Map.of("fields", java.util.List.of(), "bigSettleTemplate", "BigSettleTpl_110"));
        assertEquals("BigSettleTpl_110",ui.get("bigSettleTemplate"));
        assertEquals(false,ui.containsKey("smallSettleTemplate"));
    }

    @Test void pokerSmallTemplateMustMatchTheAuthoritativeFamily() {
        assertThrows(RuntimeException.class,()->JdbcHallRepository.settlementUi("POKER_PAO_DE_KUAI",
                Map.of("smallSettleTemplate","SmallSettleTpl_zjh_00")));
        assertEquals("SmallSettleTpl_pdk_01",JdbcHallRepository.settlementUi("POKER_PAO_DE_KUAI",
                Map.of("smallSettleTemplate","SmallSettleTpl_pdk_01")).get("smallSettleTemplate"));
        assertEquals("SmallSettleTpl_pdk_01",JdbcHallRepository.settlementUi("poker:pao-de-kuai",
                Map.of("smallSettleTemplate","SmallSettleTpl_pdk_01")).get("smallSettleTemplate"));
        assertEquals("SmallSettleTpl_510k_01",JdbcHallRepository.settlementUi("poker:510k",
                Map.of("smallSettleTemplate","SmallSettleTpl_510k_01")).get("smallSettleTemplate"));
    }
}
