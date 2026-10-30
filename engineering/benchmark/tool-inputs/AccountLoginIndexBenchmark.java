import java.nio.file.*;
import java.sql.*;
import java.util.*;
public final class AccountLoginIndexBenchmark {
 public static void main(String[] a) throws Exception {
  if(a.length!=5) throw new IllegalArgumentException("jdbcUrl user password samples output");
  int samples=Integer.parseInt(a[3]); List<Long> times=new ArrayList<>(samples);
  try(Connection c=DriverManager.getConnection(a[0],a[1],a[2])) {
   long rows,distinct; try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*),COUNT(DISTINCT account_id) FROM db_player")){r.next();rows=r.getLong(1);distinct=r.getLong(2);}
   if(rows<10_000_000L||rows!=distinct) throw new IllegalStateException("capacity/uniqueness failed");
   String key; try(Statement s=c.createStatement();ResultSet r=s.executeQuery("EXPLAIN SELECT * FROM db_player WHERE account_id=5000000 LIMIT 1")){r.next();key=r.getString("key");}
   if(!"uk_db_player_account".equals(key)) throw new IllegalStateException("unexpected plan: "+key);
   SplittableRandom random=new SplittableRandom(20260823L);
   try(PreparedStatement q=c.prepareStatement("SELECT * FROM db_player WHERE account_id=? LIMIT 1")){for(int i=0;i<100;i++)query(q,random.nextLong(1,rows+1));for(int i=0;i<samples;i++){long begin=System.nanoTime();query(q,random.nextLong(1,rows+1));times.add(System.nanoTime()-begin);}}
   Collections.sort(times);double p95=ms(times.get((int)Math.ceil(samples*.95)-1)),p99=ms(times.get((int)Math.ceil(samples*.99)-1));
   if(p95>20||p99>50)throw new IllegalStateException("latency budget failed");
   String json=String.format(Locale.ROOT,"{\n  \"rows\": %d,\n  \"distinctAccountIds\": %d,\n  \"accountIdentityType\": \"numeric-case-neutral\",\n  \"index\": \"%s\",\n  \"samples\": %d,\n  \"p95Ms\": %.3f,\n  \"p99Ms\": %.3f,\n  \"p95BudgetMs\": 20,\n  \"p99BudgetMs\": 50,\n  \"status\": \"passed\"\n}\n",rows,distinct,key,samples,p95,p99);Files.writeString(Path.of(a[4]),json);System.out.print(json);
  }
 }
 static void query(PreparedStatement q,long id)throws SQLException{q.setLong(1,id);try(ResultSet r=q.executeQuery()){if(!r.next())throw new SQLException("missing account "+id);}}
 static double ms(long n){return n/1_000_000.0;}
}
