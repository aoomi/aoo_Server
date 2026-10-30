package com.aoo.bcg.billing;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/** Executes rewards, gifts and scoped agent allocations with limits, tax, risk and compensation. */
public final class AssetGrantService {
    private final BillingService billing;private final AssetGrantRepository grants;private final RiskEvaluator risk;private final Clock clock;
    public AssetGrantService(BillingService billing,AssetGrantRepository grants,RiskEvaluator risk,Clock clock){this.billing=Objects.requireNonNull(billing);this.grants=Objects.requireNonNull(grants);this.risk=Objects.requireNonNull(risk);this.clock=Objects.requireNonNull(clock);}

    public AssetGrant execute(Command command,Policy policy){
        Objects.requireNonNull(command,"command");Objects.requireNonNull(policy,"policy");validate(command,policy);
        long tax=Math.floorDiv(Math.multiplyExact(command.grossAmount(),policy.taxBasisPoints()),10_000);
        long net=Math.subtractExact(command.grossAmount(),tax);
        AssetGrant existing=grants.find(command.businessId()).orElse(null);
        AssetGrant candidate;
        if(existing!=null){requireSame(existing,command,policy);if(existing.state()!=AssetGrant.State.CREATED)return existing;candidate=existing;}
        else{
            AssetGrant.RiskDecision decision=risk.evaluate(command);
            AssetGrant.State initial=switch(decision){case ALLOW->AssetGrant.State.CREATED;case REVIEW->AssetGrant.State.REVIEW;case REJECT->AssetGrant.State.REJECTED;};
            candidate=new AssetGrant(command.businessId(),command.kind(),command.source(),command.target(),
                    tax==0?null:policy.taxAccount(),command.grossAmount(),tax,net,command.sourceCode(),command.businessDate(),
                    decision,initial,null,1,clock.instant(),null);
            if(!grants.insert(candidate)){candidate=grants.find(command.businessId()).orElseThrow();requireSame(candidate,command,policy);if(candidate.state()!=AssetGrant.State.CREATED)return candidate;}
            if(candidate.riskDecision()!=AssetGrant.RiskDecision.ALLOW)return candidate;
        }
        String limitKey=command.source()==null?"CAMPAIGN/"+command.sourceCode():"SOURCE/"+command.source().canonicalId();
        if(!grants.reserveDailyLimit(limitKey,command.businessDate(),command.businessId(),command.grossAmount(),policy.maximumPerDay()))
            return transition(candidate,AssetGrant.State.REJECTED,"DAILY_LIMIT",null);
        boolean sourceDebited=false,taxCredited=false,targetCredited=false;
        try{
            if(command.source()!=null){billing.debit(id(command,"source-debit"),command.source(),command.grossAmount(),"GRANT_SOURCE");sourceDebited=true;}
            if(tax>0){billing.credit(id(command,"tax-credit"),policy.taxAccount(),tax,"GRANT_TAX");taxCredited=true;}
            billing.credit(id(command,"target-credit"),command.target(),net,"GRANT_TARGET");targetCredited=true;
            return transition(candidate,AssetGrant.State.COMPLETED,null,clock.instant());
        }catch(RuntimeException failure){
            boolean compensated=true;
            try{if(targetCredited)billing.debit(id(command,"target-reverse"),command.target(),net,"GRANT_REVERSE");}catch(RuntimeException ignored){compensated=false;}
            try{if(taxCredited)billing.debit(id(command,"tax-reverse"),policy.taxAccount(),tax,"GRANT_REVERSE");}catch(RuntimeException ignored){compensated=false;}
            try{if(sourceDebited)billing.credit(id(command,"source-reverse"),command.source(),command.grossAmount(),"GRANT_REVERSE");}catch(RuntimeException ignored){compensated=false;}
            return transition(candidate,compensated?AssetGrant.State.COMPENSATED:AssetGrant.State.COMPENSATION_PENDING,
                    failure.getClass().getSimpleName(),compensated?clock.instant():null);
        }
    }

    private AssetGrant transition(AssetGrant current,AssetGrant.State state,String failure,java.time.Instant completedAt){
        AssetGrant next=new AssetGrant(current.businessId(),current.kind(),current.source(),current.target(),current.taxAccount(),
                current.grossAmount(),current.taxAmount(),current.netAmount(),current.sourceCode(),current.businessDate(),
                current.riskDecision(),state,failure,current.version()+1,current.createdAt(),completedAt);
        if(!grants.replace(current.version(),next))return grants.find(current.businessId()).orElseThrow();return next;
    }
    private static String id(Command command,String suffix){return "grant:"+command.businessId()+":"+suffix;}
    private static void validate(Command command,Policy policy){
        if(command.businessId()==null||!command.businessId().matches("[A-Za-z0-9_.-]{1,80}")||command.kind()==null
                ||command.target()==null||command.grossAmount()<=0||command.grossAmount()>policy.maximumPerTransfer()
                ||command.sourceCode()==null||!command.sourceCode().matches("[A-Z0-9_.-]{1,64}")||command.businessDate()==null)
            throw new IllegalArgumentException("invalid grant command");
        if(command.kind().requiresSource()!=(command.source()!=null))throw new IllegalArgumentException("grant source mismatch");
        if(command.kind()==AssetGrant.Kind.AGENT_ALLOCATION&&(command.source().scopeId()==0||command.target().scopeId()==0
                ||command.source().scopeId()!=command.target().scopeId()))throw new IllegalArgumentException("agent allocation must remain in one scoped ledger");
    }
    private static void requireSame(AssetGrant grant,Command command,Policy policy){
        long tax=Math.floorDiv(Math.multiplyExact(command.grossAmount(),policy.taxBasisPoints()),10_000);
        if(grant.kind()!=command.kind()||!Objects.equals(grant.source(),command.source())||!grant.target().equals(command.target())
                ||grant.grossAmount()!=command.grossAmount()||grant.taxAmount()!=tax||!grant.sourceCode().equals(command.sourceCode())
                ||!grant.businessDate().equals(command.businessDate()))throw new IllegalArgumentException("businessId reused with different grant command");
    }
    public record Command(String businessId,AssetGrant.Kind kind,CurrencyAccount source,CurrencyAccount target,long grossAmount,String sourceCode,LocalDate businessDate){}
    public record Policy(long maximumPerTransfer,long maximumPerDay,int taxBasisPoints,CurrencyAccount taxAccount){
        public Policy{if(maximumPerTransfer<=0||maximumPerDay<maximumPerTransfer||taxBasisPoints<0||taxBasisPoints>10_000||(taxBasisPoints>0&&taxAccount==null)||(taxBasisPoints==0&&taxAccount!=null))throw new IllegalArgumentException("invalid grant policy");}
    }
    @FunctionalInterface public interface RiskEvaluator{AssetGrant.RiskDecision evaluate(Command command);}
}
