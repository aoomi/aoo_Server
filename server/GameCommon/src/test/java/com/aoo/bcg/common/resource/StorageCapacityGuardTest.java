package com.aoo.bcg.common.resource;
import static org.junit.jupiter.api.Assertions.*;import java.nio.file.Path;import org.junit.jupiter.api.Test;
class StorageCapacityGuardTest{
 @Test void faultInjectionRejectsEveryWritingDomainBeforeDiskIsFull(){var guard=new StorageCapacityGuard(path->100);for(var domain:StorageCapacityGuard.Domain.values()){var error=assertThrows(StorageCapacityGuard.StorageCapacityException.class,()->guard.require(Path.of("."),81,20,domain));assertEquals(domain,error.domain());}}
 @Test void acceptsWhenRequestPreservesReserve(){var guard=new StorageCapacityGuard(path->101);for(var domain:StorageCapacityGuard.Domain.values())assertDoesNotThrow(()->guard.require(Path.of("."),80,20,domain));}
}
