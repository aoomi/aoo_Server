package com.aoo.bcg.common.event;
public record ConsumedEventClaim(String eventId,String token){public ConsumedEventClaim{if(eventId==null||eventId.isBlank()||token==null||token.isBlank())throw new IllegalArgumentException("invalid consumed event claim");}}
