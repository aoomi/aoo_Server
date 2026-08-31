package com.aoo.bcg.common.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public record IdempotencyKey(String userId,String operation,long roomId,int roundNo,String requestId){
    public IdempotencyKey{if(userId==null||userId.isBlank()||operation==null||operation.isBlank()||roomId<0||roundNo<0||requestId==null||requestId.isBlank()||requestId.length()>128)throw new IllegalArgumentException("invalid idempotency scope");}
    public String storageKey(){try{String canonical=userId+'\n'+operation+'\n'+roomId+'\n'+roundNo+'\n'+requestId;return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
}
