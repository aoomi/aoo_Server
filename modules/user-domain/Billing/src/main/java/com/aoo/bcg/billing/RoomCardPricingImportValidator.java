package com.aoo.bcg.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

/** The common, offline pricing validator. This class cannot publish or charge. */
public final class RoomCardPricingImportValidator {
    public static final List<String> HEADERS = List.of("地区编码","玩法编码","身份ID","身份版本","计费方式","适用场景","人数","计费单位值","计费单位","付款方式","房卡数量","资产编码","取整方式","免费策略ref","退款规则ref","配置版本","启用状态","来源","确认状态","缺失项");
    private static final Map<String,String> KINDS = Map.of("固定局数","FIXED","循环场次","LOOP","带入额度","CARRY");
    public enum Profile { DRAFT, PRODUCTION, TEST_ONLY }
    public enum SourceClass { CURRENT_PUBLISHED, REGISTERED_ONLY, GENERATED_ONLY, COMPATIBILITY }
    public record IdentityKey(long gameId,String gameCode,String playVersion,String regionCode) {}
    public record SourceBinding(SourceClass sourceClass,long releaseId,long indexGeneration,String sourceHash) {}
    public record Identity(IdentityKey key,SourceBinding sourceBinding) {}
    public record PriceKey(IdentityKey identity,String kind,String context,int playerCount,String measureKind,int measureValue,String payerMode,long tierMinor) {}
    public record FreePolicy(String ref,String validFromISO,String validUntilISO,boolean alwaysFree) {}
    public record AuthoritySnapshot(int version,String environment,String sourceHash,List<Identity> currentIdentities,List<PriceKey> effectiveKeys,Set<String> refundRefs,List<FreePolicy> freePolicies) {
        public AuthoritySnapshot { currentIdentities=List.copyOf(currentIdentities);effectiveKeys=List.copyOf(effectiveKeys);refundRefs=Set.copyOf(refundRefs);freePolicies=List.copyOf(freePolicies); }
    }
    public record RawRow(int excelRow,List<Object> values,boolean hasFormula) { public RawRow { values=Collections.unmodifiableList(new ArrayList<>(values)); } }
    public record ImportedSheet(String name,List<String> headers,List<RawRow> rows,List<List<Object>> notes) {
        public ImportedSheet { headers=List.copyOf(headers);rows=List.copyOf(rows);notes=notes.stream().map(x->Collections.unmodifiableList(new ArrayList<>(x))).toList(); }
    }
    public record ImportedBook(String fileName,String fileHash,List<ImportedSheet> sheets) { public ImportedBook { sheets=List.copyOf(sheets); } }
    public record ImportDocument(int schemaVersion,List<ImportedBook> books) { public ImportDocument { books=List.copyOf(books); } }
    public record NormalizedPrice(PriceKey key,long unitCostMinor,String currency,String rounding,String freeRef,String refundRef,String configurationVersion,String sourceFile,String sheet,int row,String sourceRef,String confirmation,String priceBasis,SourceBinding sourceBinding) {}
    public record ValidationIssue(String code,String file,String sheet,int row) {}
    public record ValidationResult(List<ValidationIssue> issues,List<PriceKey> missingKeys,int disabledRows,List<NormalizedPrice> eligibleRows,String canonicalHash,boolean publishable) {
        public ValidationResult { issues=List.copyOf(issues);missingKeys=List.copyOf(missingKeys);eligibleRows=List.copyOf(eligibleRows); }
    }
    private final Clock clock;
    private static final ObjectMapper JSON = new ObjectMapper();
    public RoomCardPricingImportValidator(Clock clock) { this.clock=Objects.requireNonNull(clock); }

    public ValidationResult validate(ImportDocument input,AuthoritySnapshot authority,Profile profile) {
        var issues=new ArrayList<ValidationIssue>();var eligible=new ArrayList<NormalizedPrice>();int disabled=0;
        if(input.schemaVersion()!=1 || authority.version()!=1) issue(issues,"SCHEMA_VERSION","","",0);
        if(profile==Profile.PRODUCTION && !"PRODUCTION".equals(authority.environment())) issue(issues,"AUTHORITY_ENVIRONMENT","","",0);
        if(profile==Profile.TEST_ONLY && !"TEST_ONLY".equals(authority.environment())) issue(issues,"AUTHORITY_ENVIRONMENT","","",0);
        var identities=new HashMap<IdentityKey,Identity>();
        for(var identity:authority.currentIdentities()) {
            if(identity==null || identity.key()==null || !validIdentity(identity.key())) { issue(issues,"AUTHORITY_IDENTITY_INVALID","","",0);continue; }
            if(identities.putIfAbsent(identity.key(),identity)!=null) issue(issues,"AUTHORITY_IDENTITY_CONFLICT","","",0);
        }
        var effective=new HashSet<PriceKey>();
        for(var key:authority.effectiveKeys()) {
            if(!effective.add(key)) issue(issues,"AUTHORITY_EFFECTIVE_DUPLICATE","","",0);
            if(key==null || !identities.containsKey(key.identity())) issue(issues,"AUTHORITY_EFFECTIVE_IDENTITY","","",0);
        }
        if(!Objects.equals(authority.sourceHash(),authorityHash(authority))) issue(issues,"AUTHORITY_HASH_MISMATCH","","",0);
        var free=new HashMap<String,FreePolicy>();
        for(var f:authority.freePolicies()) if(f==null || !text(f.ref()) || free.putIfAbsent(f.ref(),f)!=null) issue(issues,"FREE_POLICY_CONFLICT","","",0);
        var seen=new HashSet<PriceKey>();var carryBases=new HashMap<String,Long>();var files=new HashSet<String>();
        for(var book:input.books()) {
            if(!files.add(book.fileName()) || !text(book.fileHash()) || !book.fileHash().matches("[0-9a-f]{64}")) issue(issues,"BOOK_PROVENANCE",book.fileName(),"",0);
            var sheets=new HashSet<String>();
            for(var sheet:book.sheets()) {
                if(!sheets.add(sheet.name())) issue(issues,"DUPLICATE_SHEET",book.fileName(),sheet.name(),0);
                if("说明来源".equals(sheet.name())) { if(sheet.notes().size()>100 || sheet.notes().stream().anyMatch(x->x.size()>2)) issue(issues,"NOTES_STRUCTURE",book.fileName(),sheet.name(),0);continue; }
                if(!KINDS.containsKey(sheet.name()) || !HEADERS.equals(sheet.headers()) || sheet.rows().size()>4999) {issue(issues,"SHEET_STRUCTURE",book.fileName(),sheet.name(),0);continue;}
                for(var row:sheet.rows()) {
                    String file=book.fileName(),sh=sheet.name();int n=row.excelRow();var v=row.values();
                    if(v.size()!=20 || row.hasFormula() || n<2) {issue(issues,"ROW_STRUCTURE",file,sh,n);continue;}
                    String status=str(v.get(16));boolean enabled="ENABLED".equals(status);
                    if(!enabled && !"DISABLED".equals(status)) {issue(issues,"ENABLE_STATUS",file,sh,n);continue;}
                    // Disabled source rows remain visible and cannot acquire runtime eligibility.
                    if(!enabled) {disabled++;if(v.get(10)!=null && !str(v.get(10)).isEmpty()) try {integer(v.get(10),0,Long.MAX_VALUE);} catch(IllegalArgumentException ex){issue(issues,"INVALID_AMOUNT",file,sh,n);}continue;}
                    try {
                        long id=integer(v.get(2),1,Long.MAX_VALUE);var identityKey=new IdentityKey(id,str(v.get(1)),str(v.get(3)),str(v.get(0)));
                        var identity=identities.get(identityKey);
                        if(identity==null) throw bad("IDENTITY_NOT_AUTHORIZED");
                        if(!published(identity.sourceBinding())) throw bad("AUTHORITY_NOT_CURRENT_PUBLISHED");
                        String kind=str(v.get(4)),context=str(v.get(5)),payer=str(v.get(9)),measure=str(v.get(8)),rounding=str(v.get(12));
                        if(!Objects.equals(KINDS.get(sh),kind)) throw bad("KIND_SHEET_MISMATCH");
                        if(!Set.of("HALL","CLUB","UNION").contains(context)) throw bad("CONTEXT_INVALID");
                        if("HALL".equals(context)) {if(!Set.of("OWNER","AA").contains(payer)) throw bad("PAYER_INVALID");}
                        else {if(!"ORGANIZATION_OWNER".equals(payer)) throw bad("PAYER_INVALID");payer=context;}
                        int players=(int)integer(v.get(6),1,Integer.MAX_VALUE),value=(int)integer(v.get(7),1,Integer.MAX_VALUE);long tier=0;
                        if("CARRY".equals(kind)) {
                            if("AA".equals(payer) || !"CARRY_MINOR_PER_TIER".equals(measure) || !"CEILING".equals(rounding)) throw bad("CARRY_UNIT_INVALID");tier=value;
                        } else {
                            if(!"EXACT".equals(rounding)) throw bad("ROUNDING_INVALID");
                            if("LOOP".equals(kind)) {if(!"ROUND_PER_SESSION".equals(measure)) throw bad("LOOP_UNIT_INVALID");measure="ROUND_COUNT";}
                            else if(!Set.of("ROUND_COUNT","DURATION_MINUTES").contains(measure)) throw bad("FIXED_UNIT_INVALID");
                        }
                        if(!"ROOM_CARD".equals(str(v.get(11)))) throw bad("CURRENCY_INVALID");
                        String confirmation=str(v.get(18));if(!Set.of("CONFIRMED","已确认").contains(confirmation)) throw bad("PRICE_UNCONFIRMED");
                        String version=str(v.get(15));if(!version.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,95}") || version.toLowerCase(Locale.ROOT).startsWith("draft")) throw bad("CONFIGURATION_VERSION_INVALID");
                        long cost;try {cost=integer(v.get(10),0,Long.MAX_VALUE);}catch(IllegalArgumentException ex){throw bad("INVALID_AMOUNT");}
                        String refund=str(v.get(14));if(!authority.refundRefs().contains(refund)) throw bad("REFUND_RULE_UNKNOWN");
                        String freeRef=str(v.get(13));if(freeRef.isEmpty() || "待配置".equals(freeRef)) freeRef=null;
                        if(cost==0 && (freeRef==null || !active(free.get(freeRef)))) throw bad("FREE_POLICY_INACTIVE");
                        if(cost>0 && freeRef!=null && !free.containsKey(freeRef)) throw bad("FREE_POLICY_UNKNOWN");
                        String source=str(v.get(17));if(!text(source)) throw bad("SOURCE_REFERENCE_MISSING");
                        var key=new PriceKey(identityKey,kind,context,players,measure,value,payer,tier);
                        if(!effective.contains(key)) throw bad("EXTRA_EFFECTIVE_COMBINATION");
                        if(!seen.add(key)) throw bad("DUPLICATE_PRICE");
                        if(tier>0) {
                            String base=canonical(List.of(identityKey,kind,context,players,payer));Long prior=carryBases.putIfAbsent(base,tier);
                            if(prior!=null && prior.longValue()!=tier) throw bad("OVERLAPPING_TIER");
                        }
                        eligible.add(new NormalizedPrice(key,cost,"ROOM_CARD",rounding,freeRef,refund,version,file,sh,n,source,"CONFIRMED","AA".equals(payer)?"PER_PLAYER":"PER_PAYER_TOTAL",identity.sourceBinding()));
                    } catch(IllegalArgumentException ex) {issue(issues,ex.getMessage(),file,sh,n);}
                }
            }
            if(!sheets.equals(Set.of("固定局数","循环场次","带入额度","说明来源"))) issue(issues,"SHEET_SET",book.fileName(),"",0);
        }
        var keys=new HashSet<PriceKey>();eligible.forEach(x->keys.add(x.key()));
        var missing=effective.stream().filter(x->!keys.contains(x)).sorted(Comparator.comparing(RoomCardPricingImportValidator::canonical)).toList();
        if(!missing.isEmpty()) issue(issues,"MISSING_EFFECTIVE_COMBINATIONS","","",0);
        eligible.sort(Comparator.comparing(x->canonical(x.key())));
        boolean publishable=profile!=Profile.DRAFT && issues.isEmpty() && !effective.isEmpty() && missing.isEmpty();
        return new ValidationResult(issues,missing,disabled,eligible,sha(canonical(eligible.stream().map(x->Arrays.asList(x.key(),x.unitCostMinor(),x.currency(),x.rounding(),x.freeRef(),x.refundRef(),x.configurationVersion(),x.sourceRef(),x.confirmation(),x.priceBasis(),x.sourceBinding())).toList())),publishable);
    }
    private boolean active(FreePolicy policy) {
        if(policy==null)return false;
        if(policy.alwaysFree())return policy.validFromISO()==null && policy.validUntilISO()==null;
        if(policy.validFromISO()==null || policy.validUntilISO()==null)return false;
        Instant from;
        Instant until;
        try {
            from=Instant.parse(policy.validFromISO());
            until=Instant.parse(policy.validUntilISO());
        } catch(java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("FREE_POLICY_INACTIVE",ex);
        }
        var now=clock.instant();
        return !until.isBefore(from)&&!now.isBefore(from)&&!now.isAfter(until);
    }
    private static boolean published(SourceBinding b) {return b!=null&&b.sourceClass()==SourceClass.CURRENT_PUBLISHED&&b.releaseId()>0&&b.indexGeneration()>0&&text(b.sourceHash())&&b.sourceHash().length()<=256&&b.sourceHash().chars().noneMatch(Character::isISOControl);}
    private static boolean validIdentity(IdentityKey k) {return k.gameId()>0&&text(k.gameCode())&&text(k.playVersion())&&text(k.regionCode());}
    private static boolean text(String s) {return s!=null&&!s.isBlank();}
    private static String str(Object value) {return value==null?"":value.toString();}
    private static IllegalArgumentException bad(String code) {return new IllegalArgumentException(code);}
    private static void issue(List<ValidationIssue> issues,String code,String file,String sheet,int row) {issues.add(new ValidationIssue(code,file,sheet,row));}
    private static long integer(Object value,long minimum,long maximum) {
        if(value==null || value instanceof Boolean)throw bad("INTEGER_REQUIRED");
        String s=value.toString();
        if(value instanceof Number && new BigDecimal(s).abs().compareTo(new BigDecimal("9007199254740991"))>0)throw bad("UNSAFE_INTEGER");
        if(value instanceof Double || value instanceof Float) {double d=((Number)value).doubleValue();if(!Double.isFinite(d)||d!=Math.rint(d)||Math.abs(d)>9007199254740991d)throw bad("UNSAFE_INTEGER");s=BigDecimal.valueOf(d).toBigIntegerExact().toString();}
        if(!s.matches("0|[1-9][0-9]*"))throw bad("INTEGER_REQUIRED");
        try {long n=Long.parseLong(s);if(n<minimum||n>maximum)throw bad("INTEGER_RANGE");return n;}catch(NumberFormatException ex){throw bad("INTEGER_RANGE");}
    }
    private static String canonical(Object object) {try{return JSON.writeValueAsString(object);}catch(Exception ex){throw new IllegalStateException("CANONICAL_ENCODING",ex);}}
    private static String sha(String value) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception ex){throw new IllegalStateException(ex);}}
    public static String authorityHash(AuthoritySnapshot a) {
        var identities=a.currentIdentities().stream().sorted(Comparator.comparing(RoomCardPricingImportValidator::canonical)).toList();
        var effective=a.effectiveKeys().stream().sorted(Comparator.comparing(RoomCardPricingImportValidator::canonical)).toList();
        var refs=a.refundRefs().stream().sorted().toList();var free=a.freePolicies().stream().sorted(Comparator.comparing(RoomCardPricingImportValidator::canonical)).toList();
        return sha(canonical(List.of(a.version(),a.environment(),identities,effective,refs,free)));
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=8)throw bad("ARGUMENTS");var options=new HashMap<String,String>();for(int i=0;i<args.length;i+=2)if(options.put(args[i],args[i+1])!=null)throw bad("ARGUMENTS");
        if(!options.keySet().equals(Set.of("--input","--authority","--profile","--output")))throw bad("ARGUMENTS");
        var input=JSON.readValue(Path.of(options.get("--input")).toFile(),ImportDocument.class);var authority=JSON.readValue(Path.of(options.get("--authority")).toFile(),AuthoritySnapshot.class);
        var result=new RoomCardPricingImportValidator(Clock.systemUTC()).validate(input,authority,Profile.valueOf(options.get("--profile")));
        Files.writeString(Path.of(options.get("--output")),JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result),StandardOpenOption.CREATE_NEW);
        if(!result.issues().isEmpty())System.exit(2);
    }
}
