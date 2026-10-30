package com.aoo.bcg.billing;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.junit.jupiter.api.*;
import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

/** Destructive only inside the guarded, dedicated empty test schema. No environment-based skipping. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class JdbcRoomChargeCoordinatorIntegrationTest {
    private static final String DB="aoo_fangka_isolated_25162442_r1";
    private static final Path MIGRATIONS=Path.of("server/database/migrations");
    private DataSource migration,runtime;
    private String privateInput;
    private final AtomicLong ids=new AtomicLong(251624420000L), rooms=new AtomicLong(10000);
    private JdbcBillingService billing;
    private JdbcRoomChargeCoordinator coordinator;
    private final String facts=RoomChargeOperation.hash("TEST_ONLY_VERIFIED_SERVER_EVENT");
    @BeforeAll void setup() throws Exception {
        privateInput=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8)).readLine();
        if(privateInput==null||privateInput.isBlank())throw new IllegalStateException("BLOCKED: dedicated MySQL stdin handle missing");
        Map<String,Object> config=new Json(privateInput).object();
        if(!DB.equals(config.get("expectedDatabase")))throw new IllegalStateException("wrong dedicated database guard");
        migration=source(map(config,"migration"));runtime=source(map(config,"runtime"));
        try(Connection c=migration.getConnection()) { JdbcRoomCostPolicyRepository.requireIsolated(c); }
        try(Connection c=runtime.getConnection()) { JdbcRoomCostPolicyRepository.requireIsolated(c); }
        rebuildFixture();
        billing=new JdbcBillingService(runtime,ids::incrementAndGet,Clock.systemUTC());
        coordinator=coordinator(false);
        for(long account=910001;account<=910012;account++)
            billing.credit("test-seed-"+account,account,"ROOM_CARD",account==910012?7:100,"TEST_ONLY_SEED");
    }
    @BeforeEach void resetPointer() throws Exception { execute("UPDATE aoo_room_cost_active SET publication_id=1,version=version+1 WHERE pointer_name='roomcard'"); }
    private JdbcRoomChargeCoordinator coordinator(boolean production) {
        return new JdbcRoomChargeCoordinator(runtime,billing,new JdbcRoomCostPolicyRepository(runtime,!production),Clock.systemUTC());
    }
    private RoomChargeOperation operation(long payer,RoomChargeOperation.Kind kind,long carry) {
        return new RoomChargeOperation(rooms.incrementAndGet(),"attempt1","unit1",payer,payer,1,"test-v1","TEST-REGION",kind,
                RoomChargeOperation.Context.HALL,RoomChargeOperation.Payer.OWNER,
                kind==RoomChargeOperation.Kind.CARRY?"CARRY_MINOR_PER_TIER":"ROUND_COUNT",
                kind==RoomChargeOperation.Kind.CARRY?10000:8,2,carry);
    }
    private RoomChargeOperation change(RoomChargeOperation a,long carry,int measure,RoomChargeOperation.Payer payer,long game) {
        return new RoomChargeOperation(a.roomId(),a.roomAttempt(),a.unitId(),a.actorId(),a.payerId(),game,a.playVersion(),a.regionCode(),a.kind(),a.context(),payer,a.measureKind(),measure,a.playerCount(),carry);
    }
    private JdbcRoomChargeCoordinator.RefundEvidence refundEvidence(String event) {
        return new JdbcRoomChargeCoordinator.RefundEvidence(JdbcRoomChargeCoordinator.RefundCause.POLICY_ALLOWED,event,facts);
    }
    @Test void reserveConfirmReplayAndOriginalPriceRefund() throws Exception {
        var operation=operation(910001,RoomChargeOperation.Kind.FIXED,0);long before=balance(910001);
        var original=coordinator.reserve(operation);assertEquals(7,original.amount());
        for(int i=0;i<10;i++)assertEquals(original,coordinator.reserve(operation));
        assertEquals(before-7,balance(910001));assertEquals(1,count("aoo_ledger","business_id='rcd:"+operation.operationKey()+"'"));
        var evidence=new JdbcRoomChargeCoordinator.OutcomeEvidence("actual-start",facts);
        coordinator.confirm(original.requestId(),"confirm-1",evidence);
        assertEquals(coordinator.confirm(original.requestId(),"confirm-1",evidence),coordinator.find(original.requestId()).orElseThrow());
        assertThrows(IllegalArgumentException.class,()->coordinator.confirm(original.requestId(),"confirm-other",evidence));
        execute("UPDATE aoo_room_cost_active SET publication_id=2 WHERE pointer_name='roomcard'");
        assertEquals(7,coordinator.reserve(operation).amount());
        var refund=coordinator.refund(original.requestId(),"refund-original-price",refundEvidence("early-dissolve"));
        assertEquals(7,refund.amount());assertEquals(refund,coordinator.refund(original.requestId(),"refund-original-price",refundEvidence("early-dissolve")));
        assertEquals(before,balance(910001));
        assertThrows(IllegalStateException.class,()->coordinator.confirm(original.requestId(),"confirm-1",evidence));
    }
    @Test void aaIndependentPriceAndLoopUnitPrice() {
        var base=operation(910002,RoomChargeOperation.Kind.FIXED,0);
        var aa=change(base,0,8,RoomChargeOperation.Payer.AA,1);
        assertEquals(3,coordinator.reserve(aa).amount());
        assertEquals(5,coordinator.reserve(operation(910002,RoomChargeOperation.Kind.LOOP,0)).amount());
    }
    @Test void carryUsesRealPublishedTierAndRejectsIdentityConflicts() throws Exception {
        long before=balance(910003);
        var exact=operation(910003,RoomChargeOperation.Kind.CARRY,10000);
        var over=operation(910003,RoomChargeOperation.Kind.CARRY,10001);
        assertEquals(2,coordinator.reserve(exact).amount());assertEquals(4,coordinator.reserve(over).amount());
        assertEquals(before-6,balance(910003));
        assertThrows(IllegalArgumentException.class,()->coordinator.reserve(change(exact,10001,10000,RoomChargeOperation.Payer.OWNER,1)));
        assertThrows(IllegalArgumentException.class,()->coordinator.reserve(change(over,10001,10001,RoomChargeOperation.Payer.OWNER,1)));
        assertThrows(IllegalArgumentException.class,()->new RoomChargeOperation(800,"a","u",910003,910003,1,"test-v1","TEST-REGION",RoomChargeOperation.Kind.CARRY,RoomChargeOperation.Context.HALL,RoomChargeOperation.Payer.OWNER,"ROUND_COUNT",1,2,10000));
        assertThrows(IllegalStateException.class,()->execute("UPDATE aoo_room_cost_policy SET tier_minor=9999 WHERE publication_id=1 AND billing_kind='CARRY'"));
        assertThrows(IllegalStateException.class,()->execute("UPDATE aoo_room_cost_policy SET round_count=8 WHERE publication_id=1 AND billing_kind='CARRY'"));
        assertThrows(IllegalStateException.class,()->execute("UPDATE aoo_room_cost_policy SET billing_kind='FIXED' WHERE publication_id=1 AND billing_kind='CARRY'"));
        execute("UPDATE aoo_room_cost_policy SET cost_minor=9223372036854775807 WHERE publication_id=1 AND billing_kind='CARRY'");
        var overflow=operation(910003,RoomChargeOperation.Kind.CARRY,10001);
        try { assertThrows(ArithmeticException.class,()->coordinator.reserve(overflow));assertTrue(coordinator.find(overflow.requestId()).isEmpty()); }
        finally { execute("UPDATE aoo_room_cost_policy SET cost_minor=2 WHERE publication_id=1 AND billing_kind='CARRY'"); }
        assertEquals(before-6,balance(910003));
    }
    @Test void concurrentDuplicateReserveReturnsOneCharge() throws Exception {
        var operation=operation(910004,RoomChargeOperation.Kind.FIXED,0);long before=balance(910004);
        var results=parallel(6,()->coordinator.reserve(operation));
        for(var result:results)assertEquals(7,result.amount());
        assertEquals(before-7,balance(910004));assertEquals(1,count("aoo_room_billing_reservation","request_id='"+operation.requestId()+"'"));
    }
    @Test void concurrentInsufficientBalanceNeverNegative() throws Exception {
        var a=operation(910012,RoomChargeOperation.Kind.FIXED,0);var b=operation(910012,RoomChargeOperation.Kind.FIXED,0);
        var executor=Executors.newFixedThreadPool(2);
        try {
            var fa=executor.submit(()->tryReserve(a));var fb=executor.submit(()->tryReserve(b));
            assertEquals(1,(fa.get()?1:0)+(fb.get()?1:0));assertEquals(0,balance(910012));
        } finally { executor.shutdownNow(); }
    }
    private boolean tryReserve(RoomChargeOperation operation) {
        try { coordinator.reserve(operation);return true; } catch(IllegalStateException expected) { return false; }
    }
    @Test void concurrentRefundCapAndSameIdConflict() throws Exception {
        var operation=operation(910005,RoomChargeOperation.Kind.FIXED,0);long before=balance(910005);
        var original=coordinator.reserve(operation);var counter=new AtomicLong();
        var refunds=parallel(6,()->coordinator.refund(original.requestId(),"concurrent-refund-"+counter.incrementAndGet(),refundEvidence("allowed-early")));
        assertEquals(7,refunds.stream().mapToLong(JdbcRoomChargeCoordinator.Refund::amount).sum());
        assertEquals(before,balance(910005));assertEquals(7,coordinator.find(original.requestId()).orElseThrow().refundedMinor());
        assertThrows(IllegalArgumentException.class,()->coordinator.refund(original.requestId(),"concurrent-refund-1",refundEvidence("different-facts")));
        assertEquals(1,count("aoo_ledger","business_id LIKE 'rcr:%' AND player_id=910005"));
    }
    @Test void ledgerAndRefundFailureRollBackEverySideEffect() throws Exception {
        var operation=operation(910006,RoomChargeOperation.Kind.FIXED,0);long before=balance(910006);
        execute("ALTER TABLE aoo_ledger ADD CONSTRAINT test_fail_debit CHECK (business_id <> 'rcd:"+operation.operationKey()+"')");
        try { assertThrows(IllegalStateException.class,()->coordinator.reserve(operation)); }
        finally { execute("ALTER TABLE aoo_ledger DROP CHECK test_fail_debit"); }
        assertTrue(coordinator.find(operation.requestId()).isEmpty());assertEquals(before,balance(910006));
        var original=coordinator.reserve(operation);
        execute("ALTER TABLE aoo_room_billing_reservation ADD CONSTRAINT test_fail_release CHECK(request_id <> '"+operation.requestId()+"' OR state <> 'RELEASED')");
        try { assertThrows(IllegalStateException.class,()->coordinator.refund(original.requestId(),"failed-release",refundEvidence("failure"))); }
        finally { execute("ALTER TABLE aoo_room_billing_reservation DROP CHECK test_fail_release"); }
        assertEquals(before-7,balance(910006));assertEquals(0,coordinator.find(original.requestId()).orElseThrow().refundedMinor());
        assertEquals(0,count("aoo_room_billing_refund","request_id='"+original.requestId()+"'"));
        coordinator.refund(original.requestId(),"failed-release",refundEvidence("failure"));assertEquals(before,balance(910006));
    }
    @Test void zeroHasRecordButNoLedgerMissingRetiredAndUnconfirmedFail() throws Exception {
        var base=operation(910007,RoomChargeOperation.Kind.FIXED,0);var free=change(base,0,8,RoomChargeOperation.Payer.OWNER,2);
        long before=balance(910007);var charge=coordinator.reserve(free);assertEquals(0,charge.amount());
        assertEquals(0,count("aoo_ledger","business_id='rcd:"+free.operationKey()+"'"));assertEquals(before,balance(910007));
        coordinator.confirm(charge.requestId(),"free-confirm",new JdbcRoomChargeCoordinator.OutcomeEvidence("free-start",facts));
        assertEquals(0,coordinator.refund(charge.requestId(),"free-refund",refundEvidence("free-end")).amount());
        assertThrows(IllegalArgumentException.class,()->coordinator.reserve(change(operation(910007,RoomChargeOperation.Kind.FIXED,0),0,8,RoomChargeOperation.Payer.OWNER,99)));
        execute("UPDATE aoo_room_cost_policy SET status='RETIRED' WHERE publication_id=1 AND game_id=2");
        try { assertThrows(IllegalArgumentException.class,()->coordinator.reserve(change(operation(910007,RoomChargeOperation.Kind.FIXED,0),0,8,RoomChargeOperation.Payer.OWNER,2))); }
        finally { execute("UPDATE aoo_room_cost_policy SET status='ACTIVE' WHERE publication_id=1 AND game_id=2"); }
        assertThrows(IllegalStateException.class,()->execute("UPDATE aoo_room_cost_policy SET free_policy_ref=NULL WHERE publication_id=1 AND game_id=2"));
        assertThrows(IllegalStateException.class,()->execute("UPDATE aoo_room_cost_policy SET confirmation_status='UNCONFIRMED' WHERE publication_id=1 AND game_id=2"));
        assertThrows(IllegalArgumentException.class,()->coordinator(true).reserve(operation(910007,RoomChargeOperation.Kind.FIXED,0)));
    }
    @Test void legacyOriginalIsNeverReclaimed() throws Exception {
        execute("INSERT INTO aoo_room_billing_reservation(request_id,account_id,room_id,currency_code,amount,state) VALUES('legacy-proof',910008,90001,'CRYSTAL',9,'CONFIRMED')");
        assertNull(coordinator.find("legacy-proof").orElseThrow().commandHash());
        assertThrows(IllegalStateException.class,()->coordinator.refund("legacy-proof","legacy-refund",refundEvidence("legacy")));
        assertEquals(0,count("aoo_room_billing_refund","request_id='legacy-proof'"));
    }
    @Test void newJvmRecoversCommittedChargeAndCompensates() throws Exception {
        var operation=operation(910009,RoomChargeOperation.Kind.FIXED,0);long before=balance(910009);
        var charge=coordinator.reserve(operation); // Deliberately discard the response in the business caller.
        String cp=Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
                +File.pathSeparator+Path.of(MysqlDataSource.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                +File.pathSeparator+System.getProperty("java.class.path");
        var command=List.of(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Xmx128m","-XX:ActiveProcessorCount=1","-cp",cp,getClass().getName(),"--restart-probe");
        Process child=new ProcessBuilder(command).redirectErrorStream(true).start();
        String input=privateInput.trim();input=input.substring(0,input.length()-1)+",\"probeRequestId\":"+RoomChargeOperation.json(charge.requestId())+"}";
        try(var out=child.getOutputStream()) { out.write((input+"\n").getBytes(StandardCharsets.UTF_8)); }
        String output=new String(child.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        assertEquals(0,child.waitFor());assertTrue(output.contains("RESTART_PROBE_PASS"));
        System.out.println(output.trim());assertEquals(before,balance(910009));
        assertEquals("RELEASED",coordinator.find(charge.requestId()).orElseThrow().state());
        assertEquals(7,coordinator.reserve(operation).refundedMinor());
    }
    public static void main(String[] args) {
        try {
            if(args.length!=1||!"--restart-probe".equals(args[0]))throw new IllegalArgumentException("probe only");
            String input=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8)).readLine();
            Map<String,Object> config=new Json(input).object();DataSource ds=source(map(config,"runtime"));
            try(var connection=ds.getConnection()) { JdbcRoomCostPolicyRepository.requireIsolated(connection); }
            var service=new JdbcBillingService(ds,new AtomicLong(251624429000L)::incrementAndGet,Clock.systemUTC());
            var coordinator=new JdbcRoomChargeCoordinator(ds,service,new JdbcRoomCostPolicyRepository(ds,true),Clock.systemUTC());
            String request=(String)config.get("probeRequestId");var charge=coordinator.find(request).orElseThrow();
            if(charge.amount()!=7||!"RESERVED".equals(charge.state()))throw new IllegalStateException("persisted recovery mismatch");
            var evidence=new JdbcRoomChargeCoordinator.RefundEvidence(JdbcRoomChargeCoordinator.RefundCause.BUSINESS_FAILED,"restart-business-failure",RoomChargeOperation.hash("TEST_ONLY_RESTART"));
            if(coordinator.refund(request,"restart-refund",evidence).amount()!=7)throw new IllegalStateException("restart compensation mismatch");
            System.out.println("RESTART_PROBE_PASS pid="+ProcessHandle.current().pid());
        } catch(Exception error) { System.err.println("RESTART_PROBE_FAIL type="+error.getClass().getSimpleName());System.exit(1); }
    }
    private static Map<String,Object> map(Map<String,Object> map,String key) {
        Object value=map.get(key);if(!(value instanceof Map<?,?>))throw new IllegalArgumentException("missing private role handle");
        @SuppressWarnings("unchecked") Map<String,Object> result=(Map<String,Object>)value;return result;
    }
    private static DataSource source(Map<String,Object> handle) throws SQLException {
        String url=(String)handle.get("jdbcUrl");
        if(url==null||!url.startsWith("jdbc:mysql://127.0.0.1:")||!url.contains("/"+DB)||url.contains("password="))
            throw new IllegalStateException("private endpoint mismatch");
        var ds=new MysqlDataSource();ds.setURL(url);ds.setUser((String)handle.get("username"));ds.setPassword((String)handle.get("password"));return ds;
    }
    private long balance(long account) throws SQLException {
        try(var c=runtime.getConnection();var s=c.prepareStatement("SELECT balance FROM aoo_currency_balance WHERE player_id=? AND currency='ROOM_CARD' AND currency_scope_id=0")) {
            s.setLong(1,account);try(var row=s.executeQuery()) { return row.next()?row.getLong(1):0; }
        }
    }
    private long count(String table,String predicate) throws SQLException {
        try(var c=runtime.getConnection();var s=c.createStatement();var row=s.executeQuery("SELECT COUNT(*) FROM "+table+" WHERE "+predicate)) { row.next();return row.getLong(1); }
    }
    private <T> List<T> parallel(int number,Callable<T> action) throws Exception {
        var executor=Executors.newFixedThreadPool(number);
        try { var futures=new ArrayList<Future<T>>();for(int i=0;i<number;i++)futures.add(executor.submit(action));
            var result=new ArrayList<T>();for(var future:futures)result.add(future.get(30,TimeUnit.SECONDS));return result;
        } finally { executor.shutdownNow(); }
    }
    private void execute(String sql) {
        String verb=sql.stripLeading().split("\\s+",2)[0].toUpperCase(Locale.ROOT);
        DataSource role=switch(verb) {
            case "CREATE","ALTER","DROP" -> migration;
            case "INSERT","UPDATE","DELETE","SELECT" -> runtime;
            default -> throw new IllegalArgumentException("unsupported fixture SQL operation");
        };
        try(var c=role.getConnection();var s=c.createStatement()) { s.execute(sql); }
        catch(SQLException error) { throw new IllegalStateException("fixture SQL failure ["+error.getSQLState()+"/"+error.getErrorCode()+"]"); }
    }
    private void rebuildFixture() throws Exception {
        // Only this exact dedicated schema is allowed. No historical release/index UPDATE runs.
        String[] tables={"aoo_room_billing_refund","aoo_room_billing_reservation","aoo_room_cost_active","aoo_room_cost_publication","aoo_room_cost_policy","aoo_ledger","aoo_currency_balance","aoo_currency_catalog","aoo_play_version","aoo_region"};
        // Drop in dependency order; no global tables or other schemas.
        for(String table:tables)execute("DROP TABLE IF EXISTS "+table);
        execute("CREATE TABLE aoo_region(region_code VARCHAR(32) PRIMARY KEY) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci");
        execute("CREATE TABLE aoo_play_version(game_id BIGINT UNSIGNED NOT NULL,play_version VARCHAR(64) NOT NULL,PRIMARY KEY(game_id,play_version)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci");
        apply("V20260822_04_2__currency_balance.sql","CREATE TABLE IF NOT EXISTS aoo_currency_balance");
        apply("V20260823_15__canonical_column_types.sql","ALTER TABLE aoo_currency_balance");
        apply("V20260823_26__currency_catalog_and_authority.sql","CREATE TABLE aoo_currency_catalog","INSERT INTO aoo_currency_catalog","ALTER TABLE aoo_currency_balance");
        apply("V20260822_01__business_closure.sql","CREATE TABLE IF NOT EXISTS aoo_ledger");
        apply("V20260823_11__required_business_semantics.sql","ALTER TABLE aoo_ledger");
        apply("V20260823_13__enforced_domain_checks.sql","ALTER TABLE aoo_ledger");
        apply("V20260823_15__canonical_column_types.sql","ALTER TABLE aoo_ledger");
        apply("V20260823_19__canonical_storage_layout.sql","ALTER TABLE aoo_ledger");
        apply("V20260823_23__ledger_reconciliation_dimensions.sql","ALTER TABLE aoo_ledger");
        apply("V20260824_07__ledger_scope_and_score_precision.sql","ALTER TABLE aoo_ledger");
        apply("V20260824_01__game_catalog_component_model.sql","CREATE TABLE aoo_room_cost_policy");
        apply("V20260919_06__room_cost_measure_dimension.sql","ALTER TABLE aoo_room_cost_policy");
        apply("V20260924_01__room_cost_measure_primary_identity.sql","ALTER TABLE aoo_room_cost_policy");
        apply("V20260825_79__room_authority_lifecycle.sql","CREATE TABLE IF NOT EXISTS aoo_room_billing_reservation");
        execute("INSERT INTO aoo_region VALUES('TEST-REGION')");
        execute("INSERT INTO aoo_play_version VALUES(1,'test-v1'),(2,'test-v1')");
        // Prove old rows retain their exact financial/identity columns through the new migration.
        execute("INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,round_count,measure_kind,measure_value,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status) VALUES(1,'test-v1','TEST-REGION',8,'ROUND_COUNT',8,2,'WINNER','CRYSTAL',9,1,REPEAT('a',64),'ACTIVE')");
        applyAll("V20261016_90__unified_room_card_billing_transactions.sql");
        assertEquals(1,count("aoo_room_cost_policy","publication_id=0 AND payer_mode='WINNER' AND currency_code='CRYSTAL' AND cost_minor=9 AND round_count=8 AND measure_value=8"));
        execute("INSERT INTO aoo_room_cost_publication VALUES(1,REPEAT('b',64),'TEST_ONLY',CURRENT_TIMESTAMP(3)),(2,REPEAT('c',64),'TEST_ONLY',CURRENT_TIMESTAMP(3))");
        execute("INSERT INTO aoo_room_cost_active VALUES('roomcard',1,1,CURRENT_TIMESTAMP(3))");
        policy(1,1,"FIXED","ROUND_COUNT",8,0,"OWNER",7,null);
        policy(2,1,"FIXED","ROUND_COUNT",8,0,"OWNER",9,null);
        policy(1,1,"FIXED","ROUND_COUNT",8,0,"AA",3,null);
        policy(1,1,"LOOP","ROUND_COUNT",8,0,"OWNER",5,null);
        policy(1,1,"CARRY","CARRY_MINOR_PER_TIER",10000,10000,"OWNER",2,null);
        policy(1,2,"FIXED","ROUND_COUNT",8,0,"OWNER",0,"TEST_ONLY_EXPLICIT_FREE");
    }
    private void policy(int publication,int game,String kind,String measure,int value,int tier,String payer,int cost,String free) {
        execute("INSERT INTO aoo_room_cost_policy(game_id,play_version,region_code,round_count,measure_kind,measure_value,player_count,payer_mode,currency_code,cost_minor,policy_version,content_hash,status,publication_id,billing_kind,billing_context,tier_minor,confirmation_status,free_policy_ref,refund_policy_ref) VALUES("
                +game+",'test-v1','TEST-REGION',"+value+",'"+measure+"',"+value+",2,'"+payer+"','ROOM_CARD',"+cost+",1,REPEAT('d',64),'ACTIVE',"+publication+",'"+kind+"','HALL',"+tier+",'CONFIRMED',"+(free==null?"NULL":"'"+free+"'")+",'XQP_R5_TEST_ONLY')");
    }
    private void apply(String file,String... prefixes) throws Exception {
        for(String statement:statements(file))for(String prefix:prefixes)
            if(statement.startsWith(prefix) && statement.length()>prefix.length()
                    && (Character.isWhitespace(statement.charAt(prefix.length()))||statement.charAt(prefix.length())=='(')) { execute(statement);break; }
    }
    private void applyAll(String file) throws Exception { for(String statement:statements(file))execute(statement); }
    private List<String> statements(String file) throws IOException {
        String text=Files.readString(MIGRATIONS.resolve(file));
        text=text.replaceAll("(?m)^\\s*--[^\\r\\n]*","");
        return Arrays.stream(text.split(";")).map(String::trim).filter(s->!s.isEmpty()).toList();
    }
    /** Bounded object/string JSON reader solely for the private stdin transport. Never logs its input. */
    private static final class Json {
        final String text;int index;
        Json(String text) { if(text==null||text.length()>32768)throw new IllegalArgumentException("invalid private handle");this.text=text; }
        Map<String,Object> object() {
            skip();expect('{');Map<String,Object> result=new HashMap<>();skip();
            if(take('}'))return result;
            do { String key=string();skip();expect(':');skip();Object value=peek()=='{'?object():string();
                if(result.put(key,value)!=null)throw new IllegalArgumentException("duplicate private handle field");skip();
                if(take('}'))return result;expect(',');
            } while(true);
        }
        String string() {
            skip();expect('"');StringBuilder out=new StringBuilder();
            while(index<text.length()) {
                char c=text.charAt(index++);if(c=='"')return out.toString();
                if(c=='\\') {
                    char escaped=text.charAt(index++);
                    switch(escaped) {
                        case '"','\\','/' -> out.append(escaped);
                        case 'b' -> out.append('\b');case 'f' -> out.append('\f');case 'n' -> out.append('\n');case 'r' -> out.append('\r');case 't' -> out.append('\t');
                        case 'u' -> { out.append((char)Integer.parseInt(text.substring(index,index+4),16));index+=4; }
                        default -> throw new IllegalArgumentException("invalid private handle encoding");
                    }
                } else { if(c<32)throw new IllegalArgumentException("invalid private handle control");out.append(c); }
            }
            throw new IllegalArgumentException("incomplete private handle");
        }
        char peek() { skip();if(index>=text.length())throw new IllegalArgumentException("incomplete private handle");return text.charAt(index); }
        void skip() { while(index<text.length()&&Character.isWhitespace(text.charAt(index)))index++; }
        boolean take(char c) { if(index<text.length()&&text.charAt(index)==c){index++;return true;}return false; }
        void expect(char c) { if(!take(c))throw new IllegalArgumentException("invalid private handle structure"); }
    }
}
