package server.aoo.dao.mapper.game;


import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import server.aoo.dao.entity.game.DbPlayer;
import java.util.List;


public interface DbPlayerDao extends BaseDao<DbPlayer> {
    @Query(value="select * from db_player where (createTime > ?1 or (createTime = ?1 and id > ?2)) order by createTime,id limit ?3",nativeQuery=true)
    List<DbPlayer> pageCreated(long createTime,long id,int limit);

    @Query(value="select * from db_player where (lastLogin > ?1 or (lastLogin = ?1 and id > ?2)) order by lastLogin,id limit ?3",nativeQuery=true)
    List<DbPlayer> pageLastLogin(long lastLogin,long id,int limit);

    @Query(value = "select * from db_player where account_id = ?1 limit 1", nativeQuery = true)
    public DbPlayer existAccountId(long accountId);


    @Query("update DbPlayer p set p.gold=?1 where p.id= ?2")
    @Modifying
    public int updateGoldById(int gold, long id);



    @Query("update DbPlayer p set p.gmLevel=?1 where p.id= ?2")
    @Modifying
    public int updateGmLevelById(int gmLevel, long id);

    @Query("update DbPlayer p set p.showProfile=?1 where p.id= ?2")
    @Modifying
    public int updateShowProfileById(int showProfile, long id);


    @Query("update DbPlayer p set p.name=?1,p.nameCoolingTime=?2 where p.id= ?3")
    @Modifying
    public int updateNameById(String name, int nameCoolingTime, long id);

    @Query("update DbPlayer p set p.headImageUrl=?1 where p.id= ?2")
    @Modifying
    public int updateHeadImageUrlById(String headImageUrl, long id);


    @Query("update DbPlayer p set p.phone=?1 where p.id= ?2")
    @Modifying
    public int updatePhoneById(String phone, long id);

    @Query("update DbPlayer p set p.bannedLoginExpiredTime=?1 where p.id= ?2")
    @Modifying
    public int updateBannedLoginById(int bannedLoginExpiredTime, long id);

    @Query("update DbPlayer p set p.refreshTime=?1 where p.id= ?2")
    @Modifying
    public int updateRefreshTimeById(int refreshTime, long id);

    @Query("update DbPlayer p set p.lastLogin=?1 where p.id= ?2")
    @Modifying
    public int updateLastLoginById(int lastLogin, long id);


    @Query("update DbPlayer p set p.maxChip=?1 where p.id= ?2")
    @Modifying
    public int updateMaxChipById(double maxChip, long id);

    @Query("update DbPlayer p set p.totalSetCount=?1 where p.id= ?2")
    @Modifying
    public int updateTotalSetCountById(int totalSetCount, long id);

    @Query("update DbPlayer p set p.winSetCount=?1 where p.id= ?2")
    @Modifying
    public int updateWinSetCountById(int winSetCount, long id);

    @Query("update DbPlayer p set p.totalRecharge=?1 where p.id= ?2")
    @Modifying
    public int updateTotalRechargeById(int totalRecharge, long id);


    @Query("update DbPlayer p set p.lastLogout=?1 where p.id= ?2")
    @Modifying
    public int updateLastLogoutById(int lastLogout, long id);

    @Query("update DbPlayer p set p.binding=?1 where p.id= ?2")
    @Modifying
    public int updateBindingById(int binding, long id);

    @Query("update DbPlayer p set p.realReferer=?1 where p.id= ?2")
    @Modifying
    public int updateRealRefererById(long realReferer, long id);


    @Query("update DbPlayer p set p.lastGameTime=?1 where p.id= ?2")
    @Modifying
    public int updateLastGameTimeById(int lastGameTime, long id);

    @Query("update DbPlayer p set p.activeRecordingTime=?1 where p.id= ?2")
    @Modifying
    public int updateActiveRecordingTimeById(int activeRecordingTime, long id);


    @Query("update DbPlayer p set p.vipExp=?1,p.lastGameTime=?2 where p.id= ?3")
    @Modifying
    public int updateVipExpAndLastGameTimeById(int vipExp, int lastGameTime, long id);


    @Query("update DbPlayer p set p.vipLevel=?1 where p.id= ?2")
    @Modifying
    public int updateVipLevelById(int vipLevel, long id);


    @Query("update DbPlayer p set p.vipExp=?1,p.activeRecordingTime=?2 where p.id= ?3")
    @Modifying
    public int updateVipExpAndActiveRecordingTimeById(int vipExp, int activeRecordingTime, long id);

    @Query("update DbPlayer p set p.emailAdress=?1 where p.id=?2")
    @Modifying
    public int updateEmailAdreeById(String name, long id);

    @Query(value = "select email_adress from db_player where id = ?1 limit 1", nativeQuery = true)
    public String searchEmailAdress(long pid);
}
