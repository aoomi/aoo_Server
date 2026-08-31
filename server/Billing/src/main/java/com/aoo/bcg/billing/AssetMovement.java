package com.aoo.bcg.billing;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Immutable two-leg movement used for audit/export; reversal references the original business id. */
public record AssetMovement(String businessId,Type type,AssetAccount source,AssetAccount target,long quantity,
        long sourceBefore,long sourceAfter,long targetBefore,long targetAfter,String reasonCode,
        String originalBusinessId,Instant createdAt){
    public AssetMovement{
        if(businessId==null||!businessId.matches("[A-Za-z0-9_.:-]{1,128}")||type==null||source==null||target==null
                ||source.equals(target)||quantity<=0||sourceBefore<quantity||Math.subtractExact(sourceBefore,quantity)!=sourceAfter
                ||Math.addExact(targetBefore,quantity)!=targetAfter||reasonCode==null||!reasonCode.matches("[A-Z0-9_.-]{1,64}")||createdAt==null)
            throw new IllegalArgumentException("invalid asset movement");
        if(source.kind()!=target.kind()||!source.assetCode().equals(target.assetCode()))throw new IllegalArgumentException("asset legs differ");
        if(type==Type.REVERSAL&&(originalBusinessId==null||originalBusinessId.isBlank()))throw new IllegalArgumentException("reversal must reference original movement");
        if(type!=Type.REVERSAL&&originalBusinessId!=null)throw new IllegalArgumentException("unexpected original movement");
    }
    public List<Leg> legs(){return List.of(new Leg(source,-quantity,sourceBefore,sourceAfter),new Leg(target,quantity,targetBefore,targetAfter));}
    public boolean balanced(){return legs().stream().mapToLong(Leg::delta).sum()==0;}
    public enum Type{TRANSFER,HOLD,RELEASE,CONSUME,REFUND,REWARD,GIFT,REVERSAL}
    public record Leg(AssetAccount account,long delta,long balanceBefore,long balanceAfter){public Leg{Objects.requireNonNull(account);if(delta==0||Math.addExact(balanceBefore,delta)!=balanceAfter)throw new IllegalArgumentException("invalid movement leg");}}
}
