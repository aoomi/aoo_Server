package com.aoo.bcg.common.dissolve;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Immutable, persistable dissolve vote authority. */
public record DissolveVoteState(long applicantId, Set<Long> eligiblePlayerIds, Map<Long,Boolean> votes,
                                Instant deadline, boolean approveOnTimeout) {
    public DissolveVoteState {
        if(applicantId<=0||eligiblePlayerIds==null||!eligiblePlayerIds.contains(applicantId)||deadline==null)throw new IllegalArgumentException("invalid dissolve vote");
        eligiblePlayerIds=Set.copyOf(eligiblePlayerIds);votes=Map.copyOf(votes==null?Map.of():votes);
        if(!eligiblePlayerIds.containsAll(votes.keySet()))throw new IllegalArgumentException("ineligible voter");
    }
    public static DissolveVoteState start(long applicantId,Set<Long> eligible,Instant deadline,boolean approveOnTimeout){return new DissolveVoteState(applicantId,eligible,Map.of(applicantId,true),deadline,approveOnTimeout);}
    public DissolveVoteState vote(long playerId,boolean approve,Instant now){if(!now.isBefore(deadline))throw new IllegalStateException("vote expired");if(!eligiblePlayerIds.contains(playerId))throw new SecurityException("not eligible");Map<Long,Boolean> next=new LinkedHashMap<>(votes);Boolean old=next.putIfAbsent(playerId,approve);if(old!=null&&old!=approve)throw new IllegalStateException("vote is immutable");return new DissolveVoteState(applicantId,eligiblePlayerIds,next,deadline,approveOnTimeout);}
    public DissolveDecision decision(Instant now){return new MajorityDissolvePolicy(approveOnTimeout).decide(applicantId,eligiblePlayerIds,votes,deadline,now);}
    public Map<String,Object> toMap(){return Map.of("applicantId",applicantId,"eligiblePlayerIds",eligiblePlayerIds,"votes",votes,"deadlineEpochMillis",deadline.toEpochMilli(),"approveOnTimeout",approveOnTimeout);}
    public static DissolveVoteState from(Map<String,Object> map){long applicant=((Number)map.get("applicantId")).longValue();Set<Long>eligible=((java.util.Collection<?>)map.get("eligiblePlayerIds")).stream().map(v->((Number)v).longValue()).collect(java.util.stream.Collectors.toSet());Map<Long,Boolean>votes=new LinkedHashMap<>();((Map<?,?>)map.get("votes")).forEach((k,v)->votes.put(Long.parseLong(String.valueOf(k)),Boolean.parseBoolean(String.valueOf(v))));return new DissolveVoteState(applicant,eligible,votes,Instant.ofEpochMilli(((Number)map.get("deadlineEpochMillis")).longValue()),Boolean.TRUE.equals(map.get("approveOnTimeout")));}
}
