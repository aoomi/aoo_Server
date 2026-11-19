package com.aoo.bcg.poker.nn;

import static org.junit.jupiter.api.Assertions.*;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NnParticipantProfileTest {
  @Test @SuppressWarnings("unchecked") void defaultSnapshotPublishesDisplayRulesAndEmptyNames() {
    var session=new NnSession(298001,1001,1,NnRules.defaults());
    Map<String,Object> snapshot=session.snapshotFor(1001);
    assertEquals(Map.of("baseScore",1L,"enterScore",100L,"leaveScore",50L,
        "minCarryToRob",0L,"kanShunDouEnabled",true),snapshot.get("rules"));
    assertEquals(Map.of(),snapshot.get("playerNames"));
  }

  @Test @SuppressWarnings("unchecked") void rawRobThresholdIsDisplayOnlyUntilExistingGateIsUsed() {
    var authority=provider().createAuthoritativeSession(new RoomCreationContext(298002,1001,
        Map.of("minCarryToRob",60))).orElseThrow();
    Map<String,Object> rules=(Map<String,Object>)authority.viewFor(1001).get("rules");
    assertEquals(60L,rules.get("minCarryToRob"));
  }

  @Test @SuppressWarnings("unchecked") void attachedSourcePublishesNicknameAndMissingSourcePublishesEmptyMap() {
    var attached=authority(298003);attached.attachParticipantProfileSource(accountId->"测试昵称");
    attached.execute(sit(298003,"sit-attached",1001));
    int seat=(int)attached.viewFor(1001).get("viewerSeat");
    assertEquals("测试昵称",((Map<Integer,String>)attached.viewFor(1001).get("playerNames")).get(seat));
    var detached=authority(298004);detached.execute(sit(298004,"sit-detached",1001));
    assertEquals(Map.of(),detached.viewFor(1001).get("playerNames"));
  }

  @Test void restorePreservesNamesAndAcceptsLegacyStateWithoutNames() {
    var session=new NnSession(298005,1001,1,NnRules.defaults());
    int seat=session.sit(1001,"sit");session.setPlayerName(seat,"测试昵称");
    var restored=NnSession.restore(session.authoritativeState());
    assertEquals("测试昵称",restored.playerName(seat));
    var legacy=new LinkedHashMap<>(session.authoritativeState());legacy.remove("playerNames");
    assertDoesNotThrow(()->NnSession.restore(legacy));
    assertEquals(Map.of(),NnSession.restore(legacy).snapshotFor(1001).get("playerNames"));
  }

  @Test void attachingDifferentSourceTwiceIsRejected() {
    var authority=authority(298006);
    authority.attachParticipantProfileSource(accountId->"甲");
    assertThrows(IllegalStateException.class,()->authority.attachParticipantProfileSource(accountId->"乙"));
  }

  private static NnGameProvider provider(){return new NnGameProvider(new GameDescriptor(298,"quanguo2998","全国牛牛",
      GameCategory.POKER,NnRules.FAMILY,RegionScope.NATIONAL,"","",NnRules.PLAY_VERSION));}
  private static NnAuthority authority(long roomId){return new NnAuthority(
      new NnSession(roomId,1001,1,NnRules.defaults()),NnRules.PLAY_VERSION);}
  private static GameCommandRequest sit(long roomId,String requestId,long playerId){return new GameCommandRequest(
      "poker.quanguo2998.sit_req",requestId,1,roomId,0,NnRules.PLAY_VERSION,String.valueOf(playerId),0,Map.of());}
}
