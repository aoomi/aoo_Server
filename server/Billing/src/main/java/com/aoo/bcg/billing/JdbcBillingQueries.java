package com.aoo.bcg.billing;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Billing-owned read model. Queries the same authoritative tables used by writes. */
public final class JdbcBillingQueries {
    private final DataSource source;
    public JdbcBillingQueries(DataSource source){this.source=Objects.requireNonNull(source);}

    public Balance balance(CurrencyAccount account){
        String sql="SELECT balance,version,updated_at FROM aoo_currency_balance WHERE player_id=? AND currency=? AND currency_scope_id=?";
        try(var c=source.getConnection();var s=c.prepareStatement(sql)){s.setLong(1,account.playerId());s.setString(2,account.currency());s.setLong(3,account.scopeId());try(var r=s.executeQuery()){
            if(!r.next())return new Balance(account,0,0,null);return new Balance(account,r.getLong(1),r.getLong(2),r.getTimestamp(3).toInstant());
        }}catch(SQLException e){throw new IllegalStateException("cannot query balance",e);}
    }

    public List<LedgerEntry> ledger(long playerId,String currency,long scopeId,int limit){
        if(playerId<=0||currency==null||!currency.matches("[A-Z0-9_]{1,32}")||scopeId<0||limit<1||limit>200)throw new IllegalArgumentException("invalid ledger query");
        String sql="SELECT business_id,delta,balance_after,reason_code,entry_status,channel_code,created_at FROM aoo_ledger WHERE player_id=? AND currency=? AND currency_scope_id=? ORDER BY created_at DESC,ledger_id DESC LIMIT ?";
        List<LedgerEntry> out=new ArrayList<>();try(var c=source.getConnection();var s=c.prepareStatement(sql)){s.setLong(1,playerId);s.setString(2,currency);s.setLong(3,scopeId);s.setInt(4,limit);try(var r=s.executeQuery()){while(r.next())out.add(new LedgerEntry(r.getString(1),playerId,currency,r.getLong(2),r.getLong(3),r.getString(4),r.getString(5),r.getString(6),r.getTimestamp(7).toInstant()));}return List.copyOf(out);}catch(SQLException e){throw new IllegalStateException("cannot query ledger",e);}
    }

    public PaymentProductSnapshot product(String productCode,String channelCode,Instant now){
        String sql="SELECT product_version,asset_currency,asset_units,fiat_currency,amount_minor FROM aoo_payment_product WHERE product_code=? AND channel_code=? AND status='ACTIVE' AND valid_from<=? AND (valid_until IS NULL OR valid_until>?)";
        try(var c=source.getConnection();var s=c.prepareStatement(sql)){s.setString(1,productCode);s.setString(2,channelCode);s.setTimestamp(3,java.sql.Timestamp.from(now));s.setTimestamp(4,java.sql.Timestamp.from(now));try(var r=s.executeQuery()){if(!r.next())throw new IllegalArgumentException("payment product not found");return PaymentProductSnapshot.lock(productCode,r.getLong(1),r.getString(2),r.getLong(3),r.getString(4),r.getLong(5),channelCode,now);}}catch(SQLException e){throw new IllegalStateException("cannot load payment product",e);}
    }

    public Optional<PaymentOrder> order(String orderId){return new JdbcPaymentOrderRepository(source).find(orderId);}
    public ReconciliationService.Report reconcile(LocalDate date,String status,String channel){return new JdbcReconciliationService(source::getConnection).reconcile(date,status,channel);}
    public record Balance(CurrencyAccount account,long amount,long version,Instant updatedAt){}
}
