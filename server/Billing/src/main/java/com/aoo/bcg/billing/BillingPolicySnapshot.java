package com.aoo.bcg.billing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

/** Immutable server-authored room cost policy. Client roomcost/payType values are never authoritative. */
public record BillingPolicySnapshot(
        String policyId,
        long gameId,
        String productCode,
        CurrencyAccount payerAccount,
        PaymentSubject paymentSubject,
        ChargePoint chargePoint,
        RefundPolicy refundPolicy,
        int totalRounds,
        int minimumPlayers,
        int maximumPlayers,
        long totalAmount,
        int serviceFeeBasisPoints,
        Duration reservationTtl,
        long version,
        Instant lockedAt,
        String fingerprint) {

    public BillingPolicySnapshot {
        requireCode(policyId,"policyId");
        requireCode(productCode,"productCode");
        Objects.requireNonNull(payerAccount,"payerAccount");
        Objects.requireNonNull(paymentSubject,"paymentSubject");
        Objects.requireNonNull(chargePoint,"chargePoint");
        Objects.requireNonNull(refundPolicy,"refundPolicy");
        Objects.requireNonNull(reservationTtl,"reservationTtl");
        Objects.requireNonNull(lockedAt,"lockedAt");
        if(gameId<=0||totalRounds<=0||minimumPlayers<=0||maximumPlayers<minimumPlayers||totalAmount<=0
                ||serviceFeeBasisPoints<0||serviceFeeBasisPoints>10_000||reservationTtl.isNegative()
                ||reservationTtl.isZero()||version<=0)throw new IllegalArgumentException("invalid billing policy");
        String expected=fingerprint(policyId,gameId,productCode,payerAccount,paymentSubject,chargePoint,
                refundPolicy,totalRounds,minimumPlayers,maximumPlayers,totalAmount,serviceFeeBasisPoints,
                reservationTtl,version,lockedAt);
        if(fingerprint==null||fingerprint.isBlank())fingerprint=expected;
        else if(!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),fingerprint.getBytes(StandardCharsets.US_ASCII)))
            throw new IllegalArgumentException("billing policy fingerprint mismatch");
    }

    public static BillingPolicySnapshot lock(String policyId,long gameId,String productCode,
            CurrencyAccount payerAccount,PaymentSubject paymentSubject,ChargePoint chargePoint,
            RefundPolicy refundPolicy,int totalRounds,int minimumPlayers,int maximumPlayers,long totalAmount,
            int serviceFeeBasisPoints,Duration reservationTtl,long version,Instant lockedAt){
        return new BillingPolicySnapshot(policyId,gameId,productCode,payerAccount,paymentSubject,chargePoint,
                refundPolicy,totalRounds,minimumPlayers,maximumPlayers,totalAmount,serviceFeeBasisPoints,
                reservationTtl,version,lockedAt,null);
    }

    public long amountForAaPlayer(){return Math.floorDiv(Math.addExact(totalAmount,maximumPlayers-1L),maximumPlayers);}

    public long refundableAmount(long chargedAmount,int roundsPlayed){
        if(chargedAmount<0||roundsPlayed<0||roundsPlayed>totalRounds)throw new IllegalArgumentException("invalid refund inputs");
        if(roundsPlayed==0)return chargedAmount;
        return switch(refundPolicy){
            case NONE -> 0;
            case BEFORE_START_ONLY -> 0;
            case UNUSED_ROUNDS -> Math.floorDiv(Math.multiplyExact(chargedAmount,totalRounds-roundsPlayed),totalRounds);
        };
    }

    /** Same immutable values are used by creation UI, confirmation, ledger support and reconciliation. */
    public DisplayView display(){return new DisplayView(productCode,paymentSubject,chargePoint,payerAccount.currency(),
            totalAmount,totalRounds,serviceFeeBasisPoints,version,fingerprint);}

    private static String fingerprint(String policyId,long gameId,String productCode,CurrencyAccount payer,
            PaymentSubject subject,ChargePoint point,RefundPolicy refund,int rounds,int minPlayers,int maxPlayers,
            long amount,int fee,Duration ttl,long version,Instant lockedAt){
        String canonical=String.join("|",policyId,Long.toString(gameId),productCode,payer.canonicalId(),subject.name(),
                point.name(),refund.name(),Integer.toString(rounds),Integer.toString(minPlayers),
                Integer.toString(maxPlayers),Long.toString(amount),Integer.toString(fee),Long.toString(ttl.toMillis()),
                Long.toString(version),lockedAt.toString());
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }

    private static void requireCode(String value,String name){
        if(value==null||!value.matches("[A-Za-z0-9_.:-]{1,128}"))throw new IllegalArgumentException("invalid "+name);
    }

    public enum PaymentSubject { HOST, AA, WINNER, CLUB, UNION }
    public enum ChargePoint { CREATE, FIRST_ROUND, ROOM_FULL, FINISH }
    public enum RefundPolicy { NONE, BEFORE_START_ONLY, UNUSED_ROUNDS }
    public record DisplayView(String productCode,PaymentSubject paymentSubject,ChargePoint chargePoint,
            String currency,long amount,int rounds,int serviceFeeBasisPoints,long policyVersion,String policyFingerprint){}
}
