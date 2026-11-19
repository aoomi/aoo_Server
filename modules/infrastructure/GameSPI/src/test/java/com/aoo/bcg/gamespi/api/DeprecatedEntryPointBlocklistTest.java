package com.aoo.bcg.gamespi.api;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DeprecatedEntryPointBlocklistTest{
 @Test void synchronouslyBlocksLegacyRouteMessageConfigAndListeners(){
  assertDoesNotThrow(()->DeprecatedEntryPointBlocklist.requireHttpAllowed("/api/v1/room/create"));
  assertThrows(SecurityException.class,()->DeprecatedEntryPointBlocklist.requireHttpAllowed("/v1/room/create"));
  assertThrows(SecurityException.class,()->DeprecatedEntryPointBlocklist.requireMessageAllowed("legacy.room.join"));
  assertThrows(SecurityException.class,()->DeprecatedEntryPointBlocklist.requireMessageAllowed("1004"));
  assertThrows(SecurityException.class,()->DeprecatedEntryPointBlocklist.requirePortAllowed(9998));
  assertThrows(SecurityException.class,()->DeprecatedEntryPointBlocklist.requireConfigAllowed("legacy.protocol.enabled"));
  assertDoesNotThrow(()->DeprecatedEntryPointBlocklist.requireMessageAllowed("common.room.join_req"));
 }
}
