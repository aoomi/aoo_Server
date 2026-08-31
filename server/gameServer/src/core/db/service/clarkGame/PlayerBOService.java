package core.db.service.clarkGame;

import com.ddm.server.annotation.Autowired;
import com.ddm.server.annotation.Service;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.GameConfig;
import core.db.DataBaseMgr;
import core.db.dao.clarkGame.PlayerBODao;
import core.db.entity.clarkGame.PlayerBO;
import core.db.other.Criteria;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;
import core.network.client2game.ClientSession;

import java.util.concurrent.ThreadLocalRandom;

@Service(source = "clark_game")
public class PlayerBOService implements BaseService<PlayerBO> {

    private static final int PLAYER_ID_MIN = 100000;
    private static final int PLAYER_ID_MAX_EXCLUSIVE = 1000000;

    @Autowired
    private PlayerBODao playerBODao;

    /**
     * 分配对外展示的六位随机玩家 ID。
     *
     * 数据库主键仍是最终唯一约束；这里先查重以减少冲突。当前容量目标为
     * 一万同时在线，六位空间足够，未来逼近几十万注册量时应迁移到独立
     * publicId 字段并扩大号码空间，避免把展示号继续绑定内部主键。
     */
    public long nextRandomPlayerId() {
        for (int attempt = 0; attempt < 128; attempt++) {
            long candidate = ThreadLocalRandom.current().nextInt(PLAYER_ID_MIN, PLAYER_ID_MAX_EXCLUSIVE);
            if (findOne(candidate, null) == null) {
                return candidate;
            }
        }
        throw new IllegalStateException("六位玩家ID分配失败，请检查号码池容量");
    }

    public long createPlayer(ClientSession session, int serverID, String headImageUrl,
                             int sex, long familyID, int real_referer, int tourist) {
        try {
            String nameSql = "(SELECT CONCAT(\"游客_\",auto_increment) FROM information_schema.`TABLES` WHERE TABLE_SCHEMA='" + DataBaseMgr.get("clark_game").getConnection().getCatalog() + "' AND TABLE_NAME='player')";
            String accountSql = "(SELECT auto_increment FROM information_schema.`TABLES` WHERE TABLE_SCHEMA='" + DataBaseMgr.get("clark_game").getConnection().getCatalog() + "' AND TABLE_NAME='player')";
            return playerBODao.insertAndGetGeneratedKeys("INSERT INTO `player` (`name`,`accountID`,`sid`,`wx_unionid`,`headImageUrl`,`sex`,`familyID`,`real_referer`,`icon`," +
                    "" + "`lv`,`vipLevel`,`gmLevel`,`roomCard`,`crystal`,`gold`" +
                    ") VALUES (" + nameSql + "," + accountSql + "," + serverID + ",\"" + session.getWxUnionid() + "\",\"" + headImageUrl + "\"," + sex + "," + familyID + ",\"" + real_referer + "\"," + tourist
                    + ",0,0,0," + GameConfig.NewPlayerCard() + "," + GameConfig.NewPlayerCrystal() + "," + GameConfig.NewPlayerGold()
                    + ")");
        } catch (Exception e) {
            CommLogD.error("createPlayer:" + e.getMessage());
            return -1;
        }
    }

    public Long count(Criteria criteria) {
        return playerBODao.count(criteria);
    }

    @Override
    public CustomerDao getDefaultDao() {
        return playerBODao;
    }
}
