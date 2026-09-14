package com.aoo.bcg.club;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.math.BigDecimal;

class JdbcClubServiceIntegrationTest {
    @Test void ownerCanReadAllowanceAndGrantSportsPointFromCaptainPage() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:captain_sports_point;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);service.create("create-67",67,670,"Sports Point");
        service.addInvitedMember("join-671",67,670,671);
        service.update("union-score-67",67,state->{var settings=new LinkedHashMap<>(state.settings());
            settings.put("unionTotalScore",20000);
            return JdbcClubService.copy(state,state.name(),state.status(),state.members(),state.templates(),state.tables(),state.invites(),state.records(),state.ledger(),settings,state.applications(),state.memberExtras(),state.groupings(),state.roomBans(),state.viewedRooms());});
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9018L);
        var info=(Map<?,?>)dispatch.dispatch(670,"sports-info-671","club.CClubSubordinateLevelSportsPoint",
                Map.of("clubId",67,"opPid",671));
        assertEquals(0,new BigDecimal("20000").compareTo((BigDecimal)info.get("allowSportsPoint")));
        assertEquals(0,new BigDecimal("0").compareTo((BigDecimal)info.get("sportsPoint")));
        var changed=(Map<?,?>)dispatch.dispatch(670,"sports-add-671","club.CClubSubordinateLevelClubCentUpdate",
                Map.of("clubId",67,"opPid",671,"type",0,"value",1000));
        assertEquals(0,new BigDecimal("1000").compareTo((BigDecimal)changed.get("changedValue")));
        assertEquals(0,new BigDecimal("1000").compareTo(service.get(67).memberExtras().get(671L).clubCent()));
        var ownerDetail=(Map<?,?>)dispatch.dispatch(670,"detail-67","club.CGetClubListById",Map.of("clubId",67));
        assertEquals(0,new BigDecimal("19000").compareTo((BigDecimal)ownerDetail.get("clubCent")));
    }

    @Test void captainMemberListContainsOnlySelfAndDirectPlayers() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:captain_visible_members;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);service.create("create-66",66,660,"Captain Scope");
        service.addInvitedMember("join-661",66,660,661);service.addInvitedMember("join-662",66,660,662);
        service.addInvitedMember("join-663",66,660,663);
        service.update("captain-scope-66",66,state->{var extras=new LinkedHashMap<>(state.memberExtras());
            var captain=extras.get(661L);extras.put(661L,new JdbcClubService.MemberExtraState(captain.remarkName(),true,captain.upPlayerId(),
                    captain.clubCent(),captain.caseClubCent(),captain.warningPoint(),captain.eliminatePoint()));
            var direct=extras.get(662L);extras.put(662L,new JdbcClubService.MemberExtraState(direct.remarkName(),false,661,
                    direct.clubCent(),direct.caseClubCent(),direct.warningPoint(),direct.eliminatePoint()));
            return JdbcClubService.copy(state,state.name(),state.status(),state.members(),state.templates(),state.tables(),state.invites(),state.records(),state.ledger(),state.settings(),state.applications(),extras,state.groupings(),state.roomBans(),state.viewedRooms());});
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9017L);
        var rows=(java.util.List<?>)dispatch.dispatch(661,"members-661","club.CClubGetMemberManage",
                Map.of("clubId",66,"pageType",0,"pageNum",1));
        assertEquals(java.util.List.of(661L,662L),rows.stream()
                .map(row->((Number)((Map<?,?>)row).get("pid")).longValue()).toList());

        var captainRows=(Map<?,?>)dispatch.dispatch(661,"captain-list-661","club.CClubPromotionLevelList",
                Map.of("clubId",66,"pid",0,"pageNum",1,"query",""));
        var captainItems=(java.util.List<?>)captainRows.get("clubPromotionLevelItemList");
        assertEquals(java.util.List.of(661L,662L),captainItems.stream()
                .map(row->((Number)((Map<?,?>)row).get("pid")).longValue()).toList());

        var ownerRows=(Map<?,?>)dispatch.dispatch(660,"captain-list-owner-660","club.CClubPromotionLevelList",
                Map.of("clubId",66,"pid",0,"pageNum",1,"query",""));
        var ownerItems=(java.util.List<?>)ownerRows.get("clubPromotionLevelItemList");
        var ownerIds=ownerItems.stream().map(row->((Number)((Map<?,?>)row).get("pid")).longValue()).toList();
        assertEquals(660L,ownerIds.getFirst());
        assertEquals(java.util.Set.of(660L,661L,663L),new java.util.HashSet<>(ownerIds));
    }

    @Test void authenticatedAccountIsAlwaysFirstInMemberList() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:self_first_members;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);service.create("create-65",65,650,"Self First");
        service.addInvitedMember("join-651",65,650,651);service.addInvitedMember("join-652",65,650,652);
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9016L);
        var rows=(java.util.List<?>)dispatch.dispatch(651,"members-651","club.CClubGetMemberManage",
                Map.of("clubId",65,"pageType",0,"pageNum",1));
        assertEquals(651L,((Number)((Map<?,?>)rows.getFirst()).get("pid")).longValue());
    }

    @Test void lobbyInviteUsesCaptainPermissionAndPreservesDirectOwnership() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:lobby_captain_invite;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        try(Connection connection=source.getConnection();var statement=connection.createStatement()){
            statement.execute("INSERT INTO aoo_account_identity VALUES(642,'DISPLAY_ID','642','ACTIVE')");
            statement.execute("INSERT INTO aoo_account_identity VALUES(643,'DISPLAY_ID','643','ACTIVE')");
        }
        var service=new JdbcClubService(source,mapper,clock);service.create("create-64",64,640,"Lobby Captain Invite");
        service.addInvitedMember("join-641",64,640,641);
        service.update("captain-641",64,state->{var extras=new LinkedHashMap<>(state.memberExtras());
            var old=extras.get(641L);extras.put(641L,new JdbcClubService.MemberExtraState(old.remarkName(),true,old.upPlayerId(),
                    old.clubCent(),old.caseClubCent(),old.warningPoint(),old.eliminatePoint()));
            return JdbcClubService.copy(state,state.name(),state.status(),state.members(),state.templates(),state.tables(),state.invites(),state.records(),state.ledger(),state.settings(),state.applications(),extras,state.groupings(),state.roomBans(),state.viewedRooms());});
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9015L);
        dispatch.dispatch(641,"lobby-invite-642","club.CClubFindPIDAdd",Map.of("clubId",64,"pid",642));
        var state=service.get(64);
        assertEquals("MEMBER",state.members().get(642L));
        assertEquals(641L,state.memberExtras().get(642L).upPlayerId());
        assertThrows(SecurityException.class,()->dispatch.dispatch(642,"lobby-invite-643","club.CClubFindPIDAdd",Map.of("clubId",64,"pid",643)));
    }

    @Test void promotionCaptainCanInvitePlayerDirectlyUnderSelf() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:promotion_direct_invite;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);service.create("create-63",63,630,"Captain Invite");
        service.addInvitedMember("join-631",63,630,631);
        service.update("captain-631",63,state->{var extras=new LinkedHashMap<>(state.memberExtras());
            var old=extras.get(631L);extras.put(631L,new JdbcClubService.MemberExtraState(old.remarkName(),true,old.upPlayerId(),
                    old.clubCent(),old.caseClubCent(),old.warningPoint(),old.eliminatePoint()));
            return JdbcClubService.copy(state,state.name(),state.status(),state.members(),state.templates(),state.tables(),state.invites(),state.records(),state.ledger(),state.settings(),state.applications(),extras,state.groupings(),state.roomBans(),state.viewedRooms());});
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9014L);
        dispatch.dispatch(631,"captain-invite-632","club.CClubPromotionPidAdd",Map.of("clubId",63,"pid",632));
        var state=service.get(63);
        assertEquals("MEMBER",state.members().get(632L));
        assertEquals(631L,state.memberExtras().get(632L).upPlayerId());
        assertFalse(state.memberExtras().get(632L).promotionManager());
        assertThrows(SecurityException.class,()->dispatch.dispatch(632,"member-invite-633","club.CClubPromotionPidAdd",Map.of("clubId",63,"pid",633)));
    }

    @Test void subordinateListKeepsLegacyArrayShapeAndDirectHierarchy() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:club_subordinates;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);service.create("create-62",62,620,"Promoters");
        service.addInvitedMember("join-621",62,620,621);service.addInvitedMember("join-622",62,620,622);
        service.addInvitedMember("join-623",62,620,623);
        service.update("hierarchy-62",62,state->{var extras=new LinkedHashMap<>(state.memberExtras());
            var child=extras.get(622L);extras.put(622L,new JdbcClubService.MemberExtraState(child.remarkName(),child.promotionManager(),621,
                    child.clubCent(),child.caseClubCent(),child.warningPoint(),child.eliminatePoint()));
            return JdbcClubService.copy(state,state.name(),state.status(),state.members(),state.templates(),state.tables(),state.invites(),state.records(),state.ledger(),state.settings(),state.applications(),extras,state.groupings(),state.roomBans(),state.viewedRooms());});
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9013L);
        var rows=(java.util.List<?>)dispatch.dispatch(620,"subordinates-62","club.CClubSubordinateList",
                Map.of("clubId",62,"pid",621,"pageNum",1,"query",""));
        assertEquals(2,rows.size());
        assertEquals(java.util.Set.of(621L,622L),rows.stream().map(row->((Number)((Map<?,?>)row).get("accountId")).longValue()).collect(java.util.stream.Collectors.toSet()));
        assertThrows(SecurityException.class,()->dispatch.dispatch(623,"subordinates-denied-62","club.CClubSubordinateList",
                Map.of("clubId",62,"pid",621,"pageNum",1)));
    }

    @Test void clubOwnerCanCreateSameTableRestrictionWhileMemberCannot() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:club_grouping_permissions;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);service.create("create-45",45,450,"Restrictions");
        service.addInvitedMember("join-45",45,450,451);
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9004L);
        var groups=(java.util.List<?>)dispatch.dispatch(450,"group-45","club.CClubGroupingAdd",Map.of("clubId",45));
        assertEquals(1,groups.size());
        SecurityException denied=assertThrows(SecurityException.class,
                ()->dispatch.dispatch(451,"group-denied-45","club.CClubGroupingAdd",Map.of("clubId",45)));
        assertEquals("manager required",denied.getMessage());
    }

    @Test void memberExitAlwaysUsesAuthenticatedAccountInsteadOfDisplayId() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:club_self_exit;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);service.create("create-46",46,460,"Exit");
        service.addInvitedMember("join-46",46,460,461);
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9006L);
        dispatch.dispatch(461,"exit-46","club.CClubChangePlayerStatus",Map.of("clubId",46,"pid",900461,"status",0x40));
        assertFalse(service.get(46).members().containsKey(461L));
        assertTrue(service.get(46).members().containsKey(460L));
    }

    @Test void createUnionPersistsIdentityForImmediateClubReload() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:union_create_reload;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);service.create("create-44",44,440,"UnionOwner");
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9003L);
        var created=(Map<?,?>)dispatch.dispatch(440,"create-union-44","union.CUnionCreate",Map.of(
                "clubId",44,"unionName","测试联盟","join",0,"quit",0,"unionTotalScore",2000,
                "matchRate",0,"outSports",100,"prizeType",2,"ranking",3,"value",50));
        long unionId=((Number)created.get("unionId")).longValue();
        assertTrue(unionId>0);assertEquals("测试联盟",created.get("name"));
        var reloaded=(Map<?,?>)dispatch.dispatch(440,"reload-club-44","club.CGetClubListById",Map.of("clubId",44));
        assertEquals(unionId,((Number)reloaded.get("unionId")).longValue());
        assertEquals(unionId,((Number)reloaded.get("unionSign")).longValue());
        assertEquals("测试联盟",reloaded.get("unionName"));
        assertEquals(3,((Number)reloaded.get("unionPostType")).intValue());
    }

    @Test void unionClubCentTransferConservesConfiguredTotalAndUpdatesEveryProjection() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:union_club_cent_conservation;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);service.create("create-67",67,670,"UnionScores");
        service.addInvitedMember("join-671",67,670,671);
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9014L);
        dispatch.dispatch(670,"create-union-67","union.CUnionCreate",Map.of(
                "clubId",67,"unionName","积分联盟","unionTotalScore",20000,"outSports",0));

        var transfer=(Map<?,?>)dispatch.dispatch(670,"score-67","union.CUnionClubCentUpdate",
                Map.of("clubId",67,"unionId",9014,"opPid",671,"type",0,"value",90));
        assertEquals(0,new BigDecimal("90").compareTo((BigDecimal)transfer.get("changedValue")));
        assertEquals(0,new BigDecimal("19910").compareTo((BigDecimal)transfer.get("operatorChangedValue")));

        var detail=(Map<?,?>)dispatch.dispatch(670,"detail-67","club.CGetClubListById",Map.of("clubId",67));
        assertEquals(0,new BigDecimal("19910").compareTo((BigDecimal)detail.get("clubCent")));
        var members=(java.util.List<?>)dispatch.dispatch(670,"members-67","club.CClubGetMemberManage",Map.of("clubId",67,"pageNum",1));
        assertEquals(0,new BigDecimal("19910").compareTo((BigDecimal)((Map<?,?>)members.get(0)).get("clubCent")));
        assertEquals(0,new BigDecimal("90").compareTo((BigDecimal)((Map<?,?>)members.get(1)).get("clubCent")));

        var returned=(Map<?,?>)dispatch.dispatch(670,"score-return-67","union.CUnionClubCentUpdate",
                Map.of("clubId",67,"unionId",9014,"opPid",671,"type",1,"value",40));
        assertEquals(0,new BigDecimal("50").compareTo((BigDecimal)returned.get("changedValue")));
        assertEquals(0,new BigDecimal("19950").compareTo((BigDecimal)returned.get("operatorChangedValue")));
    }

    @Test void unionOwnerDissolveClearsOwnerAndMemberUnionState() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:union_dissolve;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);service.create("create-owner-49",49,490,"UnionOwner");
        service.create("create-member-50",50,500,"UnionMember");
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9012L);
        var created=(Map<?,?>)dispatch.dispatch(490,"create-union-49","union.CUnionCreate",Map.of(
                "clubId",49,"unionName","待解散联盟","unionTotalScore",2000,"outSports",0));
        long unionId=((Number)created.get("unionId")).longValue();
        service.update("join-union-50",50,state->{var settings=new LinkedHashMap<>(state.settings());
            settings.put("unionId",unionId);settings.put("unionSign",unionId);settings.put("unionName","待解散联盟");
            settings.put("unionOwnerId",490);settings.put("unionOwnerClubId",49);
            return JdbcClubService.copy(state,state.name(),state.status(),state.members(),state.templates(),state.tables(),state.invites(),state.records(),state.ledger(),settings,state.applications(),state.memberExtras(),state.groupings(),state.roomBans(),state.viewedRooms());});
        assertThrows(SecurityException.class,()->dispatch.dispatch(500,"dissolve-denied-50","union.CUnionDissolve",Map.of("clubId",50,"unionId",unionId)));
        assertEquals(0,((Number)dispatch.dispatch(490,"dissolve-union-49","union.CUnionDissolve",Map.of("clubId",49,"unionId",unionId))).intValue());
        assertEquals(0,((Number)service.get(49).settings().getOrDefault("unionId",0)).longValue());
        assertEquals(0,((Number)service.get(50).settings().getOrDefault("unionId",0)).longValue());
    }

    @Test void listAcceptsRetiredFieldsInPersistedMemberExtraState() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:club_legacy_member_extra;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var service=new JdbcClubService(source,new ObjectMapper().findAndRegisterModules(),Clock.systemUTC());
        try(Connection c=source.getConnection();var q=c.prepareStatement("INSERT INTO aoo_club_state(club_id,state_json,row_version,updated_at) VALUES(?,?,1,CURRENT_TIMESTAMP)")) {
            q.setLong(1,39);
            q.setString(2,"{\"id\":39,\"name\":\"历史亲友圈\",\"status\":\"ACTIVE\",\"members\":{\"388\":\"OWNER\"},\"memberExtras\":{\"388\":{\"remarkName\":\"\",\"promotionManager\":false,\"upPlayerId\":0,\"clubCent\":0,\"caseClubCent\":0,\"warningPoint\":0,\"eliminatePoint\":0,\"sportsPoint\":88}}}");
            q.executeUpdate();
        }
        var rows=service.list();
        assertEquals(1,rows.size());
        assertEquals("历史亲友圈",rows.get(0).name());
        assertEquals(BigDecimal.ZERO,rows.get(0).memberExtras().get(388L).clubCent());
    }

    @Test void commitsStateAndIdempotencyInTheSameDatabase() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:club_service;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var service=new JdbcClubService(source,new ObjectMapper().findAndRegisterModules(),Clock.systemUTC());
        var first=service.create("create-40",40,400,"Durable");var replay=service.create("create-40",999,999,"ignored");
        assertEquals(first,replay);service.saveTemplate("template-40",40,400,"t1","Classic","mj","{}");
        assertEquals(1,service.get(40).templates().size());
        try(Connection c=source.getConnection();var q=c.prepareStatement("SELECT row_version FROM aoo_club_state WHERE club_id=40");var rows=q.executeQuery()){assertTrue(rows.next());assertEquals(2,rows.getLong(1));}
        try(Connection c=source.getConnection();var q=c.prepareStatement("SELECT member_status,member_role FROM aoo_club_member WHERE club_id=40 AND player_id=400");var rows=q.executeQuery()){assertTrue(rows.next());assertEquals("ACTIVE",rows.getString(1));assertEquals("OWNER",rows.getString(2));}
    }
    @Test void memberProjectionTracksRoleAndRemoval() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:club_member_projection;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var service=new JdbcClubService(source,new ObjectMapper().findAndRegisterModules(),Clock.systemUTC());
        service.create("create-41",41,410,"Members");
        service.update("join-41",41,state->JdbcClubService.copy(state,state.name(),state.status(),java.util.Map.of(410L,"OWNER",411L,"MEMBER"),state.templates(),state.tables(),state.invites(),state.records(),state.ledger(),state.settings(),state.applications(),state.memberExtras(),state.groupings(),state.roomBans(),state.viewedRooms()));
        service.setRole("role-41",41,410,411,"ADMIN");
        try(Connection c=source.getConnection();var q=c.prepareStatement("SELECT member_role FROM aoo_club_member WHERE club_id=41 AND player_id=411");var rows=q.executeQuery()){assertTrue(rows.next());assertEquals("MANAGER",rows.getString(1));}
        service.kick("kick-41",41,410,411);
        try(Connection c=source.getConnection();var q=c.prepareStatement("SELECT member_status FROM aoo_club_member WHERE club_id=41 AND player_id=411");var rows=q.executeQuery()){assertTrue(rows.next());assertEquals("LEFT",rows.getString(1));}
    }
    @Test void unionTemplateFlowPersistsAndReturnsManagerAndLobbyShapes() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:union_template_flow;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);service.create("create-42",42,420,"Union");
        service.saveTemplate("club-template-before-union",42,420,"club-template-1",
                "升级前亲友圈桌", "CD201", "{\"gameCode\":\"CD201\",\"playerNum\":2}");
        service.create("create-43",43,430,"UnionMember");
        service.update("union-42",42,state->{var settings=new LinkedHashMap<>(state.settings());settings.put("unionId",84);settings.put("unionOwnerClubId",42);
            return JdbcClubService.copy(state,state.name(),state.status(),state.members(),state.templates(),state.tables(),state.invites(),state.records(),state.ledger(),settings,state.applications(),state.memberExtras(),state.groupings(),state.roomBans(),state.viewedRooms());});
        service.update("union-member-43",43,state->{var settings=new LinkedHashMap<>(state.settings());settings.put("unionId",84);settings.put("unionOwnerClubId",42);
            return JdbcClubService.copy(state,state.name(),state.status(),state.members(),state.templates(),state.tables(),state.invites(),state.records(),state.ledger(),settings,state.applications(),state.memberExtras(),state.groupings(),state.roomBans(),state.viewedRooms());});
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9001L);
        var emptyUnionRows=(java.util.List<?>)dispatch.dispatch(420,"empty-union-template-42",
                "union.CUnionRoomCfgList",Map.of("clubId",42,"unionId",84,"pageNum",1,"classType",0));
        assertTrue(emptyUnionRows.isEmpty(),"升级前亲友圈模板不得出现在联盟房间管理");
        // 联盟房间归属联盟发起圈；客户端携带的当前圈 ID 不得把合法盟主误判为普通成员。
        Map<String,Object> payload=new LinkedHashMap<>(Map.of("clubId",999,"unionId",84,"gameId",201,
                "gameCode","CD201","roomName","验收模板","playerCount",2,"roundCount",8,"roomSportsThreshold",100,"JoinGamePoint",10));
        var created=(Map<?,?>)dispatch.dispatch(420,"create-template-42","union.CUnionCreateRoom",payload);
        int unionGameIndex=((Number)created.get("id")).intValue();
        assertEquals("验收模板",created.get("roomName"));assertEquals(201,((Number)created.get("gameId")).intValue());
        var rows=(java.util.List<?>)dispatch.dispatch(420,"list-template-42","union.CUnionRoomCfgList",Map.of("clubId",42,"unionId",84,"pageNum",1,"classType",2));
        assertEquals(1,rows.size());assertEquals(10,((Number)((Map<?,?>)((Map<?,?>)rows.get(0)).get("bRoomConfigure")).get("JoinGamePoint")).intValue());
        var disabled=(java.util.List<?>)dispatch.dispatch(420,"disable-template-42","union.CUnionRoomCfgUpdate",Map.of("clubId",999,"unionId",84,"unionRoomCfgId",unionGameIndex,"status",1));
        assertEquals(1,disabled.size());assertEquals(1,((Number)((Map<?,?>)disabled.get(0)).get("status")).intValue());
        var enabled=(java.util.List<?>)dispatch.dispatch(420,"enable-template-42","union.CUnionRoomCfgUpdate",Map.of("clubId",999,"unionId",84,"unionRoomCfgId",unionGameIndex,"status",0));
        assertEquals(1,enabled.size());assertEquals(0,((Number)((Map<?,?>)enabled.get(0)).get("status")).intValue());
        var quickRows=(java.util.List<?>)dispatch.dispatch(420,"quick-template-42","union.CUnionRoomConfigItemList",Map.of("clubId",42,"unionId",84));
        assertEquals(1,quickRows.size());assertEquals("CD201",((Map<?,?>)quickRows.get(0)).get("gameType"));
        assertEquals(2,((Number)((Map<?,?>)quickRows.get(0)).get("playerNum")).intValue());
        assertEquals(8,((Number)((Map<?,?>)quickRows.get(0)).get("setCount")).intValue());
        assertEquals("CD201",((Map<?,?>)((Map<?,?>)quickRows.get(0)).get("bRoomConfigure")).get("gameCode"));
        var memberQuickRows=(java.util.List<?>)dispatch.dispatch(430,"quick-template-member-43","union.CUnionRoomConfigItemList",Map.of("clubId",43,"unionId",84));
        assertEquals(1,memberQuickRows.size());assertEquals("验收模板",((Map<?,?>)memberQuickRows.get(0)).get("roomName"));
        var count=(Map<?,?>)dispatch.dispatch(420,"count-template-42","union.CUnionRoomCfgCount",Map.of("clubId",42,"unionId",84,"classType",2));
        assertEquals(0L,((Number)count.get("roomCount")).longValue());
        var lobby=(java.util.List<?>)dispatch.dispatch(420,"lobby-template-42","union.CUnionGetAllRoomMin",Map.of("clubId",42,"unionId",84));
        assertNotNull(lobby);
        var deleted=(java.util.List<?>)dispatch.dispatch(420,"delete-template-42","union.CUnionRoomCfgUpdate",Map.of("clubId",999,"unionId",84,"unionRoomCfgId",unionGameIndex,"status",2));
        assertTrue(deleted.isEmpty());
        assertTrue(service.get(42).templates().stream().anyMatch(template -> template.id().equals("club-template-1")),
                "联盟界面清理不得删除服务端保存的亲友圈模板");
    }
    @Test void clubTemplateCreateImmediatelyReturnsRenderableLobbyDesk() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:club_template_lobby;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);service.create("create-club-template",52,520,"Club");
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9007L);
        var payload=new LinkedHashMap<String,Object>(Map.of("clubId",52,"gameId",8,"gameCode","CD201",
                "roomName","普通亲友圈模板","playerCount",2,"roundCount",8));
        var created=(Map<?,?>)dispatch.dispatch(520,"save-club-template","club.CClubCreateGameSet",payload);
        var createdCfg=(Map<?,?>)created.get("bRoomConfigure");
        assertEquals(2,((Number)createdCfg.get("playerNum")).intValue());
        assertEquals("CD201",createdCfg.get("gameCode"));
        var lobby=(java.util.List<?>)dispatch.dispatch(520,"list-club-template","club.CClubRoomConfigItemList",Map.of("clubId",52));
        assertEquals(1,lobby.size());
        var desk=(Map<?,?>)lobby.get(0);
        assertEquals("CD201",desk.get("gameCode"));
        assertEquals(2,((Number)desk.get("playerNum")).intValue());
        assertEquals(8,((Number)desk.get("setCount")).intValue());
        var entry=dispatch.roomTemplateForEntry(520,52,((Number)desk.get("gameIndex")).longValue());
        assertEquals("CD201",entry.get("gameCode"),
                "模板主字段即使保存数字 gameId，点击空桌时也必须恢复稳定业务编码");
    }
    @Test void joinUnionAcceptsDisplayedUnionIdAndOwnerClubId() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:union_join_identifiers;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);service.create("create-owner-47",47,470,"UnionOwner");
        service.create("create-member-48",48,480,"Applicant");
        service.update("union-47",47,state->{var settings=new LinkedHashMap<>(state.settings());settings.put("unionId",847001);settings.put("unionSign",847002);settings.put("unionOwnerClubId",47);settings.put("unionJoin",0);
            return JdbcClubService.copy(state,state.name(),state.status(),state.members(),state.templates(),state.tables(),state.invites(),state.records(),state.ledger(),settings,state.applications(),state.memberExtras(),state.groupings(),state.roomBans(),state.viewedRooms());});
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9005L);
        assertEquals(1,((Number)dispatch.dispatch(480,"join-by-id-48","union.CUnionJoin",Map.of("clubId",48,"unionSign",847001))).intValue());
        assertEquals(1,((Number)dispatch.dispatch(480,"join-by-owner-48","union.CUnionJoin",Map.of("clubId",48,"unionSign",47))).intValue());
    }
    @Test void idInviteAddsMemberDirectlyUnderInviter() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:club_direct_invite;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);service.create("create-43",43,430,"DirectInvite");
        try(var connection=source.getConnection();var statement=connection.createStatement()) {
            statement.execute("INSERT INTO aoo_account_identity VALUES(431,'DISPLAY_ID','900431','ACTIVE')");
            statement.execute("INSERT INTO player_profile VALUES(431,'受邀玩家','',NULL)");
        }
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9002L);
        var found=(Map<?,?>)dispatch.dispatch(430,"find-431","club.CClubFindPIDInfo",Map.of("clubId",43,"pid","900431"));
        assertEquals(0,((Number)found.get("state")).intValue());
        var response=(Map<?,?>)dispatch.dispatch(430,"invite-431","club.CClubFindPIDAdd",Map.of("clubId",43,"pid","900431"));
        assertEquals(1,((Number)response.get("joined")).intValue());
        assertEquals("900431",response.get("playerId"));
        assertEquals(430L,((Number)response.get("upPlayerId")).longValue());
        var state=service.get(43);
        assertEquals("MEMBER",state.members().get(431L));
        assertEquals(430L,state.memberExtras().get(431L).upPlayerId());
        assertTrue(state.invites().isEmpty());
        assertTrue(dispatch.invitations(431).isEmpty());
    }
    @Test void clubCentIsIndependentForTheSamePlayerInDifferentClubs() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:club_cent_isolation;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);
        service.create("create-51",51,510,"Club A");service.create("create-52",52,520,"Club B");
        service.addInvitedMember("join-51",51,510,599);service.addInvitedMember("join-52",52,520,599);
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9010L);
        dispatch.dispatch(510,"cent-51","club.CClubCentUpdate",Map.of("clubId",51,"opPid",599,"type",0,"value",new BigDecimal("120")));
        dispatch.dispatch(520,"cent-52","club.CClubCentUpdate",Map.of("clubId",52,"opPid",599,"type",0,"value",new BigDecimal("35")));
        var first=(Map<?,?>)dispatch.dispatch(510,"info-51","club.CClubMemberClubCentInfo",Map.of("clubId",51,"opPid",599));
        var second=(Map<?,?>)dispatch.dispatch(520,"info-52","club.CClubMemberClubCentInfo",Map.of("clubId",52,"opPid",599));
        assertEquals(0,new BigDecimal("120").compareTo((BigDecimal)first.get("clubCent")));
        assertEquals(0,new BigDecimal("35").compareTo((BigDecimal)second.get("clubCent")));
    }
    @Test void clubDiamondAttentionSettingsArePersisted() throws Exception {
        JdbcDataSource source=new JdbcDataSource();source.setURL("jdbc:h2:mem:club_diamond_attention;MODE=MySQL;DB_CLOSE_DELAY=-1");
        schema(source);var mapper=new ObjectMapper().findAndRegisterModules();var clock=Clock.systemUTC();
        var service=new JdbcClubService(source,mapper,clock);service.create("create-61",61,610,"DiamondAttention");
        var dispatch=new ClubDispatchService(source,mapper,clock,()->9011L);
        var response=(Map<?,?>)dispatch.dispatch(610,"diamond-61","club.CClubChangeDiamondsAttention",
                Map.of("clubId",61,"diamondsAttentionMinister",100000,"diamondsAttentionAll",80000));
        assertEquals(100000L,((Number)response.get("diamondsAttentionMinister")).longValue());
        assertEquals(80000L,((Number)response.get("diamondsAttentionAll")).longValue());
        assertEquals(100000L,((Number)service.get(61).settings().get("diamondsAttentionMinister")).longValue());
        assertEquals(80000L,((Number)service.get(61).settings().get("diamondsAttentionAll")).longValue());
    }
    static void schema(JdbcDataSource source)throws Exception{try(Connection c=source.getConnection();var s=c.createStatement()){s.execute("CREATE TABLE aoo_club_state(club_id BIGINT PRIMARY KEY,state_json CLOB NOT NULL,row_version BIGINT NOT NULL,updated_at TIMESTAMP NOT NULL)");s.execute("CREATE TABLE aoo_club_write_idempotency(scope_key VARCHAR(160) PRIMARY KEY,response_json CLOB NOT NULL,created_at TIMESTAMP NOT NULL)");s.execute("CREATE TABLE aoo_club_member(club_id BIGINT NOT NULL,player_id BIGINT NOT NULL,member_status VARCHAR(16) NOT NULL,member_role VARCHAR(16) NOT NULL,online TINYINT NOT NULL DEFAULT 0,profile_payload CLOB NOT NULL,joined_at TIMESTAMP NOT NULL,updated_at TIMESTAMP NOT NULL,PRIMARY KEY(club_id,player_id))");s.execute("CREATE TABLE aoo_account_identity(account_id BIGINT NOT NULL,identity_type VARCHAR(24) NOT NULL,normalized_value VARCHAR(255) NOT NULL,status VARCHAR(16) NOT NULL)");s.execute("CREATE TABLE player_profile(player_id BIGINT PRIMARY KEY,nickname VARCHAR(64),avatar_url VARCHAR(255),gender_code VARCHAR(16))");}}
}
