package com.aoo.bcg.account.callback;

import com.aoo.bcg.account.wechat.WeChatHttpRoutes;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import javax.sql.DataSource;

/** Durably accepts an already verified plaintext callback; downstream processing can claim the receipt. */
public final class JdbcWeChatCallbackHandler implements WeChatHttpRoutes.CallbackHandler {
    private final DataSource source;
    public JdbcWeChatCallbackHandler(DataSource source){this.source=Objects.requireNonNull(source);}
    @Override public WeChatHttpRoutes.CallbackResponse handle(WeChatCallbackVerifier.VerifiedCallback verified,byte[] body)throws Exception{
        if(body==null||body.length==0)throw new IllegalArgumentException("empty WeChat callback");String payload=sha256(body);String event=sha256((verified.occurredAt()+":"+verified.nonce()+":"+payload).getBytes(StandardCharsets.UTF_8));
        String sql="INSERT INTO aoo_external_callback_receipt(provider,event_id,payload_sha256,schema_version,processed_at,result_code) VALUES('wechat-event',?,?,'wechat/plain-v1',CURRENT_TIMESTAMP(3),'ACCEPTED')";
        try(var c=source.getConnection();var p=c.prepareStatement(sql)){p.setString(1,event);p.setString(2,payload);p.executeUpdate();}
        return new WeChatHttpRoutes.CallbackResponse(200,"success");
    }
    private static String sha256(byte[] value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));}catch(Exception e){throw new IllegalStateException(e);}}
}
