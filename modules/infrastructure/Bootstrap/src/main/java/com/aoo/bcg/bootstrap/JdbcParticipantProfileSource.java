package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.ParticipantProfileSource;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;

final class JdbcParticipantProfileSource implements ParticipantProfileSource {
    private final DataSource source;
    JdbcParticipantProfileSource(DataSource source) { this.source=Objects.requireNonNull(source); }
    @Override public String nickname(long accountId) {
        String sql="SELECT p.nickname,a.guest FROM aoo_account a LEFT JOIN player_profile p ON p.player_id=a.account_id WHERE a.account_id=?";
        try(var connection=source.getConnection();var query=connection.prepareStatement(sql)){
            query.setLong(1,accountId);try(var rows=query.executeQuery()){
                if(!rows.next())return null;
                String name=rows.getString("nickname");return name==null||name.isBlank()?(rows.getBoolean("guest")?"游客":"玩家")+accountId:name;
            }
        }catch(SQLException failure){throw new IllegalStateException("participant profile lookup failed",failure);}
    }
}
