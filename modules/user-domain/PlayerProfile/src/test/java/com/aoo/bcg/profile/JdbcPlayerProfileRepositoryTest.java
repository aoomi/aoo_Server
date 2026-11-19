package com.aoo.bcg.profile;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.aoo.bcg.profile.PlayerProfileModels.*;

class JdbcPlayerProfileRepositoryTest {
    JdbcDataSource ds;MutableClock clock;JdbcPlayerProfileRepository repo;
    @BeforeEach void setup()throws Exception{ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:profile"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1");try(var c=ds.getConnection();var s=c.createStatement()){
        s.execute("CREATE TABLE aoo_account(account_id BIGINT PRIMARY KEY)");s.execute("CREATE TABLE media_asset(id BIGINT PRIMARY KEY,kind VARCHAR(20),state VARCHAR(20))");s.execute("CREATE TABLE media_asset_access(asset_id BIGINT,owner_id BIGINT,PRIMARY KEY(asset_id,owner_id))");
        s.execute("CREATE TABLE player_profile(player_id BIGINT PRIMARY KEY,nickname VARCHAR(96) NOT NULL,avatar_asset_id BIGINT,gender_code VARCHAR(16),language_code VARCHAR(35),sound_enabled BOOLEAN,music_enabled BOOLEAN,vibration_enabled BOOLEAN,show_gender BOOLEAN,nickname_changed_at TIMESTAMP(3),avatar_changed_at TIMESTAMP(3),profile_version BIGINT,created_at TIMESTAMP(3),updated_at TIMESTAMP(3))");s.execute("INSERT INTO aoo_account VALUES(7),(8)");s.execute("INSERT INTO media_asset VALUES(91,'AVATAR','READY'),(92,'VOICE','READY'),(93,'AVATAR','OPEN')");s.execute("INSERT INTO media_asset_access VALUES(91,7),(92,7),(93,7)");}
        clock=new MutableClock(Instant.parse("2026-08-24T00:00:00Z"));repo=new JdbcPlayerProfileRepository(ds,clock);
    }
    @Test void closesProfilePreferencesPrivacyAndPublicRedaction(){Profile initial=repo.own(7);assertEquals(0,initial.version());assertEquals("Player7",initial.nickname());
        Profile profile=repo.updateProfile(7,new ProfilePatch(0," Alice ",91L,Gender.FEMALE));assertEquals(1,profile.version());assertEquals("Alice",profile.nickname());
        Profile prefs=repo.updatePreferences(7,new PreferencePatch(1,"zh-Hans",false,false,true));assertEquals(2,prefs.version());assertFalse(prefs.soundEnabled());
        PublicProfile hidden=repo.visible(8,7);assertNull(hidden.gender());assertEquals(91,hidden.avatarAssetId());
        repo.updatePrivacy(7,new PrivacyPatch(2,true));PublicProfile shown=repo.visible(8,7);assertEquals(Gender.FEMALE,shown.gender());
    }
    @Test void rejectsVersionConflictFrequencyAndNonAuthoritativeAvatar(){repo.own(7);repo.updateProfile(7,new ProfilePatch(0,"Alice",91L,null));
        assertThrows(PlayerProfileRepository.Conflict.class,()->repo.updatePrivacy(7,new PrivacyPatch(0,true)));
        assertThrows(PlayerProfileRepository.TooFrequent.class,()->repo.updateProfile(7,new ProfilePatch(1,"Alice Two",null,null)));
        clock.advance(Duration.ofDays(7));assertThrows(IllegalArgumentException.class,()->repo.updateProfile(7,new ProfilePatch(1,"Alice Two",92L,null)));
        assertThrows(IllegalArgumentException.class,()->repo.updateProfile(7,new ProfilePatch(1,"Alice Two",93L,null)));
    }
    static final class MutableClock extends Clock{Instant now;MutableClock(Instant now){this.now=now;}void advance(Duration d){now=now.plus(d);}@Override public ZoneId getZone(){return ZoneOffset.UTC;}@Override public Clock withZone(ZoneId z){return this;}@Override public Instant instant(){return now;}}
}
