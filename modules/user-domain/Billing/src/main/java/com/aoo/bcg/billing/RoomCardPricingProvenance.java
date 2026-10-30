package com.aoo.bcg.billing;

import java.time.Instant;
import java.util.Objects;
import com.aoo.bcg.billing.RoomCardPricingImportValidator.PriceKey;
import com.aoo.bcg.billing.RoomCardPricingImportValidator.SourceBinding;
import com.aoo.bcg.billing.RoomCardPricingImportValidator.SourceClass;

/** Immutable provenance only: no price, balance, or publication pointer authority. */
public record RoomCardPricingProvenance(long publicationId,String policyKey,String configurationVersion,
        String workbookManifestHash,String authorityHash,SourceBinding sourceBinding,
        String freeRef,Instant freeValidFrom,Instant freeValidUntil,boolean alwaysFree){
    public RoomCardPricingProvenance{
        if(publicationId<=0||!hash(policyKey)||!text(configurationVersion,128)
                ||!hash(workbookManifestHash)||!hash(authorityHash))throw new IllegalArgumentException("invalid pricing provenance");
        if(sourceBinding==null||sourceBinding.sourceClass()!=SourceClass.CURRENT_PUBLISHED
                ||sourceBinding.releaseId()<=0||sourceBinding.indexGeneration()<=0||!text(sourceBinding.sourceHash(),256))
            throw new IllegalArgumentException("provenance requires current published source binding");
        if(freeRef==null){
            if(alwaysFree||freeValidFrom!=null||freeValidUntil!=null)throw new IllegalArgumentException("free window requires explicit reference");
        }else{
            if(!text(freeRef,128))throw new IllegalArgumentException("invalid free reference");
            if(alwaysFree){
                if(freeValidFrom!=null||freeValidUntil!=null)throw new IllegalArgumentException("always-free cannot have time window");
            }else if(freeValidFrom==null||freeValidUntil==null||freeValidUntil.isBefore(freeValidFrom)
                    ||!milliseconds(freeValidFrom)||!milliseconds(freeValidUntil))
                throw new IllegalArgumentException("invalid free time window");
        }
    }
    public static String policyKey(PriceKey key){
        Objects.requireNonNull(key,"price key");var id=Objects.requireNonNull(key.identity(),"identity");
        return RoomChargeOperation.hash(id.gameId(),id.playVersion(),id.regionCode(),key.kind(),key.context(),
                key.playerCount(),key.measureKind(),key.measureValue(),key.payerMode(),key.tierMinor());
    }
    public void requireCurrent(SourceBinding expected){
        if(!sourceBinding.equals(expected))throw new IllegalArgumentException("published source binding changed");
    }
    /** Zero fees need a registered window; interval endpoints match the existing importer. */
    public void requireValidAt(Instant at,boolean zeroFee){
        Objects.requireNonNull(at,"at");
        if(zeroFee&&(freeRef==null||!alwaysFree&&(at.isBefore(freeValidFrom)||at.isAfter(freeValidUntil))))
            throw new IllegalArgumentException("free policy not effective");
    }
    private static boolean milliseconds(Instant value){return value.getNano()%1_000_000==0;}
    private static boolean hash(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
    private static boolean text(String value,int max){return value!=null&&!value.isBlank()&&value.length()<=max&&value.chars().noneMatch(Character::isISOControl);}
}
