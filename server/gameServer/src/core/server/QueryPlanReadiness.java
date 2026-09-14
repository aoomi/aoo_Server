package core.server;

import java.sql.Connection;
import java.util.List;
import java.util.Set;

/** Runs optimizer plans against the real production schema before accepting traffic. */
final class QueryPlanReadiness {
    private QueryPlanReadiness() {}
    private record Plan(String name,String sql,Set<String> acceptedKeys) {}
    private static final List<Plan> PLANS=List.of(
            new Plan("idempotency-result","SELECT response_payload FROM aoo_business_idempotency FORCE INDEX (PRIMARY) WHERE request_id='plan' AND status='COMPLETED'",Set.of("PRIMARY")),
            new Plan("club-member-page","SELECT player_id FROM aoo_club_member FORCE INDEX (idx_club_member_page) WHERE club_id=1 AND member_status='ACTIVE' AND player_id>0 ORDER BY player_id LIMIT 100",Set.of("idx_club_member_page")),
            new Plan("room-event-player-view","SELECT event_sequence FROM aoo_room_event FORCE INDEX (idx_room_event_player_view) WHERE room_id=1 AND event_sequence>0 AND ((visibility='PUBLIC' AND owner_player_id=0) OR (visibility='PLAYER_PRIVATE' AND owner_player_id=1)) ORDER BY event_sequence LIMIT 100",Set.of("idx_room_event_player_view")),
            new Plan("outbox-claim","SELECT event_id FROM aoo_outbox FORCE INDEX (idx_outbox_claim) WHERE status='PENDING' AND next_attempt_at<=CURRENT_TIMESTAMP(3) ORDER BY created_at LIMIT 100",Set.of("idx_outbox_claim")),
            new Plan("ledger-business","SELECT balance_after FROM aoo_ledger FORCE INDEX (uk_ledger_business) WHERE business_id='plan'",Set.of("uk_ledger_business")),
            new Plan("template-page","SELECT template_code,template_version FROM aoo_room_template FORCE INDEX (idx_template_active_page) WHERE club_id=1 AND status='ACTIVE' AND game_id=62 ORDER BY template_code,template_version LIMIT 100",Set.of("idx_template_active_page")),
            new Plan("play-region","SELECT game_id,play_version FROM aoo_play_variant FORCE INDEX (idx_play_active_region) WHERE region_code='CN-SC-NJ' AND status='ACTIVE' ORDER BY game_id,play_version",Set.of("idx_play_active_region")));

    static void verify(Connection connection)throws Exception{
        for(Plan plan:PLANS){String json;try(var statement=connection.createStatement();var rows=statement.executeQuery("EXPLAIN FORMAT=JSON "+plan.sql())){if(!rows.next())throw new IllegalStateException("empty EXPLAIN: "+plan.name());json=rows.getString(1);}
            if(json.contains("no matching row in const table")) continue;
            boolean indexed=plan.acceptedKeys().stream().anyMatch(key->json.contains("\"key\": \""+key+"\"")||json.contains("\"key\":\""+key+"\""));
            if(!indexed||json.contains("\"access_type\": \"ALL\""))throw new IllegalStateException("unsafe query plan: "+plan.name());
        }
    }
}
