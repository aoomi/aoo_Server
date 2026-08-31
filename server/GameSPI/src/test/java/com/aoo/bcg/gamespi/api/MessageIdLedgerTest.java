package com.aoo.bcg.gamespi.api;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class MessageIdLedgerTest {
    @Test void preventsDuplicatesAndPermanentReuseAfterRetirement(){
        var ledger=new MessageIdLedger();var v2=SemanticVersion.parse("2.0.0");
        ledger.register("poker.paodekuai.play_cards_req","Poker",v2);
        assertThrows(IllegalStateException.class,()->ledger.register("poker.paodekuai.play_cards_req","ShadowPoker",v2));
        ledger.retire("poker.paodekuai.play_cards_req",SemanticVersion.parse("3.0.0"));
        assertThrows(IllegalStateException.class,()->ledger.register("poker.paodekuai.play_cards_req","Poker",v2));
        assertEquals(MessageIdLedger.State.RETIRED,ledger.require("poker.paodekuai.play_cards_req").state());
        assertThrows(IllegalArgumentException.class,()->ledger.register("bad.message","Poker",v2));
    }
}
