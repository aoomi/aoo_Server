package com.aoo.bcg.social;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;

class SocialServiceIntegrationTest {
    private SocialService service;private JdbcSocialRepository repository;
    @BeforeEach void setUp()throws Exception{
        JdbcDataSource ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:social"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try(Connection c=ds.getConnection()){
            c.createStatement().execute("CREATE TABLE social_friend_request(id BIGINT AUTO_INCREMENT PRIMARY KEY,requester_id BIGINT NOT NULL,recipient_id BIGINT NOT NULL,status VARCHAR(16) NOT NULL,created_at TIMESTAMP NOT NULL,decided_at TIMESTAMP NULL)");
            c.createStatement().execute("CREATE TABLE social_friendship(user_low BIGINT NOT NULL,user_high BIGINT NOT NULL,created_at TIMESTAMP NOT NULL,PRIMARY KEY(user_low,user_high))");
            c.createStatement().execute("CREATE TABLE social_block(blocker_id BIGINT NOT NULL,blocked_id BIGINT NOT NULL,created_at TIMESTAMP NOT NULL,PRIMARY KEY(blocker_id,blocked_id))");
            c.createStatement().execute("CREATE TABLE social_presence(account_id BIGINT PRIMARY KEY,state VARCHAR(16),room_id BIGINT,visibility VARCHAR(16),last_seen_at TIMESTAMP)");
            c.createStatement().execute("CREATE TABLE social_notification(id BIGINT AUTO_INCREMENT PRIMARY KEY,recipient_id BIGINT,type VARCHAR(32),dedupe_key VARCHAR(128),payload VARCHAR(4096),created_at TIMESTAMP,UNIQUE(recipient_id,dedupe_key))");
            c.createStatement().execute("CREATE TABLE social_mail(id BIGINT AUTO_INCREMENT PRIMARY KEY,recipient_id BIGINT,dedupe_key VARCHAR(128),subject VARCHAR(256),body VARCHAR(4096),created_at TIMESTAMP,UNIQUE(recipient_id,dedupe_key))");
            c.createStatement().execute("CREATE TABLE social_read_cursor(account_id BIGINT,stream VARCHAR(16),cursor_id BIGINT,updated_at TIMESTAMP,PRIMARY KEY(account_id,stream))");
            c.createStatement().execute("CREATE TABLE social_notice(id BIGINT AUTO_INCREMENT PRIMARY KEY,title VARCHAR(256),content VARCHAR(4096),starts_at TIMESTAMP,ends_at TIMESTAMP,published BOOLEAN,updated_at TIMESTAMP)");
        }
        Clock clock=Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"),ZoneOffset.UTC);repository=new JdbcSocialRepository(ds,clock);service=new SocialService(repository);
    }
    @Test void closesFriendLifecycleAndEnforcesBlockPrivacy(){
        var request=service.request(10,20,"friend-1");assertEquals("PENDING",request.status());
        assertThrows(SecurityException.class,()->service.decide(30,request.id(),true,"bad"));
        assertEquals("ACCEPTED",service.decide(20,request.id(),true,"answer-1").status());assertTrue(repository.areFriends(10,20));
        service.presence(10,"ONLINE",77L,"FRIENDS");assertEquals(77L,service.presence(20,10).roomId());
        assertEquals(1,service.friends(20).size());assertEquals("ONLINE",service.friends(20).get(0).state());
        service.block(20,10);assertFalse(repository.areFriends(10,20));assertThrows(SecurityException.class,()->service.presence(10,20));assertThrows(SecurityException.class,()->service.roomInvite(10,20,77,"invite-blocked"));
        service.unblock(20,10);var rejected=service.request(10,20,"friend-2");assertEquals("REJECTED",service.decide(20,rejected.id(),false,"answer-2").status());
    }
    @Test void listsPendingRequestsOnlyForRecipient(){
        service.request(10,20,"pending-1");service.request(30,20,"pending-2");service.request(40,50,"other");
        assertEquals(2,service.pendingRequests(20).size());assertTrue(service.pendingRequests(10).isEmpty());
    }
    @Test void deduplicatesNotificationsAndReplaysOfflineFromDurableCursor(){
        var r=service.request(1,2,"same-event");var initial=service.notifications(2,0,20);assertEquals(1,initial.items().size());
        assertEquals(r.id(),service.request(1,2,"same-event").id());assertEquals(1,service.notifications(2,0,20).items().size());
        long cursor=initial.nextCursor();assertEquals(cursor,service.read(2,"NOTIFICATION",cursor));assertEquals(cursor,service.notifications(2,0,20).readCursor());
        assertTrue(service.notifications(2,cursor,20).items().isEmpty());
    }
    @Test void systemRoleOwnsSystemMessageAndMail(){
        var user=new SocialPrincipal(1,"USER");var system=new SocialPrincipal(99,"SYSTEM");
        assertThrows(SecurityException.class,()->service.systemMessage(user,2,"s1","{}"));
        var first=service.systemMessage(system,2,"s1","{\"notice\":true}");assertEquals(first.id(),service.systemMessage(system,2,"s1","ignored").id());
        var mail=service.systemMail(system,2,"m1","Maintenance","Complete");assertEquals(mail.id(),service.systemMail(system,2,"m1","ignored","ignored").id());
        assertEquals(1,service.mails(2,0,10).items().size());
        assertEquals(mail,service.mail(2,mail.id()));
        assertThrows(IllegalArgumentException.class,()->service.mail(3,mail.id()));
        assertEquals(1,service.redDots(2).mail());
        service.read(2,"MAIL",mail.id());assertEquals(0,service.redDots(2).mail());
    }
}
