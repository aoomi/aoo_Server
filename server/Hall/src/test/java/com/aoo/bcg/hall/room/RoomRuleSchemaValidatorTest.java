package com.aoo.bcg.hall.room;

import static org.junit.jupiter.api.Assertions.*;
import com.aoo.bcg.hall.http.HallError;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class RoomRuleSchemaValidatorTest {
    private static final Map<String,Object> SCHEMA = Map.of("fields", List.of(
            Map.of("key","roundCount","label","局数","control","STEPPER","required",true,
                    "defaultValue",8,"min",8,"max",16,"step",2),
            Map.of("key","playerCount","label","人数","control","SINGLE_SELECT","required",true,
                    "defaultValue",3,"options",List.of(Map.of("value",2,"label","2人"),Map.of("value",3,"label","3人"))),
            Map.of("key","commonOptions","label","通用选项","control","MULTI_SELECT","defaultValue",List.of(),
                    "options",List.of(Map.of("value","voice","label","语音"),Map.of("value","debug","label","调试","disabled",true))),
            Map.of("key","fourWithThree","label","四带三","control","CHECKBOX","disabled",true,
                    "defaultValue",List.of(),"options",List.of(Map.of("value",true,"label","开启")))));

    @Test void normalizesDefaultsAndValidValues() {
        assertEquals(Map.of("roundCount",8,"playerCount",2,"commonOptions",List.of("voice")),
                RoomRuleSchemaValidator.validate(SCHEMA,Map.of("playerCount",2,"commonOptions",List.of("voice"))));
    }

    @Test void rejectsDisabledUnknownAndInvalidStepperValues() {
        assertThrows(HallError.class,() -> RoomRuleSchemaValidator.validate(SCHEMA,Map.of("fourWithThree",List.of(true))));
        assertThrows(HallError.class,() -> RoomRuleSchemaValidator.validate(SCHEMA,Map.of("unknown",1)));
        assertThrows(HallError.class,() -> RoomRuleSchemaValidator.validate(SCHEMA,Map.of("roundCount",9)));
        assertThrows(HallError.class,() -> RoomRuleSchemaValidator.validate(SCHEMA,Map.of("commonOptions",List.of("debug"))));
    }

    @Test void enforcesPublishedDependenciesAndConflicts() {
        Map<String,Object> schema=Map.of(
                "fields",List.of(
                        Map.of("key","tripleMode","control","SINGLE_SELECT","required",true,
                                "defaultValue","SINGLES","options",List.of("SINGLES","PAIRS")),
                        Map.of("key","tripleTwo","control","SINGLE_SELECT","required",true,
                                "defaultValue",false,"options",List.of(false,true)),
                        Map.of("key","freeAttachments","control","SINGLE_SELECT","required",true,
                                "defaultValue",false,"options",List.of(false,true))),
                "constraints",List.of(
                        Map.of("type","REQUIRES","field","tripleMode","whenValues",List.of("PAIRS"),
                                "otherField","tripleTwo","allowedValues",List.of(true)),
                        Map.of("type","CONFLICTS","field","tripleMode","whenValues",List.of("PAIRS"),
                                "otherField","freeAttachments","allowedValues",List.of(true))));
        assertDoesNotThrow(()->RoomRuleSchemaValidator.validate(schema,
                Map.of("tripleMode","PAIRS","tripleTwo",true,"freeAttachments",false)));
        HallError missing=assertThrows(HallError.class,()->RoomRuleSchemaValidator.validate(schema,
                Map.of("tripleMode","PAIRS","tripleTwo",false,"freeAttachments",false)));
        assertEquals("HALL_RULE_DEPENDENCY_INVALID",missing.code());
        HallError conflict=assertThrows(HallError.class,()->RoomRuleSchemaValidator.validate(schema,
                Map.of("tripleMode","PAIRS","tripleTwo",true,"freeAttachments",true)));
        assertEquals("HALL_RULE_CONFLICT",conflict.code());
    }
}
