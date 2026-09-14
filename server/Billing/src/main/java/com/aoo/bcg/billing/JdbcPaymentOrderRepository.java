package com.aoo.bcg.billing;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

/** MySQL implementation for durable payment orders, callback replay hashes and provider claims. */
public final class JdbcPaymentOrderRepository implements PaymentOrderRepository {
    private final DataSource dataSource;
    public JdbcPaymentOrderRepository(DataSource dataSource){this.dataSource=Objects.requireNonNull(dataSource);}
    @Override public Optional<PaymentOrder> find(String orderId){
        String sql="SELECT buyer_player_id,asset_currency,asset_scope_id,product_code,product_version,asset_units,fiat_currency,amount_minor,channel_code,product_locked_at,product_fingerprint,state,provider_transaction_id,callback_digests,failure_code,version,created_at,paid_at,delivered_at,refunded_at FROM aoo_payment_order WHERE order_id=?";
        try(var connection=dataSource.getConnection();var statement=connection.prepareStatement(sql)){statement.setString(1,orderId);try(var row=statement.executeQuery()){if(!row.next())return Optional.empty();
            var buyer=new CurrencyAccount(row.getLong(1),row.getString(2),row.getLong(3));
            var product=new PaymentProductSnapshot(row.getString(4),row.getLong(5),row.getString(2),row.getLong(6),row.getString(7),row.getLong(8),row.getString(9),row.getTimestamp(10).toInstant(),row.getString(11));
            return Optional.of(new PaymentOrder(orderId,buyer,product,PaymentOrder.State.valueOf(row.getString(12)),row.getString(13),decode(row.getString(14)),row.getString(15),row.getLong(16),row.getTimestamp(17).toInstant(),instant(row.getTimestamp(18)),instant(row.getTimestamp(19)),instant(row.getTimestamp(20))));}}
        catch(SQLException error){throw new IllegalStateException("cannot load payment order",error);}
    }
    @Override public boolean insert(PaymentOrder order){
        String sql="INSERT INTO aoo_payment_order(order_id,buyer_player_id,asset_currency,asset_scope_id,product_code,product_version,asset_units,fiat_currency,amount_minor,channel_code,product_locked_at,product_fingerprint,state,provider_transaction_id,callback_digests,failure_code,version,created_at,paid_at,delivered_at,refunded_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try(var connection=dataSource.getConnection();var statement=connection.prepareStatement(sql)){bindAll(statement,order);statement.executeUpdate();return true;}catch(SQLException duplicate){if(integrity(duplicate))return false;throw new IllegalStateException("cannot insert payment order",duplicate);}
    }
    @Override public boolean replace(long expectedVersion,PaymentOrder order){
        String sql="UPDATE aoo_payment_order SET state=?,provider_transaction_id=?,callback_digests=?,failure_code=?,version=?,paid_at=?,delivered_at=?,refunded_at=? WHERE order_id=? AND version=?";
        try(var connection=dataSource.getConnection();var statement=connection.prepareStatement(sql)){statement.setString(1,order.state().name());statement.setString(2,order.providerTransactionId());statement.setString(3,encode(order.callbackDigests()));statement.setString(4,order.failureCode());statement.setLong(5,order.version());timestamp(statement,6,order.paidAt());timestamp(statement,7,order.deliveredAt());timestamp(statement,8,order.refundedAt());statement.setString(9,order.orderId());statement.setLong(10,expectedVersion);return statement.executeUpdate()==1;}catch(SQLException duplicate){if(integrity(duplicate))return false;throw new IllegalStateException("cannot update payment order",duplicate);}
    }
    @Override public boolean claimProviderTransaction(String transactionId,String orderId){
        String insert="INSERT IGNORE INTO aoo_payment_transaction_claim(provider_transaction_id,order_id,created_at) VALUES(?,?,CURRENT_TIMESTAMP(3))";
        String select="SELECT order_id FROM aoo_payment_transaction_claim WHERE provider_transaction_id=?";
        try(var connection=dataSource.getConnection()){try(var statement=connection.prepareStatement(insert)){statement.setString(1,transactionId);statement.setString(2,orderId);statement.executeUpdate();}try(var statement=connection.prepareStatement(select)){statement.setString(1,transactionId);try(var row=statement.executeQuery()){return row.next()&&orderId.equals(row.getString(1));}}}catch(SQLException error){throw new IllegalStateException("cannot claim provider transaction",error);}
    }
    private static void bindAll(java.sql.PreparedStatement statement,PaymentOrder order)throws SQLException{
        int i=1;statement.setString(i++,order.orderId());statement.setLong(i++,order.buyer().playerId());statement.setString(i++,order.buyer().currency());statement.setLong(i++,order.buyer().scopeId());statement.setString(i++,order.product().productCode());statement.setLong(i++,order.product().version());statement.setLong(i++,order.product().assetUnits());statement.setString(i++,order.product().fiatCurrency());statement.setLong(i++,order.product().amountMinor());statement.setString(i++,order.product().channelCode());statement.setTimestamp(i++,Timestamp.from(order.product().lockedAt()));statement.setString(i++,order.product().fingerprint());statement.setString(i++,order.state().name());statement.setString(i++,order.providerTransactionId());statement.setString(i++,encode(order.callbackDigests()));statement.setString(i++,order.failureCode());statement.setLong(i++,order.version());statement.setTimestamp(i++,Timestamp.from(order.createdAt()));timestamp(statement,i++,order.paidAt());timestamp(statement,i++,order.deliveredAt());timestamp(statement,i,order.refundedAt());
    }
    private static String encode(Map<String,String> callbacks){StringBuilder out=new StringBuilder();callbacks.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->out.append(entry.getKey()).append('=').append(entry.getValue()).append('\n'));return out.toString();}
    private static Map<String,String> decode(String encoded){Map<String,String> result=new LinkedHashMap<>();if(encoded==null||encoded.isEmpty())return result;for(String line:encoded.split("\\n")){int split=line.indexOf('=');if(split<=0||split==line.length()-1)throw new IllegalStateException("invalid callback digest encoding");result.put(line.substring(0,split),line.substring(split+1));}return result;}
    private static java.time.Instant instant(Timestamp value){return value==null?null:value.toInstant();}
    private static void timestamp(java.sql.PreparedStatement statement,int index,java.time.Instant value)throws SQLException{if(value==null)statement.setNull(index,java.sql.Types.TIMESTAMP);else statement.setTimestamp(index,Timestamp.from(value));}
    private static boolean integrity(SQLException error){return error.getSQLState()!=null&&error.getSQLState().startsWith("23");}
}
