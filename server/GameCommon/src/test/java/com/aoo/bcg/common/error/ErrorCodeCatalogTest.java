package com.aoo.bcg.common.error;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ErrorCodeCatalogTest {
    @Test void publishedMeaningIsImmutableAndRetiredNumberRemainsReserved(){
        var catalog=ErrorCodeCatalog.standard();
        var original=catalog.require(1009);
        assertThrows(IllegalStateException.class,()->catalog.publish(new ErrorCodeCatalog.Definition(1009,"OTHER","changed","2.1.0",null)));
        catalog.retire(1009,"3.0.0");
        assertEquals("3.0.0",catalog.require(1009).retiredVersion());
        assertThrows(IllegalStateException.class,()->catalog.publish(original));
        assertThrows(IllegalStateException.class,()->catalog.publish(new ErrorCodeCatalog.Definition(1099,"DUPLICATE_SUBMISSION","new use","3.0.0",null)));
    }
}
