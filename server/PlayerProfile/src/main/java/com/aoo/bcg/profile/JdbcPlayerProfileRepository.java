package com.aoo.bcg.profile;

import javax.sql.DataSource;
import java.sql.*;
import java.time.*;
import java.util.*;
import static com.aoo.bcg.profile.PlayerProfileModels.*;

public final class JdbcPlayerProfileRepository implements PlayerProfileRepository {
    public static final Duration NICKNAME_INTERVAL=Duration.ofDays(7), AVATAR_INTERVAL=Duration.ofHours(1);
    private final DataSource source; private final Clock clock;
    public JdbcPlayerProfileRepository(DataSource source,Clock clock){this.source=Objects.requireNonNull(source);this.clock=Objects.requireNonNull(clock);}
    @Override public Profile own(long id){try(var c=source.getConnection()){return load(c,id,false);}catch(SQLException e){throw db(e);}}
    @Override public PublicProfile visible(long viewer,long id){Profile p=own(id);return new PublicProfile(id,p.nickname(),p.avatarAssetId(),p.showGender()?p.gender():null);}
    @Override public Profile updateProfile(long id,ProfilePatch patch){Objects.requireNonNull(patch);return mutate(id,patch.expectedVersion(),(c,current,now)->{
        String nickname=patch.nickname()==null?current.nickname():normalizeNickname(patch.nickname());
        Gender gender=patch.gender()==null?current.gender():patch.gender();
        Long avatar=patch.avatarAssetId()==null?current.avatarAssetId():patch.avatarAssetId();
        boolean nicknameChanged=!nickname.equals(current.nickname()),avatarChanged=!Objects.equals(avatar,current.avatarAssetId());
        Times times=times(c,id);if(nicknameChanged)enforce("nickname",times.nickname(),NICKNAME_INTERVAL,now);if(avatarChanged){validateAvatar(c,id,avatar);enforce("avatar",times.avatar(),AVATAR_INTERVAL,now);}
        try(var p=c.prepareStatement("UPDATE player_profile SET nickname=?,avatar_asset_id=?,gender_code=?,nickname_changed_at=?,avatar_changed_at=?,profile_version=profile_version+1,updated_at=? WHERE player_id=? AND profile_version=?")){
            p.setString(1,nickname);if(avatar==null)p.setNull(2,Types.BIGINT);else p.setLong(2,avatar);p.setString(3,gender.name());timestamp(p,4,nicknameChanged?now:times.nickname());timestamp(p,5,avatarChanged?now:times.avatar());p.setTimestamp(6,Timestamp.from(now));p.setLong(7,id);p.setLong(8,patch.expectedVersion());check(p.executeUpdate());
        } return null;
    });}
    @Override public Profile updatePreferences(long id,PreferencePatch patch){Objects.requireNonNull(patch);return mutate(id,patch.expectedVersion(),(c,v,now)->{
        String language=patch.language()==null?v.language():normalizeLanguage(patch.language());boolean sound=patch.soundEnabled()==null?v.soundEnabled():patch.soundEnabled();boolean music=patch.musicEnabled()==null?v.musicEnabled():patch.musicEnabled();boolean vibration=patch.vibrationEnabled()==null?v.vibrationEnabled():patch.vibrationEnabled();
        try(var p=c.prepareStatement("UPDATE player_profile SET language_code=?,sound_enabled=?,music_enabled=?,vibration_enabled=?,profile_version=profile_version+1,updated_at=? WHERE player_id=? AND profile_version=?")){p.setString(1,language);p.setBoolean(2,sound);p.setBoolean(3,music);p.setBoolean(4,vibration);p.setTimestamp(5,Timestamp.from(now));p.setLong(6,id);p.setLong(7,patch.expectedVersion());check(p.executeUpdate());}return null;
    });}
    @Override public Profile updatePrivacy(long id,PrivacyPatch patch){Objects.requireNonNull(patch);return mutate(id,patch.expectedVersion(),(c,v,now)->{
        boolean gender=patch.showGender()==null?v.showGender():patch.showGender();
        try(var p=c.prepareStatement("UPDATE player_profile SET show_gender=?,profile_version=profile_version+1,updated_at=? WHERE player_id=? AND profile_version=?")){p.setBoolean(1,gender);p.setTimestamp(2,Timestamp.from(now));p.setLong(3,id);p.setLong(4,patch.expectedVersion());check(p.executeUpdate());}return null;
    });}
    private Profile mutate(long id,long version,Mutation mutation){if(version<0)throw new IllegalArgumentException("expectedVersion must be non-negative");try(var c=source.getConnection()){c.setAutoCommit(false);try{ensure(c,id);Profile current=load(c,id,true);if(current.version()!=version)throw new Conflict("profile version conflict");mutation.run(c,current,clock.instant());Profile result=load(c,id,false);c.commit();return result;}catch(RuntimeException|SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}catch(SQLException e){throw db(e);}}
    private void ensure(Connection c,long id)throws SQLException{if(id<=0)throw new IllegalArgumentException("playerId must be positive");try(var p=c.prepareStatement("INSERT INTO player_profile(player_id,nickname,gender_code,language_code,sound_enabled,music_enabled,vibration_enabled,show_gender,profile_version,created_at,updated_at) SELECT account_id,?, 'UNSPECIFIED','en',TRUE,TRUE,TRUE,FALSE,0,?,? FROM aoo_account WHERE account_id=? AND NOT EXISTS(SELECT 1 FROM player_profile WHERE player_id=?)")){Instant now=clock.instant();p.setString(1,"Player"+id);p.setTimestamp(2,Timestamp.from(now));p.setTimestamp(3,Timestamp.from(now));p.setLong(4,id);p.setLong(5,id);p.executeUpdate();}}
    private Profile load(Connection c,long id,boolean lock)throws SQLException{ensure(c,id);String sql="SELECT player_id,nickname,avatar_asset_id,gender_code,language_code,sound_enabled,music_enabled,vibration_enabled,show_gender,profile_version FROM player_profile WHERE player_id=?"+(lock?" FOR UPDATE":"");try(var p=c.prepareStatement(sql)){p.setLong(1,id);try(var r=p.executeQuery()){if(!r.next())throw new NotFound("player profile not found");long avatar=r.getLong(3);boolean absent=r.wasNull();return new Profile(r.getLong(1),r.getString(2),absent?null:avatar,Gender.valueOf(r.getString(4)),r.getString(5),r.getBoolean(6),r.getBoolean(7),r.getBoolean(8),r.getBoolean(9),r.getLong(10));}}}
    private void validateAvatar(Connection c,long owner,Long asset)throws SQLException{if(asset==null)return;try(var p=c.prepareStatement("SELECT 1 FROM media_asset a JOIN media_asset_access x ON x.asset_id=a.id WHERE a.id=? AND a.state='READY' AND a.kind='AVATAR' AND x.owner_id=?")){p.setLong(1,asset);p.setLong(2,owner);try(var r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("avatarAssetId is not an owned READY Media avatar");}}}
    private Times times(Connection c,long id)throws SQLException{try(var p=c.prepareStatement("SELECT nickname_changed_at,avatar_changed_at FROM player_profile WHERE player_id=?")){p.setLong(1,id);try(var r=p.executeQuery()){r.next();return new Times(instant(r,1),instant(r,2));}}}
    private static void enforce(String field,Instant changed,Duration interval,Instant now){if(changed==null)return;Instant allowed=changed.plus(interval);if(now.isBefore(allowed))throw new TooFrequent(field+" may not be changed yet",Math.max(1,Duration.between(now,allowed).toSeconds()));}
    private static String normalizeNickname(String v){String n=v==null?"":v.strip().replaceAll("\\s+"," ");int count=n.codePointCount(0,n.length());if(count<2||count>24||n.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("nickname must contain 2-24 safe characters");return n;}
    private static String normalizeLanguage(String v){String n=v==null?"":v.strip();if(!n.matches("[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*"))throw new IllegalArgumentException("invalid language tag");return n;}
    private static Instant instant(ResultSet r,int i)throws SQLException{Timestamp t=r.getTimestamp(i);return t==null?null:t.toInstant();}private static void timestamp(PreparedStatement p,int i,Instant v)throws SQLException{if(v==null)p.setNull(i,Types.TIMESTAMP);else p.setTimestamp(i,Timestamp.from(v));}private static void check(int n){if(n!=1)throw new Conflict("profile version conflict");}private static IllegalStateException db(SQLException e){return new IllegalStateException("player profile repository failure",e);}
    private record Times(Instant nickname,Instant avatar){} @FunctionalInterface private interface Mutation{Void run(Connection c,Profile p,Instant now)throws SQLException;}
}
