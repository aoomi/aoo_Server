package com.aoo.bcg.billing;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CurrencyAccountContractTest {
    @Test void activeCurrenciesAreGlobalAndShareAssetValidation(){
        for(String code:new String[]{"ROOM_CARD","GOLD"}){
            var account=new CurrencyAccount(1,code);
            assertFalse(account.scoped());
            assertEquals(account,AssetAccount.currency(account).toCurrencyAccount());
            assertThrows(IllegalArgumentException.class,()->new CurrencyAccount(1,code,9));
            assertThrows(IllegalArgumentException.class,()->new CurrencyAccount(1,code,-1));
            assertThrows(IllegalArgumentException.class,()->new AssetAccount(AssetAccount.Principal.PLAYER,1,AssetAccount.Kind.CURRENCY,code,AssetAccount.Scope.UNION,9));
        }
    }
    @Test void retiredCurrenciesCannotEnterNewAccountsButHistoricalLedgerRemainsReadable(){
        for(String code:new String[]{"CRYSTAL","SPORTS_POINT","CLUB_CARD"}){
            assertThrows(IllegalArgumentException.class,()->new CurrencyAccount(1,code));
            assertThrows(IllegalArgumentException.class,()->new AssetAccount(AssetAccount.Principal.PLAYER,1,AssetAccount.Kind.CURRENCY,code,AssetAccount.Scope.GLOBAL,0));
            var historical=new LedgerEntry("historical:"+code,1,code,7,17,"MIGRATION",Instant.EPOCH);
            assertEquals(code,historical.currency());assertEquals(10,historical.balanceBefore());
        }
        assertEquals(AssetAccount.Kind.ITEM,AssetAccount.item(1,"PROP.LUCKY_CARD",AssetAccount.Scope.UNION,9).kind());
    }
}
