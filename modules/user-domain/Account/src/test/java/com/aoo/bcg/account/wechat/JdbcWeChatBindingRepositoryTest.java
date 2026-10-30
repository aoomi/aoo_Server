package com.aoo.bcg.account.wechat;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import java.time.Instant;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

class JdbcWeChatBindingRepositoryTest {
    @Test void mapsMigrationColumnsAndEnforcesIdempotentUniqueClaims()throws Exception{
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:wechat;MODE=MySQL;DB_CLOSE_DELAY=-1");
        try(var c=ds.getConnection();var s=c.createStatement()){s.execute("CREATE TABLE aoo_account_identity(identity_id BIGINT AUTO_INCREMENT PRIMARY KEY,identity_type VARCHAR(24),normalized_value VARCHAR(255),value_hash CHAR(64),account_id BIGINT,verified BOOLEAN,status VARCHAR(16),created_at TIMESTAMP,updated_at TIMESTAMP,UNIQUE(identity_type,value_hash,status))");}
        var repo=new JdbcWeChatBindingRepository(ds);var value=new WeChatBindingService.Binding(7,"wx-app","openid-7","union-7",1,Instant.parse("2026-08-24T00:00:00Z"));
        assertEquals(WeChatBindingService.ClaimStatus.CREATED,repo.claim(value).status());assertEquals(value,repo.findByAccountId(7).orElseThrow());
        assertEquals(WeChatBindingService.ClaimStatus.IDEMPOTENT,repo.claim(value).status());
        assertEquals(WeChatBindingService.ClaimStatus.IDENTITY_OWNED_BY_ANOTHER_ACCOUNT,repo.claim(new WeChatBindingService.Binding(8,"wx-app","openid-7",null,1,value.boundAt())).status());
        assertFalse(repo.delete(7,"openid-7",2));assertTrue(repo.delete(7,"openid-7",1));assertTrue(repo.findByAccountId(7).isEmpty());
    }
}
