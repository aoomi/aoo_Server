package com.aoo.bcg.bootstrap;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.h2.jdbcx.JdbcDataSource;

class ClubBootstrapIntegrationTest {
    @Test void productionBootstrapMountsPersistentClubRoute() throws Exception {
        String url="jdbc:h2:mem:club_bootstrap;MODE=MySQL;DB_CLOSE_DELAY=-1";
        JdbcDataSource source=new JdbcDataSource();source.setURL(url);source.setUser("sa");
        try(var c=source.getConnection();var s=c.createStatement()){s.execute("CREATE TABLE aoo_club_state(club_id BIGINT PRIMARY KEY,state_json CLOB NOT NULL,row_version BIGINT NOT NULL,updated_at TIMESTAMP NOT NULL)");s.execute("CREATE TABLE aoo_club_write_idempotency(scope_key VARCHAR(160) PRIMARY KEY,response_json CLOB NOT NULL,created_at TIMESTAMP NOT NULL)");}
        var server=BootstrapAPP.startClub(source,0);try{int port=server.getAddress().getPort();HttpClient client=HttpClient.newHttpClient();HttpRequest request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/v1/clubs")).header("Idempotency-Key","http-create").POST(HttpRequest.BodyPublishers.ofString("action=create&clubId=55&actorId=550&name=HTTP")).build();assertEquals(200,client.send(request,HttpResponse.BodyHandlers.ofString()).statusCode());try(var c=source.getConnection();var q=c.prepareStatement("SELECT state_json FROM aoo_club_state WHERE club_id=55");var rows=q.executeQuery()){assertTrue(rows.next());assertTrue(rows.getString(1).contains("HTTP"));}}finally{server.stop(0);}
    }
}
