package com.aoo.bcg.common.event;

import java.util.Map;

public record CompensationEvent(String compensatesBusinessEventId,String reason,Map<String,Object> correction){
    public CompensationEvent{if(compensatesBusinessEventId==null||compensatesBusinessEventId.isBlank()||reason==null||reason.isBlank()||reason.length()>500)throw new IllegalArgumentException("invalid compensation event");correction=Map.copyOf(correction==null?Map.of():correction);}
}
