package com.aoo.bcg.billing;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.aoo.bcg.billing.RoomCardPricingImportValidator.*;

class RoomCardPricingImportValidatorTest {
    final RoomCardPricingImportValidator validator=new RoomCardPricingImportValidator(Clock.fixed(Instant.parse("2026-10-16T00:00:00Z"),ZoneOffset.UTC));
    final IdentityKey id=new IdentityKey(991001,"synthetic991001","v1","TEST-ONLY");
    final SourceBinding binding=new SourceBinding(SourceClass.CURRENT_PUBLISHED,1,2,"legacy-opaque-source");
    PriceKey key(String kind,String context,int value,String payer) {return new PriceKey(id,kind,context,2,"CARRY".equals(kind)?"CARRY_MINOR_PER_TIER":"ROUND_COUNT",value,payer,"CARRY".equals(kind)?value:0);}
    AuthoritySnapshot authority(List<PriceKey> keys,SourceBinding source,List<FreePolicy> free) {
        var a=new AuthoritySnapshot(1,"TEST_ONLY","",List.of(new Identity(id,source)),keys,Set.of("XQP_R5"),free);
        return new AuthoritySnapshot(1,a.environment(),authorityHash(a),a.currentIdentities(),keys,a.refundRefs(),free);
    }
    List<Object> row(String kind,String context,String payer,int value,Object amount) {
        return new ArrayList<>(Arrays.asList("TEST-ONLY","synthetic991001",991001,"v1",kind,context,2,value,"CARRY".equals(kind)?"CARRY_MINOR_PER_TIER":"LOOP".equals(kind)?"ROUND_PER_SESSION":"ROUND_COUNT",payer,amount,"ROOM_CARD","CARRY".equals(kind)?"CEILING":"EXACT",null,"XQP_R5","confirmed-v1","ENABLED","synthetic-only","CONFIRMED",null));
    }
    ImportDocument document(List<List<Object>> rows) {
        var sheets=new ArrayList<ImportedSheet>();
        for(var entry:Map.of("固定局数","FIXED","循环场次","LOOP","带入额度","CARRY").entrySet()) {
            var rr=new ArrayList<RawRow>();int n=2;for(var row:rows)if(entry.getValue().equals(row.get(4)))rr.add(new RawRow(n++,row,false));
            sheets.add(new ImportedSheet(entry.getKey(),HEADERS,rr,List.of()));
        }
        sheets.add(new ImportedSheet("说明来源",List.of(),List.of(),List.of(List.of("退款来源","XQP_R5"))));
        return new ImportDocument(1,List.of(new ImportedBook("synthetic.xlsx","a".repeat(64),sheets)));
    }
    ValidationResult run(List<List<Object>> rows,AuthoritySnapshot a) {return validator.validate(document(rows),a,Profile.TEST_ONLY);}
    void rejected(List<Object> row,AuthoritySnapshot a,String code) {var result=run(List.of(row),a);assertFalse(result.publishable());assertTrue(result.issues().stream().anyMatch(x->code.equals(x.code())),result.issues().toString());}

    @Test void independentAaPriceAndStableCanonicalOrder() {
        var a=authority(List.of(key("FIXED","HALL",8,"OWNER"),key("FIXED","HALL",8,"AA")),binding,List.of());
        var owner=row("FIXED","HALL","OWNER",8,7);var aa=row("FIXED","HALL","AA",8,3);
        var result=run(List.of(owner,aa),a);assertTrue(result.publishable(),result.issues().toString());
        assertEquals(3,result.eligibleRows().stream().filter(x->x.key().payerMode().equals("AA")).findFirst().orElseThrow().unitCostMinor());
        assertEquals("PER_PLAYER",result.eligibleRows().stream().filter(x->x.key().payerMode().equals("AA")).findFirst().orElseThrow().priceBasis());
        assertEquals(result.canonicalHash(),run(List.of(aa,owner),a).canonicalHash());
        aa.set(10,4);assertNotEquals(result.canonicalHash(),run(List.of(owner,aa),a).canonicalHash());
    }
    @Test void organizationOwnerIsContextSpecific() {
        for(String context:List.of("CLUB","UNION")) {var a=authority(List.of(key("FIXED",context,8,context)),binding,List.of());assertTrue(run(List.of(row("FIXED",context,"ORGANIZATION_OWNER",8,7)),a).publishable());rejected(row("FIXED",context,"OWNER",8,7),a,"PAYER_INVALID");}
    }
    @Test void extraAndMissingEffectiveCombinationsAreBothRejected() {
        var a=authority(List.of(key("FIXED","HALL",8,"OWNER"),key("FIXED","HALL",8,"AA")),binding,List.of());
        var result=run(List.of(row("FIXED","HALL","OWNER",8,7)),a);assertEquals(1,result.missingKeys().size());assertFalse(result.publishable());
        rejected(row("FIXED","HALL","OWNER",12,7),a,"EXTRA_EFFECTIVE_COMBINATION");
        rejected(row("LOOP","HALL","OWNER",8,7),a,"EXTRA_EFFECTIVE_COMBINATION");
        var cross=row("FIXED","HALL","OWNER",8,7);cross.set(0,"OTHER");rejected(cross,a,"IDENTITY_NOT_AUTHORIZED");
    }
    @Test void authoritySourceClassificationAndBindingAreMandatory() {
        var k=key("FIXED","HALL",8,"OWNER");var row=row("FIXED","HALL","OWNER",8,7);
        for(var cls:List.of(SourceClass.REGISTERED_ONLY,SourceClass.GENERATED_ONLY,SourceClass.COMPATIBILITY))rejected(row,authority(List.of(k),new SourceBinding(cls,1,2,"s"),List.of()),"AUTHORITY_NOT_CURRENT_PUBLISHED");
        for(var b:List.of(new SourceBinding(SourceClass.CURRENT_PUBLISHED,0,2,"s"),new SourceBinding(SourceClass.CURRENT_PUBLISHED,1,0,"s"),new SourceBinding(SourceClass.CURRENT_PUBLISHED,1,2,"")))rejected(row,authority(List.of(k),b,List.of()),"AUTHORITY_NOT_CURRENT_PUBLISHED");
        var a=authority(List.of(k),binding,List.of());assertTrue(run(List.of(row),a).publishable());assertFalse(validator.validate(document(List.of(row)),a,Profile.PRODUCTION).publishable());
        var changed=authority(List.of(k),new SourceBinding(SourceClass.CURRENT_PUBLISHED,2,2,"s"),List.of());assertNotEquals(a.sourceHash(),changed.sourceHash());
        var conflict=new AuthoritySnapshot(1,"TEST_ONLY","",List.of(new Identity(id,binding),new Identity(id,binding)),List.of(k),Set.of("XQP_R5"),List.of());
        assertTrue(run(List.of(row),conflict).issues().stream().anyMatch(x->x.code().equals("AUTHORITY_IDENTITY_CONFLICT")));
        var tampered=new AuthoritySnapshot(1,"TEST_ONLY",a.sourceHash(),changed.currentIdentities(),a.effectiveKeys(),a.refundRefs(),a.freePolicies());rejected(row,tampered,"AUTHORITY_HASH_MISMATCH");
    }
    @Test void originalUnitIntegersRejectLossAndUnconfirmedPrices() {
        var a=authority(List.of(key("FIXED","HALL",8,"OWNER")),binding,List.of());
        for(Object amount:Arrays.asList(null,-1,1.5,"01","1.0","9223372036854775808",9007199254740992d,9007199254740992L))rejected(row("FIXED","HALL","OWNER",8,amount),a,"INVALID_AMOUNT");
        assertEquals(Long.MAX_VALUE,run(List.of(row("FIXED","HALL","OWNER",8,"9223372036854775807")),a).eligibleRows().getFirst().unitCostMinor());
        var pending=row("FIXED","HALL","OWNER",8,7);pending.set(18,"XQP参考、待确认");rejected(pending,a,"PRICE_UNCONFIRMED");
        var winner=row("FIXED","HALL","WINNER",8,7);rejected(winner,a,"PAYER_INVALID");
        var crystal=row("FIXED","HALL","OWNER",8,7);crystal.set(11,"CRYSTAL");rejected(crystal,a,"CURRENCY_INVALID");
    }
    @Test void freePriceRequiresExplicitCurrentPolicy() {
        var k=key("FIXED","HALL",8,"OWNER");var zero=row("FIXED","HALL","OWNER",8,0);zero.set(13,"free");
        rejected(zero,authority(List.of(k),binding,List.of()),"FREE_POLICY_INACTIVE");
        rejected(zero,authority(List.of(k),binding,List.of(new FreePolicy("free","2026-01-01T00:00:00Z","2026-10-15T00:00:00Z",false))),"FREE_POLICY_INACTIVE");
        assertTrue(run(List.of(zero),authority(List.of(k),binding,List.of(new FreePolicy("free",null,null,true)))).publishable());
        assertTrue(run(List.of(zero),authority(List.of(k),binding,List.of(new FreePolicy("free","2026-10-16T00:00:00Z","2026-10-17T00:00:00Z",false)))).publishable());
    }
    @Test void invalidFreePolicyDatesRemainRejected() {
        var k=key("FIXED","HALL",8,"OWNER");var zero=row("FIXED","HALL","OWNER",8,0);zero.set(13,"free");
        for(var policy:List.of(
                new FreePolicy("free","invalid","2026-10-17T00:00:00Z",false),
                new FreePolicy("free","2026-10-15T00:00:00Z","invalid",false),
                new FreePolicy("free",null,"2026-10-17T00:00:00Z",false),
                new FreePolicy("free","2026-10-15T00:00:00Z",null,false),
                new FreePolicy("free","2026-10-17T00:00:00Z","2026-10-15T00:00:00Z",false))) {
            var result=run(List.of(zero),authority(List.of(k),binding,List.of(policy)));
            assertFalse(result.publishable());
            assertTrue(result.issues().stream().anyMatch(x->"FREE_POLICY_INACTIVE".equals(x.code())),result.issues().toString());
        }
    }
    @Test void freePolicyIncludesBothTimeBoundaries() {
        var k=key("FIXED","HALL",8,"OWNER");var zero=row("FIXED","HALL","OWNER",8,0);zero.set(13,"free");
        for(var policy:List.of(
                new FreePolicy("free","2026-10-16T00:00:00Z","2026-10-17T00:00:00Z",false),
                new FreePolicy("free","2026-10-15T00:00:00Z","2026-10-16T00:00:00Z",false))) {
            assertTrue(run(List.of(zero),authority(List.of(k),binding,List.of(policy))).publishable());
        }
    }
    @Test void clockFailureIsNotReportedAsInvalidFreePolicy() {
        var failure=new IllegalStateException("synthetic clock failure");
        var brokenClock=new Clock() {
            public ZoneId getZone() {return ZoneOffset.UTC;}
            public Clock withZone(ZoneId zone) {return this;}
            public Instant instant() {throw failure;}
        };
        var zero=row("FIXED","HALL","OWNER",8,0);zero.set(13,"free");
        var a=authority(List.of(key("FIXED","HALL",8,"OWNER")),binding,List.of(new FreePolicy("free","2026-10-15T00:00:00Z","2026-10-17T00:00:00Z",false)));
        assertSame(failure,assertThrows(IllegalStateException.class,()->new RoomCardPricingImportValidator(brokenClock).validate(document(List.of(zero)),a,Profile.TEST_ONLY)));
    }
    @Test void carryTierAndLoopUnitsCannotBeInvented() {
        var carry=row("CARRY","HALL","OWNER",10000,2);var a=authority(List.of(key("CARRY","HALL",10000,"OWNER")),binding,List.of());var result=run(List.of(carry),a);assertTrue(result.publishable());assertEquals(10000,result.eligibleRows().getFirst().key().tierMinor());assertEquals(2,result.eligibleRows().getFirst().unitCostMinor());
        carry.set(8,"ROUND_COUNT");rejected(carry,a,"CARRY_UNIT_INVALID");
        var loop=row("LOOP","HALL","OWNER",8,5);var la=authority(List.of(key("LOOP","HALL",8,"OWNER")),binding,List.of());assertTrue(run(List.of(loop),la).publishable());loop.set(8,"ROUND_COUNT");rejected(loop,la,"LOOP_UNIT_INVALID");
    }
    @Test void duplicateAndOverlappingTiersFailClosed() {
        var a=authority(List.of(key("FIXED","HALL",8,"OWNER")),binding,List.of());var owner=row("FIXED","HALL","OWNER",8,7);assertTrue(run(List.of(owner,owner),a).issues().stream().anyMatch(x->x.code().equals("DUPLICATE_PRICE")));
        var ca=authority(List.of(key("CARRY","HALL",10000,"OWNER"),key("CARRY","HALL",20000,"OWNER")),binding,List.of());assertTrue(run(List.of(row("CARRY","HALL","OWNER",10000,2),row("CARRY","HALL","OWNER",20000,3)),ca).issues().stream().anyMatch(x->x.code().equals("OVERLAPPING_TIER")));
    }
    @Test void disabledDraftRemainsNonPublishableAndMissingAmountStaysNull() {
        var draft=row("FIXED","HALL","OWNER",8,null);draft.set(16,"DISABLED");draft.set(18,"待配置");var a=authority(List.of(),new SourceBinding(SourceClass.REGISTERED_ONLY,0,0,null),List.of());var result=validator.validate(document(List.of(draft)),a,Profile.DRAFT);assertEquals(1,result.disabledRows());assertTrue(result.eligibleRows().isEmpty());assertFalse(result.publishable());assertNull(draft.get(10));
    }
}
