package com.aoo.bcg.billing;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/** Durable room-charge transactions. Business lifecycle permission belongs to the server adapter. */
public final class JdbcRoomChargeCoordinator {
    private final DataSource source;
    private final JdbcBillingService billing;
    private final JdbcRoomCostPolicyRepository policies;
    private final Clock clock;
    private final TrustedPayerVerifier payerVerifier;
    public JdbcRoomChargeCoordinator(DataSource source,JdbcBillingService billing,
            JdbcRoomCostPolicyRepository policies,Clock clock) {
        this(source,billing,policies,clock,null);
    }
    public JdbcRoomChargeCoordinator(DataSource source,JdbcBillingService billing,
            JdbcRoomCostPolicyRepository policies,Clock clock,TrustedPayerVerifier payerVerifier) {
        this.payerVerifier=payerVerifier;
        this.source=Objects.requireNonNull(source);this.billing=Objects.requireNonNull(billing);
        this.policies=Objects.requireNonNull(policies);this.clock=Objects.requireNonNull(clock);
    }
    /** Trusted server room context, never deserialize directly from client input. */
    public record Subject(long initiatingActorId,long creatorId,long sourceClubId) {
        public Subject { if(initiatingActorId<=0||creatorId<=0||sourceClubId<0)
            throw new IllegalArgumentException("invalid trusted payer subject"); }
        /** Canonical durable command identity; authentication remains the server caller's responsibility. */
        public String commandHash(RoomChargeOperation operation) {
            return RoomChargeOperation.hash("ROOM_CARD_TRUSTED_SUBJECT_V1",
                    Objects.requireNonNull(operation).commandHash(),initiatingActorId,creatorId,sourceClubId);
        }
    }
    @FunctionalInterface public interface TrustedPayerVerifier {
        void verify(Connection connection,RoomChargeOperation operation,Subject subject)throws SQLException;
    }
    private void verifyPayer(Connection connection,RoomChargeOperation operation,Subject subject) {
        if(subject==null) {
            if(operation.context()!=RoomChargeOperation.Context.HALL)
                throw new IllegalArgumentException("organization requires trusted payer subject");
            return;
        }
        if(operation.context()==RoomChargeOperation.Context.HALL) {
            if(subject.sourceClubId()!=0 || operation.payerMode()==RoomChargeOperation.Payer.OWNER
                    && operation.actorId()!=subject.creatorId())
                throw new IllegalArgumentException("hall trusted payer mismatch");
        } else if(subject.sourceClubId()<=0)throw new IllegalArgumentException("organization scope missing");
        if(payerVerifier==null)throw new IllegalStateException("trusted payer verifier missing");
        try { payerVerifier.verify(connection,operation,subject); }
        catch(SQLException denied) { throw new IllegalStateException("trusted payer verification failed",denied); }
    }
    public record Charge(String requestId,String commandHash,long payerId,long roomId,String currency,
            long amount,long refundedMinor,String state,long publicationId,String policySnapshot,
            String confirmationId,String confirmationHash) { }
    public record OutcomeEvidence(String eventId,String factsHash) {
        public OutcomeEvidence { RoomChargeOperation.requireToken(eventId,128); requireHash(factsHash); }
        String hash() { return RoomChargeOperation.hash(eventId,factsHash); }
    }
    public enum RefundCause { BUSINESS_FAILED, POLICY_ALLOWED }
    /** Server-only verified permission; never deserialize this from a client request. */
    public record RefundEvidence(RefundCause cause,String eventId,String factsHash) {
        public RefundEvidence { Objects.requireNonNull(cause);RoomChargeOperation.requireToken(eventId,128);requireHash(factsHash); }
        String hash(String requestId) { return RoomChargeOperation.hash(requestId,cause,eventId,factsHash); }
        String json() { return "{\"cause\":"+RoomChargeOperation.json(cause.name())+",\"eventId\":"
                +RoomChargeOperation.json(eventId)+",\"factsHash\":"+RoomChargeOperation.json(factsHash)+"}"; }
    }
    public record Refund(String refundKey,String requestId,long amount,String creditBusinessId) { }
    private static void requireHash(String hash) {
        if(hash==null||!hash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("invalid evidence hash");
    }
    public Charge reserve(RoomChargeOperation operation) {
        Objects.requireNonNull(operation);
        if(operation.context()!=RoomChargeOperation.Context.HALL)
            throw new IllegalArgumentException("organization requires trusted payer subject");
        return reserveInternal(operation,null,null);
    }
    public Charge reserve(RoomChargeOperation operation,Subject subject) {
        return reserveInternal(Objects.requireNonNull(operation),Objects.requireNonNull(subject),null);
    }
    public Charge reserve(RoomBillingSourceBinding.BoundIntent bound) {
        Objects.requireNonNull(bound);return reserveInternal(bound.intent().operation(),bound.subject(),bound);
    }
    private void verifyBound(Connection connection,RoomBillingSourceBinding.BoundIntent bound)throws SQLException {
        if(bound==null)return;
        var sources=new JdbcRoomBillingSourceBindingRepository(new JdbcRoomBillingIntentJournal());
        var persisted=sources.find(connection,bound.key(),true).orElseThrow(()->new IllegalArgumentException("durable billing source missing"));
        if(!persisted.equals(bound))throw new IllegalArgumentException("durable billing source conflict");
        if(new JdbcRoomBillingBusinessOutcomeRepository(sources).find(connection,bound.key(),RoomBillingBusinessOutcome.EventType.CREATE_ABORTED_BEFORE_COMMIT).isPresent())
            throw new IllegalStateException("billing create source aborted");
    }
    private Charge reserveInternal(RoomChargeOperation operation,Subject subject,RoomBillingSourceBinding.BoundIntent bound) {
        try {
            return transaction(connection -> {
                return reserveAt(connection,operation,subject,bound,null,null);
            });
        } catch(DatabaseFailure failure) {
            // Unique insertion loser performs one read after rollback, never a second debit attempt.
            if(!"23000".equals(failure.sqlState))throw failure;
            return transaction(connection -> {
                verifyBound(connection,bound);
                verifyPayer(connection,operation,subject);
                Charge winner=read(connection,operation.requestId(),true);
                if(winner==null)throw failure;
                requireSame(winner,operation,subject);return winner;
            });
        }
    }
    // Caller transaction only: no independent retry, commit, or rollback around the native command.
    Charge reserveAa(Connection connection,RoomChargeOperation operation,Subject subject,
            RoomBillingSourceBinding.AuthorityReference authority,RoomAaCreation.PolicyIdentity creationPolicy)throws SQLException {
        requireCallerTransaction(connection);
        if(operation.kind()!=RoomChargeOperation.Kind.FIXED||operation.context()!=RoomChargeOperation.Context.HALL
                ||operation.payerMode()!=RoomChargeOperation.Payer.AA||operation.payerId()!=operation.actorId()
                ||subject.initiatingActorId()!=operation.actorId()||subject.sourceClubId()!=0)
            throw new IllegalArgumentException("invalid AA participant binding");
        return reserveAt(connection,operation,subject,null,Objects.requireNonNull(authority),Objects.requireNonNull(creationPolicy));
    }
    JdbcRoomCostPolicyRepository.PolicySnapshot policyForAaCreation(Connection connection,RoomChargeOperation operation,
            RoomBillingSourceBinding.AuthorityReference authority)throws SQLException {
        requireCallerTransaction(connection);return policies.load(connection,operation,authority,clock.instant());
    }
    Charge findForAa(Connection connection,String requestId)throws SQLException {
        requireCallerTransaction(connection);return read(connection,requestId,true);
    }
    static void requireCallerTransaction(Connection connection)throws SQLException {
        if(Objects.requireNonNull(connection).getAutoCommit())throw new IllegalStateException("caller transaction required");
    }
    private Charge reserveAt(Connection connection,RoomChargeOperation operation,Subject subject,
            RoomBillingSourceBinding.BoundIntent bound,RoomBillingSourceBinding.AuthorityReference authority,RoomAaCreation.PolicyIdentity creationPolicy)throws SQLException {
        verifyBound(connection,bound);
        verifyPayer(connection,operation,subject);
        Charge original=read(connection,operation.requestId(),true);
        if(original!=null) { requireSame(original,operation,subject);return original; }
        var policy=creationPolicy!=null?policies.loadAtPublication(connection,operation,authority,clock.instant(),creationPolicy.publicationId()):bound==null?policies.load(connection,operation):policies.load(connection,bound,clock.instant());
        if(creationPolicy!=null&&(!creationPolicy.snapshot().equals(policy)||!creationPolicy.policyKey().equals(RoomChargeOperation.hash(operation.gameId(),operation.playVersion(),operation.regionCode(),operation.kind(),operation.context(),operation.playerCount(),operation.measureKind(),operation.measureValue(),operation.payerMode(),0L))))
            throw new IllegalArgumentException("AA creation policy identity conflict");
        String debit=policy.amount()==0?null:"rcd:"+operation.operationKey();
        try(var s=connection.prepareStatement("INSERT INTO aoo_room_billing_reservation"
                +"(request_id,account_id,room_id,currency_code,amount,state,created_at,updated_at,"
                +"billing_operation_key,command_hash,actor_id,room_attempt,billing_kind,billing_unit,"
                +"policy_publication_id,policy_snapshot,debit_business_id) VALUES(?,?,?,?,?,'RESERVED',?,?,?,?,?,?,?,?,?,?,?)")) {
            int i=1;s.setString(i++,operation.requestId());s.setLong(i++,operation.payerId());s.setLong(i++,operation.roomId());
            s.setString(i++,policy.currency());s.setLong(i++,policy.amount());
            s.setTimestamp(i++,now());s.setTimestamp(i++,now());s.setString(i++,operation.operationKey());
            s.setString(i++,commandHash(operation,subject));s.setLong(i++,operation.actorId());s.setString(i++,operation.roomAttempt());
            s.setString(i++,operation.kind().name());s.setString(i++,operation.unitId());s.setLong(i++,policy.publicationId());
            s.setString(i++,policy.json());s.setString(i,debit);s.executeUpdate();
        }
        if(policy.amount()>0)billing.debit(connection,debit,operation.payerId(),policy.currency(),policy.amount(),"ROOM_CARD_RESERVE");
        return Objects.requireNonNull(read(connection,operation.requestId(),false));
    }
    public Charge confirm(String requestId,String confirmationId,OutcomeEvidence evidence) {
        RoomChargeOperation.requireToken(confirmationId,128);Objects.requireNonNull(evidence);
        return transaction(connection -> confirmAt(connection,requestId,confirmationId,evidence));
    }
    Charge confirmCarry(Connection connection,String requestId,String confirmationId,OutcomeEvidence evidence)throws SQLException {
        requireCallerTransaction(connection);RoomChargeOperation.requireToken(confirmationId,128);Objects.requireNonNull(evidence);
        return confirmAt(connection,requestId,confirmationId,evidence);
    }
    private Charge confirmAt(Connection connection,String requestId,String confirmationId,OutcomeEvidence evidence)throws SQLException {
            Charge original=requireOriginal(connection,requestId);String hash=evidence.hash();
            if("CONFIRMED".equals(original.state())) {
                if(!confirmationId.equals(original.confirmationId())||!hash.equals(original.confirmationHash()))
                    throw new IllegalArgumentException("confirmation identity conflict");
                return original;
            }
            if(!"RESERVED".equals(original.state()))throw new IllegalStateException("terminal charge cannot confirm");
            try(var s=connection.prepareStatement("UPDATE aoo_room_billing_reservation SET state='CONFIRMED',confirmation_id=?,confirmation_hash=?,updated_at=? WHERE request_id=?")) {
                s.setString(1,confirmationId);s.setString(2,hash);s.setTimestamp(3,now());s.setString(4,requestId);
                if(s.executeUpdate()!=1)throw new IllegalStateException("charge confirmation lost");
            }
            return Objects.requireNonNull(read(connection,requestId,false));
    }
    public Refund refund(String requestId,String refundId,RefundEvidence evidence) {
        RoomChargeOperation.requireToken(refundId,128);Objects.requireNonNull(evidence);
        return transaction(connection -> refundAt(connection,requestId,refundId,evidence));
    }
    Refund refundCarry(Connection connection,String requestId,String refundId,RefundEvidence evidence)throws SQLException {
        requireCallerTransaction(connection);RoomChargeOperation.requireToken(refundId,128);Objects.requireNonNull(evidence);
        return refundAt(connection,requestId,refundId,evidence);
    }
    Charge findCarry(Connection connection,RoomChargeOperation operation,Subject subject)throws SQLException {
        requireCallerTransaction(connection);Charge original=requireOriginal(connection,operation.requestId());
        requireSame(original,operation,subject);return original;
    }
    private Refund refundAt(Connection connection,String requestId,String refundId,RefundEvidence evidence)throws SQLException {
        String key=RoomChargeOperation.hash("ROOM_CARD_REFUND",refundId),hash=evidence.hash(requestId);
            Charge original=requireOriginal(connection,requestId);
            try(var s=connection.prepareStatement("SELECT request_id,command_hash,amount,credit_business_id FROM aoo_room_billing_refund WHERE refund_id=? FOR UPDATE")) {
                s.setString(1,key);
                try(var row=s.executeQuery()) {
                    if(row.next()) {
                        if(!requestId.equals(row.getString(1))||!hash.equals(row.getString(2)))
                            throw new IllegalArgumentException("refund identity conflict");
                        return new Refund(key,requestId,row.getLong(3),row.getString(4));
                    }
                }
            }
            if(!("RESERVED".equals(original.state())||"CONFIRMED".equals(original.state())||"RELEASED".equals(original.state())))
                throw new IllegalStateException("charge cannot refund");
            long remaining=Math.subtractExact(original.amount(),original.refundedMinor());
            if(remaining<0)throw new IllegalStateException("corrupt refund total");
            String credit=remaining==0?null:"rcr:"+key;
            try(var s=connection.prepareStatement("INSERT INTO aoo_room_billing_refund(refund_id,request_id,command_hash,amount,credit_business_id,evidence_json,created_at) VALUES(?,?,?,?,?,?,?)")) {
                s.setString(1,key);s.setString(2,requestId);s.setString(3,hash);s.setLong(4,remaining);
                s.setString(5,credit);s.setString(6,evidence.json());s.setTimestamp(7,now());s.executeUpdate();
            }
            if(remaining>0)billing.credit(connection,credit,original.payerId(),original.currency(),remaining,"ROOM_CARD_REFUND");
            try(var s=connection.prepareStatement("UPDATE aoo_room_billing_reservation SET refunded_minor=?,state='RELEASED',updated_at=? WHERE request_id=?")) {
                s.setLong(1,original.amount());s.setTimestamp(2,now());s.setString(3,requestId);
                if(s.executeUpdate()!=1)throw new IllegalStateException("charge refund update lost");
            }
            return new Refund(key,requestId,remaining,credit);
    }
    public Optional<Charge> find(String requestId) {
        RoomChargeOperation.requireToken(requestId,128);
        return transaction(connection -> Optional.ofNullable(read(connection,requestId,false)));
    }
    private Charge requireOriginal(Connection connection,String requestId)throws SQLException {
        RoomChargeOperation.requireToken(requestId,128);Charge original=read(connection,requestId,true);
        if(original==null)throw new IllegalArgumentException("unknown original charge");
        if(original.commandHash()==null)throw new IllegalStateException("UNMIGRATED_LEGACY");
        return original;
    }
    private static String commandHash(RoomChargeOperation operation,Subject subject) {
        return subject==null?operation.commandHash():subject.commandHash(operation);
    }
    private static void requireSame(Charge charge,RoomChargeOperation operation,Subject subject) {
        if(charge.commandHash()==null)throw new IllegalStateException("UNMIGRATED_LEGACY");
        if(!charge.commandHash().equals(commandHash(operation,subject)))throw new IllegalArgumentException("operation identity conflict");
    }
    private Charge read(Connection connection,String requestId,boolean lock)throws SQLException {
        try(var s=connection.prepareStatement("SELECT request_id,command_hash,account_id,room_id,currency_code,amount,refunded_minor,state,policy_publication_id,policy_snapshot,confirmation_id,confirmation_hash FROM aoo_room_billing_reservation WHERE request_id=?"+(lock?" FOR UPDATE":""))) {
            s.setString(1,requestId);try(var row=s.executeQuery()) {
                return row.next()?new Charge(row.getString(1),row.getString(2),row.getLong(3),row.getLong(4),row.getString(5),row.getLong(6),row.getLong(7),row.getString(8),row.getLong(9),row.getString(10),row.getString(11),row.getString(12)):null;
            }
        }
    }
    private Timestamp now() { return Timestamp.from(clock.instant()); }
    @FunctionalInterface private interface Work<T> { T apply(Connection connection)throws SQLException; }
    private <T> T transaction(Work<T> work) {
        try(Connection connection=source.getConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            try { T result=work.apply(connection);connection.commit();return result; }
            catch(SQLException error) { connection.rollback();throw databaseFailure(error); }
            catch(RuntimeException error) { connection.rollback();throw error; }
        } catch(SQLException error) { throw databaseFailure(error); }
    }
    private static DatabaseFailure databaseFailure(SQLException error) { return new DatabaseFailure(error.getSQLState(),error.getErrorCode()); }
    private static final class DatabaseFailure extends IllegalStateException {
        final String sqlState;
        DatabaseFailure(String state,int code) { super("billing database failure ["+state+"/"+code+"]");sqlState=state; }
    }
}
