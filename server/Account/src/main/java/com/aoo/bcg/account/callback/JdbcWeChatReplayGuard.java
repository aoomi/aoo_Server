package com.aoo.bcg.account.callback;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import javax.sql.DataSource;

/** Cross-node replay claim for signed WeChat callbacks. */
public final class JdbcWeChatReplayGuard implements WeChatCallbackVerifier.ReplayGuard {
    private final DataSource source;
    public JdbcWeChatReplayGuard(DataSource source){this.source=Objects.requireNonNull(source);}
    @Override public boolean claim(String replayKey,Instant retainUntil){String digest=sha256(replayKey);String sql="INSERT INTO aoo_external_callback_receipt(provider,event_id,payload_sha256,schema_version,processed_at,result_code) VALUES('wechat-callback',?,?,'wechat/v1',CURRENT_TIMESTAMP(3),'VERIFIED')";try(var c=source.getConnection();var p=c.prepareStatement(sql)){p.setString(1,digest);p.setString(2,digest);p.executeUpdate();return true;}catch(SQLIntegrityConstraintViolationException duplicate){return false;}catch(Exception e){throw new IllegalStateException("WeChat callback replay persistence unavailable",e);}}
    private static String sha256(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
