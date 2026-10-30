package com.aoo.bcg.common.serialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DomainJsonDecoderTest {
    private static final TypeReference<Map<String,Object>> DOCUMENT = new TypeReference<>() {};
    @Test void rerunsSchemaAndDomainInvariantAfterObjectMapperDecode() {
        var decoder = new DomainJsonDecoder(new ObjectMapper());
        assertEquals(3, ((Number) decoder.decode("{\"seatCount\":3}", DOCUMENT,
                value -> { DomainJsonDecoder.requireDocument(value); if (((Number)value.get("seatCount")).intValue() < 2) throw new IllegalArgumentException("seatCount"); }).get("seatCount")).intValue());
        assertThrows(IllegalArgumentException.class, () -> decoder.decode("{\"seatCount\":1}", DOCUMENT,
                value -> { if (((Number)value.get("seatCount")).intValue() < 2) throw new IllegalArgumentException("seatCount"); }));
        assertThrows(IllegalArgumentException.class, () -> decoder.decode("null", DOCUMENT, DomainJsonDecoder::requireDocument));
        assertThrows(IllegalArgumentException.class, () -> decoder.decode("{\"\":1}", DOCUMENT, DomainJsonDecoder::requireDocument));
    }
}
