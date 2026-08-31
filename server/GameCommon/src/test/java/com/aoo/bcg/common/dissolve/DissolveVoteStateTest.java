package com.aoo.bcg.common.dissolve;
import java.time.Instant;import java.util.Set;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class DissolveVoteStateTest{
 @Test void startVoteDeadlineAndDefaultSurviveSnapshot(){Instant deadline=Instant.ofEpochSecond(100);var state=DissolveVoteState.start(1,Set.of(1L,2L,3L),deadline,true).vote(2,false,Instant.ofEpochSecond(50));var restored=DissolveVoteState.from(state.toMap());assertEquals(state,restored);assertEquals(DissolveDecision.WAITING,restored.decision(Instant.ofEpochSecond(60)));assertEquals(DissolveDecision.EXPIRED_APPROVED,restored.decision(deadline));assertThrows(IllegalStateException.class,()->restored.vote(3,true,deadline));}
 @Test void majorityUsesAllEligiblePlayersRatherThanOnlyVotesAlreadyCast(){Instant now=Instant.ofEpochSecond(10);var state=DissolveVoteState.start(1,Set.of(1L,2L,3L,4L),now.plusSeconds(60),true);assertEquals(DissolveDecision.WAITING,state.decision(now));assertEquals(DissolveDecision.APPROVED,state.vote(2,true,now).vote(3,true,now).decision(now));assertEquals(DissolveDecision.REJECTED,state.vote(2,false,now).vote(3,false,now).vote(4,false,now).decision(now));}
}
