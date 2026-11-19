package com.aoo.bcg.common.event;

import java.util.Map;

public record ConsumedEventResult(int code, int schemaVersion, Map<String,Object> data) {
    public ConsumedEventResult {
        if(schemaVersion<=0)throw new IllegalArgumentException("result schema version must be positive");
        data=Map.copyOf(data==null?Map.of():data);
    }
    public static ConsumedEventResult success(){return new ConsumedEventResult(0,1,Map.of());}
}
