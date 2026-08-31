package com.aoo.bcg.billing;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;

/** Durable grants plus transactionally reserved daily limits. */
public final class JdbcAssetGrantRepository implements AssetGrantRepository {
    private final DataSource dataSource;
    public JdbcAssetGrantRepository(DataSource dataSource){this.dataSource=Objects.requireNonNull(dataSource);}
    @Override public Optional<AssetGrant> find(String businessId){
        String sql="SELECT kind,source_player_id,source_currency,source_scope_id,target_player_id,target_currency,target_scope_id,tax_player_id,tax_currency,tax_scope_id,gross_amount,tax_amount,net_amount,source_code,business_date,risk_decision,state,failure_code,version,created_at,completed_at FROM aoo_asset_grant WHERE business_id=?";
        try(var connection=dataSource.getConnection();var statement=connection.prepareStatement(sql)){statement.setString(1,businessId);try(var row=statement.executeQuery()){if(!row.next())return Optional.empty();
            CurrencyAccount source=row.getObject(2)==null?null:new CurrencyAccount(row.getLong(2),row.getString(3),row.getLong(4));CurrencyAccount target=new CurrencyAccount(row.getLong(5),row.getString(6),row.getLong(7));CurrencyAccount tax=row.getObject(8)==null?null:new CurrencyAccount(row.getLong(8),row.getString(9),row.getLong(10));
            return Optional.of(new AssetGrant(businessId,AssetGrant.Kind.valueOf(row.getString(1)),source,target,tax,row.getLong(11),row.getLong(12),row.getLong(13),row.getString(14),row.getDate(15).toLocalDate(),AssetGrant.RiskDecision.valueOf(row.getString(16)),AssetGrant.State.valueOf(row.getString(17)),row.getString(18),row.getLong(19),row.getTimestamp(20).toInstant(),instant(row.getTimestamp(21))));}}
        catch(SQLException error){throw new IllegalStateException("cannot load asset grant",error);}
    }
    @Override public boolean insert(AssetGrant grant){
        String sql="INSERT INTO aoo_asset_grant(business_id,kind,source_player_id,source_currency,source_scope_id,target_player_id,target_currency,target_scope_id,tax_player_id,tax_currency,tax_scope_id,gross_amount,tax_amount,net_amount,source_code,business_date,risk_decision,state,failure_code,version,created_at,completed_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try(var connection=dataSource.getConnection();var statement=connection.prepareStatement(sql)){int i=1;statement.setString(i++,grant.businessId());statement.setString(i++,grant.kind().name());i=account(statement,i,grant.source());i=account(statement,i,grant.target());i=account(statement,i,grant.taxAccount());statement.setLong(i++,grant.grossAmount());statement.setLong(i++,grant.taxAmount());statement.setLong(i++,grant.netAmount());statement.setString(i++,grant.sourceCode());statement.setDate(i++,Date.valueOf(grant.businessDate()));statement.setString(i++,grant.riskDecision().name());statement.setString(i++,grant.state().name());statement.setString(i++,grant.failureCode());statement.setLong(i++,grant.version());statement.setTimestamp(i++,Timestamp.from(grant.createdAt()));timestamp(statement,i,grant.completedAt());statement.executeUpdate();return true;}catch(SQLException duplicate){if(integrity(duplicate))return false;throw new IllegalStateException("cannot insert asset grant",duplicate);}
    }
    @Override public boolean replace(long expectedVersion,AssetGrant grant){
        String sql="UPDATE aoo_asset_grant SET state=?,failure_code=?,version=?,completed_at=? WHERE business_id=? AND version=?";
        try(var connection=dataSource.getConnection();var statement=connection.prepareStatement(sql)){statement.setString(1,grant.state().name());statement.setString(2,grant.failureCode());statement.setLong(3,grant.version());timestamp(statement,4,grant.completedAt());statement.setString(5,grant.businessId());statement.setLong(6,expectedVersion);return statement.executeUpdate()==1;}catch(SQLException error){throw new IllegalStateException("cannot update asset grant",error);}
    }
    @Override public boolean reserveDailyLimit(String limitKey,LocalDate date,String businessId,long amount,long maximum){
        try(var connection=dataSource.getConnection()){connection.setAutoCommit(false);try{
            try(var statement=connection.prepareStatement("SELECT amount FROM aoo_asset_grant_limit_reservation WHERE limit_key=? AND business_date=? AND business_id=?")){statement.setString(1,limitKey);statement.setDate(2,Date.valueOf(date));statement.setString(3,businessId);try(var row=statement.executeQuery()){if(row.next()){if(row.getLong(1)!=amount)throw new IllegalArgumentException("daily limit reservation changed");connection.rollback();return true;}}}
            try(var statement=connection.prepareStatement("INSERT INTO aoo_asset_grant_limit_bucket(limit_key,business_date,used_amount) VALUES(?,?,0) ON DUPLICATE KEY UPDATE limit_key=limit_key")){statement.setString(1,limitKey);statement.setDate(2,Date.valueOf(date));statement.executeUpdate();}
            long used;try(var statement=connection.prepareStatement("SELECT used_amount FROM aoo_asset_grant_limit_bucket WHERE limit_key=? AND business_date=? FOR UPDATE")){statement.setString(1,limitKey);statement.setDate(2,Date.valueOf(date));try(var row=statement.executeQuery()){if(!row.next())throw new IllegalStateException("daily limit bucket missing");used=row.getLong(1);}}
            try(var statement=connection.prepareStatement("SELECT amount FROM aoo_asset_grant_limit_reservation WHERE limit_key=? AND business_date=? AND business_id=?")){statement.setString(1,limitKey);statement.setDate(2,Date.valueOf(date));statement.setString(3,businessId);try(var row=statement.executeQuery()){if(row.next()){if(row.getLong(1)!=amount)throw new IllegalArgumentException("daily limit reservation changed");connection.rollback();return true;}}}
            long next=Math.addExact(used,amount);if(next>maximum){connection.rollback();return false;}
            try(var statement=connection.prepareStatement("INSERT INTO aoo_asset_grant_limit_reservation(limit_key,business_date,business_id,amount,created_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP(3))")){statement.setString(1,limitKey);statement.setDate(2,Date.valueOf(date));statement.setString(3,businessId);statement.setLong(4,amount);statement.executeUpdate();}
            try(var statement=connection.prepareStatement("UPDATE aoo_asset_grant_limit_bucket SET used_amount=? WHERE limit_key=? AND business_date=?")){statement.setLong(1,next);statement.setString(2,limitKey);statement.setDate(3,Date.valueOf(date));statement.executeUpdate();}
            connection.commit();return true;
        }catch(Exception error){connection.rollback();if(error instanceof RuntimeException runtime)throw runtime;throw error;}finally{connection.setAutoCommit(true);}}
        catch(SQLException error){throw new IllegalStateException("cannot reserve grant daily limit",error);}
    }
    private static int account(java.sql.PreparedStatement statement,int index,CurrencyAccount account)throws SQLException{if(account==null){statement.setNull(index++,java.sql.Types.BIGINT);statement.setNull(index++,java.sql.Types.VARCHAR);statement.setNull(index++,java.sql.Types.BIGINT);}else{statement.setLong(index++,account.playerId());statement.setString(index++,account.currency());statement.setLong(index++,account.scopeId());}return index;}
    private static java.time.Instant instant(Timestamp value){return value==null?null:value.toInstant();}
    private static void timestamp(java.sql.PreparedStatement statement,int index,java.time.Instant value)throws SQLException{if(value==null)statement.setNull(index,java.sql.Types.TIMESTAMP);else statement.setTimestamp(index,Timestamp.from(value));}
    private static boolean integrity(SQLException error){return error.getSQLState()!=null&&error.getSQLState().startsWith("23");}
}
