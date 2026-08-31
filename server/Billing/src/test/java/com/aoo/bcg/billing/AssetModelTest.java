package com.aoo.bcg.billing;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AssetModelTest {
    @Test void currenciesItemsScopesAndTwoLegBalancesShareOneModel(){
        var city=AssetAccount.currency(new CurrencyAccount(1,"CITY_ROOM_CARD",510100));assertEquals(AssetAccount.Scope.CITY,city.scope());assertEquals(new CurrencyAccount(1,"CITY_ROOM_CARD",510100),city.toCurrencyAccount());
        var source=AssetAccount.item(1,"PROP.LUCKY_CARD",AssetAccount.Scope.GLOBAL,0);var target=AssetAccount.item(2,"PROP.LUCKY_CARD",AssetAccount.Scope.GLOBAL,0);
        var movement=new AssetMovement("gift-item-1",AssetMovement.Type.GIFT,source,target,3,10,7,1,4,"ACTIVITY_GIFT",null,Instant.EPOCH);
        assertTrue(movement.balanced());assertEquals(-3,movement.legs().getFirst().delta());assertEquals(3,movement.legs().getLast().delta());
        assertThrows(IllegalArgumentException.class,()->AssetAccount.item(1,"PROP",AssetAccount.Scope.CITY,0));
    }
}
