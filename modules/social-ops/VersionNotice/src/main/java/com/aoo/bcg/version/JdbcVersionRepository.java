package com.aoo.bcg.version;

import javax.sql.DataSource;import java.sql.*;import java.time.Instant;import java.util.*;
import static com.aoo.bcg.version.VersionModels.*;

public final class JdbcVersionRepository implements VersionRepository {
    private final DataSource source; public JdbcVersionRepository(DataSource source){this.source=Objects.requireNonNull(source);}
    public Optional<Release> eligibleRelease(String p,String c,int bucket,Instant now){String sql="SELECT id,version,min_supported_version,force_update,rollout_percent,rollback_version,download_url FROM aoo_client_release WHERE platform=? AND channel=? AND state='ACTIVE' AND published_at<=? AND rollout_percent>? ORDER BY version_code DESC LIMIT 1";return oneRelease(sql,p,c,Timestamp.from(now),bucket);}
    public Optional<Release> rollback(String p,String c,String v){String sql="SELECT id,version,min_supported_version,force_update,rollout_percent,rollback_version,download_url FROM aoo_client_release WHERE platform=? AND channel=? AND version=? AND state IN ('ACTIVE','ROLLED_BACK') LIMIT 1";return oneRelease(sql,p,c,v);}
    private Optional<Release> oneRelease(String sql,Object... args){try(Connection x=source.getConnection();PreparedStatement q=x.prepareStatement(sql)){bind(q,args);try(ResultSet r=q.executeQuery()){return r.next()?Optional.of(release(r)):Optional.empty();}}catch(SQLException e){throw new IllegalStateException("version repository unavailable",e);}}
    public List<Notice> notices(String p,String c,Instant now){String sql="SELECT id,title,body,severity,starts_at,ends_at FROM aoo_client_notice WHERE enabled=TRUE AND (platform='*' OR platform=?) AND (channel='*' OR channel=?) AND starts_at<=? AND (ends_at IS NULL OR ends_at>?) ORDER BY severity DESC,id";List<Notice> out=new ArrayList<>();try(Connection x=source.getConnection();PreparedStatement q=x.prepareStatement(sql)){bind(q,p,c,Timestamp.from(now),Timestamp.from(now));try(ResultSet r=q.executeQuery()){while(r.next())out.add(new Notice(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),instant(r,5),instant(r,6)));}return out;}catch(SQLException e){throw new IllegalStateException("notice repository unavailable",e);}}
    public Optional<Maintenance> maintenance(String p,String c,Instant now){String sql="SELECT id,starts_at,ends_at,message,login_blocked FROM aoo_maintenance_window WHERE enabled=TRUE AND (platform='*' OR platform=?) AND (channel='*' OR channel=?) AND starts_at<=? AND ends_at>? ORDER BY login_blocked DESC,id LIMIT 1";try(Connection x=source.getConnection();PreparedStatement q=x.prepareStatement(sql)){bind(q,p,c,Timestamp.from(now),Timestamp.from(now));try(ResultSet r=q.executeQuery()){return r.next()?Optional.of(new Maintenance(r.getLong(1),instant(r,2),instant(r,3),r.getString(4),r.getBoolean(5))):Optional.empty();}}catch(SQLException e){throw new IllegalStateException("maintenance repository unavailable",e);}}
    public Optional<Manifest> manifest(long releaseId){
        String head="SELECT id,version,content_sha256,signature_algorithm,key_id,signature FROM aoo_resource_manifest WHERE release_id=? AND state='PUBLISHED'";
        try(Connection x=source.getConnection();PreparedStatement q=x.prepareStatement(head)){
            q.setLong(1,releaseId);
            try(ResultSet r=q.executeQuery()){
                if(!r.next())return Optional.empty();
                long id=r.getLong(1);List<Asset> assets=new ArrayList<>();
                try(PreparedStatement a=x.prepareStatement("SELECT asset_path,size_bytes,sha256 FROM aoo_resource_manifest_entry WHERE manifest_id=? ORDER BY asset_path")){
                    a.setLong(1,id);
                    try(ResultSet ar=a.executeQuery()){while(ar.next())assets.add(new Asset(ar.getString(1),ar.getLong(2),ar.getString(3)));}
                }
                return Optional.of(new Manifest(id,r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),List.copyOf(assets)));
            }
        }catch(SQLException e){throw new IllegalStateException("manifest repository unavailable",e);}
    }
    public List<FeatureFlag> featureFlags(String p,String c,int bucket,Instant now){
        String sql="SELECT flag_key,enabled,flag_value FROM aoo_client_feature_flag WHERE (platform='*' OR platform=?) AND (channel='*' OR channel=?) AND enabled_from<=? AND (enabled_until IS NULL OR enabled_until>?) AND rollout_percent>? ORDER BY flag_key";
        List<FeatureFlag> out=new ArrayList<>();try(Connection x=source.getConnection();PreparedStatement q=x.prepareStatement(sql)){bind(q,p,c,Timestamp.from(now),Timestamp.from(now),bucket);try(ResultSet r=q.executeQuery()){while(r.next())out.add(new FeatureFlag(r.getString(1),r.getBoolean(2),r.getString(3)));}return List.copyOf(out);}catch(SQLException e){throw new IllegalStateException("feature flag repository unavailable",e);}
    }
    public Directory directory(String p,String c,int bucket,Long preferred,Instant now){
        String sql="SELECT id,server_code,display_name,region,public_endpoint,state,weight,migrate_to_server_id,message,directory_revision FROM aoo_server_directory WHERE (platform='*' OR platform=?) AND (channel='*' OR channel=?) AND visible_from<=? AND (visible_until IS NULL OR visible_until>?) AND rollout_percent>? AND state<>'OFFLINE' ORDER BY region,weight DESC,id";
        List<ServerNode> out=new ArrayList<>();long revision=0;try(Connection x=source.getConnection();PreparedStatement q=x.prepareStatement(sql)){bind(q,p,c,Timestamp.from(now),Timestamp.from(now),bucket);try(ResultSet r=q.executeQuery()){while(r.next()){Long migrate=(Long)r.getObject(8);out.add(new ServerNode(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getInt(7),migrate,r.getString(9)));revision=Math.max(revision,r.getLong(10));}}}catch(SQLException e){throw new IllegalStateException("server directory repository unavailable",e);}
        Long selected=null;if(preferred!=null){for(ServerNode n:out)if(n.id()==preferred){selected=n.migrateToServerId()!=null?n.migrateToServerId():n.id();break;}}if(selected==null)for(ServerNode n:out)if("ACTIVE".equals(n.state())){selected=n.id();break;}return new Directory(revision,List.copyOf(out),selected);
    }
    private static Release release(ResultSet r)throws SQLException{return new Release(r.getLong(1),r.getString(2),r.getString(3),r.getBoolean(4),r.getInt(5),r.getString(6),r.getString(7));}
    private static Instant instant(ResultSet r,int n)throws SQLException{Timestamp t=r.getTimestamp(n);return t==null?null:t.toInstant();}private static void bind(PreparedStatement q,Object... a)throws SQLException{for(int i=0;i<a.length;i++)q.setObject(i+1,a[i]);}
}
